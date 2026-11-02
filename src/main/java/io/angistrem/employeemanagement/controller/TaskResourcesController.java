package io.angistrem.employeemanagement.controller;

import io.angistrem.employeemanagement.model.ProjectResource;
import io.angistrem.employeemanagement.model.Task;
import io.angistrem.employeemanagement.model.TaskResourceAssignment;
import io.angistrem.employeemanagement.util.SceneManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TaskResourcesController {
    @FXML private Label taskTitleLabel;
    @FXML private TableView<TaskResourceAssignment> assignmentTable;
    @FXML private TableColumn<TaskResourceAssignment, String> resourceColumn;
    @FXML private TableColumn<TaskResourceAssignment, String> roleColumn;
    @FXML private TableColumn<TaskResourceAssignment, String> allocationColumn;
    @FXML private TableColumn<TaskResourceAssignment, String> plannedHoursColumn;
    @FXML private TableColumn<TaskResourceAssignment, String> actualHoursColumn;

    @FXML private ComboBox<String> resourceBox;
    @FXML private TextField roleField;
    @FXML private TextField allocationField;
    @FXML private TextField plannedHoursField;
    @FXML private TextField actualHoursField;

    private final ObservableList<TaskResourceAssignment> workingAssignments = FXCollections.observableArrayList();
    private final Map<String, ProjectResource> resourceByLabel = new LinkedHashMap<>();
    private final Map<String, ProjectResource> resourceById = new LinkedHashMap<>();
    private Task task;
    private boolean changed;

    @FXML
    public void initialize() {
        resourceColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getResourceName()));
        roleColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getRole()));
        allocationColumn.setCellValueFactory(data -> new SimpleStringProperty(formatDouble(data.getValue().getAllocation())));
        plannedHoursColumn.setCellValueFactory(data -> new SimpleStringProperty(formatDouble(data.getValue().getPlannedHours())));
        actualHoursColumn.setCellValueFactory(data -> new SimpleStringProperty(formatDouble(data.getValue().getActualHours())));

        assignmentTable.setItems(workingAssignments);
        assignmentTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            pushFormToAssignment(oldValue);
            populateForm(newValue);
        });
    }

    public void setContext(Task task, ObservableList<ProjectResource> availableResources) {
        this.task = task;
        taskTitleLabel.setText(task == null ? "Ресурсы задачи" : "Ресурсы задачи: " + task.getName());
        workingAssignments.setAll(copyAssignments(task == null ? List.of() : task.getResourceAssignments()));
        resourceByLabel.clear();
        resourceById.clear();
        resourceBox.getItems().clear();

        if (availableResources != null) {
            for (ProjectResource resource : availableResources) {
                if (resource == null) {
                    continue;
                }
                String label = resource.getName() + (resource.getRole() == null || resource.getRole().isBlank() ? "" : " — " + resource.getRole());
                resourceByLabel.put(label, resource);
                resourceById.put(resource.getId(), resource);
                if (resource.isActive()) {
                    resourceBox.getItems().add(label);
                }
            }
        }

        if (!workingAssignments.isEmpty()) {
            assignmentTable.getSelectionModel().selectFirst();
        } else {
            clearForm();
        }
    }

    public boolean isChanged() {
        return changed;
    }

    @FXML
    private void handleAddAssignment() {
        String selectedLabel = resourceBox.getValue();
        if (selectedLabel == null || selectedLabel.isBlank()) {
            showError("Сначала выберите ресурс из справочника.");
            return;
        }
        ProjectResource resource = resourceByLabel.get(selectedLabel);
        if (resource == null) {
            showError("Не удалось определить выбранный ресурс.");
            return;
        }
        for (TaskResourceAssignment assignment : workingAssignments) {
            if (resource.getId().equals(assignment.getResourceId())) {
                assignmentTable.getSelectionModel().select(assignment);
                return;
            }
        }
        TaskResourceAssignment assignment = new TaskResourceAssignment();
        assignment.setResourceId(resource.getId());
        assignment.setResourceName(resource.getName());
        assignment.setRole(resource.getRole());
        assignment.setAllocation(1.0);
        workingAssignments.add(assignment);
        assignmentTable.getSelectionModel().select(assignment);
    }

    @FXML
    private void handleDeleteAssignment() {
        TaskResourceAssignment selected = assignmentTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        int index = assignmentTable.getSelectionModel().getSelectedIndex();
        workingAssignments.remove(selected);
        if (workingAssignments.isEmpty()) {
            clearForm();
        } else {
            assignmentTable.getSelectionModel().select(Math.max(0, Math.min(index, workingAssignments.size() - 1)));
        }
    }

    @FXML
    private void handleSave() {
        TaskResourceAssignment selected = assignmentTable.getSelectionModel().getSelectedItem();
        pushFormToAssignment(selected);
        if (task != null) {
            task.setResourceAssignments(copyAssignments(workingAssignments));
        }
        changed = true;
        close();
    }

    @FXML
    private void handleCancel() {
        changed = false;
        close();
    }

    private void pushFormToAssignment(TaskResourceAssignment assignment) {
        if (assignment == null) {
            return;
        }
        String selectedLabel = resourceBox.getValue();
        ProjectResource resource = selectedLabel == null ? resourceById.get(assignment.getResourceId()) : resourceByLabel.get(selectedLabel);
        if (resource != null) {
            assignment.setResourceId(resource.getId());
            assignment.setResourceName(resource.getName());
            if (roleField.getText() == null || roleField.getText().isBlank()) {
                assignment.setRole(resource.getRole());
            }
        }
        assignment.setRole(roleField.getText());
        assignment.setAllocation(parseDouble(allocationField.getText(), 1.0));
        assignment.setPlannedHours(parseDouble(plannedHoursField.getText(), 0.0));
        assignment.setActualHours(parseDouble(actualHoursField.getText(), 0.0));
        assignmentTable.refresh();
    }

    private void populateForm(TaskResourceAssignment assignment) {
        if (assignment == null) {
            clearForm();
            return;
        }
        ProjectResource resource = resourceById.get(assignment.getResourceId());
        if (resource != null) {
            String label = resource.getName() + (resource.getRole() == null || resource.getRole().isBlank() ? "" : " — " + resource.getRole());
            resourceBox.setValue(label);
        } else {
            resourceBox.setValue(null);
        }
        roleField.setText(assignment.getRole());
        allocationField.setText(formatDouble(assignment.getAllocation()));
        plannedHoursField.setText(formatDouble(assignment.getPlannedHours()));
        actualHoursField.setText(formatDouble(assignment.getActualHours()));
    }

    private void clearForm() {
        resourceBox.setValue(null);
        roleField.clear();
        allocationField.setText("1");
        plannedHoursField.setText("0");
        actualHoursField.setText("0");
    }

    private List<TaskResourceAssignment> copyAssignments(List<TaskResourceAssignment> assignments) {
        List<TaskResourceAssignment> copies = new ArrayList<>();
        for (TaskResourceAssignment assignment : assignments) {
            if (assignment == null) {
                continue;
            }
            TaskResourceAssignment copy = new TaskResourceAssignment();
            copy.setResourceId(assignment.getResourceId());
            copy.setResourceName(assignment.getResourceName());
            copy.setRole(assignment.getRole());
            copy.setAllocation(assignment.getAllocation());
            copy.setPlannedHours(assignment.getPlannedHours());
            copy.setActualHours(assignment.getActualHours());
            copies.add(copy);
        }
        return copies;
    }

    private double parseDouble(String value, double fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Double.parseDouble(value.replace(',', '.').trim());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private String formatDouble(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.00001) {
            return String.valueOf((long) Math.rint(value));
        }
        return String.format(java.util.Locale.US, "%.2f", value);
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Ресурсы задачи");
        alert.setHeaderText(null);
        alert.setContentText(message);
        SceneManager.applyThemeToDialog(alert);
        alert.showAndWait();
    }

    private void close() {
        ((Stage) assignmentTable.getScene().getWindow()).close();
    }
}
