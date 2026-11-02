package io.angistrem.employeemanagement.dialog;

import io.angistrem.employeemanagement.controller.ResourceDirectoryController;
import io.angistrem.employeemanagement.model.ProjectResource;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;

import java.io.IOException;

public class ResourceDirectoryDialog extends BaseDialog<Void> {
    private ResourceDirectoryController controller;
    private final ObservableList<ProjectResource> resources;

    public ResourceDirectoryDialog(ObservableList<ProjectResource> resources) {
        super("Справочник ресурсов");
        this.resources = resources;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/io/angistrem/employeemanagement/view/dialog/resource-directory-dialog.fxml"));
            Parent parent = loader.load();
            controller = loader.getController();
            Scene scene = new Scene(parent);
            configureScene(scene);
            stage.setWidth(980);
            stage.setHeight(660);
        } catch (IOException e) {
            throw new RuntimeException("Не удалось открыть справочник ресурсов", e);
        }
    }

    @Override
    public void show() {
        controller.setResources(resources);
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
