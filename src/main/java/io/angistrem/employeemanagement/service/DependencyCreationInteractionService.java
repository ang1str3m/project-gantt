package io.angistrem.employeemanagement.service;

import javafx.scene.Node;
import javafx.scene.input.MouseEvent;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class DependencyCreationInteractionService {

    public String sourceHighlightStyle(boolean dependencyCreationMode, boolean selected, boolean pendingSource) {
        if (dependencyCreationMode && pendingSource && !selected) {
            return "-fx-effect: dropshadow(gaussian, rgba(37,99,235,0.45), 12, 0.25, 0, 0);";
        }
        return "";
    }

    public void installItemClickHandler(Node node,
                                        Object item,
                                        boolean dependencyCreationMode,
                                        Supplier<Object> pendingSourceSupplier,
                                        Consumer<Object> pendingSourceSetter,
                                        Consumer<Object> selectHandler,
                                        BiConsumer<Object, Object> createDependencyHandler,
                                        Runnable refreshHandler) {
        if (node == null || item == null) {
            return;
        }
        node.setOnMouseClicked(event -> handleClick(
                event,
                item,
                dependencyCreationMode,
                pendingSourceSupplier,
                pendingSourceSetter,
                selectHandler,
                createDependencyHandler,
                refreshHandler
        ));
    }

    private void handleClick(MouseEvent event,
                             Object item,
                             boolean dependencyCreationMode,
                             Supplier<Object> pendingSourceSupplier,
                             Consumer<Object> pendingSourceSetter,
                             Consumer<Object> selectHandler,
                             BiConsumer<Object, Object> createDependencyHandler,
                             Runnable refreshHandler) {
        if (!dependencyCreationMode) {
            if (selectHandler != null) {
                selectHandler.accept(item);
            }
            return;
        }
        Object pendingSource = pendingSourceSupplier != null ? pendingSourceSupplier.get() : null;
        if (pendingSource == null) {
            if (pendingSourceSetter != null) {
                pendingSourceSetter.accept(item);
            }
            if (refreshHandler != null) {
                refreshHandler.run();
            }
            event.consume();
            return;
        }
        if (pendingSource == item) {
            if (pendingSourceSetter != null) {
                pendingSourceSetter.accept(null);
            }
            if (refreshHandler != null) {
                refreshHandler.run();
            }
            event.consume();
            return;
        }
        if (createDependencyHandler != null) {
            createDependencyHandler.accept(pendingSource, item);
        }
        if (pendingSourceSetter != null) {
            pendingSourceSetter.accept(null);
        }
        if (refreshHandler != null) {
            refreshHandler.run();
        }
        event.consume();
    }
}
