package io.angistrem.employeemanagement.dialog;

import io.angistrem.employeemanagement.controller.GroupController;
import io.angistrem.employeemanagement.model.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;

import java.io.IOException;

public class GroupDialog extends BaseDialog<Task> {
    private GroupController controller;
    private final Task group;

    public GroupDialog() {
        this(null);
    }

    public GroupDialog(Task group) {
        super(group == null ? "Новая группа задач" : "Редактирование группы задач");
        this.group = group;

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/io/angistrem/employeemanagement/view/dialog/group-dialog.fxml"));
            Parent parent = loader.load();
            controller = loader.getController();

            Scene scene = new Scene(parent);
            configureScene(scene);
            stage.setWidth(620);
            stage.setHeight(340);
        } catch (IOException e) {
            throw new RuntimeException("Не удалось открыть диалог группы задач", e);
        }
    }

    @Override
    public void show() {
        if (group != null) {
            controller.setGroup(group);
        }
        stage.showAndWait();
        result = controller.getGroup();
    }

    @Override
    public Task getResult() {
        return result;
    }
}
