package io.angistrem.employeemanagement.util;

import io.angistrem.employeemanagement.model.Settings;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public final class SceneManager {
    private static Stage primaryStage;
    private static final String BASE_THEME_STYLESHEET =
            SceneManager.class.getResource("/io/angistrem/employeemanagement/styles/app-base.css").toExternalForm();
    private static final String SETTINGS_PREMIUM_STYLESHEET =
            SceneManager.class.getResource("/io/angistrem/employeemanagement/styles/settings-premium.css").toExternalForm();
    private static final String DARK_THEME_STYLESHEET =
            SceneManager.class.getResource("/io/angistrem/employeemanagement/styles/theme-dark.css").toExternalForm();

    private SceneManager() {
    }

    public static void init(Stage stage) {
        primaryStage = stage;
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);
    }

    public static void switchTo(String fxmlPath) {
        ensureInitialized();
        try {
            Parent root = loadRoot("/io/angistrem/employeemanagement/" + fxmlPath);
            Scene scene = new Scene(root);
            applyTheme(scene);
            primaryStage.setScene(scene);
            maximize();
            primaryStage.show();
        } catch (IOException e) {
            throw new RuntimeException("Не удалось загрузить сцену: " + fxmlPath, e);
        }
    }

    public static <T> T loadFXML(String absoluteResourcePath) throws IOException {
        FXMLLoader loader = new FXMLLoader(getResource(absoluteResourcePath));
        loader.load();
        return loader.getController();
    }

    public static Stage getPrimaryStage() {
        ensureInitialized();
        return primaryStage;
    }

    public static void applyThemeToCurrentScene() {
        ensureInitialized();
        if (primaryStage.getScene() != null) {
            applyTheme(primaryStage.getScene());
        }
    }

    public static void applyThemeToDialog(Dialog<?> dialog) {
        if (dialog == null) {
            return;
        }
        applyThemeToDialogPane(dialog.getDialogPane());
    }

    public static void applyThemeToDialogPane(DialogPane dialogPane) {
        if (dialogPane == null) {
            return;
        }
        Scene scene = dialogPane.getScene();
        if (scene != null) {
            applyTheme(scene);
        }
    }

    private static Parent loadRoot(String absoluteResourcePath) throws IOException {
        FXMLLoader loader = new FXMLLoader(getResource(absoluteResourcePath));
        return loader.load();
    }

    private static URL getResource(String absoluteResourcePath) {
        URL resource = SceneManager.class.getResource(absoluteResourcePath);
        if (resource == null) {
            throw new RuntimeException("FXML не найден: " + absoluteResourcePath);
        }
        return resource;
    }

    private static void maximize() {
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        primaryStage.setX(bounds.getMinX());
        primaryStage.setY(bounds.getMinY());
        primaryStage.setWidth(bounds.getWidth());
        primaryStage.setHeight(bounds.getHeight());
        primaryStage.setMaximized(true);
    }

    private static void ensureInitialized() {
        if (primaryStage == null) {
            throw new IllegalStateException("SceneManager не инициализирован");
        }
    }

    public static void applyTheme(Scene scene) {
        if (scene == null) {
            return;
        }
        if (!scene.getStylesheets().contains(BASE_THEME_STYLESHEET)) {
            scene.getStylesheets().add(BASE_THEME_STYLESHEET);
        }
        if (!scene.getStylesheets().contains(SETTINGS_PREMIUM_STYLESHEET)) {
            scene.getStylesheets().add(SETTINGS_PREMIUM_STYLESHEET);
        }
        scene.getStylesheets().remove(DARK_THEME_STYLESHEET);

        Settings settings = AppSettings.get();
        String theme = settings == null || settings.getTheme() == null ? "light" : settings.getTheme().trim().toLowerCase();
        if ("dark".equals(theme)) {
            scene.getStylesheets().add(DARK_THEME_STYLESHEET);
        }
        if (scene.getRoot() != null) {
            scene.getRoot().getStyleClass().removeAll("text-compact", "text-standard", "text-presentation");
            String textStyle = settings == null || settings.getTextStyle() == null
                    ? "standard"
                    : settings.getTextStyle().trim().toLowerCase();
            String rootStyleClass = switch (textStyle) {
                case "compact" -> "text-compact";
                case "presentation" -> "text-presentation";
                default -> "text-standard";
            };
            scene.getRoot().getStyleClass().add(rootStyleClass);
            String fontFamily = settings == null || settings.getFontFamily() == null || settings.getFontFamily().isBlank()
                    ? "Segoe UI"
                    : settings.getFontFamily().trim().replace("'", "\\'");
            scene.getRoot().setStyle("-fx-font-family: '" + fontFamily + "';");
        }
    }
}
