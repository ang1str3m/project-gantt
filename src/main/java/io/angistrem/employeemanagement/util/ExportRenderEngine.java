package io.angistrem.employeemanagement.util;

import io.angistrem.employeemanagement.controller.MainController;
import io.angistrem.employeemanagement.model.SubTask;
import io.angistrem.employeemanagement.model.Settings;
import io.angistrem.employeemanagement.model.Task;
import io.angistrem.employeemanagement.model.TaskDependency;
import io.angistrem.employeemanagement.view.GanttChart;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ExportRenderEngine {

    private enum ScaleMode {
        DAY,
        WEEK,
        MONTH,
        QUARTER,
        YEAR
    }

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final double ROW_HEIGHT = 24;
    private static final double TOP_HEADER_HEIGHT = 20;
    private static final double MIDDLE_HEADER_HEIGHT = 20;
    private static final double BOTTOM_HEADER_HEIGHT = 24;

    private static final double[] TABLE_WIDTHS = {34, 260, 110, 58, 58, 42, 62, 36, 60};
    private static final String[] HEADERS = {
            "№", "Наименование работ", "Исполнитель", "Начало", "Окончание",
            "Ед.", "Объём", "%", "Статус"
    };

    private static double preferredDayWidth = 30.0;

    private ExportRenderEngine() {
    }

    public static double getPreferredDayWidth() {
        return preferredDayWidth;
    }

    public static void setPreferredDayWidth(double value) {
        preferredDayWidth = Math.max(1.5, value);
    }

    public static WritableImage renderOverviewImage(List<Task> tasks, double totalWidth, double currentDayWidth) {
        VBox root = buildOverviewReport(tasks, totalWidth, currentDayWidth);
        return snapshot(root);
    }

    public static Node buildOverviewNode(List<Task> tasks, double totalWidth, double currentDayWidth) {
        return buildOverviewReport(tasks, totalWidth, currentDayWidth);
    }

    public static Node buildOverviewNode(MainController controller, double totalWidth) {
        if (controller == null || controller.getTasks() == null) {
            return new VBox();
        }
        double currentDayWidth = controller.getGanttChart() != null ? controller.getGanttChart().getDayWidth() : preferredDayWidth;
        return buildOverviewReport(controller, totalWidth, currentDayWidth);
    }

    private static VBox buildOverviewReport(List<Task> tasks, double totalWidth, double currentDayWidth) {
        List<RowData> rows = flattenRows(tasks);
        DateRange range = computeProjectRange(rows);
        ScaleMode scaleMode = detectScaleMode(currentDayWidth);

        double tableWidth = totalTableWidth();
        long totalDays = ChronoUnit.DAYS.between(range.start, range.end) + 1;
        double requestedGanttWidth = totalDays * Math.max(0.8, currentDayWidth);
        double targetGanttWidth = totalWidth > 0 ? Math.max(0, totalWidth - tableWidth - 24) : 0;
        double ganttWidth = targetGanttWidth > 0
                ? Math.max(280, targetGanttWidth)
                : Math.max(280, requestedGanttWidth);
        double renderDayWidth = totalDays > 0
                ? Math.max(0.8, ganttWidth / totalDays)
                : Math.max(0.8, currentDayWidth);
        totalWidth = tableWidth + 24 + ganttWidth;

        VBox root = new VBox(8);
        root.setPadding(new Insets(14));
        root.setStyle("-fx-background-color: white;");
        root.getChildren().add(buildBody(rows, tasks, range, ganttWidth, scaleMode, renderDayWidth));

        root.setMinWidth(totalWidth);
        root.setPrefWidth(totalWidth);
        root.setMaxWidth(totalWidth);
        root.setMinHeight(Region.USE_PREF_SIZE);

        return root;
    }

    private static VBox buildOverviewReport(MainController controller, double totalWidth, double currentDayWidth) {
        List<Task> taskList = controller.getTasks() == null ? List.of() : new ArrayList<>(controller.getTasks());
        List<RowData> rows = flattenRows(taskList);
        double tableWidth = totalTableWidth();

        WritableImage liveHeaderImage = snapshotLiveNode(controller.getGanttChart() == null ? null : controller.getGanttChart().getHeaderView());
        WritableImage liveChartImage = snapshotLiveNode(controller.getGanttChart() == null ? null : controller.getGanttChart().getChartView());
        double liveChartWidth = Math.max(
                280,
                Math.max(
                        liveHeaderImage == null ? 0 : liveHeaderImage.getWidth(),
                        liveChartImage == null ? 0 : liveChartImage.getWidth()
                )
        );
        totalWidth = tableWidth + 24 + liveChartWidth;

        VBox root = new VBox(8);
        root.setPadding(new Insets(14));
        root.setStyle("-fx-background-color: white;");
        root.getChildren().add(buildLiveBody(rows, liveHeaderImage, liveChartImage, liveChartWidth));

        root.setMinWidth(totalWidth);
        root.setPrefWidth(totalWidth);
        root.setMaxWidth(totalWidth);
        root.setMinHeight(Region.USE_PREF_SIZE);
        return root;
    }

    private static ScaleMode detectScaleMode(double dayWidth) {
        if (dayWidth >= 18) return ScaleMode.DAY;
        if (dayWidth >= 8) return ScaleMode.WEEK;
        if (dayWidth >= 3.5) return ScaleMode.MONTH;
        if (dayWidth >= 2.2) return ScaleMode.QUARTER;
        return ScaleMode.YEAR;
    }

    private static VBox buildTitle(List<Task> tasks) {
        LocalDate start = tasks.stream()
                .map(Task::getStart)
                .filter(d -> d != null)
                .min(LocalDate::compareTo)
                .orElse(LocalDate.now());

        LocalDate end = tasks.stream()
                .map(Task::getEnd)
                .filter(d -> d != null)
                .max(LocalDate::compareTo)
                .orElse(start);

        VBox box = new VBox(4);
        box.setPadding(new Insets(0, 0, 8, 0));
        box.setStyle("-fx-border-color: #cbd5e1; -fx-border-width: 0 0 2 0;");

        Label title = new Label("Общий план проекта");
        title.setFont(Font.font(16));
        title.setStyle("-fx-font-weight: bold;");

        HBox meta = new HBox(16);
        meta.getChildren().addAll(
                new Label("Период проекта: " + formatDate(start) + " — " + formatDate(end)),
                new Label("Дата: " + LocalDate.now().format(DATE_FORMATTER))
        );

        box.getChildren().addAll(title, meta);
        return box;
    }

    private static HBox buildBody(List<RowData> rows, List<Task> tasks, DateRange range, double ganttWidth, ScaleMode scaleMode, double currentDayWidth) {
        VBox table = buildTableBlock(rows);
        VBox gantt = buildGanttBlock(tasks, rows.size(), ganttWidth, currentDayWidth);

        HBox body = new HBox(12, table, gantt);
        body.setFillHeight(false);
        body.setMinWidth(totalTableWidth() + 12 + ganttWidth);
        body.setPrefWidth(totalTableWidth() + 12 + ganttWidth);
        body.setMaxWidth(totalTableWidth() + 12 + ganttWidth);
        HBox.setHgrow(gantt, Priority.NEVER);
        HBox.setHgrow(table, Priority.NEVER);
        return body;
    }

    private static HBox buildLiveBody(List<RowData> rows, WritableImage liveHeaderImage, WritableImage liveChartImage, double chartWidth) {
        VBox table = buildTableBlock(rows);
        VBox gantt = buildLiveGanttBlock(liveHeaderImage, liveChartImage, chartWidth);

        HBox body = new HBox(12, table, gantt);
        body.setFillHeight(false);
        body.setMinWidth(totalTableWidth() + 12 + chartWidth);
        body.setPrefWidth(totalTableWidth() + 12 + chartWidth);
        body.setMaxWidth(totalTableWidth() + 12 + chartWidth);
        HBox.setHgrow(gantt, Priority.NEVER);
        HBox.setHgrow(table, Priority.NEVER);
        return body;
    }

    private static VBox buildTableBlock(List<RowData> rows) {
        VBox box = new VBox();
        double width = totalTableWidth();
        box.setMinWidth(width);
        box.setPrefWidth(width);
        box.setMaxWidth(width);
        box.setStyle("-fx-border-color: #94a3b8; -fx-border-width: 1; -fx-background-color: white;");

        box.getChildren().add(buildTableHeader(TOP_HEADER_HEIGHT + MIDDLE_HEADER_HEIGHT + BOTTOM_HEADER_HEIGHT));
        for (RowData row : rows) {
            box.getChildren().add(buildTableRow(row));
        }
        return box;
    }

    private static VBox buildGanttBlock(List<Task> tasks, int rowCount, double targetWidth, double currentDayWidth) {
        VBox wrapper = new VBox();
        wrapper.setMinWidth(targetWidth);
        wrapper.setPrefWidth(targetWidth);
        wrapper.setMaxWidth(targetWidth);
        wrapper.setStyle("-fx-border-color: #94a3b8; -fx-border-width: 1; -fx-background-color: white;");

        GanttChart chart = new GanttChart();
        ObservableList<Task> observableTasks = FXCollections.observableArrayList(tasks);
        chart.setMinWidth(targetWidth);
        chart.setPrefWidth(targetWidth);
        chart.setMaxWidth(targetWidth);
        chart.setTasks(observableTasks);
        chart.setDayWidth(Math.max(1.5, currentDayWidth));
        chart.refresh();

        Group layoutRoot = new Group(chart);
        new Scene(layoutRoot);
        layoutRoot.applyCss();
        layoutRoot.layout();
        chart.applyCss();
        chart.layout();

        double headerHeight = chart.getHeaderHeight();
        if (headerHeight <= 0) {
            headerHeight = 64;
        }
        double totalHeight = headerHeight + (rowCount * ROW_HEIGHT);
        chart.setMinHeight(totalHeight);
        chart.setPrefHeight(totalHeight);
        chart.setMaxHeight(totalHeight);
        chart.refresh();
        chart.applyCss();
        chart.layout();

        wrapper.getChildren().add(chart);
        return wrapper;
    }

    private static VBox buildLiveGanttBlock(WritableImage headerImage, WritableImage chartImage, double width) {
        VBox wrapper = new VBox();
        wrapper.setMinWidth(width);
        wrapper.setPrefWidth(width);
        wrapper.setMaxWidth(width);
        wrapper.setStyle("-fx-border-color: #94a3b8; -fx-border-width: 1; -fx-background-color: white;");

        if (headerImage == null && chartImage == null) {
            Pane placeholder = new Pane();
            placeholder.setMinSize(width, 240);
            placeholder.setPrefSize(width, 240);
            placeholder.setStyle("-fx-background-color: white;");
            wrapper.getChildren().add(placeholder);
            return wrapper;
        }

        if (headerImage != null) {
            ImageView headerView = new ImageView(headerImage);
            headerView.setPreserveRatio(false);
            headerView.setFitWidth(width);
            headerView.setFitHeight(headerImage.getHeight());
            wrapper.getChildren().add(headerView);
        }

        if (chartImage != null) {
            ImageView chartView = new ImageView(chartImage);
            chartView.setPreserveRatio(false);
            chartView.setFitWidth(width);
            chartView.setFitHeight(chartImage.getHeight());
            wrapper.getChildren().add(chartView);
        }
        return wrapper;
    }

    private static HBox buildTableHeader(double headerHeight) {
        HBox header = new HBox();
        for (int i = 0; i < HEADERS.length; i++) {
            header.getChildren().add(
                    makeCell(HEADERS[i], TABLE_WIDTHS[i], headerHeight, "#dbe7f3", true, Pos.CENTER)
            );
        }
        return header;
    }

    private static HBox buildTableRow(RowData row) {
        String bg = row.subTask ? "#ffffff" : "#f8fafc";

        return new HBox(
                makeCell(row.position, TABLE_WIDTHS[0], ROW_HEIGHT, bg, false, Pos.CENTER),
                makeCell(row.name, TABLE_WIDTHS[1], ROW_HEIGHT, bg, !row.subTask, Pos.CENTER_LEFT),
                makeCell(row.company, TABLE_WIDTHS[2], ROW_HEIGHT, bg, false, Pos.CENTER_LEFT),
                makeCell(formatDate(row.start), TABLE_WIDTHS[3], ROW_HEIGHT, bg, false, Pos.CENTER),
                makeCell(formatDate(row.end), TABLE_WIDTHS[4], ROW_HEIGHT, bg, false, Pos.CENTER),
                makeCell(row.unit, TABLE_WIDTHS[5], ROW_HEIGHT, bg, false, Pos.CENTER),
                makeCell(row.volume, TABLE_WIDTHS[6], ROW_HEIGHT, bg, false, Pos.CENTER_RIGHT),
                makeCell(row.percentText, TABLE_WIDTHS[7], ROW_HEIGHT, bg, false, Pos.CENTER),
                makeCell(row.status, TABLE_WIDTHS[8], ROW_HEIGHT, bg, false, Pos.CENTER)
        );
    }

    private static VBox buildGanttBlock(List<RowData> rows, DateRange range, double targetWidth, ScaleMode scaleMode) {
        VBox gantt = new VBox();
        gantt.setMinWidth(targetWidth);
        gantt.setPrefWidth(targetWidth);
        gantt.setMaxWidth(targetWidth);
        gantt.setStyle("-fx-border-color: #94a3b8; -fx-border-width: 1; -fx-background-color: white;");

        long totalDays = ChronoUnit.DAYS.between(range.start, range.end) + 1;
        double dayWidth = targetWidth / Math.max(totalDays, 1);

        gantt.getChildren().add(buildYearHeader(range, dayWidth));

        switch (scaleMode) {
            case DAY, WEEK -> gantt.getChildren().add(buildMonthHeader(range, dayWidth));
            case MONTH, QUARTER -> gantt.getChildren().add(buildQuarterHeader(range, dayWidth));
            case YEAR -> gantt.getChildren().add(buildHalfYearHeader(range, dayWidth));
        }

        switch (scaleMode) {
            case DAY -> gantt.getChildren().add(buildDayHeader(range, dayWidth));
            case WEEK -> gantt.getChildren().add(buildWeekHeader(range, dayWidth));
            case MONTH -> gantt.getChildren().add(buildMonthShortHeader(range, dayWidth));
            case QUARTER, YEAR -> { }
        }

        Pane pane = new Pane();
        pane.setMinWidth(targetWidth);
        pane.setPrefWidth(targetWidth);
        pane.setMaxWidth(targetWidth);
        pane.setMinHeight(rows.size() * ROW_HEIGHT);
        pane.setPrefHeight(rows.size() * ROW_HEIGHT);
        pane.setStyle("-fx-background-color: white;");

        drawGrid(pane, rows.size(), range, dayWidth, targetWidth, scaleMode);
        drawBars(pane, rows, range, dayWidth, targetWidth);

        gantt.getChildren().add(pane);
        return gantt;
    }

    private static HBox buildYearHeader(DateRange range, double dayWidth) {
        HBox box = new HBox();
        LocalDate cursor = range.start;

        while (!cursor.isAfter(range.end)) {
            LocalDate yearEnd = LocalDate.of(cursor.getYear(), 12, 31);
            if (yearEnd.isAfter(range.end)) {
                yearEnd = range.end;
            }

            long days = ChronoUnit.DAYS.between(cursor, yearEnd) + 1;
            box.getChildren().add(
                    makeCell(String.valueOf(cursor.getYear()), days * dayWidth, TOP_HEADER_HEIGHT, "#dde7f3", true, Pos.CENTER)
            );
            cursor = yearEnd.plusDays(1);
        }

        return box;
    }

    private static HBox buildMonthHeader(DateRange range, double dayWidth) {
        HBox box = new HBox();
        LocalDate cursor = range.start;

        while (!cursor.isAfter(range.end)) {
            LocalDate monthEnd = YearMonth.from(cursor).atEndOfMonth();
            if (monthEnd.isAfter(range.end)) {
                monthEnd = range.end;
            }

            long days = ChronoUnit.DAYS.between(cursor, monthEnd) + 1;
            String text = cursor.getMonth().getDisplayName(TextStyle.FULL_STANDALONE, Locale.forLanguageTag("ru"));
            text = text.substring(0, 1).toUpperCase(Locale.ROOT) + text.substring(1);

            box.getChildren().add(
                    makeCell(text, days * dayWidth, MIDDLE_HEADER_HEIGHT, "#edf3f8", true, Pos.CENTER)
            );
            cursor = monthEnd.plusDays(1);
        }

        return box;
    }

    private static HBox buildQuarterHeader(DateRange range, double dayWidth) {
        HBox box = new HBox();
        LocalDate cursor = range.start;

        while (!cursor.isAfter(range.end)) {
            int quarter = ((cursor.getMonthValue() - 1) / 3) + 1;
            int quarterEndMonth = quarter * 3;
            LocalDate quarterEnd = LocalDate.of(
                    cursor.getYear(),
                    quarterEndMonth,
                    YearMonth.of(cursor.getYear(), quarterEndMonth).lengthOfMonth()
            );
            if (quarterEnd.isAfter(range.end)) {
                quarterEnd = range.end;
            }

            long days = ChronoUnit.DAYS.between(cursor, quarterEnd) + 1;
            box.getChildren().add(
                    makeCell(quarter + " кв.", days * dayWidth, MIDDLE_HEADER_HEIGHT, "#edf3f8", true, Pos.CENTER)
            );
            cursor = quarterEnd.plusDays(1);
        }

        return box;
    }

    private static HBox buildHalfYearHeader(DateRange range, double dayWidth) {
        HBox box = new HBox();
        LocalDate cursor = range.start;

        while (!cursor.isAfter(range.end)) {
            int half = cursor.getMonthValue() <= 6 ? 1 : 2;
            LocalDate blockEnd = LocalDate.of(cursor.getYear(), half == 1 ? 6 : 12, half == 1 ? 30 : 31);
            if (blockEnd.isAfter(range.end)) {
                blockEnd = range.end;
            }

            long days = ChronoUnit.DAYS.between(cursor, blockEnd) + 1;
            box.getChildren().add(
                    makeCell(half + " полугодие", days * dayWidth, MIDDLE_HEADER_HEIGHT, "#edf3f8", true, Pos.CENTER)
            );
            cursor = blockEnd.plusDays(1);
        }

        return box;
    }

    private static HBox buildDayHeader(DateRange range, double dayWidth) {
        HBox box = new HBox();
        LocalDate cursor = range.start;

        while (!cursor.isAfter(range.end)) {
            String bg = isWeekend(cursor) ? "#fef2f2" : "#ffffff";
            box.getChildren().add(
                    makeCell(
                            cursor.getDayOfMonth() + "\n" + shortDayName(cursor.getDayOfWeek()),
                            Math.max(dayWidth, 1),
                            BOTTOM_HEADER_HEIGHT,
                            bg,
                            false,
                            Pos.CENTER
                    )
            );
            cursor = cursor.plusDays(1);
        }

        return box;
    }

    private static HBox buildWeekHeader(DateRange range, double dayWidth) {
        HBox box = new HBox();
        LocalDate cursor = range.start;

        while (!cursor.isAfter(range.end)) {
            LocalDate weekEnd = cursor.plusDays(6);
            if (weekEnd.isAfter(range.end)) {
                weekEnd = range.end;
            }

            long days = ChronoUnit.DAYS.between(cursor, weekEnd) + 1;
            int week = cursor.get(ChronoField.ALIGNED_WEEK_OF_YEAR);

            box.getChildren().add(
                    makeCell("Нед. " + week, days * dayWidth, BOTTOM_HEADER_HEIGHT, "#ffffff", false, Pos.CENTER)
            );
            cursor = weekEnd.plusDays(1);
        }

        return box;
    }

    private static HBox buildMonthShortHeader(DateRange range, double dayWidth) {
        HBox box = new HBox();
        LocalDate cursor = range.start;

        while (!cursor.isAfter(range.end)) {
            LocalDate monthEnd = YearMonth.from(cursor).atEndOfMonth();
            if (monthEnd.isAfter(range.end)) {
                monthEnd = range.end;
            }

            long days = ChronoUnit.DAYS.between(cursor, monthEnd) + 1;
            String text = cursor.getMonth().getDisplayName(TextStyle.SHORT_STANDALONE, Locale.forLanguageTag("ru"));
            text = text.substring(0, 1).toUpperCase(Locale.ROOT) + text.substring(1);

            box.getChildren().add(
                    makeCell(text, days * dayWidth, BOTTOM_HEADER_HEIGHT, "#ffffff", false, Pos.CENTER)
            );
            cursor = monthEnd.plusDays(1);
        }

        return box;
    }

    private static void drawGrid(Pane pane, int rowsCount, DateRange range, double dayWidth, double targetWidth, ScaleMode scaleMode) {
        LocalDate today = LocalDate.now();

        if (scaleMode == ScaleMode.DAY) {
            long totalDays = ChronoUnit.DAYS.between(range.start, range.end) + 1;

            for (int i = 0; i <= totalDays; i++) {
                LocalDate date = range.start.plusDays(i);
                double x = Math.min(i * dayWidth, targetWidth);

                if (i < totalDays && isWeekend(date)) {
                    Rectangle weekend = new Rectangle(x, 0, dayWidth, rowsCount * ROW_HEIGHT);
                    weekend.setFill(Color.web("#fefbf3"));
                    pane.getChildren().add(weekend);
                }

                Line line = new Line(x, 0, x, rowsCount * ROW_HEIGHT);
                if (i < totalDays && date.equals(today)) {
                    line.setStroke(Color.web("#ef4444"));
                    line.setStrokeWidth(1.8);
                } else if (i < totalDays && date.getDayOfMonth() == 1) {
                    line.setStroke(Color.web("#94a3b8"));
                    line.setStrokeWidth(1.0);
                } else {
                    line.setStroke(Color.web("#e2e8f0"));
                    line.setStrokeWidth(0.6);
                }
                pane.getChildren().add(line);
            }
        } else if (scaleMode == ScaleMode.WEEK) {
            LocalDate cursor = range.start;
            while (!cursor.isAfter(range.end)) {
                double x = ChronoUnit.DAYS.between(range.start, cursor) * dayWidth;
                Line line = new Line(x, 0, x, rowsCount * ROW_HEIGHT);

                if (cursor.equals(today) || (cursor.isBefore(today) && cursor.plusDays(6).isAfter(today))) {
                    line.setStroke(Color.web("#ef4444"));
                    line.setStrokeWidth(1.8);
                } else if (cursor.getDayOfMonth() <= 7) {
                    line.setStroke(Color.web("#94a3b8"));
                    line.setStrokeWidth(1.0);
                } else {
                    line.setStroke(Color.web("#dbe4ef"));
                    line.setStrokeWidth(0.8);
                }

                pane.getChildren().add(line);
                cursor = cursor.plusWeeks(1);
            }
        } else if (scaleMode == ScaleMode.MONTH) {
            LocalDate cursor = range.start.withDayOfMonth(1);
            if (cursor.isAfter(range.start)) {
                cursor = range.start;
            }

            while (!cursor.isAfter(range.end)) {
                double x = ChronoUnit.DAYS.between(range.start, cursor) * dayWidth;
                Line line = new Line(x, 0, x, rowsCount * ROW_HEIGHT);

                if (cursor.getMonthValue() == 1) {
                    line.setStroke(Color.web("#64748b"));
                    line.setStrokeWidth(1.2);
                } else {
                    line.setStroke(Color.web("#94a3b8"));
                    line.setStrokeWidth(0.9);
                }

                pane.getChildren().add(line);
                cursor = cursor.plusMonths(1).withDayOfMonth(1);
            }
        } else {
            LocalDate cursor = range.start.withDayOfMonth(1);
            if (cursor.isAfter(range.start)) {
                cursor = range.start;
            }

            while (!cursor.isAfter(range.end)) {
                double x = ChronoUnit.DAYS.between(range.start, cursor) * dayWidth;
                Line line = new Line(x, 0, x, rowsCount * ROW_HEIGHT);

                boolean quarterStart = cursor.getMonthValue() == 1 || cursor.getMonthValue() == 4
                        || cursor.getMonthValue() == 7 || cursor.getMonthValue() == 10;

                if (cursor.getMonthValue() == 1) {
                    line.setStroke(Color.web("#475569"));
                    line.setStrokeWidth(1.4);
                } else if (quarterStart) {
                    line.setStroke(Color.web("#94a3b8"));
                    line.setStrokeWidth(1.0);
                } else if (scaleMode == ScaleMode.QUARTER) {
                    line.setStroke(Color.web("#d8e0ea"));
                    line.setStrokeWidth(0.7);
                } else {
                    cursor = cursor.plusMonths(3).withDayOfMonth(1);
                    pane.getChildren().add(line);
                    continue;
                }

                pane.getChildren().add(line);
                cursor = cursor.plusMonths(1).withDayOfMonth(1);
            }
        }

        for (int i = 0; i <= rowsCount; i++) {
            double y = i * ROW_HEIGHT;
            Line line = new Line(0, y, targetWidth, y);
            line.setStroke(Color.web("#f1f5f9"));
            line.setStrokeWidth(0.4);
            pane.getChildren().add(line);
        }
    }

    private static void drawBars(Pane pane, List<RowData> rows, DateRange range, double dayWidth, double targetWidth) {
        Map<Task, double[]> taskBars = new HashMap<>();

        for (int i = 0; i < rows.size(); i++) {
            RowData row = rows.get(i);
            if (row.start == null || row.end == null) continue;

            long offset = ChronoUnit.DAYS.between(range.start, row.start);
            long duration = ChronoUnit.DAYS.between(row.start, row.end) + 1;

            double x = Math.max(0, offset * dayWidth);
            double width = Math.max(duration * dayWidth, 1.4);

            if (x > targetWidth) continue;
            if (x + width > targetWidth) {
                width = Math.max(1.0, targetWidth - x);
            }

            double height = row.subTask ? 7 : 10;
            double y = i * ROW_HEIGHT + (ROW_HEIGHT - height) / 2.0;

            Rectangle bar = new Rectangle(x, y, width, height);
            bar.setArcWidth(4);
            bar.setArcHeight(4);
            bar.setFill(statusColor(row.status));
            bar.setStroke(Color.web("#334155"));
            bar.setStrokeWidth(row.subTask ? 0.25 : 0.4);
            pane.getChildren().add(bar);

            if (row.percentValue > 0 && width > 4) {
                Rectangle progress = new Rectangle(
                        x,
                        y,
                        Math.max(1.2, width * (row.percentValue / 100.0)),
                        height
                );
                progress.setArcWidth(4);
                progress.setArcHeight(4);
                progress.setFill(Color.rgb(255, 255, 255, 0.28));
                pane.getChildren().add(progress);
            }

            if (!row.subTask && row.taskRef != null) {
                taskBars.put(row.taskRef, new double[]{x, x + width, y + (height / 2.0)});
            }
        }

        drawDependencies(pane, taskBars);
    }

    private static void drawDependencies(Pane pane, Map<Task, double[]> taskBars) {
        if (taskBars.isEmpty()) {
            return;
        }

        int connectionIndex = 0;
        for (Map.Entry<Task, double[]> entry : taskBars.entrySet()) {
            Task target = entry.getKey();
            double[] to = entry.getValue();

            if (target.getDependencies() == null || target.getDependencies().isEmpty()) {
                continue;
            }

            for (TaskDependency dependency : target.getDependencies()) {
                if (dependency == null || dependency.getPredecessorPosition() == null) {
                    continue;
                }

                Task predecessor = findTaskByPosition(taskBars.keySet(), dependency.getPredecessorPosition().trim());
                if (predecessor == null) {
                    continue;
                }
                double[] from = taskBars.get(predecessor);
                if (from == null) {
                    continue;
                }

                String type = dependency.getType() == null ? "FS" : dependency.getType().toUpperCase(Locale.ROOT);
                boolean fromRight = "FS".equals(type) || "FF".equals(type);
                boolean toRight = "FF".equals(type) || "SF".equals(type);

                double sourceEdgeX = fromRight ? from[1] : from[0];
                double targetEdgeX = toRight ? to[1] : to[0];
                double startX = sourceEdgeX + (fromRight ? 3.0 : -3.0);
                double startY = from[2];

                double arrowSize = 6.0;
                double tipX = targetEdgeX + (toRight ? 6.0 : -6.0);
                double tipY = to[2];
                double lineEndX = toRight ? tipX - (arrowSize + 1.0) : tipX + (arrowSize + 1.0);

                double laneOffset = ((connectionIndex % 7) - 3) * 2.5;
                if (Math.abs(tipY - startY) < 3.0) {
                    laneOffset += (connectionIndex % 2 == 0 ? -8.0 : 8.0);
                }
                double y1 = startY + laneOffset;
                double y2 = tipY + laneOffset;
                double midX = toRight
                        ? Math.max(Math.max(startX, lineEndX) + 12.0, startX + 8.0)
                        : Math.min(Math.min(startX, lineEndX) - 12.0, startX - 8.0);

                Color color = target.isCriticalDependency() ? Color.web("#b91c1c") : dependencyColor(type);

                Line halo1 = new Line(startX, startY, midX, y1);
                Line halo2 = new Line(midX, y1, midX, y2);
                Line halo3 = new Line(midX, y2, lineEndX, tipY);
                styleLine(halo1, Color.rgb(255, 255, 255, 0.92), 2.6);
                styleLine(halo2, Color.rgb(255, 255, 255, 0.92), 2.6);
                styleLine(halo3, Color.rgb(255, 255, 255, 0.92), 2.6);

                Line line1 = new Line(startX, startY, midX, y1);
                Line line2 = new Line(midX, y1, midX, y2);
                Line line3 = new Line(midX, y2, lineEndX, tipY);
                styleLine(line1, color, 1.05);
                styleLine(line2, color, 1.05);
                styleLine(line3, color, 1.05);

                Polygon arrow = buildArrowHead(tipX, tipY, toRight, arrowSize);
                arrow.setFill(color);

                pane.getChildren().addAll(halo1, halo2, halo3, line1, line2, line3, arrow);
                connectionIndex++;
            }
        }
    }

    private static Task findTaskByPosition(Iterable<Task> tasks, String position) {
        for (Task task : tasks) {
            if (position.equals(task.getPosition())) {
                return task;
            }
        }
        return null;
    }

    private static Color dependencyColor(String type) {
        return switch (type) {
            case "SS" -> Color.web("#2563eb");
            case "FF" -> Color.web("#0f766e");
            case "SF" -> Color.web("#b45309");
            default -> Color.web("#64748b");
        };
    }

    private static void styleLine(Line line, Color color, double width) {
        line.setStroke(color);
        line.setStrokeWidth(width);
        line.setSmooth(true);
    }

    private static Polygon buildArrowHead(double xTip, double yTip, boolean pointRight, double size) {
        double backX = pointRight ? xTip - size : xTip + size;
        return new Polygon(
                xTip, yTip,
                backX, yTip - (size * 0.6),
                backX, yTip + (size * 0.6)
        );
    }

    private static Color statusColor(String status) {
        if (status == null) return Color.web("#94a3b8");
        String s = status.toLowerCase(Locale.ROOT);
        if (s.contains("заверш")) return Color.web("#16a34a");
        if (s.contains("проср")) return Color.web("#dc2626");
        if (s.contains("работ")) return Color.web("#2563eb");
        return Color.web("#64748b");
    }

    private static List<RowData> flattenRows(List<Task> tasks) {
        List<RowData> rows = new ArrayList<>();
        for (Task task : tasks) {
            rows.add(RowData.forTask(task));
            for (SubTask subTask : task.getObservableSubTasks()) {
                rows.add(RowData.forSubTask(subTask));
            }
        }
        return rows;
    }

    private static DateRange computeProjectRange(List<RowData> rows) {
        LocalDate min = rows.stream()
                .map(r -> r.start)
                .filter(d -> d != null)
                .min(LocalDate::compareTo)
                .orElse(LocalDate.now());

        LocalDate max = rows.stream()
                .map(r -> r.end)
                .filter(d -> d != null)
                .max(LocalDate::compareTo)
                .orElse(min.plusMonths(1));

        LocalDate paddedEnd = max.plusDays(2);
        LocalDate extendedEnd = paddedEnd.plusMonths(1).withDayOfMonth(paddedEnd.plusMonths(1).lengthOfMonth());
        return new DateRange(min.minusDays(2), extendedEnd);
    }

    private static double totalTableWidth() {
        double sum = 0;
        for (double w : TABLE_WIDTHS) sum += w;
        return sum;
    }

    private static Label makeCell(String text, double width, double height, String bg, boolean bold, Pos pos) {
        Label label = new Label(text == null ? "" : text);
        label.setWrapText(true);
        label.setAlignment(pos);
        label.setPadding(new Insets(1, 3, 1, 3));
        label.setMinWidth(width);
        label.setPrefWidth(width);
        label.setMaxWidth(width);
        label.setMinHeight(height);
        label.setPrefHeight(height);
        label.setMaxHeight(height);
        label.setStyle(
                "-fx-font-size: 9px;" +
                        (bold ? "-fx-font-weight: bold;" : "") +
                        "-fx-background-color: " + bg + ";" +
                        "-fx-border-color: #cbd5e1; -fx-border-width: 0 1 1 0;"
        );
        return label;
    }

    private static String shortDayName(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> "Пн";
            case TUESDAY -> "Вт";
            case WEDNESDAY -> "Ср";
            case THURSDAY -> "Чт";
            case FRIDAY -> "Пт";
            case SATURDAY -> "Сб";
            case SUNDAY -> "Вс";
        };
    }

    private static WritableImage snapshot(Node node) {
        if (node == null) {
            return null;
        }

        boolean attachedToScene = node.getScene() != null;
        if (!attachedToScene) {
            if (node instanceof Region region) {
                region.applyCss();
                region.autosize();
            }

            Group group = new Group(node);
            new Scene(group);
            group.applyCss();
            group.layout();
        }

        double width = Math.ceil(Math.max(
                node.getLayoutBounds().getWidth(),
                node.getBoundsInLocal().getWidth()
        ));
        double height = Math.ceil(Math.max(
                node.getLayoutBounds().getHeight(),
                node.getBoundsInLocal().getHeight()
        ));

        if (width < 1) width = 1;
        if (height < 1) height = 1;

        SnapshotParameters parameters = new SnapshotParameters();
        parameters.setFill(Color.WHITE);

        WritableImage img = new WritableImage((int) width, (int) height);
        node.snapshot(parameters, img);
        return img;
    }

    private static WritableImage snapshotLiveNode(Node node) {
        return snapshot(node);
    }

    private static String formatDate(LocalDate date) {
        return date == null ? "" : date.format(DATE_FORMATTER);
    }

    private static boolean isWeekend(LocalDate date) {
        return date.getDayOfWeek().getValue() >= 6;
    }

    private record DateRange(LocalDate start, LocalDate end) {}

    private static final class RowData {
        final boolean subTask;
        final Task taskRef;
        final String position;
        final String name;
        final String company;
        final LocalDate start;
        final LocalDate end;
        final String unit;
        final String volume;
        final double percentValue;
        final String percentText;
        final String status;

        private RowData(boolean subTask, Task taskRef, String position, String name, String company,
                        LocalDate start, LocalDate end, String unit, String volume,
                        double percentValue, String percentText, String status) {
            this.subTask = subTask;
            this.taskRef = taskRef;
            this.position = position;
            this.name = name;
            this.company = company;
            this.start = start;
            this.end = end;
            this.unit = unit;
            this.volume = volume;
            this.percentValue = percentValue;
            this.percentText = percentText;
            this.status = status;
        }

        static RowData forTask(Task task) {
            return new RowData(
                    false,
                    task,
                    task.getPosition(),
                    task.getName(),
                    task.getCompany(),
                    task.getStart(),
                    task.getEnd(),
                    "",
                    "",
                    task.getPercent(),
                    String.format(Locale.US, "%.0f%%", task.getPercent()),
                    task.getStatus()
            );
        }

        static RowData forSubTask(SubTask subTask) {
            return new RowData(
                    true,
                    subTask.getParentTask(),
                    subTask.getSubPosition(),
                    "   " + subTask.getName(),
                    subTask.getCompany(),
                    subTask.getStart(),
                    subTask.getEnd(),
                    subTask.getUnitOfMeasurement(),
                    subTask.getVolumeActual() + " / " + subTask.getVolumeProject(),
                    subTask.getPercent(),
                    String.format(Locale.US, "%.0f%%", subTask.getPercent()),
                    subTask.getStatus()
            );
        }
    }
}
