package io.angistrem.employeemanagement.util;

import io.angistrem.employeemanagement.model.Settings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.util.Optional;
import java.util.function.UnaryOperator;

public final class DependencyDialog {
    private static final String[] DEPENDENCY_TYPES = {"FS", "SS", "FF", "SF"};

    private DependencyDialog() {
    }

    public record Result(String type, int lagDays, boolean critical) {
    }

    public static Optional<Result> showCreate(Window owner, String sourceLabel, String targetLabel) {
        return show(owner, "Новая зависимость", sourceLabel, targetLabel, "FS", 0, false);
    }

    public static Optional<Result> showEdit(Window owner, String sourceLabel, String targetLabel, String type, int lagDays, boolean critical) {
        String normalizedType = type == null || type.isBlank() ? "FS" : type.trim().toUpperCase();
        return show(owner, "Редактирование зависимости", sourceLabel, targetLabel, normalizedType, lagDays, critical);
    }

    private static Optional<Result> show(
            Window owner,
            String title,
            String sourceLabel,
            String targetLabel,
            String selectedType,
            int lagDays,
            boolean critical
    ) {
        boolean dark = isDarkTheme();

        Dialog<Result> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(null);
        dialog.setGraphic(null);
        if (owner != null) {
            dialog.initOwner(owner);
        }

        ButtonType applyButtonType = new ButtonType("Сохранить", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(applyButtonType, cancelButtonType);

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + color(dark, "#f8fafc", "#0f172a") + ";");

        Label routeLabel = new Label(sourceLabel + "  ->  " + targetLabel);
        routeLabel.setWrapText(true);
        routeLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + color(dark, "#93c5fd", "#1d4ed8") + ";");

        Label hintLabel = new Label("Выберите тип связи, лаг в днях и при необходимости отметьте её как критическую.");
        hintLabel.setWrapText(true);
        hintLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: " + color(dark, "#cbd5e1", "#475569") + ";");

        VBox headerBox = new VBox(6, titleLabel, routeLabel, hintLabel);
        headerBox.setPadding(new Insets(16));
        headerBox.setStyle(
                "-fx-background-color: " + color(dark, "linear-gradient(to right, #172554, #0f766e)", "linear-gradient(to right, #dbeafe, #dcfce7)") + ";"
                        + "-fx-background-radius: 14;"
                        + "-fx-border-radius: 14;"
                        + "-fx-border-color: " + color(dark, "rgba(96,165,250,0.28)", "rgba(59,130,246,0.18)") + ";"
        );

        ComboBox<String> typeBox = new ComboBox<>();
        typeBox.getItems().addAll(DEPENDENCY_TYPES);
        typeBox.setValue(selectedType);
        typeBox.setMaxWidth(Double.MAX_VALUE);

        TextField lagField = new TextField(String.valueOf(lagDays));
        lagField.setPromptText("0");
        lagField.setTextFormatter(createLagFormatter());

        CheckBox criticalBox = new CheckBox("Критическая зависимость");
        criticalBox.setSelected(critical);
        criticalBox.setStyle("-fx-text-fill: " + color(dark, "#e5e7eb", "#1f2937") + ";");

        Label typeLabel = formLabel("Тип зависимости", dark);
        Label lagLabel = formLabel("Лаг, дней", dark);

        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(10);
        form.add(typeLabel, 0, 0);
        form.add(typeBox, 1, 0);
        form.add(lagLabel, 0, 1);
        form.add(lagField, 1, 1);
        form.add(criticalBox, 1, 2);
        GridPane.setHgrow(typeBox, Priority.ALWAYS);
        GridPane.setHgrow(lagField, Priority.ALWAYS);

        VBox legendBox = new VBox(8,
                legendRow("FS", "Окончание -> начало", dark),
                legendRow("SS", "Начало -> начало", dark),
                legendRow("FF", "Окончание -> окончание", dark),
                legendRow("SF", "Начало -> окончание", dark)
        );
        legendBox.setPadding(new Insets(14));
        legendBox.setStyle(
                "-fx-background-color: " + color(dark, "rgba(15,23,42,0.52)", "#ffffff") + ";"
                        + "-fx-background-radius: 12;"
                        + "-fx-border-radius: 12;"
                        + "-fx-border-color: " + color(dark, "rgba(100,116,139,0.28)", "rgba(148,163,184,0.22)") + ";"
        );

        Label noteLabel = new Label("Подсказка: отрицательный лаг позволяет целевой задаче начаться раньше, чем обычно.");
        noteLabel.setWrapText(true);
        noteLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " + color(dark, "#94a3b8", "#64748b") + ";");

        VBox content = new VBox(14, headerBox, form, new Separator(), legendBox, noteLabel);
        content.setPadding(new Insets(8, 10, 4, 10));
        content.setPrefWidth(440);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefWidth(490);
        SceneManager.applyThemeToDialog(dialog);

        dialog.setResultConverter(buttonType -> {
            if (buttonType != applyButtonType) {
                return null;
            }
            String lagText = lagField.getText() == null || lagField.getText().isBlank() || "-".equals(lagField.getText().trim())
                    ? "0"
                    : lagField.getText().trim();
            return new Result(typeBox.getValue(), Integer.parseInt(lagText), criticalBox.isSelected());
        });

        return dialog.showAndWait();
    }

    private static Label formLabel(String text, boolean dark) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + color(dark, "#dbe4f0", "#334155") + ";");
        return label;
    }

    private static HBox legendRow(String code, String description, boolean dark) {
        Label badge = new Label(code);
        badge.setMinWidth(40);
        badge.setAlignment(Pos.CENTER);
        badge.setStyle(
                "-fx-background-color: " + color(dark, "#1d4ed8", "#dbeafe") + ";"
                        + "-fx-text-fill: " + color(dark, "#ffffff", "#1d4ed8") + ";"
                        + "-fx-font-size: 11px;"
                        + "-fx-font-weight: bold;"
                        + "-fx-background-radius: 999;"
                        + "-fx-padding: 4 10 4 10;"
        );

        Label text = new Label(description);
        text.setWrapText(true);
        text.setStyle("-fx-font-size: 12px; -fx-text-fill: " + color(dark, "#e2e8f0", "#334155") + ";");

        HBox row = new HBox(10, badge, text);
        row.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(text, Priority.ALWAYS);
        return row;
    }

    private static TextFormatter<String> createLagFormatter() {
        UnaryOperator<TextFormatter.Change> filter = change -> {
            String newText = change.getControlNewText();
            return newText.matches("-?\\d{0,4}") ? change : null;
        };
        return new TextFormatter<>(filter);
    }

    private static boolean isDarkTheme() {
        Settings settings = AppSettings.get();
        if (settings == null || settings.getTheme() == null) {
            return false;
        }
        return "dark".equalsIgnoreCase(settings.getTheme().trim());
    }

    private static String color(boolean dark, String darkColor, String lightColor) {
        return dark ? darkColor : lightColor;
    }
}
