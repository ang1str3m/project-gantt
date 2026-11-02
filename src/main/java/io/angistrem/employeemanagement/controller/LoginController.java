package io.angistrem.employeemanagement.controller;

import io.angistrem.employeemanagement.dialog.RegisterDialog;
import io.angistrem.employeemanagement.model.Settings;
import io.angistrem.employeemanagement.model.User;
import io.angistrem.employeemanagement.repository.UserRepository;
import io.angistrem.employeemanagement.util.AppSettings;
import io.angistrem.employeemanagement.util.AppInfo;
import io.angistrem.employeemanagement.util.AppPaths;
import io.angistrem.employeemanagement.util.PasswordUtils;
import io.angistrem.employeemanagement.util.SceneManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoginController {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoginController.class);

    @FXML private Button login;
    @FXML private HBox loginRoot;
    @FXML private TextField loginField;
    @FXML private PasswordField passwordField;
    @FXML private TextField passwordVisibleField;
    @FXML private Button togglePasswordButton;
    @FXML private FontIcon passwordEyeIcon;

    private final UserRepository userRepository = new UserRepository();
    private boolean passwordVisible = false;

    @FXML
    public void initialize() {
        applyThemeVariant();
        passwordVisibleField.textProperty().bindBidirectional(passwordField.textProperty());
        setPasswordVisible(false);

        Settings settings = AppSettings.get();
        if (settings != null && settings.getLastUser() != null && !settings.getLastUser().isBlank()) {
            loginField.setText(settings.getLastUser().trim());
        }
        tryAutoLoginFallback(settings);
    }

    @FXML
    private void handleLogin() {
        String enteredLogin = loginField.getText() == null ? "" : loginField.getText().trim();
        String enteredPassword = passwordField.getText() == null ? "" : passwordField.getText();

        if (enteredLogin.isBlank() || enteredPassword.isBlank()) {
            showError("Ошибка авторизации", "Введите логин и пароль.");
            return;
        }

        User user = userRepository.findByLogin(enteredLogin);
        if (user != null && PasswordUtils.matches(enteredPassword, user.getPassword())) {
            if (PasswordUtils.needsRehash(user.getPassword())) {
                try {
                    String upgradedHash = PasswordUtils.hash(enteredPassword);
                    userRepository.updatePassword(user.getId(), upgradedHash);
                    user.setPassword(upgradedHash);
                } catch (RuntimeException e) {
                    LOGGER.warn("Failed to upgrade legacy password hash for user {}", user.getLogin(), e);
                }
            }
            saveLastUser(enteredLogin);
            SceneManager.switchTo("view/menu-view-controller.fxml");
            return;
        }

        showError("Ошибка авторизации", "Аккаунт не найден или указан неверный логин/пароль.");
    }

    @FXML
    private void handleTogglePasswordVisibility() {
        setPasswordVisible(!passwordVisible);
    }

    @FXML
    private void handleRegister() {
        RegisterDialog dialog = new RegisterDialog();
        dialog.show();
        User newUser = dialog.getResult();

        if (newUser == null) {
            return;
        }

        boolean saved = userRepository.save(newUser);
        if (saved) {
            Alert success = new Alert(Alert.AlertType.INFORMATION);
            success.setTitle("Регистрация");
            success.setHeaderText(null);
            success.setContentText("Пользователь успешно зарегистрирован.");
            SceneManager.applyThemeToDialog(success);
            success.showAndWait();
        } else {
            showError("Ошибка регистрации", "Пользователь с таким логином уже существует.");
        }
    }

    @FXML
    public void handleClose() {
        System.exit(0);
    }

    private void setPasswordVisible(boolean visible) {
        passwordVisible = visible;
        passwordVisibleField.setManaged(visible);
        passwordVisibleField.setVisible(visible);
        passwordField.setManaged(!visible);
        passwordField.setVisible(!visible);

        if (passwordEyeIcon != null) {
            passwordEyeIcon.setIconLiteral(visible ? "mdi2e-eye-off-outline" : "mdi2e-eye-outline");
        }
    }

    private void saveLastUser(String login) {
        Settings settings = AppSettings.get();
        if (settings == null) {
            return;
        }
        settings.setLastUser(login == null ? "" : login.trim());
        settings.setAutoLoginVersion(settings.isAutoLogin() ? AppInfo.currentVersion() : "");
        settings.setAutoLoginMachineId(settings.isAutoLogin() ? AppPaths.getCurrentMachineId() : "");
        AppSettings.save(settings);
    }

    private void tryAutoLoginFallback(Settings settings) {
        if (settings == null || !settings.isAutoLogin()) {
            return;
        }
        String machineId = AppPaths.getCurrentMachineId();
        String savedMachineId = settings.getAutoLoginMachineId();
        if (savedMachineId == null || savedMachineId.isBlank() || !machineId.equalsIgnoreCase(savedMachineId.trim())) {
            settings.setAutoLogin(false);
            settings.setAutoLoginVersion("");
            settings.setAutoLoginMachineId("");
            AppSettings.save(settings);
            return;
        }
        String lastUser = settings.getLastUser();
        if (lastUser == null || lastUser.isBlank()) {
            return;
        }
        if (userRepository.findByLogin(lastUser.trim()) == null) {
            return;
        }
        Platform.runLater(() -> SceneManager.switchTo("view/menu-view-controller.fxml"));
    }

    private void showError(String title, String message) {
        Alert error = new Alert(Alert.AlertType.ERROR);
        error.setTitle(title);
        error.setHeaderText(null);
        error.setContentText(message);
        SceneManager.applyThemeToDialog(error);
        error.showAndWait();
    }

    private void applyThemeVariant() {
        if (loginRoot == null) {
            return;
        }
        loginRoot.getStyleClass().removeAll("login-light", "login-dark");
        String theme = AppSettings.get() == null ? "light" : AppSettings.get().getTheme();
        if ("dark".equalsIgnoreCase(theme)) {
            loginRoot.getStyleClass().add("login-dark");
        } else {
            loginRoot.getStyleClass().add("login-light");
        }
    }
}
