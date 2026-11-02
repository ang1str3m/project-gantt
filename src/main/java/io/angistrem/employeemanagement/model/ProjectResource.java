package io.angistrem.employeemanagement.model;

import java.util.UUID;

public class ProjectResource {
    private String id = UUID.randomUUID().toString();
    private String name = "";
    private String type = "PERSON";
    private String company = "";
    private String role = "";
    private double capacityPerDay = 1.0;
    private double costPerDay = 0.0;
    private boolean active = true;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id == null || id.isBlank() ? UUID.randomUUID().toString() : id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name == null ? "" : name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type == null || type.isBlank() ? "PERSON" : type;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company == null ? "" : company;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role == null ? "" : role;
    }

    public double getCapacityPerDay() {
        return capacityPerDay;
    }

    public void setCapacityPerDay(double capacityPerDay) {
        this.capacityPerDay = Math.max(0.0, capacityPerDay);
    }

    public double getCostPerDay() {
        return costPerDay;
    }

    public void setCostPerDay(double costPerDay) {
        this.costPerDay = Math.max(0.0, costPerDay);
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
