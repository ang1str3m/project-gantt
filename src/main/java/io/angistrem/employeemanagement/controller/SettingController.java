package io.angistrem.employeemanagement.controller;

import io.angistrem.employeemanagement.model.Settings;
import io.angistrem.employeemanagement.util.AppSettings;
import io.angistrem.employeemanagement.util.AppInfo;
import io.angistrem.employeemanagement.util.AppPaths;
import io.angistrem.employeemanagement.util.HotkeyManager;
import io.angistrem.employeemanagement.util.SceneManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.QuadCurveTo;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.net.URL;

public class SettingController {

    @FXML private AnchorPane contentArea;
    @FXML private javafx.scene.control.ToggleButton general;
    @FXML private javafx.scene.control.ToggleButton save;
    @FXML private javafx.scene.control.ToggleButton shortcuts;
    @FXML private javafx.scene.control.ToggleButton connectionToDB;
    @FXML private javafx.scene.control.ToggleButton infoApp;

    private Settings currentSettings;

    @FXML
    public void initialize() {
        currentSettings = AppSettings.get();
        loadPage("general");
        if (general != null) {
            general.setSelected(true);
        }
    }

    @FXML private void handleGeneral() { loadPage("general"); }
    @FXML private void handleSaveAndLoad() { loadPage("save"); }
    @FXML private void handleShortcuts() { loadPage("shortcuts"); }
    @FXML private void handleConnectionToDB() { loadPage("connection-to-db"); }
    @FXML private void handleInfo() { loadPage("info"); }
    @FXML private void handleBack() { SceneManager.switchTo("view/menu-view-controller.fxml"); }

    private void loadPage(String pageName) {
        try {
            URL url = getClass().getResource("/io/angistrem/employeemanagement/settings-content/" + pageName + "-settings.fxml");
            if (url == null) {
                throw new IllegalStateException("Не найден файл настроек: " + pageName + "-settings.fxml");
            }

            FXMLLoader loader = new FXMLLoader(url);
            Node page = loader.load();

            contentArea.getChildren().setAll(page);
            AnchorPane.setTopAnchor(page, 0.0);
            AnchorPane.setRightAnchor(page, 0.0);
            AnchorPane.setBottomAnchor(page, 0.0);
            AnchorPane.setLeftAnchor(page, 0.0);

            if ("general".equals(pageName)) {
                bindGeneralPage(page);
            } else if ("save".equals(pageName)) {
                bindSavePage(page);
            } else if ("shortcuts".equals(pageName)) {
                bindShortcutsPage(page);
            }
        } catch (IOException e) {
            throw new RuntimeException("Не удалось загрузить страницу настроек: " + pageName, e);
        }
    }

    private void bindGeneralPage(Node page) {
        ComboBox<String> themeBox = lookup(page, "#themeBox", ComboBox.class);
        CheckBox autoLoginCheckBox = lookup(page, "#autoLoginCheckBox", CheckBox.class);
        ColorPicker taskColorPicker = lookup(page, "#taskColorPicker", ColorPicker.class);
        ColorPicker criticalTaskColorPicker = lookup(page, "#criticalTaskColorPicker", ColorPicker.class);
        ColorPicker subTaskColorPicker = lookup(page, "#subTaskColorPicker", ColorPicker.class);
        ColorPicker priorityHighColorPicker = lookup(page, "#priorityHighColorPicker", ColorPicker.class);
        ColorPicker priorityMediumColorPicker = lookup(page, "#priorityMediumColorPicker", ColorPicker.class);
        ColorPicker priorityLowColorPicker = lookup(page, "#priorityLowColorPicker", ColorPicker.class);
        ColorPicker dependencyFsColorPicker = lookup(page, "#dependencyFsColorPicker", ColorPicker.class);
        ColorPicker dependencySsColorPicker = lookup(page, "#dependencySsColorPicker", ColorPicker.class);
        ColorPicker dependencyFfColorPicker = lookup(page, "#dependencyFfColorPicker", ColorPicker.class);
        ColorPicker dependencySfColorPicker = lookup(page, "#dependencySfColorPicker", ColorPicker.class);
        ComboBox<String> dependencyLineStyleBox = lookup(page, "#dependencyLineStyleBox", ComboBox.class);
        ComboBox<String> textStyleBox = lookup(page, "#textStyleBox", ComboBox.class);
        ComboBox<String> fontFamilyBox = lookup(page, "#fontFamilyBox", ComboBox.class);
        Pane dependencyLinePreviewPane = lookup(page, "#dependencyLinePreviewPane", Pane.class);
        Button resetGeneralColorsBtn = lookup(page, "#resetGeneralColorsBtn", Button.class);
        Button applyButton = lookup(page, "#applyGeneralBtn", Button.class);
        Button cancelButton = lookup(page, "#cancelGeneralBtn", Button.class);

        themeBox.getItems().setAll("light", "dark");
        themeBox.setValue(blankToDefault(currentSettings.getTheme(), "light"));
        autoLoginCheckBox.setSelected(currentSettings.isAutoLogin());
        taskColorPicker.setValue(parseColor(currentSettings.getTaskColor(), "#2563EB"));
        criticalTaskColorPicker.setValue(parseColor(currentSettings.getCriticalTaskColor(), "#DC2626"));
        subTaskColorPicker.setValue(parseColor(currentSettings.getSubTaskColor(), "#16A34A"));
        priorityHighColorPicker.setValue(parseColor(currentSettings.getPriorityHighColor(), "#DC2626"));
        priorityMediumColorPicker.setValue(parseColor(currentSettings.getPriorityMediumColor(), "#2563EB"));
        priorityLowColorPicker.setValue(parseColor(currentSettings.getPriorityLowColor(), "#0F766E"));
        dependencyFsColorPicker.setValue(parseColor(currentSettings.getDependencyFsColor(), "#7C848F"));
        dependencySsColorPicker.setValue(parseColor(currentSettings.getDependencySsColor(), "#2563EB"));
        dependencyFfColorPicker.setValue(parseColor(currentSettings.getDependencyFfColor(), "#0F766E"));
        dependencySfColorPicker.setValue(parseColor(currentSettings.getDependencySfColor(), "#B45309"));
        dependencyLineStyleBox.getItems().setAll("classic", "rounded", "metro", "flow");
        dependencyLineStyleBox.setValue(blankToDefault(currentSettings.getDependencyLineStyle(), "rounded"));
        textStyleBox.getItems().setAll("compact", "standard", "presentation");
        textStyleBox.setValue(blankToDefault(currentSettings.getTextStyle(), "standard"));
        fontFamilyBox.getItems().setAll(Font.getFamilies());
        fontFamilyBox.setValue(blankToDefault(currentSettings.getFontFamily(), "Segoe UI"));
        renderDependencyLinePreview(dependencyLinePreviewPane, dependencyLineStyleBox.getValue());
        dependencyLineStyleBox.valueProperty().addListener((obs, oldValue, newValue) ->
                renderDependencyLinePreview(dependencyLinePreviewPane, blankToDefault(newValue, "rounded")));

        resetGeneralColorsBtn.setOnAction(e -> {
            taskColorPicker.setValue(parseColor("#2563EB", "#2563EB"));
            criticalTaskColorPicker.setValue(parseColor("#DC2626", "#DC2626"));
            subTaskColorPicker.setValue(parseColor("#16A34A", "#16A34A"));
            priorityHighColorPicker.setValue(parseColor("#DC2626", "#DC2626"));
            priorityMediumColorPicker.setValue(parseColor("#2563EB", "#2563EB"));
            priorityLowColorPicker.setValue(parseColor("#0F766E", "#0F766E"));
            dependencyFsColorPicker.setValue(parseColor("#7C848F", "#7C848F"));
            dependencySsColorPicker.setValue(parseColor("#2563EB", "#2563EB"));
            dependencyFfColorPicker.setValue(parseColor("#0F766E", "#0F766E"));
            dependencySfColorPicker.setValue(parseColor("#B45309", "#B45309"));
            dependencyLineStyleBox.setValue("rounded");
            textStyleBox.setValue("standard");
            fontFamilyBox.setValue("Segoe UI");
            renderDependencyLinePreview(dependencyLinePreviewPane, "rounded");
        });

        applyButton.setOnAction(event -> {
            currentSettings.setTheme(themeBox.getValue() == null ? "light" : themeBox.getValue());
            currentSettings.setAutoLogin(autoLoginCheckBox.isSelected());
            currentSettings.setAutoLoginVersion(autoLoginCheckBox.isSelected() ? AppInfo.currentVersion() : "");
            currentSettings.setAutoLoginMachineId(autoLoginCheckBox.isSelected() ? AppPaths.getCurrentMachineId() : "");
            currentSettings.setTaskColor(toHex(taskColorPicker.getValue()));
            currentSettings.setCriticalTaskColor(toHex(criticalTaskColorPicker.getValue()));
            currentSettings.setSubTaskColor(toHex(subTaskColorPicker.getValue()));
            currentSettings.setPriorityHighColor(toHex(priorityHighColorPicker.getValue()));
            currentSettings.setPriorityMediumColor(toHex(priorityMediumColorPicker.getValue()));
            currentSettings.setPriorityLowColor(toHex(priorityLowColorPicker.getValue()));
            currentSettings.setDependencyFsColor(toHex(dependencyFsColorPicker.getValue()));
            currentSettings.setDependencySsColor(toHex(dependencySsColorPicker.getValue()));
            currentSettings.setDependencyFfColor(toHex(dependencyFfColorPicker.getValue()));
            currentSettings.setDependencySfColor(toHex(dependencySfColorPicker.getValue()));
            currentSettings.setDependencyLineStyle(dependencyLineStyleBox.getValue());
            currentSettings.setTextStyle(textStyleBox.getValue());
            currentSettings.setFontFamily(fontFamilyBox.getValue());
            AppSettings.save(currentSettings);
            SceneManager.applyThemeToCurrentScene();
            showInfo("Настройки", "Общие настройки успешно сохранены.");
        });

        cancelButton.setOnAction(event -> {
            AppSettings.reload();
            currentSettings = AppSettings.get();
            themeBox.setValue(blankToDefault(currentSettings.getTheme(), "light"));
            autoLoginCheckBox.setSelected(currentSettings.isAutoLogin());
            taskColorPicker.setValue(parseColor(currentSettings.getTaskColor(), "#2563EB"));
            criticalTaskColorPicker.setValue(parseColor(currentSettings.getCriticalTaskColor(), "#DC2626"));
            subTaskColorPicker.setValue(parseColor(currentSettings.getSubTaskColor(), "#16A34A"));
            priorityHighColorPicker.setValue(parseColor(currentSettings.getPriorityHighColor(), "#DC2626"));
            priorityMediumColorPicker.setValue(parseColor(currentSettings.getPriorityMediumColor(), "#2563EB"));
            priorityLowColorPicker.setValue(parseColor(currentSettings.getPriorityLowColor(), "#0F766E"));
            dependencyFsColorPicker.setValue(parseColor(currentSettings.getDependencyFsColor(), "#7C848F"));
            dependencySsColorPicker.setValue(parseColor(currentSettings.getDependencySsColor(), "#2563EB"));
            dependencyFfColorPicker.setValue(parseColor(currentSettings.getDependencyFfColor(), "#0F766E"));
            dependencySfColorPicker.setValue(parseColor(currentSettings.getDependencySfColor(), "#B45309"));
            dependencyLineStyleBox.setValue(blankToDefault(currentSettings.getDependencyLineStyle(), "rounded"));
            textStyleBox.setValue(blankToDefault(currentSettings.getTextStyle(), "standard"));
            fontFamilyBox.setValue(blankToDefault(currentSettings.getFontFamily(), "Segoe UI"));
            renderDependencyLinePreview(dependencyLinePreviewPane, dependencyLineStyleBox.getValue());
            SceneManager.applyThemeToCurrentScene();
        });
    }

    private void bindSavePage(Node page) {
        TextField projectDirField = lookup(page, "#defaultProjectDirField", TextField.class);
        TextField pdfDirField = lookup(page, "#defaultPdfDirField", TextField.class);
        TextField csvDirField = lookup(page, "#defaultCsvDirField", TextField.class);
        CheckBox keepDialogsCheckBox = lookup(page, "#keepDialogsInLastDirCheckBox", CheckBox.class);
        Button browseProjectBtn = lookup(page, "#browseProjectDirBtn", Button.class);
        Button browsePdfBtn = lookup(page, "#browsePdfDirBtn", Button.class);
        Button browseCsvBtn = lookup(page, "#browseCsvDirBtn", Button.class);
        Button resetBtn = lookup(page, "#resetSavePathsBtn", Button.class);
        Button cancelBtn = lookup(page, "#cancelSaveBtn", Button.class);
        Button applyBtn = lookup(page, "#applySaveBtn", Button.class);

        projectDirField.setText(blankToDefault(currentSettings.getDefaultProjectDirectory(), ""));
        pdfDirField.setText(blankToDefault(currentSettings.getDefaultPdfDirectory(), ""));
        csvDirField.setText(blankToDefault(currentSettings.getDefaultCsvDirectory(), ""));
        keepDialogsCheckBox.setSelected(currentSettings.isKeepDialogsInLastDir());

        browseProjectBtn.setOnAction(e -> {
            String path = chooseDirectory("Выберите папку проектов", projectDirField.getText());
            if (path != null) {
                projectDirField.setText(path);
            }
        });
        browsePdfBtn.setOnAction(e -> {
            String path = chooseDirectory("Выберите папку PDF", pdfDirField.getText());
            if (path != null) {
                pdfDirField.setText(path);
            }
        });
        browseCsvBtn.setOnAction(e -> {
            String path = chooseDirectory("Выберите папку CSV", csvDirField.getText());
            if (path != null) {
                csvDirField.setText(path);
            }
        });

        resetBtn.setOnAction(e -> {
            projectDirField.setText(AppPaths.getDefaultProjectsDirectory());
            pdfDirField.setText(AppPaths.getDefaultPdfDirectory());
            csvDirField.setText(AppPaths.getDefaultCsvDirectory());
            keepDialogsCheckBox.setSelected(true);
        });

        applyBtn.setOnAction(e -> {
            currentSettings.setDefaultProjectDirectory(projectDirField.getText());
            currentSettings.setDefaultPdfDirectory(pdfDirField.getText());
            currentSettings.setDefaultCsvDirectory(csvDirField.getText());
            currentSettings.setKeepDialogsInLastDir(keepDialogsCheckBox.isSelected());
            AppSettings.save(currentSettings);
            showInfo("Настройки", "Пути сохранения успешно обновлены.");
        });

        cancelBtn.setOnAction(e -> {
            AppSettings.reload();
            currentSettings = AppSettings.get();
            projectDirField.setText(blankToDefault(currentSettings.getDefaultProjectDirectory(), ""));
            pdfDirField.setText(blankToDefault(currentSettings.getDefaultPdfDirectory(), ""));
            csvDirField.setText(blankToDefault(currentSettings.getDefaultCsvDirectory(), ""));
            keepDialogsCheckBox.setSelected(currentSettings.isKeepDialogsInLastDir());
        });
    }

    private void bindShortcutsPage(Node page) {
        TextField hotkeyNewProjectField = lookup(page, "#hotkeyNewProjectField", TextField.class);
        TextField hotkeyOpenProjectField = lookup(page, "#hotkeyOpenProjectField", TextField.class);
        TextField hotkeySaveProjectField = lookup(page, "#hotkeySaveProjectField", TextField.class);
        TextField hotkeySaveAsProjectField = lookup(page, "#hotkeySaveAsProjectField", TextField.class);
        TextField hotkeyPrintProjectField = lookup(page, "#hotkeyPrintProjectField", TextField.class);
        TextField hotkeyExportPdfField = lookup(page, "#hotkeyExportPdfField", TextField.class);
        TextField hotkeyExportCsvField = lookup(page, "#hotkeyExportCsvField", TextField.class);
        TextField hotkeyFocusSearchField = lookup(page, "#hotkeyFocusSearchField", TextField.class);
        TextField hotkeyRecalculateField = lookup(page, "#hotkeyRecalculateField", TextField.class);
        TextField hotkeyZoomInField = lookup(page, "#hotkeyZoomInField", TextField.class);
        TextField hotkeyZoomOutField = lookup(page, "#hotkeyZoomOutField", TextField.class);
        TextField hotkeyZoomResetField = lookup(page, "#hotkeyZoomResetField", TextField.class);
        TextField hotkeyShowCriticalPathField = lookup(page, "#hotkeyShowCriticalPathField", TextField.class);
        TextField hotkeyShowHelpField = lookup(page, "#hotkeyShowHelpField", TextField.class);
        TextField hotkeyUndoDeleteField = lookup(page, "#hotkeyUndoDeleteField", TextField.class);
        Button resetHotkeysBtn = lookup(page, "#resetHotkeysBtn", Button.class);
        Button showHotkeysPreviewBtn = lookup(page, "#showHotkeysPreviewBtn", Button.class);
        Button cancelHotkeysBtn = lookup(page, "#cancelHotkeysBtn", Button.class);
        Button applyHotkeysBtn = lookup(page, "#applyHotkeysBtn", Button.class);

        Runnable fillFromSettings = () -> {
            hotkeyNewProjectField.setText(blankToDefault(currentSettings.getHotkeyNewProject(), "Ctrl+N"));
            hotkeyOpenProjectField.setText(blankToDefault(currentSettings.getHotkeyOpenProject(), "Ctrl+O"));
            hotkeySaveProjectField.setText(blankToDefault(currentSettings.getHotkeySaveProject(), "Ctrl+S"));
            hotkeySaveAsProjectField.setText(blankToDefault(currentSettings.getHotkeySaveAsProject(), "Ctrl+Shift+S"));
            hotkeyPrintProjectField.setText(blankToDefault(currentSettings.getHotkeyPrintProject(), "Ctrl+P"));
            hotkeyExportPdfField.setText(blankToDefault(currentSettings.getHotkeyExportPdf(), "Ctrl+Shift+P"));
            hotkeyExportCsvField.setText(blankToDefault(currentSettings.getHotkeyExportCsv(), "Ctrl+Shift+E"));
            hotkeyFocusSearchField.setText(blankToDefault(currentSettings.getHotkeyFocusSearch(), "Ctrl+F"));
            hotkeyRecalculateField.setText(blankToDefault(currentSettings.getHotkeyRecalculate(), "F5"));
            hotkeyZoomInField.setText(blankToDefault(currentSettings.getHotkeyZoomIn(), "Ctrl+Equals"));
            hotkeyZoomOutField.setText(blankToDefault(currentSettings.getHotkeyZoomOut(), "Ctrl+Minus"));
            hotkeyZoomResetField.setText(blankToDefault(currentSettings.getHotkeyZoomReset(), "Ctrl+0"));
            hotkeyShowCriticalPathField.setText(blankToDefault(currentSettings.getHotkeyShowCriticalPath(), "Ctrl+Shift+C"));
            hotkeyShowHelpField.setText(blankToDefault(currentSettings.getHotkeyShowHelp(), "F1"));
            hotkeyUndoDeleteField.setText(blankToDefault(currentSettings.getHotkeyUndoDelete(), "Ctrl+Z"));
        };
        fillFromSettings.run();

        for (TextField field : new TextField[]{
                hotkeyNewProjectField,
                hotkeyOpenProjectField,
                hotkeySaveProjectField,
                hotkeySaveAsProjectField,
                hotkeyPrintProjectField,
                hotkeyExportPdfField,
                hotkeyExportCsvField,
                hotkeyFocusSearchField,
                hotkeyRecalculateField,
                hotkeyZoomInField,
                hotkeyZoomOutField,
                hotkeyZoomResetField,
                hotkeyShowCriticalPathField,
                hotkeyShowHelpField,
                hotkeyUndoDeleteField
        }) {
            prepareShortcutCapture(field);
        }

        resetHotkeysBtn.setOnAction(e -> {
            Settings defaults = new Settings();
            hotkeyNewProjectField.setText(defaults.getHotkeyNewProject());
            hotkeyOpenProjectField.setText(defaults.getHotkeyOpenProject());
            hotkeySaveProjectField.setText(defaults.getHotkeySaveProject());
            hotkeySaveAsProjectField.setText(defaults.getHotkeySaveAsProject());
            hotkeyPrintProjectField.setText(defaults.getHotkeyPrintProject());
            hotkeyExportPdfField.setText(defaults.getHotkeyExportPdf());
            hotkeyExportCsvField.setText(defaults.getHotkeyExportCsv());
            hotkeyFocusSearchField.setText(defaults.getHotkeyFocusSearch());
            hotkeyRecalculateField.setText(defaults.getHotkeyRecalculate());
            hotkeyZoomInField.setText(defaults.getHotkeyZoomIn());
            hotkeyZoomOutField.setText(defaults.getHotkeyZoomOut());
            hotkeyZoomResetField.setText(defaults.getHotkeyZoomReset());
            hotkeyShowCriticalPathField.setText(defaults.getHotkeyShowCriticalPath());
            hotkeyShowHelpField.setText(defaults.getHotkeyShowHelp());
            hotkeyUndoDeleteField.setText(defaults.getHotkeyUndoDelete());
        });

        showHotkeysPreviewBtn.setOnAction(e ->
                HotkeyManager.showHelp(
                        contentArea != null && contentArea.getScene() != null ? contentArea.getScene().getWindow() : null,
                        buildShortcutSettingsDraft(
                                hotkeyNewProjectField.getText(),
                                hotkeyOpenProjectField.getText(),
                                hotkeySaveProjectField.getText(),
                                hotkeySaveAsProjectField.getText(),
                                hotkeyPrintProjectField.getText(),
                                hotkeyExportPdfField.getText(),
                                hotkeyExportCsvField.getText(),
                                hotkeyFocusSearchField.getText(),
                                hotkeyRecalculateField.getText(),
                                hotkeyZoomInField.getText(),
                                hotkeyZoomOutField.getText(),
                                hotkeyZoomResetField.getText(),
                                hotkeyShowCriticalPathField.getText(),
                                hotkeyShowHelpField.getText(),
                                hotkeyUndoDeleteField.getText()
                        )
                )
        );

        cancelHotkeysBtn.setOnAction(e -> {
            AppSettings.reload();
            currentSettings = AppSettings.get();
            fillFromSettings.run();
        });

        applyHotkeysBtn.setOnAction(e -> {
            if (!validateShortcutField(hotkeyNewProjectField)
                    || !validateShortcutField(hotkeyOpenProjectField)
                    || !validateShortcutField(hotkeySaveProjectField)
                    || !validateShortcutField(hotkeySaveAsProjectField)
                    || !validateShortcutField(hotkeyPrintProjectField)
                    || !validateShortcutField(hotkeyExportPdfField)
                    || !validateShortcutField(hotkeyExportCsvField)
                    || !validateShortcutField(hotkeyFocusSearchField)
                    || !validateShortcutField(hotkeyRecalculateField)
                    || !validateShortcutField(hotkeyZoomInField)
                    || !validateShortcutField(hotkeyZoomOutField)
                    || !validateShortcutField(hotkeyZoomResetField)
                    || !validateShortcutField(hotkeyShowCriticalPathField)
                    || !validateShortcutField(hotkeyShowHelpField)
                    || !validateShortcutField(hotkeyUndoDeleteField)) {
                return;
            }

            currentSettings.setHotkeyNewProject(hotkeyNewProjectField.getText());
            currentSettings.setHotkeyOpenProject(hotkeyOpenProjectField.getText());
            currentSettings.setHotkeySaveProject(hotkeySaveProjectField.getText());
            currentSettings.setHotkeySaveAsProject(hotkeySaveAsProjectField.getText());
            currentSettings.setHotkeyPrintProject(hotkeyPrintProjectField.getText());
            currentSettings.setHotkeyExportPdf(hotkeyExportPdfField.getText());
            currentSettings.setHotkeyExportCsv(hotkeyExportCsvField.getText());
            currentSettings.setHotkeyFocusSearch(hotkeyFocusSearchField.getText());
            currentSettings.setHotkeyRecalculate(hotkeyRecalculateField.getText());
            currentSettings.setHotkeyZoomIn(hotkeyZoomInField.getText());
            currentSettings.setHotkeyZoomOut(hotkeyZoomOutField.getText());
            currentSettings.setHotkeyZoomReset(hotkeyZoomResetField.getText());
            currentSettings.setHotkeyShowCriticalPath(hotkeyShowCriticalPathField.getText());
            currentSettings.setHotkeyShowHelp(hotkeyShowHelpField.getText());
            currentSettings.setHotkeyUndoDelete(hotkeyUndoDeleteField.getText());
            AppSettings.save(currentSettings);
            showInfo("Настройки", "Горячие клавиши успешно сохранены.");
        });
    }

    private String chooseDirectory(String title, String currentPath) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle(title);
        if (currentPath != null && !currentPath.isBlank()) {
            File dir = new File(currentPath);
            if (dir.exists() && dir.isDirectory()) {
                chooser.setInitialDirectory(dir);
            }
        }
        Window window = contentArea != null && contentArea.getScene() != null ? contentArea.getScene().getWindow() : null;
        File selected = chooser.showDialog(window);
        return selected == null ? null : selected.getAbsolutePath();
    }

    @SuppressWarnings("unchecked")
    private <T> T lookup(Node page, String selector, Class<T> type) {
        Node node = page.lookup(selector);
        if (node == null) {
            throw new IllegalStateException("На странице настроек не найден элемент " + selector);
        }
        return (T) node;
    }

    private Color parseColor(String value, String fallback) {
        try {
            return Color.web(value);
        } catch (Exception e) {
            return Color.web(fallback);
        }
    }

    private String toHex(Color color) {
        if (color == null) {
            return "#000000";
        }
        int r = (int) Math.round(color.getRed() * 255);
        int g = (int) Math.round(color.getGreen() * 255);
        int b = (int) Math.round(color.getBlue() * 255);
        return String.format("#%02X%02X%02X", r, g, b);
    }

    private String blankToDefault(String value, String def) {
        return value == null || value.isBlank() ? def : value;
    }

    private Settings buildShortcutSettingsDraft(
            String newProject,
            String openProject,
            String saveProject,
            String saveAsProject,
            String printProject,
            String exportPdf,
            String exportCsv,
            String focusSearch,
            String recalculate,
            String zoomIn,
            String zoomOut,
            String zoomReset,
            String showCriticalPath,
            String showHelp,
            String undoDelete
    ) {
        Settings draft = new Settings();
        draft.setHotkeyNewProject(newProject);
        draft.setHotkeyOpenProject(openProject);
        draft.setHotkeySaveProject(saveProject);
        draft.setHotkeySaveAsProject(saveAsProject);
        draft.setHotkeyPrintProject(printProject);
        draft.setHotkeyExportPdf(exportPdf);
        draft.setHotkeyExportCsv(exportCsv);
        draft.setHotkeyFocusSearch(focusSearch);
        draft.setHotkeyRecalculate(recalculate);
        draft.setHotkeyZoomIn(zoomIn);
        draft.setHotkeyZoomOut(zoomOut);
        draft.setHotkeyZoomReset(zoomReset);
        draft.setHotkeyShowCriticalPath(showCriticalPath);
        draft.setHotkeyShowHelp(showHelp);
        draft.setHotkeyUndoDelete(undoDelete);
        return draft;
    }

    private boolean validateShortcutField(TextField field) {
        return HotkeyManager.validateCombination(field.getText(), message -> showError("Hotkeys", message));
    }

    private void prepareShortcutCapture(TextField field) {
        if (field == null) {
            return;
        }
        field.setPromptText("Press shortcut");
        field.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            KeyCode code = event.getCode();
            if (code == null) {
                return;
            }
            if (code == KeyCode.TAB) {
                return;
            }
            if (code == KeyCode.BACK_SPACE || code == KeyCode.DELETE) {
                field.clear();
                event.consume();
                return;
            }
            if (code.isModifierKey()) {
                event.consume();
                return;
            }

            String combination = buildShortcutText(event);
            if (!combination.isBlank()) {
                field.setText(combination);
            }
            event.consume();
        });
    }

    private String buildShortcutText(KeyEvent event) {
        StringBuilder builder = new StringBuilder();
        if (event.isControlDown()) {
            builder.append("Ctrl+");
        }
        if (event.isAltDown()) {
            builder.append("Alt+");
        }
        if (event.isShiftDown()) {
            builder.append("Shift+");
        }
        if (event.isMetaDown()) {
            builder.append("Meta+");
        }

        KeyCode code = event.getCode();
        String key = switch (code) {
            case EQUALS -> "Equals";
            case MINUS -> "Minus";
            case DIGIT0 -> "0";
            case DIGIT1 -> "1";
            case DIGIT2 -> "2";
            case DIGIT3 -> "3";
            case DIGIT4 -> "4";
            case DIGIT5 -> "5";
            case DIGIT6 -> "6";
            case DIGIT7 -> "7";
            case DIGIT8 -> "8";
            case DIGIT9 -> "9";
            default -> code.getName();
        };

        if (key == null || key.isBlank()) {
            return "";
        }
        builder.append(key.replace(" ", ""));
        return builder.toString();
    }

    private void renderDependencyLinePreview(Pane previewPane, String styleName) {
        if (previewPane == null) {
            return;
        }
        previewPane.getChildren().clear();

        double width = previewPane.getPrefWidth() > 0 ? previewPane.getPrefWidth() : 220.0;
        double height = previewPane.getPrefHeight() > 0 ? previewPane.getPrefHeight() : 74.0;
        double startX = 18.0;
        double startY = 22.0;
        double endX = width - 26.0;
        double endY = height - 20.0;
        String style = blankToDefault(styleName, "rounded").trim().toLowerCase();

        Path shadow;
        Path line;
            if ("classic".equals(style)) {
                shadow = buildPreviewPolyline(
                        new Point2D(startX, startY),
                        new Point2D(88.0, startY),
                    new Point2D(88.0, endY),
                    new Point2D(endX, endY)
            );
            line = buildPreviewPolyline(
                    new Point2D(startX, startY),
                    new Point2D(88.0, startY),
                    new Point2D(88.0, endY),
                    new Point2D(endX, endY)
            );
        } else if ("metro".equals(style)) {
            shadow = buildPreviewRounded(
                    8.0,
                    new Point2D(startX, startY),
                    new Point2D(96.0, startY),
                    new Point2D(136.0, startY),
                    new Point2D(136.0, endY),
                    new Point2D(endX, endY)
            );
            line = buildPreviewRounded(
                    8.0,
                    new Point2D(startX, startY),
                    new Point2D(96.0, startY),
                    new Point2D(136.0, startY),
                    new Point2D(136.0, endY),
                    new Point2D(endX, endY)
            );
        } else if ("flow".equals(style)) {
            shadow = buildPreviewRounded(
                    11.0,
                    new Point2D(startX, startY),
                    new Point2D(92.0, startY),
                    new Point2D(128.0, startY),
                    new Point2D(128.0, endY - 8.0),
                    new Point2D(endX - 8.0, endY - 8.0),
                    new Point2D(endX, endY)
            );
            line = buildPreviewRounded(
                    11.0,
                    new Point2D(startX, startY),
                    new Point2D(92.0, startY),
                    new Point2D(128.0, startY),
                    new Point2D(128.0, endY - 8.0),
                    new Point2D(endX - 8.0, endY - 8.0),
                    new Point2D(endX, endY)
            );
        } else {
            shadow = buildPreviewRounded(
                    14.0,
                    new Point2D(startX, startY),
                    new Point2D(96.0, startY),
                    new Point2D(126.0, startY),
                    new Point2D(126.0, endY),
                    new Point2D(endX, endY)
            );
            line = buildPreviewRounded(
                    14.0,
                    new Point2D(startX, startY),
                    new Point2D(96.0, startY),
                    new Point2D(126.0, startY),
                    new Point2D(126.0, endY),
                    new Point2D(endX, endY)
            );
        }

        shadow.setFill(null);
        shadow.setStroke(Color.rgb(15, 23, 42, 0.12));
        shadow.setStrokeWidth("metro".equals(style) ? 3.8 : ("flow".equals(style) ? 4.0 : 4.2));
        shadow.setStrokeLineCap(StrokeLineCap.ROUND);

        line.setFill(null);
        line.setStroke(parseColor(currentSettings.getDependencyFsColor(), "#7C848F"));
        line.setStrokeWidth("classic".equals(style) ? 1.3 : ("flow".equals(style) ? 1.45 : 1.5));
        line.setStrokeLineCap(StrokeLineCap.ROUND);

        Polygon arrow = new Polygon(
                endX, endY,
                endX - 11.0, endY - 5.0,
                endX - 11.0, endY + 5.0
        );
        arrow.setFill(parseColor(currentSettings.getDependencyFsColor(), "#7C848F"));

        previewPane.getChildren().addAll(shadow, line, arrow);
    }

    private Path buildPreviewPolyline(Point2D... points) {
        Path path = new Path();
        if (points == null || points.length < 2) {
            return path;
        }
        path.getElements().add(new MoveTo(points[0].getX(), points[0].getY()));
        for (int i = 1; i < points.length; i++) {
            path.getElements().add(new LineTo(points[i].getX(), points[i].getY()));
        }
        return path;
    }

    private Path buildPreviewRounded(double radius, Point2D... points) {
        Path path = new Path();
        if (points == null || points.length < 2) {
            return path;
        }
        path.getElements().add(new MoveTo(points[0].getX(), points[0].getY()));
        for (int i = 1; i < points.length - 1; i++) {
            Point2D prev = points[i - 1];
            Point2D current = points[i];
            Point2D next = points[i + 1];
            Point2D v1 = current.subtract(prev);
            Point2D v2 = next.subtract(current);
            double len1 = v1.magnitude();
            double len2 = v2.magnitude();
            if (len1 < 0.001 || len2 < 0.001) {
                path.getElements().add(new LineTo(current.getX(), current.getY()));
                continue;
            }
            double cornerRadius = Math.min(radius, Math.min(len1, len2) / 2.0);
            Point2D before = current.subtract(v1.normalize().multiply(cornerRadius));
            Point2D after = current.add(v2.normalize().multiply(cornerRadius));
            path.getElements().add(new LineTo(before.getX(), before.getY()));
            path.getElements().add(new QuadCurveTo(current.getX(), current.getY(), after.getX(), after.getY()));
        }
        Point2D last = points[points.length - 1];
        path.getElements().add(new LineTo(last.getX(), last.getY()));
        return path;
    }

    private void showInfo(String title, String text) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(text);
        SceneManager.applyThemeToDialog(alert);
        alert.showAndWait();
    }

    private void showError(String title, String text) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(text);
        SceneManager.applyThemeToDialog(alert);
        alert.showAndWait();
    }
}
