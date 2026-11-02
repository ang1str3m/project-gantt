package io.angistrem.employeemanagement;

import io.angistrem.employeemanagement.model.Settings;
import io.angistrem.employeemanagement.repository.DatabaseManager;
import io.angistrem.employeemanagement.repository.UserRepository;
import io.angistrem.employeemanagement.service.RuntimeModeService;
import io.angistrem.employeemanagement.util.AppInfo;
import io.angistrem.employeemanagement.util.AppPaths;
import io.angistrem.employeemanagement.util.AppSettings;
import io.angistrem.employeemanagement.util.SceneManager;
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class App extends Application {
    private static final long MIN_SPLASH_MILLIS = 15_000L;

    private final UserRepository userRepository = new UserRepository();
    private final RuntimeModeService runtimeModeService = new RuntimeModeService();

    @Override
    public void start(Stage stage) {
        Scene splashScene = buildSplashScene();
        stage.setTitle("Gantrem");
        stage.getIcons().setAll(loadWindowIcons());
        stage.setScene(splashScene);
        stage.setMinWidth(900);
        stage.setMinHeight(640);
        stage.centerOnScreen();
        stage.show();

        Label statusLabel = (Label) splashScene.lookup("#splashStatus");
        Label stageLabel = (Label) splashScene.lookup("#splashStage");
        ProgressBar progressBar = (ProgressBar) splashScene.lookup("#splashProgress");
        Label percentLabel = (Label) splashScene.lookup("#splashPercent");

        Task<Boolean> bootstrapTask = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                long startedAt = System.currentTimeMillis();

                publishStage("\u0428\u0430\u0433 1 \u0438\u0437 4", "\u041f\u043e\u0434\u0433\u043e\u0442\u043e\u0432\u043a\u0430 \u0440\u0430\u0431\u043e\u0447\u0435\u0433\u043e \u043f\u0440\u043e\u0441\u0442\u0440\u0430\u043d\u0441\u0442\u0432\u0430", 0.12);
                Thread.sleep(1500);

                publishStage("\u0428\u0430\u0433 2 \u0438\u0437 4", "\u0418\u043d\u0438\u0446\u0438\u0430\u043b\u0438\u0437\u0430\u0446\u0438\u044f \u0445\u0440\u0430\u043d\u0438\u043b\u0438\u0449\u0430 \u0438 \u0441\u0435\u0440\u0432\u0438\u0441\u043e\u0432", 0.34);
                DatabaseManager.initDatabase();
                if (runtimeModeService.isRunningFromIde()) {
                    userRepository.ensureQuickAccessAdmin();
                }
                Thread.sleep(1000);

                publishStage("\u0428\u0430\u0433 3 \u0438\u0437 4", "\u041f\u0440\u043e\u0432\u0435\u0440\u043a\u0430 \u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u0442\u0435\u043b\u044f \u0438 \u043f\u0430\u0440\u0430\u043c\u0435\u0442\u0440\u043e\u0432 \u0437\u0430\u043f\u0443\u0441\u043a\u0430", 0.58);
                boolean autoLogin = shouldAutoLogin();
                Thread.sleep(1500);

                publishStage("\u0428\u0430\u0433 4 \u0438\u0437 4", "\u041f\u043e\u0434\u0433\u043e\u0442\u043e\u0432\u043a\u0430 \u0438\u043d\u0442\u0435\u0440\u0444\u0435\u0439\u0441\u0430 Gantrem", 0.74);
                holdSplashUntilMinimum(startedAt);

                updateMessage("\u0413\u043e\u0442\u043e\u0432\u043e \u043a \u0437\u0430\u043f\u0443\u0441\u043a\u0443");
                updateProgress(1.0, 1.0);
                return autoLogin;
            }

            private void publishStage(String shortStage, String message, double progress) {
                updateTitle(shortStage);
                updateMessage(message);
                updateProgress(progress, 1.0);
            }

            private void holdSplashUntilMinimum(long startedAt) throws InterruptedException {
                long elapsed = System.currentTimeMillis() - startedAt;
                long remaining = Math.max(0L, MIN_SPLASH_MILLIS - elapsed);
                if (remaining == 0L) {
                    return;
                }

                int steps = Math.max(1, (int) Math.ceil(remaining / 180.0));
                double from = Math.max(getProgress(), 0.74);
                for (int i = 1; i <= steps; i++) {
                    double ratio = (double) i / (double) steps;
                    double progress = from + ((0.98 - from) * ratio);
                    updateTitle("\u0424\u0438\u043d\u0430\u043b\u044c\u043d\u044b\u0439 \u044d\u0442\u0430\u043f");
                    updateMessage("\u041f\u043e\u043b\u0438\u0440\u0443\u0435\u043c \u0438\u043d\u0442\u0435\u0440\u0444\u0435\u0439\u0441 \u0438 \u043f\u043e\u0434\u0433\u043e\u0442\u0430\u0432\u043b\u0438\u0432\u0430\u0435\u043c \u0440\u0430\u0431\u043e\u0447\u0443\u044e \u0441\u0446\u0435\u043d\u0443");
                    updateProgress(progress, 1.0);
                    Thread.sleep(Math.max(60L, remaining / steps));
                }
            }
        };

        if (statusLabel != null) {
            statusLabel.textProperty().bind(bootstrapTask.messageProperty());
        }
        if (stageLabel != null) {
            stageLabel.textProperty().bind(bootstrapTask.titleProperty());
        }
        if (progressBar != null) {
            progressBar.progressProperty().bind(bootstrapTask.progressProperty());
        }
        if (percentLabel != null) {
            percentLabel.textProperty().bind(bootstrapTask.progressProperty().multiply(100.0).asString("%.0f%%"));
        }

        bootstrapTask.setOnSucceeded(event -> {
            unbindSplash(statusLabel, stageLabel, progressBar, percentLabel);
            SceneManager.init(stage);
            if (Boolean.TRUE.equals(bootstrapTask.getValue())) {
                SceneManager.switchTo("view/menu-view-controller.fxml");
            } else {
                SceneManager.switchTo("view/login-view-controller.fxml");
            }
        });

        bootstrapTask.setOnFailed(event -> {
            unbindSplash(statusLabel, stageLabel, progressBar, percentLabel);
            if (stageLabel != null) {
                stageLabel.setText("\u041e\u0448\u0438\u0431\u043a\u0430 \u0437\u0430\u043f\u0443\u0441\u043a\u0430");
            }
            if (statusLabel != null) {
                statusLabel.setText("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043a\u043e\u0440\u0440\u0435\u043a\u0442\u043d\u043e \u0438\u043d\u0438\u0446\u0438\u0430\u043b\u0438\u0437\u0438\u0440\u043e\u0432\u0430\u0442\u044c \u043f\u0440\u0438\u043b\u043e\u0436\u0435\u043d\u0438\u0435");
            }
            if (progressBar != null) {
                progressBar.setProgress(1.0);
                progressBar.setStyle(
                        "-fx-accent: #ef4444;"
                                + "-fx-control-inner-background: rgba(148,163,184,0.18);"
                );
            }
            if (percentLabel != null) {
                percentLabel.setText("100%");
            }
            SceneManager.init(stage);
            SceneManager.switchTo("view/login-view-controller.fxml");
        });

        Thread worker = new Thread(bootstrapTask, "gantrem-bootstrap");
        worker.setDaemon(true);
        worker.start();
    }

    private void unbindSplash(Label statusLabel, Label stageLabel, ProgressBar progressBar, Label percentLabel) {
        if (statusLabel != null) {
            statusLabel.textProperty().unbind();
        }
        if (stageLabel != null) {
            stageLabel.textProperty().unbind();
        }
        if (progressBar != null) {
            progressBar.progressProperty().unbind();
        }
        if (percentLabel != null) {
            percentLabel.textProperty().unbind();
        }
    }

    private Scene buildSplashScene() {
        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: linear-gradient(to bottom right, #06111f 0%, #0f172a 38%, #111827 100%);");

        Circle glowLeft = new Circle(240, Color.rgb(59, 130, 246, 0.16));
        glowLeft.setTranslateX(-260);
        glowLeft.setTranslateY(-110);

        Circle glowRight = new Circle(280, Color.rgb(20, 184, 166, 0.12));
        glowRight.setTranslateX(310);
        glowRight.setTranslateY(180);

        Rectangle panelShadow = new Rectangle(0, 0, 760, 470);
        panelShadow.setArcWidth(42);
        panelShadow.setArcHeight(42);
        panelShadow.setFill(Color.rgb(2, 6, 23, 0.28));
        panelShadow.setTranslateY(10);

        VBox card = new VBox(20);
        card.setMaxWidth(720);
        card.setPadding(new Insets(30, 34, 28, 34));
        card.setStyle(
                "-fx-background-color: linear-gradient(to bottom, rgba(15,23,42,0.97), rgba(9,14,28,0.985));"
                        + "-fx-background-radius: 34;"
                        + "-fx-border-color: rgba(148,163,184,0.16);"
                        + "-fx-border-radius: 34;"
        );

        Label badge = new Label("PROJECT CONTROL PLATFORM");
        badge.setStyle(
                "-fx-font-size: 10px; -fx-font-weight: bold; -fx-letter-spacing: 1px;"
                        + "-fx-text-fill: #cbd5e1;"
                        + "-fx-background-color: rgba(30,41,59,0.82);"
                        + "-fx-padding: 7 12 7 12; -fx-background-radius: 999;"
        );

        ImageView logoView = new ImageView(loadLogo());
        logoView.setFitWidth(104);
        logoView.setFitHeight(104);
        logoView.setPreserveRatio(true);

        StackPane logoPlate = new StackPane(logoView);
        logoPlate.setPrefSize(140, 140);
        logoPlate.setMaxSize(140, 140);
        logoPlate.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, rgba(30,64,175,0.85), rgba(15,118,110,0.88));"
                        + "-fx-background-radius: 38;"
                        + "-fx-border-color: rgba(255,255,255,0.20);"
                        + "-fx-border-radius: 38;"
        );

        VBox titleBox = new VBox(8);
        titleBox.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Gantrem");
        title.setFont(Font.font("Segoe UI", FontWeight.EXTRA_BOLD, 38));
        title.setTextFill(Color.web("#f8fafc"));
        Label subtitle = new Label("\u041f\u0440\u0435\u043c\u0438\u0430\u043b\u044c\u043d\u044b\u0439 \u0438\u043d\u0441\u0442\u0440\u0443\u043c\u0435\u043d\u0442 \u0443\u043f\u0440\u0430\u0432\u043b\u0435\u043d\u0438\u044f \u0437\u0430\u0434\u0430\u0447\u0430\u043c\u0438, \u0441\u0440\u043e\u043a\u0430\u043c\u0438 \u0438 \u0437\u0430\u0432\u0438\u0441\u0438\u043c\u043e\u0441\u0442\u044f\u043c\u0438");
        subtitle.setWrapText(true);
        subtitle.setMaxWidth(430);
        subtitle.setFont(Font.font("Segoe UI", FontWeight.MEDIUM, 14));
        subtitle.setTextFill(Color.web("#cbd5e1"));
        Label featureLine = new Label("\u0414\u0438\u0430\u0433\u0440\u0430\u043c\u043c\u0430 \u0413\u0430\u043d\u0442\u0430  \u2022  \u041a\u0440\u0438\u0442\u0438\u0447\u0435\u0441\u043a\u0438\u0439 \u043f\u0443\u0442\u044c  \u2022  \u041f\u0435\u0447\u0430\u0442\u044c \u0438 PDF  \u2022  \u0424\u0438\u043d\u0430\u043d\u0441\u043e\u0432\u044b\u0435 \u043e\u0433\u0440\u0430\u043d\u0438\u0447\u0435\u043d\u0438\u044f");
        featureLine.setWrapText(true);
        featureLine.setFont(Font.font("Segoe UI", 12));
        featureLine.setTextFill(Color.web("#93c5fd"));

        HBox heroRow = new HBox(22, logoPlate, titleBox);
        heroRow.setAlignment(Pos.CENTER_LEFT);

        Label stageLabel = new Label("\u0418\u043d\u0438\u0446\u0438\u0430\u043b\u0438\u0437\u0430\u0446\u0438\u044f");
        stageLabel.setId("splashStage");
        stageLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 13));
        stageLabel.setTextFill(Color.web("#e2e8f0"));

        Label statusLabel = new Label("\u0417\u0430\u043f\u0443\u0441\u043a \u043f\u0440\u0438\u043b\u043e\u0436\u0435\u043d\u0438\u044f");
        statusLabel.setId("splashStatus");
        statusLabel.setFont(Font.font("Segoe UI", 13));
        statusLabel.setTextFill(Color.web("#94a3b8"));

        ProgressBar progressBar = new ProgressBar(0);
        progressBar.setId("splashProgress");
        progressBar.setPrefWidth(560);
        progressBar.setPrefHeight(12);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setStyle(
                "-fx-accent: #38bdf8;"
                        + "-fx-control-inner-background: rgba(148,163,184,0.18);"
                        + "-fx-background-insets: 0;"
        );

        Label percentLabel = new Label("0%");
        percentLabel.setId("splashPercent");
        percentLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        percentLabel.setTextFill(Color.web("#f8fafc"));
        percentLabel.setMinWidth(52);
        percentLabel.setAlignment(Pos.CENTER_RIGHT);

        HBox progressRow = new HBox(16, progressBar, percentLabel);
        progressRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(progressBar, Priority.ALWAYS);

        VBox progressBox = new VBox(10, stageLabel, statusLabel, progressRow);
        progressBox.setPadding(new Insets(20, 22, 20, 22));
        progressBox.setStyle(
                "-fx-background-color: rgba(15,23,42,0.72);"
                        + "-fx-background-radius: 20;"
                        + "-fx-border-color: rgba(148,163,184,0.16);"
                        + "-fx-border-radius: 20;"
        );

        Label footerLeft = new Label("\u0420\u0430\u0437\u0440\u0430\u0431\u043e\u0442\u043a\u0430: Angistrem");
        footerLeft.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 11));
        footerLeft.setTextFill(Color.web("#64748b"));

        Label footerRight = new Label("\u0412\u0435\u0440\u0441\u0438\u044f " + AppInfo.currentVersion());
        footerRight.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 11));
        footerRight.setTextFill(Color.web("#64748b"));

        Region footerSpacer = new Region();
        HBox.setHgrow(footerSpacer, Priority.ALWAYS);

        HBox footer = new HBox(12, footerLeft, footerSpacer, footerRight);
        footer.setAlignment(Pos.CENTER_LEFT);

        card.getChildren().addAll(badge, heroRow, progressBox, footer);

        root.getChildren().addAll(glowLeft, glowRight, panelShadow, card);
        StackPane.setAlignment(card, Pos.CENTER);

        return new Scene(root, 1040, 720);
    }

    private Image loadLogo() {
        try (InputStream stream = getClass().getResourceAsStream("/io/angistrem/employeemanagement/img/Logo_for_app.png")) {
            if (stream != null) {
                return new Image(stream);
            }
        } catch (Exception ignored) {
            // Fallback below.
        }

        return new Image("data:image/png;base64,"
                + "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO7ZL6sAAAAASUVORK5CYII=");
    }

    private List<Image> loadWindowIcons() {
        List<Image> icons = new ArrayList<>();
        String[] candidates = {
                "/io/angistrem/employeemanagement/img/gantrem-icon-16.png",
                "/io/angistrem/employeemanagement/img/gantrem-icon-24.png",
                "/io/angistrem/employeemanagement/img/gantrem-icon-32.png",
                "/io/angistrem/employeemanagement/img/gantrem-icon-48.png",
                "/io/angistrem/employeemanagement/img/gantrem-icon-64.png",
                "/io/angistrem/employeemanagement/img/gantrem-icon-128.png",
                "/io/angistrem/employeemanagement/img/gantrem-icon-256.png",
                "/io/angistrem/employeemanagement/img/gantrem-icon-512.png",
                "/io/angistrem/employeemanagement/img/gantrem-icon-1024.png",
                "/io/angistrem/employeemanagement/img/Logo_for_app.png"
        };

        for (String candidate : candidates) {
            try (InputStream stream = getClass().getResourceAsStream(candidate)) {
                if (stream != null) {
                    icons.add(new Image(stream));
                }
            } catch (Exception ignored) {
                // Skip unreadable icon candidate.
            }
        }
        return icons;
    }

    private boolean shouldAutoLogin() {
        Settings settings = AppSettings.get();
        if (settings == null || !settings.isAutoLogin()) {
            return false;
        }

        String lastUser = settings.getLastUser();
        if (lastUser == null || lastUser.isBlank()) {
            return false;
        }

        String normalizedLogin = lastUser.trim();
        String machineId = AppPaths.getCurrentMachineId();
        String savedMachineId = settings.getAutoLoginMachineId();
        if (savedMachineId == null || savedMachineId.isBlank() || !machineId.equalsIgnoreCase(savedMachineId.trim())) {
            settings.setAutoLogin(false);
            settings.setAutoLoginVersion("");
            settings.setAutoLoginMachineId("");
            AppSettings.save(settings);
            return false;
        }

        String autoLoginVersion = settings.getAutoLoginVersion();
        if (autoLoginVersion == null || !AppInfo.currentVersion().equals(autoLoginVersion)) {
            settings.setAutoLoginVersion(AppInfo.currentVersion());
            settings.setLastUser(normalizedLogin);
            settings.setAutoLoginMachineId(machineId);
            AppSettings.save(settings);
        }

        return userRepository.findByLogin(normalizedLogin) != null;
    }
}
