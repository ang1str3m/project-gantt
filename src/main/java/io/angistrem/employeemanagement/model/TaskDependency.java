package io.angistrem.employeemanagement.model;

public class TaskDependency {
    private String predecessorPosition;
    private String type = "FS";
    private int lagDays = 0;
    private boolean critical = false;

    public TaskDependency() {
    }

    public TaskDependency(String predecessorPosition, String type, int lagDays) {
        this.predecessorPosition = predecessorPosition;
        this.type = type != null ? type : "FS";
        this.lagDays = lagDays;
    }

    public TaskDependency(String predecessorPosition, String type, int lagDays, boolean critical) {
        this.predecessorPosition = predecessorPosition;
        this.type = type != null ? type : "FS";
        this.lagDays = lagDays;
        this.critical = critical;
    }

    public String getPredecessorPosition() {
        return predecessorPosition;
    }

    public void setPredecessorPosition(String predecessorPosition) {
        this.predecessorPosition = predecessorPosition;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type != null ? type : "FS";
    }

    public int getLagDays() {
        return lagDays;
    }

    public void setLagDays(int lagDays) {
        this.lagDays = lagDays;
    }

    public boolean isCritical() {
        return critical;
    }

    public void setCritical(boolean critical) {
        this.critical = critical;
    }
}
