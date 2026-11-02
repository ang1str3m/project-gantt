package io.angistrem.employeemanagement.dialog;

import io.angistrem.employeemanagement.controller.BudgetController;
import io.angistrem.employeemanagement.model.BudgetItem;
import io.angistrem.employeemanagement.model.Task;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;

import java.io.IOException;
import java.util.List;

public class BudgetDialog extends BaseDialog<Void> {
    private final ObservableList<BudgetItem> budgetItems;
    private final List<Task> tasks;
    private BudgetController controller;

    public BudgetDialog(ObservableList<BudgetItem> budgetItems, List<Task> tasks) {
        super("Бюджет проекта");
        this.budgetItems = budgetItems;
        this.tasks = tasks;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/io/angistrem/employeemanagement/view/dialog/budget-dialog.fxml"));
            Parent parent = loader.load();
            controller = loader.getController();
            Scene scene = new Scene(parent);
            configureScene(scene);
            stage.setWidth(1180);
            stage.setHeight(760);
            stage.setMinWidth(1100);
            stage.setMinHeight(720);
        } catch (IOException e) {
            throw new RuntimeException("Не удалось открыть бюджет проекта", e);
        }
    }

    @Override
    public void show() {
        controller.setContext(budgetItems, tasks);
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
