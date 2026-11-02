package io.angistrem.employeemanagement.util;

import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;

import java.time.LocalDate;

public final class ValidationUtils {
    private static final String INVALID_STYLE_CLASS = "invalid-field";
    private static final String WARNING_STYLE_CLASS = "warning-field";

    private ValidationUtils() {
    }

    public static boolean validateRequiredTextField(TextField field, String fieldName) {
        if (field == null || field.getText() == null || field.getText().trim().isEmpty()) {
            showError("Ошибка", "Поле '" + fieldName + "' не может быть пустым");
            if (field != null) {
                markInvalid(field);
                field.requestFocus();
            }
            return false;
        }
        clearValidationState(field);
        return true;
    }

    public static boolean validateDatePicker(DatePicker datePicker, String fieldName) {
        if (datePicker == null || datePicker.getValue() == null) {
            showError("Ошибка", "Поле '" + fieldName + "' должно быть заполнено");
            if (datePicker != null) {
                markInvalid(datePicker);
                datePicker.requestFocus();
            }
            return false;
        }
        clearValidationState(datePicker);
        return true;
    }

    public static boolean validateDateRange(LocalDate start, LocalDate end) {
        if (start != null && end != null && end.isBefore(start)) {
            showError("Ошибка", "Дата окончания не может быть раньше даты начала");
            return false;
        }
        return true;
    }

    public static boolean validatePositiveInteger(TextField field, String fieldName) {
        Integer value = parseInteger(field, fieldName);
        if (value == null) {
            return false;
        }
        if (value <= 0) {
            showError("Ошибка", "Поле '" + fieldName + "' должно быть положительным числом");
            markInvalid(field);
            field.requestFocus();
            return false;
        }
        clearValidationState(field);
        return true;
    }

    public static boolean validateNonNegativeInteger(TextField field, String fieldName) {
        Integer value = parseInteger(field, fieldName);
        if (value == null) {
            return false;
        }
        if (value < 0) {
            showError("Ошибка", "Поле '" + fieldName + "' не может быть отрицательным");
            markInvalid(field);
            field.requestFocus();
            return false;
        }
        clearValidationState(field);
        return true;
    }

    public static Integer parseInteger(TextField field, String fieldName) {
        try {
            Integer value = Integer.parseInt(field.getText().trim());
            clearValidationState(field);
            return value;
        } catch (Exception e) {
            showError("Ошибка", "Поле '" + fieldName + "' должно быть целым числом");
            if (field != null) {
                markInvalid(field);
                field.requestFocus();
            }
            return null;
        }
    }

    public static String safeTrim(TextField field) {
        return field == null || field.getText() == null ? "" : field.getText().trim();
    }

    public static void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        SceneManager.applyThemeToDialog(alert);
        alert.showAndWait();
    }

    public static void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        SceneManager.applyThemeToDialog(alert);
        alert.showAndWait();
    }

    public static void markInvalid(Node node) {
        applyStateClass(node, INVALID_STYLE_CLASS, WARNING_STYLE_CLASS);
    }

    public static void markWarning(Node node) {
        applyStateClass(node, WARNING_STYLE_CLASS, INVALID_STYLE_CLASS);
    }

    public static void clearValidationState(Node node) {
        if (node == null) {
            return;
        }
        node.getStyleClass().removeAll(INVALID_STYLE_CLASS, WARNING_STYLE_CLASS);
    }

    private static void applyStateClass(Node node, String addClass, String removeClass) {
        if (node == null) {
            return;
        }
        node.getStyleClass().remove(removeClass);
        if (!node.getStyleClass().contains(addClass)) {
            node.getStyleClass().add(addClass);
        }
    }
}
