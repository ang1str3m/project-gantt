package io.angistrem.employeemanagement.controller;

import io.angistrem.employeemanagement.model.ProjectResource;
import io.angistrem.employeemanagement.util.SceneManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

public class ResourceDirectoryController {
    @FXML private TableView<ProjectResource> resourceTable;
    @FXML private TableColumn<ProjectResource, String> nameColumn;
    @FXML private TableColumn<ProjectResource, String> typeColumn;
    @FXML private TableColumn<ProjectResource, String> companyColumn;
    @FXML private TableColumn<ProjectResource, String> roleColumn;
    @FXML private TableColumn<ProjectResource, String> capacityColumn;
    @FXML private TableColumn<ProjectResource, String> costColumn;
    @FXML private TableColumn<ProjectResource, String> activeColumn;

    @FXML private TextField nameField;
    @FXML private ComboBox<String> typeBox;
    @FXML private TextField companyField;
    @FXML private TextField roleField;
    @FXML private TextField capacityField;
    @FXML private TextField costField;
    @FXML private CheckBox activeCheck;

    private final ObservableList<ProjectResource> workingResources = FXCollections.observableArrayList();
    private ObservableList<ProjectResource> sourceResources;
    private boolean changed;

    @FXML
    public void initialize() {
        typeBox.getItems().setAll("PERSON", "EQUIPMENT", "CONTRACTOR", "MATERIAL");

        nameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        typeColumn.setCellValueFactory(data -> new SimpleStringProperty(localizeType(data.getValue().getType())));
        companyColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCompany()));
        roleColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getRole()));
        capacityColumn.setCellValueFactory(data -> new SimpleStringProperty(formatDouble(data.getValue().getCapacityPerDay())));
        costColumn.setCellValueFactory(data -> new SimpleStringProperty(formatDouble(data.getValue().getCostPerDay())));
        activeColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().isActive() ? "Да" : "Нет"));

        resourceTable.setItems(workingResources);
        resourceTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            pushFormToResource(oldValue);
            populateForm(newValue);
        });
    }

    public void setResources(ObservableList<ProjectResource> resources) {
        this.sourceResources = resources;
        workingResources.setAll(copyResources(resources == null ? List.of() : resources));
        if (!workingResources.isEmpty()) {
            resourceTable.getSelectionModel().selectFirst();
        } else {
            clearForm();
        }
    }

    public boolean isChanged() {
        return changed;
    }

    @FXML
    private void handleAddResource() {
        ProjectResource resource = new ProjectResource();
        resource.setName("Новый ресурс");
        resource.setType("PERSON");
        resource.setCapacityPerDay(1.0);
        resource.setActive(true);
        workingResources.add(resource);
        resourceTable.getSelectionModel().select(resource);
        nameField.requestFocus();
        nameField.selectAll();
    }

    @FXML
    private void handleDeleteResource() {
        ProjectResource selected = resourceTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        int index = resourceTable.getSelectionModel().getSelectedIndex();
        workingResources.remove(selected);
        if (workingResources.isEmpty()) {
            clearForm();
        } else {
            resourceTable.getSelectionModel().select(Math.max(0, Math.min(index, workingResources.size() - 1)));
        }
    }

    @FXML
    private void handleSave() {
        ProjectResource selected = resourceTable.getSelectionModel().getSelectedItem();
        pushFormToResource(selected);
        if (!validateResources()) {
            return;
        }
        if (sourceResources != null) {
            sourceResources.setAll(copyResources(workingResources));
        }
        changed = true;
        close();
    }

    @FXML
    private void handleCancel() {
        changed = false;
        close();
    }

    private boolean validateResources() {
        for (ProjectResource resource : workingResources) {
            if (resource.getName() == null || resource.getName().isBlank()) {
                showError("Укажите название ресурса для каждой строки справочника.");
                return false;
            }
        }
        return true;
    }

    private void pushFormToResource(ProjectResource resource) {
        if (resource == null) {
            return;
        }
        resource.setName(nameField.getText());
        resource.setType(typeBox.getValue());
        resource.setCompany(companyField.getText());
        resource.setRole(roleField.getText());
        resource.setCapacityPerDay(parseDouble(capacityField.getText(), 1.0));
        resource.setCostPerDay(parseDouble(costField.getText(), 0.0));
        resource.setActive(activeCheck.isSelected());
        resourceTable.refresh();
    }

    private void populateForm(ProjectResource resource) {
        if (resource == null) {
            clearForm();
            return;
        }
        nameField.setText(resource.getName());
        typeBox.setValue(resource.getType());
        companyField.setText(resource.getCompany());
        roleField.setText(resource.getRole());
        capacityField.setText(formatDouble(resource.getCapacityPerDay()));
        costField.setText(formatDouble(resource.getCostPerDay()));
        activeCheck.setSelected(resource.isActive());
    }

    private void clearForm() {
        nameField.clear();
        typeBox.setValue("PERSON");
        companyField.clear();
        roleField.clear();
        capacityField.setText("1");
        costField.setText("0");
        activeCheck.setSelected(true);
    }

    private List<ProjectResource> copyResources(List<ProjectResource> resources) {
        List<ProjectResource> copies = new ArrayList<>();
        for (ProjectResource resource : resources) {
            if (resource == null) {
                continue;
            }
            ProjectResource copy = new ProjectResource();
            copy.setId(resource.getId());
            copy.setName(resource.getName());
            copy.setType(resource.getType());
            copy.setCompany(resource.getCompany());
            copy.setRole(resource.getRole());
            copy.setCapacityPerDay(resource.getCapacityPerDay());
            copy.setCostPerDay(resource.getCostPerDay());
            copy.setActive(resource.isActive());
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

    private String localizeType(String type) {
        if (type == null) {
            return "";
        }
        return switch (type) {
            case "PERSON" -> "Человек";
            case "EQUIPMENT" -> "Техника";
            case "CONTRACTOR" -> "Подрядчик";
            case "MATERIAL" -> "Материал";
            default -> type;
        };
    }

    private String formatDouble(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.00001) {
            return String.valueOf((long) Math.rint(value));
        }
        return String.format(java.util.Locale.US, "%.2f", value);
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Ресурсы");
        alert.setHeaderText(null);
        alert.setContentText(message);
        SceneManager.applyThemeToDialog(alert);
        alert.showAndWait();
    }

    private void close() {
        ((Stage) resourceTable.getScene().getWindow()).close();
    }
}
