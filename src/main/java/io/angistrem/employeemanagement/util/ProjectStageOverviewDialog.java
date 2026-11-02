package io.angistrem.employeemanagement.util;

import io.angistrem.employeemanagement.model.BudgetItem;
import io.angistrem.employeemanagement.model.Task;
import io.angistrem.employeemanagement.model.TaskStatus;
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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class ProjectStageOverviewDialog {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private ProjectStageOverviewDialog() {
    }

    public static void show(Window owner, List<Task> topLevelTasks, ObservableList<BudgetItem> budgetItems) {
        boolean dark = isDarkTheme();
        List<StageSummary> stages = buildStageSummaries(topLevelTasks, budgetItems);

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Сводка по этапам");
        dialog.setHeaderText(null);
        dialog.setGraphic(null);
        if (owner != null) {
            dialog.initOwner(owner);
        }

        VBox content = new VBox(14,
                heroCard(dark, stages),
                summaryRow(dark, stages),
                stagesSection(dark, stages)
        );
        content.setPadding(new Insets(8, 10, 4, 10));
        content.setPrefWidth(1060);
        content.setFillWidth(true);

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setPannable(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        dialog.getDialogPane().setContent(scrollPane);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
        dialog.getDialogPane().setPrefSize(1120, 900);
        SceneManager.applyThemeToDialog(dialog);
        dialog.showAndWait();
    }

    private static VBox heroCard(boolean dark, List<StageSummary> stages) {
        Label title = new Label("Сводка по этапам");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: 900; -fx-text-fill: "
                + color(dark, "#ffffff", "#0f172a") + ";");

        StageSummary hottest = stages.stream().max(Comparator.comparingInt(StageSummary::pressureScore)).orElse(null);
        String subtitleText = hottest == null
                ? "В проекте пока нет этапов с данными для управленческой сводки."
                : "Сильнее всего внимания сейчас требует этап «" + hottest.name()
                + "»: " + hottest.healthLabel().toLowerCase(Locale.ROOT) + ", "
                + "критических задач — " + hottest.criticalTasks()
                + ", перегруженных — " + hottest.resourceHotTasks()
                + ", бюджетное отклонение — " + formatSignedMoney(hottest.variance()) + ".";

        Label subtitle = new Label(subtitleText);
        applyWrappedTextStyle(subtitle, "-fx-font-size: 13px; -fx-line-spacing: 3px; -fx-text-fill: "
                + color(dark, "#eff6ff", "#1e293b") + ";");

        VBox box = new VBox(8, title, subtitle);
        box.setPadding(new Insets(18));
        box.setStyle("-fx-background-color: "
                + color(dark, "linear-gradient(to right, #0f172a, #1d4ed8)", "linear-gradient(to right, #dbeafe, #eff6ff)") + ";"
                + "-fx-background-radius: 18; -fx-border-radius: 18;"
                + "-fx-effect: dropshadow(gaussian, rgba(15,23,42,0.12), 22, 0.18, 0, 5);");
        return box;
    }

    private static HBox summaryRow(boolean dark, List<StageSummary> stages) {
        int pressuredStages = (int) stages.stream().filter(stage -> stage.pressureScore() > 0).count();
        StageSummary biggestVariance = stages.stream()
                .max(Comparator.comparingDouble(stage -> Math.abs(stage.variance())))
                .orElse(null);
        StageSummary hottestResources = stages.stream()
                .max(Comparator.comparingInt(StageSummary::resourceHotTasks))
                .orElse(null);

        return new HBox(10,
                metricCard(dark, "Этапов в проекте", String.valueOf(stages.size())),
                metricCard(dark, "Под давлением", String.valueOf(pressuredStages)),
                metricCard(dark, "Самый дорогой этап",
                        biggestVariance == null ? "—" : biggestVariance.name()),
                metricCard(dark, "Горячий по ресурсам",
                        hottestResources == null ? "—" : hottestResources.name())
        );
    }

    private static VBox stagesSection(boolean dark, List<StageSummary> stages) {
        VBox section = new VBox(10);
        if (stages.isEmpty()) {
            Label empty = new Label("Нет этапов для анализа. Создайте верхнеуровневые этапы или группы задач.");
            applyWrappedTextStyle(empty, "-fx-font-size: 12px; -fx-text-fill: " + color(dark, "#cbd5e1", "#475569") + ";");
            VBox card = new VBox(empty);
            card.setPadding(new Insets(16));
            card.setStyle(panelStyle(dark, color(dark, "rgba(15,23,42,0.84)", "#ffffff"),
                    color(dark, "rgba(100,116,139,0.35)", "rgba(148,163,184,0.24)"), 12));
            section.getChildren().add(card);
            return section;
        }
        for (StageSummary stage : stages) {
            section.getChildren().add(stageCard(dark, stage));
        }
        return section;
    }

    private static VBox stageCard(boolean dark, StageSummary stage) {
        Label title = new Label(stage.position() + "  " + stage.name());
        applyWrappedTextStyle(title, "-fx-font-size: 15px; -fx-font-weight: 900; -fx-text-fill: "
                + color(dark, "#f8fafc", "#0f172a") + ";");

        Label period = new Label("Период: " + formatDate(stage.start()) + " — " + formatDate(stage.end())
                + " • Длительность: " + stage.durationDays() + " дн.");
        applyWrappedTextStyle(period, "-fx-font-size: 12px; -fx-text-fill: " + color(dark, "#cbd5e1", "#475569") + ";");

        HBox chips = new HBox(8,
                chip(stage.healthLabel(), healthTextColor(stage.pressureScore(), dark), healthBackground(stage.pressureScore(), dark)),
                chip("Критических: " + stage.criticalTasks(),
                        color(dark, "#fed7aa", "#9a3412"),
                        color(dark, "rgba(249,115,22,0.20)", "rgba(255,237,213,0.95)")),
                chip("Ресурсный перегруз: " + stage.resourceHotTasks(),
                        color(dark, "#fde68a", "#92400e"),
                        color(dark, "rgba(245,158,11,0.18)", "rgba(254,243,199,0.95)")),
                chip("Бюджетное давление: " + (stage.budgetOverrun() ? "да" : "нет"),
                        color(dark, stage.budgetOverrun() ? "#ddd6fe" : "#bfdbfe", stage.budgetOverrun() ? "#6d28d9" : "#1d4ed8"),
                        color(dark, stage.budgetOverrun() ? "rgba(124,58,237,0.20)" : "rgba(37,99,235,0.16)",
                                stage.budgetOverrun() ? "rgba(237,233,254,0.95)" : "rgba(219,234,254,0.95)"))
        );

        Label progress = new Label("Прогресс: " + String.format(Locale.US, "%.1f%%", stage.percent())
                + " • Задач: " + stage.leafTasks()
                + " • Просрочено: " + stage.overdueTasks()
                + " • Завершено: " + stage.completedTasks());
        applyWrappedTextStyle(progress, "-fx-font-size: 12px; -fx-text-fill: " + color(dark, "#dbe4f0", "#334155") + ";");

        Label budget = new Label("Бюджет: план " + formatMoney(stage.plannedBudget())
                + ", факт " + formatMoney(stage.actualBudget())
                + ", отклонение " + formatSignedMoney(stage.variance()));
        applyWrappedTextStyle(budget, "-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: "
                + color(dark, "#e9d5ff", "#5b21b6") + ";");

        Label insight = new Label(stage.insight());
        applyWrappedTextStyle(insight, "-fx-font-size: 12px; -fx-line-spacing: 2px; -fx-text-fill: "
                + color(dark, "#dbe4f0", "#475569") + ";");

        VBox card = new VBox(8, title, period, chips, progress, budget, insight);
        card.setPadding(new Insets(14));
        card.setStyle(panelStyle(dark, color(dark, "rgba(15,23,42,0.90)", "rgba(255,255,255,0.98)"),
                color(dark, "rgba(100,116,139,0.30)", "rgba(148,163,184,0.22)"), 14));
        return card;
    }

    private static VBox metricCard(boolean dark, String title, String value) {
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " + color(dark, "#94a3b8", "#64748b") + ";");

        Label valueLabel = new Label(value);
        applyWrappedTextStyle(valueLabel, "-fx-font-size: 15px; -fx-font-weight: 900; -fx-text-fill: "
                + color(dark, "#f8fafc", "#0f172a") + ";");

        VBox card = new VBox(4, titleLabel, valueLabel);
        card.setPadding(new Insets(12));
        card.setStyle(panelStyle(dark, color(dark, "rgba(15,23,42,0.84)", "#ffffff"),
                color(dark, "rgba(100,116,139,0.35)", "rgba(148,163,184,0.24)"), 12));
        HBox.setHgrow(card, Priority.ALWAYS);
        card.setMaxWidth(Double.MAX_VALUE);
        return card;
    }

    private static Label chip(String text, String textColor, String backgroundColor) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: " + textColor + ";"
                + "-fx-background-color: " + backgroundColor + ";"
                + "-fx-background-radius: 999; -fx-padding: 4 10 4 10;");
        return label;
    }

    private static List<StageSummary> buildStageSummaries(List<Task> topLevelTasks, ObservableList<BudgetItem> budgetItems) {
        List<StageSummary> summaries = new ArrayList<>();
        if (topLevelTasks == null) {
            return summaries;
        }
        for (Task task : topLevelTasks) {
            if (task == null || task.isMilestone()) {
                continue;
            }
            List<Task> descendants = collectStageTasks(task);
            List<Task> leafTasks = descendants.stream().filter(stageTask -> !stageTask.isGroup()).toList();
            int criticalTasks = (int) leafTasks.stream().filter(Task::isCriticalDependency).count();
            int overdueTasks = (int) leafTasks.stream()
                    .filter(stageTask -> TaskStatus.OVERDUE.equals(stageTask.getStatus()))
                    .count();
            int completedTasks = (int) leafTasks.stream()
                    .filter(stageTask -> TaskStatus.COMPLETED.equals(stageTask.getStatus()))
                    .count();
            int resourceHotTasks = (int) leafTasks.stream().filter(Task::isResourceOverloaded).count();
            int budgetHotTasks = (int) leafTasks.stream().filter(Task::isBudgetOverrun).count();

            double plannedBudget = 0.0;
            double actualBudget = 0.0;
            if (budgetItems != null) {
                for (BudgetItem item : budgetItems) {
                    if (item == null || item.getTaskPosition() == null || item.getTaskPosition().isBlank()) {
                        continue;
                    }
                    if (!item.getTaskPosition().startsWith(task.getPosition())) {
                        continue;
                    }
                    plannedBudget += Math.max(0.0, item.getPlannedAmount());
                    actualBudget += Math.max(0.0, item.getActualAmount());
                }
            }
            double variance = actualBudget - plannedBudget;
            int pressureScore = criticalTasks + overdueTasks + resourceHotTasks + budgetHotTasks + (variance > 0.0001 ? 1 : 0);
            summaries.add(new StageSummary(
                    task.getPosition(),
                    task.getName(),
                    task.getStart(),
                    task.getEnd(),
                    stageDuration(task),
                    task.getPercent(),
                    leafTasks.size(),
                    criticalTasks,
                    overdueTasks,
                    completedTasks,
                    resourceHotTasks,
                    plannedBudget,
                    actualBudget,
                    variance,
                    variance > 0.0001,
                    pressureScore,
                    buildInsight(task, criticalTasks, overdueTasks, resourceHotTasks, variance)
            ));
        }
        return summaries;
    }

    private static List<Task> collectStageTasks(Task root) {
        List<Task> collected = new ArrayList<>();
        if (root == null) {
            return collected;
        }
        collected.add(root);
        for (Task child : root.getObservableChildTasks()) {
            collected.addAll(collectStageTasks(child));
        }
        return collected;
    }

    private static long stageDuration(Task task) {
        if (task == null || task.getStart() == null || task.getEnd() == null) {
            return 0;
        }
        return Math.max(1, ChronoUnit.DAYS.between(task.getStart(), task.getEnd()) + 1);
    }

    private static String buildInsight(Task task, int criticalTasks, int overdueTasks, int resourceHotTasks, double variance) {
        List<String> parts = new ArrayList<>();
        if (criticalTasks > 0) {
            parts.add("внутри этапа есть критические задачи");
        }
        if (overdueTasks > 0) {
            parts.add("есть просрочка по срокам");
        }
        if (resourceHotTasks > 0) {
            parts.add("ресурсы уже перегружены");
        }
        if (variance > 0.0001) {
            parts.add("факт по бюджету выше плана");
        }
        if (parts.isEmpty()) {
            parts.add("этап выглядит стабильно и без явного давления");
        }
        return "По этапу «" + task.getName() + "» " + String.join(", ", parts) + ".";
    }

    private static String formatDate(LocalDate value) {
        return value == null ? "—" : DATE_FORMAT.format(value);
    }

    private static String formatMoney(double value) {
        return String.format(Locale.US, "%,.0f", value).replace(',', ' ') + " ₽";
    }

    private static String formatSignedMoney(double value) {
        return (value > 0.0001 ? "+" : "") + formatMoney(value);
    }

    private static String healthTextColor(int score, boolean dark) {
        if (score >= 5) {
            return color(dark, "#fecaca", "#991b1b");
        }
        if (score >= 2) {
            return color(dark, "#fde68a", "#92400e");
        }
        return color(dark, "#bbf7d0", "#166534");
    }

    private static String healthBackground(int score, boolean dark) {
        if (score >= 5) {
            return color(dark, "rgba(239,68,68,0.18)", "rgba(254,226,226,0.95)");
        }
        if (score >= 2) {
            return color(dark, "rgba(245,158,11,0.18)", "rgba(254,243,199,0.95)");
        }
        return color(dark, "rgba(34,197,94,0.18)", "rgba(220,252,231,0.95)");
    }

    private static String color(boolean dark, String darkColor, String lightColor) {
        return dark ? darkColor : lightColor;
    }

    private static boolean isDarkTheme() {
        var settings = AppSettings.get();
        return settings != null
                && settings.getTheme() != null
                && "dark".equalsIgnoreCase(settings.getTheme().trim());
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

    private static String panelStyle(boolean dark, String background, String border, int radius) {
        return "-fx-background-color: " + background + ";"
                + "-fx-background-radius: " + radius + ";"
                + "-fx-border-radius: " + radius + ";"
                + "-fx-border-color: " + border + ";"
                + "-fx-effect: dropshadow(gaussian, " + color(dark, "rgba(15,23,42,0.18)", "rgba(15,23,42,0.06)") + ", 18, 0.16, 0, 4);";
    }

    private record StageSummary(
            String position,
            String name,
            LocalDate start,
            LocalDate end,
            long durationDays,
            double percent,
            int leafTasks,
            int criticalTasks,
            int overdueTasks,
            int completedTasks,
            int resourceHotTasks,
            double plannedBudget,
            double actualBudget,
            double variance,
            boolean budgetOverrun,
            int pressureScore,
            String insight
    ) {
        String healthLabel() {
            if (pressureScore >= 5) {
                return "Требует срочного внимания";
            }
            if (pressureScore >= 2) {
                return "Под давлением";
            }
            return "Этап стабилен";
        }
    }
}
