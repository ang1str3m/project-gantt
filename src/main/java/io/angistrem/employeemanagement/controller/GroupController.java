package io.angistrem.employeemanagement.controller;

import io.angistrem.employeemanagement.model.Task;
import io.angistrem.employeemanagement.util.ValidationUtils;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class GroupController {
    @FXML private TextField groupNameField;

    private Task group;
    private boolean editMode;

    @FXML
    public void initialize() {
        // No-op
    }

    public void setGroup(Task group) {
        this.group = group;
        this.editMode = true;
        if (groupNameField != null && group != null) {
            groupNameField.setText(group.getName());
        }
    }

    @FXML
    private void handleSave() {
        if (!ValidationUtils.validateRequiredTextField(groupNameField, "Название группы")) {
            return;
        }

        String groupName = groupNameField.getText().trim();
        if (!editMode || group == null) {
            group = new Task();
            group.setTaskType("GROUP");
            group.setPriority("Средний");
        }

        group.setName(groupName);
        group.setCompany("");
        group.updateAggregateFromChildren();
        close();
    }

    @FXML
    private void handleCancel() {
        if (!editMode) {
            group = null;
        }
        close();
    }

    public Task getGroup() {
        return group;
    }

    private void close() {
        ((Stage) groupNameField.getScene().getWindow()).close();
    }
}
