package io.angistrem.employeemanagement.util;

import io.angistrem.employeemanagement.model.Settings;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

public final class HotkeyManager {

    public record HotkeyAction(String title, String combination, Runnable action) {
    }

    private HotkeyManager() {
    }

    public static Map<String, String> defaultHotkeys() {
        return flattenGroups(new Settings());
    }

    public static Map<String, String> describe(Settings settings) {
        return flattenGroups(settings == null ? new Settings() : settings);
    }

    public static void install(Scene scene, Iterable<HotkeyAction> hotkeyActions) {
        if (scene == null || hotkeyActions == null) {
            return;
        }
        scene.getAccelerators().clear();
        for (HotkeyAction hotkeyAction : hotkeyActions) {
            if (hotkeyAction == null || hotkeyAction.action() == null) {
                continue;
            }
            KeyCombination combination = parse(hotkeyAction.combination());
            if (combination == null) {
                continue;
            }
            scene.getAccelerators().put(combination, hotkeyAction.action());
        }
    }

    public static void showHelp(Window owner, Settings settings) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Горячие клавиши");
        alert.setHeaderText("Быстрые действия приложения");
        if (owner != null) {
            alert.initOwner(owner);
        }
        SceneManager.applyThemeToDialog(alert);

        VBox content = new VBox(12);
        content.setStyle("-fx-background-color: linear-gradient(to bottom, #ffffff, #f8fbff);"
                + "-fx-padding: 14;");

        for (Map.Entry<String, Map<String, String>> group : grouped(settings).entrySet()) {
            content.getChildren().add(buildGroupCard(group.getKey(), group.getValue()));
        }

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefViewportWidth(620);
        scrollPane.setPrefViewportHeight(420);
        scrollPane.setStyle("-fx-background: #ffffff; -fx-background-color: #ffffff;");

        DialogPane pane = alert.getDialogPane();
        pane.setStyle("-fx-background-color: #ffffff;");
        pane.setContent(scrollPane);
        pane.setPrefWidth(700);
        pane.getButtonTypes().setAll(ButtonType.OK);
        alert.showAndWait();
    }

    public static boolean validateCombination(String value, Consumer<String> errorConsumer) {
        if (parse(value) != null) {
            return true;
        }
        if (errorConsumer != null) {
            errorConsumer.accept("Не удалось распознать комбинацию: " + value
                    + ". Примеры: Ctrl+S, Ctrl+Shift+P, F5, Ctrl+0.");
        }
        return false;
    }

    public static String formatForDisplay(String value) {
        if (value == null || value.isBlank()) {
            return "Не назначено";
        }
        return value.replace("Shortcut", "Ctrl");
    }

    private static VBox buildGroupCard(String title, Map<String, String> entries) {
        VBox card = new VBox(8);
        card.setStyle("-fx-background-color: #ffffff;"
                + "-fx-background-radius: 14;"
                + "-fx-border-color: #dbe4f0;"
                + "-fx-border-radius: 14;"
                + "-fx-padding: 12 14 12 14;");

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");

        GridPane table = new GridPane();
        table.setHgap(12);
        table.setVgap(8);

        int row = 0;
        for (Map.Entry<String, String> entry : entries.entrySet()) {
            Label action = new Label(entry.getKey());
            action.setStyle("-fx-font-size: 12.5px; -fx-text-fill: #334155;");

            Label combo = new Label(formatForDisplay(entry.getValue()));
            combo.setStyle("-fx-background-color: #eff6ff;"
                    + "-fx-border-color: #bfdbfe;"
                    + "-fx-background-radius: 8;"
                    + "-fx-border-radius: 8;"
                    + "-fx-padding: 4 10 4 10;"
                    + "-fx-font-size: 12px;"
                    + "-fx-font-weight: bold;"
                    + "-fx-text-fill: #1d4ed8;");
            combo.setMinWidth(120);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            HBox actionBox = new HBox(6, action, spacer);
            HBox.setHgrow(actionBox, Priority.ALWAYS);

            table.add(actionBox, 0, row);
            table.add(combo, 1, row);
            row++;
        }

        card.getChildren().addAll(titleLabel, table);
        return card;
    }

    private static Map<String, Map<String, String>> grouped(Settings settings) {
        Settings source = settings == null ? new Settings() : settings;
        Map<String, Map<String, String>> groups = new LinkedHashMap<>();

        Map<String, String> file = new LinkedHashMap<>();
        file.put("Новый проект", source.getHotkeyNewProject());
        file.put("Загрузить проект", source.getHotkeyOpenProject());
        file.put("Сохранить", source.getHotkeySaveProject());
        file.put("Сохранить как", source.getHotkeySaveAsProject());
        groups.put("Файл", file);

        Map<String, String> export = new LinkedHashMap<>();
        export.put("Печать проекта", source.getHotkeyPrintProject());
        export.put("Экспорт в PDF", source.getHotkeyExportPdf());
        export.put("Экспорт в CSV", source.getHotkeyExportCsv());
        groups.put("Печать и экспорт", export);

        Map<String, String> navigation = new LinkedHashMap<>();
        navigation.put("Фокус на поиск", source.getHotkeyFocusSearch());
        navigation.put("Пересчитать зависимости", source.getHotkeyRecalculate());
        navigation.put("Показать подсказку", source.getHotkeyShowHelp());
        navigation.put("Отменить удаление", source.getHotkeyUndoDelete());
        groups.put("Навигация", navigation);

        Map<String, String> chart = new LinkedHashMap<>();
        chart.put("Приблизить диаграмму", source.getHotkeyZoomIn());
        chart.put("Отдалить диаграмму", source.getHotkeyZoomOut());
        chart.put("Сбросить масштаб", source.getHotkeyZoomReset());
        groups.put("Диаграмма", chart);

        return groups;
    }

    private static Map<String, String> flattenGroups(Settings settings) {
        Map<String, String> flat = new LinkedHashMap<>();
        for (Map<String, String> group : grouped(settings).values()) {
            flat.putAll(group);
        }
        return flat;
    }

    private static KeyCombination parse(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim()
                .replace("Control", "Ctrl")
                .replace("control", "Ctrl")
                .replace("COMMAND", "Meta")
                .replace("command", "Meta");
        try {
            return KeyCombination.keyCombination(normalized);
        } catch (Exception ignored) {
            try {
                return KeyCombination.valueOf(normalized.toUpperCase(Locale.ROOT));
            } catch (Exception ignoredAgain) {
                return null;
            }
        }
    }
}
