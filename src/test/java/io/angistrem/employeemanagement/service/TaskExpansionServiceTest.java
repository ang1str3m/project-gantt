package io.angistrem.employeemanagement.service;

import io.angistrem.employeemanagement.model.Task;
import io.angistrem.employeemanagement.model.TaskStatus;
import javafx.scene.control.TreeItem;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskExpansionServiceTest {

    private final TaskExpansionService service = new TaskExpansionService();

    @Test
    void setSelectedTaskExpandedShouldAffectTaskItemsOnly() {
        Task task = new Task("T", "C", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 2), TaskStatus.PLANNED, "Medium");
        TreeItem<Object> taskItem = new TreeItem<>(task);
        taskItem.setExpanded(false);
        TreeItem<Object> otherItem = new TreeItem<>(new Object());

        assertTrue(service.setSelectedTaskExpanded(taskItem, true));
        assertTrue(taskItem.isExpanded());
        assertFalse(service.setSelectedTaskExpanded(otherItem, true));
        assertFalse(service.setSelectedTaskExpanded(null, true));
    }

    @Test
    void applyExpansionStateToRootChildrenShouldToggleOnlyTaskChildren() {
        Task task = new Task("T", "C", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 2), TaskStatus.PLANNED, "Medium");
        TreeItem<Object> root = new TreeItem<>(null);
        TreeItem<Object> taskChild = new TreeItem<>(task);
        TreeItem<Object> otherChild = new TreeItem<>(new Object());
        taskChild.setExpanded(false);
        otherChild.setExpanded(false);
        root.getChildren().addAll(taskChild, otherChild);

        assertTrue(service.applyExpansionStateToRootChildren(root, true));
        assertTrue(taskChild.isExpanded());
        assertFalse(otherChild.isExpanded());
        assertFalse(service.applyExpansionStateToRootChildren(null, true));
    }
}
