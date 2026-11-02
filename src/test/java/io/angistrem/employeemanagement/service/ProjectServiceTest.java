package io.angistrem.employeemanagement.service;

import io.angistrem.employeemanagement.model.BudgetItem;
import io.angistrem.employeemanagement.model.ProjectResource;
import io.angistrem.employeemanagement.model.Task;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectServiceTest {

    private final ProjectService projectService = new ProjectService();

    @Test
    void normalizeProjectNameShouldFallbackToDefaultForBlankValues() {
        assertEquals("Новый проект", projectService.normalizeProjectName(null));
        assertEquals("Новый проект", projectService.normalizeProjectName("   "));
        assertEquals("Demo", projectService.normalizeProjectName("  Demo "));
    }

    @Test
    void createProjectDocumentShouldCreateDefensiveCopies() {
        List<Task> tasks = new ArrayList<>();
        tasks.add(new Task());
        List<ProjectResource> resources = new ArrayList<>();
        ProjectResource resource = new ProjectResource();
        resource.setName("R1");
        resource.setRole("Engineer");
        resource.setCapacityPerDay(8.0);
        resources.add(resource);
        List<BudgetItem> budgetItems = new ArrayList<>();
        budgetItems.add(new BudgetItem());

        var document = projectService.createProjectDocument(" Demo ", " Author ", tasks, resources, budgetItems);

        assertEquals("Demo", document.getProjectName());
        assertEquals("Author", document.getAuthor());
        assertEquals(1, document.getTasks().size());
        assertEquals(1, document.getResources().size());
        assertEquals(1, document.getBudgetItems().size());
        assertNotSame(tasks, document.getTasks());
        assertNotSame(resources, document.getResources());
        assertNotSame(budgetItems, document.getBudgetItems());

        tasks.clear();
        resources.clear();
        budgetItems.clear();

        assertEquals(1, document.getTasks().size());
        assertEquals(1, document.getResources().size());
        assertEquals(1, document.getBudgetItems().size());
    }

    @Test
    void createProjectDocumentShouldHandleNullCollections() {
        var document = projectService.createProjectDocument(null, null, null, null, null);

        assertEquals("Новый проект", document.getProjectName());
        assertEquals("", document.getAuthor());
        assertTrue(document.getTasks().isEmpty());
        assertTrue(document.getResources().isEmpty());
        assertTrue(document.getBudgetItems().isEmpty());
    }
}
