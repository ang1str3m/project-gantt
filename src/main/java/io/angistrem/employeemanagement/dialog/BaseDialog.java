package io.angistrem.employeemanagement.dialog;

import io.angistrem.employeemanagement.util.SceneManager;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

public abstract class BaseDialog<T> {
    protected final Stage stage;
    protected T result;

    public BaseDialog(String title) {
        stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.DECORATED);
        stage.setTitle(title);
        stage.setResizable(false);
        stage.setMinWidth(640);
        stage.setMinHeight(420);
    }

    public void setOwner(Window owner) {
        if (owner != null) {
            stage.initOwner(owner);
        }
    }

    public Stage getStage() {
        return stage;
    }

    protected void configureScene(Scene scene) {
        if (scene == null) {
            return;
        }
        String dialogStylesheet = getClass().getResource("/io/angistrem/employeemanagement/styles/dialog.css").toExternalForm();
        if (!scene.getStylesheets().contains(dialogStylesheet)) {
            scene.getStylesheets().add(dialogStylesheet);
        }
        SceneManager.applyTheme(scene);
        stage.setScene(scene);
        stage.centerOnScreen();
    }

    public abstract void show();

    public abstract T getResult();
}
