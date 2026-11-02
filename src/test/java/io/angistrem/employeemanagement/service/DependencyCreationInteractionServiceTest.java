package io.angistrem.employeemanagement.service;

import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DependencyCreationInteractionServiceTest {

    private final DependencyCreationInteractionService service = new DependencyCreationInteractionService();

    @Test
    void sourceHighlightStyleShouldHighlightOnlyPendingSourceInDependencyMode() {
        String highlighted = service.sourceHighlightStyle(true, false, true);
        String normal = service.sourceHighlightStyle(true, true, true);
        assertTrue(highlighted.contains("dropshadow"));
        assertEquals("", normal);
    }

    @Test
    void clickShouldSelectItemOutsideDependencyMode() {
        Region node = new Region();
        Object item = new Object();
        AtomicReference<Object> selected = new AtomicReference<>();
        AtomicInteger refreshCount = new AtomicInteger();

        service.installItemClickHandler(
                node,
                item,
                false,
                () -> null,
                value -> { },
                selected::set,
                (source, target) -> { },
                refreshCount::incrementAndGet
        );

        node.getOnMouseClicked().handle(clickEvent());

        assertEquals(item, selected.get());
        assertEquals(0, refreshCount.get());
    }

    @Test
    void clickShouldSetPendingSourceWhenDependencyModeAndPendingIsEmpty() {
        Region node = new Region();
        Object item = new Object();
        AtomicReference<Object> pending = new AtomicReference<>();
        AtomicInteger refreshCount = new AtomicInteger();

        service.installItemClickHandler(
                node,
                item,
                true,
                pending::get,
                pending::set,
                selected -> { },
                (source, target) -> { },
                refreshCount::incrementAndGet
        );

        MouseEvent event = clickEvent();
        node.getOnMouseClicked().handle(event);

        assertEquals(item, pending.get());
        assertEquals(1, refreshCount.get());
        assertTrue(event.isConsumed());
    }

    @Test
    void clickShouldCreateDependencyAndClearPendingSource() {
        Region node = new Region();
        Object source = new Object();
        Object target = new Object();
        AtomicReference<Object> pending = new AtomicReference<>(source);
        AtomicReference<Object> createdSource = new AtomicReference<>();
        AtomicReference<Object> createdTarget = new AtomicReference<>();
        AtomicInteger refreshCount = new AtomicInteger();

        service.installItemClickHandler(
                node,
                target,
                true,
                pending::get,
                pending::set,
                selected -> { },
                (createdFrom, createdTo) -> {
                    createdSource.set(createdFrom);
                    createdTarget.set(createdTo);
                },
                refreshCount::incrementAndGet
        );

        MouseEvent event = clickEvent();
        node.getOnMouseClicked().handle(event);

        assertEquals(source, createdSource.get());
        assertEquals(target, createdTarget.get());
        assertEquals(null, pending.get());
        assertEquals(1, refreshCount.get());
        assertTrue(event.isConsumed());
    }

    private MouseEvent clickEvent() {
        return new MouseEvent(
                MouseEvent.MOUSE_CLICKED,
                10.0, 10.0, 10.0, 10.0,
                MouseButton.PRIMARY,
                1,
                false, false, false, false,
                true, false, false,
                true, false, false,
                null
        );
    }
}
