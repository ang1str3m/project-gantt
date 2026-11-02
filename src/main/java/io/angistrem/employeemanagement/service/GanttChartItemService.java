package io.angistrem.employeemanagement.service;

import io.angistrem.employeemanagement.model.SubTask;
import io.angistrem.employeemanagement.model.Task;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class GanttChartItemService {

    public List<Object> resolveOrderedItems(List<Task> tasks, List<Object> explicitDisplayOrder, Set<Object> visibleItems) {
        if (explicitDisplayOrder != null && !explicitDisplayOrder.isEmpty()) {
            return explicitDisplayOrder;
        }

        List<Object> orderedItems = new ArrayList<>();
        if (tasks == null) {
            return orderedItems;
        }
        for (Task task : tasks) {
            appendTaskToDisplayOrder(task, orderedItems, visibleItems);
        }
        return orderedItems;
    }

    public boolean isTaskVisible(Task task, Set<Object> visibleItems) {
        return visibleItems == null || visibleItems.isEmpty() || visibleItems.contains(task);
    }

    public boolean isSubTaskVisible(SubTask subTask, Set<Object> visibleItems) {
        return visibleItems == null || visibleItems.isEmpty() || visibleItems.contains(subTask);
    }

    private boolean isTaskExpanded(Task task, Set<Object> visibleItems) {
        if (visibleItems == null || visibleItems.isEmpty()) {
            return true;
        }
        return visibleItems.contains(task + "_expanded");
    }

    private void appendTaskToDisplayOrder(Task task, List<Object> orderedItems, Set<Object> visibleItems) {
        if (task == null) {
            return;
        }
        orderedItems.add(task);
        if (!isTaskExpanded(task, visibleItems)) {
            return;
        }
        if (task.isGroup()) {
            for (Task childTask : task.getObservableChildTasks()) {
                appendTaskToDisplayOrder(childTask, orderedItems, visibleItems);
            }
            return;
        }
        orderedItems.addAll(task.getObservableSubTasks());
    }
}
