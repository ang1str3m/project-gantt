package io.angistrem.employeemanagement.controller;

import io.angistrem.employeemanagement.model.User;
import io.angistrem.employeemanagement.repository.UserRepository;
import io.angistrem.employeemanagement.util.PasswordUtils;
import io.angistrem.employeemanagement.util.SceneManager;
import io.angistrem.employeemanagement.util.ValidationUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class RegisterController {
    private User user;
    private final UserRepository userRepository = new UserRepository();

    @FXML private TextField name;
    @FXML private TextField login;
    @FXML private PasswordField password;
    @FXML private PasswordField confirmPassword;

    @FXML
    private void initialize() {
        name.textProperty().addListener((obs, oldVal, newVal) -> ValidationUtils.clearValidationState(name));
        login.textProperty().addListener((obs, oldVal, newVal) -> ValidationUtils.clearValidationState(login));
        password.textProperty().addListener((obs, oldVal, newVal) -> ValidationUtils.clearValidationState(password));
        confirmPassword.textProperty().addListener((obs, oldVal, newVal) -> ValidationUtils.clearValidationState(confirmPassword));
    }

    @FXML
    private void handleOk() {
        ValidationUtils.clearValidationState(name);
        ValidationUtils.clearValidationState(login);
        ValidationUtils.clearValidationState(password);
        ValidationUtils.clearValidationState(confirmPassword);
        String nameValue = safeText(name);
        String loginValue = safeText(login);
        String passwordValue = safePassword(password);
        String confirmPasswordValue = safePassword(confirmPassword);

        if (nameValue.isBlank() || loginValue.isBlank() || passwordValue.isBlank() || confirmPasswordValue.isBlank()) {
            if (nameValue.isBlank()) ValidationUtils.markInvalid(name);
            if (loginValue.isBlank()) ValidationUtils.markInvalid(login);
            if (passwordValue.isBlank()) ValidationUtils.markInvalid(password);
            if (confirmPasswordValue.isBlank()) ValidationUtils.markInvalid(confirmPassword);
            showError("Не все поля заполнены", "Заполните все обязательные поля.");
            return;
        }

        if (loginValue.length() < 4 || !loginValue.matches("[A-Za-z0-9._-]+")) {
            ValidationUtils.markInvalid(login);
            showError("Некорректный логин", "Логин должен содержать не менее 4 символов и только буквы, цифры, '.', '_' или '-'.");
            return;
        }

        if (!isStrongPassword(passwordValue)) {
            ValidationUtils.markInvalid(password);
            showError("Слабый пароль", "Пароль должен быть не короче 8 символов и содержать строчные, заглавные буквы и цифры.");
            return;
        }

        if (!passwordValue.equals(confirmPasswordValue)) {
            ValidationUtils.markInvalid(password);
            ValidationUtils.markInvalid(confirmPassword);
            showError("Пароли не совпадают", "Проверьте поле подтверждения пароля.");
            return;
        }

        if (userRepository.findByLogin(loginValue) != null) {
            ValidationUtils.markInvalid(login);
            showError("Ошибка регистрации", "Пользователь с таким логином уже существует.");
            return;
        }

        user = new User(nameValue, loginValue, PasswordUtils.hash(passwordValue));
        close();
    }

    @FXML
    private void handleCancel() {
        user = null;
        close();
    }

    public User getUser() {
        return user;
    }

    private boolean isStrongPassword(String value) {
        return value.length() >= 8
                && value.chars().anyMatch(Character::isUpperCase)
                && value.chars().anyMatch(Character::isLowerCase)
                && value.chars().anyMatch(Character::isDigit);
    }

    private String safeText(TextField field) {
        return field.getText() == null ? "" : field.getText().trim();
    }

    private String safePassword(PasswordField field) {
        return field.getText() == null ? "" : field.getText();
    }

    private void close() {
        Stage stage = (Stage) name.getScene().getWindow();
        stage.close();
    }

    private void showError(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Регистрация");
        alert.setHeaderText(header);
        alert.setContentText(message);
        SceneManager.applyThemeToDialog(alert);
        alert.showAndWait();
    }
}
