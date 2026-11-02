package io.angistrem.employeemanagement.dialog;

import io.angistrem.employeemanagement.controller.RegisterController;
import io.angistrem.employeemanagement.model.User;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;

import java.io.IOException;

public class RegisterDialog extends BaseDialog<User> {
    private RegisterController controller;

    public RegisterDialog() {
        super("Регистрация нового пользователя");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/io/angistrem/employeemanagement/view/dialog/register-dialog.fxml"));
            Parent parent = loader.load();
            controller = loader.getController();

            Scene scene = new Scene(parent);
            configureScene(scene);
        } catch (IOException e) {
            throw new RuntimeException("Не удалось открыть диалог регистрации", e);
        }
    }

    @Override
    public void show() {
        stage.showAndWait();
        result = controller.getUser();
    }

    @Override
    public User getResult() {
        return result;
    }
}
