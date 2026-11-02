package io.angistrem.employeemanagement.service;

import io.angistrem.employeemanagement.model.BudgetItem;
import io.angistrem.employeemanagement.model.SubTask;
import io.angistrem.employeemanagement.model.Task;
import javafx.collections.ObservableList;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public class TaskService {

    public List<Task> collectAllTasks(List<Task> source) {
        List<Task> collected = new ArrayList<>();
        if (source == null) {
            return collected;
        }
        for (Task task : source) {
            collectTaskRecursive(task, collected);
        }
        return collected;
    }

    public String nextTopLevelPosition(List<Task> tasks) {
        int max = 0;
        if (tasks == null) {
            return "1.";
        }
        for (Task task : tasks) {
            if (task.getPosition() == null) {
                continue;
            }
            String[] parts = task.getPosition().split("\\.");
            if (parts.length == 0) {
                continue;
            }
            String digits = parts[0].replaceAll("[^0-9]", "");
            if (digits.isBlank()) {
                continue;
            }
            max = Math.max(max, Integer.parseInt(digits));
        }
        return (max + 1) + ".";
    }

    public void normalizeTaskHierarchy(List<Task> rootTasks, List<BudgetItem> budgetItems) {
        if (rootTasks == null) {
            return;
        }

        Map<Task, String> oldPositions = new IdentityHashMap<>();
        for (Task task : collectAllTasks(rootTasks)) {
            if (task != null && task.getPosition() != null && !task.getPosition().isBlank()) {
                oldPositions.put(task, task.getPosition());
            }
        }

        for (int i = 0; i < rootTasks.size(); i++) {
            Task task = rootTasks.get(i);
            task.setPosition((i + 1) + ".");
            task.recalculateSubTaskPositions();
            task.recalculateChildTaskPositions();
            if (task.isGroup()) {
                task.updateAggregateFromChildren();
            } else {
                task.updatePercentFromSubTasks();
            }
        }

        if (budgetItems == null || budgetItems.isEmpty()) {
            return;
        }

        Map<String, String> updatedPositions = new HashMap<>();
        for (Task task : collectAllTasks(rootTasks)) {
            String oldPosition = oldPositions.get(task);
            String newPosition = task == null ? null : task.getPosition();
            if (oldPosition == null || newPosition == null || oldPosition.equals(newPosition)) {
                continue;
            }
            updatedPositions.put(oldPosition, newPosition);
        }

        for (BudgetItem item : budgetItems) {
            if (item == null || item.getTaskPosition() == null) {
                continue;
            }
            String remapped = updatedPositions.get(item.getTaskPosition());
            if (remapped != null && !remapped.isBlank()) {
                item.setTaskPosition(remapped);
            }
        }
    }

    public boolean moveTaskToGroup(List<Task> rootTasks, Task task, Task group) {
        if (!canMoveTaskIntoGroup(rootTasks, task, group)) {
            return false;
        }
        removeTaskFromCurrentParent(rootTasks, task);
        group.addChildTask(task);
        return true;
    }

    public boolean moveTaskToRoot(List<Task> rootTasks, Task task) {
        if (!canMoveTaskToRoot(rootTasks, task)) {
            return false;
        }
        removeTaskFromCurrentParent(rootTasks, task);
        rootTasks.add(task);
        return true;
    }

    public boolean moveTaskRelative(List<Task> rootTasks, Task task, Task target, boolean insertAfter) {
        if (!canReorderTaskRelative(task, target)) {
            return false;
        }

        List<Task> sourceContainer = getSiblingTaskContainer(rootTasks, task);
        List<Task> targetContainer = getSiblingTaskContainer(rootTasks, target);
        if (sourceContainer == null || targetContainer == null) {
            return false;
        }

        int targetIndex = targetContainer.indexOf(target);
        int currentIndex = sourceContainer.indexOf(task);
        if (targetIndex < 0 || currentIndex < 0) {
            return false;
        }

        if (sourceContainer == targetContainer) {
            sourceContainer.remove(currentIndex);
            if (currentIndex < targetIndex) {
                targetIndex--;
            }
        } else {
            sourceContainer.remove(currentIndex);
        }

        if (insertAfter) {
            targetIndex++;
        }

        targetIndex = Math.max(0, Math.min(targetIndex, targetContainer.size()));
        targetContainer.add(targetIndex, task);
        return true;
    }

    public boolean canMoveTaskIntoGroup(List<Task> rootTasks, Task task, Task group) {
        if (task == null || task.isGroup() || group == null || !group.isGroup() || task == group) {
            return false;
        }
        return !isDescendantTask(group, task);
    }

    public boolean canMoveTaskToRoot(List<Task> rootTasks, Task task) {
        return task != null && findParentTask(rootTasks, task) != null;
    }

    public boolean canReorderTaskRelative(Task task, Task target) {
        if (task == null || target == null || task == target) {
            return false;
        }
        return !isDescendantTask(task, target);
    }

    public boolean canMoveSubTaskToTask(SubTask subTask, Task targetTask) {
        return subTask != null && targetTask != null && !targetTask.isGroup();
    }

    public boolean applyGanttDateChange(Object item, LocalDate newStart, LocalDate newEnd) {
        if (item == null || newStart == null || newEnd == null) {
            return false;
        }

        if (item instanceof Task task) {
            if (task.isGroup()) {
                return true;
            }

            LocalDate originalStart = task.getStart();
            LocalDate originalEnd = task.getEnd();
            if (task.isMilestone()) {
                task.setStart(newEnd);
                task.setEnd(newEnd);
                return true;
            }

            task.setStart(newStart);
            task.setEnd(newEnd);

            long originalDuration = originalStart != null && originalEnd != null
                    ? ChronoUnit.DAYS.between(originalStart, originalEnd)
                    : -1;
            long newDuration = ChronoUnit.DAYS.between(newStart, newEnd);
            boolean movedAsWhole = originalStart != null && originalEnd != null && originalDuration == newDuration;
            if (movedAsWhole) {
                long shiftDays = ChronoUnit.DAYS.between(originalStart, newStart);
                if (shiftDays != 0) {
                    shiftSubTaskDates(task, shiftDays);
                }
            }
            return true;
        }

        if (item instanceof SubTask subTask) {
            subTask.setStart(newStart);
            subTask.setEnd(newEnd);
            return true;
        }

        return false;
    }

    public Task moveSubTaskToTask(SubTask subTask, Task targetTask, int index) {
        if (!canMoveSubTaskToTask(subTask, targetTask)) {
            return null;
        }
        Task sourceParent = subTask.getParentTask();
        if (sourceParent == null) {
            return null;
        }

        ObservableList<SubTask> sourceSubTasks = sourceParent.getObservableSubTasks();
        int currentIndex = sourceSubTasks.indexOf(subTask);
        if (currentIndex < 0) {
            return null;
        }

        sourceSubTasks.remove(currentIndex);
        if (sourceParent == targetTask && currentIndex < index) {
            index--;
        }

        ObservableList<SubTask> targetSubTasks = targetTask.getObservableSubTasks();
        int safeIndex = Math.max(0, Math.min(index, targetSubTasks.size()));
        targetSubTasks.add(safeIndex, subTask);
        subTask.setParentTask(targetTask);

        sourceParent.recalculateSubTaskPositions();
        sourceParent.updatePercentFromSubTasks();
        if (sourceParent != targetTask) {
            targetTask.recalculateSubTaskPositions();
            targetTask.updatePercentFromSubTasks();
        }
        return targetTask;
    }

    public Task moveSubTaskRelative(SubTask subTask, SubTask target, boolean insertAfter) {
        if (subTask == null || target == null || subTask == target) {
            return null;
        }
        Task targetParent = target.getParentTask();
        if (targetParent == null) {
            return null;
        }
        int targetIndex = targetParent.getObservableSubTasks().indexOf(target);
        if (targetIndex < 0) {
            return null;
        }
        if (insertAfter) {
            targetIndex++;
        }
        return moveSubTaskToTask(subTask, targetParent, targetIndex);
    }

    public void removeTaskFromCurrentParent(List<Task> rootTasks, Task task) {
        if (task == null) {
            return;
        }
        Task parent = findParentTask(rootTasks, task);
        if (parent != null) {
            parent.removeChildTask(task);
            parent.recalculateChildTaskPositions();
            return;
        }
        rootTasks.remove(task);
    }

    public Task findParentTask(List<Task> rootTasks, Task target) {
        if (target == null || rootTasks == null) {
            return null;
        }
        for (Task rootTask : rootTasks) {
            Task parent = findParentTaskRecursive(rootTask, target);
            if (parent != null) {
                return parent;
            }
        }
        return null;
    }

    public boolean isDescendantTask(Task candidateAncestor, Task target) {
        if (candidateAncestor == null || target == null) {
            return false;
        }
        for (Task childTask : candidateAncestor.getObservableChildTasks()) {
            if (childTask == target || isDescendantTask(childTask, target)) {
                return true;
            }
        }
        return false;
    }

    private void collectTaskRecursive(Task task, List<Task> target) {
        if (task == null) {
            return;
        }
        target.add(task);
        for (Task childTask : task.getObservableChildTasks()) {
            collectTaskRecursive(childTask, target);
        }
    }

    private Task findParentTaskRecursive(Task current, Task target) {
        if (current == null) {
            return null;
        }
        for (Task childTask : current.getObservableChildTasks()) {
            if (childTask == target) {
                return current;
            }
            Task nestedParent = findParentTaskRecursive(childTask, target);
            if (nestedParent != null) {
                return nestedParent;
            }
        }
        return null;
    }

    private List<Task> getSiblingTaskContainer(List<Task> rootTasks, Task task) {
        if (task == null) {
            return null;
        }
        Task parent = findParentTask(rootTasks, task);
        return parent == null ? rootTasks : parent.getObservableChildTasks();
    }

    private void shiftSubTaskDates(Task task, long shiftDays) {
        for (SubTask subTask : task.getObservableSubTasks()) {
            if (subTask.getStart() != null) {
                subTask.setStart(subTask.getStart().plusDays(shiftDays));
            }
            if (subTask.getEnd() != null) {
                subTask.setEnd(subTask.getEnd().plusDays(shiftDays));
            }
        }
    }
}
