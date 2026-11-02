
/*
 * Copyright (c) 2026.
 */

package io.angistrem.employeemanagement.view;

import io.angistrem.employeemanagement.util.CriticalPathAnalyzer;
import io.angistrem.employeemanagement.model.SubTask;
import io.angistrem.employeemanagement.model.Task;
import io.angistrem.employeemanagement.model.TaskDependency;
import io.angistrem.employeemanagement.model.Settings;
import io.angistrem.employeemanagement.service.GanttDateService;
import io.angistrem.employeemanagement.service.GanttDependencyPathService;
import io.angistrem.employeemanagement.service.GanttChartItemService;
import io.angistrem.employeemanagement.service.GanttPresentationService;
import io.angistrem.employeemanagement.service.DependencyRoutingService;
import io.angistrem.employeemanagement.service.DependencyInteractionService;
import io.angistrem.employeemanagement.service.DependencyBadgeLayoutService;
import io.angistrem.employeemanagement.service.DependencyCreationInteractionService;
import io.angistrem.employeemanagement.service.FinancialMarkerLayoutService;
import io.angistrem.employeemanagement.service.FinancialMarkerInteractionService;
import io.angistrem.employeemanagement.service.FinancialMarkerRenderService;
import io.angistrem.employeemanagement.service.FinancialMarkerSpecService;
import io.angistrem.employeemanagement.service.DependencySpecBuildService;
import io.angistrem.employeemanagement.service.DependencyRenderService;
import io.angistrem.employeemanagement.service.DependencyLineStyleService;
import io.angistrem.employeemanagement.service.GanttDragOverlayService;
import io.angistrem.employeemanagement.service.GanttItemDateInteractionService;
import io.angistrem.employeemanagement.service.GanttOverlapService;
import io.angistrem.employeemanagement.service.GanttTextService;
import io.angistrem.employeemanagement.service.GanttThemeTokensService;
import io.angistrem.employeemanagement.service.TaskVisualTokensService;
import io.angistrem.employeemanagement.service.MainTaskBarPathService;
import io.angistrem.employeemanagement.util.AppSettings;
import io.angistrem.employeemanagement.util.ExportRenderEngine;
import javafx.collections.ListChangeListener;
import javafx.geometry.BoundingBox;
import javafx.geometry.Point2D;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.*;

public class GanttChart extends VBox {

    private enum ScaleMode {
        DAY,
        WEEK,
        MONTH,
        QUARTER,
        YEAR
    }

    private static final double DEFAULT_DAY_WIDTH = 30;
    private static final double MIN_DAY_WIDTH = 1.5;
    private static final double MAX_DAY_WIDTH = 120;
    private static final double DEFAULT_ROW_HEIGHT = 28;
    private static final double TOP_SCALE_HEIGHT = 20;
    private static final double MIDDLE_SCALE_HEIGHT = 20;
    private static final double BOTTOM_SCALE_HEIGHT = 24;
    private static final DateTimeFormatter TOOLTIP_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final double WEEK_PRESET_DAY_WIDTH = 15.0;
    private static final double MONTH_PRESET_DAY_WIDTH = 5.0;
    private static final double QUARTER_PRESET_DAY_WIDTH = 2.6;
    private static final double YEAR_PRESET_DAY_WIDTH = 1.8;

    private double dayWidth = DEFAULT_DAY_WIDTH;
    private double rowHeight = DEFAULT_ROW_HEIGHT;

    private final VBox headerBox = new VBox();
    private final HBox topScale = new HBox();
    private final HBox middleScale = new HBox();
    private final HBox bottomScale = new HBox();
    private final Pane chartPane = new Pane();

    private ObservableList<Task> tasks;
    private java.util.List<Object> displayOrder = new java.util.ArrayList<>();
    private LocalDate startDate;
    private LocalDate endDate;
    private Set<Object> visibleItems;

    private ScrollPane parentScrollPane;
    private ScaleMode scaleMode = ScaleMode.DAY;
    private ScaleMode forcedScaleMode = ScaleMode.DAY;
    private DependencyEditHandler dependencyEditHandler;
    private DependencyCreateHandler dependencyCreateHandler;
    private ItemSelectionHandler itemSelectionHandler;
    private ItemDateChangeHandler itemDateChangeHandler;
    private FinancialDateChangeHandler financialDateChangeHandler;
    private boolean showDependencies = true;
    private Object selectedItem;
    private boolean dependencyCreationMode = false;
    private Object pendingDependencySource;
    private final Set<String> criticalPathItemKeys = new HashSet<>();
    private final Set<String> criticalPathEdgeKeys = new HashSet<>();
    private final GanttChartItemService ganttChartItemService = new GanttChartItemService();
    private final GanttDateService ganttDateService = new GanttDateService();
    private final GanttItemDateInteractionService ganttItemDateInteractionService = new GanttItemDateInteractionService();
    private final GanttDependencyPathService ganttDependencyPathService = new GanttDependencyPathService();
    private final GanttPresentationService ganttPresentationService = new GanttPresentationService();
    private final DependencyRoutingService dependencyRoutingService = new DependencyRoutingService();
    private final DependencyInteractionService dependencyInteractionService = new DependencyInteractionService();
    private final DependencyBadgeLayoutService dependencyBadgeLayoutService = new DependencyBadgeLayoutService();
    private final DependencyCreationInteractionService dependencyCreationInteractionService = new DependencyCreationInteractionService();
    private final DependencySpecBuildService dependencySpecBuildService = new DependencySpecBuildService();
    private final DependencyRenderService dependencyRenderService = new DependencyRenderService();
    private final DependencyLineStyleService dependencyLineStyleService = new DependencyLineStyleService();
    private final FinancialMarkerLayoutService financialMarkerLayoutService = new FinancialMarkerLayoutService();
    private final FinancialMarkerInteractionService financialMarkerInteractionService = new FinancialMarkerInteractionService();
    private final FinancialMarkerRenderService financialMarkerRenderService = new FinancialMarkerRenderService();
    private final FinancialMarkerSpecService financialMarkerSpecService = new FinancialMarkerSpecService();
    private final GanttDragOverlayService ganttDragOverlayService = new GanttDragOverlayService();
    private final GanttOverlapService ganttOverlapService = new GanttOverlapService();
    private final GanttTextService ganttTextService = new GanttTextService();
    private final GanttThemeTokensService ganttThemeTokensService = new GanttThemeTokensService();
    private final TaskVisualTokensService taskVisualTokensService = new TaskVisualTokensService();
    private final MainTaskBarPathService mainTaskBarPathService = new MainTaskBarPathService();
    private final Label dragInfoBadge = new Label();
    private final Line dragStartGuide = new Line();
    private final Line dragEndGuide = new Line();

    public enum FinancialMarkerType {
        ADVANCE,
        KS
    }

    public GanttChart() {
        headerBox.getChildren().addAll(topScale, middleScale, bottomScale);

        topScale.setMinHeight(TOP_SCALE_HEIGHT);
        topScale.setPrefHeight(TOP_SCALE_HEIGHT);
        middleScale.setMinHeight(MIDDLE_SCALE_HEIGHT);
        middleScale.setPrefHeight(MIDDLE_SCALE_HEIGHT);
        bottomScale.setMinHeight(BOTTOM_SCALE_HEIGHT);
        bottomScale.setPrefHeight(BOTTOM_SCALE_HEIGHT);

        applyThemeStyles();
        chartPane.setMinHeight(200);
        dragInfoBadge.setManaged(false);
        dragInfoBadge.setVisible(false);
        dragInfoBadge.setMouseTransparent(true);
        dragInfoBadge.setStyle(
                "-fx-background-color: rgba(15,23,42,0.92);"
                        + "-fx-text-fill: white;"
                        + "-fx-font-size: 11px;"
                        + "-fx-font-weight: bold;"
                        + "-fx-padding: 6 10 6 10;"
                        + "-fx-background-radius: 999;"
                        + "-fx-border-radius: 999;"
                        + "-fx-border-color: rgba(148,163,184,0.5);"
        );
        configureDragGuide(dragStartGuide, "#2563eb");
        configureDragGuide(dragEndGuide, "#0f766e");
        Rectangle chartClip = new Rectangle();
        chartClip.widthProperty().bind(chartPane.widthProperty());
        chartClip.heightProperty().bind(chartPane.heightProperty());
        chartPane.setClip(chartClip);
    }

    public void setParentScrollPane(ScrollPane scrollPane) {
        this.parentScrollPane = scrollPane;
        if (headerBox != null) {
            headerBox.setTranslateY(0);
        }
    }

    public void setTasks(ObservableList<Task> tasks) {
        this.tasks = tasks;
        if (tasks != null) {
            tasks.addListener((ListChangeListener<Task>) c -> updateChart());
        }
        updateChart();
    }

    private void applyThemeStyles() {
        boolean darkTheme = isDarkTheme();
        if (darkTheme) {
            headerBox.setStyle("-fx-background-color: linear-gradient(to bottom, #182235, #101827);"
                    + "-fx-border-color: #334155; -fx-border-width: 0 0 1 0;");
            chartPane.setStyle("-fx-background-color: #fffdf8;");
        } else {
            headerBox.setStyle("-fx-background-color: linear-gradient(to bottom, #f8fbff, #edf3fb);"
                    + "-fx-border-color: #d6dde8; -fx-border-width: 0 0 1 0;");
            chartPane.setStyle("-fx-background-color: #ffffff;");
        }
    }

    public void setVisibleItems(Set<Object> visibleItems) {
        this.visibleItems = visibleItems;
        updateChart();
    }

    public Set<Object> getVisibleItems() {
        return visibleItems;
    }

    public void setDisplayOrder(java.util.List<Object> displayOrder) {
        this.displayOrder = displayOrder != null ? new java.util.ArrayList<>(displayOrder) : new java.util.ArrayList<>();
        updateChart();
    }

    public void setDependencyEditHandler(DependencyEditHandler dependencyEditHandler) {
        this.dependencyEditHandler = dependencyEditHandler;
    }

    public void setDependencyCreateHandler(DependencyCreateHandler dependencyCreateHandler) {
        this.dependencyCreateHandler = dependencyCreateHandler;
    }

    public void setItemSelectionHandler(ItemSelectionHandler itemSelectionHandler) {
        this.itemSelectionHandler = itemSelectionHandler;
    }

    public void setItemDateChangeHandler(ItemDateChangeHandler itemDateChangeHandler) {
        this.itemDateChangeHandler = itemDateChangeHandler;
    }

    public void setFinancialDateChangeHandler(FinancialDateChangeHandler financialDateChangeHandler) {
        this.financialDateChangeHandler = financialDateChangeHandler;
    }

    public void updateChart() {
        applyThemeStyles();
        if (tasks == null || tasks.isEmpty()) {
            topScale.getChildren().clear();
            middleScale.getChildren().clear();
            bottomScale.getChildren().clear();
            chartPane.getChildren().clear();
            return;
        }

        calculateDateRange();
        scaleMode = detectScaleMode();
        buildTimeScale();
        drawTasks();
    }

    private boolean isDarkTheme() {
        Settings settings = AppSettings.get();
        return settings != null && settings.getTheme() != null
                && "dark".equalsIgnoreCase(settings.getTheme().trim());
    }

    private ScaleMode detectScaleMode() {
        if (forcedScaleMode != null) {
            return forcedScaleMode;
        }
        if (dayWidth >= 18) return ScaleMode.DAY;
        if (dayWidth >= 8) return ScaleMode.WEEK;
        if (dayWidth >= 3.5) return ScaleMode.MONTH;
        if (dayWidth >= 2.2) return ScaleMode.QUARTER;
        return ScaleMode.YEAR;
    }

    private void calculateDateRange() {
        List<LocalDate> allDates = new ArrayList<>();

        for (Task task : tasks) {
            collectTaskDates(task, allDates);
        }

        if (allDates.isEmpty()) {
            startDate = LocalDate.now().withDayOfMonth(1);
            LocalDate baseEnd = startDate.plusMonths(2).withDayOfMonth(startDate.plusMonths(2).lengthOfMonth());
            endDate = baseEnd.plusMonths(1).withDayOfMonth(baseEnd.plusMonths(1).lengthOfMonth());
        } else {
            startDate = Collections.min(allDates).minusDays(2);
            LocalDate baseEnd = Collections.max(allDates).plusDays(2);
            endDate = baseEnd.plusMonths(1).withDayOfMonth(baseEnd.plusMonths(1).lengthOfMonth());
        }
    }

    private void buildTimeScale() {
        topScale.getChildren().clear();
        middleScale.getChildren().clear();
        bottomScale.getChildren().clear();

        long totalDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        double totalWidth = snap(totalDays * dayWidth);
        double chartWidth = Math.max(totalWidth, getViewportWidth());

        buildTopScale();
        buildMiddleScale();
        buildBottomScale();

        topScale.setPrefWidth(totalWidth);
        middleScale.setPrefWidth(totalWidth);
        bottomScale.setPrefWidth(totalWidth);
        headerBox.setPrefWidth(totalWidth);
        headerBox.setMinWidth(totalWidth);
        chartPane.setPrefWidth(chartWidth);
        chartPane.setMinWidth(chartWidth);

        boolean showBottom = true;
        // Keep header height stable across zoom modes so rows stay aligned with the table.
        bottomScale.setManaged(true);
        bottomScale.setVisible(showBottom);
    }

    private void buildTopScale() {
        LocalDate cursor = startDate;

        while (!cursor.isAfter(endDate)) {
            LocalDate yearEnd = LocalDate.of(cursor.getYear(), 12, 31);
            if (yearEnd.isAfter(endDate)) {
                yearEnd = endDate;
            }

            long daysInBlock = ChronoUnit.DAYS.between(cursor, yearEnd) + 1;
            Label label = scaleLabel(
                    String.valueOf(cursor.getYear()),
                    snap(daysInBlock * dayWidth),
                    TOP_SCALE_HEIGHT,
                    "#dde7f3",
                    "#1f2937",
                    true
            );
            topScale.getChildren().add(label);
            cursor = yearEnd.plusDays(1);
        }
    }

    private void buildMiddleScale() {
        switch (scaleMode) {
            case DAY -> buildMonthRow();
            case WEEK -> buildMonthRow();
            case MONTH -> buildMonthRow();
            case QUARTER -> buildQuarterRow();
            case YEAR -> buildQuarterRow();
        }
    }

    private void buildBottomScale() {
        switch (scaleMode) {
            case DAY -> buildDayRowAdaptive(true);
            case WEEK -> buildWeekRowAlignedV2();
            case MONTH -> buildWeekOfMonthRowCompactV2();
            case QUARTER -> buildMonthShortRow();
            case YEAR -> buildMonthShortRow();
        }
    }

    private void buildMonthRow() {
        LocalDate cursor = startDate;

        while (!cursor.isAfter(endDate)) {
            YearMonth ym = YearMonth.from(cursor);
            LocalDate monthEnd = ym.atEndOfMonth();
            if (monthEnd.isAfter(endDate)) {
                monthEnd = endDate;
            }

            long daysInBlock = ChronoUnit.DAYS.between(cursor, monthEnd) + 1;
            String text = ym.getMonth().getDisplayName(TextStyle.FULL_STANDALONE, Locale.forLanguageTag("ru"));
            text = text.substring(0, 1).toUpperCase(Locale.ROOT) + text.substring(1);

            middleScale.getChildren().add(scaleLabel(
                    text,
                    snap(daysInBlock * dayWidth),
                    MIDDLE_SCALE_HEIGHT,
                    "#edf3f8",
                    "#334155",
                    true
            ));

            cursor = monthEnd.plusDays(1);
        }
    }

    private void buildWeekOfYearRow() {
        LocalDate cursor = startDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        while (!cursor.isAfter(endDate)) {
            LocalDate rawEnd = cursor.plusDays(6);
            LocalDate blockStart = cursor.isBefore(startDate) ? startDate : cursor;
            LocalDate blockEnd = rawEnd.isAfter(endDate) ? endDate : rawEnd;
            LocalDate monthEnd = YearMonth.from(blockStart).atEndOfMonth();
            if (blockEnd.isAfter(monthEnd)) {
                blockEnd = monthEnd;
            }
            long daysInBlock = ChronoUnit.DAYS.between(blockStart, blockEnd) + 1;
            int week = cursor.get(WeekFields.ISO.weekOfWeekBasedYear());
            middleScale.getChildren().add(scaleLabel(
                    "\u041d\u0435\u0434. " + week,
                    snap(daysInBlock * dayWidth),
                    MIDDLE_SCALE_HEIGHT,
                    "#edf3f8",
                    "#334155",
                    true
            ));
            cursor = blockEnd.plusDays(1);
        }
    }

    private void buildQuarterRow() {
        LocalDate cursor = startDate;
        while (!cursor.isAfter(endDate)) {
            int quarter = ((cursor.getMonthValue() - 1) / 3) + 1;
            int quarterEndMonth = quarter * 3;
            LocalDate quarterEnd = LocalDate.of(cursor.getYear(), quarterEndMonth,
                    YearMonth.of(cursor.getYear(), quarterEndMonth).lengthOfMonth());
            if (quarterEnd.isAfter(endDate)) {
                quarterEnd = endDate;
            }
            long daysInBlock = ChronoUnit.DAYS.between(cursor, quarterEnd) + 1;
            middleScale.getChildren().add(scaleLabel(
                    quarter + " \u043a\u0432.",
                    snap(daysInBlock * dayWidth),
                    MIDDLE_SCALE_HEIGHT,
                    "#edf3f8",
                    "#334155",
                    true
            ));
            cursor = quarterEnd.plusDays(1);
        }
    }

    private void buildHalfYearRow() {
        LocalDate cursor = startDate;
        while (!cursor.isAfter(endDate)) {
            int half = cursor.getMonthValue() <= 6 ? 1 : 2;
            LocalDate blockEnd = LocalDate.of(cursor.getYear(), half == 1 ? 6 : 12, half == 1 ? 30 : 31);
            if (blockEnd.isAfter(endDate)) {
                blockEnd = endDate;
            }
            long daysInBlock = ChronoUnit.DAYS.between(cursor, blockEnd) + 1;
            middleScale.getChildren().add(scaleLabel(
                    half + " \u043f\u043e\u043b\u0443\u0433\u043e\u0434\u0438\u0435",
                    snap(daysInBlock * dayWidth),
                    MIDDLE_SCALE_HEIGHT,
                    "#edf3f8",
                    "#334155",
                    true
            ));
            cursor = blockEnd.plusDays(1);
        }
    }

    private void buildDayRowAdaptive(boolean showWeekday) {
        LocalDate today = LocalDate.now();
        boolean showDayName = showWeekday && dayWidth >= 16;
        int spanDays;
        if (showWeekday) {
            spanDays = 1;
        } else if (scaleMode == ScaleMode.WEEK) {
            spanDays = Math.max(1, (int) Math.ceil(12.0 / Math.max(dayWidth, 1.0)));
        } else {
            spanDays = Math.max(2, (int) Math.ceil(24.0 / Math.max(dayWidth, 1.0)));
        }

        LocalDate cursor = startDate;
        while (!cursor.isAfter(endDate)) {
            LocalDate blockEnd = cursor.plusDays(spanDays - 1L);
            if (blockEnd.isAfter(endDate)) {
                blockEnd = endDate;
            }

            long daysInBlock = ChronoUnit.DAYS.between(cursor, blockEnd) + 1;

            VBox cell = new VBox();
            cell.setAlignment(Pos.CENTER);
            cell.setPrefWidth(snap(daysInBlock * dayWidth));
            cell.setMinHeight(BOTTOM_SCALE_HEIGHT);
            cell.setPrefHeight(BOTTOM_SCALE_HEIGHT);

            String labelText = ganttDateService.buildDateBlockLabel(cursor, blockEnd, daysInBlock);
            Label dayNumber = new Label(labelText);
            dayNumber.setTextOverrun(OverrunStyle.CLIP);
            dayNumber.setMaxWidth(Double.MAX_VALUE);
            dayNumber.setAlignment(Pos.CENTER);

            if (showDayName && daysInBlock == 1) {
                Label dayName = new Label(ganttDateService.shortDayName(cursor.getDayOfWeek()));
                dayName.setTextOverrun(OverrunStyle.CLIP);
                dayName.setStyle("-fx-font-size: 8px;");
                cell.getChildren().addAll(dayNumber, dayName);
            } else {
                cell.getChildren().add(dayNumber);
            }

            boolean blockHasWeekend = ganttDateService.containsWeekend(cursor, blockEnd);
            boolean blockHasToday = !today.isBefore(cursor) && !today.isAfter(blockEnd);

            String bg = blockHasWeekend ? "#fef2f2" : "#ffffff";
            String textColor = blockHasWeekend ? "#b91c1c" : "#334155";
            String borderColor = "#e2e8f0";
            if (blockHasToday) {
                bg = "#dbeafe";
                textColor = "#1d4ed8";
                borderColor = "#60a5fa";
            }

            // Inline text color to avoid global theme rules hiding day labels.
            dayNumber.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: " + textColor + ";");
            if (showDayName && daysInBlock == 1 && cell.getChildren().size() > 1 && cell.getChildren().get(1) instanceof Label dayName) {
                dayName.setStyle("-fx-font-size: 8px; -fx-text-fill: " + textColor + ";");
            }

            cell.setStyle("-fx-background-color: " + bg + ";"
                    + "-fx-border-color: " + borderColor + ";"
                    + "-fx-border-width: 0 1 1 0;");

            String tooltip = cursor.format(TOOLTIP_FORMATTER);
            if (!cursor.equals(blockEnd)) {
                tooltip += " - " + blockEnd.format(TOOLTIP_FORMATTER);
            }
            Tooltip.install(cell, new Tooltip(tooltip));
            bottomScale.getChildren().add(cell);

            cursor = blockEnd.plusDays(1);
        }
    }

    private void buildWeekRow() {
        LocalDate cursor = startDate;
        while (!cursor.isAfter(endDate)) {
            LocalDate weekEnd = cursor.plusDays(6);
            if (weekEnd.isAfter(endDate)) {
                weekEnd = endDate;
            }

            long daysInBlock = ChronoUnit.DAYS.between(cursor, weekEnd) + 1;
            int week = cursor.get(ChronoField.ALIGNED_WEEK_OF_YEAR);

            bottomScale.getChildren().add(scaleLabel(
                    "\u041d\u0435\u0434. " + week,
                    snap(daysInBlock * dayWidth),
                    BOTTOM_SCALE_HEIGHT,
                    "#ffffff",
                    "#334155",
                    false
            ));

            cursor = weekEnd.plusDays(1);
        }
    }

    private void buildMonthShortRow() {
        LocalDate cursor = startDate;
        while (!cursor.isAfter(endDate)) {
            LocalDate monthEnd = YearMonth.from(cursor).atEndOfMonth();
            if (monthEnd.isAfter(endDate)) {
                monthEnd = endDate;
            }

            long daysInBlock = ChronoUnit.DAYS.between(cursor, monthEnd) + 1;
            String text = cursor.getMonth().getDisplayName(TextStyle.SHORT_STANDALONE, Locale.forLanguageTag("ru"));
            text = text.substring(0, 1).toUpperCase(Locale.ROOT) + text.substring(1);

            bottomScale.getChildren().add(scaleLabel(
                    text,
                    snap(daysInBlock * dayWidth),
                    BOTTOM_SCALE_HEIGHT,
                    "#ffffff",
                    "#334155",
                    false
            ));

            cursor = monthEnd.plusDays(1);
        }
    }

    private void buildWeekRowAligned() {
        LocalDate cursor = startDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        while (!cursor.isAfter(endDate)) {
            LocalDate weekEnd = cursor.plusDays(6);
            LocalDate blockStart = cursor.isBefore(startDate) ? startDate : cursor;
            LocalDate blockEnd = weekEnd.isAfter(endDate) ? endDate : weekEnd;
            long daysInBlock = ChronoUnit.DAYS.between(blockStart, blockEnd) + 1;

            int week = cursor.get(WeekFields.ISO.weekOfWeekBasedYear());
            bottomScale.getChildren().add(scaleLabel(
                    "\u041d\u0435\u0434. " + week,
                    snap(daysInBlock * dayWidth),
                    BOTTOM_SCALE_HEIGHT,
                    "#ffffff",
                    "#334155",
                    false
            ));

            cursor = cursor.plusWeeks(1);
        }
    }

    private void buildWeekOfMonthRowCompact() {
        LocalDate cursor = startDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        while (!cursor.isAfter(endDate)) {
            LocalDate weekEnd = cursor.plusDays(6);
            LocalDate blockStart = cursor.isBefore(startDate) ? startDate : cursor;
            LocalDate blockEnd = weekEnd.isAfter(endDate) ? endDate : weekEnd;
            LocalDate monthEnd = YearMonth.from(blockStart).atEndOfMonth();
            if (blockEnd.isAfter(monthEnd)) {
                blockEnd = monthEnd;
            }

            long daysInBlock = ChronoUnit.DAYS.between(blockStart, blockEnd) + 1;
            int weekOfMonth = ganttDateService.weekNumberInMonth(blockStart);

            VBox cell = new VBox();
            cell.setAlignment(Pos.CENTER);
            cell.setPrefWidth(snap(daysInBlock * dayWidth));
            cell.setMinHeight(BOTTOM_SCALE_HEIGHT);
            cell.setPrefHeight(BOTTOM_SCALE_HEIGHT);

            Label weekNum = new Label(String.valueOf(weekOfMonth));
            weekNum.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #334155;");
            cell.getChildren().add(weekNum);

            cell.setStyle("-fx-background-color: #ffffff; -fx-border-color: #d6dde8; -fx-border-width: 0 1 1 0;");
            Tooltip.install(cell, new Tooltip(blockStart.format(TOOLTIP_FORMATTER) + " - " + blockEnd.format(TOOLTIP_FORMATTER)));
            bottomScale.getChildren().add(cell);

            cursor = blockEnd.plusDays(1);
            if (cursor.getDayOfWeek() != DayOfWeek.MONDAY) {
                cursor = cursor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            }
        }
    }

    private void buildWeekRowClean() {
        LocalDate cursor = startDate;
        while (!cursor.isAfter(endDate)) {
            LocalDate weekEnd = cursor.plusDays(6);
            if (weekEnd.isAfter(endDate)) {
                weekEnd = endDate;
            }

            long daysInBlock = ChronoUnit.DAYS.between(cursor, weekEnd) + 1;
            int week = cursor.get(ChronoField.ALIGNED_WEEK_OF_YEAR);

            bottomScale.getChildren().add(scaleLabel(
                    "\u041d\u0435\u0434. " + week,
                    snap(daysInBlock * dayWidth),
                    BOTTOM_SCALE_HEIGHT,
                    "#ffffff",
                    "#334155",
                    false
            ));

            cursor = weekEnd.plusDays(1);
        }
    }

    private void buildWeekOfMonthRow() {
        LocalDate cursor = startDate;
        while (!cursor.isAfter(endDate)) {
            int weekOfMonth = cursor.get(ChronoField.ALIGNED_WEEK_OF_MONTH);
            LocalDate weekEnd = cursor.with(ChronoField.ALIGNED_WEEK_OF_MONTH, weekOfMonth).plusDays(6);
            LocalDate monthEnd = YearMonth.from(cursor).atEndOfMonth();
            if (weekEnd.isAfter(monthEnd)) {
                weekEnd = monthEnd;
            }
            if (weekEnd.isAfter(endDate)) {
                weekEnd = endDate;
            }

            long daysInBlock = ChronoUnit.DAYS.between(cursor, weekEnd) + 1;
            bottomScale.getChildren().add(scaleLabel(
                    weekOfMonth + " \u043d\u0435\u0434.",
                    snap(daysInBlock * dayWidth),
                    BOTTOM_SCALE_HEIGHT,
                    "#ffffff",
                    "#334155",
                    false
            ));

            cursor = weekEnd.plusDays(1);
        }
    }

    // Stable weekly scale aligned to calendar weeks; segments do not cross month boundaries.
    private void buildWeekRowAlignedV2() {
        LocalDate cursor = startDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        while (!cursor.isAfter(endDate)) {
            LocalDate rawEnd = cursor.plusDays(6);
            LocalDate blockStart = cursor.isBefore(startDate) ? startDate : cursor;
            LocalDate blockEnd = rawEnd.isAfter(endDate) ? endDate : rawEnd;
            LocalDate monthEnd = YearMonth.from(blockStart).atEndOfMonth();
            if (blockEnd.isAfter(monthEnd)) {
                blockEnd = monthEnd;
            }
            long daysInBlock = ChronoUnit.DAYS.between(blockStart, blockEnd) + 1;
            int week = cursor.get(WeekFields.ISO.weekOfWeekBasedYear());
            bottomScale.getChildren().add(scaleLabel(
                    "\u041d\u0435\u0434. " + week,
                    snap(daysInBlock * dayWidth),
                    BOTTOM_SCALE_HEIGHT,
                    "#ffffff",
                    "#334155",
                    false
            ));
            cursor = blockEnd.plusDays(1);
        }
    }

    private void buildWeekDayDateRow() {
        LocalDate today = LocalDate.now();
        long totalDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        for (int i = 0; i < totalDays; i++) {
            LocalDate day = startDate.plusDays(i);

            VBox cell = new VBox();
            cell.setAlignment(Pos.CENTER);
            cell.setPrefWidth(snap(dayWidth));
            cell.setMinHeight(BOTTOM_SCALE_HEIGHT);
            cell.setPrefHeight(BOTTOM_SCALE_HEIGHT);

            String dayName = day.getDayOfWeek().getDisplayName(TextStyle.SHORT_STANDALONE, Locale.forLanguageTag("ru"));
            if (dayName != null && !dayName.isBlank()) {
                dayName = dayName.substring(0, 1).toUpperCase(Locale.ROOT) + dayName.substring(1);
            }

            Label nameLabel = new Label(dayName == null ? "" : dayName);
            nameLabel.setStyle("-fx-font-size: 8px;");
            Label dateLabel = new Label(String.valueOf(day.getDayOfMonth()));
            dateLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: bold;");
            cell.getChildren().addAll(nameLabel, dateLabel);

            String bg = ganttDateService.isWeekend(day) ? "#fef2f2" : "#ffffff";
            String textColor = ganttDateService.isWeekend(day) ? "#b91c1c" : "#334155";
            String borderColor = "#e2e8f0";
            if (day.equals(today)) {
                bg = "#dbeafe";
                textColor = "#1d4ed8";
                borderColor = "#60a5fa";
            }

            nameLabel.setStyle("-fx-font-size: 8px; -fx-text-fill: " + textColor + ";");
            dateLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: " + textColor + ";");
            cell.setStyle("-fx-background-color: " + bg + "; -fx-border-color: " + borderColor + "; -fx-border-width: 0 1 1 0;");

            Tooltip.install(cell, new Tooltip(day.format(TOOLTIP_FORMATTER)));
            bottomScale.getChildren().add(cell);
        }
    }

    private void buildWeekDayNameRow() {
        LocalDate today = LocalDate.now();
        long totalDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        for (int i = 0; i < totalDays; i++) {
            LocalDate day = startDate.plusDays(i);

            VBox cell = new VBox();
            cell.setAlignment(Pos.CENTER);
            cell.setPrefWidth(snap(dayWidth));
            cell.setMinHeight(BOTTOM_SCALE_HEIGHT);
            cell.setPrefHeight(BOTTOM_SCALE_HEIGHT);

            String dayName = ganttDateService.shortDayName(day.getDayOfWeek());

            Label nameLabel = new Label(dayName == null ? "" : dayName);
            nameLabel.setStyle("-fx-font-size: 9px; -fx-font-weight: bold;");
            cell.getChildren().add(nameLabel);

            String bg = ganttDateService.isWeekend(day) ? "#fef2f2" : "#ffffff";
            String textColor = ganttDateService.isWeekend(day) ? "#b91c1c" : "#334155";
            String borderColor = "#e2e8f0";
            if (day.equals(today)) {
                bg = "#dbeafe";
                textColor = "#1d4ed8";
                borderColor = "#60a5fa";
            }

            nameLabel.setStyle("-fx-font-size: 9px; -fx-font-weight: bold; -fx-text-fill: " + textColor + ";");
            cell.setStyle("-fx-background-color: " + bg + "; -fx-border-color: " + borderColor + "; -fx-border-width: 0 1 1 0;");

            Tooltip.install(cell, new Tooltip(day.format(TOOLTIP_FORMATTER)));
            bottomScale.getChildren().add(cell);
        }
    }

    // Month mode: two-line labels "N" + "\u043d\u0435\u0434." and forward-only cursor to prevent infinite loops.
    private void buildWeekOfMonthRowCompactV2() {
        LocalDate cursor = startDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        while (!cursor.isAfter(endDate)) {
            LocalDate rawEnd = cursor.plusDays(6);
            LocalDate blockStart = cursor.isBefore(startDate) ? startDate : cursor;
            LocalDate blockEnd = rawEnd.isAfter(endDate) ? endDate : rawEnd;
            LocalDate monthEnd = YearMonth.from(blockStart).atEndOfMonth();
            if (blockEnd.isAfter(monthEnd)) {
                blockEnd = monthEnd;
            }
            long daysInBlock = ChronoUnit.DAYS.between(blockStart, blockEnd) + 1;
            int weekOfMonth = ganttDateService.weekNumberInMonth(blockStart);
            VBox cell = new VBox();
            cell.setAlignment(Pos.CENTER);
            cell.setPrefWidth(snap(daysInBlock * dayWidth));
            cell.setMinHeight(BOTTOM_SCALE_HEIGHT);
            cell.setPrefHeight(BOTTOM_SCALE_HEIGHT);
            Label weekNum = new Label(String.valueOf(weekOfMonth));
            weekNum.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #334155;");
            Label weekSuffix = new Label("\u043d\u0435\u0434.");
            weekSuffix.setStyle("-fx-font-size: 8px; -fx-text-fill: #64748b;");
            cell.getChildren().addAll(weekNum, weekSuffix);
            cell.setStyle("-fx-background-color: #ffffff; -fx-border-color: #d6dde8; -fx-border-width: 0 1 1 0;");
            Tooltip.install(cell, new Tooltip(blockStart.format(TOOLTIP_FORMATTER) + " - " + blockEnd.format(TOOLTIP_FORMATTER)));
            bottomScale.getChildren().add(cell);
            cursor = blockEnd.plusDays(1);
        }
    }

    private Label scaleLabel(String text, double width, double height, String bg, String color, boolean bold) {
        Label label = new Label(text);
        label.setPrefWidth(snap(width));
        label.setMinHeight(height);
        label.setAlignment(Pos.CENTER);
        label.setStyle("-fx-font-size: 10px;"
                + (bold ? "-fx-font-weight: bold;" : "")
                + "-fx-text-fill: " + color + ";"
                + "-fx-background-color: " + bg + ";"
                + "-fx-border-color: #d6dde8; -fx-border-width: 0 1 1 0;");
        return label;
    }

    private void drawTasks() {
        chartPane.getChildren().clear();
        chartPane.getChildren().add(dragInfoBadge);
        chartPane.getChildren().addAll(dragStartGuide, dragEndGuide);

        double y = 0;

        java.util.List<Object> orderedItems = ganttChartItemService.resolveOrderedItems(tasks, displayOrder, visibleItems);

        double contentHeight = Math.max(orderedItems.size() * rowHeight, 120);
        double fullHeight = Math.max(contentHeight, getViewportHeight());
        chartPane.setPrefHeight(fullHeight);
        drawRowStripes(fullHeight);
        drawGrid(fullHeight);
        Map<String, Rectangle> taskBars = new LinkedHashMap<>();
        Map<String, Object> itemsByKey = new LinkedHashMap<>();
        List<Runnable> overlayPainters = new ArrayList<>();

        for (Object item : orderedItems) {
            if (item instanceof Task task) {
                if (ganttChartItemService.isTaskVisible(task, visibleItems)) {
                    Color taskColor = resolveTaskColor(task);
                    boolean selected = item == selectedItem;
                    String tooltipText = buildTaskTooltip(task);
                    Rectangle bar;
                    if (task.isMilestone()) {
                        LocalDate milestoneDate = task.getStart() != null ? task.getStart() : task.getEnd();
                        bar = drawMilestoneMarker(milestoneDate, y, taskColor, selected, tooltipText, task);
                    } else {
                        bar = drawTaskBar(task.getStart(), task.getEnd(), y, taskColor, task.isGroup() ? 11 : 13, false, selected, tooltipText, task);
                    }
                    if (bar != null) {
                        String key = task.getPosition();
                        if (key != null) {
                            taskBars.put(key, bar);
                            itemsByKey.put(key, task);
                        }
                        final double markerY = y;
                        overlayPainters.add(() -> drawFinancialMarkers(task, markerY));
                    }
                    y += rowHeight;
                }
            } else if (item instanceof SubTask st) {
                if (ganttChartItemService.isSubTaskVisible(st, visibleItems)) {
                    boolean selected = item == selectedItem;
                    Rectangle bar = drawTaskBar(st.getStart(), st.getEnd(), y, resolveSubTaskColor(), 12, true, selected, buildSubTaskTooltip(st), st);
                    if (bar != null && st.getSubPosition() != null) {
                        taskBars.put(st.getSubPosition(), bar);
                        itemsByKey.put(st.getSubPosition(), st);
                    }
                    y += rowHeight;
                }
            }
        }

        chartPane.setPrefHeight(Math.max(Math.max(y, 120), getViewportHeight()));
        if (showDependencies) {
            drawDependencyArrows(taskBars, itemsByKey);
        }
        overlayPainters.forEach(Runnable::run);
    }

    private void drawRowStripes(double height) {
        int rows = (int) Math.ceil(height / rowHeight);
        for (int row = 0; row < rows; row++) {
            if (row % 2 == 1) {
                Rectangle stripe = new Rectangle(0, row * rowHeight, chartPane.getPrefWidth(), rowHeight);
                stripe.setFill(ganttThemeTokensService.rowStripeFill());
                chartPane.getChildren().add(stripe);
            }
        }
    }

    private void drawGrid(double height) {
        LocalDate today = LocalDate.now();
        if (!today.isBefore(startDate) && !today.isAfter(endDate)) {
            double todayX = snap(ChronoUnit.DAYS.between(startDate, today) * dayWidth);
            Rectangle todayBand = new Rectangle(todayX, 0, Math.max(snap(dayWidth), 2.0), height);
            todayBand.setFill(ganttThemeTokensService.todayBandFill());
            chartPane.getChildren().add(todayBand);
        }

        if (scaleMode == ScaleMode.DAY) {
            long totalDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;

            for (int i = 0; i <= totalDays; i++) {
                LocalDate currentDate = startDate.plusDays(i);
                double x = snap(i * dayWidth);

                if (i < totalDays && ganttDateService.isWeekend(currentDate)) {
                    Rectangle weekend = new Rectangle(x, 0, Math.max(snap(dayWidth), 1.0), height);
                    weekend.setFill(Color.web("#fefbf3"));
                    chartPane.getChildren().add(weekend);
                }

                Line line = new Line(x, 0, x, height);

                if (i < totalDays && currentDate.equals(today)) {
                    line.setStroke(ganttThemeTokensService.todayLineStroke());
                    line.setStrokeWidth(ganttThemeTokensService.todayLineWidth());
                } else if (i < totalDays && currentDate.getDayOfMonth() == 1) {
                    line.setStroke(ganttThemeTokensService.gridMajorStroke());
                    line.setStrokeWidth(ganttThemeTokensService.dayMajorLineWidth());
                } else {
                    line.setStroke(ganttThemeTokensService.gridLineStroke());
                    line.setStrokeWidth(ganttThemeTokensService.dayBaseLineWidth());
                }

                chartPane.getChildren().add(line);
            }
        } else if (scaleMode == ScaleMode.WEEK) {
            LocalDate cursor = startDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            while (!cursor.isAfter(endDate)) {
                LocalDate rawEnd = cursor.plusDays(6);
                LocalDate lineDate = cursor.isBefore(startDate) ? startDate : cursor;
                LocalDate blockEnd = rawEnd.isAfter(endDate) ? endDate : rawEnd;
                LocalDate monthEnd = YearMonth.from(lineDate).atEndOfMonth();
                if (blockEnd.isAfter(monthEnd)) {
                    blockEnd = monthEnd;
                }
                double x = snap(ChronoUnit.DAYS.between(startDate, lineDate) * dayWidth);
                Line line = new Line(x, 0, x, height);
                if (!today.isBefore(lineDate) && !today.isAfter(blockEnd)) {
                    line.setStroke(ganttThemeTokensService.todayLineStroke());
                    line.setStrokeWidth(ganttThemeTokensService.todayLineWidth());
                } else if (lineDate.getDayOfMonth() <= 7) {
                    line.setStroke(ganttThemeTokensService.gridMajorStroke());
                    line.setStrokeWidth(ganttThemeTokensService.dayMajorLineWidth());
                } else {
                    line.setStroke(ganttThemeTokensService.weekMinorStroke());
                    line.setStrokeWidth(ganttThemeTokensService.weekMinorLineWidth());
                }
                chartPane.getChildren().add(line);
                cursor = blockEnd.plusDays(1);
            }
        } else if (scaleMode == ScaleMode.MONTH) {
            LocalDate cursor = startDate.withDayOfMonth(1);
            if (cursor.isAfter(startDate)) {
                cursor = startDate;
            }
            while (!cursor.isAfter(endDate)) {
                double x = snap(ChronoUnit.DAYS.between(startDate, cursor) * dayWidth);
                Line line = new Line(x, 0, x, height);
                if (cursor.getMonthValue() == 1) {
                    line.setStroke(ganttThemeTokensService.gridStrongStroke());
                    line.setStrokeWidth(ganttThemeTokensService.monthStrongLineWidth());
                } else {
                    line.setStroke(ganttThemeTokensService.gridMajorStroke());
                    line.setStrokeWidth(ganttThemeTokensService.monthMajorLineWidth());
                }
                chartPane.getChildren().add(line);
                cursor = cursor.plusMonths(1).withDayOfMonth(1);
            }
        } else if (scaleMode == ScaleMode.QUARTER) {
            LocalDate cursor = startDate;
            while (!cursor.isAfter(endDate)) {
                LocalDate monthStart = cursor.withDayOfMonth(1);
                LocalDate lineDate = monthStart.isBefore(startDate) ? startDate : monthStart;
                double x = snap(ChronoUnit.DAYS.between(startDate, lineDate) * dayWidth);
                Line line = new Line(x, 0, x, height);

                boolean quarterStart = monthStart.getMonthValue() == 1 || monthStart.getMonthValue() == 4
                        || monthStart.getMonthValue() == 7 || monthStart.getMonthValue() == 10;

                if (monthStart.getMonthValue() == 1) {
                    line.setStroke(ganttThemeTokensService.gridStrongStroke());
                    line.setStrokeWidth(ganttThemeTokensService.quarterStrongLineWidth());
                } else if (quarterStart) {
                    line.setStroke(ganttThemeTokensService.gridMajorStroke());
                    line.setStrokeWidth(ganttThemeTokensService.quarterMajorLineWidth());
                } else {
                    line.setStroke(ganttThemeTokensService.quarterMinorStroke());
                    line.setStrokeWidth(ganttThemeTokensService.quarterMinorLineWidth());
                }

                chartPane.getChildren().add(line);
                cursor = YearMonth.from(cursor).atEndOfMonth().plusDays(1);
            }
        } else {
            LocalDate cursor = startDate.withDayOfMonth(1);
            if (cursor.isAfter(startDate)) {
                cursor = startDate;
            }
            while (!cursor.isAfter(endDate)) {
                int month = cursor.getMonthValue();
                boolean halfYearStart = month == 1 || month == 7;
                boolean firstVisible = cursor.equals(startDate.withDayOfMonth(1));
                if (halfYearStart || firstVisible) {
                    double x = snap(ChronoUnit.DAYS.between(startDate, cursor) * dayWidth);
                    Line line = new Line(x, 0, x, height);
                    line.setStroke(halfYearStart ? ganttThemeTokensService.gridStrongStroke() : ganttThemeTokensService.gridMajorStroke());
                    line.setStrokeWidth(halfYearStart ? ganttThemeTokensService.yearStrongLineWidth() : ganttThemeTokensService.yearMajorLineWidth());
                    chartPane.getChildren().add(line);
                }
                cursor = cursor.plusMonths(1).withDayOfMonth(1);
            }
        }

        for (int row = 0; row <= height / rowHeight; row++) {
            double y = row * rowHeight;
            Line h = new Line(0, y, getGridWidth(), y);
            h.setStroke(ganttThemeTokensService.horizontalLineStroke());
            h.setStrokeWidth(ganttThemeTokensService.horizontalLineWidth());
            chartPane.getChildren().add(h);
        }
    }

    private void collectTaskDates(Task task, List<LocalDate> allDates) {
        if (task == null) {
            return;
        }

        if (task.getStart() != null) allDates.add(task.getStart());
        if (task.getEnd() != null) allDates.add(task.getEnd());

        for (SubTask st : task.getObservableSubTasks()) {
            if (st.getStart() != null) allDates.add(st.getStart());
            if (st.getEnd() != null) allDates.add(st.getEnd());
        }

        for (Task childTask : task.getObservableChildTasks()) {
            collectTaskDates(childTask, allDates);
        }
    }

    private Rectangle drawTaskBar(LocalDate start, LocalDate end, double y, Color color, double barHeight, boolean subTask, boolean selected, String tooltipText, Object itemRef) {
        if (start == null || end == null) return null;
        boolean criticalPathItem = isCriticalPathItem(itemRef);

        long offset = ChronoUnit.DAYS.between(startDate, start);
        long duration = ChronoUnit.DAYS.between(start, end) + 1;
        if (duration <= 0) return null;

        double startX = snap(offset * dayWidth);
        double width = Math.max(snap(duration * dayWidth), subTask ? 5 : 7);
        double topY = snap(y + ((rowHeight - barHeight) / 2.0));
        barHeight = snap(barHeight);

        Rectangle anchor = new Rectangle(startX, topY, width, barHeight);
        anchor.setFill(Color.TRANSPARENT);
        anchor.setStroke(Color.TRANSPARENT);

        if (subTask) {
            Rectangle rect = new Rectangle(startX, topY, width, barHeight);
            rect.setArcWidth(6);
            rect.setArcHeight(6);
            rect.setFill(color);
            rect.setStroke(taskVisualTokensService.subTaskStroke(criticalPathItem, selected));
            rect.setStrokeWidth(taskVisualTokensService.subTaskStrokeWidth(criticalPathItem, selected));
            rect.setSmooth(false);

            if (tooltipText != null && !tooltipText.isBlank()) {
                Tooltip.install(rect, new Tooltip(tooltipText));
            }

            double baseOpacity = taskVisualTokensService.subTaskBaseOpacity(criticalPathItem, selected);
            rect.setOpacity(baseOpacity);
            rect.setOnMouseEntered(e -> {
                rect.setOpacity(1.0);
                rect.setStrokeWidth(taskVisualTokensService.subTaskHoverStrokeWidth(criticalPathItem, selected));
                rect.toFront();
            });
            rect.setOnMouseExited(e -> {
                rect.setOpacity(baseOpacity);
                rect.setStrokeWidth(taskVisualTokensService.subTaskStrokeWidth(criticalPathItem, selected));
            });
            installItemInteraction(rect, itemRef, selected);
            installDateInteraction(rect, anchor, itemRef, false);
            addResizeHandles(anchor, selected, false);

            chartPane.getChildren().add(rect);
            return rect;
        }

        SVGPath barShape = createMainTaskBarShape(startX, topY, width, barHeight);
        barShape.setFill(color);
        barShape.setStroke(taskVisualTokensService.taskStroke(criticalPathItem, selected));
        barShape.setStrokeWidth(taskVisualTokensService.taskStrokeWidth(criticalPathItem, selected));
        barShape.setSmooth(false);

        if (tooltipText != null && !tooltipText.isBlank()) {
            Tooltip.install(barShape, new Tooltip(tooltipText));
        }

        double baseOpacity = taskVisualTokensService.taskBaseOpacity(criticalPathItem, selected);
        barShape.setOpacity(baseOpacity);
        barShape.setOnMouseEntered(e -> {
            barShape.setOpacity(1.0);
            barShape.setStrokeWidth(taskVisualTokensService.taskHoverStrokeWidth(criticalPathItem, selected));
            barShape.toFront();
        });
        barShape.setOnMouseExited(e -> {
            barShape.setOpacity(baseOpacity);
            barShape.setStrokeWidth(taskVisualTokensService.taskStrokeWidth(criticalPathItem, selected));
        });
        installItemInteraction(barShape, itemRef, selected);
        installDateInteraction(barShape, anchor, itemRef, false);
        addResizeHandles(anchor, selected, false);

        chartPane.getChildren().add(barShape);
        return anchor;
    }

    private Rectangle drawMilestoneMarker(LocalDate date, double y, Color color, boolean selected, String tooltipText, Object itemRef) {
        if (date == null) {
            return null;
        }
        boolean criticalPathItem = isCriticalPathItem(itemRef);

        double centerX = snap(ChronoUnit.DAYS.between(startDate, date) * dayWidth + (dayWidth / 2.0));
        double centerY = snap(y + (rowHeight / 2.0));
        double half = Math.max(4.0, Math.min(9.0, dayWidth * 0.45));

        Polygon diamond = new Polygon(
                centerX, centerY - half,
                centerX + half, centerY,
                centerX, centerY + half,
                centerX - half, centerY
        );
        diamond.setFill(color);
        diamond.setStroke(taskVisualTokensService.milestoneStroke(criticalPathItem, selected));
        diamond.setStrokeWidth(taskVisualTokensService.milestoneStrokeWidth(criticalPathItem, selected));
        diamond.setSmooth(false);

        if (tooltipText != null && !tooltipText.isBlank()) {
            Tooltip.install(diamond, new Tooltip(ganttTextService.milestoneTooltip(tooltipText, date, TOOLTIP_FORMATTER)));
        }

        diamond.setOnMouseEntered(e -> {
            diamond.setStrokeWidth(taskVisualTokensService.milestoneHoverStrokeWidth(criticalPathItem, selected));
            diamond.toFront();
        });
        diamond.setOnMouseExited(e -> diamond.setStrokeWidth(taskVisualTokensService.milestoneStrokeWidth(criticalPathItem, selected)));
        installItemInteraction(diamond, itemRef, selected);

        chartPane.getChildren().add(diamond);

        double anchorWidth = Math.max(6.0, half * 1.8);
        double anchorHeight = Math.max(12.0, half * 1.8);
        Rectangle anchor = new Rectangle(centerX - anchorWidth / 2.0, centerY - anchorHeight / 2.0, anchorWidth, anchorHeight);
        installDateInteraction(diamond, anchor, itemRef, true);
        addResizeHandles(anchor, selected, true);
        return anchor;
    }

    private SVGPath createMainTaskBarShape(double x, double y, double width, double height) {
        SVGPath shape = new SVGPath();
        shape.setContent(mainTaskBarPathService.buildPath(x, y, width, height));
        return shape;
    }

    private void configureDragGuide(Line line, String color) {
        line.setVisible(false);
        line.setManaged(false);
        line.setMouseTransparent(true);
        line.setStroke(Color.web(color));
        line.setStrokeWidth(ganttThemeTokensService.dragGuideWidth());
        double[] dragPattern = ganttThemeTokensService.dragGuideDashPattern();
        line.getStrokeDashArray().setAll(dragPattern[0], dragPattern[1]);
        line.setOpacity(ganttThemeTokensService.dragGuideOpacity());
    }

    private void addResizeHandles(Rectangle anchor, boolean selected, boolean milestone) {
        if (anchor == null || !selected) {
            return;
        }

        double centerY = anchor.getY() + anchor.getHeight() / 2.0;
        if (milestone) {
            Rectangle handle = new Rectangle(anchor.getX() - 1.5, centerY - 7.0, anchor.getWidth() + 3.0, 14.0);
            handle.setArcWidth(8);
            handle.setArcHeight(8);
            handle.setFill(ganttThemeTokensService.milestoneResizeHandleFill());
            handle.setStroke(ganttThemeTokensService.milestoneResizeHandleStroke());
            handle.setStrokeWidth(ganttThemeTokensService.milestoneResizeHandleStrokeWidth());
            handle.setMouseTransparent(true);
            chartPane.getChildren().add(handle);
            return;
        }

        chartPane.getChildren().add(createResizeHandle(anchor.getX() - 3.0, centerY - 7.0));
        chartPane.getChildren().add(createResizeHandle(anchor.getX() + anchor.getWidth() - 3.0, centerY - 7.0));
    }

    private Rectangle createResizeHandle(double x, double y) {
        Rectangle handle = new Rectangle(x, y, 6.0, 14.0);
        handle.setArcWidth(6);
        handle.setArcHeight(6);
        handle.setFill(ganttThemeTokensService.resizeHandleFill());
        handle.setStroke(ganttThemeTokensService.resizeHandleStroke());
        handle.setStrokeWidth(ganttThemeTokensService.resizeHandleStrokeWidth());
        handle.setStyle(ganttThemeTokensService.resizeHandleEffect());
        handle.setMouseTransparent(true);
        return handle;
    }

    private void drawDependencyArrows(Map<String, Rectangle> taskBars, Map<String, Object> itemsByKey) {
        if (taskBars.isEmpty() || tasks == null) {
            return;
        }
        List<DependencySpecBuildService.DependencyRenderSpec> normalizedSpecs =
                dependencySpecBuildService.buildNormalizedSpecs(taskBars, itemsByKey, tasks);

        GanttDependencyPathService.LineStyle lineStyle = currentDependencyLineStyle();
        List<BoundingBox> dependencyBadgeBounds = new ArrayList<>();
        List<DependencyRoutingService.LaneReservation> laneReservations = new ArrayList<>();
        for (DependencySpecBuildService.DependencyRenderSpec spec : normalizedSpecs) {
            boolean highlighted = isCriticalPathEdge(spec.predecessorKey(), spec.targetKey());
            dependencyRenderService.renderOrthogonalDependency(
                    chartPane,
                    new DependencyRenderService.RenderRequest(
                            spec.target(),
                            spec.predecessorKey(),
                            spec.targetKey(),
                            spec.predecessorBar(),
                            spec.targetBar(),
                            spec.type(),
                            spec.lagDays(),
                            spec.critical(),
                            spec.dependency(),
                            lineStyle,
                            spec.sourceOffsetY(),
                            spec.targetOffsetY(),
                            rowHeight,
                            getChartRightLimit(),
                            Math.max(24.0, chartPane.getPrefHeight() - 12.0)
                    ),
                    highlighted,
                    colorByDependencyType(spec.type()),
                    dependencyBadgeBounds,
                    laneReservations,
                    dependencyRoutingService,
                    ganttDependencyPathService,
                    dependencyInteractionService,
                    dependencyBadgeLayoutService,
                    bounds -> overlapsExistingTaskBar(
                            bounds.getMinX(),
                            bounds.getMinY(),
                            bounds.getWidth(),
                            bounds.getHeight()
                    ),
                    dependencyEditHandler != null ? targetTask -> dependencyEditHandler.edit(targetTask, spec.dependency()) : null
            );
        }
    }
    private Color colorByDependencyType(String type) {
        return ganttPresentationService.colorByDependencyType(type, AppSettings.get());
    }

    private Color resolveTaskColor(Task task) {
        return ganttPresentationService.resolveTaskColor(task, AppSettings.get());
    }

    private Color resolveSubTaskColor() {
        return ganttPresentationService.resolveSubTaskColor(AppSettings.get());
    }

    private String buildTaskTooltip(Task task) {
        return ganttPresentationService.buildTaskTooltip(task, ganttDateService, TOOLTIP_FORMATTER);
    }

    private String buildSubTaskTooltip(SubTask subTask) {
        return ganttPresentationService.buildSubTaskTooltip(subTask, ganttDateService, TOOLTIP_FORMATTER);
    }
    private GanttDependencyPathService.LineStyle currentDependencyLineStyle() {
        return dependencyLineStyleService.resolve(AppSettings.get());
    }

    private void drawFinancialMarkers(Task task, double rowY) {
        for (FinancialMarkerSpecService.MarkerSpec spec : financialMarkerSpecService.buildSpecs(task)) {
            FinancialMarkerType markerType = markerTypeByCode(spec.code());
            if (markerType == null) {
                continue;
            }
            drawFinancialMarker(
                    task,
                    markerType,
                    spec.date(),
                    rowY,
                    spec.shortLabel(),
                    Color.web(spec.colorHex()),
                    spec.title(),
                    spec.blocking()
            );
        }
    }

    private FinancialMarkerType markerTypeByCode(String code) {
        if (code == null) {
            return null;
        }
        return switch (code.trim().toUpperCase(Locale.ROOT)) {
            case "ADVANCE" -> FinancialMarkerType.ADVANCE;
            case "KS" -> FinancialMarkerType.KS;
            default -> null;
        };
    }

    private void drawFinancialMarker(Task task, FinancialMarkerType markerType, LocalDate date, double rowY, String shortLabel, Color color, String title, boolean blocking) {
        if (date == null || date.isBefore(startDate) || date.isAfter(endDate)) {
            return;
        }

        double x = snap(financialMarkerRenderService.markerX(startDate, date, dayWidth));
        FinancialMarkerRenderService.MarkerGeometry geometry = financialMarkerRenderService.geometry(x, rowY, rowHeight);
        FinancialMarkerRenderService.BadgeMetrics badgeMetrics = financialMarkerRenderService.badgeMetrics(shortLabel);
        Point2D badgePosition = financialMarkerLayoutService.chooseBadgePosition(
                x,
                rowY,
                badgeMetrics.width(),
                badgeMetrics.height(),
                getChartRightLimit(),
                bounds -> overlapsExistingTaskBar(
                        bounds.getMinX(),
                        bounds.getMinY(),
                        bounds.getWidth(),
                        bounds.getHeight()
                )
        );
        double badgeX = badgePosition.getX();
        double badgeY = badgePosition.getY();

        Line glowLine = financialMarkerRenderService.createGlowLine(geometry, color, blocking);
        Line markerLine = financialMarkerRenderService.createMarkerLine(geometry, color, blocking);
        Circle headDot = financialMarkerRenderService.createHeadDot(geometry, rowY, color, blocking);
        StackPane badgePane = financialMarkerRenderService.createBadgePane(shortLabel, color, badgeX, badgeY, badgeMetrics);
        Rectangle hitHandle = financialMarkerRenderService.createHitHandle(geometry);

        Label accent = null;
        if (blocking) {
            accent = financialMarkerRenderService.createBlockingAccent(
                    getChartRightLimit(),
                    badgeX,
                    badgeY,
                    badgeMetrics,
                    rowY
            );
        }

        String tooltipText = financialMarkerRenderService.tooltipText(title, date, TOOLTIP_FORMATTER, blocking);
        Tooltip tooltip = new Tooltip(tooltipText);
        tooltip.setShowDelay(Duration.millis(250));
        Tooltip.install(markerLine, tooltip);
        Tooltip.install(headDot, tooltip);
        Tooltip.install(badgePane, tooltip);
        Tooltip.install(hitHandle, tooltip);
        if (accent != null) {
            Tooltip.install(accent, tooltip);
        }

        financialMarkerInteractionService.installDragHandlers(
                task,
                markerType,
                date,
                shortLabel,
                rowY,
                dayWidth,
                this::chartX,
                selectedTask -> {
                    if (itemSelectionHandler != null) {
                        itemSelectionHandler.select(selectedTask);
                    }
                },
                this::showFinancialMarkerDrag,
                () -> {
                    hideDragBadge();
                    hideDragGuides();
                },
                this::refresh,
                (selectedTask, selectedMarkerType, newDate, originalDate) -> {
                    if (financialDateChangeHandler != null) {
                        financialDateChangeHandler.change(selectedTask, selectedMarkerType, newDate);
                    } else {
                        refresh();
                    }
                },
                hitHandle, markerLine, headDot, badgePane, accent
        );

        chartPane.getChildren().addAll(glowLine, markerLine, headDot, hitHandle, badgePane);
        if (accent != null) {
            chartPane.getChildren().add(accent);
        }
        hitHandle.toFront();
        badgePane.toFront();
        headDot.toFront();
        if (accent != null) {
            accent.toFront();
        }
    }

    private boolean overlapsExistingTaskBar(double x, double y, double width, double height) {
        return ganttOverlapService.overlapsTaskBar(
                new BoundingBox(x, y, width, height),
                chartPane.getChildren(),
                dragStartGuide,
                dragEndGuide
        );
    }

    private void showFinancialMarkerDrag(String shortLabel, LocalDate date, double pointerX, double rowY) {
        dragInfoBadge.setText(ganttTextService.financialMarkerDragText(shortLabel, date, TOOLTIP_FORMATTER));
        dragInfoBadge.autosize();
        GanttDragOverlayService.BadgePosition badgePosition = ganttDragOverlayService.badgePosition(
                pointerX,
                rowY,
                dragInfoBadge.getWidth(),
                dragInfoBadge.getHeight(),
                getChartRightLimit()
        );
        dragInfoBadge.relocate(badgePosition.x(), badgePosition.y());
        dragInfoBadge.setVisible(true);
        dragInfoBadge.toFront();

        double markerX = snap(ganttDragOverlayService.markerGuideX(startDate, date, dayWidth));
        double height = Math.max(chartPane.getHeight(), chartPane.getPrefHeight());
        dragStartGuide.setStartX(markerX);
        dragStartGuide.setEndX(markerX);
        dragStartGuide.setStartY(0);
        dragStartGuide.setEndY(height);
        dragStartGuide.setVisible(true);
        dragStartGuide.toFront();
        dragEndGuide.setVisible(false);
    }

    private void installItemInteraction(Node node, Object item, boolean selected) {
        if (node == null || item == null) {
            return;
        }
        dependencyCreationInteractionService.installItemClickHandler(
                node,
                item,
                dependencyCreationMode,
                () -> pendingDependencySource,
                value -> pendingDependencySource = value,
                selectedItem -> {
                    if (itemSelectionHandler != null) {
                        itemSelectionHandler.select(selectedItem);
                    }
                },
                (source, target) -> {
                    if (dependencyCreateHandler != null) {
                        dependencyCreateHandler.create(source, target);
                    }
                },
                this::refresh
        );

        node.setStyle(dependencyCreationInteractionService.sourceHighlightStyle(
                dependencyCreationMode,
                selected,
                pendingDependencySource == item
        ));
    }

    private void installDateInteraction(Node node, Rectangle anchor, Object item, boolean milestone) {
        ganttItemDateInteractionService.installDateInteraction(
                node,
                anchor,
                item,
                milestone,
                dependencyCreationMode,
                dayWidth,
                this::chartX,
                this::itemStart,
                this::itemEnd,
                selectedItem -> {
                    if (itemSelectionHandler != null) {
                        itemSelectionHandler.select(selectedItem);
                    }
                },
                (session, newStart, newEnd, pointerX) -> {
                    applyDragPreview(session, newStart, newEnd);
                    showDragBadge(session, newStart, newEnd, pointerX, session.anchor().getY());
                },
                () -> {
                    hideDragBadge();
                    hideDragGuides();
                },
                (session, newStart, newEnd) -> {
                    if (itemDateChangeHandler != null
                            && ganttItemDateInteractionService.hasRangeChanged(
                            newStart,
                            newEnd,
                            session.originalStart(),
                            session.originalEnd())) {
                        itemDateChangeHandler.change(session.item(), newStart, newEnd);
                    } else {
                        refresh();
                    }
                }
        );
    }

    private double chartX(MouseEvent event) {
        return chartPane.sceneToLocal(event.getSceneX(), event.getSceneY()).getX();
    }


    private void applyDragPreview(GanttItemDateInteractionService.DragSession context, LocalDate newStart, LocalDate newEnd) {
        double x = snap(ChronoUnit.DAYS.between(startDate, newStart) * dayWidth);
        double width = Math.max(snap((ChronoUnit.DAYS.between(newStart, newEnd) + 1) * dayWidth), 5.0);

        if (context.node() instanceof Rectangle rect) {
            rect.setTranslateX(0);
            rect.setX(x);
            rect.setWidth(width);
            return;
        }
        if (context.node() instanceof SVGPath svgPath) {
            svgPath.setTranslateX(0);
            svgPath.setContent(mainTaskBarPathService.buildPath(x, context.anchor().getY(), width, context.anchor().getHeight()));
            return;
        }
        if (context.node() instanceof Polygon diamond) {
            double centerX = snap(ChronoUnit.DAYS.between(startDate, newEnd) * dayWidth + (dayWidth / 2.0));
            double centerY = context.anchor().getY() + context.anchor().getHeight() / 2.0;
            double half = Math.max(4.0, Math.min(9.0, dayWidth * 0.45));
            diamond.getPoints().setAll(
                    centerX, centerY - half,
                    centerX + half, centerY,
                    centerX, centerY + half,
                    centerX - half, centerY
            );
        }
    }

    private void showDragBadge(GanttItemDateInteractionService.DragSession context, LocalDate newStart, LocalDate newEnd, double pointerX, double anchorY) {
        dragInfoBadge.setText(ganttTextService.dragBadgeText(context, newStart, newEnd, TOOLTIP_FORMATTER));
        dragInfoBadge.autosize();
        GanttDragOverlayService.BadgePosition badgePosition = ganttDragOverlayService.badgePosition(
                pointerX,
                anchorY,
                dragInfoBadge.getWidth(),
                dragInfoBadge.getHeight(),
                getChartRightLimit()
        );
        dragInfoBadge.relocate(badgePosition.x(), badgePosition.y());
        dragInfoBadge.setVisible(true);
        dragInfoBadge.toFront();
        showDragGuides(newStart, newEnd);
    }

    private void hideDragBadge() {
        dragInfoBadge.setVisible(false);
    }

    private void showDragGuides(LocalDate newStart, LocalDate newEnd) {
        double height = Math.max(chartPane.getHeight(), chartPane.getPrefHeight());
        GanttDragOverlayService.GuideRange guideRange = ganttDragOverlayService.itemGuideRange(startDate, newStart, newEnd, dayWidth);
        double startX = snap(guideRange.startX());
        double endX = snap(guideRange.endX());

        dragStartGuide.setStartX(startX);
        dragStartGuide.setEndX(startX);
        dragStartGuide.setStartY(0);
        dragStartGuide.setEndY(height);
        dragStartGuide.setVisible(true);
        dragStartGuide.toFront();

        dragEndGuide.setStartX(endX);
        dragEndGuide.setEndX(endX);
        dragEndGuide.setStartY(0);
        dragEndGuide.setEndY(height);
        dragEndGuide.setVisible(true);
        dragEndGuide.toFront();
    }

    private void hideDragGuides() {
        dragStartGuide.setVisible(false);
        dragEndGuide.setVisible(false);
    }

    private LocalDate itemStart(Object item) {
        if (item instanceof Task task) {
            return task.getStart();
        }
        if (item instanceof SubTask subTask) {
            return subTask.getStart();
        }
        return null;
    }

    private LocalDate itemEnd(Object item) {
        if (item instanceof Task task) {
            return task.getEnd();
        }
        if (item instanceof SubTask subTask) {
            return subTask.getEnd();
        }
        return null;
    }

    @FunctionalInterface
    public interface DependencyEditHandler {
        void edit(Object targetTask, TaskDependency dependency);
    }

    @FunctionalInterface
    public interface DependencyCreateHandler {
        void create(Object sourceItem, Object targetItem);
    }

    @FunctionalInterface
    public interface ItemSelectionHandler {
        void select(Object item);
    }

    @FunctionalInterface
    public interface ItemDateChangeHandler {
        void change(Object item, LocalDate newStart, LocalDate newEnd);
    }

    @FunctionalInterface
    public interface FinancialDateChangeHandler {
        void change(Task task, FinancialMarkerType markerType, LocalDate newDate);
    }

    public double getHeaderHeight() {
        return headerBox.getHeight();
    }

    public Node getHeaderView() {
        return headerBox;
    }

    public Node getChartView() {
        return chartPane;
    }

    public void setChartPreferredHeight(double height) {
        chartPane.setMinHeight(height);
        chartPane.setPrefHeight(height);
    }

    public void setRowHeight(double rowHeight) {
        this.rowHeight = Math.max(20.0, rowHeight);
    }

    public void zoomIn() {
        forcedScaleMode = null;
        setDayWidth(dayWidth * 1.2);
    }

    public void zoomOut() {
        forcedScaleMode = null;
        setDayWidth(dayWidth * 0.8);
    }

    public void zoomToFit() {
        if (parentScrollPane == null || startDate == null || endDate == null) return;

        double viewportWidth = parentScrollPane.getViewportBounds().getWidth();
        long totalDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        if (totalDays <= 0) return;

        forcedScaleMode = null;
        setDayWidth(viewportWidth / totalDays);
    }

    public void zoomToWeek() {
        forcedScaleMode = ScaleMode.WEEK;
        setDayWidth(WEEK_PRESET_DAY_WIDTH);
    }

    public void zoomToMonth() {
        forcedScaleMode = ScaleMode.MONTH;
        setDayWidth(MONTH_PRESET_DAY_WIDTH);
    }

    public void zoomToQuarter() {
        forcedScaleMode = ScaleMode.QUARTER;
        setDayWidth(QUARTER_PRESET_DAY_WIDTH);
    }

    public void zoomToYear() {
        forcedScaleMode = ScaleMode.YEAR;
        setDayWidth(YEAR_PRESET_DAY_WIDTH);
    }

    public void zoomResetPreset() {
        forcedScaleMode = ScaleMode.DAY;
        setDayWidth(DEFAULT_DAY_WIDTH);
    }

    public int getZoomPercent() {
        return (int) ((dayWidth / DEFAULT_DAY_WIDTH) * 100);
    }

    public void setDayWidth(double value) {
        dayWidth = Math.max(MIN_DAY_WIDTH, Math.min(MAX_DAY_WIDTH, value));
        pendingDependencySource = null;
        ExportRenderEngine.setPreferredDayWidth(dayWidth);
        updateChart();
        requestLayout();
    }

    public double getDayWidth() {
        return dayWidth;
    }

    public void setShowDependencies(boolean showDependencies) {
        this.showDependencies = showDependencies;
        refresh();
    }

    public void setCriticalPathHighlight(CriticalPathAnalyzer.Analysis analysis) {
        criticalPathItemKeys.clear();
        criticalPathEdgeKeys.clear();
        if (analysis != null && analysis.steps() != null) {
            String previousKey = null;
            for (CriticalPathAnalyzer.PathStep step : analysis.steps()) {
                if (step == null || step.key() == null || step.key().isBlank()) {
                    continue;
                }
                String currentKey = step.key().trim();
                criticalPathItemKeys.add(currentKey);
                if (previousKey != null) {
                    criticalPathEdgeKeys.add(previousKey + "->" + currentKey);
                }
                previousKey = currentKey;
            }
        }
        refresh();
    }

    public void clearCriticalPathHighlight() {
        criticalPathItemKeys.clear();
        criticalPathEdgeKeys.clear();
        refresh();
    }

    public void setSelectedItem(Object selectedItem) {
        this.selectedItem = selectedItem;
        refresh();
    }

    public void setDependencyCreationMode(boolean dependencyCreationMode) {
        this.dependencyCreationMode = dependencyCreationMode;
        if (!dependencyCreationMode) {
            pendingDependencySource = null;
        }
        refresh();
    }

    private double getChartRightLimit() {
        double width = Math.max(
                chartPane.getWidth(),
                Math.max(chartPane.getLayoutBounds().getWidth(), chartPane.getPrefWidth())
        );
        return Math.max(12.0, width - 4.0);
    }

    private double getGridWidth() {
        return Math.max(chartPane.getPrefWidth(), getViewportWidth());
    }

    private double getViewportWidth() {
        if (parentScrollPane == null || parentScrollPane.getViewportBounds() == null) {
            return 0.0;
        }
        return Math.max(0.0, parentScrollPane.getViewportBounds().getWidth());
    }

    private double getViewportHeight() {
        if (parentScrollPane == null || parentScrollPane.getViewportBounds() == null) {
            return 0.0;
        }
        return Math.max(120.0, parentScrollPane.getViewportBounds().getHeight());
    }

    private double snap(double value) {
        return Math.round(value * 2.0) / 2.0;
    }

    private double snapToHorizontalGridLine(double y) {
        if (rowHeight <= 0.01) {
            return snap(y);
        }
        double gridY = Math.round(y / rowHeight) * rowHeight;
        return snap(gridY);
    }

    public void refresh() {
        updateChart();
    }

    private boolean isCriticalPathItem(Object item) {
        String key = itemKey(item);
        return key != null && criticalPathItemKeys.contains(key);
    }

    private boolean isCriticalPathEdge(String predecessorKey, String targetKey) {
        if (predecessorKey == null || targetKey == null) {
            return false;
        }
        return criticalPathEdgeKeys.contains(predecessorKey.trim() + "->" + targetKey.trim());
    }

    private String itemKey(Object item) {
        if (item instanceof Task task) {
            return task.getPosition();
        }
        if (item instanceof SubTask subTask) {
            return subTask.getSubPosition();
        }
        return null;
    }
}

