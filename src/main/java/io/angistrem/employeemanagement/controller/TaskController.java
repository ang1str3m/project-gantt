package io.angistrem.employeemanagement.controller;

import io.angistrem.employeemanagement.model.Task;
import io.angistrem.employeemanagement.model.TaskDependency;
import io.angistrem.employeemanagement.model.TaskStatus;
import io.angistrem.employeemanagement.util.SceneManager;
import io.angistrem.employeemanagement.util.ValidationUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TaskController {
    @FXML private TextField nameField;
    @FXML private TextField companyField;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private ComboBox<String> statusBox;
    @FXML private ComboBox<String> priorityBox;
    @FXML private ComboBox<String> taskTypeBox;
    @FXML private TextField dependencyPositionsField;
    @FXML private ComboBox<String> dependencyPredecessorBox;
    @FXML private ComboBox<String> dependencyTypeBox;
    @FXML private TextField dependencyLagField;
    @FXML private ListView<TaskDependency> dependencyListView;
    @FXML private Label dependencyLegendLabel;
    @FXML private Label dependencyCriticalHintLabel;
    @FXML private Button toggleDependencyCriticalButton;
    @FXML private CheckBox criticalDependencyBox;
    @FXML private DatePicker contractAdvanceDatePicker;
    @FXML private DatePicker ksPaymentDatePicker;
    @FXML private CheckBox requireAdvanceBox;
    @FXML private CheckBox requireKsBox;

    private Task task;
    private boolean isEditMode = false;
    private List<Task> availableTasks = List.of();
    private final ObservableList<TaskDependency> editedDependencies = FXCollections.observableArrayList();
    private final Map<String, String> dependencyPositionByLabel = new LinkedHashMap<>();

    @FXML
    public void initialize() {
        statusBox.getItems().setAll(TaskStatus.PLANNED, TaskStatus.IN_PROGRESS, TaskStatus.COMPLETED, TaskStatus.OVERDUE);
        taskTypeBox.getItems().setAll("Обычная", "Веха");
        taskTypeBox.getSelectionModel().select(0);
        priorityBox.getItems().setAll("Низкий", "Средний", "Высокий", "Критический");

        dependencyTypeBox.getItems().setAll("FS", "SS", "FF", "SF");
        dependencyTypeBox.setValue("FS");
        dependencyLagField.setText("0");

        dependencyListView.setItems(editedDependencies);
        dependencyListView.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(TaskDependency item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : formatDependencyReadable(item));
            }
        });
        dependencyListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, selected) -> {
            if (selected != null) {
                selectDependencyInEditor(selected);
            }
            updateDependencyCriticalUi(selected);
        });
        if (dependencyLegendLabel != null) {
            dependencyLegendLabel.setText("FS: начало после окончания, SS: начало после начала, FF: окончание после окончания, SF: окончание после начала");
        }

        if (!isEditMode) {
            startDatePicker.setValue(LocalDate.now());
            endDatePicker.setValue(LocalDate.now().plusDays(7));
            statusBox.setValue(TaskStatus.PLANNED);
            taskTypeBox.getSelectionModel().select(0);
            priorityBox.setValue("Средний");
        }

        taskTypeBox.valueProperty().addListener((obs, oldVal, newVal) -> applyTaskTypeMode());
        endDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (isMilestoneSelected() && newVal != null) {
                startDatePicker.setValue(newVal);
            }
        });
        applyTaskTypeMode();
        startDatePicker.valueProperty().addListener((obs, oldDate, newDate) -> validateDates());
        endDatePicker.valueProperty().addListener((obs, oldDate, newDate) -> validateDates());
        updateDependencyCriticalUi(null);
    }

    public void setAvailableTasks(List<Task> availableTasks) {
        this.availableTasks = availableTasks != null ? new ArrayList<>(availableTasks) : List.of();
        refreshPredecessorOptions();
    }

    public void setTask(Task task) {
        this.task = task;
        this.isEditMode = true;
        nameField.setText(task.getName());
        companyField.setText(task.getCompany());
        startDatePicker.setValue(task.getStart());
        endDatePicker.setValue(task.getEnd());
        statusBox.setValue(task.getStatus());
        priorityBox.setValue(task.getPriority());
        taskTypeBox.getSelectionModel().select(task.isMilestone() ? 1 : 0);
        applyTaskTypeMode();
        dependencyPositionsField.setText(formatDependencies(task.getDependencies()));
        editedDependencies.setAll(copyDependencies(task.getDependencies()));
        criticalDependencyBox.setSelected(task.isCriticalDependency());
        contractAdvanceDatePicker.setValue(task.getContractAdvanceDate());
        ksPaymentDatePicker.setValue(task.getKsPaymentDate());
        requireAdvanceBox.setSelected(task.isRequireAdvanceBeforeStart());
        requireKsBox.setSelected(task.isRequireKsBeforeStart());
        refreshPredecessorOptions();
        updateDependencyCriticalUi(dependencyListView.getSelectionModel().getSelectedItem());
    }

    @FXML
    private void handleAddDependency() {
        String selectedLabel = dependencyPredecessorBox.getValue();
        if (selectedLabel == null || selectedLabel.isBlank()) {
            showError("Ошибка", "Выберите задачу-предшественника.");
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
            showError("Ошибка", "Лаг должен быть целым числом, например: 0, 2, -1.");
            return;
        }

        String type = dependencyTypeBox.getValue() == null ? "FS" : dependencyTypeBox.getValue();
        TaskDependency newDependency = new TaskDependency(predecessorPosition, type, lagDays);

        for (TaskDependency existing : editedDependencies) {
            if (predecessorPosition.equals(existing.getPredecessorPosition()) && type.equalsIgnoreCase(existing.getType())) {
                existing.setLagDays(lagDays);
                dependencyListView.refresh();
                dependencyPositionsField.setText(formatDependencies(editedDependencies));
                updateDependencyCriticalUi(existing);
                return;
            }
        }

        editedDependencies.add(newDependency);
        dependencyListView.getSelectionModel().select(newDependency);
        dependencyPositionsField.setText(formatDependencies(editedDependencies));
    }

    @FXML
    private void handleRemoveDependency() {
        TaskDependency selected = dependencyListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        editedDependencies.remove(selected);
        dependencyListView.getSelectionModel().clearSelection();
        dependencyPositionsField.setText(formatDependencies(editedDependencies));
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
        dependencyPositionsField.setText(formatDependencies(editedDependencies));
        updateDependencyCriticalUi(selected);
    }

    @FXML
    private void handleParseDependenciesFromText() {
        List<TaskDependency> parsed = parseDependencies(dependencyPositionsField.getText());
        editedDependencies.setAll(copyDependencies(parsed));
        if (!editedDependencies.isEmpty()) {
            dependencyListView.getSelectionModel().selectFirst();
        }
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
        String status = statusBox.getValue();
        String priority = priorityBox.getValue();
        String taskType = isMilestoneSelected() ? "MILESTONE" : "TASK";
        if ("MILESTONE".equals(taskType)) {
            start = end;
        }
        List<TaskDependency> dependencies = copyDependencies(editedDependencies);
        boolean critical = criticalDependencyBox.isSelected();
        LocalDate advanceDate = contractAdvanceDatePicker.getValue();
        LocalDate ksDate = ksPaymentDatePicker.getValue();
        boolean requireAdvance = requireAdvanceBox.isSelected();
        boolean requireKs = requireKsBox.isSelected();

        if (task != null && isEditMode) {
            task.setName(name);
            task.setCompany(company);
            task.setStart(start);
            task.setEnd(end);
            task.setStatus(status);
            task.setPriority(priority);
        } else {
            task = new Task(name, company, start, end, status, priority);
        }

        task.setDependencies(dependencies);
        task.setTaskType(taskType);
        task.setCriticalDependency(critical || dependencies.stream().anyMatch(TaskDependency::isCritical));
        task.setContractAdvanceDate(advanceDate);
        task.setKsPaymentDate(ksDate);
        task.setRequireAdvanceBeforeStart(requireAdvance);
        task.setRequireKsBeforeStart(requireKs);

        close();
    }

    @FXML
    private void handleExit() {
        task = null;
        close();
    }

    public Task getTask() {
        return task;
    }

    private void close() {
        ((Stage) nameField.getScene().getWindow()).close();
    }

    private boolean isMilestoneSelected() {
        return taskTypeBox != null && taskTypeBox.getSelectionModel().getSelectedIndex() == 1;
    }

    private void applyTaskTypeMode() {
        boolean milestone = isMilestoneSelected();
        startDatePicker.setDisable(milestone);
        if (milestone && endDatePicker.getValue() != null) {
            startDatePicker.setValue(endDatePicker.getValue());
        }
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

    private void resetDateStyles() {
        ValidationUtils.clearValidationState(startDatePicker);
        ValidationUtils.clearValidationState(endDatePicker);
    }

    private boolean validateFields() {
        if (!ValidationUtils.validateRequiredTextField(nameField, "Название задачи")) {
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
        if (statusBox.getValue() == null) {
            ValidationUtils.markInvalid(statusBox);
            showError("Ошибка", "Выберите статус задачи");
            return false;
        }
        if (priorityBox.getValue() == null) {
            ValidationUtils.markInvalid(priorityBox);
            showError("Ошибка", "Выберите приоритет задачи");
            return false;
        }
        if (requireAdvanceBox.isSelected() && contractAdvanceDatePicker.getValue() == null) {
            ValidationUtils.markInvalid(contractAdvanceDatePicker);
            showError("Ошибка", "Укажите дату авансирования, если включено финансовое ограничение.");
            return false;
        }
        if (requireKsBox.isSelected() && ksPaymentDatePicker.getValue() == null) {
            ValidationUtils.markInvalid(ksPaymentDatePicker);
            showError("Ошибка", "Укажите дату оплаты КС, если включено финансовое ограничение.");
            return false;
        }
        return true;
    }

    private void refreshPredecessorOptions() {
        if (dependencyPredecessorBox == null) {
            return;
        }

        String currentPosition = task != null ? task.getPosition() : null;
        dependencyPositionByLabel.clear();
        dependencyPredecessorBox.getItems().clear();

        for (Task t : availableTasks) {
            if (t == null || t.getPosition() == null) {
                continue;
            }
            if (currentPosition != null && currentPosition.equals(t.getPosition())) {
                continue;
            }
            String label = t.getPosition() + " - " + safeTaskName(t.getName());
            dependencyPositionByLabel.put(label, t.getPosition());
            dependencyPredecessorBox.getItems().add(label);

            for (var subTask : t.getObservableSubTasks()) {
                if (subTask == null || subTask.getSubPosition() == null) {
                    continue;
                }
                String subPosition = subTask.getSubPosition().trim();
                if (currentPosition != null && currentPosition.equals(subPosition)) {
                    continue;
                }
                String subLabel = subPosition + " - " + safeTaskName(subTask.getName());
                dependencyPositionByLabel.put(subLabel, subPosition);
                dependencyPredecessorBox.getItems().add(subLabel);
            }
        }

        if (!dependencyPredecessorBox.getItems().isEmpty() && dependencyPredecessorBox.getValue() == null) {
            dependencyPredecessorBox.setValue(dependencyPredecessorBox.getItems().get(0));
        }
    }

    private void selectDependencyInEditor(TaskDependency selected) {
        String selectedLabel = dependencyPositionByLabel.entrySet().stream()
                .filter(entry -> selected.getPredecessorPosition().equals(entry.getValue()))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);

        if (selectedLabel != null) {
            dependencyPredecessorBox.setValue(selectedLabel);
        }

        String type = selected.getType() == null ? "FS" : selected.getType().toUpperCase();
        if (!List.of("FS", "SS", "FF", "SF").contains(type)) {
            type = "FS";
        }
        dependencyTypeBox.setValue(type);
        dependencyLagField.setText(String.valueOf(selected.getLagDays()));
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
                dependencyCriticalHintLabel.setText("Выберите зависимость в списке, чтобы отдельно отметить её как критическую.");
            } else if (dependency.isCritical()) {
                dependencyCriticalHintLabel.setText("Эта связь считается критической и будет выделена на диаграмме.");
            } else {
                dependencyCriticalHintLabel.setText("Сейчас это обычная связь. При необходимости можно включить критичность.");
            }
        }
    }

    private String safeTaskName(String name) {
        if (name == null || name.isBlank()) {
            return "Без названия";
        }
        return name.length() > 40 ? name.substring(0, 40) + "..." : name;
    }

    private List<TaskDependency> parseDependencies(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }

        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(this::parseDependencyToken)
                .filter(d -> d.getPredecessorPosition() != null && !d.getPredecessorPosition().isBlank())
                .collect(Collectors.toList());
    }

    private TaskDependency parseDependencyToken(String token) {
        String predecessor = token;
        String type = "FS";
        int lag = 0;

        if (token.contains(":")) {
            String[] parts = token.split(":", 2);
            predecessor = parts[0].trim();
            String tail = parts[1].trim().toUpperCase();

            if (tail.length() >= 2) {
                type = tail.substring(0, 2);
                String lagPart = tail.substring(2).replace("!", "").trim();
                if (!lagPart.isBlank()) {
                    try {
                        lag = Integer.parseInt(lagPart.replace(" ", ""));
                    } catch (NumberFormatException ignored) {
                        lag = 0;
                    }
                }
            }
        }

        boolean critical = token.contains("!");

        if (!List.of("FS", "SS", "FF", "SF").contains(type)) {
            type = "FS";
        }
        return new TaskDependency(predecessor, type, lag, critical);
    }

    private List<TaskDependency> copyDependencies(List<TaskDependency> dependencies) {
        if (dependencies == null) {
            return List.of();
        }
        return dependencies.stream()
                .filter(d -> d != null && d.getPredecessorPosition() != null && !d.getPredecessorPosition().isBlank())
                .map(d -> new TaskDependency(d.getPredecessorPosition(), d.getType(), d.getLagDays(), d.isCritical()))
                .collect(Collectors.toList());
    }

    private String formatDependencies(List<TaskDependency> dependencies) {
        if (dependencies == null || dependencies.isEmpty()) {
            return "";
        }
        return dependencies.stream()
                .filter(d -> d != null && d.getPredecessorPosition() != null && !d.getPredecessorPosition().isBlank())
                .map(d -> {
                    String type = d.getType() == null ? "FS" : d.getType().toUpperCase();
                    String base = d.getPredecessorPosition() + ":" + type;
                    String critical = d.isCritical() ? "!" : "";
                    if (d.getLagDays() == 0) {
                        return base + critical;
                    }
                    return base + (d.getLagDays() > 0 ? "+" : "") + d.getLagDays() + critical;
                })
                .collect(Collectors.joining(", "));
    }

    private String formatDependencyReadable(TaskDependency dependency) {
        String type = dependency.getType() == null ? "FS" : dependency.getType().toUpperCase();
        String lag = dependency.getLagDays() == 0 ? "" : (dependency.getLagDays() > 0 ? "+" : "") + dependency.getLagDays();
        return dependency.getPredecessorPosition() + " : " + type + lag + (dependency.isCritical() ? " [критическая]" : "");
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        SceneManager.applyThemeToDialog(alert);
        alert.showAndWait();
    }

}
