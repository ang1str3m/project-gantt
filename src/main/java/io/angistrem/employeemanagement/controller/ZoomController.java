package io.angistrem.employeemanagement.controller;

import io.angistrem.employeemanagement.view.GanttChart;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

public class ZoomController {

    private GanttChart ganttChart;
    private ScrollPane scrollPane;

    @FXML private Label zoomLevelLabel;
    @FXML private HBox zoomToolBar;
    @FXML private ToggleGroup zoomToggleGroup;
    @FXML private ToggleButton zoomFitButton;
    @FXML private ToggleButton zoomWeekButton;
    @FXML private ToggleButton zoomMonthButton;
    @FXML private ToggleButton zoomQuarterButton;
    @FXML private ToggleButton zoomYearButton;
    @FXML private ToggleButton zoomResetButton;
    @FXML private ToggleButton dependencyModeButton;
    @FXML private ToggleButton criticalPathButton;
    @FXML private Button projectAnalysisButton;

    private Runnable showCriticalPathAction;
    private Runnable showProjectAnalysisAction;

    public void init(GanttChart ganttChart, ScrollPane scrollPane) {
        this.ganttChart = ganttChart;
        this.scrollPane = scrollPane;

        if (this.ganttChart != null) {
            this.ganttChart.setParentScrollPane(scrollPane);
        }

        updateZoomLabel();
    }

    public void setShowCriticalPathAction(Runnable showCriticalPathAction) {
        this.showCriticalPathAction = showCriticalPathAction;
    }

    public void setShowProjectAnalysisAction(Runnable showProjectAnalysisAction) {
        this.showProjectAnalysisAction = showProjectAnalysisAction;
    }

    public void setCriticalPathVisible(boolean visible) {
        if (criticalPathButton == null) {
            return;
        }
        criticalPathButton.setSelected(visible);
        criticalPathButton.setText(visible ? "Скрыть критический путь" : "Критический путь");
    }

    @FXML
    public void initialize() {
        if (zoomToolBar != null) {
            zoomToolBar.setMaxWidth(Double.MAX_VALUE);
            if (zoomToolBar.getParent() instanceof HBox) {
                HBox.setHgrow(zoomToolBar, Priority.ALWAYS);
            }
            zoomToolBar.setMinWidth(Control.USE_PREF_SIZE);
        }

        clearToggleSelection();
        updateZoomLabel();
        setCriticalPathVisible(false);

        if (dependencyModeButton != null) {
            dependencyModeButton.setTooltip(new Tooltip(
                    "Режим создания зависимости мышкой: сначала выберите исходную задачу, затем целевую."
            ));
        }
        if (criticalPathButton != null) {
            criticalPathButton.setTooltip(new Tooltip(
                    "Показать или скрыть подсветку критического пути проекта."
            ));
        }
        if (projectAnalysisButton != null) {
            projectAnalysisButton.setTooltip(new Tooltip(
                    "Открыть анализ проекта: ключевая задача, влияние на срок и рекомендации по усилению."
            ));
        }
    }

    @FXML
    private void handleZoomIn() {
        if (ganttChart == null) {
            return;
        }
        ganttChart.zoomIn();
        clearToggleSelection();
        updateZoomLabel();
    }

    @FXML
    private void handleZoomOut() {
        if (ganttChart == null) {
            return;
        }
        ganttChart.zoomOut();
        clearToggleSelection();
        updateZoomLabel();
    }

    @FXML
    private void handleZoomFit() {
        if (ganttChart == null || scrollPane == null) {
            return;
        }
        ganttChart.zoomToFit();
        selectToggle(zoomFitButton);
        updateZoomLabel();
    }

    @FXML
    private void handleZoomWeek() {
        if (ganttChart == null) {
            return;
        }
        ganttChart.zoomToWeek();
        selectToggle(zoomWeekButton);
        updateZoomLabel();
    }

    @FXML
    private void handleZoomMonth() {
        if (ganttChart == null) {
            return;
        }
        ganttChart.zoomToMonth();
        selectToggle(zoomMonthButton);
        updateZoomLabel();
    }

    @FXML
    private void handleZoomQuarter() {
        if (ganttChart == null) {
            return;
        }
        ganttChart.zoomToQuarter();
        selectToggle(zoomQuarterButton);
        updateZoomLabel();
    }

    @FXML
    private void handleZoomYear() {
        if (ganttChart == null) {
            return;
        }
        ganttChart.zoomToYear();
        selectToggle(zoomYearButton);
        updateZoomLabel();
    }

    @FXML
    private void handleZoomReset() {
        if (ganttChart == null) {
            return;
        }
        ganttChart.zoomResetPreset();
        clearToggleSelection();
        updateZoomLabel();
    }

    @FXML
    private void handleShowCriticalPath() {
        if (showCriticalPathAction != null) {
            showCriticalPathAction.run();
        }
    }

    @FXML
    private void handleShowProjectAnalysis() {
        if (showProjectAnalysisAction != null) {
            showProjectAnalysisAction.run();
        }
    }

    @FXML
    private void handleToggleDependencyMode() {
        if (ganttChart == null || dependencyModeButton == null) {
            return;
        }
        ganttChart.setDependencyCreationMode(dependencyModeButton.isSelected());
    }

    public void refreshZoomLabel() {
        updateZoomLabel();
    }

    public void zoomInAction() {
        handleZoomIn();
    }

    public void zoomOutAction() {
        handleZoomOut();
    }

    public void zoomResetAction() {
        handleZoomReset();
    }

    private void updateZoomLabel() {
        if (zoomLevelLabel == null) {
            return;
        }
        int percent = ganttChart == null ? 100 : ganttChart.getZoomPercent();
        zoomLevelLabel.setText(percent + "%");
    }

    private void clearToggleSelection() {
        if (zoomToggleGroup != null) {
            zoomToggleGroup.selectToggle(null);
        }
    }

    private void selectToggle(ToggleButton button) {
        if (button != null) {
            button.setSelected(true);
        }
    }
}
