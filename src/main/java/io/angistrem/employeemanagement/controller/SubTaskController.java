package io.angistrem.employeemanagement.controller;

import io.angistrem.employeemanagement.model.SubTask;
import io.angistrem.employeemanagement.model.Task;
import io.angistrem.employeemanagement.model.TaskDependency;
import io.angistrem.employeemanagement.util.SceneManager;
import io.angistrem.employeemanagement.util.ValidationUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SubTaskController {

    @FXML private TextField nameField;
    @FXML private TextField companyField;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private ComboBox<String> unitOfMeasurementBox;
    @FXML private TextField volumeProjectField;
    @FXML private TextField volumeActualField;
    @FXML private ComboBox<String> dependencyPredecessorBox;
    @FXML private ComboBox<String> dependencyTypeBox;
    @FXML private TextField dependencyLagField;
    @FXML private ListView<TaskDependency> dependencyListView;
    @FXML private Label dependencyLegendLabel;
    @FXML private Label dependencyCriticalHintLabel;
    @FXML private Button toggleDependencyCriticalButton;

    private Task parentTask;
    private SubTask subTask;
    private boolean isEditMode = false;
    private List<Task> availableTasks = List.of();
    private final ObservableList<TaskDependency> editedDependencies = FXCollections.observableArrayList();
    private final Map<String, String> dependencyPositionByLabel = new LinkedHashMap<>();

    @FXML
    public void initialize() {
        unitOfMeasurementBox.getItems().setAll("м2", "м3", "компл.", "шт.", "т", "п.м.");
        unitOfMeasurementBox.setValue("м2");

        dependencyTypeBox.getItems().setAll("FS", "SS", "FF", "SF");
        dependencyTypeBox.setValue("FS");
        dependencyLagField.setText("0");
        dependencyListView.setItems(editedDependencies);
        dependencyListView.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(TaskDependency item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : formatDependency(item));
            }
        });
        dependencyListView.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, selected) -> updateDependencyCriticalUi(selected));
        if (dependencyLegendLabel != null) {
            dependencyLegendLabel.setText("FS: начало после окончания, SS: начало после начала, FF: окончание после окончания, SF: окончание после начала");
        }

        if (!isEditMode) {
            startDatePicker.setValue(LocalDate.now());
            endDatePicker.setValue(LocalDate.now().plusDays(3));
            volumeProjectField.setText("1");
            volumeActualField.setText("0");
        }

        startDatePicker.valueProperty().addListener((obs, oldDate, newDate) -> validateDates());
        endDatePicker.valueProperty().addListener((obs, oldDate, newDate) -> validateDates());
        volumeProjectField.textProperty().addListener((obs, oldVal, newVal) -> validateVolumeFieldsSilently());
        volumeActualField.textProperty().addListener((obs, oldVal, newVal) -> validateVolumeFieldsSilently());
        updateDependencyCriticalUi(null);
    }

    public void setParentTask(Task task) {
        this.parentTask = task;
    }

    public void setAvailableTasks(List<Task> availableTasks) {
        this.availableTasks = availableTasks != null ? new ArrayList<>(availableTasks) : List.of();
        refreshPredecessorOptions();
    }

    public void setSubTask(SubTask subTask) {
        this.subTask = subTask;
        this.isEditMode = true;
        nameField.setText(subTask.getName());
        companyField.setText(subTask.getCompany());
        startDatePicker.setValue(subTask.getStart());
        endDatePicker.setValue(subTask.getEnd());
        unitOfMeasurementBox.setValue(subTask.getUnitOfMeasurement());
        volumeProjectField.setText(String.valueOf(subTask.getVolumeProject()));
        volumeActualField.setText(String.valueOf(subTask.getVolumeActual()));
        editedDependencies.setAll(copyDependencies(subTask.getDependencies()));
        refreshPredecessorOptions();
        updateDependencyCriticalUi(dependencyListView.getSelectionModel().getSelectedItem());
    }

    public SubTask getSubTask() {
        return subTask;
    }

    @FXML
    private void handleAddDependency() {
        String selectedLabel = dependencyPredecessorBox.getValue();
        if (selectedLabel == null || selectedLabel.isBlank()) {
            showError("Ошибка", "Выберите элемент-предшественник.");
            return;
        }

        String predecessorPosition = dependencyPositionByLabel.get(selectedLabel);
        if (predecessorPosition == null || predecessorPosition.isBlank()) {
            showError("Ошибка", "Не удалось определить позицию предшественника.");
            return;
        }

        int lagDays;
        try {
            lagDays = Integer.parseInt(dependencyLagField.getText().trim());
        } catch (NumberFormatException e) {
            showError("Ошибка", "Лаг должен быть целым числом.");
            return;
        }

        String type = dependencyTypeBox.getValue() == null ? "FS" : dependencyTypeBox.getValue();
        TaskDependency newDependency = new TaskDependency(predecessorPosition, type, lagDays);

        for (TaskDependency existing : editedDependencies) {
            if (predecessorPosition.equals(existing.getPredecessorPosition()) && type.equalsIgnoreCase(existing.getType())) {
                existing.setLagDays(lagDays);
                dependencyListView.refresh();
                updateDependencyCriticalUi(existing);
                return;
            }
        }

        editedDependencies.add(newDependency);
        dependencyListView.getSelectionModel().select(newDependency);
    }

    @FXML
    private void handleRemoveDependency() {
        TaskDependency selected = dependencyListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            editedDependencies.remove(selected);
        }
        updateDependencyCriticalUi(null);
    }

    @FXML
    private void handleToggleDependencyCritical() {
        TaskDependency selected = dependencyListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Ошибка", "Сначала выберите зависимость в списке.");
            return;
        }
        selected.setCritical(!selected.isCritical());
        dependencyListView.refresh();
        updateDependencyCriticalUi(selected);
    }

    @FXML
    private void handleOk() {
        if (!validateFields()) {
            return;
        }

        String name = nameField.getText().trim();
        String company = companyField.getText().trim();
        LocalDate start = startDatePicker.getValue();
        LocalDate end = endDatePicker.getValue();
        String unit = unitOfMeasurementBox.getValue();
        int volumeProject = Integer.parseInt(volumeProjectField.getText().trim());
        int volumeActual = Integer.parseInt(volumeActualField.getText().trim());

        if (subTask != null && isEditMode) {
            subTask.setName(name);
            subTask.setCompany(company);
            subTask.setStart(start);
            subTask.setEnd(end);
            subTask.setUnitOfMeasurement(unit);
            subTask.setVolumeProject(volumeProject);
            subTask.setVolumeActual(volumeActual);
        } else {
            if (parentTask == null) {
                showError("Ошибка", "Не выбрана родительская задача для подзадачи.");
                return;
            }
            subTask = new SubTask(parentTask, name, company, start, end, unit, volumeProject, volumeActual);
        }

        subTask.setDependencies(copyDependencies(editedDependencies));
        closeStage();
    }

    @FXML
    private void handleExit() {
        subTask = null;
        closeStage();
    }

    private void refreshPredecessorOptions() {
        if (dependencyPredecessorBox == null) {
            return;
        }

        String currentPosition = subTask != null ? subTask.getSubPosition() : null;
        dependencyPositionByLabel.clear();
        dependencyPredecessorBox.getItems().clear();

        for (Task task : availableTasks) {
            if (task.getPosition() != null && !task.getPosition().equals(currentPosition)) {
                String label = task.getPosition() + " - " + safeName(task.getName());
                dependencyPositionByLabel.put(label, task.getPosition());
                dependencyPredecessorBox.getItems().add(label);
            }

            for (SubTask candidate : task.getObservableSubTasks()) {
                String subPosition = candidate.getSubPosition();
                if (subPosition == null || subPosition.equals(currentPosition)) {
                    continue;
                }
                String label = subPosition + " - " + safeName(candidate.getName());
                dependencyPositionByLabel.put(label, subPosition);
                dependencyPredecessorBox.getItems().add(label);
            }
        }

        if (!dependencyPredecessorBox.getItems().isEmpty()) {
            dependencyPredecessorBox.setValue(dependencyPredecessorBox.getItems().get(0));
        }
    }

    private String formatDependency(TaskDependency dependency) {
        String type = dependency.getType() == null ? "FS" : dependency.getType().toUpperCase();
        String lag = dependency.getLagDays() == 0 ? "" : (dependency.getLagDays() > 0 ? "+" : "") + dependency.getLagDays();
        return dependency.getPredecessorPosition() + " : " + type + lag + (dependency.isCritical() ? " [критическая]" : "");
    }

    private List<TaskDependency> copyDependencies(List<TaskDependency> source) {
        if (source == null) {
            return new ArrayList<>();
        }
        return source.stream()
                .map(item -> new TaskDependency(item.getPredecessorPosition(), item.getType(), item.getLagDays(), item.isCritical()))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private void updateDependencyCriticalUi(TaskDependency dependency) {
        boolean hasSelection = dependency != null;
        if (toggleDependencyCriticalButton != null) {
            toggleDependencyCriticalButton.setDisable(!hasSelection);
            toggleDependencyCriticalButton.setText(hasSelection && dependency.isCritical()
                    ? "Снять критичность"
                    : "Сделать критической");
        }
        if (dependencyCriticalHintLabel != null) {
            if (!hasSelection) {
                dependencyCriticalHintLabel.setText("Выберите связь из списка, чтобы отметить её как критическую.");
            } else if (dependency.isCritical()) {
                dependencyCriticalHintLabel.setText("Критическая связь будет заметно выделяться на диаграмме.");
            } else {
                dependencyCriticalHintLabel.setText("Обычная связь. При необходимости можно быстро повысить её важность.");
            }
        }
    }

    private String safeName(String value) {
        return value == null || value.isBlank() ? "Без названия" : value;
    }

    private boolean validateFields() {
        if (!ValidationUtils.validateRequiredTextField(nameField, "Название подзадачи")) {
            return false;
        }
        if (!ValidationUtils.validateRequiredTextField(companyField, "Компания-исполнитель")) {
            return false;
        }
        if (!ValidationUtils.validateDatePicker(startDatePicker, "Дата начала")) {
            return false;
        }
        if (!ValidationUtils.validateDatePicker(endDatePicker, "Дата окончания")) {
            return false;
        }
        if (!ValidationUtils.validateDateRange(startDatePicker.getValue(), endDatePicker.getValue())) {
            return false;
        }
        if (unitOfMeasurementBox.getValue() == null || unitOfMeasurementBox.getValue().isBlank()) {
            showError("Ошибка", "Выберите единицу измерения.");
            return false;
        }
        if (!ValidationUtils.validatePositiveInteger(volumeProjectField, "Проектный объем")) {
            return false;
        }
        if (!ValidationUtils.validateNonNegativeInteger(volumeActualField, "Фактический объем")) {
            return false;
        }
        return validateVolumeFieldsSilently();
    }

    private boolean validateDates() {
        LocalDate start = startDatePicker.getValue();
        LocalDate end = endDatePicker.getValue();

        if (start == null || end == null) {
            resetDateStyles();
            return true;
        }

        boolean valid = !end.isBefore(start);
        if (!valid) {
            ValidationUtils.markInvalid(startDatePicker);
            ValidationUtils.markInvalid(endDatePicker);
        } else {
            resetDateStyles();
        }
        return valid;
    }

    private boolean validateVolumeFieldsSilently() {
        String projectText = volumeProjectField.getText() == null ? "" : volumeProjectField.getText().trim();
        String actualText = volumeActualField.getText() == null ? "" : volumeActualField.getText().trim();

        if (projectText.isEmpty() || actualText.isEmpty()) {
            return true;
        }

        try {
            int project = Integer.parseInt(projectText);
            int actual = Integer.parseInt(actualText);

            if (project > 0) {
                ValidationUtils.clearValidationState(volumeProjectField);
            } else {
                ValidationUtils.markInvalid(volumeProjectField);
            }

            if (actual < 0) {
                ValidationUtils.markInvalid(volumeActualField);
                return false;
            }

            if (project > 0 && actual > project) {
                ValidationUtils.markWarning(volumeActualField);
            } else {
                ValidationUtils.clearValidationState(volumeActualField);
            }

            return project > 0;
        } catch (NumberFormatException e) {
            ValidationUtils.markInvalid(volumeProjectField);
            ValidationUtils.markInvalid(volumeActualField);
            return false;
        }
    }

    private void resetDateStyles() {
        ValidationUtils.clearValidationState(startDatePicker);
        ValidationUtils.clearValidationState(endDatePicker);
    }

    private void closeStage() {
        ((Stage) nameField.getScene().getWindow()).close();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.setTitle(title);
        alert.setHeaderText(null);
        SceneManager.applyThemeToDialog(alert);
        alert.showAndWait();
    }
}
