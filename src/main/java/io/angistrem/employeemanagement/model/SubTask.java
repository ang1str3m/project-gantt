package io.angistrem.employeemanagement.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import javafx.beans.property.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SubTask {

    @JsonIgnore
    private final StringProperty subPosition = new SimpleStringProperty();
    @JsonIgnore
    private final StringProperty name = new SimpleStringProperty();
    @JsonIgnore
    private final StringProperty company = new SimpleStringProperty();
    @JsonIgnore
    private final ObjectProperty<LocalDate> start = new SimpleObjectProperty<>();
    @JsonIgnore
    private final ObjectProperty<LocalDate> end = new SimpleObjectProperty<>();
    @JsonIgnore
    private final IntegerProperty durationDays = new SimpleIntegerProperty();
    @JsonIgnore
    private final StringProperty unitOfMeasurement = new SimpleStringProperty();
    @JsonIgnore
    private final IntegerProperty volumeProject = new SimpleIntegerProperty(0);
    @JsonIgnore
    private final IntegerProperty volumeActual = new SimpleIntegerProperty(0);
    @JsonIgnore
    private final DoubleProperty percent = new SimpleDoubleProperty(0);
    @JsonIgnore
    private final StringProperty status = new SimpleStringProperty(TaskStatus.PLANNED);
    @JsonIgnore
    private Task parentTask;

    private List<TaskDependency> dependencies = new ArrayList<>();

    public SubTask() {
    }

    public SubTask(Task task, String name, String company,
                   LocalDate start, LocalDate end, String unitOfMeasurement,
                   Integer volumeProject, Integer volumeActual) {
        this.parentTask = task;
        setName(name);
        setCompany(company);
        setStart(start);
        setEnd(end);
        setUnitOfMeasurement(unitOfMeasurement);
        setVolumeProject(volumeProject != null ? volumeProject : 0);
        setVolumeActual(volumeActual != null ? volumeActual : 0);
        updatePercentVolume();
        updateStatus();
    }

    public void updatePositionAfterLoad(Task parentTask, int index) {
        this.parentTask = parentTask;
        String position = parentTask.getPosition() + (index + 1) + ".";
        setSubPosition(position);
        updateDurationDays();
        updatePercentVolume();
        updateStatus();
    }

    public String getSubPosition() {
        return subPosition.get();
    }

    public void setSubPosition(String subPosition) {
        this.subPosition.set(subPosition);
    }

    public StringProperty subPositionProperty() {
        return subPosition;
    }

    public String getName() {
        return name.get();
    }

    public void setName(String name) {
        this.name.set(name != null ? name : "");
    }

    public StringProperty nameProperty() {
        return name;
    }

    public String getCompany() {
        return company.get();
    }

    public void setCompany(String company) {
        this.company.set(company != null ? company : "");
    }

    public StringProperty companyProperty() {
        return company;
    }

    public LocalDate getStart() {
        return start.get();
    }

    public void setStart(LocalDate start) {
        this.start.set(start);
        updateDurationDays();
        updateStatus();
    }

    public ObjectProperty<LocalDate> startProperty() {
        return start;
    }

    public LocalDate getEnd() {
        return end.get();
    }

    public void setEnd(LocalDate end) {
        this.end.set(end);
        updateDurationDays();
        updateStatus();
    }

    public ObjectProperty<LocalDate> endProperty() {
        return end;
    }

    public int getDurationDays() {
        return durationDays.get();
    }

    public IntegerProperty durationDaysProperty() {
        return durationDays;
    }

    private void updateDurationDays() {
        if (getStart() != null && getEnd() != null) {
            long days = ChronoUnit.DAYS.between(getStart(), getEnd()) + 1;
            durationDays.set((int) Math.max(days, 0));
        } else {
            durationDays.set(0);
        }
    }

    public String getUnitOfMeasurement() {
        return unitOfMeasurement.get();
    }

    public void setUnitOfMeasurement(String unitOfMeasurement) {
        this.unitOfMeasurement.set(unitOfMeasurement != null ? unitOfMeasurement : "");
    }

    public StringProperty unitOfMeasurementProperty() {
        return unitOfMeasurement;
    }

    public String getStatus() {
        return status.get();
    }

    public void setStatus(String status) {
        this.status.set(status != null ? status : TaskStatus.PLANNED);
    }

    public StringProperty statusProperty() {
        return status;
    }

    public void updateStatus() {
        String newStatus;
        double currentPercent = getPercent();
        LocalDate now = LocalDate.now();

        if (currentPercent >= 100.0) {
            newStatus = TaskStatus.COMPLETED;
        } else if (getEnd() != null && now.isAfter(getEnd())) {
            newStatus = TaskStatus.OVERDUE;
        } else if (getStart() != null && now.isBefore(getStart())) {
            newStatus = TaskStatus.PLANNED;
        } else {
            newStatus = TaskStatus.IN_PROGRESS;
        }
        status.set(newStatus);
    }

    public int getVolumeProject() {
        return volumeProject.get();
    }

    public void setVolumeProject(Integer volumeProject) {
        this.volumeProject.set(volumeProject != null ? volumeProject : 0);
        updatePercentVolume();
        updateStatus();
    }

    public IntegerProperty volumeProjectProperty() {
        return volumeProject;
    }

    public int getVolumeActual() {
        return volumeActual.get();
    }

    public void setVolumeActual(Integer volumeActual) {
        this.volumeActual.set(volumeActual != null ? volumeActual : 0);
        updatePercentVolume();
        updateStatus();
    }

    public IntegerProperty volumeActualProperty() {
        return volumeActual;
    }

    public double getPercent() {
        return percent.get();
    }

    public DoubleProperty percentProperty() {
        return percent;
    }

    public void updatePercentVolume() {
        double percentValue = 0.0;
        int proj = getVolumeProject();
        if (proj > 0) {
            percentValue = (getVolumeActual() * 100.0) / proj;
            if (percentValue > 100) percentValue = 100;
            if (percentValue < 0) percentValue = 0;
        }
        percent.set(percentValue);
    }

    public List<TaskDependency> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<TaskDependency> dependencies) {
        this.dependencies = dependencies != null ? new ArrayList<>(dependencies) : new ArrayList<>();
    }

    public List<String> getDependencyPositions() {
        return dependencies.stream()
                .map(TaskDependency::getPredecessorPosition)
                .toList();
    }

    public void setDependencyPositions(List<String> dependencyPositions) {
        if (dependencyPositions == null) {
            this.dependencies = new ArrayList<>();
            return;
        }
        this.dependencies = dependencyPositions.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(s -> new TaskDependency(s.trim(), "FS", 0))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @JsonIgnore
    public Task getParentTask() {
        return parentTask;
    }

    public void setParentTask(Task parentTask) {
        this.parentTask = parentTask;
    }

    @JsonProperty("subPosition")
    public String getJsonSubPosition() {
        return getSubPosition();
    }

    @JsonProperty("subPosition")
    public void setJsonSubPosition(String subPosition) {
        setSubPosition(subPosition);
    }

    @JsonProperty("name")
    public String getJsonName() {
        return getName();
    }

    @JsonProperty("name")
    public void setJsonName(String name) {
        setName(name);
    }

    @JsonProperty("company")
    public String getJsonCompany() {
        return getCompany();
    }

    @JsonProperty("company")
    public void setJsonCompany(String company) {
        setCompany(company);
    }

    @JsonProperty("start")
    public LocalDate getJsonStart() {
        return getStart();
    }

    @JsonProperty("start")
    public void setJsonStart(LocalDate start) {
        setStart(start);
    }

    @JsonProperty("end")
    public LocalDate getJsonEnd() {
        return getEnd();
    }

    @JsonProperty("end")
    public void setJsonEnd(LocalDate end) {
        setEnd(end);
    }

    @JsonProperty("unitOfMeasurement")
    public String getJsonUnitOfMeasurement() {
        return getUnitOfMeasurement();
    }

    @JsonProperty("unitOfMeasurement")
    public void setJsonUnitOfMeasurement(String unitOfMeasurement) {
        setUnitOfMeasurement(unitOfMeasurement);
    }

    @JsonProperty("volumeProject")
    public int getJsonVolumeProject() {
        return getVolumeProject();
    }

    @JsonProperty("volumeProject")
    public void setJsonVolumeProject(int volumeProject) {
        setVolumeProject(volumeProject);
    }

    @JsonProperty("volumeActual")
    public int getJsonVolumeActual() {
        return getVolumeActual();
    }

    @JsonProperty("volumeActual")
    public void setJsonVolumeActual(int volumeActual) {
        setVolumeActual(volumeActual);
    }

    @JsonProperty("percent")
    public double getJsonPercent() {
        return getPercent();
    }

    @JsonProperty("percent")
    public void setJsonPercent(double ignored) {
        updatePercentVolume();
    }

    @JsonProperty("status")
    public String getJsonStatus() {
        return getStatus();
    }

    @JsonProperty("status")
    public void setJsonStatus(String status) {
        setStatus(status);
    }

    @JsonProperty("dependencies")
    public List<TaskDependency> getJsonDependencies() {
        return getDependencies();
    }

    @JsonProperty("dependencies")
    public void setJsonDependencies(List<TaskDependency> dependencies) {
        setDependencies(dependencies);
    }
}
