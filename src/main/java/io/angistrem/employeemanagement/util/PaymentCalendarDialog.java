package io.angistrem.employeemanagement.util;

import io.angistrem.employeemanagement.model.BudgetItem;
import io.angistrem.employeemanagement.model.Task;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class PaymentCalendarDialog {
    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("LLLL yyyy", new Locale("ru"));

    private PaymentCalendarDialog() {
    }

    public static void show(Window owner, ObservableList<BudgetItem> budgetItems, List<Task> topLevelTasks) {
        boolean dark = isDarkTheme();
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Платёжный календарь");
        dialog.setHeaderText(null);
        dialog.setGraphic(null);
        if (owner != null) {
            dialog.initOwner(owner);
        }

        List<MonthBucket> buckets = buildBuckets(budgetItems);
        List<StageSnapshot> stageSnapshots = buildStageSnapshots(topLevelTasks, budgetItems);

        VBox content = new VBox(14,
                headerCard(dark, buckets),
                summaryCard(dark, buckets),
                stageSection(dark, stageSnapshots),
                monthSection(dark, buckets)
        );
        content.setPadding(new Insets(8, 10, 4, 10));
        content.setPrefWidth(980);
        content.setFillWidth(true);

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setPannable(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        dialog.getDialogPane().setContent(scrollPane);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
        dialog.getDialogPane().setPrefSize(1040, 860);
        SceneManager.applyThemeToDialog(dialog);
        dialog.showAndWait();
    }

    private static VBox headerCard(boolean dark, List<MonthBucket> buckets) {
        Label title = new Label("Платёжный календарь");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: 900; -fx-text-fill: " + color(dark, "#ffffff", "#0f172a") + ";");

        String description = buckets.isEmpty()
                ? "Пока нет бюджетных статей с датами. Сначала добавьте статьи бюджета и плановые или фактические даты платежей."
                : "Календарь показывает, в какие месяцы у проекта приходится основной объём плановых и фактических платежей, чтобы видеть пики нагрузки по деньгам.";

        Label subtitle = new Label(description);
        applyWrappedTextStyle(subtitle, "-fx-font-size: 13px; -fx-line-spacing: 3px; -fx-text-fill: "
                + color(dark, "#eff6ff", "#1e293b") + ";");

        VBox box = new VBox(8, title, subtitle);
        box.setPadding(new Insets(18));
        box.setStyle("-fx-background-color: "
                + color(dark, "linear-gradient(to right, #0f172a, #0f766e)", "linear-gradient(to right, #dbeafe, #ecfeff)") + ";"
                + "-fx-background-radius: 18; -fx-border-radius: 18;");
        return box;
    }

    private static HBox summaryCard(boolean dark, List<MonthBucket> buckets) {
        double planned = buckets.stream().mapToDouble(MonthBucket::planned).sum();
        double actual = buckets.stream().mapToDouble(MonthBucket::actual).sum();
        double peak = buckets.stream().mapToDouble(MonthBucket::maxMonth).max().orElse(0.0);

        return new HBox(10,
                metricCard(dark, "Месяцев в календаре", String.valueOf(buckets.size())),
                metricCard(dark, "Плановые платежи", formatMoney(planned)),
                metricCard(dark, "Фактические платежи", formatMoney(actual)),
                metricCard(dark, "Пиковый месяц", formatMoney(peak))
        );
    }

    private static VBox monthSection(boolean dark, List<MonthBucket> buckets) {
        VBox section = new VBox(10);
        if (buckets.isEmpty()) {
            section.getChildren().add(emptyCard(dark));
            return section;
        }
        for (MonthBucket bucket : buckets) {
            section.getChildren().add(monthCard(dark, bucket));
        }
        return section;
    }

    private static VBox stageSection(boolean dark, List<StageSnapshot> stageSnapshots) {
        VBox section = new VBox(10);
        Label title = new Label("Дорогие этапы");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: 900; -fx-text-fill: "
                + color(dark, "#f8fafc", "#0f172a") + ";");
        section.getChildren().add(title);

        if (stageSnapshots.isEmpty()) {
            Label empty = new Label("У этапов пока нет связанного бюджета. Назначьте статьи на задачи или группы проекта.");
            applyWrappedTextStyle(empty, "-fx-font-size: 12px; -fx-text-fill: " + color(dark, "#cbd5e1", "#475569") + ";");
            VBox card = new VBox(empty);
            card.setPadding(new Insets(14));
            card.setStyle("-fx-background-color: " + color(dark, "rgba(15,23,42,0.84)", "#ffffff") + ";"
                    + "-fx-background-radius: 12; -fx-border-radius: 12;"
                    + "-fx-border-color: " + color(dark, "rgba(100,116,139,0.35)", "rgba(148,163,184,0.24)") + ";");
            section.getChildren().add(card);
            return section;
        }

        VBox cards = new VBox(8);
        int limit = Math.min(stageSnapshots.size(), 5);
        for (int i = 0; i < limit; i++) {
            cards.getChildren().add(stageCard(dark, i + 1, stageSnapshots.get(i)));
        }
        section.getChildren().add(cards);
        return section;
    }

    private static VBox monthCard(boolean dark, MonthBucket bucket) {
        Label title = new Label(bucket.title());
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: 900; -fx-text-fill: "
                + color(dark, "#f8fafc", "#0f172a") + ";");

        HBox amounts = new HBox(14,
                pill(dark, "План", formatMoney(bucket.planned()), "#2563eb"),
                pill(dark, "Факт", formatMoney(bucket.actual()), "#10b981"),
                pill(dark, "Отклонение", formatMoney(bucket.actual() - bucket.planned()), bucket.actual() > bucket.planned() ? "#ef4444" : "#f59e0b")
        );

        VBox details = new VBox(6);
        int limit = Math.min(bucket.lines().size(), 8);
        for (int i = 0; i < limit; i++) {
            details.getChildren().add(detailLine(dark, bucket.lines().get(i)));
        }
        if (bucket.lines().size() > limit) {
            details.getChildren().add(detailLine(dark, "… и ещё " + (bucket.lines().size() - limit) + " операций"));
        }

        VBox card = new VBox(10, title, amounts, details);
        card.setPadding(new Insets(14));
        card.setStyle("-fx-background-color: " + color(dark, "rgba(15,23,42,0.90)", "rgba(255,255,255,0.98)") + ";"
                + "-fx-background-radius: 14; -fx-border-radius: 14;"
                + "-fx-border-color: " + color(dark, "rgba(100,116,139,0.30)", "rgba(148,163,184,0.22)") + ";");
        return card;
    }

    private static VBox stageCard(boolean dark, int index, StageSnapshot snapshot) {
        Label stageLabel = new Label("#" + index + "  " + snapshot.stageName());
        applyWrappedTextStyle(stageLabel, "-fx-font-size: 13px; -fx-font-weight: 800; -fx-text-fill: "
                + color(dark, "#f8fafc", "#0f172a") + ";");

        Label positionLabel = new Label(snapshot.position());
        positionLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " + color(dark, "#94a3b8", "#64748b") + ";");

        HBox pills = new HBox(10,
                pill(dark, "План", formatMoney(snapshot.planned()), "#2563eb"),
                pill(dark, "Факт", formatMoney(snapshot.actual()), "#10b981"),
                pill(dark, "Отклонение", formatMoney(snapshot.actual() - snapshot.planned()),
                        snapshot.actual() > snapshot.planned() ? "#ef4444" : "#f59e0b")
        );

        Label details = new Label("Статей бюджета: " + snapshot.itemCount()
                + " • Пиковая сумма этапа: " + formatMoney(Math.max(snapshot.planned(), snapshot.actual())));
        applyWrappedTextStyle(details, "-fx-font-size: 11px; -fx-text-fill: "
                + color(dark, "#cbd5e1", "#475569") + ";");

        VBox card = new VBox(8, stageLabel, positionLabel, pills, details);
        card.setPadding(new Insets(12));
        card.setStyle("-fx-background-color: " + color(dark, "rgba(15,23,42,0.84)", "#ffffff") + ";"
                + "-fx-background-radius: 12; -fx-border-radius: 12;"
                + "-fx-border-color: " + color(dark, "rgba(100,116,139,0.35)", "rgba(148,163,184,0.24)") + ";");
        return card;
    }

    private static VBox emptyCard(boolean dark) {
        Label text = new Label("В календаре пока нет ни одной статьи бюджета с датой платежа.");
        applyWrappedTextStyle(text, "-fx-font-size: 13px; -fx-text-fill: " + color(dark, "#dbe4f0", "#334155") + ";");
        VBox card = new VBox(text);
        card.setPadding(new Insets(18));
        card.setStyle("-fx-background-color: " + color(dark, "rgba(15,23,42,0.90)", "rgba(255,255,255,0.98)") + ";"
                + "-fx-background-radius: 14; -fx-border-radius: 14;"
                + "-fx-border-color: " + color(dark, "rgba(100,116,139,0.30)", "rgba(148,163,184,0.22)") + ";");
        return card;
    }

    private static HBox pill(boolean dark, String title, String value, String accent) {
        Label label = new Label(title + ": " + value);
        label.setStyle("-fx-font-size: 12px; -fx-font-weight: 800; -fx-text-fill: " + color(dark, "#f8fafc", "#0f172a") + ";");
        HBox box = new HBox(label);
        box.setPadding(new Insets(6, 10, 6, 10));
        box.setStyle("-fx-background-color: " + accent + "22; -fx-border-color: " + accent + "66;"
                + "-fx-background-radius: 10; -fx-border-radius: 10;");
        return box;
    }

    private static Label detailLine(boolean dark, String text) {
        Label label = new Label(text);
        applyWrappedTextStyle(label, "-fx-font-size: 12px; -fx-text-fill: " + color(dark, "#dbe4f0", "#334155") + ";");
        return label;
    }

    private static VBox metricCard(boolean dark, String title, String value) {
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " + color(dark, "#94a3b8", "#64748b") + ";");

        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: 900; -fx-text-fill: "
                + color(dark, "#f8fafc", "#0f172a") + ";");

        VBox card = new VBox(4, titleLabel, valueLabel);
        card.setPadding(new Insets(12));
        card.setStyle("-fx-background-color: " + color(dark, "rgba(15,23,42,0.84)", "#ffffff") + ";"
                + "-fx-background-radius: 12; -fx-border-radius: 12;"
                + "-fx-border-color: " + color(dark, "rgba(100,116,139,0.35)", "rgba(148,163,184,0.24)") + ";");
        HBox.setHgrow(card, Priority.ALWAYS);
        card.setMaxWidth(Double.MAX_VALUE);
        return card;
    }

    private static List<MonthBucket> buildBuckets(ObservableList<BudgetItem> budgetItems) {
        Map<YearMonth, MutableBucket> buckets = new LinkedHashMap<>();
        if (budgetItems != null) {
            for (BudgetItem item : budgetItems) {
                if (item == null) {
                    continue;
                }
                if (item.getPlannedDate() != null) {
                    YearMonth month = YearMonth.from(item.getPlannedDate());
                    buckets.computeIfAbsent(month, MutableBucket::new)
                            .addPlanned(item);
                }
                if (item.getActualDate() != null) {
                    YearMonth month = YearMonth.from(item.getActualDate());
                    buckets.computeIfAbsent(month, MutableBucket::new)
                            .addActual(item);
                }
            }
        }
        return buckets.values().stream()
                .sorted(Comparator.comparing(MutableBucket::month))
                .map(MutableBucket::toImmutable)
                .toList();
    }

    private static List<StageSnapshot> buildStageSnapshots(List<Task> topLevelTasks, ObservableList<BudgetItem> budgetItems) {
        List<StageSnapshot> snapshots = new ArrayList<>();
        if (topLevelTasks == null || budgetItems == null || budgetItems.isEmpty()) {
            return snapshots;
        }
        for (Task task : topLevelTasks) {
            if (task == null || task.getPosition() == null || task.getPosition().isBlank()) {
                continue;
            }
            double planned = 0.0;
            double actual = 0.0;
            int itemCount = 0;
            for (BudgetItem item : budgetItems) {
                if (item == null || item.getTaskPosition() == null || item.getTaskPosition().isBlank()) {
                    continue;
                }
                if (!item.getTaskPosition().startsWith(task.getPosition())) {
                    continue;
                }
                planned += Math.max(0.0, item.getPlannedAmount());
                actual += Math.max(0.0, item.getActualAmount());
                itemCount++;
            }
            if (itemCount == 0) {
                continue;
            }
            snapshots.add(new StageSnapshot(task.getPosition(), task.getName(), planned, actual, itemCount));
        }
        snapshots.sort(Comparator.comparingDouble((StageSnapshot stage) -> Math.max(stage.planned(), stage.actual())).reversed());
        return snapshots;
    }

    private static String formatMoney(double value) {
        return String.format(Locale.US, "%,.2f", value).replace(',', ' ') + " ₽";
    }

    private static boolean isDarkTheme() {
        var settings = AppSettings.get();
        return settings != null
                && settings.getTheme() != null
                && "dark".equalsIgnoreCase(settings.getTheme().trim());
    }

    private static String color(boolean dark, String darkColor, String lightColor) {
        return dark ? darkColor : lightColor;
    }

    private static void applyWrappedTextStyle(Label label, String style) {
        label.setWrapText(true);
        label.setTextOverrun(javafx.scene.control.OverrunStyle.CLIP);
        label.setMinHeight(Region.USE_PREF_SIZE);
        label.setPrefHeight(Region.USE_COMPUTED_SIZE);
        label.setMaxHeight(Double.MAX_VALUE);
        label.setStyle(style);
        VBox.setVgrow(label, Priority.NEVER);
    }

    private record MonthBucket(YearMonth month, double planned, double actual, List<String> lines) {
        String title() {
            return month.format(MONTH_FORMAT).substring(0, 1).toUpperCase(new Locale("ru"))
                    + month.format(MONTH_FORMAT).substring(1);
        }

        double maxMonth() {
            return Math.max(planned, actual);
        }
    }

    private record StageSnapshot(String position, String stageName, double planned, double actual, int itemCount) {
    }

    private static final class MutableBucket {
        private final YearMonth month;
        private double planned;
        private double actual;
        private final List<String> lines = new ArrayList<>();

        private MutableBucket(YearMonth month) {
            this.month = month;
        }

        private YearMonth month() {
            return month;
        }

        private void addPlanned(BudgetItem item) {
            planned += item.getPlannedAmount();
            lines.add("План: " + safe(item.getTaskPosition()) + " " + safe(item.getName()) + " — " + formatMoney(item.getPlannedAmount()));
        }

        private void addActual(BudgetItem item) {
            actual += item.getActualAmount();
            lines.add("Факт: " + safe(item.getTaskPosition()) + " " + safe(item.getName()) + " — " + formatMoney(item.getActualAmount()));
        }

        private MonthBucket toImmutable() {
            return new MonthBucket(month, planned, actual, List.copyOf(lines));
        }
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }
}
