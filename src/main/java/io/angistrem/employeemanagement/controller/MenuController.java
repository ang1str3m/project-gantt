/*
 * Copyright (c) 2026.
 */

package io.angistrem.employeemanagement.controller;

import io.angistrem.employeemanagement.model.ProjectDocument;
import io.angistrem.employeemanagement.util.AppSettings;
import io.angistrem.employeemanagement.util.DemoProjectFactory;
import io.angistrem.employeemanagement.util.ProjectFileManager;
import io.angistrem.employeemanagement.util.ProjectMetadataDialog;
import io.angistrem.employeemanagement.util.SceneManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.io.File;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class MenuController {
    private static boolean autoRestorePromptHandledThisSession = false;
    private static final DateTimeFormatter RECENT_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneId.systemDefault());

    @FXML
    private BorderPane menuRoot;
    @FXML
    private Button newProject;
    @FXML
    private Button loadProject;
    @FXML
    private Button demoProject;
    @FXML
    private Button settingBtn;
    @FXML
    private Button exit;
    @FXML
    private VBox recentProjectsBox;
    @FXML
    private Label noRecentProjectsLabel;

    @FXML
    private void initialize() {
        applyThemeVariant();
        promptAutoRestoreIfNeeded();
        refreshRecentProjects();
    }

    @FXML
    private void handleNewProject() {
        String defaultAuthor = "";
        var settings = AppSettings.get();
        if (settings != null && settings.getAuthorProject() != null) {
            defaultAuthor = settings.getAuthorProject();
        }

        var metadata = ProjectMetadataDialog.show(
                SceneManager.getPrimaryStage(),
                "Новый проект",
                "Введите данные проекта",
                "Новый проект",
                defaultAuthor
        );
        if (metadata.isEmpty()) {
            return;
        }

        ProjectDocument document = new ProjectDocument();
        document.setProjectName(metadata.get().projectName());
        document.setAuthor(metadata.get().author());
        MainController.setLoadedProjectDocument(document);
        SceneManager.switchTo("view/main-view-controller.fxml");
    }

    @FXML
    private void handleLoadProject() {
        Window window = SceneManager.getPrimaryStage();
        ProjectFileManager.LoadedProject loadedProject = ProjectFileManager.loadProjectDocumentFromFile(window);
        if (loadedProject == null) {
            refreshRecentProjects();
            return;
        }

        openLoadedProject(loadedProject);
        refreshRecentProjects();
    }

    @FXML
    private void handleDemoProject() {
        MainController.setLoadedProjectDocument(DemoProjectFactory.createDemoProject());
        SceneManager.switchTo("view/main-view-controller.fxml");
    }

    @FXML
    private void handleSettingBtn() {
        SceneManager.switchTo("view/setting-view-controller.fxml");
    }

    @FXML
    private void handleExit() {
        System.exit(0);
    }

    private void promptAutoRestoreIfNeeded() {
        if (autoRestorePromptHandledThisSession) {
            return;
        }
        autoRestorePromptHandledThisSession = true;

        if (!ProjectFileManager.hasAutoSave()) {
            return;
        }

        ProjectDocument autosavedDocument = ProjectFileManager.autoLoadDocument();
        if (autosavedDocument == null || autosavedDocument.getTasks() == null || autosavedDocument.getTasks().isEmpty()) {
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Восстановление проекта");
        alert.setHeaderText("Найдено автосохранение");
        alert.setContentText("Загрузить последний автоматически сохраненный проект?");

        ButtonType restoreButton = new ButtonType("Восстановить");
        ButtonType skipButton = new ButtonType("Пропустить");
        ButtonType discardButton = new ButtonType("Удалить", ButtonBar.ButtonData.NO);
        alert.getButtonTypes().setAll(restoreButton, skipButton, discardButton);
        SceneManager.applyThemeToDialog(alert);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == restoreButton) {
            MainController.setLoadedProjectDocument(autosavedDocument);
            Platform.runLater(() -> SceneManager.switchTo("view/main-view-controller.fxml"));
            return;
        }

        if (result.isPresent() && result.get() == discardButton) {
            ProjectFileManager.clearAutoSave();
        }
    }

    private void refreshRecentProjects() {
        if (recentProjectsBox == null) {
            return;
        }

        recentProjectsBox.getChildren().clear();
        List<File> recentFiles = ProjectFileManager.getRecentProjectFiles();
        noRecentProjectsLabel.setVisible(recentFiles.isEmpty());
        noRecentProjectsLabel.setManaged(recentFiles.isEmpty());

        for (File file : recentFiles) {
            recentProjectsBox.getChildren().add(createRecentProjectCard(file));
        }
    }

    private HBox createRecentProjectCard(File file) {
        Label icon = new Label("JSON");
        icon.getStyleClass().add("recent-project-badge");

        Label name = new Label(file.getName());
        name.getStyleClass().add("recent-project-title");
        name.setWrapText(true);

        Label path = new Label(file.getAbsolutePath());
        path.getStyleClass().add("recent-project-path");
        path.setWrapText(true);

        Label meta = new Label("Изменён: " + RECENT_FORMATTER.format(Instant.ofEpochMilli(file.lastModified())));
        meta.getStyleClass().add("recent-project-meta");

        VBox textBox = new VBox(4, name, path, meta);
        textBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        Button openButton = new Button("Открыть");
        openButton.getStyleClass().add("recent-open-button");
        openButton.setOnAction(event -> openRecentProject(file));

        HBox row = new HBox(14, icon, textBox, openButton);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("recent-project-card");
        return row;
    }

    private void openRecentProject(File file) {
        ProjectFileManager.LoadedProject loadedProject = ProjectFileManager.loadProjectDocument(file);
        if (loadedProject == null) {
            showRecentProjectError(file);
            refreshRecentProjects();
            return;
        }
        openLoadedProject(loadedProject);
        refreshRecentProjects();
    }

    private void openLoadedProject(ProjectFileManager.LoadedProject loadedProject) {
        MainController.setLoadedProjectDocument(loadedProject.document());
        SceneManager.switchTo("view/main-view-controller.fxml");
    }

    private void showRecentProjectError(File file) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Недавние проекты");
        alert.setHeaderText("Не удалось открыть проект");
        alert.setContentText(file.getAbsolutePath());
        SceneManager.applyThemeToDialog(alert);
        alert.showAndWait();
    }

    private void applyThemeVariant() {
        if (menuRoot == null) {
            return;
        }
        menuRoot.getStyleClass().removeAll("menu-light", "menu-dark");
        String theme = AppSettings.get() == null ? "light" : AppSettings.get().getTheme();
        if ("dark".equalsIgnoreCase(theme)) {
            menuRoot.getStyleClass().add("menu-dark");
        } else {
            menuRoot.getStyleClass().add("menu-light");
        }
    }
}
