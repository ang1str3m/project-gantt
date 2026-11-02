package io.angistrem.employeemanagement.controller.menu;

import io.angistrem.employeemanagement.controller.MainController;
import io.angistrem.employeemanagement.dialog.SubTaskDialog;
import io.angistrem.employeemanagement.dialog.TaskDialog;
import io.angistrem.employeemanagement.dialog.GroupDialog;
import io.angistrem.employeemanagement.model.SubTask;
import io.angistrem.employeemanagement.model.Task;
import io.angistrem.employeemanagement.view.GanttChart;
import javafx.collections.ObservableList;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableView;

import java.util.List;

public class TaskContextMenu extends ContextMenu {

    private final MainController mainController;
    private final TreeItem<Object> selectedItem;
    private final TreeTableView<Object> treeTableView;
    private final GanttChart ganttChart;
    private final ObservableList<Task> tasks;
    private final Runnable onDataChanged;

    public TaskContextMenu(MainController mainController,
                           TreeItem<Object> selectedItem,
                           TreeTableView<Object> treeTableView,
                           GanttChart ganttChart,
                           ObservableList<Task> tasks) {
        this(mainController, selectedItem, treeTableView, ganttChart, tasks, null);
    }

    public TaskContextMenu(MainController mainController,
                           TreeItem<Object> selectedItem,
                           TreeTableView<Object> treeTableView,
                           GanttChart ganttChart,
                           ObservableList<Task> tasks,
                           Runnable onDataChanged) {
        this.mainController = mainController;
        this.selectedItem = selectedItem;
        this.treeTableView = treeTableView;
        this.ganttChart = ganttChart;
        this.tasks = tasks;
        this.onDataChanged = onDataChanged;
        buildMenu();
    }

    private void buildMenu() {
        MenuItem addTaskItem = new MenuItem("\u0421\u043e\u0437\u0434\u0430\u0442\u044c \u0437\u0430\u0434\u0430\u0447\u0443");
        addTaskItem.setOnAction(e -> createTask());
        MenuItem addGroupItem = new MenuItem("\u0421\u043e\u0437\u0434\u0430\u0442\u044c \u0433\u0440\u0443\u043f\u043f\u0443");
        addGroupItem.setOnAction(e -> createGroup());

        MenuItem collapseAllItem = new MenuItem("\u0421\u0432\u0435\u0440\u043d\u0443\u0442\u044c \u0432\u0441\u0435 \u043f\u043e\u0434\u0437\u0430\u0434\u0430\u0447\u0438");
        collapseAllItem.setOnAction(e -> {
            if (mainController != null) {
                mainController.collapseAllSubTasks();
            }
        });

        MenuItem expandAllItem = new MenuItem("\u0420\u0430\u0437\u0432\u0435\u0440\u043d\u0443\u0442\u044c \u0432\u0441\u0435 \u043f\u043e\u0434\u0437\u0430\u0434\u0430\u0447\u0438");
        expandAllItem.setOnAction(e -> {
            if (mainController != null) {
                mainController.expandAllSubTasks();
            }
        });

        Object value = selectedItem != null ? selectedItem.getValue() : null;

        if (value instanceof Task task) {
            MenuItem addChildItem = task.isGroup()
                    ? new MenuItem("\u0414\u043e\u0431\u0430\u0432\u0438\u0442\u044c \u0437\u0430\u0434\u0430\u0447\u0443 \u0432 \u0433\u0440\u0443\u043f\u043f\u0443")
                    : new MenuItem("\u0414\u043e\u0431\u0430\u0432\u0438\u0442\u044c \u043f\u043e\u0434\u0437\u0430\u0434\u0430\u0447\u0443");
            addChildItem.setOnAction(e -> {
                if (task.isGroup()) {
                    addTaskToGroup(task);
                } else {
                    addSubTask(task);
                }
            });

            MenuItem editTaskItem = new MenuItem(task.isGroup()
                    ? "\u0420\u0435\u0434\u0430\u043a\u0442\u0438\u0440\u043e\u0432\u0430\u0442\u044c \u0433\u0440\u0443\u043f\u043f\u0443"
                    : "\u0420\u0435\u0434\u0430\u043a\u0442\u0438\u0440\u043e\u0432\u0430\u0442\u044c \u0437\u0430\u0434\u0430\u0447\u0443");
            editTaskItem.setOnAction(e -> editTask(task));

            MenuItem deleteTaskItem = new MenuItem(task.isGroup()
                    ? "\u0423\u0434\u0430\u043b\u0438\u0442\u044c \u0433\u0440\u0443\u043f\u043f\u0443"
                    : "\u0423\u0434\u0430\u043b\u0438\u0442\u044c \u0437\u0430\u0434\u0430\u0447\u0443");
            deleteTaskItem.setOnAction(e -> deleteTask(task));

            MenuItem taskResourcesItem = new MenuItem("\u0420\u0435\u0441\u0443\u0440\u0441\u044b \u0437\u0430\u0434\u0430\u0447\u0438");
            taskResourcesItem.setDisable(task.isGroup());
            taskResourcesItem.setOnAction(e -> openTaskResources(task));

            MenuItem expandSelectedItem = new MenuItem("\u0420\u0430\u0437\u0432\u0435\u0440\u043d\u0443\u0442\u044c \u043f\u043e\u0434\u0437\u0430\u0434\u0430\u0447\u0438 \u0432\u044b\u0431\u0440\u0430\u043d\u043d\u043e\u0439 \u0437\u0430\u0434\u0430\u0447\u0438");
            expandSelectedItem.setOnAction(e -> {
                if (mainController != null) {
                    mainController.expandSelectedTaskSubTasks();
                }
            });

            MenuItem collapseSelectedItem = new MenuItem("\u0421\u0432\u0435\u0440\u043d\u0443\u0442\u044c \u043f\u043e\u0434\u0437\u0430\u0434\u0430\u0447\u0438 \u0432\u044b\u0431\u0440\u0430\u043d\u043d\u043e\u0439 \u0437\u0430\u0434\u0430\u0447\u0438");
            collapseSelectedItem.setOnAction(e -> {
                if (mainController != null) {
                    mainController.collapseSelectedTaskSubTasks();
                }
            });

            getItems().addAll(
                    addTaskItem,
                    addGroupItem,
                    addChildItem,
                    taskResourcesItem,
                    editTaskItem,
                    deleteTaskItem,
                    new SeparatorMenuItem(),
                    expandSelectedItem,
                    collapseSelectedItem,
                    expandAllItem,
                    collapseAllItem
            );
            return;
        }

        if (value instanceof SubTask subTask) {
            MenuItem editSubTaskItem = new MenuItem("\u0420\u0435\u0434\u0430\u043a\u0442\u0438\u0440\u043e\u0432\u0430\u0442\u044c \u043f\u043e\u0434\u0437\u0430\u0434\u0430\u0447\u0443");
            editSubTaskItem.setOnAction(e -> editSubTask(subTask));

            MenuItem deleteSubTaskItem = new MenuItem("\u0423\u0434\u0430\u043b\u0438\u0442\u044c \u043f\u043e\u0434\u0437\u0430\u0434\u0430\u0447\u0443");
            deleteSubTaskItem.setOnAction(e -> deleteSubTask(subTask));

            getItems().addAll(
                    addTaskItem,
                    addGroupItem,
                    editSubTaskItem,
                    deleteSubTaskItem,
                    new SeparatorMenuItem(),
                    expandAllItem,
                    collapseAllItem
            );
            return;
        }

        getItems().addAll(addTaskItem, addGroupItem, new SeparatorMenuItem(), expandAllItem, collapseAllItem);
    }

    private void createTask() {
        TaskDialog dialog = new TaskDialog(resolveAvailableTasks());
        dialog.show();

        Task result = dialog.getResult();
        if (result != null) {
            tasks.add(result);
            refreshView();
        }
    }

    private void createGroup() {
        GroupDialog dialog = new GroupDialog();
        dialog.show();

        Task result = dialog.getResult();
        if (result != null) {
            tasks.add(result);
            refreshView();
        }
    }

    private void addSubTask(Task parentTask) {
        SubTaskDialog dialog = new SubTaskDialog(tasks);
        dialog.setParentTask(parentTask);
        dialog.show();

        SubTask result = dialog.getResult();
        if (result != null) {
            parentTask.addSubTask(result);
            refreshView();
        }
    }

    private void addTaskToGroup(Task group) {
        TaskDialog dialog = new TaskDialog(resolveAvailableTasks());
        dialog.show();

        Task result = dialog.getResult();
        if (result != null) {
            result.setTaskType(result.isMilestone() ? "MILESTONE" : "TASK");
            if (mainController != null) {
                mainController.addTaskToGroup(group, result);
            } else {
                group.addChildTask(result);
                refreshView();
            }
        }
    }

    private void editTask(Task task) {
        if (task.isGroup()) {
            editGroup(task);
            return;
        }
        boolean groupTask = task.isGroup();
        TaskDialog dialog = new TaskDialog(task, tasks);
        dialog.show();

        Task result = dialog.getResult();
        if (result != null) {
            task.setName(result.getName());
            task.setCompany(result.getCompany());
            task.setStart(result.getStart());
            task.setEnd(result.getEnd());
            task.setStatus(result.getStatus());
            task.setPriority(result.getPriority());
            task.setTaskType(groupTask ? "GROUP" : result.getTaskType());
            task.setDependencies(groupTask ? java.util.List.of() : result.getDependencies());
            task.setCriticalDependency(result.isCriticalDependency());
            task.setContractAdvanceDate(result.getContractAdvanceDate());
            task.setKsPaymentDate(result.getKsPaymentDate());
            task.setRequireAdvanceBeforeStart(result.isRequireAdvanceBeforeStart());
            task.setRequireKsBeforeStart(result.isRequireKsBeforeStart());
            task.updatePercentFromSubTasks();
            if (mainController != null) {
                mainController.handleManualDialogEdit(task);
            } else {
                refreshView();
            }
        }
    }

    private void editGroup(Task task) {
        GroupDialog dialog = new GroupDialog(task);
        dialog.show();

        Task result = dialog.getResult();
        if (result != null) {
            task.setName(result.getName());
            task.updateAggregateFromChildren();
            if (mainController != null) {
                mainController.handleManualDialogEdit(task);
            } else {
                refreshView();
            }
        }
    }

    private void deleteTask(Task task) {
        if (mainController != null && tasks.contains(task)) {
            mainController.deleteTaskWithUndo(task);
        } else {
            Task parentGroup = findParentGroup(task, tasks);
            if (parentGroup != null) {
                parentGroup.removeChildTask(task);
            } else {
                tasks.remove(task);
            }
        }
        refreshView();
    }

    private void openTaskResources(Task task) {
        if (mainController == null || task == null || task.isGroup()) {
            return;
        }
        mainController.openTaskResources(task);
    }

    private void editSubTask(SubTask subTask) {
        SubTaskDialog dialog = new SubTaskDialog(subTask, tasks);
        dialog.show();

        SubTask result = dialog.getResult();
        if (result != null) {
            subTask.setName(result.getName());
            subTask.setCompany(result.getCompany());
            subTask.setStart(result.getStart());
            subTask.setEnd(result.getEnd());
            subTask.setUnitOfMeasurement(result.getUnitOfMeasurement());
            subTask.setVolumeProject(result.getVolumeProject());
            subTask.setVolumeActual(result.getVolumeActual());
            subTask.setDependencies(result.getDependencies());
            subTask.updatePercentVolume();
            subTask.updateStatus();

            if (subTask.getParentTask() != null) {
                subTask.getParentTask().updatePercentFromSubTasks();
            }
            if (mainController != null) {
                mainController.handleManualDialogEdit(subTask);
            } else {
                refreshView();
            }
        }
    }

    private void deleteSubTask(SubTask subTask) {
        if (mainController != null) {
            mainController.deleteSubTaskWithUndo(subTask);
        } else {
            for (Task task : tasks) {
                if (task.getObservableSubTasks().contains(subTask)) {
                    task.removeSubTask(subTask);
                    break;
                }
            }
        }
        refreshView();
    }

    private void refreshView() {
        if (treeTableView != null) {
            treeTableView.refresh();
        }
        if (ganttChart != null) {
            ganttChart.refresh();
        }
        if (onDataChanged != null) {
            onDataChanged.run();
        }
    }

    private List<Task> resolveAvailableTasks() {
        if (mainController != null) {
            return mainController.getAllTasksSnapshot().stream()
                    .filter(task -> task != null && !task.isGroup())
                    .toList();
        }
        return new java.util.ArrayList<>(tasks).stream()
                .filter(task -> task != null && !task.isGroup())
                .toList();
    }

    private Task findParentGroup(Task target, ObservableList<Task> roots) {
        for (Task root : roots) {
            Task parent = findParentGroupRecursive(root, target);
            if (parent != null) {
                return parent;
            }
        }
        return null;
    }

    private Task findParentGroupRecursive(Task candidate, Task target) {
        for (Task child : candidate.getObservableChildTasks()) {
            if (child == target) {
                return candidate;
            }
            Task parent = findParentGroupRecursive(child, target);
            if (parent != null) {
                return parent;
            }
        }
        return null;
    }
}
