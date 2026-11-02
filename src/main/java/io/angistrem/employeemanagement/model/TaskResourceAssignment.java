package io.angistrem.employeemanagement.model;

public class TaskResourceAssignment {
    private String resourceId = "";
    private String resourceName = "";
    private String role = "";
    private double allocation = 1.0;
    private double plannedHours = 0.0;
    private double actualHours = 0.0;

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId == null ? "" : resourceId;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName == null ? "" : resourceName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role == null ? "" : role;
    }

    public double getAllocation() {
        return allocation;
    }

    public void setAllocation(double allocation) {
        this.allocation = Math.max(0.0, allocation);
    }

    public double getPlannedHours() {
        return plannedHours;
    }

    public void setPlannedHours(double plannedHours) {
        this.plannedHours = Math.max(0.0, plannedHours);
    }

    public double getActualHours() {
        return actualHours;
    }

    public void setActualHours(double actualHours) {
        this.actualHours = Math.max(0.0, actualHours);
    }
}
