package io.angistrem.employeemanagement.controller;

import io.angistrem.employeemanagement.model.Task;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class TaskTreeViewSupport {

    private TaskTreeViewSupport() {
    }

    static int parsePosition(String value) {
        if (value == null || value.isBlank()) {
            return Integer.MAX_VALUE;
        }

        String normalized = value.replace(',', '.').trim();
        String[] parts = normalized.split("\\.");
        int result = 0;

        for (String part : parts) {
            String digits = part.replaceAll("[^0-9]", "");
            if (digits.isBlank()) {
                continue;
            }
            int n = Integer.parseInt(digits);
            result = result * 1000 + n;
        }

        return result;
    }

    static int getVisibleRowCount(TreeTableView<Object> treeTableView) {
        int count = 0;
        TreeItem<Object> root = treeTableView.getRoot();

        for (TreeItem<Object> item : root.getChildren()) {
            count += countVisible(item);
        }

        return count;
    }

    static VisibleItems collectVisibleItems(TreeTableView<Object> treeTableView) {
        Set<Object> visible = new HashSet<>();
        List<Object> orderedItems = new ArrayList<>();
        TreeItem<Object> root = treeTableView.getRoot();

        for (TreeItem<Object> item : root.getChildren()) {
            collectVisible(item, visible, orderedItems);
        }

        return new VisibleItems(visible, orderedItems);
    }

    private static int countVisible(TreeItem<Object> item) {
        if (item == null) {
            return 0;
        }

        int count = 1;
        if (item.isExpanded()) {
            for (TreeItem<Object> child : item.getChildren()) {
                count += countVisible(child);
            }
        }
        return count;
    }

    private static void collectVisible(TreeItem<Object> item, Set<Object> visible, List<Object> orderedItems) {
        if (item == null || item.getValue() == null) {
            return;
        }

        Object value = item.getValue();
        visible.add(value);
        orderedItems.add(value);

        if (value instanceof Task task && item.isExpanded()) {
            visible.add(task + "_expanded");
        }

        if (!item.isExpanded()) {
            return;
        }

        for (TreeItem<Object> child : item.getChildren()) {
            collectVisible(child, visible, orderedItems);
        }
    }

    static final class VisibleItems {
        private final Set<Object> visible;
        private final List<Object> orderedItems;

        VisibleItems(Set<Object> visible, List<Object> orderedItems) {
            this.visible = visible;
            this.orderedItems = orderedItems;
        }

        Set<Object> visible() {
            return visible;
        }

        List<Object> orderedItems() {
            return orderedItems;
        }
    }
}
