
/*
 * Copyright (c) 2026.
 */

package io.angistrem.employeemanagement.controller;

import io.angistrem.employeemanagement.controller.menu.MainMenuBar;
import io.angistrem.employeemanagement.controller.menu.TaskContextMenu;
import io.angistrem.employeemanagement.dialog.ResourceDirectoryDialog;
import io.angistrem.employeemanagement.dialog.BudgetDialog;
import io.angistrem.employeemanagement.model.BudgetItem;
import io.angistrem.employeemanagement.model.ProjectDocument;
import io.angistrem.employeemanagement.model.ProjectResource;
import io.angistrem.employeemanagement.model.Settings;
import io.angistrem.employeemanagement.model.SubTask;
import io.angistrem.employeemanagement.model.Task;
import io.angistrem.employeemanagement.model.TaskDependency;
import io.angistrem.employeemanagement.model.TaskStatus;
import io.angistrem.employeemanagement.service.AutosaveService;
import io.angistrem.employeemanagement.service.DashboardService;
import io.angistrem.employeemanagement.service.DependencyEditingService;
import io.angistrem.employeemanagement.service.DropExecutionService;
import io.angistrem.employeemanagement.service.MainViewLayoutService;
import io.angistrem.employeemanagement.service.MainControllerUiSyncService;
import io.angistrem.employeemanagement.service.ProjectService;
import io.angistrem.employeemanagement.service.TaskExpansionService;
import io.angistrem.employeemanagement.service.TaskFilterService;
import io.angistrem.employeemanagement.service.TaskHealthService;
import io.angistrem.employeemanagement.service.TaskInteractionService;
import io.angistrem.employeemanagement.service.TaskService;
import io.angistrem.employeemanagement.service.WindowRecoveryService;
import io.angistrem.employeemanagement.dialog.TaskResourcesDialog;
import io.angistrem.employeemanagement.util.AppSettings;
import io.angistrem.employeemanagement.util.CriticalPathAnalyzer;
import io.angistrem.employeemanagement.util.CriticalPathDialog;
import io.angistrem.employeemanagement.util.DependencyDialog;
import io.angistrem.employeemanagement.util.HotkeyManager;
import io.angistrem.employeemanagement.util.ProjectAnalysisAnalyzer;
import io.angistrem.employeemanagement.util.ProjectAnalysisDialog;
import io.angistrem.employeemanagement.util.ProjectStageOverviewDialog;
import io.angistrem.employeemanagement.util.SceneManager;
import io.angistrem.employeemanagement.util.ResourceLoadAnalyzer;
import io.angistrem.employeemanagement.util.ResourceLoadDialog;
import io.angistrem.employeemanagement.util.PaymentCalendarDialog;
import io.angistrem.employeemanagement.util.TaskDependencyScheduler;
import io.angistrem.employeemanagement.view.GanttChart;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.*;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.geometry.Rectangle2D;
import javafx.util.Duration;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class MainController {
    private static final DateTimeFormatter TABLE_DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private boolean sceneSizeBindingApplied = false;

    @FXML private TreeTableView<Object> treeTableView;
    @FXML private TreeTableColumn<Object, String> positionColumn;
    @FXML private TreeTableColumn<Object, String> taskNameColumn;
    @FXML private TreeTableColumn<Object, String> companyColumn;
    @FXML private TreeTableColumn<Object, LocalDate> startColumn;
    @FXML private TreeTableColumn<Object, LocalDate> endColumn;
    @FXML private TreeTableColumn<Object, String> unitOfMeasurementColumn;
    @FXML private TreeTableColumn<Object, Integer> volProjectColumn;
    @FXML private TreeTableColumn<Object, Integer> volActualColumn;
    @FXML private TreeTableColumn<Object, Double> percentColumn;
    @FXML private TreeTableColumn<Object, String> statusColumn;
    @FXML private TreeTableColumn<Object, String> priorityColumn;

    @FXML private ScrollPane ganttHeaderScroll;
    @FXML private ScrollPane ganttDiagram;
    @FXML private Region treeBottomSpacer;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilterBox;
    @FXML private HBox zoomBarContainer;
    @FXML private HBox filterBar;
    @FXML private HBox dashboardBar;
    @FXML private VBox topBarContainer;
    @FXML private VBox workspaceRoot;
    @FXML private SplitPane mainSplitPane;
    @FXML private CheckBox autoScheduleCheckBox;
    @FXML private Label totalTasksLabel;
    @FXML private Label totalSubTasksLabel;
    @FXML private Label completedLabel;
    @FXML private Label overdueLabel;
    @FXML private ProgressBar overallProgressBar;
    @FXML private Label overallProgressLabel;
    @FXML private Label riskSummaryLabel;
    @FXML private MainMenuBar mainMenuBarController;
    @FXML private ZoomController zoomBarController;

    private GanttChart ganttChart;
    private final ObservableList<Task> tasks = FXCollections.observableArrayList();
    private final ObservableList<ProjectResource> projectResources = FXCollections.observableArrayList();
    private final ObservableList<BudgetItem> budgetItems = FXCollections.observableArrayList();
    private ContextMenu currentContextMenu;
    private boolean scrollSyncInProgress = false;
    private boolean ganttHorizontalSyncInProgress = false;
    private ScrollBar treeVerticalScrollBar;
    private final Map<Task, ListChangeListener<SubTask>> subTaskListeners = new HashMap<>();
    private String searchQuery = "";
    private String statusFilter = "ALL";
    private boolean scheduleRecalcInProgress = false;
    private String projectName = "\u041d\u043e\u0432\u044b\u0439 \u043f\u0440\u043e\u0435\u043a\u0442";
    private String projectAuthor = "";
    private final ProjectService projectService = new ProjectService();
    private final AutosaveService autosaveService = new AutosaveService();
    private final DashboardService dashboardService = new DashboardService();
    private final DependencyEditingService dependencyEditingService = new DependencyEditingService();
    private final DropExecutionService dropExecutionService = new DropExecutionService();
    private final MainControllerUiSyncService mainControllerUiSyncService = new MainControllerUiSyncService();
    private final MainViewLayoutService mainViewLayoutService = new MainViewLayoutService();
    private final TaskExpansionService taskExpansionService = new TaskExpansionService();
    private final WindowRecoveryService windowRecoveryService = new WindowRecoveryService();
    private final TaskService taskService = new TaskService();
    private final TaskFilterService taskFilterService = new TaskFilterService();
    private final TaskHealthService taskHealthService = new TaskHealthService();
    private final TaskInteractionService taskInteractionService = new TaskInteractionService();
    private boolean showDependencies = true;
    private final Deque<DeletedItem> deletedItems = new ArrayDeque<>();
    private boolean criticalPathPromptInProgress = false;
    private CriticalPathAnalyzer.Analysis highlightedCriticalPath;
    private Task draggedTask;
    private SubTask draggedSubTask;

    private static List<Task> loadedTasks;
    private static ProjectDocument loadedProjectDocument;

    private record DeletedItem(Task parentTask, Task task, SubTask subTask, int index) {
        static DeletedItem task(Task task, int index) {
            return new DeletedItem(null, task, null, index);
        }

        static DeletedItem subTask(Task parentTask, SubTask subTask, int index) {
            return new DeletedItem(parentTask, null, subTask, index);
        }

        boolean isTask() {
            return task != null;
        }
    }

    public static void setLoadedTasks(List<Task> tasks) {
        loadedTasks = tasks;
        loadedProjectDocument = null;
    }

    public static void setLoadedProjectDocument(ProjectDocument document) {
        loadedProjectDocument = document;
        loadedTasks = null;
    }

    @FXML
    public void initialize() {
        treeTableView.setFixedCellSize(resolveTableRowHeight());
        setupColumns();

        positionColumn.setComparator((a, b) -> {
            try {
                return Integer.compare(TaskTreeViewSupport.parsePosition(a), TaskTreeViewSupport.parsePosition(b));
            } catch (Exception e) {
                return a.compareToIgnoreCase(b);
            }
        });

        TreeItem<Object> root = new TreeItem<>(null);
        root.setExpanded(true);
        treeTableView.setRoot(root);
        treeTableView.setShowRoot(false);

        if (loadedProjectDocument != null) {
            tasks.setAll(loadedProjectDocument.getTasks() == null ? List.of() : loadedProjectDocument.getTasks());
            projectResources.setAll(loadedProjectDocument.getResources() == null ? List.of() : loadedProjectDocument.getResources());
            budgetItems.setAll(loadedProjectDocument.getBudgetItems() == null ? List.of() : loadedProjectDocument.getBudgetItems());
            Task.syncCounterFromTasks(tasks);
            projectName = projectService.normalizeProjectName(loadedProjectDocument.getProjectName());
            projectAuthor = projectService.normalizeAuthor(loadedProjectDocument.getAuthor());
            syncProjectMetadataToSessionSettings();
            loadedProjectDocument = null;
        } else if (loadedTasks != null) {
            tasks.setAll(loadedTasks);
            Task.syncCounterFromTasks(tasks);
            loadedTasks = null;
        } else {
            Task.syncCounterFromTasks(tasks);
        }

        tasks.addListener((ListChangeListener<Task>) c -> {
            if (scheduleRecalcInProgress) {
                return;
            }
            while (c.next()) {
                if (c.wasAdded() || c.wasRemoved() || c.wasUpdated()) {
                    if (mainMenuBarController != null) {
                        mainMenuBarController.markAsUnsaved();
                    }
                }
            }
            clearCriticalPathState();
            if (isAutoScheduleEnabled()) {
                applyScheduleConstraints();
            }
            autoSaveProjectDocument();
            recalculateResourceLoadFlags();
            rebuildTree();
            updateVisibleItemsForGantt();
            updateDashboard();
        });

        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldValue, newValue) -> {
                searchQuery = newValue == null ? "" : newValue.trim().toLowerCase();
                rebuildTree();
                updateVisibleItemsForGantt();
                updateDashboard();
            });
        }

        if (statusFilterBox != null) {
            statusFilterBox.getItems().setAll(
                    "ALL",
                    TaskStatus.PLANNED,
                    TaskStatus.IN_PROGRESS,
                    TaskStatus.COMPLETED,
                    TaskStatus.OVERDUE
            );
            statusFilterBox.setValue("ALL");
            statusFilterBox.valueProperty().addListener((obs, oldValue, newValue) -> {
                statusFilter = newValue == null ? "ALL" : newValue;
                rebuildTree();
                updateVisibleItemsForGantt();
                updateDashboard();
            });
        }

        ganttChart = new GanttChart();
        ganttChart.setTasks(tasks);
        ganttChart.setShowDependencies(showDependencies);

        treeTableView.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                applySceneSizeBindings(newScene);
            }
        });
        Platform.runLater(() -> {
            if (treeTableView.getScene() != null) {
                applySceneSizeBindings(treeTableView.getScene());
            }
        });
        ganttChart.setDependencyEditHandler(this::editDependencyFromGantt);
        ganttChart.setDependencyCreateHandler(this::createDependencyFromGantt);
        ganttChart.setItemSelectionHandler(this::selectObjectInTree);
        ganttChart.setItemDateChangeHandler(this::applyDatesFromGantt);
        ganttChart.setFinancialDateChangeHandler(this::applyFinancialDateFromGantt);
        ganttHeaderScroll.setContent(ganttChart.getHeaderView());
        ganttDiagram.setContent(ganttChart.getChartView());
        ganttChart.setParentScrollPane(ganttDiagram);
        ganttHeaderScroll.setFitToHeight(true);
        ganttHeaderScroll.setFitToWidth(false);
        ganttHeaderScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        ganttHeaderScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        ganttHeaderScroll.setPannable(false);
        ganttDiagram.setFitToHeight(false);
        ganttDiagram.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        ganttDiagram.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        bindGanttHorizontalScrolls();

        if (zoomBarController != null) {
            zoomBarController.init(ganttChart, ganttDiagram);
            zoomBarController.setShowCriticalPathAction(this::showCriticalPathFromToolbar);
            zoomBarController.setShowProjectAnalysisAction(this::showProjectAnalysisFromToolbar);
        }

        if (mainMenuBarController != null) {
            mainMenuBarController.setMainController(this);
            mainMenuBarController.markAsSaved();
        }

        installHotkeysWhenSceneReady();

        if (isAutoScheduleEnabled()) {
            applyScheduleConstraints();
        }
        recalculateResourceLoadFlags();
        rebuildTree();
        updateVisibleItemsForGantt();
        updateDashboard();

        setupBidirectionalScrollSync();
        setupTreeBottomSpacerSync();

        treeTableView.setOnContextMenuRequested(this::showTreeContextMenu);
        treeTableView.setSortPolicy(tv -> {
            boolean result = TreeTableView.DEFAULT_SORT_POLICY.call(tv);
            Platform.runLater(() -> {
                updateVisibleItemsForGantt();
                updateDashboard();
                if (ganttChart != null) {
                    ganttChart.refresh();
                }
            });
            return result;
        });

        setupSelectionHandling();
        setupDashboardInteractions();
    }

    private void setupColumns() {
        positionColumn.setCellValueFactory(cellData -> {
            Object value = cellData.getValue().getValue();
            if (value instanceof Task task) return task.positionProperty();
            if (value instanceof SubTask subTask) return subTask.subPositionProperty();
            return new SimpleStringProperty("");
        });

        taskNameColumn.setCellValueFactory(cellData -> {
            Object value = cellData.getValue().getValue();
            if (value instanceof Task task) return task.nameProperty();
            if (value instanceof SubTask subTask) return subTask.nameProperty();
            return new SimpleStringProperty("");
        });

        companyColumn.setCellValueFactory(cellData -> {
            Object value = cellData.getValue().getValue();
            if (value instanceof Task task) return task.companyProperty();
            if (value instanceof SubTask subTask) return subTask.companyProperty();
            return new SimpleStringProperty("");
        });

        startColumn.setCellValueFactory(cellData -> {
            Object value = cellData.getValue().getValue();
            if (value instanceof Task task) return task.startProperty();
            if (value instanceof SubTask subTask) return subTask.startProperty();
            return null;
        });
        startColumn.setCellFactory(column -> new TreeTableCell<>() {
            @Override
            protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : TABLE_DATE_FORMAT.format(item));
            }
        });

        endColumn.setCellValueFactory(cellData -> {
            Object value = cellData.getValue().getValue();
            if (value instanceof Task task) return task.endProperty();
            if (value instanceof SubTask subTask) return subTask.endProperty();
            return null;
        });
        endColumn.setCellFactory(column -> new TreeTableCell<>() {
            @Override
            protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : TABLE_DATE_FORMAT.format(item));
            }
        });

        unitOfMeasurementColumn.setCellValueFactory(cellData -> {
            Object value = cellData.getValue().getValue();
            if (value instanceof SubTask subTask) return subTask.unitOfMeasurementProperty();
            return new SimpleStringProperty("");
        });

        volProjectColumn.setCellValueFactory(cellData -> {
            Object value = cellData.getValue().getValue();
            if (value instanceof SubTask subTask) return subTask.volumeProjectProperty().asObject();
            return null;
        });

        volActualColumn.setCellValueFactory(cellData -> {
            Object value = cellData.getValue().getValue();
            if (value instanceof SubTask subTask) return subTask.volumeActualProperty().asObject();
            return null;
        });

        percentColumn.setCellValueFactory(cellData -> {
            Object value = cellData.getValue().getValue();
            if (value instanceof Task task) return task.percentProperty().asObject();
            if (value instanceof SubTask subTask) return subTask.percentProperty().asObject();
            return null;
        });

        statusColumn.setCellValueFactory(cellData -> {
            Object value = cellData.getValue().getValue();
            if (value instanceof Task task) return task.statusProperty();
            if (value instanceof SubTask subTask) return subTask.statusProperty();
            return new SimpleStringProperty("");
        });

        priorityColumn.setCellValueFactory(cellData -> {
            Object value = cellData.getValue().getValue();
            if (value instanceof Task task) return task.priorityProperty();
            return new SimpleStringProperty("");
        });
    }

    private void rebuildTree() {
        TreeItem<Object> root = treeTableView.getRoot();
        Set<Object> expansionState = captureExpansionState(root);
        detachRemovedTaskListeners();
        root.getChildren().clear();

        for (Task task : tasks) {
            TreeItem<Object> taskItem = buildTaskTreeItem(task, expansionState);
            if (taskItem != null) {
                root.getChildren().add(taskItem);
            }
        }

        updateGanttHeight();
    }

    private Set<Object> captureExpansionState(TreeItem<Object> root) {
        if (root == null) {
            return Set.of();
        }
        return TaskTreeViewSupport.collectVisibleItems(treeTableView).visible();
    }

    private TreeItem<Object> buildTaskTreeItem(Task task, Set<Object> expansionState) {
        if (task == null || !taskMatchesFilters(task)) {
            return null;
        }

        TreeItem<Object> taskItem = new TreeItem<>(task);
        boolean hasSavedState = expansionState != null && !expansionState.isEmpty();
        taskItem.setExpanded(!hasSavedState || expansionState.contains(task + "_expanded"));
        taskItem.expandedProperty().addListener((obs, oldVal, newVal) -> {
            updateVisibleItemsForGantt();
            updateGanttHeight();
        });

        attachTaskListeners(task);

        if (task.isGroup()) {
            for (Task childTask : getChildTasksForTree(task)) {
                TreeItem<Object> childItem = buildTaskTreeItem(childTask, expansionState);
                if (childItem != null) {
                    taskItem.getChildren().add(childItem);
                }
            }
        } else {
            for (SubTask subTask : getSubTasksForTree(task)) {
                taskItem.getChildren().add(new TreeItem<>(subTask));
            }
        }

        return taskItem;
    }

    private void attachTaskListeners(Task task) {
        if (subTaskListeners.containsKey(task)) {
            return;
        }

        ListChangeListener<SubTask> listener = c -> {
            if (scheduleRecalcInProgress) {
                return;
            }
            if (isAutoScheduleEnabled()) {
                applyScheduleConstraints();
            }
            autoSaveProjectDocument();
            rebuildTree();
            updateVisibleItemsForGantt();
            updateGanttHeight();
            updateDashboard();
            if (mainMenuBarController != null) {
                mainMenuBarController.markAsUnsaved();
            }
        };

        task.getObservableSubTasks().addListener(listener);
        task.getObservableChildTasks().addListener((ListChangeListener<Task>) c -> {
            if (scheduleRecalcInProgress) {
                return;
            }
            if (isAutoScheduleEnabled()) {
                applyScheduleConstraints();
            }
            autoSaveProjectDocument();
            rebuildTree();
            updateVisibleItemsForGantt();
            updateGanttHeight();
            updateDashboard();
            if (mainMenuBarController != null) {
                mainMenuBarController.markAsUnsaved();
            }
        });
        subTaskListeners.put(task, listener);
    }

    private void detachRemovedTaskListeners() {
        Set<Task> currentTasks = new HashSet<>(collectAllTasks(tasks));
        Set<Task> trackedTasks = new HashSet<>(subTaskListeners.keySet());

        for (Task task : trackedTasks) {
            if (!currentTasks.contains(task)) {
                ListChangeListener<SubTask> listener = subTaskListeners.remove(task);
                if (listener != null) {
                    task.getObservableSubTasks().removeListener(listener);
                }
            }
        }
    }

    @FXML
    private void showTreeContextMenu(ContextMenuEvent event) {

        if (currentContextMenu != null) {
            currentContextMenu.hide();
        }

        TreeItem<Object> selectedItem = treeTableView.getSelectionModel().getSelectedItem();

        currentContextMenu = new TaskContextMenu(
                this,
                selectedItem,
                treeTableView,
                ganttChart,
                tasks,
                () -> {
                    if (isAutoScheduleEnabled()) {
                        applyScheduleConstraints();
                    }
                    autoSaveProjectDocument();
                    rebuildTree();
                    updateVisibleItemsForGantt();
                    updateDashboard();
                    if (mainMenuBarController != null) {
                        mainMenuBarController.markAsUnsaved();
                    }
                }
        );

        currentContextMenu.setOnHidden(e -> currentContextMenu = null);
        currentContextMenu.show(treeTableView, event.getScreenX(), event.getScreenY());

        event.consume();
    }

    @FXML
    private void handleTableClick(MouseEvent event) {
        if (event.getTarget() instanceof TreeTableView) {
            treeTableView.getSelectionModel().clearSelection();
        }
    }

    public TreeTableView<Object> getTreeTableView() {
        return treeTableView;
    }

    public ScrollPane getGanttDiagram() {
        return ganttDiagram;
    }

    public ScrollPane getGanttHeaderScroll() {
        return ganttHeaderScroll;
    }

    public void refreshAfterExternalRender() {
        if (treeTableView == null || treeTableView.getScene() == null) {
            return;
        }

        Platform.runLater(() -> {
            if (treeTableView != null) {
                treeTableView.requestLayout();
                treeTableView.refresh();
            }
            if (ganttChart != null) {
                ganttChart.refresh();
            }
            if (ganttHeaderScroll != null) {
                ganttHeaderScroll.requestLayout();
            }
            if (ganttDiagram != null) {
                ganttDiagram.requestLayout();
            }
            if (workspaceRoot != null) {
                workspaceRoot.requestLayout();
            }
            if (topBarContainer != null) {
                topBarContainer.requestLayout();
            }
            if (zoomBarContainer != null) {
                zoomBarContainer.requestLayout();
            }
            if (filterBar != null) {
                filterBar.requestLayout();
            }
            if (dashboardBar != null) {
                dashboardBar.requestLayout();
            }
            if (mainSplitPane != null) {
                mainSplitPane.requestLayout();
                mainSplitPane.setDividerPositions(0.385);
            }

            PauseTransition relayoutDelay = new PauseTransition(Duration.millis(140));
            relayoutDelay.setOnFinished(e -> {
                syncTreeBottomSpacerWithGanttScrollbar();
                Scene scene = treeTableView.getScene();
                if (scene == null || scene.getRoot() == null) {
                    return;
                }
                javafx.scene.Parent root = scene.getRoot();
                root.requestLayout();
                root.applyCss();
                if (scene.getWindow() instanceof javafx.stage.Stage stage) {
                    forceRootGeometry(Math.max(scene.getWidth(), stage.getWidth()), Math.max(scene.getHeight(), stage.getHeight()), root);
                    syncContainerWidths(Math.max(scene.getWidth(), stage.getWidth()));
                    if (stage.isMaximized()) {
                        root.layout();
                        if (treeTableView != null) {
                            treeTableView.refresh();
                        }
                    } else {
                        forceWindowRelayout(stage);
                    }
                } else {
                    root.layout();
                }
            });
            relayoutDelay.play();
        });
    }

    public void recoverWindowAfterSystemDialog() {
        if (treeTableView == null || treeTableView.getScene() == null) {
            return;
        }
        Platform.runLater(() -> {
            scheduleWindowRecoveryCheck(8);
        });
    }

    private void scheduleWindowRecoveryCheck(int attemptsRemaining) {
        if (attemptsRemaining <= 0) {
            return;
        }

        PauseTransition delay = new PauseTransition(Duration.millis(110));
        delay.setOnFinished(event -> {
            performWindowRecoveryCheck();
            scheduleWindowRecoveryCheck(attemptsRemaining - 1);
        });
        delay.play();
    }

    private boolean performWindowRecoveryCheck() {
        if (treeTableView == null || treeTableView.getScene() == null) {
            return false;
        }

        Scene scene = treeTableView.getScene();
        if (!(scene.getWindow() instanceof javafx.stage.Stage stage) || !stage.isShowing()) {
            return false;
        }

        javafx.scene.Parent root = scene.getRoot();
        if (root == null) {
            return false;
        }

        root.applyCss();
        root.layout();
        if (treeTableView != null) {
            treeTableView.refresh();
        }
        if (ganttChart != null) {
            ganttChart.refresh();
        }

        if (!isLayoutCollapsed(stage, scene, root)) {
            return false;
        }

        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        WindowRecoveryService.TargetSize targetSize = windowRecoveryService.resolveTargetSize(
                scene.getWidth(),
                scene.getHeight(),
                stage.getWidth(),
                stage.getHeight(),
                stage.isMaximized(),
                bounds.getWidth(),
                bounds.getHeight()
        );
        double targetWidth = targetSize.width();
        double targetHeight = targetSize.height();

        forceRootGeometry(targetWidth, targetHeight, root);
        syncContainerWidths(targetWidth);
        root.requestLayout();
        root.layout();

        if (!isLayoutCollapsed(stage, scene, root)) {
            finalizeWindowRecovery(scene, root);
            return true;
        }

        if (stage.isMaximized()) {
            stage.setX(bounds.getMinX());
            stage.setY(bounds.getMinY());
            stage.setWidth(bounds.getWidth());
            stage.setHeight(bounds.getHeight());
            PauseTransition maximizeRefreshDelay = new PauseTransition(Duration.millis(40));
            maximizeRefreshDelay.setOnFinished(event -> {
                stage.setMaximized(true);
                Platform.runLater(() -> finalizeWindowRecovery(scene, root));
            });
            maximizeRefreshDelay.play();
        } else {
            forceWindowRelayout(stage);
            Platform.runLater(() -> finalizeWindowRecovery(scene, root));
        }
        return true;
    }

    private void finalizeWindowRecovery(Scene scene, javafx.scene.Parent root) {
        if (scene == null || root == null) {
            return;
        }
        root.applyCss();
        root.layout();
        if (treeTableView != null) {
            treeTableView.requestLayout();
            treeTableView.refresh();
        }
        if (ganttChart != null) {
            ganttChart.refresh();
        }
        if (mainSplitPane != null) {
            mainSplitPane.requestLayout();
            mainSplitPane.setDividerPositions(0.385);
        }
        syncTreeBottomSpacerWithGanttScrollbar();
    }

    private boolean isLayoutCollapsed(javafx.stage.Stage stage, Scene scene, javafx.scene.Parent root) {
        double sceneWidth = scene == null ? 0 : scene.getWidth();
        double stageWidth = stage == null ? 0 : stage.getWidth();
        double rootWidth = root == null ? 0 : root.getLayoutBounds().getWidth();
        double topWidth = topBarContainer == null ? 0 : topBarContainer.getLayoutBounds().getWidth();
        double workspaceWidth = workspaceRoot == null ? 0 : workspaceRoot.getLayoutBounds().getWidth();
        double splitWidth = mainSplitPane == null ? 0 : mainSplitPane.getLayoutBounds().getWidth();
        return windowRecoveryService.isLayoutCollapsed(
                sceneWidth,
                stageWidth,
                rootWidth,
                topWidth,
                workspaceWidth,
                splitWidth
        );
    }

    private void forceWindowRelayout(javafx.stage.Stage stage) {
        if (stage == null || !stage.isShowing()) {
            return;
        }

        double width = stage.getWidth();
        double height = stage.getHeight();
        if (windowRecoveryService.shouldNudgeWindow(width, height, stage.getMinWidth(), stage.getMinHeight())) {
            stage.setWidth(width + 1);
            stage.setWidth(width);
        }
    }

    private void applySceneSizeBindings(Scene scene) {
        if (scene == null || sceneSizeBindingApplied) {
            return;
        }
        sceneSizeBindingApplied = true;

        if (scene.getRoot() instanceof Region region) {
            region.prefWidthProperty().bind(scene.widthProperty());
            region.prefHeightProperty().bind(scene.heightProperty());
        }
        if (topBarContainer != null) {
            topBarContainer.prefWidthProperty().bind(scene.widthProperty());
        }
        if (workspaceRoot != null) {
            workspaceRoot.prefWidthProperty().bind(scene.widthProperty());
        }
        if (zoomBarContainer != null) {
            zoomBarContainer.prefWidthProperty().bind(scene.widthProperty());
        }
        if (filterBar != null) {
            filterBar.prefWidthProperty().bind(scene.widthProperty());
        }
        if (dashboardBar != null) {
            dashboardBar.prefWidthProperty().bind(scene.widthProperty());
        }
        if (mainSplitPane != null) {
            mainSplitPane.prefWidthProperty().bind(scene.widthProperty());
        }
    }

    private void forceRootGeometry(double width, double height, javafx.scene.Parent root) {
        if (root == null || width <= 0 || height <= 0) {
            return;
        }

        if (root instanceof Region region) {
            if (!region.prefWidthProperty().isBound()) {
                region.setPrefWidth(width);
            }
            if (!region.prefHeightProperty().isBound()) {
                region.setPrefHeight(height);
            }
            region.resizeRelocate(0, 0, width, height);
        }
    }

    private void syncContainerWidths(double width) {
        if (width <= 0) {
            return;
        }
        if (topBarContainer != null) {
            topBarContainer.requestLayout();
        }
        if (workspaceRoot != null) {
            workspaceRoot.requestLayout();
        }
        if (zoomBarContainer != null) {
            zoomBarContainer.requestLayout();
        }
        if (filterBar != null) {
            filterBar.requestLayout();
        }
        if (dashboardBar != null) {
            dashboardBar.requestLayout();
        }
        if (mainSplitPane != null) {
            mainSplitPane.requestLayout();
            mainSplitPane.setDividerPositions(0.385);
        }
    }

    public GanttChart getGanttChart() {
        return ganttChart;
    }

    public ObservableList<Task> getTasks() {
        return tasks;
    }

    public ObservableList<ProjectResource> getProjectResources() {
        return projectResources;
    }

    public List<Task> getAllTasksSnapshot() {
        return new ArrayList<>(collectAllTasks(tasks));
    }

    public VBox getWorkspaceRoot() {
        return workspaceRoot;
    }

    public Set<Object> getVisibleItemsSnapshot() {
        if (treeTableView == null) {
            return Set.of();
        }
        return new HashSet<>(TaskTreeViewSupport.collectVisibleItems(treeTableView).visible());
    }

    public List<Object> getDisplayOrderSnapshot() {
        if (treeTableView == null) {
            return List.of();
        }
        return new ArrayList<>(TaskTreeViewSupport.collectVisibleItems(treeTableView).orderedItems());
    }

    public void setColumnVisible(String columnKey, boolean visible) {
        TreeTableColumn<Object, ?> column = findColumn(columnKey);
        if (column != null) {
            column.setVisible(visible);
            if (ganttChart != null) {
                ganttChart.refresh();
            }
        }
    }

    public boolean isColumnVisible(String columnKey) {
        TreeTableColumn<Object, ?> column = findColumn(columnKey);
        return column != null && column.isVisible();
    }

    public void setShowDependencies(boolean visible) {
        showDependencies = visible;
        if (ganttChart != null) {
            ganttChart.setShowDependencies(visible);
        }
    }

    public boolean isShowDependencies() {
        return showDependencies;
    }

    private TreeTableColumn<Object, ?> findColumn(String columnKey) {
        return switch (columnKey) {
            case "position" -> positionColumn;
            case "name" -> taskNameColumn;
            case "company" -> companyColumn;
            case "start" -> startColumn;
            case "end" -> endColumn;
            case "unit" -> unitOfMeasurementColumn;
            case "volProject" -> volProjectColumn;
            case "volActual" -> volActualColumn;
            case "percent" -> percentColumn;
            case "status" -> statusColumn;
            case "priority" -> priorityColumn;
            default -> null;
        };
    }

    public String resolveColumnKey(TreeTableColumn<Object, ?> column) {
        if (column == null) {
            return null;
        }
        if (column == positionColumn) return "position";
        if (column == taskNameColumn) return "name";
        if (column == companyColumn) return "company";
        if (column == startColumn) return "start";
        if (column == endColumn) return "end";
        if (column == unitOfMeasurementColumn) return "unit";
        if (column == volProjectColumn) return "volProject";
        if (column == volActualColumn) return "volActual";
        if (column == percentColumn) return "percent";
        if (column == statusColumn) return "status";
        if (column == priorityColumn) return "priority";
        return null;
    }

    public String getProjectName() {
        return projectService.normalizeProjectName(projectName);
    }

    public String getProjectAuthor() {
        return projectService.normalizeAuthor(projectAuthor);
    }

    public void setProjectMetadata(String projectName, String author, boolean markUnsaved) {
        this.projectName = projectService.normalizeProjectName(projectName);
        this.projectAuthor = projectService.normalizeAuthor(author);
        syncProjectMetadataToSessionSettings();
        if (markUnsaved && mainMenuBarController != null) {
            mainMenuBarController.markAsUnsaved();
        }
    }

    public ProjectDocument toProjectDocument() {
        return projectService.createProjectDocument(
                getProjectName(),
                getProjectAuthor(),
                tasks,
                projectResources,
                budgetItems
        );
    }

    public void flushAutosaveNow() {
        autosaveService.flushPending();
    }

    public ObservableList<BudgetItem> getBudgetItems() {
        return budgetItems;
    }

    public void openResourceDirectory() {
        ResourceDirectoryDialog dialog = new ResourceDirectoryDialog(projectResources);
        if (treeTableView != null && treeTableView.getScene() != null) {
            dialog.setOwner(treeTableView.getScene().getWindow());
        }
        dialog.show();
        if (dialog.isChanged()) {
            syncAllTaskAssignmentsWithResources();
            autoSaveProjectDocument();
            if (mainMenuBarController != null) {
                mainMenuBarController.markAsUnsaved();
            }
            updateDashboard();
            if (treeTableView != null) {
                treeTableView.refresh();
            }
        }
    }

    public void openProjectBudget() {
        BudgetDialog dialog = new BudgetDialog(budgetItems, collectAllTasks(tasks));
        if (treeTableView != null && treeTableView.getScene() != null) {
            dialog.setOwner(treeTableView.getScene().getWindow());
        }
        dialog.show();
        if (dialog.isChanged()) {
            recalculateResourceLoadFlags();
            autoSaveProjectDocument();
            if (mainMenuBarController != null) {
                mainMenuBarController.markAsUnsaved();
            }
            if (treeTableView != null) {
                treeTableView.refresh();
            }
            if (ganttChart != null) {
                ganttChart.refresh();
            }
            updateDashboard();
        }
    }

    public void openPaymentCalendar() {
        if (treeTableView != null && treeTableView.getScene() != null) {
            PaymentCalendarDialog.show(treeTableView.getScene().getWindow(), budgetItems, tasks);
        }
    }

    public void openTaskResources(Task task) {
        if (task == null || task.isGroup()) {
            return;
        }
        TaskResourcesDialog dialog = new TaskResourcesDialog(task, projectResources);
        if (treeTableView != null && treeTableView.getScene() != null) {
            dialog.setOwner(treeTableView.getScene().getWindow());
        }
        dialog.show();
        if (dialog.isChanged()) {
            syncTaskAssignmentsWithResources(task);
            recalculateTaskCosts(task);
            autoSaveProjectDocument();
            if (mainMenuBarController != null) {
                mainMenuBarController.markAsUnsaved();
            }
            refreshAfterTaskMutation(task);
        }
    }

    public void openResourceLoadAnalysis() {
        ResourceLoadAnalyzer.Analysis analysis = ResourceLoadAnalyzer.analyze(projectResources, collectAllTasks(tasks));
        if (treeTableView != null && treeTableView.getScene() != null) {
            ResourceLoadDialog.show(treeTableView.getScene().getWindow(), analysis);
        }
    }

    public void openStageOverview() {
        if (treeTableView != null && treeTableView.getScene() != null) {
            ProjectStageOverviewDialog.show(treeTableView.getScene().getWindow(), tasks, budgetItems);
        }
    }

    private void syncAllTaskAssignmentsWithResources() {
        for (Task task : collectAllTasks(tasks)) {
            syncTaskAssignmentsWithResources(task);
            recalculateTaskCosts(task);
        }
        recalculateResourceLoadFlags();
    }

    private void syncTaskAssignmentsWithResources(Task task) {
        taskHealthService.syncTaskAssignmentsWithResources(task, projectResources);
    }

    private void recalculateTaskCosts(Task task) {
        taskHealthService.recalculateTaskCosts(task, projectResources);
    }

    private void recalculateResourceLoadFlags() {
        taskHealthService.recalculateResourceAndBudgetFlags(
                collectAllTasks(tasks),
                projectResources,
                budgetItems
        );
    }

    private void setupSelectionHandling() {
        setupTaskDragAndDrop();

        treeTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldItem, newItem) -> {
            Object selectedValue = newItem == null ? null : newItem.getValue();
            if (ganttChart != null) {
                ganttChart.setSelectedItem(selectedValue);
            }
        });

        treeTableView.setOnMouseClicked(event -> {
            TreeItem<Object> item = treeTableView.getSelectionModel().getSelectedItem();
            if (item == null || treeTableView.getRow(item) < 0 || event.getTarget() instanceof TreeTableView) {
                treeTableView.getSelectionModel().clearSelection();
            }
        });

        treeTableView.setOnMousePressed(event -> {
            if (currentContextMenu != null) {
                currentContextMenu.hide();
            }

            if (event.getTarget() instanceof TreeTableView) {
                treeTableView.getSelectionModel().clearSelection();
            }
        });

        treeTableView.setOnKeyPressed(event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                treeTableView.getSelectionModel().clearSelection();
            }
        });
    }

    private void setupDashboardInteractions() {
        if (riskSummaryLabel != null) {
            riskSummaryLabel.setCursor(javafx.scene.Cursor.HAND);
            riskSummaryLabel.setOnMouseClicked(event -> openStageOverview());
        }
    }

    private void setupTaskDragAndDrop() {
        treeTableView.setRowFactory(tv -> {
            TreeTableRow<Object> row = new TreeTableRow<>() {
                @Override
                protected void updateItem(Object item, boolean empty) {
                    super.updateItem(item, empty);
                    clearDropStyle(this);
                    getStyleClass().remove("group-row");
                    getStyleClass().remove("resource-overloaded-row");
                    getStyleClass().remove("budget-overrun-row");
                    setOpacity(1.0);
                    if (!empty && item instanceof Task task) {
                        if (task.isGroup()) {
                            ensureRowStyleClass(this, "group-row");
                        }
                        if (task.isResourceOverloaded()) {
                            ensureRowStyleClass(this, "resource-overloaded-row");
                        }
                        if (task.isBudgetOverrun()) {
                            ensureRowStyleClass(this, "budget-overrun-row");
                        }
                        String tooltipText = buildTaskSignalTooltip(task);
                        setTooltip(tooltipText == null || tooltipText.isBlank() ? null : new Tooltip(tooltipText));
                    } else {
                        setTooltip(null);
                    }
                }
            };
            PauseTransition expandDelay = new PauseTransition(Duration.millis(260));

            row.setOnDragDetected(event -> {
                if (row.isEmpty()) {
                    return;
                }
                Object item = row.getItem();
                if (!(item instanceof Task) && !(item instanceof SubTask)) {
                    return;
                }
                draggedTask = item instanceof Task task ? task : null;
                draggedSubTask = item instanceof SubTask subTask ? subTask : null;
                Dragboard dragboard = row.startDragAndDrop(TransferMode.MOVE);
                ClipboardContent content = new ClipboardContent();
                if (item instanceof Task task) {
                    content.putString(task.getPosition() == null ? task.getName() : task.getPosition());
                } else if (item instanceof SubTask subTask) {
                    content.putString(subTask.getSubPosition() == null ? subTask.getName() : subTask.getSubPosition());
                }
                dragboard.setContent(content);
                WritableImage dragPreview = createRowDragPreview(row);
                if (dragPreview != null) {
                    dragboard.setDragView(dragPreview, 18, 12);
                }
                applyDragSourceStyle(row);
                event.consume();
            });

            row.setOnDragOver(event -> {
                if (draggedTask == null && draggedSubTask == null) {
                    return;
                }
                TaskInteractionService.DropMode dropMode = resolveDropMode(row, event);
                if (dropMode != TaskInteractionService.DropMode.NONE) {
                    event.acceptTransferModes(TransferMode.MOVE);
                }
                applyDropStyle(row, dropMode);
                handleAutoExpandDuringDrag(row, dropMode, expandDelay);
                autoScrollDuringDrag(event.getSceneY());
                event.consume();
            });

            row.setOnDragDropped(event -> {
                boolean completed = false;
                TaskInteractionService.DropMode dropMode = resolveDropMode(row, event);
                if (dropMode != TaskInteractionService.DropMode.NONE) {
                    completed = handleDrop(row, dropMode);
                }
                expandDelay.stop();
                clearDropStyle(row);
                event.setDropCompleted(completed);
                draggedTask = null;
                draggedSubTask = null;
                event.consume();
            });

            row.setOnDragExited(event -> {
                expandDelay.stop();
                clearDropStyle(row);
            });
            row.setOnDragDone(event -> {
                expandDelay.stop();
                clearDropStyle(row);
                clearDragSourceStyle(row);
                draggedTask = null;
                draggedSubTask = null;
            });
            return row;
        });

        treeTableView.setOnDragOver(event -> {
            if (draggedTask != null && canMoveTaskToRoot(draggedTask)) {
                event.acceptTransferModes(TransferMode.MOVE);
            }
            autoScrollDuringDrag(event.getSceneY());
            event.consume();
        });

        treeTableView.setOnDragDropped(event -> {
            boolean completed = false;
            if (draggedTask != null && canMoveTaskToRoot(draggedTask)) {
                moveTaskToRoot(draggedTask);
                completed = true;
            }
            event.setDropCompleted(completed);
            draggedTask = null;
            draggedSubTask = null;
            event.consume();
        });
    }

    private void installHotkeysWhenSceneReady() {
        if (treeTableView == null) {
            return;
        }
        treeTableView.sceneProperty().addListener((obs, oldScene, newScene) -> applyHotkeys(newScene));
        if (treeTableView.getScene() != null) {
            applyHotkeys(treeTableView.getScene());
        } else {
            Platform.runLater(() -> applyHotkeys(treeTableView.getScene()));
        }
    }

    private void applyHotkeys(Scene scene) {
        if (scene == null) {
            return;
        }
        Settings settings = AppSettings.get();
        HotkeyManager.install(scene, List.of(
                new HotkeyManager.HotkeyAction("\u041d\u043e\u0432\u044b\u0439 \u043f\u0440\u043e\u0435\u043a\u0442", settings.getHotkeyNewProject(),
                        () -> runIfReady(mainMenuBarController, () -> mainMenuBarController.newProjectAction())),
                new HotkeyManager.HotkeyAction("\u0417\u0430\u0433\u0440\u0443\u0437\u0438\u0442\u044c \u043f\u0440\u043e\u0435\u043a\u0442", settings.getHotkeyOpenProject(),
                        () -> runIfReady(mainMenuBarController, () -> mainMenuBarController.loadProjectAction())),
                new HotkeyManager.HotkeyAction("\u0421\u043e\u0445\u0440\u0430\u043d\u0438\u0442\u044c", settings.getHotkeySaveProject(),
                        () -> runIfReady(mainMenuBarController, () -> mainMenuBarController.saveProjectAction())),
                new HotkeyManager.HotkeyAction("\u0421\u043e\u0445\u0440\u0430\u043d\u0438\u0442\u044c \u043a\u0430\u043a", settings.getHotkeySaveAsProject(),
                        () -> runIfReady(mainMenuBarController, () -> mainMenuBarController.saveAsProjectAction())),
                new HotkeyManager.HotkeyAction("\u041f\u0435\u0447\u0430\u0442\u044c \u043f\u0440\u043e\u0435\u043a\u0442\u0430", settings.getHotkeyPrintProject(),
                        () -> runIfReady(mainMenuBarController, () -> mainMenuBarController.printProjectAction())),
                new HotkeyManager.HotkeyAction("\u042d\u043a\u0441\u043f\u043e\u0440\u0442 PDF", settings.getHotkeyExportPdf(),
                        () -> runIfReady(mainMenuBarController, () -> mainMenuBarController.exportPdfAction())),
                new HotkeyManager.HotkeyAction("\u042d\u043a\u0441\u043f\u043e\u0440\u0442 CSV", settings.getHotkeyExportCsv(),
                        () -> runIfReady(mainMenuBarController, () -> mainMenuBarController.exportCsvAction())),
                new HotkeyManager.HotkeyAction("\u041f\u043e\u0438\u0441\u043a", settings.getHotkeyFocusSearch(),
                        this::focusSearchField),
                new HotkeyManager.HotkeyAction("\u041f\u0435\u0440\u0435\u0441\u0447\u0438\u0442\u0430\u0442\u044c", settings.getHotkeyRecalculate(),
                        this::handleRecalculateSchedule),
                new HotkeyManager.HotkeyAction("\u041c\u0430\u0441\u0448\u0442\u0430\u0431 +", settings.getHotkeyZoomIn(),
                        () -> runIfReady(zoomBarController, () -> zoomBarController.zoomInAction())),
                new HotkeyManager.HotkeyAction("\u041c\u0430\u0441\u0448\u0442\u0430\u0431 -", settings.getHotkeyZoomOut(),
                        () -> runIfReady(zoomBarController, () -> zoomBarController.zoomOutAction())),
                new HotkeyManager.HotkeyAction("\u041c\u0430\u0441\u0448\u0442\u0430\u0431 100%", settings.getHotkeyZoomReset(),
                        () -> runIfReady(zoomBarController, () -> zoomBarController.zoomResetAction())),
                new HotkeyManager.HotkeyAction("\u041f\u043e\u0434\u0441\u043a\u0430\u0437\u043a\u0430", settings.getHotkeyShowHelp(),
                        () -> HotkeyManager.showHelp(
                                treeTableView.getScene() == null ? null : treeTableView.getScene().getWindow(),
                                settings)),
                new HotkeyManager.HotkeyAction("\u041e\u0442\u043c\u0435\u043d\u0438\u0442\u044c \u0443\u0434\u0430\u043b\u0435\u043d\u0438\u0435", settings.getHotkeyUndoDelete(),
                        this::undoLastDeletion)
        ));

        // Hard fallback so Ctrl+P always opens the unified print center
        // even if user settings contain an invalid or conflicting shortcut.
        scene.getAccelerators().put(
                KeyCombination.keyCombination("Ctrl+P"),
                () -> runIfReady(mainMenuBarController, () -> mainMenuBarController.printProjectAction())
        );
    }

    private void focusSearchField() {
        if (searchField != null) {
            searchField.requestFocus();
            searchField.selectAll();
        }
    }

    private void runIfReady(Object owner, Runnable action) {
        if (owner != null && action != null) {
            action.run();
        }
    }

    private int getVisibleRowCount() {
        return TaskTreeViewSupport.getVisibleRowCount(treeTableView);
    }

    private void updateGanttHeight() {
        if (ganttChart != null) {
            int visibleRows = getVisibleRowCount();
            double rowHeight = resolveTableRowHeight();
            treeTableView.setFixedCellSize(rowHeight);
            ganttChart.setRowHeight(rowHeight);
            double headerHeight = mainViewLayoutService.resolveHeaderHeight(ganttChart.getHeaderHeight());

            if (ganttHeaderScroll != null) {
                ganttHeaderScroll.setMinHeight(headerHeight);
                ganttHeaderScroll.setPrefHeight(headerHeight);
                ganttHeaderScroll.setMaxHeight(headerHeight);
            }

            double chartHeight = mainViewLayoutService.resolveChartHeight(visibleRows, rowHeight);
            ganttChart.setChartPreferredHeight(chartHeight);
            ganttChart.refresh();
            syncTreeBottomSpacerWithGanttScrollbar();
        }
    }

    private void setupTreeBottomSpacerSync() {
        Platform.runLater(this::syncTreeBottomSpacerWithGanttScrollbar);
        ganttDiagram.skinProperty().addListener((obs, oldSkin, newSkin) ->
                Platform.runLater(this::syncTreeBottomSpacerWithGanttScrollbar));
        ganttDiagram.viewportBoundsProperty().addListener((obs, oldBounds, newBounds) ->
                Platform.runLater(this::syncTreeBottomSpacerWithGanttScrollbar));
        ganttDiagram.hvalueProperty().addListener((obs, oldVal, newVal) ->
                syncTreeBottomSpacerWithGanttScrollbar());
    }

    private void syncTreeBottomSpacerWithGanttScrollbar() {
        if (treeBottomSpacer == null || ganttDiagram == null) {
            return;
        }
        double spacerHeight = 0.0;
        for (Node node : ganttDiagram.lookupAll(".scroll-bar")) {
            if (node instanceof ScrollBar scrollBar) {
                spacerHeight = mainViewLayoutService.accumulateHorizontalScrollbarHeight(
                        spacerHeight,
                        scrollBar.getOrientation() == Orientation.HORIZONTAL,
                        scrollBar.isVisible(),
                        scrollBar.getHeight()
                );
            }
        }
        treeBottomSpacer.setMinHeight(spacerHeight);
        treeBottomSpacer.setPrefHeight(spacerHeight);
        treeBottomSpacer.setMaxHeight(spacerHeight);
    }

    private double resolveTableRowHeight() {
        Settings settings = AppSettings.get();
        String textStyle = settings == null || settings.getTextStyle() == null
                ? "standard"
                : settings.getTextStyle().trim().toLowerCase();
        return mainViewLayoutService.resolveTableRowHeight(textStyle);
    }

    private void bindGanttHorizontalScrolls() {
        if (ganttHeaderScroll == null || ganttDiagram == null) {
            return;
        }

        ganttHeaderScroll.hvalueProperty().addListener((obs, oldValue, newValue) -> {
            if (ganttHorizontalSyncInProgress) {
                return;
            }
            ganttHorizontalSyncInProgress = true;
            ganttDiagram.setHvalue(newValue.doubleValue());
            ganttHorizontalSyncInProgress = false;
        });

        ganttDiagram.hvalueProperty().addListener((obs, oldValue, newValue) -> {
            if (ganttHorizontalSyncInProgress) {
                return;
            }
            ganttHorizontalSyncInProgress = true;
            ganttHeaderScroll.setHvalue(newValue.doubleValue());
            ganttHorizontalSyncInProgress = false;
        });

        ganttDiagram.viewportBoundsProperty().addListener((obs, oldValue, newValue) ->
                Platform.runLater(() -> ganttHeaderScroll.setHvalue(ganttDiagram.getHvalue())));
    }

    private void updateVisibleItemsForGantt() {
        TaskTreeViewSupport.VisibleItems visibleItems = TaskTreeViewSupport.collectVisibleItems(treeTableView);

        if (ganttChart != null) {
            ganttChart.setVisibleItems(visibleItems.visible());
            ganttChart.setDisplayOrder(visibleItems.orderedItems());
        }
    }

    private void setupBidirectionalScrollSync() {
        Runnable bindTreeScrollBar = () -> {
            for (Node node : treeTableView.lookupAll(".scroll-bar")) {
                if (node instanceof ScrollBar scrollBar && scrollBar.getOrientation() == Orientation.VERTICAL) {
                    treeVerticalScrollBar = scrollBar;

                    treeVerticalScrollBar.valueProperty().addListener((obs, oldVal, newVal) -> {
                        if (scrollSyncInProgress) {
                            return;
                        }

                        double min = treeVerticalScrollBar.getMin();
                        double max = treeVerticalScrollBar.getMax();
                        if (max <= min) {
                            return;
                        }

                        scrollSyncInProgress = true;
                        double normalized = (newVal.doubleValue() - min) / (max - min);
                        ganttDiagram.setVvalue(Math.max(0, Math.min(1, normalized)));
                        scrollSyncInProgress = false;
                    });
                    break;
                }
            }
        };

        treeTableView.skinProperty().addListener((obs, oldSkin, newSkin) -> Platform.runLater(bindTreeScrollBar));
        Platform.runLater(bindTreeScrollBar);

        ganttDiagram.addEventFilter(javafx.scene.input.ScrollEvent.SCROLL, event -> {
            if (event.isControlDown() && zoomBarController != null) {
                if (event.getDeltaY() > 0) {
                    zoomBarController.zoomInAction();
                } else if (event.getDeltaY() < 0) {
                    zoomBarController.zoomOutAction();
                }
                event.consume();
                return;
            }
            if (treeVerticalScrollBar == null) {
                return;
            }

            double min = treeVerticalScrollBar.getMin();
            double max = treeVerticalScrollBar.getMax();
            if (max <= min) {
                return;
            }

            double step = Math.max((max - min) / Math.max(treeTableView.getExpandedItemCount() / 4.0, 1), 0.01);
            double delta = event.getDeltaY() > 0 ? -step : step;

            scrollSyncInProgress = true;
            double newValue = Math.max(min, Math.min(max, treeVerticalScrollBar.getValue() + delta));
            treeVerticalScrollBar.setValue(newValue);
            ganttDiagram.setVvalue((newValue - min) / (max - min));
            scrollSyncInProgress = false;

            event.consume();
        });
    }

    private boolean taskMatchesFilters(Task task) {
        return taskFilterService.taskMatchesFilters(task, searchQuery, statusFilter);
    }

    private boolean taskMatchesStatus(Task task) {
        return taskFilterService.taskMatchesStatus(task, statusFilter);
    }

    private List<SubTask> getSubTasksForTree(Task task) {
        return taskFilterService.getSubTasksForTree(task, searchQuery, statusFilter);
    }

    private List<Task> getChildTasksForTree(Task task) {
        return taskFilterService.getChildTasksForTree(task, searchQuery, statusFilter);
    }

    @FXML
    private void handleRecalculateSchedule() {
        boolean recalculated = applyScheduleConstraints();
        autoSaveProjectDocument();
        rebuildTree();
        updateVisibleItemsForGantt();
        updateDashboard();
        if (mainMenuBarController != null) {
            mainMenuBarController.markAsUnsaved();
        }
        if (recalculated) {
            maybeOfferCriticalPath();
        }
    }

    private boolean applyScheduleConstraints() {
        if (scheduleRecalcInProgress) {
            return false;
        }
        try {
            scheduleRecalcInProgress = true;
            TaskDependencyScheduler.applyConstraints(tasks);
            return true;
        } catch (IllegalStateException e) {
            showScheduleError(e.getMessage());
            return false;
        } finally {
            scheduleRecalcInProgress = false;
        }
    }

    private void maybeOfferCriticalPath() {
        if (criticalPathPromptInProgress || treeTableView == null || treeTableView.getScene() == null) {
            return;
        }

        CriticalPathAnalyzer.Analysis analysis = CriticalPathAnalyzer.analyze(tasks);
        if (analysis == null || !analysis.isMeaningful()) {
            return;
        }

        criticalPathPromptInProgress = true;
        try {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("\u041a\u0440\u0438\u0442\u0438\u0447\u0435\u0441\u043a\u0438\u0439 \u043f\u0443\u0442\u044c");
            alert.setHeaderText("\u041d\u0430\u0439\u0434\u0435\u043d \u043a\u0440\u0438\u0442\u0438\u0447\u0435\u0441\u043a\u0438\u0439 \u043f\u0443\u0442\u044c \u043f\u0440\u043e\u0435\u043a\u0442\u0430");
            alert.setContentText(
                    "\u0414\u043b\u0438\u0442\u0435\u043b\u044c\u043d\u043e\u0441\u0442\u044c: " + analysis.totalDays() + " \u0434\u043d.\n"
                            + "\u042d\u043b\u0435\u043c\u0435\u043d\u0442\u043e\u0432 \u0432 \u0446\u0435\u043f\u043e\u0447\u043a\u0435: " + analysis.steps().size() + "\n\n"
                            + "\u041f\u043e\u043a\u0430\u0437\u0430\u0442\u044c \u043f\u0443\u0442\u044c \u0432 \u043e\u0442\u0434\u0435\u043b\u044c\u043d\u043e\u043c \u043e\u043a\u043d\u0435?"
            );
            alert.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
            SceneManager.applyThemeToDialog(alert);
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.YES) {
                presentCriticalPath(analysis, true);
            }
        } finally {
            criticalPathPromptInProgress = false;
        }
    }

    private void showCriticalPathFromToolbar() {
        if (highlightedCriticalPath != null) {
            clearCriticalPathState();
            return;
        }
        CriticalPathAnalyzer.Analysis analysis = CriticalPathAnalyzer.analyze(tasks);
        if (analysis == null || !analysis.isMeaningful()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("\u041a\u0440\u0438\u0442\u0438\u0447\u0435\u0441\u043a\u0438\u0439 \u043f\u0443\u0442\u044c");
            alert.setHeaderText("\u041a\u0440\u0438\u0442\u0438\u0447\u0435\u0441\u043a\u0438\u0439 \u043f\u0443\u0442\u044c \u043d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d");
            alert.setContentText("\u0414\u043b\u044f \u0442\u0435\u043a\u0443\u0449\u0435\u0433\u043e \u043f\u043b\u0430\u043d\u0430 \u043d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043f\u043e\u0441\u0442\u0440\u043e\u0438\u0442\u044c \u0432\u044b\u0440\u0430\u0437\u0438\u0442\u0435\u043b\u044c\u043d\u0443\u044e \u043a\u0440\u0438\u0442\u0438\u0447\u0435\u0441\u043a\u0443\u044e \u0446\u0435\u043f\u043e\u0447\u043a\u0443. \u041f\u0440\u043e\u0432\u0435\u0440\u044c \u0437\u0430\u0432\u0438\u0441\u0438\u043c\u043e\u0441\u0442\u0438 \u0438 \u043f\u0435\u0440\u0435\u0441\u0447\u0438\u0442\u0430\u0439 \u0440\u0430\u0441\u043f\u0438\u0441\u0430\u043d\u0438\u0435.");
            SceneManager.applyThemeToDialog(alert);
            alert.showAndWait();
            clearCriticalPathState();
            return;
        }
        presentCriticalPath(analysis, false);
    }

    private void showProjectAnalysisFromToolbar() {
        ProjectAnalysisAnalyzer.Analysis analysis = ProjectAnalysisAnalyzer.analyze(tasks, projectResources, budgetItems);
        if (analysis == null || !analysis.isMeaningful()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Анализ проекта");
            alert.setHeaderText("Недостаточно данных для анализа");
            alert.setContentText("Для текущего плана не удалось собрать выразимую аналитику. Проверь задачи, даты, зависимости и пересчитай расписание.");
            SceneManager.applyThemeToDialog(alert);
            alert.showAndWait();
            return;
        }
        if (treeTableView != null && treeTableView.getScene() != null) {
            ProjectAnalysisDialog.show(treeTableView.getScene().getWindow(), analysis);
        }
    }

    private void presentCriticalPath(CriticalPathAnalyzer.Analysis analysis, boolean openDialog) {
        highlightedCriticalPath = analysis;
        if (ganttChart != null) {
            ganttChart.setCriticalPathHighlight(analysis);
        }
        if (zoomBarController != null) {
            zoomBarController.setCriticalPathVisible(true);
        }
        if (openDialog && treeTableView != null && treeTableView.getScene() != null) {
            CriticalPathDialog.show(treeTableView.getScene().getWindow(), analysis);
        }
    }

    private void clearCriticalPathState() {
        highlightedCriticalPath = null;
        if (ganttChart != null) {
            ganttChart.clearCriticalPathHighlight();
        }
        if (zoomBarController != null) {
            zoomBarController.setCriticalPathVisible(false);
        }
    }

    private boolean isAutoScheduleEnabled() {
        return autoScheduleCheckBox == null || autoScheduleCheckBox.isSelected();
    }

    private void selectObjectInTree(Object target) {
        if (treeTableView == null || treeTableView.getRoot() == null || target == null) {
            return;
        }
        trySelectObjectInTree(target, 6);
    }

    private void trySelectObjectInTree(Object target, int attemptsLeft) {
        if (treeTableView == null || treeTableView.getRoot() == null || target == null || attemptsLeft <= 0) {
            return;
        }
        TreeItem<Object> match = findTreeItem(treeTableView.getRoot(), target);
        if (match == null) {
            Platform.runLater(() -> trySelectObjectInTree(target, attemptsLeft - 1));
            return;
        }
        expandParents(match);
        if (syncTreeSelection(match)) {
            return;
        }
        Platform.runLater(() -> trySelectObjectInTree(target, attemptsLeft - 1));
    }

    private boolean syncTreeSelection(TreeItem<Object> item) {
        if (treeTableView == null || item == null) {
            return false;
        }
        try {
            treeTableView.applyCss();
            treeTableView.layout();
            int row = treeTableView.getRow(item);
            if (row < 0) {
                return false;
            }
            if (treeTableView.getSelectionModel().getSelectedItem() != item) {
                treeTableView.getSelectionModel().select(item);
            }
            treeTableView.getFocusModel().focus(row);
            scrollTreeToRowIfNeeded(row);
            treeTableView.refresh();
            return true;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private void scrollTreeToRowIfNeeded(int row) {
        if (treeTableView == null || row < 0) {
            return;
        }
        ScrollBar verticalBar = findTreeVerticalScrollBar();
        double fixedCellSize = treeTableView.getFixedCellSize();
        if (verticalBar == null || !verticalBar.isVisible() || fixedCellSize <= 0) {
            treeTableView.scrollTo(row);
            return;
        }

        int visibleRows = Math.max(1, (int) Math.floor(treeTableView.getHeight() / fixedCellSize));
        int expandedRows = Math.max(treeTableView.getExpandedItemCount(), visibleRows);
        int maxFirstRow = Math.max(expandedRows - visibleRows, 0);
        int firstVisibleRow = (int) Math.round(verticalBar.getValue() * maxFirstRow);
        int lastVisibleRow = firstVisibleRow + visibleRows - 1;

        if (row < firstVisibleRow + 1 || row > lastVisibleRow - 1) {
            treeTableView.scrollTo(row);
        }
    }

    private TreeItem<Object> findTreeItem(TreeItem<Object> root, Object target) {
        if (root == null) {
            return null;
        }
        if (root.getValue() == target) {
            return root;
        }
        for (TreeItem<Object> child : root.getChildren()) {
            TreeItem<Object> match = findTreeItem(child, target);
            if (match != null) {
                return match;
            }
        }
        return null;
    }

    private void expandParents(TreeItem<Object> item) {
        TreeItem<Object> cursor = item;
        while (cursor != null) {
            cursor.setExpanded(true);
            cursor = cursor.getParent();
        }
    }

    public void collapseAllSubTasks() {
        applyExpansionStateToAllTasks(false);
    }

    public void expandAllSubTasks() {
        applyExpansionStateToAllTasks(true);
    }

    public void expandSelectedTaskSubTasks() {
        TreeItem<Object> selected = treeTableView == null ? null : treeTableView.getSelectionModel().getSelectedItem();
        if (taskExpansionService.setSelectedTaskExpanded(selected, true)) {
            syncAfterExpansionChange();
        }
    }

    public void collapseSelectedTaskSubTasks() {
        TreeItem<Object> selected = treeTableView == null ? null : treeTableView.getSelectionModel().getSelectedItem();
        if (taskExpansionService.setSelectedTaskExpanded(selected, false)) {
            syncAfterExpansionChange();
        }
    }

    private void applyExpansionStateToAllTasks(boolean expanded) {
        TreeItem<Object> root = treeTableView == null ? null : treeTableView.getRoot();
        if (!taskExpansionService.applyExpansionStateToRootChildren(root, expanded)) {
            return;
        }
        syncAfterExpansionChange();
    }

    private void syncAfterExpansionChange() {
        mainControllerUiSyncService.syncAfterExpansionChange(
                this::updateVisibleItemsForGantt,
                this::updateGanttHeight,
                treeTableView == null ? null : treeTableView::refresh,
                ganttChart == null ? null : ganttChart::refresh
        );
    }

    public void deleteTaskWithUndo(Task task) {
        if (task == null) {
            return;
        }
        int index = tasks.indexOf(task);
        if (index < 0) {
            return;
        }
        deletedItems.push(DeletedItem.task(task, index));
        tasks.remove(task);
    }

    public void deleteSubTaskWithUndo(SubTask subTask) {
        if (subTask == null) {
            return;
        }
        Task parent = subTask.getParentTask();
        if (parent == null) {
            return;
        }
        int index = parent.getObservableSubTasks().indexOf(subTask);
        if (index < 0) {
            return;
        }
        deletedItems.push(DeletedItem.subTask(parent, subTask, index));
        parent.removeSubTask(subTask);
    }

    public void undoLastDeletion() {
        DeletedItem deleted = deletedItems.pollFirst();
        if (deleted == null) {
            return;
        }

        if (deleted.isTask()) {
            int index = Math.max(0, Math.min(deleted.index(), tasks.size()));
            tasks.add(index, deleted.task());
            selectObjectInTree(deleted.task());
            return;
        }

        Task parent = deleted.parentTask();
        SubTask subTask = deleted.subTask();
        if (parent == null || subTask == null) {
            return;
        }
        ObservableList<SubTask> subTasks = parent.getObservableSubTasks();
        int index = Math.max(0, Math.min(deleted.index(), subTasks.size()));
        subTasks.add(index, subTask);
        parent.updatePercentFromSubTasks();
        rebuildTree();
        updateVisibleItemsForGantt();
        updateDashboard();
        selectObjectInTree(subTask);
        if (mainMenuBarController != null) {
            mainMenuBarController.markAsUnsaved();
        }
    }

    public void clearUndoHistory() {
        deletedItems.clear();
    }

    public void showHelpDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("\u0421\u043f\u0440\u0430\u0432\u043a\u0430");
        alert.setHeaderText("\u041a\u0430\u043a \u0440\u0430\u0431\u043e\u0442\u0430\u0435\u0442 Gantrem");
        alert.setContentText(
                "1. Create tasks from the table or context menu.\n"
                        + "2. Subtasks affect parent progress and are shown on the chart.\n"
                        + "3. Dependencies can be added in task dialogs or directly on the chart in Link mode.\n"
                        + "4. Link types: FS = finish -> start, SS = start -> start, FF = finish -> finish, SF = start -> finish.\n"
                        + "5. Negative lag moves the dependent item earlier, positive lag moves it later.\n"
                        + "6. AV and KS markers on the chart show advance and KS payment dates.\n"
                        + "7. For tasks with subtasks: 0% = Planned, 0-99.9% = In progress, 100% = Completed.\n"
                        + "8. Print and PDF export use the current table and chart state.\n"
                        + "9. Ctrl+Z restores the last deleted task or subtask."
        );
        SceneManager.applyThemeToDialog(alert);
        alert.showAndWait();
    }

    private void createDependencyFromGantt(Object sourceItem, Object targetItem) {
        if (sourceItem == null || targetItem == null || sourceItem == targetItem) {
            return;
        }

        Optional<DependencyDialog.Result> dialogResult = DependencyDialog.showCreate(
                treeTableView == null || treeTableView.getScene() == null ? null : treeTableView.getScene().getWindow(),
                dependencyEditingService.itemLabel(sourceItem),
                dependencyEditingService.itemLabel(targetItem)
        );
        if (dialogResult.isEmpty()) {
            return;
        }

        boolean updated = dependencyEditingService.upsertDependency(
                sourceItem,
                targetItem,
                dialogResult.get().type(),
                dialogResult.get().lagDays(),
                dialogResult.get().critical()
        );
        if (!updated) {
            return;
        }

        if (isAutoScheduleEnabled()) {
            applyScheduleConstraints();
        }
        autoSaveProjectDocument();
        rebuildTree();
        updateVisibleItemsForGantt();
        updateDashboard();
        if (mainMenuBarController != null) {
            mainMenuBarController.markAsUnsaved();
        }
    }

    private void recomputeCriticalDependencyFlags() {
        for (Task task : collectAllTasks(tasks)) {
            boolean hasCriticalDependency = task.getDependencies().stream().anyMatch(TaskDependency::isCritical)
                    || task.getObservableSubTasks().stream()
                    .flatMap(subTask -> subTask.getDependencies().stream())
                    .anyMatch(TaskDependency::isCritical);
            task.setCriticalDependency(task.isCriticalDependency() || hasCriticalDependency);
        }
    }

    private void applyDatesFromGantt(Object item, LocalDate newStart, LocalDate newEnd) {
        boolean changed = taskService.applyGanttDateChange(item, newStart, newEnd);
        if (!changed) {
            return;
        }
        refreshAfterTaskMutation(item);
    }

    private void refreshAfterTaskMutation(Object item) {
        clearCriticalPathState();
        recalculateResourceLoadFlags();
        autoSaveProjectDocument();
        rebuildTree();
        updateVisibleItemsForGantt();
        updateDashboard();
        Platform.runLater(() -> selectObjectInTree(item));
        if (mainMenuBarController != null) {
            mainMenuBarController.markAsUnsaved();
        }
    }

    private void applyFinancialDateFromGantt(Task task, GanttChart.FinancialMarkerType markerType, LocalDate newDate) {
        if (task == null || markerType == null || newDate == null) {
            return;
        }

        switch (markerType) {
            case ADVANCE -> task.setContractAdvanceDate(newDate);
            case KS -> task.setKsPaymentDate(newDate);
        }
        refreshAfterTaskMutation(task);
    }

    public void handleManualDialogEdit(Object item) {
        if (item == null) {
            return;
        }
        refreshAfterTaskMutation(item);
    }

    public void addTaskToGroup(Task group, Task childTask) {
        if (group == null || childTask == null || !group.isGroup()) {
            return;
        }
        taskService.removeTaskFromCurrentParent(tasks, childTask);
        group.addChildTask(childTask);
        normalizeTaskHierarchy();
        refreshAfterTaskMutation(group);
    }

    public void moveTaskToGroup(Task task, Task group) {
        if (!taskService.moveTaskToGroup(tasks, task, group)) {
            return;
        }
        normalizeTaskHierarchy();
        refreshAfterTaskMutation(task);
    }

    public void moveTaskToRoot(Task task) {
        if (!taskService.moveTaskToRoot(tasks, task)) {
            return;
        }
        normalizeTaskHierarchy();
        refreshAfterTaskMutation(task);
    }

    public void moveTaskRelative(Task task, Task target, boolean insertAfter) {
        if (!taskService.moveTaskRelative(tasks, task, target, insertAfter)) {
            return;
        }
        normalizeTaskHierarchy();
        refreshAfterTaskMutation(task);
    }

    private boolean canMoveTaskIntoGroup(Task task, Task group) {
        return taskService.canMoveTaskIntoGroup(tasks, task, group);
    }

    private boolean canMoveTaskToRoot(Task task) {
        return taskService.canMoveTaskToRoot(tasks, task);
    }

    private boolean canReorderTaskRelative(Task task, Task target) {
        return taskService.canReorderTaskRelative(task, target);
    }

    private Task findParentTask(Task target) {
        return taskService.findParentTask(tasks, target);
    }

    private boolean isDescendantTask(Task candidateAncestor, Task target) {
        return taskService.isDescendantTask(candidateAncestor, target);
    }

    private boolean canMoveSubTaskToTask(SubTask subTask, Task targetTask) {
        return taskService.canMoveSubTaskToTask(subTask, targetTask);
    }

    private void moveSubTaskToTask(SubTask subTask, Task targetTask, int index) {
        Task updatedParent = taskService.moveSubTaskToTask(subTask, targetTask, index);
        if (updatedParent != null) {
            refreshAfterTaskMutation(updatedParent);
        }
    }

    private void moveSubTaskRelative(SubTask subTask, SubTask target, boolean insertAfter) {
        Task updatedParent = taskService.moveSubTaskRelative(subTask, target, insertAfter);
        if (updatedParent != null) {
            refreshAfterTaskMutation(updatedParent);
        }
    }

    private void normalizeTaskHierarchy() {
        taskService.normalizeTaskHierarchy(tasks, budgetItems);
    }

    private String nextTopLevelPosition() {
        return taskService.nextTopLevelPosition(tasks);
    }

    private List<Task> collectAllTasks(List<Task> source) {
        return taskService.collectAllTasks(source);
    }

    private void editDependencyFromGantt(Object targetTask, TaskDependency dependency) {
        if (targetTask == null || dependency == null) {
            return;
        }

        Optional<DependencyDialog.Result> dialogResult = DependencyDialog.showEdit(
                treeTableView == null || treeTableView.getScene() == null ? null : treeTableView.getScene().getWindow(),
                dependency.getPredecessorPosition(),
                dependencyEditingService.itemLabel(targetTask),
                dependency.getType(),
                dependency.getLagDays(),
                dependency.isCritical()
        );
        if (dialogResult.isEmpty()) {
            return;
        }

        dependencyEditingService.applyEdit(
                dependency,
                dialogResult.get().type(),
                dialogResult.get().lagDays(),
                dialogResult.get().critical()
        );

        if (isAutoScheduleEnabled()) {
            applyScheduleConstraints();
        }
        autoSaveProjectDocument();
        rebuildTree();
        updateVisibleItemsForGantt();
        updateDashboard();
        if (mainMenuBarController != null) {
            mainMenuBarController.markAsUnsaved();
        }
    }

    private TaskInteractionService.DropMode resolveDropMode(TreeTableRow<Object> row, javafx.scene.input.DragEvent event) {
        return taskInteractionService.resolveDropMode(
                draggedTask,
                draggedSubTask,
                row == null ? null : row.getItem(),
                row == null || row.isEmpty(),
                row == null ? 0.0 : row.getHeight(),
                event == null ? 0.0 : event.getY(),
                this::canMoveTaskToRoot,
                this::canMoveTaskIntoGroup,
                this::canReorderTaskRelative,
                this::canMoveSubTaskToTask
        );
    }

    private boolean handleDrop(TreeTableRow<Object> row, TaskInteractionService.DropMode dropMode) {
        if (dropMode == TaskInteractionService.DropMode.NONE) {
            return false;
        }
        if (draggedTask != null) {
            return handleTaskDrop(row, dropMode);
        }
        return handleSubTaskDrop(row, dropMode);
    }

    private boolean handleTaskDrop(TreeTableRow<Object> row, TaskInteractionService.DropMode dropMode) {
        Object target = row == null ? null : row.getItem();
        return dropExecutionService.handleTaskDrop(
                draggedTask,
                target,
                dropMode,
                this::moveTaskToGroup,
                this::moveTaskRelative,
                this::moveTaskToRoot
        );
    }

    private boolean handleSubTaskDrop(TreeTableRow<Object> row, TaskInteractionService.DropMode dropMode) {
        Object target = row == null ? null : row.getItem();
        return dropExecutionService.handleSubTaskDrop(
                draggedSubTask,
                target,
                dropMode,
                this::moveSubTaskRelative,
                this::moveSubTaskToTask
        );
    }

    private void applyDropStyle(TreeTableRow<Object> row, TaskInteractionService.DropMode dropMode) {
        clearDropStyle(row);
        switch (dropMode) {
            case INTO_GROUP, INTO_TASK -> ensureRowStyleClass(row, "drag-target-inside");
            case BEFORE -> ensureRowStyleClass(row, "drag-target-before");
            case AFTER -> ensureRowStyleClass(row, "drag-target-after");
            default -> {
            }
        }
    }

    private void clearDropStyle(TreeTableRow<Object> row) {
        if (row == null) {
            return;
        }
        row.getStyleClass().removeAll("drag-target-before", "drag-target-after", "drag-target-inside");
    }

    private void applyDragSourceStyle(TreeTableRow<Object> row) {
        ensureRowStyleClass(row, "drag-source-row");
    }

    private void clearDragSourceStyle(TreeTableRow<Object> row) {
        if (row == null) {
            return;
        }
        row.getStyleClass().remove("drag-source-row");
        row.setOpacity(1.0);
    }

    private void ensureRowStyleClass(TreeTableRow<Object> row, String styleClass) {
        if (row == null || styleClass == null || styleClass.isBlank()) {
            return;
        }
        if (!row.getStyleClass().contains(styleClass)) {
            row.getStyleClass().add(styleClass);
        }
    }

    private WritableImage createRowDragPreview(TreeTableRow<Object> row) {
        if (row == null || row.isEmpty()) {
            return null;
        }
        SnapshotParameters parameters = new SnapshotParameters();
        parameters.setFill(Color.TRANSPARENT);
        return row.snapshot(parameters, null);
    }

    private void handleAutoExpandDuringDrag(TreeTableRow<Object> row, TaskInteractionService.DropMode dropMode, PauseTransition expandDelay) {
        if (row == null || expandDelay == null) {
            return;
        }
        if (dropMode == TaskInteractionService.DropMode.INTO_GROUP && row.getTreeItem() != null && !row.getTreeItem().isExpanded()) {
            expandDelay.setOnFinished(evt -> {
                if (row.getTreeItem() != null) {
                    row.getTreeItem().setExpanded(true);
                    updateVisibleItemsForGantt();
                    updateGanttHeight();
                }
            });
            expandDelay.playFromStart();
            return;
        }
        expandDelay.stop();
    }

    private void autoScrollDuringDrag(double sceneY) {
        if (treeTableView == null || treeTableView.getScene() == null) {
            return;
        }
        ScrollBar verticalBar = findTreeVerticalScrollBar();
        if (verticalBar == null || !verticalBar.isVisible()) {
            return;
        }
        double localY = treeTableView.sceneToLocal(0, sceneY).getY();
        double threshold = 34.0;
        double step = 0.035;
        if (localY < threshold) {
            verticalBar.setValue(Math.max(verticalBar.getMin(), verticalBar.getValue() - step));
        } else if (localY > treeTableView.getHeight() - threshold) {
            verticalBar.setValue(Math.min(verticalBar.getMax(), verticalBar.getValue() + step));
        }
    }

    private ScrollBar findTreeVerticalScrollBar() {
        if (treeVerticalScrollBar != null) {
            return treeVerticalScrollBar;
        }
        if (treeTableView == null) {
            return null;
        }
        for (Node node : treeTableView.lookupAll(".scroll-bar")) {
            if (node instanceof ScrollBar scrollBar && scrollBar.getOrientation() == Orientation.VERTICAL) {
                treeVerticalScrollBar = scrollBar;
                break;
            }
        }
        return treeVerticalScrollBar;
    }

    private void showScheduleError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("\u041e\u0448\u0438\u0431\u043a\u0438 \u0437\u0430\u0432\u0438\u0441\u0438\u043c\u043e\u0441\u0442\u0435\u0439");
        alert.setHeaderText("\u041f\u0435\u0440\u0435\u0441\u0447\u0451\u0442 \u0440\u0430\u0441\u043f\u0438\u0441\u0430\u043d\u0438\u044f \u043d\u0435 \u0432\u044b\u043f\u043e\u043b\u043d\u0435\u043d");
        alert.setContentText(message);
        SceneManager.applyThemeToDialog(alert);
        alert.showAndWait();
    }

    private void updateDashboard() {
        if (totalTasksLabel == null || totalSubTasksLabel == null || completedLabel == null
                || overdueLabel == null || overallProgressBar == null || overallProgressLabel == null
                || riskSummaryLabel == null) {
            return;
        }

        TreeItem<Object> root = treeTableView.getRoot();
        if (root == null) {
            return;
        }

        DashboardService.DashboardSnapshot snapshot = dashboardService.buildSnapshot(root, budgetItems);

        totalTasksLabel.setText(String.valueOf(snapshot.taskCount()));
        totalSubTasksLabel.setText(String.valueOf(snapshot.subTaskCount()));
        completedLabel.setText(String.valueOf(snapshot.completedCount()));
        overdueLabel.setText(String.valueOf(snapshot.overdueCount()));
        overallProgressBar.setProgress(snapshot.normalizedProgress());
        overallProgressLabel.setText(dashboardService.formatOverallProgress(snapshot.progress()));

        riskSummaryLabel.setText(String.valueOf(snapshot.totalRisks()));
        riskSummaryLabel.setMinWidth(36.0);
        riskSummaryLabel.getStyleClass().removeAll("dashboard-risk-badge-risky", "dashboard-risk-badge-stable");
        if (snapshot.totalRisks() == 0) {
            ensureStyleClass(riskSummaryLabel, "dashboard-risk-badge-stable");
        } else if (snapshot.totalRisks() >= 6) {
            ensureStyleClass(riskSummaryLabel, "dashboard-risk-badge-risky");
        }
        Tooltip riskTooltip = new Tooltip(dashboardService.buildRiskTooltipText(snapshot));
        riskTooltip.setShowDelay(Duration.millis(450));
        riskTooltip.setHideDelay(Duration.seconds(5));
        riskSummaryLabel.setTooltip(riskTooltip);
    }

    private void ensureStyleClass(javafx.scene.control.Labeled control, String styleClass) {
        if (control == null || styleClass == null || styleClass.isBlank()) {
            return;
        }
        if (!control.getStyleClass().contains(styleClass)) {
            control.getStyleClass().add(styleClass);
        }
    }

    private String buildTaskSignalTooltip(Task task) {
        if (task == null) {
            return "";
        }
        List<String> lines = new ArrayList<>();
        if (task.isResourceOverloaded() && task.getResourceLoadSummary() != null && !task.getResourceLoadSummary().isBlank()) {
            lines.add("Ресурсы: " + task.getResourceLoadSummary());
        }
        if (task.isBudgetOverrun() && task.getBudgetSummary() != null && !task.getBudgetSummary().isBlank()) {
            lines.add(task.getBudgetSummary());
        }
        return String.join("\n", lines);
    }

    private void syncProjectMetadataToSessionSettings() {
        projectService.syncMetadataToSessionSettings(getProjectName(), getProjectAuthor());
    }

    private void autoSaveProjectDocument() {
        autosaveService.save(toProjectDocument());
    }

}
