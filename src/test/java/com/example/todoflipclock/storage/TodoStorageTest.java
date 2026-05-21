package com.example.todoflipclock.storage;

import com.example.todoflipclock.model.TodoItem;
import com.example.todoflipclock.model.TodoItem.Priority;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TodoStorageTest {

    @TempDir
    Path tempDir;

    // -----------------------------------------------------------------------
    // Save + load round-trip
    // -----------------------------------------------------------------------

    @Test
    void testSaveAndLoad() throws Exception {
        Path file = tempDir.resolve("tasks.txt");
        TodoStorage storage = new TodoStorage(file);

        List<TodoItem> input = List.of(
                new TodoItem("Task 1", false, Priority.HIGH, LocalDate.of(2026, 6, 1)),
                new TodoItem("Task 2", true, Priority.LOW, null));

        storage.save(input);
        assertTrue(Files.exists(file));

        List<TodoItem> loaded = storage.load();
        assertEquals(2, loaded.size());

        assertEquals("Task 1", loaded.get(0).getText());
        assertFalse(loaded.get(0).isCompleted());
        assertEquals(Priority.HIGH, loaded.get(0).getPriority());
        assertEquals(LocalDate.of(2026, 6, 1), loaded.get(0).getDueDate());

        assertEquals("Task 2", loaded.get(1).getText());
        assertTrue(loaded.get(1).isCompleted());
        assertEquals(Priority.LOW, loaded.get(1).getPriority());
        assertNull(loaded.get(1).getDueDate());
    }

    // -----------------------------------------------------------------------
    // Empty
    // -----------------------------------------------------------------------

    @Test
    void testLoadEmptyFileReturnsEmptyList() {
        Path file = tempDir.resolve("empty.txt");
        TodoStorage storage = new TodoStorage(file);

        List<TodoItem> loaded = storage.load();
        assertNotNull(loaded);
        assertTrue(loaded.isEmpty());
    }

    @Test
    void testLoadNonexistentFileReturnsEmptyList() {
        TodoStorage storage = new TodoStorage(tempDir.resolve("nonexistent.txt"));
        List<TodoItem> loaded = storage.load();
        assertNotNull(loaded);
        assertTrue(loaded.isEmpty());
    }

    @Test
    void testSaveEmptyList() throws Exception {
        Path file = tempDir.resolve("tasks.txt");
        TodoStorage storage = new TodoStorage(file);
        storage.save(List.of());

        assertTrue(Files.exists(file));
        List<String> lines = Files.readAllLines(file);
        assertTrue(lines.isEmpty());
    }

    // -----------------------------------------------------------------------
    // Special characters
    // -----------------------------------------------------------------------

    @Test
    void testChineseCharacters() {
        Path file = tempDir.resolve("tasks.txt");
        TodoStorage storage = new TodoStorage(file);

        List<TodoItem> input = List.of(
                new TodoItem("完成报告", false),
                new TodoItem("检查邮件 📧", false));

        storage.save(input);
        List<TodoItem> loaded = storage.load();

        assertEquals(2, loaded.size());
        assertEquals("完成报告", loaded.get(0).getText());
        assertEquals("检查邮件 📧", loaded.get(1).getText());
    }

    @Test
    void testEmojiInTaskText() {
        Path file = tempDir.resolve("tasks.txt");
        TodoStorage storage = new TodoStorage(file);

        List<TodoItem> input = List.of(
                new TodoItem("🎉 Party prep 🎂", false),
                new TodoItem("😀😃😄😁", true));

        storage.save(input);
        List<TodoItem> loaded = storage.load();

        assertEquals(2, loaded.size());
        assertEquals("🎉 Party prep 🎂", loaded.get(0).getText());
        assertTrue(loaded.get(1).isCompleted());
        assertEquals("😀😃😄😁", loaded.get(1).getText());
    }

    @Test
    void testNewlinesAreNotInTaskTextButAreEncoded() throws Exception {
        Path file = tempDir.resolve("tasks.txt");
        TodoStorage storage = new TodoStorage(file);

        // Task text itself has no newlines; we just verify file has correct line count
        List<TodoItem> input = List.of(
                new TodoItem("Line one", false),
                new TodoItem("Line two", true),
                new TodoItem("Line three", false));

        storage.save(input);
        List<String> lines = Files.readAllLines(file);

        assertEquals(3, lines.size());
        List<TodoItem> loaded = storage.load();
        assertEquals(3, loaded.size());
    }

    @Test
    void testPipeCharacterInText() {
        Path file = tempDir.resolve("tasks.txt");
        TodoStorage storage = new TodoStorage(file);

        List<TodoItem> input = List.of(new TodoItem("a|b|c", false));
        storage.save(input);
        List<TodoItem> loaded = storage.load();

        assertEquals(1, loaded.size());
        assertEquals("a|b|c", loaded.get(0).getText());
    }

    // -----------------------------------------------------------------------
    // Corrupt data
    // -----------------------------------------------------------------------

    @Test
    void testCorruptLineSkipped() throws Exception {
        Path file = tempDir.resolve("tasks.txt");
        // Write a valid line, a bad line, and another valid line
        Files.writeString(file, """
                false|NONE|none|VGFzayAx
                this line has no pipes
                true|HIGH|2026-12-25|VGFzayAy
                """);

        TodoStorage storage = new TodoStorage(file);
        List<TodoItem> loaded = storage.load();

        assertEquals(2, loaded.size());
        assertEquals("Task 1", loaded.get(0).getText());
        assertEquals("Task 2", loaded.get(1).getText());
    }

    @Test
    void testInvalidBase64Skipped() throws Exception {
        Path file = tempDir.resolve("tasks.txt");
        Files.writeString(file, "false|NONE|none|!!!not-base64!!!\n");

        TodoStorage storage = new TodoStorage(file);
        List<TodoItem> loaded = storage.load();

        assertTrue(loaded.isEmpty());
    }

    @Test
    void testEmptyLineSkipped() throws Exception {
        Path file = tempDir.resolve("tasks.txt");
        Files.writeString(file, "\nfalse|NONE|none|VGFzaw==\n\n");

        TodoStorage storage = new TodoStorage(file);
        List<TodoItem> loaded = storage.load();

        assertEquals(1, loaded.size());
        assertEquals("Task", loaded.get(0).getText());
    }

    // -----------------------------------------------------------------------
    // Backward compatibility — old 2-field format
    // -----------------------------------------------------------------------

    @Test
    void testOldFormatCompatibility() throws Exception {
        Path file = tempDir.resolve("tasks.txt");
        // Old format: completed|Base64(text)
        // "false|QnV5IG1pbGs="  →  "Buy milk"
        // "true|RG9uZQ=="       →  "Done"
        Files.writeString(file, """
                false|QnV5IG1pbGs=
                true|RG9uZQ==
                """);

        TodoStorage storage = new TodoStorage(file);
        List<TodoItem> loaded = storage.load();

        assertEquals(2, loaded.size());

        assertEquals("Buy milk", loaded.get(0).getText());
        assertFalse(loaded.get(0).isCompleted());
        assertEquals(Priority.NONE, loaded.get(0).getPriority());
        assertNull(loaded.get(0).getDueDate());

        assertEquals("Done", loaded.get(1).getText());
        assertTrue(loaded.get(1).isCompleted());
        assertEquals(Priority.NONE, loaded.get(1).getPriority());
        assertNull(loaded.get(1).getDueDate());
    }

    @Test
    void testMixedOldAndNewFormat() throws Exception {
        Path file = tempDir.resolve("tasks.txt");
        Files.writeString(file, """
                false|QnV5IG1pbGs=
                true|HIGH|2026-06-01|UmVwb3J0
                false|NONE|none|VGFzaw==
                """);

        TodoStorage storage = new TodoStorage(file);
        List<TodoItem> loaded = storage.load();

        assertEquals(3, loaded.size());
        // Old format line
        assertEquals("Buy milk", loaded.get(0).getText());
        assertEquals(Priority.NONE, loaded.get(0).getPriority());
        // New format with date
        assertEquals("Report", loaded.get(1).getText());
        assertEquals(Priority.HIGH, loaded.get(1).getPriority());
        assertEquals(LocalDate.of(2026, 6, 1), loaded.get(1).getDueDate());
        // New format without date
        assertEquals("Task", loaded.get(2).getText());
        assertEquals(Priority.NONE, loaded.get(2).getPriority());
    }

    @Test
    void testRoundTripPreservesAllFields() {
        Path file = tempDir.resolve("tasks.txt");
        TodoStorage storage = new TodoStorage(file);

        List<TodoItem> input = new ArrayList<>();
        input.add(new TodoItem("Alpha", false, Priority.MEDIUM, LocalDate.of(2026, 3, 15)));
        input.add(new TodoItem("Beta", true, Priority.NONE, null));
        input.add(new TodoItem("Gamma with | pipe", false, Priority.HIGH, LocalDate.of(2026, 9, 1)));

        storage.save(input);
        List<TodoItem> loaded = storage.load();

        assertEquals(3, loaded.size());
        for (int i = 0; i < input.size(); i++) {
            assertEquals(input.get(i).getText(), loaded.get(i).getText());
            assertEquals(input.get(i).isCompleted(), loaded.get(i).isCompleted());
            assertEquals(input.get(i).getPriority(), loaded.get(i).getPriority());
            assertEquals(input.get(i).getDueDate(), loaded.get(i).getDueDate());
        }
    }
}
