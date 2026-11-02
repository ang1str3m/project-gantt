package io.angistrem.employeemanagement.controller.menu;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.angistrem.employeemanagement.controller.MainController;
import io.angistrem.employeemanagement.model.ProjectDocument;
import io.angistrem.employeemanagement.model.Settings;
import io.angistrem.employeemanagement.model.Task;
import io.angistrem.employeemanagement.util.AppSettings;
import io.angistrem.employeemanagement.util.CsvExporter;
import io.angistrem.employeemanagement.util.PdfExporter;
import io.angistrem.employeemanagement.util.ProjectFileManager;
import io.angistrem.employeemanagement.util.ProjectMetadataDialog;
import io.angistrem.employeemanagement.util.ReportPrinter;
import io.angistrem.employeemanagement.util.SceneManager;
import io.angistrem.employeemanagement.util.UserGuideDialog;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Optional;

public class MainMenuBar {
    private MainController mainController;
    private File currentProjectFile;
    private boolean hasUnsavedChanges = false;

    @FXML private Label viewPositionColumnCheck;
    @FXML private Label viewNameColumnCheck;
    @FXML private Label viewCompanyColumnCheck;
    @FXML private Label viewStartColumnCheck;
    @FXML private Label viewEndColumnCheck;
    @FXML private Label viewUnitColumnCheck;
    @FXML private Label viewVolProjectColumnCheck;
    @FXML private Label viewVolActualColumnCheck;
    @FXML private Label viewPercentColumnCheck;
    @FXML private Label viewStatusColumnCheck;
    @FXML private Label viewPriorityColumnCheck;
    @FXML private Label viewDependenciesCheck;

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
        syncViewMenuState();
        updateStageTitle();
    }

    @FXML
    private void handleTogglePositionColumn(ActionEvent event) { toggleColumn("position"); }
    @FXML
    private void handleTogglePositionColumnClick(MouseEvent event) { toggleColumn("position"); }
    @FXML
    private void handleToggleNameColumn(ActionEvent event) { toggleColumn("name"); }
    @FXML
    private void handleToggleNameColumnClick(MouseEvent event) { toggleColumn("name"); }
    @FXML
    private void handleToggleCompanyColumn(ActionEvent event) { toggleColumn("company"); }
    @FXML
    private void handleToggleCompanyColumnClick(MouseEvent event) { toggleColumn("company"); }
    @FXML
    private void handleToggleStartColumn(ActionEvent event) { toggleColumn("start"); }
    @FXML
    private void handleToggleStartColumnClick(MouseEvent event) { toggleColumn("start"); }
    @FXML
    private void handleToggleEndColumn(ActionEvent event) { toggleColumn("end"); }
    @FXML
    private void handleToggleEndColumnClick(MouseEvent event) { toggleColumn("end"); }
    @FXML
    private void handleToggleUnitColumn(ActionEvent event) { toggleColumn("unit"); }
    @FXML
    private void handleToggleUnitColumnClick(MouseEvent event) { toggleColumn("unit"); }
    @FXML
    private void handleToggleVolProjectColumn(ActionEvent event) { toggleColumn("volProject"); }
    @FXML
    private void handleToggleVolProjectColumnClick(MouseEvent event) { toggleColumn("volProject"); }
    @FXML
    private void handleToggleVolActualColumn(ActionEvent event) { toggleColumn("volActual"); }
    @FXML
    private void handleToggleVolActualColumnClick(MouseEvent event) { toggleColumn("volActual"); }
    @FXML
    private void handleTogglePercentColumn(ActionEvent event) { toggleColumn("percent"); }
    @FXML
    private void handleTogglePercentColumnClick(MouseEvent event) { toggleColumn("percent"); }
    @FXML
    private void handleToggleStatusColumn(ActionEvent event) { toggleColumn("status"); }
    @FXML
    private void handleToggleStatusColumnClick(MouseEvent event) { toggleColumn("status"); }
    @FXML
    private void handleTogglePriorityColumn(ActionEvent event) { toggleColumn("priority"); }
    @FXML
    private void handleTogglePriorityColumnClick(MouseEvent event) { toggleColumn("priority"); }
    @FXML
    private void handleToggleDependencies(ActionEvent event) {
        if (mainController == null) {
            return;
        }
        mainController.setShowDependencies(!mainController.isShowDependencies());
        syncViewMenuState();
    }
    @FXML
    private void handleToggleDependenciesClick(MouseEvent event) {
        if (mainController == null) {
            return;
        }
        mainController.setShowDependencies(!mainController.isShowDependencies());
        syncViewMenuState();
    }

    public void markAsSaved() {
        hasUnsavedChanges = false;
        updateStageTitle();
    }

    public void markAsUnsaved() {
        hasUnsavedChanges = true;
        updateStageTitle();
    }

    @FXML
    private void handleNewProject() {
        flushAutosaveIfPossible();
        if (!checkUnsavedChanges()) {
            return;
        }

        var metadata = ProjectMetadataDialog.show(
                getWindow(),
                "Новый проект",
                "Введите данные проекта",
                "Новый проект",
                ""
        );
        if (metadata.isEmpty()) {
            return;
        }

        ProjectDocument document = new ProjectDocument();
        document.setProjectName(metadata.get().projectName());
        document.setAuthor(metadata.get().author());
        Task.resetCounter();
        if (mainController != null) {
            mainController.clearUndoHistory();
        }
        MainController.setLoadedProjectDocument(document);

        currentProjectFile = null;
        hasUnsavedChanges = false;
        SceneManager.switchTo("view/main-view-controller.fxml");
    }

    @FXML
    private void handleLoadProject() {
        flushAutosaveIfPossible();
        if (mainController == null || !checkUnsavedChanges()) {
            return;
        }

        Window window = getWindow();
        ProjectFileManager.LoadedProject loadedProject = ProjectFileManager.loadProjectDocumentFromFile(window);
        if (loadedProject == null || loadedProject.document() == null) {
            return;
        }

        ProjectDocument document = loadedProject.document();
        mainController.getTasks().setAll(document.getTasks() == null ? java.util.List.of() : document.getTasks());
        mainController.setProjectMetadata(document.getProjectName(), document.getAuthor(), false);
        mainController.clearUndoHistory();
        mainController.getTreeTableView().refresh();
        if (mainController.getGanttChart() != null) {
            mainController.getGanttChart().refresh();
        }

        currentProjectFile = loadedProject.file();
        ProjectFileManager.registerRecentProject(currentProjectFile);
        markAsSaved();
    }

    @FXML
    private void handleSaveProject() {
        saveProjectInternal(true);
    }

    @FXML
    private void handleSaveAsProject() {
        saveAsProjectInternal(true);
    }

    @FXML
    private void handleEditProjectDetails() {
        if (mainController == null) {
            return;
        }

        var metadata = ProjectMetadataDialog.show(
                getWindow(),
                "Данные проекта",
                "Редактирование проекта",
                mainController.getProjectName(),
                mainController.getProjectAuthor()
        );
        if (metadata.isEmpty()) {
            return;
        }

        String oldName = mainController.getProjectName();
        String oldAuthor = mainController.getProjectAuthor();
        String newName = metadata.get().projectName();
        String newAuthor = metadata.get().author();

        if (!oldName.equals(newName) || !oldAuthor.equals(newAuthor)) {
            mainController.setProjectMetadata(newName, newAuthor, true);
            updateStageTitle();
        }
    }

    @FXML
    private void handleManageResources() {
        if (mainController == null) {
            return;
        }
        mainController.openResourceDirectory();
    }

    @FXML
    private void handleProjectBudget() {
        if (mainController == null) {
            return;
        }
        mainController.openProjectBudget();
    }

    @FXML
    private void handlePaymentCalendar() {
        if (mainController == null) {
            return;
        }
        mainController.openPaymentCalendar();
    }

    @FXML
    private void handleResourceLoad() {
        if (mainController == null) {
            return;
        }
        mainController.openResourceLoadAnalysis();
    }

    @FXML
    private void handleStageOverview() {
        if (mainController == null) {
            return;
        }
        mainController.openStageOverview();
    }

    @FXML
    private void handleCloseProject() {
        flushAutosaveIfPossible();
        if (!checkUnsavedChanges()) {
            return;
        }

        if (mainController != null) {
            mainController.getTasks().clear();
            mainController.clearUndoHistory();
            mainController.getTreeTableView().refresh();
            if (mainController.getGanttChart() != null) {
                mainController.getGanttChart().refresh();
            }
        }

        currentProjectFile = null;
        hasUnsavedChanges = false;
        SceneManager.switchTo("view/menu-view-controller.fxml");
    }

    @FXML
    private void handleExit() {
        flushAutosaveIfPossible();
        if (checkUnsavedChanges()) {
            System.exit(0);
        }
    }

    @FXML
    private void handlePrintFullProject() {
        if (mainController == null) {
            return;
        }
        ReportPrinter.printFullProject(mainController, getWindow());
    }

    @FXML
    private void handleExportPdf() {
        if (mainController == null) {
            return;
        }
        PdfExporter.exportCurrentViewToPdf(mainController, getWindow());
    }

    @FXML
    private void handleExportCsv() {
        if (mainController == null) {
            return;
        }
        boolean ok = CsvExporter.exportToCsv(mainController.getTasks(), getWindow());
        if (ok) {
            showInfo("CSV", "CSV успешно сохранён.");
        } else {
            showError("CSV", "Не удалось экспортировать CSV.");
        }
    }

    private boolean saveProjectInternal(boolean showSuccessMessage) {
        if (mainController == null) {
            return false;
        }
        if (currentProjectFile == null) {
            return saveAsProjectInternal(showSuccessMessage);
        }

        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(currentProjectFile, mainController.toProjectDocument());
            ProjectFileManager.registerRecentProject(currentProjectFile);
            ProjectFileManager.clearAutoSave();
            markAsSaved();
            if (showSuccessMessage) {
                showInfo("Сохранение", "Проект успешно сохранён.");
            }
            return true;
        } catch (IOException e) {
            showError("Ошибка", "Не удалось сохранить проект: " + e.getMessage());
            return false;
        }
    }

    private boolean saveAsProjectInternal(boolean showSuccessMessage) {
        if (mainController == null) {
            return false;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Сохранить проект как");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON files (*.json)", "*.json"));
        fileChooser.setInitialFileName("project_" + LocalDate.now() + ".json");

        Settings settings = AppSettings.get();
        if (settings != null && settings.getDefaultProjectDirectory() != null && !settings.getDefaultProjectDirectory().isBlank()) {
            File initialDir = new File(settings.getDefaultProjectDirectory());
            if (initialDir.exists() && initialDir.isDirectory()) {
                fileChooser.setInitialDirectory(initialDir);
            }
        }

        File file = fileChooser.showSaveDialog(getWindow());
        if (file == null) {
            return false;
        }

        currentProjectFile = file;
        ProjectFileManager.registerRecentProject(currentProjectFile);
        return saveProjectInternal(showSuccessMessage);
    }

    private Window getWindow() {
        return mainController != null && mainController.getTreeTableView() != null
                ? mainController.getTreeTableView().getScene().getWindow()
                : null;
    }

    private void flushAutosaveIfPossible() {
        if (mainController != null) {
            mainController.flushAutosaveNow();
        }
    }

    private void updateStageTitle() {
        if (mainController == null || mainController.getTreeTableView() == null || mainController.getTreeTableView().getScene() == null) {
            return;
        }

        Stage stage = (Stage) mainController.getTreeTableView().getScene().getWindow();
        String baseTitle = currentProjectFile != null ? currentProjectFile.getName() : mainController.getProjectName();
        if (hasUnsavedChanges) {
            baseTitle += " *";
        }
        stage.setTitle(baseTitle + " - Gantrem");
    }

    private boolean checkUnsavedChanges() {
        if (!hasUnsavedChanges) {
            return true;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Несохранённые изменения");
        alert.setHeaderText("Есть несохранённые изменения");
        alert.setContentText("Что вы хотите сделать?");

        ButtonType saveButton = new ButtonType("Сохранить");
        ButtonType dontSaveButton = new ButtonType("Не сохранять");
        ButtonType cancelButton = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(saveButton, dontSaveButton, cancelButton);

        SceneManager.applyThemeToDialog(alert);
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == saveButton) {
            return saveProjectInternal(false);
        }
        return result.isPresent() && result.get() == dontSaveButton;
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        SceneManager.applyThemeToDialog(alert);
        alert.showAndWait();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        SceneManager.applyThemeToDialog(alert);
        alert.showAndWait();
    }

    private void toggleColumn(String columnKey) {
        if (mainController == null) {
            return;
        }
        boolean nextVisible = !mainController.isColumnVisible(columnKey);
        mainController.setColumnVisible(columnKey, nextVisible);
        syncViewMenuState();
    }

    @FXML
    private void handleHelp() {
        if (mainController != null) {
            mainController.showHelpDialog();
        }
    }

    @FXML
    private void handleUserGuide() {
        UserGuideDialog.show(getWindow());
    }

    private void syncViewMenuState() {
        if (mainController == null) {
            return;
        }
        setCheckVisible(viewPositionColumnCheck, mainController.isColumnVisible("position"));
        setCheckVisible(viewNameColumnCheck, mainController.isColumnVisible("name"));
        setCheckVisible(viewCompanyColumnCheck, mainController.isColumnVisible("company"));
        setCheckVisible(viewStartColumnCheck, mainController.isColumnVisible("start"));
        setCheckVisible(viewEndColumnCheck, mainController.isColumnVisible("end"));
        setCheckVisible(viewUnitColumnCheck, mainController.isColumnVisible("unit"));
        setCheckVisible(viewVolProjectColumnCheck, mainController.isColumnVisible("volProject"));
        setCheckVisible(viewVolActualColumnCheck, mainController.isColumnVisible("volActual"));
        setCheckVisible(viewPercentColumnCheck, mainController.isColumnVisible("percent"));
        setCheckVisible(viewStatusColumnCheck, mainController.isColumnVisible("status"));
        setCheckVisible(viewPriorityColumnCheck, mainController.isColumnVisible("priority"));
        setCheckVisible(viewDependenciesCheck, mainController.isShowDependencies());
    }

    private void setCheckVisible(Label label, boolean visible) {
        if (label == null) {
            return;
        }
        label.setText(visible ? "✓" : "");
    }

    public void newProjectAction() {
        handleNewProject();
    }

    public void loadProjectAction() {
        handleLoadProject();
    }

    public void saveProjectAction() {
        handleSaveProject();
    }

    public void saveAsProjectAction() {
        handleSaveAsProject();
    }

    public void printProjectAction() {
        handlePrintFullProject();
    }

    public void exportPdfAction() {
        handleExportPdf();
    }

    public void exportCsvAction() {
        handleExportCsv();
    }
}
