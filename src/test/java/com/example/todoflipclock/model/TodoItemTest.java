package com.example.todoflipclock.model;

import com.example.todoflipclock.model.TodoItem.Priority;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class TodoItemTest {

    @Test
    void testTwoArgConstructorDefaults() {
        TodoItem item = new TodoItem("Buy milk", false);
        assertEquals("Buy milk", item.getText());
        assertFalse(item.isCompleted());
        assertEquals(Priority.NONE, item.getPriority());
        assertNull(item.getDueDate());
    }

    @Test
    void testFourArgConstructor() {
        LocalDate due = LocalDate.of(2026, 6, 15);
        TodoItem item = new TodoItem("Report", true, Priority.HIGH, due);

        assertEquals("Report", item.getText());
        assertTrue(item.isCompleted());
        assertEquals(Priority.HIGH, item.getPriority());
        assertEquals(due, item.getDueDate());
    }

    @Test
    void testFourArgConstructorNullDueDate() {
        TodoItem item = new TodoItem("Task", false, Priority.LOW, null);
        assertEquals(Priority.LOW, item.getPriority());
        assertNull(item.getDueDate());
    }

    @Test
    void testTextPropertyGetSet() {
        TodoItem item = new TodoItem("A", false);
        assertEquals("A", item.getText());
        item.textProperty().set("B");
        assertEquals("B", item.getText());

        StringProperty prop = item.textProperty();
        assertNotNull(prop);
        assertEquals("B", prop.get());
    }

    @Test
    void testCompletedPropertyGetSet() {
        TodoItem item = new TodoItem("X", false);
        assertFalse(item.isCompleted());
        item.setCompleted(true);
        assertTrue(item.isCompleted());

        BooleanProperty prop = item.completedProperty();
        assertNotNull(prop);
        assertTrue(prop.get());
    }

    @Test
    void testPriorityPropertyGetSet() {
        TodoItem item = new TodoItem("X", false);
        assertEquals(Priority.NONE, item.getPriority());

        item.setPriority(Priority.HIGH);
        assertEquals(Priority.HIGH, item.getPriority());

        ObjectProperty<Priority> prop = item.priorityProperty();
        assertNotNull(prop);
        assertEquals(Priority.HIGH, prop.get());
    }

    @Test
    void testDueDatePropertyGetSet() {
        TodoItem item = new TodoItem("X", false);
        assertNull(item.getDueDate());

        LocalDate date = LocalDate.of(2026, 12, 25);
        item.setDueDate(date);
        assertEquals(date, item.getDueDate());

        ObjectProperty<LocalDate> prop = item.dueDateProperty();
        assertNotNull(prop);
        assertEquals(date, prop.get());
        item.setDueDate(null);
        assertNull(item.getDueDate());
    }

    @Test
    void testPriorityAllEnumValues() {
        for (Priority p : Priority.values()) {
            TodoItem item = new TodoItem("t", false, p, null);
            assertEquals(p, item.getPriority());
        }
    }

    @Test
    void testTextPropertyBindingFires() {
        TodoItem item = new TodoItem("old", false);
        AtomicReference<String> captured = new AtomicReference<>();
        item.textProperty().addListener((obs, oldVal, newVal) -> captured.set(newVal));

        item.textProperty().set("new");
        assertEquals("new", captured.get());
    }

    @Test
    void testCompletedPropertyBindingFires() {
        TodoItem item = new TodoItem("x", false);
        AtomicBoolean captured = new AtomicBoolean(false);
        item.completedProperty().addListener((obs, oldVal, newVal) -> captured.set(newVal));

        item.setCompleted(true);
        assertTrue(captured.get());
    }

    @Test
    void testPriorityPropertyBindingFires() {
        TodoItem item = new TodoItem("x", false);
        AtomicReference<Priority> captured = new AtomicReference<>();
        item.priorityProperty().addListener((obs, oldVal, newVal) -> captured.set(newVal));

        item.setPriority(Priority.MEDIUM);
        assertEquals(Priority.MEDIUM, captured.get());
    }

    @Test
    void testDueDatePropertyBindingFires() {
        TodoItem item = new TodoItem("x", false);
        LocalDate d = LocalDate.of(2026, 8, 1);
        AtomicReference<LocalDate> captured = new AtomicReference<>();
        item.dueDateProperty().addListener((obs, oldVal, newVal) -> captured.set(newVal));

        item.setDueDate(d);
        assertEquals(d, captured.get());
    }
}
