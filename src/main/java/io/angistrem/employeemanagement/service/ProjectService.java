package io.angistrem.employeemanagement.service;

import io.angistrem.employeemanagement.model.BudgetItem;
import io.angistrem.employeemanagement.model.ProjectDocument;
import io.angistrem.employeemanagement.model.ProjectResource;
import io.angistrem.employeemanagement.model.Settings;
import io.angistrem.employeemanagement.model.Task;
import io.angistrem.employeemanagement.util.AppSettings;

import java.util.ArrayList;
import java.util.List;

public class ProjectService {
    private static final String DEFAULT_PROJECT_NAME = "Новый проект";

    public String normalizeProjectName(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT_PROJECT_NAME;
        }
        return value.trim();
    }

    public String normalizeAuthor(String value) {
        return value == null ? "" : value.trim();
    }

    public ProjectDocument createProjectDocument(
            String projectName,
            String author,
            List<Task> tasks,
            List<ProjectResource> resources,
            List<BudgetItem> budgetItems
    ) {
        ProjectDocument document = new ProjectDocument();
        document.setProjectName(normalizeProjectName(projectName));
        document.setAuthor(normalizeAuthor(author));
        document.setTasks(tasks == null ? List.of() : new ArrayList<>(tasks));
        document.setResources(resources == null ? List.of() : new ArrayList<>(resources));
        document.setBudgetItems(budgetItems == null ? List.of() : new ArrayList<>(budgetItems));
        return document;
    }

    public void syncMetadataToSessionSettings(String projectName, String author) {
        Settings settings = AppSettings.get();
        if (settings == null) {
            return;
        }
        settings.setProjectName(normalizeProjectName(projectName));
        settings.setAuthorProject(normalizeAuthor(author));
    }
}

