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

public final class ResourceLoadDialog {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private ResourceLoadDialog() {
    }

    public static void show(Window owner, ResourceLoadAnalyzer.Analysis analysis) {
        if (analysis == null || !analysis.hasData()) {
            return;
        }

        boolean dark = isDarkTheme();
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Загрузка ресурсов");
        dialog.setHeaderText(null);
        dialog.setGraphic(null);
        if (owner != null) {
            dialog.initOwner(owner);
        }

        VBox content = new VBox(14,
                headerCard(dark, analysis),
                summaryCard(dark, analysis),
                resourceSection(dark, analysis)
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

    private static VBox headerCard(boolean dark, ResourceLoadAnalyzer.Analysis analysis) {
        String text = analysis.hasAssignments()
                ? "Здесь видно, какие ресурсы перегружены, в какие дни возникает конфликт и какие задачи дают пиковую нагрузку."
                : "В проекте пока нет назначений ресурсов на задачи. Сначала назначьте людей, технику или подрядчиков.";

        Label title = new Label("Загрузка ресурсов");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: 900; -fx-text-fill: " + color(dark, "#ffffff", "#0f172a") + ";");

        Label subtitle = new Label(text);
        applyWrappedTextStyle(subtitle, "-fx-font-size: 13px; -fx-line-spacing: 3px; -fx-text-fill: "
                + color(dark, "#eff6ff", "#1e293b") + ";");

        VBox box = new VBox(8, title, subtitle);
        box.setPadding(new Insets(18));
        box.setStyle("-fx-background-color: "
                + color(dark, "linear-gradient(to right, #0f172a, #1d4ed8)", "linear-gradient(to right, #dbeafe, #eff6ff)") + ";"
                + "-fx-background-radius: 18; -fx-border-radius: 18;");
        return box;
    }

    private static HBox summaryCard(boolean dark, ResourceLoadAnalyzer.Analysis analysis) {
        return new HBox(10,
                metricCard(dark, "Ресурсов в справочнике", String.valueOf(analysis.items().size())),
                metricCard(dark, "С назначениями", String.valueOf(analysis.assignedResources())),
                metricCard(dark, "Перегружено", String.valueOf(analysis.overloadedResources())),
                metricCard(dark, "Статус", analysis.overloadedResources() > 0 ? "Есть конфликты" : "Перегруза нет")
        );
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

    private static VBox resourceSection(boolean dark, ResourceLoadAnalyzer.Analysis analysis) {
        VBox box = new VBox(10);
        for (ResourceLoadAnalyzer.ResourceLoadItem item : analysis.items()) {
            box.getChildren().add(resourceCard(dark, item));
        }
        return box;
    }

    private static VBox resourceCard(boolean dark, ResourceLoadAnalyzer.ResourceLoadItem item) {
        Label title = new Label(item.resourceName() + suffix(item));
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: 900; -fx-text-fill: "
                + color(dark, "#f8fafc", "#0f172a") + ";");

        Label meta = new Label(
                "Тип: " + localizeType(item.resourceType())
                        + "    |    Компания: " + safe(item.company())
                        + "    |    Роль: " + safe(item.role())
                        + "    |    Мощность: " + format(item.capacity())
                        + "    |    Пиковая загрузка: " + format(item.peakLoad())
        );
        applyWrappedTextStyle(meta, "-fx-font-size: 12px; -fx-text-fill: " + color(dark, "#cbd5e1", "#475569") + ";");

        Label status = new Label(item.hasOverload()
                ? "Перегруз до +" + format(item.maxOverload()) + " сверх мощности. Нужна разгрузка или перенос задач."
                : "Ресурс укладывается в доступную мощность по текущим назначениям.");
        applyWrappedTextStyle(status, "-fx-font-size: 12px; -fx-font-weight: 800; -fx-text-fill: "
                + color(dark, item.hasOverload() ? "#fecaca" : "#bbf7d0", item.hasOverload() ? "#b91c1c" : "#166534") + ";");

        VBox assignments = new VBox(6);
        if (item.assignments().isEmpty()) {
            assignments.getChildren().add(detailLine(dark, "Назначений пока нет."));
        } else {
            int limit = Math.min(item.assignments().size(), 6);
            for (int i = 0; i < limit; i++) {
                ResourceLoadAnalyzer.ResourceAssignmentView assignment = item.assignments().get(i);
                assignments.getChildren().add(detailLine(dark,
                        assignment.taskPosition() + " " + assignment.taskName()
                                + " — " + DATE_FORMAT.format(assignment.start())
                                + " → " + DATE_FORMAT.format(assignment.end())
                                + " | загрузка " + format(assignment.allocation())
                                + (assignment.role() == null || assignment.role().isBlank() ? "" : " | роль " + assignment.role())));
            }
            if (item.assignments().size() > limit) {
                assignments.getChildren().add(detailLine(dark, "… и ещё " + (item.assignments().size() - limit) + " назначений"));
            }
        }

        VBox conflicts = new VBox(6);
        if (item.conflicts().isEmpty()) {
            conflicts.getChildren().add(detailLine(dark, "Конфликтов по дням не найдено."));
        } else {
            int limit = Math.min(item.conflicts().size(), 4);
            for (int i = 0; i < limit; i++) {
                ResourceLoadAnalyzer.LoadConflict conflict = item.conflicts().get(i);
                conflicts.getChildren().add(detailLine(dark,
                        DATE_FORMAT.format(conflict.date())
                                + " — общая загрузка " + format(conflict.totalLoad())
                                + ", перегруз +" + format(conflict.overload())));
            }
            if (item.conflicts().size() > limit) {
                conflicts.getChildren().add(detailLine(dark, "… и ещё " + (item.conflicts().size() - limit) + " конфликтных дней"));
            }
        }

        Label assignmentsHeader = sectionHeader(dark, "Задачи ресурса");
        Label conflictsHeader = sectionHeader(dark, "Конфликты и пики");

        VBox card = new VBox(10, title, meta, status, assignmentsHeader, assignments, conflictsHeader, conflicts);
        card.setPadding(new Insets(14));
        card.setStyle("-fx-background-color: " + color(dark,
                item.hasOverload() ? "rgba(69,10,10,0.42)" : "rgba(15,23,42,0.90)",
                item.hasOverload() ? "rgba(254,242,242,0.98)" : "rgba(255,255,255,0.98)") + ";"
                + "-fx-background-radius: 14; -fx-border-radius: 14;"
                + "-fx-border-color: " + color(dark,
                item.hasOverload() ? "rgba(248,113,113,0.38)" : "rgba(100,116,139,0.30)",
                item.hasOverload() ? "rgba(220,38,38,0.22)" : "rgba(148,163,184,0.22)") + ";");
        return card;
    }

    private static Label sectionHeader(boolean dark, String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 12px; -fx-font-weight: 900; -fx-text-fill: " + color(dark, "#93c5fd", "#1d4ed8") + ";");
        return label;
    }

    private static Label detailLine(boolean dark, String text) {
        Label label = new Label(text);
        applyWrappedTextStyle(label, "-fx-font-size: 12px; -fx-text-fill: " + color(dark, "#dbe4f0", "#334155") + ";");
        return label;
    }

    private static String suffix(ResourceLoadAnalyzer.ResourceLoadItem item) {
        return item.hasOverload() ? "  —  перегруз" : "";
    }

    private static String localizeType(String type) {
        if (type == null) {
            return "";
        }
        return switch (type) {
            case "PERSON" -> "Человек";
            case "EQUIPMENT" -> "Техника";
            case "CONTRACTOR" -> "Подрядчик";
            case "MATERIAL" -> "Материал";
            default -> type;
        };
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    private static String format(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.00001) {
            return String.valueOf((long) Math.rint(value));
        }
        return String.format(java.util.Locale.US, "%.2f", value);
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
}
