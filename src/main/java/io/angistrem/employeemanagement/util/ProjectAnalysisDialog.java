package io.angistrem.employeemanagement.util;

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

import java.time.format.DateTimeFormatter;

public final class ProjectAnalysisDialog {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private ProjectAnalysisDialog() {
    }

    public static void show(Window owner, ProjectAnalysisAnalyzer.Analysis analysis) {
        if (analysis == null || !analysis.isMeaningful()) {
            return;
        }

        boolean dark = isDarkTheme();
        ProjectAnalysisAnalyzer.ImpactItem primary = analysis.primaryItem();

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Анализ проекта");
        dialog.setHeaderText(null);
        dialog.setGraphic(null);
        if (owner != null) {
            dialog.initOwner(owner);
        }

        VBox content = new VBox(14,
                heroCard(dark, analysis, primary),
                metricRow(dark, analysis),
                pressureRow(dark, analysis),
                recommendationCard(dark, analysis, primary),
                rankingSection(dark, analysis)
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
        dialog.getDialogPane().setPrefSize(1040, 880);
        SceneManager.applyThemeToDialog(dialog);
        dialog.showAndWait();
    }

    private static VBox heroCard(boolean dark, ProjectAnalysisAnalyzer.Analysis analysis, ProjectAnalysisAnalyzer.ImpactItem primary) {
        Label title = new Label("Анализ проекта");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: 900; -fx-text-fill: "
                + color(dark, "#ffffff", "#0f172a") + ";");

        String summary = primary == null
                ? "Ключевой драйвер финиша не определён. График сейчас распределяет риск между несколькими этапами."
                : "Финальную веху «" + analysis.finalMilestoneLabel() + "» сильнее всего двигает задача «" + primary.label()
                + "» исполнителя " + primary.assignee() + ".";

        Label subtitle = new Label(summary);
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

    private static HBox metricRow(boolean dark, ProjectAnalysisAnalyzer.Analysis analysis) {
        return new HBox(10,
                metricCard(dark, "Старт проекта", DATE_FORMAT.format(analysis.projectStart())),
                metricCard(dark, "Финиш проекта", DATE_FORMAT.format(analysis.projectEnd())),
                metricCard(dark, "Финальная веха", analysis.finalMilestoneLabel()),
                metricCard(dark, "Длительность", analysis.totalDays() + " дн.")
        );
    }

    private static HBox pressureRow(boolean dark, ProjectAnalysisAnalyzer.Analysis analysis) {
        ProjectAnalysisAnalyzer.BudgetPressure budget = analysis.budgetPressure();
        ProjectAnalysisAnalyzer.ResourcePressure resources = analysis.resourcePressure();
        return new HBox(10,
                summaryCard(dark, "Сроки",
                        analysis.criticalPath() == null ? "Критический путь не найден" : analysis.criticalPath().steps().size() + " этапов в критическом пути",
                        analysis.primaryItem() == null
                                ? "У проекта нет одного доминирующего драйвера сдвига."
                                : "При +1 дне на ключевой задаче финиш смещается на +" + analysis.primaryItem().oneDayImpact() + " дн."),
                summaryCard(dark, "Деньги",
                        formatMoney(budget.variance()),
                        "План: " + formatMoney(budget.plannedTotal())
                                + " | Факт: " + formatMoney(budget.actualTotal())
                                + " | Пик платежей: " + budget.peakMonthLabel()),
                summaryCard(dark, "Ресурсы",
                        resources.overloadedResources() + " перегружено",
                        "Назначено: " + resources.resourcesAssigned()
                                + " | Самый горячий ресурс: " + resources.hottestResourceName()
                                + " | Конфликтных дней: " + resources.hottestResourceConflictDays())
        );
    }

    private static VBox metricCard(boolean dark, String title, String value) {
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " + color(dark, "#94a3b8", "#64748b") + ";");

        Label valueLabel = new Label(value);
        applyWrappedTextStyle(valueLabel, "-fx-font-size: 16px; -fx-font-weight: 900; -fx-text-fill: "
                + color(dark, "#f8fafc", "#0f172a") + ";");

        VBox card = new VBox(4, titleLabel, valueLabel);
        card.setPadding(new Insets(12));
        card.setStyle(panelStyle(dark, color(dark, "rgba(15,23,42,0.84)", "#ffffff"),
                color(dark, "rgba(100,116,139,0.35)", "rgba(148,163,184,0.24)"), 12));
        HBox.setHgrow(card, Priority.ALWAYS);
        card.setMaxWidth(Double.MAX_VALUE);
        return card;
    }

    private static VBox summaryCard(boolean dark, String title, String value, String text) {
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " + color(dark, "#94a3b8", "#64748b") + ";");

        Label valueLabel = new Label(value);
        applyWrappedTextStyle(valueLabel, "-fx-font-size: 14px; -fx-font-weight: 900; -fx-text-fill: "
                + color(dark, "#f8fafc", "#0f172a") + ";");

        Label textLabel = new Label(text);
        applyWrappedTextStyle(textLabel, "-fx-font-size: 12px; -fx-text-fill: " + color(dark, "#dbe4f0", "#475569") + ";");

        VBox card = new VBox(6, titleLabel, valueLabel, textLabel);
        card.setPadding(new Insets(14));
        card.setStyle(panelStyle(dark, color(dark, "rgba(30,41,59,0.96)", "rgba(255,255,255,0.98)"),
                color(dark, "rgba(100,116,139,0.32)", "rgba(148,163,184,0.22)"), 14));
        HBox.setHgrow(card, Priority.ALWAYS);
        card.setMaxWidth(Double.MAX_VALUE);
        return card;
    }

    private static VBox recommendationCard(boolean dark, ProjectAnalysisAnalyzer.Analysis analysis, ProjectAnalysisAnalyzer.ImpactItem primary) {
        String title = "Рекомендация";
        String body;
        if (primary == null) {
            body = "Сейчас проект не завязан на один критический драйвер. Держать фокус стоит на поздних этапах, пиковых месяцах платежей и ресурсах с конфликтами.";
        } else {
            String finishDate = DATE_FORMAT.format(analysis.projectEnd());
            String shiftedOneDay = DATE_FORMAT.format(analysis.projectEnd().plusDays(primary.oneDayImpact()));
            String shiftedThreeDays = DATE_FORMAT.format(analysis.projectEnd().plusDays(primary.threeDayImpact()));
            body = "Если задача «" + primary.label() + "» исполнителя " + primary.assignee()
                    + " задержится на 1 день, финальная веха «" + analysis.finalMilestoneLabel() + "» сместится с "
                    + finishDate + " на " + shiftedOneDay + ". При задержке на 3 дня финиш уйдёт на " + shiftedThreeDays + ". "
                    + (primary.overloadedByResources()
                    ? "По этой задаче уже есть перегруз ресурсов, значит усиливать нужно именно её команду. "
                    : "Ресурсного перегруза по этой задаче сейчас нет, значит акцент стоит делать на дисциплину исполнения и резервы. ")
                    + (primary.budgetVariance() > 0.0
                    ? "Фактические затраты по связанным статьям уже превышают план на " + formatMoney(primary.budgetVariance()) + "."
                    : primary.plannedBudget() > 0.0
                    ? "По связанным статьям бюджета пока нет превышения плана."
                    : "По этой задаче бюджетные статьи ещё не заведены, поэтому финансовый контроль стоит добавить.");
        }

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 900; -fx-text-fill: "
                + color(dark, "#fde68a", "#92400e") + ";");

        Label bodyLabel = new Label(body);
        applyWrappedTextStyle(bodyLabel, "-fx-font-size: 12px; -fx-line-spacing: 2px; -fx-text-fill: "
                + color(dark, "#fef3c7", "#78350f") + ";");

        VBox card = new VBox(6, titleLabel, bodyLabel);
        card.setPadding(new Insets(14));
        card.setStyle("-fx-background-color: " + color(dark, "rgba(120,53,15,0.28)", "rgba(254,243,199,0.92)") + ";"
                + "-fx-background-radius: 14; -fx-border-radius: 14;"
                + "-fx-border-color: " + color(dark, "rgba(245,158,11,0.28)", "rgba(217,119,6,0.22)") + ";"
                + "-fx-effect: dropshadow(gaussian, rgba(245,158,11,0.10), 16, 0.18, 0, 4);");
        return card;
    }

    private static VBox rankingSection(boolean dark, ProjectAnalysisAnalyzer.Analysis analysis) {
        Label header = new Label("Рейтинг влияния задач");
        header.setStyle("-fx-font-size: 15px; -fx-font-weight: 900; -fx-text-fill: "
                + color(dark, "#f8fafc", "#0f172a") + ";");

        VBox itemsBox = new VBox(10);
        ProjectAnalysisAnalyzer.ImpactItem primary = analysis.primaryItem();
        for (ProjectAnalysisAnalyzer.ImpactItem item : analysis.rankedItems()) {
            itemsBox.getChildren().add(impactCard(dark, item, primary != null && primary.key().equals(item.key())));
        }

        ScrollPane scrollPane = new ScrollPane(itemsBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setPannable(true);
        scrollPane.setPrefViewportHeight(420);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        return new VBox(10, header, scrollPane);
    }

    private static VBox impactCard(boolean dark, ProjectAnalysisAnalyzer.ImpactItem item, boolean primary) {
        Label keyLabel = chip(item.key(),
                color(dark, "#dbeafe", "#1d4ed8"),
                color(dark, "rgba(37,99,235,0.16)", "rgba(219,234,254,0.95)"));

        Label cpLabel = chip(item.criticalPath() ? "Критический путь" : "Вне критического пути",
                color(dark, item.criticalPath() ? "#bbf7d0" : "#cbd5e1", item.criticalPath() ? "#166534" : "#475569"),
                color(dark,
                        item.criticalPath() ? "rgba(22,163,74,0.16)" : "rgba(100,116,139,0.16)",
                        item.criticalPath() ? "rgba(220,252,231,0.96)" : "rgba(241,245,249,0.96)"));

        Label resourceLabel = chip(item.overloadedByResources() ? "Есть перегруз" : "Ресурсно стабильно",
                color(dark, item.overloadedByResources() ? "#fed7aa" : "#bfdbfe", item.overloadedByResources() ? "#9a3412" : "#1d4ed8"),
                color(dark,
                        item.overloadedByResources() ? "rgba(249,115,22,0.20)" : "rgba(37,99,235,0.16)",
                        item.overloadedByResources() ? "rgba(255,237,213,0.95)" : "rgba(219,234,254,0.95)"));

        HBox chips = new HBox(8, keyLabel, cpLabel, resourceLabel);

        Label nameLabel = new Label(item.label());
        applyWrappedTextStyle(nameLabel, "-fx-font-size: 14px; -fx-font-weight: 900; -fx-text-fill: "
                + color(dark, "#f8fafc", "#0f172a") + ";");

        Label metaLabel = new Label("Исполнитель: " + item.assignee()
                + " | Период: " + DATE_FORMAT.format(item.start()) + " — " + DATE_FORMAT.format(item.end())
                + " | Длительность: " + item.durationDays() + " дн."
                + " | Назначено ресурсов: " + item.assignedResources());
        applyWrappedTextStyle(metaLabel, "-fx-font-size: 12px; -fx-text-fill: " + color(dark, "#cbd5e1", "#475569") + ";");

        Label impactLabel = new Label("Запас: " + item.slackDays() + " дн. | Смещение финиша при +1 дне: +" + item.oneDayImpact()
                + " дн. | При +3 днях: +" + item.threeDayImpact() + " дн.");
        applyWrappedTextStyle(impactLabel, "-fx-font-size: 12px; -fx-font-weight: 800; -fx-text-fill: "
                + color(dark, "#bfdbfe", "#1d4ed8") + ";");

        String budgetText = item.plannedBudget() > 0.0 || item.actualBudget() > 0.0
                ? "Бюджет по связанным статьям: план " + formatMoney(item.plannedBudget()) + ", факт " + formatMoney(item.actualBudget())
                + ", отклонение " + formatMoney(item.budgetVariance()) + "."
                : "Бюджетные статьи по этой задаче пока не заведены.";
        Label budgetLabel = new Label(budgetText);
        applyWrappedTextStyle(budgetLabel, "-fx-font-size: 12px; -fx-text-fill: " + color(dark, "#dbe4f0", "#334155") + ";");

        VBox card = new VBox(8, chips, nameLabel, metaLabel, impactLabel, budgetLabel);
        card.setPadding(new Insets(14));
        card.setStyle(panelStyle(dark, color(dark,
                primary ? "rgba(30,41,59,0.98)" : "rgba(15,23,42,0.90)",
                primary ? "#ffffff" : "rgba(255,255,255,0.96)"),
                color(dark,
                primary ? "rgba(96,165,250,0.42)" : "rgba(100,116,139,0.30)",
                primary ? "rgba(59,130,246,0.28)" : "rgba(148,163,184,0.22)"), 14));
        return card;
    }

    private static Label chip(String text, String textColor, String backgroundColor) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 11px; -fx-font-weight: 800;"
                + "-fx-text-fill: " + textColor + ";"
                + "-fx-background-color: " + backgroundColor + ";"
                + "-fx-padding: 4 10 4 10; -fx-background-radius: 999;");
        return label;
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

    private static String formatMoney(double value) {
        return String.format(java.util.Locale.US, "%,.2f", value).replace(',', ' ') + " ₽";
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
}
