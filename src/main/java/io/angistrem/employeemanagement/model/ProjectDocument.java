package io.angistrem.employeemanagement.model;

import java.util.ArrayList;
import java.util.List;

public class ProjectDocument {
    private String projectName = "";
    private String author = "";
    private List<Task> tasks = new ArrayList<>();
    private List<ProjectResource> resources = new ArrayList<>();
    private List<BudgetItem> budgetItems = new ArrayList<>();

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName == null ? "" : projectName;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author == null ? "" : author;
    }

    public List<Task> getTasks() {
        return tasks;
    }

    public void setTasks(List<Task> tasks) {
        this.tasks = tasks == null ? new ArrayList<>() : tasks;
    }

    public List<ProjectResource> getResources() {
        return resources;
    }

    public void setResources(List<ProjectResource> resources) {
        this.resources = resources == null ? new ArrayList<>() : resources;
    }

    public List<BudgetItem> getBudgetItems() {
        return budgetItems;
    }

    public void setBudgetItems(List<BudgetItem> budgetItems) {
        this.budgetItems = budgetItems == null ? new ArrayList<>() : budgetItems;
    }
}
