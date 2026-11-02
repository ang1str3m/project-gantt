package io.angistrem.employeemanagement.dialog;

import io.angistrem.employeemanagement.controller.TaskController;
import io.angistrem.employeemanagement.model.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;

import java.io.IOException;
import java.util.List;

public class TaskDialog extends BaseDialog<Task> {
    private TaskController controller;
    private final Task task;
    private final List<Task> availableTasks;

    public TaskDialog() {
        this(null, null);
    }

    public TaskDialog(Task task) {
        this(task, null);
    }

    public TaskDialog(List<Task> availableTasks) {
        this(null, availableTasks);
    }

    public TaskDialog(Task task, List<Task> availableTasks) {
        super(task == null ? "\u041d\u043e\u0432\u0430\u044f \u0437\u0430\u0434\u0430\u0447\u0430" : "\u0420\u0435\u0434\u0430\u043a\u0442\u0438\u0440\u043e\u0432\u0430\u043d\u0438\u0435 \u0437\u0430\u0434\u0430\u0447\u0438");
        this.task = task;
        this.availableTasks = availableTasks;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/io/angistrem/employeemanagement/view/dialog/task-dialog.fxml"));
            Parent parent = loader.load();
            controller = loader.getController();

            Scene scene = new Scene(parent);
            configureScene(scene);
            stage.setWidth(860);
            stage.setHeight(860);
        } catch (IOException e) {
            throw new RuntimeException("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043e\u0442\u043a\u0440\u044b\u0442\u044c \u0434\u0438\u0430\u043b\u043e\u0433 \u0437\u0430\u0434\u0430\u0447\u0438", e);
        }
    }

    @Override
    public void show() {
        controller.setAvailableTasks(availableTasks);
        if (task != null) {
            controller.setTask(task);
        }
        stage.showAndWait();
        result = controller.getTask();
    }

    @Override
    public Task getResult() {
        return result;
    }
}
