package io.angistrem.employeemanagement.controller;

import io.angistrem.employeemanagement.model.BudgetItem;
import io.angistrem.employeemanagement.model.Task;
import io.angistrem.employeemanagement.util.SceneManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BudgetController {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    @FXML private Label plannedTotalLabel;
    @FXML private Label actualTotalLabel;
    @FXML private Label varianceLabel;

    @FXML private TableView<BudgetItem> budgetTable;
    @FXML private TableColumn<BudgetItem, String> taskColumn;
    @FXML private TableColumn<BudgetItem, String> nameColumn;
    @FXML private TableColumn<BudgetItem, String> categoryColumn;
    @FXML private TableColumn<BudgetItem, String> plannedColumn;
    @FXML private TableColumn<BudgetItem, String> actualColumn;
    @FXML private TableColumn<BudgetItem, String> plannedDateColumn;
    @FXML private TableColumn<BudgetItem, String> actualDateColumn;
    @FXML private TableColumn<BudgetItem, String> statusColumn;

    @FXML private ComboBox<TaskOption> taskBox;
    @FXML private TextField nameField;
    @FXML private TextField categoryField;
    @FXML private TextField plannedAmountField;
    @FXML private TextField actualAmountField;
    @FXML private DatePicker plannedDatePicker;
    @FXML private DatePicker actualDatePicker;
    @FXML private ComboBox<String> statusBox;

    private final ObservableList<BudgetItem> workingItems = FXCollections.observableArrayList();
    private final ObservableList<TaskOption> taskOptions = FXCollections.observableArrayList();
    private ObservableList<BudgetItem> sourceItems;
    private boolean changed;

    @FXML
    public void initialize() {
        taskColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTaskPosition()));
        nameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        categoryColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCategory()));
        plannedColumn.setCellValueFactory(data -> new SimpleStringProperty(formatMoney(data.getValue().getPlannedAmount())));
        actualColumn.setCellValueFactory(data -> new SimpleStringProperty(formatMoney(data.getValue().getActualAmount())));
        plannedDateColumn.setCellValueFactory(data -> new SimpleStringProperty(formatDate(data.getValue().getPlannedDate())));
        actualDateColumn.setCellValueFactory(data -> new SimpleStringProperty(formatDate(data.getValue().getActualDate())));
        statusColumn.setCellValueFactory(data -> new SimpleStringProperty(localizeStatus(data.getValue().getStatus())));

        statusBox.getItems().setAll("PLANNED", "APPROVED", "IN_PROGRESS", "PAID", "CLOSED");
        taskBox.setItems(taskOptions);
        budgetTable.setItems(workingItems);

        budgetTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            pushFormToItem(oldValue);
            populateForm(newValue);
        });
    }

    public void setContext(ObservableList<BudgetItem> items, List<Task> tasks) {
        this.sourceItems = items;
        this.workingItems.setAll(copyItems(items == null ? List.of() : items));
        this.taskOptions.setAll(buildTaskOptions(tasks));

        if (!workingItems.isEmpty()) {
            budgetTable.getSelectionModel().selectFirst();
        } else {
            clearForm();
        }
        updateSummary();
    }

    public boolean isChanged() {
        return changed;
    }

    @FXML
    private void handleAddItem() {
        BudgetItem item = new BudgetItem();
        item.setName("Новая статья бюджета");
        item.setCategory("Строительно-монтажные работы");
        item.setStatus("PLANNED");
        if (!taskOptions.isEmpty()) {
            item.setTaskPosition(taskOptions.get(0).position());
        }
        workingItems.add(item);
        budgetTable.getSelectionModel().select(item);
        nameField.requestFocus();
        nameField.selectAll();
        updateSummary();
    }

    @FXML
    private void handleDeleteItem() {
        BudgetItem selected = budgetTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        int index = budgetTable.getSelectionModel().getSelectedIndex();
        workingItems.remove(selected);
        if (workingItems.isEmpty()) {
            clearForm();
        } else {
            budgetTable.getSelectionModel().select(Math.max(0, Math.min(index, workingItems.size() - 1)));
        }
        updateSummary();
    }

    @FXML
    private void handleSave() {
        pushFormToItem(budgetTable.getSelectionModel().getSelectedItem());
        if (!validateItems()) {
            return;
        }
        if (sourceItems != null) {
            sourceItems.setAll(copyItems(workingItems));
        }
        changed = true;
        close();
    }

    @FXML
    private void handleCancel() {
        changed = false;
        close();
    }

    private void populateForm(BudgetItem item) {
        if (item == null) {
            clearForm();
            return;
        }
        taskBox.setValue(findTaskOption(item.getTaskPosition()));
        nameField.setText(item.getName());
        categoryField.setText(item.getCategory());
        plannedAmountField.setText(formatEditableNumber(item.getPlannedAmount()));
        actualAmountField.setText(formatEditableNumber(item.getActualAmount()));
        plannedDatePicker.setValue(item.getPlannedDate());
        actualDatePicker.setValue(item.getActualDate());
        statusBox.setValue(item.getStatus());
    }

    private void clearForm() {
        taskBox.setValue(taskOptions.isEmpty() ? null : taskOptions.get(0));
        nameField.clear();
        categoryField.clear();
        plannedAmountField.setText("0");
        actualAmountField.setText("0");
        plannedDatePicker.setValue(null);
        actualDatePicker.setValue(null);
        statusBox.setValue("PLANNED");
    }

    private void pushFormToItem(BudgetItem item) {
        if (item == null) {
            return;
        }
        TaskOption option = taskBox.getValue();
        item.setTaskPosition(option == null ? "" : option.position());
        item.setName(nameField.getText());
        item.setCategory(categoryField.getText());
        item.setPlannedAmount(parseAmount(plannedAmountField.getText()));
        item.setActualAmount(parseAmount(actualAmountField.getText()));
        item.setPlannedDate(plannedDatePicker.getValue());
        item.setActualDate(actualDatePicker.getValue());
        item.setStatus(statusBox.getValue());
        budgetTable.refresh();
        updateSummary();
    }

    private boolean validateItems() {
        for (BudgetItem item : workingItems) {
            if (item.getName() == null || item.getName().isBlank()) {
                showError("У каждой статьи бюджета должно быть название.");
                return false;
            }
            if (item.getPlannedAmount() <= 0.0 && item.getActualAmount() <= 0.0) {
                showError("Укажите плановую или фактическую сумму хотя бы для одной статьи.");
                return false;
            }
        }
        return true;
    }

    private void updateSummary() {
        double plannedTotal = workingItems.stream().mapToDouble(BudgetItem::getPlannedAmount).sum();
        double actualTotal = workingItems.stream().mapToDouble(BudgetItem::getActualAmount).sum();
        double variance = actualTotal - plannedTotal;

        plannedTotalLabel.setText(formatMoney(plannedTotal));
        actualTotalLabel.setText(formatMoney(actualTotal));
        varianceLabel.setText(formatMoney(variance));
        varianceLabel.setStyle(variance > 0.0001
                ? "-fx-font-weight: bold; -fx-text-fill: #ef4444;"
                : variance < -0.0001
                ? "-fx-font-weight: bold; -fx-text-fill: #10b981;"
                : "-fx-font-weight: bold; -fx-text-fill: #e2e8f0;");
    }

    private List<TaskOption> buildTaskOptions(List<Task> tasks) {
        List<TaskOption> options = new ArrayList<>();
        for (Task task : tasks) {
            if (task == null) {
                continue;
            }
            options.add(new TaskOption(task.getPosition(), task.getPosition() + " " + task.getName()));
        }
        return options;
    }

    private TaskOption findTaskOption(String position) {
        for (TaskOption option : taskOptions) {
            if (option.position().equals(position)) {
                return option;
            }
        }
        return null;
    }

    private List<BudgetItem> copyItems(List<BudgetItem> items) {
        List<BudgetItem> copies = new ArrayList<>();
        for (BudgetItem item : items) {
            if (item == null) {
                continue;
            }
            BudgetItem copy = new BudgetItem();
            copy.setId(item.getId());
            copy.setTaskPosition(item.getTaskPosition());
            copy.setName(item.getName());
            copy.setCategory(item.getCategory());
            copy.setPlannedAmount(item.getPlannedAmount());
            copy.setActualAmount(item.getActualAmount());
            copy.setPlannedDate(item.getPlannedDate());
            copy.setActualDate(item.getActualDate());
            copy.setStatus(item.getStatus());
            copies.add(copy);
        }
        return copies;
    }

    private double parseAmount(String value) {
        if (value == null || value.isBlank()) {
            return 0.0;
        }
        try {
            return Math.max(0.0, Double.parseDouble(value.replace(" ", "").replace(',', '.').trim()));
        } catch (NumberFormatException ex) {
            return 0.0;
        }
    }

    private String localizeStatus(String status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case "PLANNED" -> "План";
            case "APPROVED" -> "Согласовано";
            case "IN_PROGRESS" -> "В работе";
            case "PAID" -> "Оплачено";
            case "CLOSED" -> "Закрыто";
            default -> status;
        };
    }

    private String formatDate(LocalDate date) {
        return date == null ? "" : DATE_FORMAT.format(date);
    }

    private String formatMoney(double value) {
        return String.format(Locale.US, "%,.2f", value).replace(',', ' ') + " ₽";
    }

    private String formatEditableNumber(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.00001) {
            return String.valueOf((long) Math.rint(value));
        }
        return String.format(Locale.US, "%.2f", value);
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Бюджет проекта");
        alert.setHeaderText(null);
        alert.setContentText(message);
        SceneManager.applyThemeToDialog(alert);
        alert.showAndWait();
    }

    private void close() {
        ((Stage) budgetTable.getScene().getWindow()).close();
    }

    public record TaskOption(String position, String label) {
        @Override
        public String toString() {
            return label;
        }
    }
}
