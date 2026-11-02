package io.angistrem.employeemanagement.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class Task {
    private static final AtomicInteger TASK_COUNTER = new AtomicInteger(0);

    public static void resetCounter() {
        TASK_COUNTER.set(0);
    }

    public static void syncCounterFromTasks(List<Task> tasks) {
        int max = 0;
        if (tasks != null) {
            for (Task task : tasks) {
                if (task == null || task.getPosition() == null) {
                    continue;
                }
                String raw = extractTopLevelPosition(task.getPosition());
                try {
                    max = Math.max(max, Integer.parseInt(raw));
                } catch (NumberFormatException ignored) {
                    // Keep best known counter value when imported positions have custom text.
                }
            }
        }
        TASK_COUNTER.set(max);
    }

    @JsonIgnore
    private final StringProperty position = new SimpleStringProperty();
    @JsonIgnore
    private final StringProperty name = new SimpleStringProperty();
    @JsonIgnore
    private final StringProperty company = new SimpleStringProperty();
    @JsonIgnore
    private final ObjectProperty<LocalDate> start = new SimpleObjectProperty<>();
    @JsonIgnore
    private final ObjectProperty<LocalDate> end = new SimpleObjectProperty<>();
    @JsonIgnore
    private final StringProperty status = new SimpleStringProperty(TaskStatus.PLANNED);
    @JsonIgnore
    private final StringProperty priority = new SimpleStringProperty("Средний");
    @JsonIgnore
    private final DoubleProperty percent = new SimpleDoubleProperty(0);
    @JsonIgnore
    private final StringProperty taskType = new SimpleStringProperty("TASK");

    @JsonIgnore
    private final BooleanProperty criticalDependency = new SimpleBooleanProperty(false);
    @JsonIgnore
    private final BooleanProperty resourceOverloaded = new SimpleBooleanProperty(false);
    @JsonIgnore
    private final BooleanProperty budgetOverrun = new SimpleBooleanProperty(false);
    @JsonIgnore
    private final ObjectProperty<LocalDate> contractAdvanceDate = new SimpleObjectProperty<>();
    @JsonIgnore
    private final ObjectProperty<LocalDate> ksPaymentDate = new SimpleObjectProperty<>();
    @JsonIgnore
    private final BooleanProperty requireAdvanceBeforeStart = new SimpleBooleanProperty(false);
    @JsonIgnore
    private final BooleanProperty requireKsBeforeStart = new SimpleBooleanProperty(false);

    private List<SubTask> subTasks = new ArrayList<>();
    private List<Task> childTasks = new ArrayList<>();
    private List<TaskDependency> dependencies = new ArrayList<>();
    private List<TaskResourceAssignment> resourceAssignments = new ArrayList<>();
    private double plannedCost = 0.0;
    private double actualCost = 0.0;
    private String resourceLoadSummary = "";
    private String budgetSummary = "";

    @JsonIgnore
    private ObservableList<SubTask> observableSubTasks;
    @JsonIgnore
    private ObservableList<Task> observableChildTasks;
    @JsonIgnore
    private final Set<SubTask> observedSubTasks = Collections.newSetFromMap(new IdentityHashMap<>());
    @JsonIgnore
    private final Set<Task> observedChildTasks = Collections.newSetFromMap(new IdentityHashMap<>());

    public Task() {
        setPosition(TASK_COUNTER.incrementAndGet() + ".");
    }

    public Task(String name, String company, LocalDate start, LocalDate end, String status, String priority) {
        setPosition(TASK_COUNTER.incrementAndGet() + ".");
        setName(name);
        setCompany(company);
        setStart(start);
        setEnd(end);
        setStatus(status);
        setPriority(priority);
        updatePercentFromSubTasks();
    }

    public String getPosition() {
        return position.get();
    }

    public void setPosition(String position) {
        this.position.set(position);
    }

    public StringProperty positionProperty() {
        return position;
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
    }

    public ObjectProperty<LocalDate> startProperty() {
        return start;
    }

    public LocalDate getEnd() {
        return end.get();
    }

    public void setEnd(LocalDate end) {
        this.end.set(end);
    }

    public ObjectProperty<LocalDate> endProperty() {
        return end;
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

    public String getPriority() {
        return priority.get();
    }

    public void setPriority(String priority) {
        this.priority.set(priority != null ? priority : "Средний");
    }

    public StringProperty priorityProperty() {
        return priority;
    }

    public double getPercent() {
        return percent.get();
    }

    public DoubleProperty percentProperty() {
        return percent;
    }

    public String getTaskType() {
        return taskType.get();
    }

    public void setTaskType(String taskType) {
        String normalized = taskType == null ? "TASK" : taskType.trim().toUpperCase();
        if (!"MILESTONE".equals(normalized) && !"GROUP".equals(normalized)) {
            normalized = "TASK";
        }
        this.taskType.set(normalized);
        if (isGroup()) {
            setDependencies(List.of());
            updateAggregateFromChildren();
        }
    }

    public StringProperty taskTypeProperty() {
        return taskType;
    }

    @JsonIgnore
    public boolean isMilestone() {
        return "MILESTONE".equalsIgnoreCase(getTaskType());
    }

    @JsonIgnore
    public boolean isGroup() {
        return "GROUP".equalsIgnoreCase(getTaskType());
    }

    public boolean isCriticalDependency() {
        return criticalDependency.get();
    }

    public void setCriticalDependency(boolean value) {
        this.criticalDependency.set(value);
    }

    public BooleanProperty criticalDependencyProperty() {
        return criticalDependency;
    }

    public boolean isResourceOverloaded() {
        return resourceOverloaded.get();
    }

    public void setResourceOverloaded(boolean resourceOverloaded) {
        this.resourceOverloaded.set(resourceOverloaded);
    }

    public BooleanProperty resourceOverloadedProperty() {
        return resourceOverloaded;
    }

    public boolean isBudgetOverrun() {
        return budgetOverrun.get();
    }

    public void setBudgetOverrun(boolean budgetOverrun) {
        this.budgetOverrun.set(budgetOverrun);
    }

    public BooleanProperty budgetOverrunProperty() {
        return budgetOverrun;
    }

    public LocalDate getContractAdvanceDate() {
        return contractAdvanceDate.get();
    }

    public void setContractAdvanceDate(LocalDate value) {
        this.contractAdvanceDate.set(value);
    }

    public ObjectProperty<LocalDate> contractAdvanceDateProperty() {
        return contractAdvanceDate;
    }

    public LocalDate getKsPaymentDate() {
        return ksPaymentDate.get();
    }

    public void setKsPaymentDate(LocalDate value) {
        this.ksPaymentDate.set(value);
    }

    public ObjectProperty<LocalDate> ksPaymentDateProperty() {
        return ksPaymentDate;
    }

    public boolean isRequireAdvanceBeforeStart() {
        return requireAdvanceBeforeStart.get();
    }

    public void setRequireAdvanceBeforeStart(boolean value) {
        this.requireAdvanceBeforeStart.set(value);
    }

    public BooleanProperty requireAdvanceBeforeStartProperty() {
        return requireAdvanceBeforeStart;
    }

    public boolean isRequireKsBeforeStart() {
        return requireKsBeforeStart.get();
    }

    public void setRequireKsBeforeStart(boolean value) {
        this.requireKsBeforeStart.set(value);
    }

    public BooleanProperty requireKsBeforeStartProperty() {
        return requireKsBeforeStart;
    }

    public List<TaskDependency> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<TaskDependency> dependencies) {
        this.dependencies = dependencies != null ? new ArrayList<>(dependencies) : new ArrayList<>();
    }

    public List<TaskResourceAssignment> getResourceAssignments() {
        return resourceAssignments;
    }

    public void setResourceAssignments(List<TaskResourceAssignment> resourceAssignments) {
        this.resourceAssignments = resourceAssignments != null ? new ArrayList<>(resourceAssignments) : new ArrayList<>();
    }

    public double getPlannedCost() {
        return plannedCost;
    }

    public void setPlannedCost(double plannedCost) {
        this.plannedCost = Math.max(0.0, plannedCost);
    }

    public double getActualCost() {
        return actualCost;
    }

    public void setActualCost(double actualCost) {
        this.actualCost = Math.max(0.0, actualCost);
    }

    public String getResourceLoadSummary() {
        return resourceLoadSummary;
    }

    public void setResourceLoadSummary(String resourceLoadSummary) {
        this.resourceLoadSummary = resourceLoadSummary == null ? "" : resourceLoadSummary;
    }

    public String getBudgetSummary() {
        return budgetSummary;
    }

    public void setBudgetSummary(String budgetSummary) {
        this.budgetSummary = budgetSummary == null ? "" : budgetSummary;
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
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }

    public List<SubTask> getSubTasks() {
        return subTasks;
    }

    public void setSubTasks(List<SubTask> subTasks) {
        this.subTasks = subTasks != null ? new ArrayList<>(subTasks) : new ArrayList<>();
        this.observableSubTasks = null;
        this.observedSubTasks.clear();
        updatePercentFromSubTasks();
    }

    public List<Task> getChildTasks() {
        return childTasks;
    }

    public void setChildTasks(List<Task> childTasks) {
        this.childTasks = childTasks != null ? new ArrayList<>(childTasks) : new ArrayList<>();
        this.observableChildTasks = null;
        this.observedChildTasks.clear();
        recalculateChildTaskPositions();
        updateAggregateFromChildren();
    }

    @JsonIgnore
    public ObservableList<SubTask> getObservableSubTasks() {
        if (observableSubTasks == null) {
            observableSubTasks = FXCollections.observableArrayList(subTasks);

            for (SubTask subTask : observableSubTasks) {
                attachSubTask(subTask);
            }

            observableSubTasks.addListener((ListChangeListener<SubTask>) change -> {
                while (change.next()) {
                    if (change.wasRemoved()) {
                        subTasks.removeAll(change.getRemoved());
                    }
                    if (change.wasAdded()) {
                        for (SubTask added : change.getAddedSubList()) {
                            if (!subTasks.contains(added)) {
                                subTasks.add(added);
                            }
                            attachSubTask(added);
                        }
                    }
                }
                recalculateSubTaskPositions();
                updatePercentFromSubTasks();
            });
        }
        return observableSubTasks;
    }

    @JsonIgnore
    public ObservableList<Task> getObservableChildTasks() {
        if (observableChildTasks == null) {
            observableChildTasks = FXCollections.observableArrayList(childTasks);

            for (Task childTask : observableChildTasks) {
                attachChildTask(childTask);
            }

            observableChildTasks.addListener((ListChangeListener<Task>) change -> {
                while (change.next()) {
                    if (change.wasRemoved()) {
                        childTasks.removeAll(change.getRemoved());
                    }
                    if (change.wasAdded()) {
                        for (Task added : change.getAddedSubList()) {
                            if (!childTasks.contains(added)) {
                                childTasks.add(added);
                            }
                            attachChildTask(added);
                        }
                    }
                }
                recalculateChildTaskPositions();
                updateAggregateFromChildren();
            });
        }
        return observableChildTasks;
    }

    private void attachSubTask(SubTask subTask) {
        if (!observedSubTasks.add(subTask)) {
            return;
        }

        subTask.setParentTask(this);
        subTask.volumeProjectProperty().addListener((obs, oldVal, newVal) -> updatePercentFromSubTasks());
        subTask.volumeActualProperty().addListener((obs, oldVal, newVal) -> updatePercentFromSubTasks());
        subTask.startProperty().addListener((obs, oldVal, newVal) -> updatePercentFromSubTasks());
        subTask.endProperty().addListener((obs, oldVal, newVal) -> updatePercentFromSubTasks());
    }

    private void attachChildTask(Task childTask) {
        if (childTask == null || !observedChildTasks.add(childTask)) {
            return;
        }

        childTask.startProperty().addListener((obs, oldVal, newVal) -> updateAggregateFromChildren());
        childTask.endProperty().addListener((obs, oldVal, newVal) -> updateAggregateFromChildren());
        childTask.percentProperty().addListener((obs, oldVal, newVal) -> updateAggregateFromChildren());
        childTask.taskTypeProperty().addListener((obs, oldVal, newVal) -> updateAggregateFromChildren());
        childTask.getObservableSubTasks().addListener((ListChangeListener<SubTask>) change -> updateAggregateFromChildren());
        childTask.getObservableChildTasks().addListener((ListChangeListener<Task>) change -> updateAggregateFromChildren());
    }

    public void updatePercentFromSubTasks() {
        if (isGroup()) {
            updateAggregateFromChildren();
            return;
        }
        if (subTasks.isEmpty()) {
            percent.set(0);
            updateStatusFromPercent();
            return;
        }

        double totalPercent = subTasks.stream()
                .mapToDouble(SubTask::getPercent)
                .average()
                .orElse(0);

        double rounded = Math.round(totalPercent * 10) / 10.0;
        percent.set(rounded);
        updateStatusFromPercent();
    }

    public void updateAggregateFromChildren() {
        if (!isGroup()) {
            return;
        }

        List<Task> children = getObservableChildTasks().stream()
                .filter(Objects::nonNull)
                .toList();

        if (children.isEmpty()) {
            percent.set(0);
            setStart(null);
            setEnd(null);
            setStatus(TaskStatus.PLANNED);
            return;
        }

        LocalDate minStart = children.stream()
                .map(Task::getStart)
                .filter(Objects::nonNull)
                .min(LocalDate::compareTo)
                .orElse(null);
        LocalDate maxEnd = children.stream()
                .map(Task::getEnd)
                .filter(Objects::nonNull)
                .max(LocalDate::compareTo)
                .orElse(null);

        setStart(minStart);
        setEnd(maxEnd);

        double avgPercent = children.stream()
                .mapToDouble(Task::getPercent)
                .average()
                .orElse(0);
        percent.set(Math.round(avgPercent * 10.0) / 10.0);
        updateStatusFromPercent();
    }

    private void updateStatusFromPercent() {
        double currentPercent = percent.get();

        if (currentPercent >= 100.0) {
            setStatus(TaskStatus.COMPLETED);
        } else if (currentPercent > 0) {
            setStatus(TaskStatus.IN_PROGRESS);
        } else {
            setStatus(TaskStatus.PLANNED);
        }
    }

    public void addSubTask(SubTask subTask) {
        getObservableSubTasks().add(subTask);
    }

    public void removeSubTask(SubTask subTask) {
        getObservableSubTasks().remove(subTask);
    }

    public void addChildTask(Task childTask) {
        if (childTask == null) {
            return;
        }
        getObservableChildTasks().add(childTask);
    }

    public void removeChildTask(Task childTask) {
        getObservableChildTasks().remove(childTask);
    }

    public void recalculateSubTaskPositions() {
        for (int i = 0; i < getObservableSubTasks().size(); i++) {
            SubTask subTask = getObservableSubTasks().get(i);
            subTask.updatePositionAfterLoad(this, i);
        }
    }

    public void recalculateChildTaskPositions() {
        for (int i = 0; i < getObservableChildTasks().size(); i++) {
            Task childTask = getObservableChildTasks().get(i);
            childTask.setPosition(getPosition() + (i + 1) + ".");
            childTask.recalculateSubTaskPositions();
            childTask.recalculateChildTaskPositions();
            if (childTask.isGroup()) {
                childTask.updateAggregateFromChildren();
            } else {
                childTask.updatePercentFromSubTasks();
            }
        }
        if (isGroup()) {
            updateAggregateFromChildren();
        }
    }

    @JsonProperty("position")
    public String getJsonPosition() {
        return getPosition();
    }

    @JsonProperty("position")
    public void setJsonPosition(String position) {
        setPosition(position);
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

    @JsonProperty("status")
    public String getJsonStatus() {
        return getStatus();
    }

    @JsonProperty("status")
    public void setJsonStatus(String status) {
        setStatus(status);
    }

    @JsonProperty("priority")
    public String getJsonPriority() {
        return getPriority();
    }

    @JsonProperty("priority")
    public void setJsonPriority(String priority) {
        setPriority(priority);
    }

    @JsonProperty("percent")
    public Double getJsonPercent() {
        return getPercent();
    }

    @JsonProperty("percent")
    public void setJsonPercent(Double ignored) {
        // percent is recalculated automatically from subtasks
    }

    @JsonProperty("taskType")
    public String getJsonTaskType() {
        return getTaskType();
    }

    @JsonProperty("taskType")
    public void setJsonTaskType(String taskType) {
        setTaskType(taskType);
    }

    @JsonProperty("dependencies")
    public List<TaskDependency> getJsonDependencies() {
        return getDependencies();
    }

    @JsonProperty("dependencies")
    public void setJsonDependencies(List<TaskDependency> dependencies) {
        setDependencies(dependencies);
    }

    @JsonProperty("resourceAssignments")
    public List<TaskResourceAssignment> getJsonResourceAssignments() {
        return getResourceAssignments();
    }

    @JsonProperty("resourceAssignments")
    public void setJsonResourceAssignments(List<TaskResourceAssignment> resourceAssignments) {
        setResourceAssignments(resourceAssignments);
    }

    @JsonProperty("plannedCost")
    public double getJsonPlannedCost() {
        return getPlannedCost();
    }

    @JsonProperty("plannedCost")
    public void setJsonPlannedCost(double plannedCost) {
        setPlannedCost(plannedCost);
    }

    @JsonProperty("actualCost")
    public double getJsonActualCost() {
        return getActualCost();
    }

    @JsonProperty("actualCost")
    public void setJsonActualCost(double actualCost) {
        setActualCost(actualCost);
    }

    @JsonProperty("childTasks")
    public List<Task> getJsonChildTasks() {
        return getChildTasks();
    }

    @JsonProperty("childTasks")
    public void setJsonChildTasks(List<Task> childTasks) {
        setChildTasks(childTasks);
    }

    @JsonProperty("dependencyPositions")
    public List<String> getJsonDependencyPositions() {
        return getDependencyPositions();
    }

    @JsonProperty("dependencyPositions")
    public void setJsonDependencyPositions(List<String> dependencyPositions) {
        if (dependencies == null || dependencies.isEmpty()) {
            setDependencyPositions(dependencyPositions);
        }
    }

    @JsonProperty("criticalDependency")
    public Boolean getJsonCriticalDependency() {
        return isCriticalDependency();
    }

    @JsonProperty("criticalDependency")
    public void setJsonCriticalDependency(Boolean value) {
        setCriticalDependency(value != null && value);
    }

    @JsonProperty("contractAdvanceDate")
    public LocalDate getJsonContractAdvanceDate() {
        return getContractAdvanceDate();
    }

    @JsonProperty("contractAdvanceDate")
    public void setJsonContractAdvanceDate(LocalDate value) {
        setContractAdvanceDate(value);
    }

    @JsonProperty("ksPaymentDate")
    public LocalDate getJsonKsPaymentDate() {
        return getKsPaymentDate();
    }

    @JsonProperty("ksPaymentDate")
    public void setJsonKsPaymentDate(LocalDate value) {
        setKsPaymentDate(value);
    }

    @JsonProperty("requireAdvanceBeforeStart")
    public Boolean getJsonRequireAdvanceBeforeStart() {
        return isRequireAdvanceBeforeStart();
    }

    @JsonProperty("requireAdvanceBeforeStart")
    public void setJsonRequireAdvanceBeforeStart(Boolean value) {
        setRequireAdvanceBeforeStart(value != null && value);
    }

    @JsonProperty("requireKsBeforeStart")
    public Boolean getJsonRequireKsBeforeStart() {
        return isRequireKsBeforeStart();
    }

    @JsonProperty("requireKsBeforeStart")
    public void setJsonRequireKsBeforeStart(Boolean value) {
        setRequireKsBeforeStart(value != null && value);
    }

    private static String extractTopLevelPosition(String position) {
        if (position == null) {
            return "";
        }
        String normalized = position.replace(',', '.').trim();
        int dotIndex = normalized.indexOf('.');
        String firstPart = dotIndex >= 0 ? normalized.substring(0, dotIndex) : normalized;
        return firstPart.replaceAll("[^0-9]", "");
    }
}
