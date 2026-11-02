package io.angistrem.employeemanagement.dialog;

import io.angistrem.employeemanagement.controller.TaskResourcesController;
import io.angistrem.employeemanagement.model.ProjectResource;
import io.angistrem.employeemanagement.model.Task;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;

import java.io.IOException;

public class TaskResourcesDialog extends BaseDialog<Void> {
    private final Task task;
    private final ObservableList<ProjectResource> resources;
    private TaskResourcesController controller;

    public TaskResourcesDialog(Task task, ObservableList<ProjectResource> resources) {
        super("Ресурсы задачи");
        this.task = task;
        this.resources = resources;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/io/angistrem/employeemanagement/view/dialog/task-resources-dialog.fxml"));
            Parent parent = loader.load();
            controller = loader.getController();
            Scene scene = new Scene(parent);
            configureScene(scene);
            stage.setWidth(900);
            stage.setHeight(620);
        } catch (IOException e) {
            throw new RuntimeException("Не удалось открыть ресурсы задачи", e);
        }
    }

    @Override
    public void show() {
        controller.setContext(task, resources);
        stage.showAndWait();
    }

    public boolean isChanged() {
        return controller != null && controller.isChanged();
    }

    @Override
    public Void getResult() {
        return null;
    }
}
