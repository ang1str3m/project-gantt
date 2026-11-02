package io.angistrem.employeemanagement.util;

import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.util.Optional;

public final class ProjectMetadataDialog {

    public record Metadata(String projectName, String author) {
    }

    private ProjectMetadataDialog() {
    }

    public static Optional<Metadata> show(
            Window owner,
            String title,
            String header,
            String initialProjectName,
            String initialAuthor
    ) {
        Dialog<Metadata> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(null);
        dialog.setGraphic(null);
        if (owner != null) {
            dialog.initOwner(owner);
        }

        TextField projectField = new TextField(defaultValue(initialProjectName, "Новый проект"));
        TextField authorField = new TextField(defaultValue(initialAuthor, ""));
        projectField.setPromptText("Название проекта");
        authorField.setPromptText("Автор");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("dialog-title");

        Label subtitleLabel = new Label(header);
        subtitleLabel.setWrapText(true);
        subtitleLabel.getStyleClass().add("dialog-subtitle");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);
        grid.setPadding(new Insets(16));
        grid.getStyleClass().add("dialog-section");
        grid.add(new Label("Проект:"), 0, 0);
        grid.add(projectField, 1, 0);
        grid.add(new Label("Автор:"), 0, 1);
        grid.add(authorField, 1, 1);
        GridPane.setHgrow(projectField, Priority.ALWAYS);
        GridPane.setHgrow(authorField, Priority.ALWAYS);

        VBox content = new VBox(12, titleLabel, subtitleLabel, grid);
        content.setPadding(new Insets(8, 10, 4, 10));
        content.setPrefWidth(430);

        ButtonType saveButton = new ButtonType("Сохранить", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().setAll(saveButton, cancelButton);
        SceneManager.applyThemeToDialog(dialog);
        dialog.setResultConverter(button -> {
            if (button != saveButton) {
                return null;
            }
            String projectName = normalizeProjectName(projectField.getText());
            String author = normalizeAuthor(authorField.getText());
            return new Metadata(projectName, author);
        });

        return dialog.showAndWait();
    }

    private static String normalizeProjectName(String value) {
        if (value == null || value.isBlank()) {
            return "Новый проект";
        }
        return value.trim();
    }

    private static String normalizeAuthor(String value) {
        return value == null ? "" : value.trim();
    }

    private static String defaultValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
