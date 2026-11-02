package io.angistrem.employeemanagement.model;

import java.time.LocalDate;

public class FinanceTask {
    private String name;
    private LocalDate start;
    private String status;
    private String priority;

    public FinanceTask(String name, LocalDate start, String status, String priority) {
        this.name = name;
        this.start = start;
        this.status = status;
        this.priority = priority;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDate getStart() {
        return start;
    }

    public void setStart(LocalDate start) {
        this.start = start;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }
}
