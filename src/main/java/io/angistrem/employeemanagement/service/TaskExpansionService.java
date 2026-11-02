package io.angistrem.employeemanagement.service;

import io.angistrem.employeemanagement.model.Task;
import javafx.scene.control.TreeItem;

public class TaskExpansionService {

    public boolean setSelectedTaskExpanded(TreeItem<Object> selected, boolean expanded) {
        if (selected == null || !(selected.getValue() instanceof Task)) {
            return false;
        }
        selected.setExpanded(expanded);
        return true;
    }

    public boolean applyExpansionStateToRootChildren(TreeItem<Object> root, boolean expanded) {
        if (root == null) {
            return false;
        }
        for (TreeItem<Object> item : root.getChildren()) {
            if (item != null && item.getValue() instanceof Task) {
                item.setExpanded(expanded);
            }
        }
        return true;
    }
}
