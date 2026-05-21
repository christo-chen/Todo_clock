package com.example.todoflipclock.service;

import com.example.todoflipclock.model.TodoItem;
import com.example.todoflipclock.model.TodoItem.Priority;
import com.example.todoflipclock.storage.TodoStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TodoServiceTest {

    @TempDir
    Path tempDir;

    private TodoStorage storage;
    private TodoService service;

    @BeforeEach
    void setUp() {
        storage = new TodoStorage(tempDir.resolve("tasks.txt"));
        service = new TodoService(storage);
    }

    // -----------------------------------------------------------------------
    // addTask
    // -----------------------------------------------------------------------

    @Test
    void testAddNormalTask() {
        service.addTask("Hello", Priority.NONE, null);
        assertEquals(1, service.getItems().size());
        assertEquals("Hello", service.getItems().get(0).getText());
    }

    @Test
    void testAddTaskWithPriorityAndDueDate() {
        LocalDate due = LocalDate.of(2026, 12, 31);
        service.addTask("Important", Priority.HIGH, due);

        TodoItem item = service.getItems().get(0);
        assertEquals("Important", item.getText());
        assertEquals(Priority.HIGH, item.getPriority());
        assertEquals(due, item.getDueDate());
    }

    @Test
    void testAddBlankTaskIgnored() {
        service.addTask("   ", Priority.NONE, null);
        assertTrue(service.getItems().isEmpty());
    }

    @Test
    void testAddEmptyStringIgnored() {
        service.addTask("", Priority.MEDIUM, LocalDate.now());
        assertTrue(service.getItems().isEmpty());
    }

    @Test
    void testAddMultipleTasks() {
        service.addTask("One", Priority.NONE, null);
        service.addTask("Two", Priority.HIGH, null);
        service.addTask("Three", Priority.LOW, null);

        assertEquals(3, service.getItems().size());
        assertEquals("One", service.getItems().get(0).getText());
        assertEquals("Three", service.getItems().get(2).getText());
    }

    // -----------------------------------------------------------------------
    // deleteTask
    // -----------------------------------------------------------------------

    @Test
    void testDeleteTask() {
        service.addTask("A", Priority.NONE, null);
        service.addTask("B", Priority.NONE, null);
        service.addTask("C", Priority.NONE, null);

        TodoItem toRemove = service.getItems().get(1);
        service.deleteTask(toRemove);

        assertEquals(2, service.getItems().size());
        assertEquals("A", service.getItems().get(0).getText());
        assertEquals("C", service.getItems().get(1).getText());
    }

    @Test
    void testDeleteOnlyTask() {
        service.addTask("Only", Priority.NONE, null);
        service.deleteTask(service.getItems().get(0));
        assertTrue(service.getItems().isEmpty());
    }

    // -----------------------------------------------------------------------
    // toggleCompleted
    // -----------------------------------------------------------------------

    @Test
    void testToggleCompleted() {
        service.addTask("Task", Priority.NONE, null);
        TodoItem item = service.getItems().get(0);
        assertFalse(item.isCompleted());

        service.toggleCompleted(item);
        assertTrue(item.isCompleted());

        service.toggleCompleted(item);
        assertFalse(item.isCompleted());
    }

    // -----------------------------------------------------------------------
    // moveTask
    // -----------------------------------------------------------------------

    @Test
    void testMoveTaskForward() {
        service.addTask("A", Priority.NONE, null);
        service.addTask("B", Priority.NONE, null);
        service.addTask("C", Priority.NONE, null);

        service.moveTask(0, 2);
        assertEquals("B", service.getItems().get(0).getText());
        assertEquals("C", service.getItems().get(1).getText());
        assertEquals("A", service.getItems().get(2).getText());
    }

    @Test
    void testMoveTaskBackward() {
        service.addTask("A", Priority.NONE, null);
        service.addTask("B", Priority.NONE, null);
        service.addTask("C", Priority.NONE, null);

        service.moveTask(2, 0);
        assertEquals("C", service.getItems().get(0).getText());
        assertEquals("A", service.getItems().get(1).getText());
        assertEquals("B", service.getItems().get(2).getText());
    }

    @Test
    void testMoveTaskOutOfBoundsIgnored() {
        service.addTask("A", Priority.NONE, null);
        service.addTask("B", Priority.NONE, null);

        service.moveTask(0, -1);
        assertEquals(2, service.getItems().size());
        assertEquals("A", service.getItems().get(0).getText());

        service.moveTask(0, 999);
        assertEquals("A", service.getItems().get(0).getText());

        service.moveTask(-1, 0);
        assertEquals("A", service.getItems().get(0).getText());
    }

    // -----------------------------------------------------------------------
    // Auto-save on list changes (verified via file contents)
    // -----------------------------------------------------------------------

    @Test
    void testAutoSaveOnAdd() throws Exception {
        service.addTask("Persisted", Priority.HIGH, LocalDate.of(2026, 6, 1));

        Path file = tempDir.resolve("tasks.txt");
        assertTrue(Files.exists(file), "File should exist after add");

        List<String> lines = Files.readAllLines(file);
        assertEquals(1, lines.size());
        // Verify format: completed|priority|dueDate|Base64
        String[] parts = lines.get(0).split("\\|", -1);
        assertEquals("false", parts[0]);
        assertEquals("HIGH", parts[1]);
        assertEquals("2026-06-01", parts[2]);
        String decoded = new String(Base64.getDecoder().decode(parts[3]));
        assertEquals("Persisted", decoded);
    }

    @Test
    void testAutoSaveOnDelete() throws Exception {
        service.addTask("Keep", Priority.NONE, null);
        service.addTask("Remove", Priority.NONE, null);

        service.deleteTask(service.getItems().get(1));

        Path file = tempDir.resolve("tasks.txt");
        List<String> lines = Files.readAllLines(file);
        assertEquals(1, lines.size());
    }

    @Test
    void testAutoSaveOnToggleCompleted() throws Exception {
        service.addTask("Task", Priority.NONE, null);
        TodoItem item = service.getItems().get(0);

        service.toggleCompleted(item);

        Path file = tempDir.resolve("tasks.txt");
        List<String> lines = Files.readAllLines(file);
        assertTrue(lines.get(0).startsWith("true|"));
    }

    @Test
    void testAutoSaveOnMove() throws Exception {
        service.addTask("First", Priority.NONE, null);
        service.addTask("Second", Priority.NONE, null);

        service.moveTask(0, 1);

        Path file = tempDir.resolve("tasks.txt");
        List<String> lines = Files.readAllLines(file);
        assertEquals(2, lines.size());
    }

    @Test
    void testExplicitSave() throws Exception {
        service.addTask("Task", Priority.NONE, null);
        Path file = tempDir.resolve("tasks.txt");

        // File should already exist from auto-save on add
        assertTrue(Files.exists(file));
        service.save();
        assertTrue(Files.exists(file));
    }

    // -----------------------------------------------------------------------
    // getItems returns live observable list
    // -----------------------------------------------------------------------

    @Test
    void testGetItemsReturnsLiveList() {
        service.addTask("A", Priority.NONE, null);
        var items = service.getItems();
        items.add(new TodoItem("Manual", false));
        assertEquals(2, service.getItems().size());
    }
}
