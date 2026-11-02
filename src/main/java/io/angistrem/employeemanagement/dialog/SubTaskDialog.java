package io.angistrem.employeemanagement.dialog;

import io.angistrem.employeemanagement.controller.SubTaskController;
import io.angistrem.employeemanagement.model.SubTask;
import io.angistrem.employeemanagement.model.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;

import java.io.IOException;
import java.util.List;

public class SubTaskDialog extends BaseDialog<SubTask> {
    private SubTaskController controller;
    private final SubTask subTask;
    private Task parentTask;
    private final List<Task> availableTasks;

    public SubTaskDialog() {
        this(null, null);
    }

    public SubTaskDialog(SubTask subTask) {
        this(subTask, null);
    }

    public SubTaskDialog(List<Task> availableTasks) {
        this(null, availableTasks);
    }

    public SubTaskDialog(SubTask subTask, List<Task> availableTasks) {
        super(subTask == null ? "\u041d\u043e\u0432\u0430\u044f \u043f\u043e\u0434\u0437\u0430\u0434\u0430\u0447\u0430" : "\u0420\u0435\u0434\u0430\u043a\u0442\u0438\u0440\u043e\u0432\u0430\u043d\u0438\u0435 \u043f\u043e\u0434\u0437\u0430\u0434\u0430\u0447\u0438");
        this.subTask = subTask;
        this.availableTasks = availableTasks;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/io/angistrem/employeemanagement/view/dialog/sub-task-dialog.fxml"));
            Parent parent = loader.load();
            controller = loader.getController();

            Scene scene = new Scene(parent);
            configureScene(scene);
            stage.setWidth(820);
            stage.setHeight(760);
        } catch (IOException e) {
            throw new RuntimeException("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043e\u0442\u043a\u0440\u044b\u0442\u044c \u0434\u0438\u0430\u043b\u043e\u0433 \u043f\u043e\u0434\u0437\u0430\u0434\u0430\u0447\u0438", e);
        }
    }

    public void setParentTask(Task parentTask) {
        this.parentTask = parentTask;
    }

    @Override
    public void show() {
        controller.setAvailableTasks(availableTasks);
        if (parentTask != null) {
            controller.setParentTask(parentTask);
        }
        if (subTask != null) {
            controller.setSubTask(subTask);
        }
        stage.showAndWait();
        result = controller.getSubTask();
    }

    @Override
    public SubTask getResult() {
        return result;
    }
}
