package io.angistrem.employeemanagement.util;

import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.time.format.DateTimeFormatter;

public final class CriticalPathDialog {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private CriticalPathDialog() {
    }

    public static void show(Window owner, CriticalPathAnalyzer.Analysis analysis) {
        if (analysis == null || !analysis.isMeaningful()) {
            return;
        }

        boolean dark = isDarkTheme();

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("\u041a\u0440\u0438\u0442\u0438\u0447\u0435\u0441\u043a\u0438\u0439 \u043f\u0443\u0442\u044c");
        dialog.setHeaderText(null);
        dialog.setGraphic(null);
        if (owner != null) {
            dialog.initOwner(owner);
        }

        Label titleLabel = new Label("\u041a\u0440\u0438\u0442\u0438\u0447\u0435\u0441\u043a\u0438\u0439 \u043f\u0443\u0442\u044c \u043f\u0440\u043e\u0435\u043a\u0442\u0430");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: "
                + color(dark, "#f8fafc", "#0f172a") + ";");

        Label subtitleLabel = new Label(
                "\u041d\u0430\u0439\u0434\u0435\u043d \u0441\u0430\u043c\u044b\u0439 \u0434\u043b\u0438\u043d\u043d\u044b\u0439 \u0437\u0430\u0432\u0438\u0441\u0438\u043c\u044b\u0439 \u043c\u0430\u0440\u0448\u0440\u0443\u0442 \u043f\u043e\u0441\u043b\u0435 \u043f\u0435\u0440\u0435\u0441\u0447\u0435\u0442\u0430 \u0440\u0430\u0441\u043f\u0438\u0441\u0430\u043d\u0438\u044f. "
                        + "\u0418\u043c\u0435\u043d\u043d\u043e \u044d\u0442\u0430 \u0446\u0435\u043f\u043e\u0447\u043a\u0430 \u0441\u0438\u043b\u044c\u043d\u0435\u0435 \u0432\u0441\u0435\u0433\u043e \u0432\u043b\u0438\u044f\u0435\u0442 \u043d\u0430 \u043a\u043e\u043d\u0435\u0447\u043d\u044b\u0439 \u0441\u0440\u043e\u043a \u043f\u0440\u043e\u0435\u043a\u0442\u0430."
        );
        subtitleLabel.setWrapText(true);
        subtitleLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: "
                + color(dark, "#cbd5e1", "#475569") + ";");

        VBox hero = new VBox(6, titleLabel, subtitleLabel);
        hero.setPadding(new Insets(16));
        hero.setStyle(
                "-fx-background-color: "
                        + color(dark, "linear-gradient(to right, #7c3aed, #0f766e)",
                        "linear-gradient(to right, #ede9fe, #dcfce7)")
                        + ";"
                        + "-fx-background-radius: 14;"
                        + "-fx-border-radius: 14;"
                        + "-fx-effect: dropshadow(gaussian, rgba(15,23,42,0.12), 18, 0.18, 0, 4);"
        );

        HBox metrics = new HBox(10,
                metricCard("\u0414\u043b\u0438\u0442\u0435\u043b\u044c\u043d\u043e\u0441\u0442\u044c", analysis.totalDays() + " \u0434\u043d.", dark),
                metricCard("\u041d\u0430\u0447\u0430\u043b\u043e", DATE_FORMAT.format(analysis.start()), dark),
                metricCard("\u041e\u043a\u043e\u043d\u0447\u0430\u043d\u0438\u0435", DATE_FORMAT.format(analysis.end()), dark),
                metricCard("\u042d\u043b\u0435\u043c\u0435\u043d\u0442\u043e\u0432", String.valueOf(analysis.steps().size()), dark)
        );

        VBox stepsBox = new VBox(10);
        for (int i = 0; i < analysis.steps().size(); i++) {
            CriticalPathAnalyzer.PathStep step = analysis.steps().get(i);
            if (i > 0) {
                Label edge = new Label(step.incomingDependency() == null || step.incomingDependency().isBlank()
                        ? "\u0417\u0430\u0432\u0438\u0441\u0438\u043c\u043e\u0441\u0442\u044c"
                        : step.incomingDependency());
                edge.setStyle(
                        "-fx-font-size: 11px; -fx-font-weight: bold;"
                                + "-fx-text-fill: " + color(dark, "#bfdbfe", "#1d4ed8") + ";"
                                + "-fx-background-color: " + color(dark, "rgba(30,64,175,0.18)", "rgba(219,234,254,0.9)") + ";"
                                + "-fx-padding: 4 10 4 10; -fx-background-radius: 999;"
                );
                stepsBox.getChildren().add(edge);
            }
            stepsBox.getChildren().add(stepCard(step, dark));
        }

        ScrollPane scrollPane = new ScrollPane(stepsBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setPannable(true);
        scrollPane.setPrefViewportHeight(430);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        VBox content = new VBox(14, hero, metrics, scrollPane);
        content.setPadding(new Insets(8, 10, 4, 10));
        content.setPrefWidth(760);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
        SceneManager.applyThemeToDialog(dialog);
        dialog.getDialogPane().setPrefSize(820, 700);
        dialog.showAndWait();
    }

    private static VBox metricCard(String title, String value, boolean dark) {
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " + color(dark, "#94a3b8", "#64748b") + ";");

        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: "
                + color(dark, "#f8fafc", "#0f172a") + ";");

        VBox card = new VBox(4, titleLabel, valueLabel);
        card.setPadding(new Insets(12));
        card.setStyle(panelStyle(dark,
                color(dark, "rgba(15,23,42,0.84)", "#ffffff"),
                color(dark, "rgba(100,116,139,0.35)", "rgba(148,163,184,0.24)"),
                12));
        HBox.setHgrow(card, Priority.ALWAYS);
        card.setMaxWidth(Double.MAX_VALUE);
        return card;
    }

    private static VBox stepCard(CriticalPathAnalyzer.PathStep step, boolean dark) {
        Label keyLabel = new Label(step.key());
        keyLabel.setStyle(
                "-fx-font-size: 11px; -fx-font-weight: bold;"
                        + "-fx-text-fill: " + color(dark, "#dbeafe", "#1d4ed8") + ";"
                        + "-fx-background-color: " + color(dark, "rgba(37,99,235,0.18)", "rgba(219,234,254,0.95)") + ";"
                        + "-fx-padding: 4 10 4 10; -fx-background-radius: 999;"
        );

        Label nameLabel = new Label(step.label());
        nameLabel.setWrapText(true);
        nameLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: "
                + color(dark, "#f8fafc", "#0f172a") + ";");

        Label datesLabel = new Label(DATE_FORMAT.format(step.start()) + " - " + DATE_FORMAT.format(step.end()));
        datesLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: "
                + color(dark, "#cbd5e1", "#475569") + ";");

        VBox card = new VBox(8, keyLabel, nameLabel, datesLabel);
        card.setPadding(new Insets(14));
        card.setStyle(panelStyle(dark,
                color(dark, "rgba(15,23,42,0.9)", "#ffffff"),
                color(dark, "rgba(100,116,139,0.35)", "rgba(148,163,184,0.22)"),
                14));
        return card;
    }

    private static boolean isDarkTheme() {
        var settings = AppSettings.get();
        return settings != null && settings.getTheme() != null
                && "dark".equalsIgnoreCase(settings.getTheme().trim());
    }

    private static String color(boolean dark, String darkColor, String lightColor) {
        return dark ? darkColor : lightColor;
    }

    private static String panelStyle(boolean dark, String background, String border, int radius) {
        return "-fx-background-color: " + background + ";"
                + "-fx-background-radius: " + radius + ";"
                + "-fx-border-radius: " + radius + ";"
                + "-fx-border-color: " + border + ";"
                + "-fx-effect: dropshadow(gaussian, " + color(dark, "rgba(15,23,42,0.18)", "rgba(15,23,42,0.06)") + ", 18, 0.16, 0, 4);";
    }
}
