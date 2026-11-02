package io.angistrem.employeemanagement.service;

import io.angistrem.employeemanagement.model.SubTask;
import io.angistrem.employeemanagement.model.Task;
import io.angistrem.employeemanagement.model.TaskDependency;

import java.util.List;

public class DependencyEditingService {

    public boolean upsertDependency(Object sourceItem, Object targetItem, String type, int lagDays, boolean critical) {
        if (sourceItem == null || targetItem == null || sourceItem == targetItem) {
            return false;
        }

        List<TaskDependency> dependencies = dependencyListForItem(targetItem);
        String predecessorKey = itemKey(sourceItem);
        if (dependencies == null || predecessorKey == null) {
            return false;
        }

        for (TaskDependency existing : dependencies) {
            if (predecessorKey.equals(existing.getPredecessorPosition())) {
                existing.setType(type);
                existing.setLagDays(lagDays);
                existing.setCritical(critical);
                return true;
            }
        }

        dependencies.add(new TaskDependency(predecessorKey, type, lagDays, critical));
        return true;
    }

    public void applyEdit(TaskDependency dependency, String type, int lagDays, boolean critical) {
        if (dependency == null) {
            return;
        }
        dependency.setType(type);
        dependency.setLagDays(lagDays);
        dependency.setCritical(critical);
    }

    public String itemLabel(Object item) {
        if (item instanceof Task task) {
            return task.getPosition() + " " + task.getName();
        }
        if (item instanceof SubTask subTask) {
            return subTask.getSubPosition() + " " + subTask.getName();
        }
        return "элемент";
    }

    private List<TaskDependency> dependencyListForItem(Object item) {
        if (item instanceof Task task) {
            return task.getDependencies();
        }
        if (item instanceof SubTask subTask) {
            return subTask.getDependencies();
        }
        return null;
    }

    private String itemKey(Object item) {
        if (item instanceof Task task) {
            return task.getPosition();
        }
        if (item instanceof SubTask subTask) {
            return subTask.getSubPosition();
        }
        return null;
    }
}
