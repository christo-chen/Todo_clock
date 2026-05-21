package com.example.todoflipclock.service;

import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class CountdownServiceTest {

    @BeforeAll
    static void initToolkit() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException e) {
            // Already initialized
        }
    }

    @Test
    void testSetDuration() {
        CountdownService cs = new CountdownService();
        cs.setDuration(5);
        assertEquals("05:00", cs.remainingTimeProperty().get());
    }

    @Test
    void testSetDurationDifferentValues() {
        CountdownService cs = new CountdownService();
        cs.setDuration(1);
        assertEquals("01:00", cs.remainingTimeProperty().get());
        cs.setDuration(60);
        assertEquals("60:00", cs.remainingTimeProperty().get());
    }

    @Test
    void testMinimumDurationIsOneMinute() {
        CountdownService cs = new CountdownService();
        cs.setDuration(0);
        assertEquals("01:00", cs.remainingTimeProperty().get());
    }

    @Test
    void testRemainingTimeFormat() {
        CountdownService cs = new CountdownService();
        cs.setDuration(25);
        String time = cs.remainingTimeProperty().get();
        assertTrue(time.matches("\\d{2}:\\d{2}"));
        assertEquals(5, time.length());
    }

    @Test
    void testNotRunningInitially() {
        CountdownService cs = new CountdownService();
        assertFalse(cs.isRunning());
        assertFalse(cs.isRunningProperty().get());
    }

    @Test
    void testStartAndIsRunning() throws Exception {
        CountdownService cs = new CountdownService();
        cs.setDuration(60);
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            cs.start();
            assertTrue(cs.isRunning());
            cs.pause();
            latch.countDown();
        });

        assertTrue(latch.await(2, TimeUnit.SECONDS));
    }

    @Test
    void testPauseStopsRunning() throws Exception {
        CountdownService cs = new CountdownService();
        cs.setDuration(60);
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            cs.start();
            cs.pause();
            latch.countDown();
        });

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertFalse(cs.isRunning());
    }

    @Test
    void testResetRestoresTime() throws Exception {
        CountdownService cs = new CountdownService();
        cs.setDuration(10);
        assertEquals("10:00", cs.remainingTimeProperty().get());
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            cs.setDuration(10);
            cs.start();
            // Let it run briefly then stop
            try { Thread.sleep(200); } catch (InterruptedException ignored) {}
            cs.pause();
            cs.reset();
            latch.countDown();
        });

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertEquals("10:00", cs.remainingTimeProperty().get());
        assertFalse(cs.isRunning());
    }

    @Test
    void testStopClears() throws Exception {
        CountdownService cs = new CountdownService();
        cs.setDuration(30);
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            cs.stop();
            latch.countDown();
        });

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertEquals("00:00", cs.remainingTimeProperty().get());
        assertFalse(cs.isRunning());
        assertEquals("", cs.taskNameProperty().get());
    }

    @Test
    void testTaskNameProperty() {
        CountdownService cs = new CountdownService();
        assertEquals("", cs.taskNameProperty().get());

        cs.taskNameProperty().set("Write report");
        assertEquals("Write report", cs.taskNameProperty().get());

        cs.taskNameProperty().set("");
        assertEquals("", cs.taskNameProperty().get());
    }

    @Test
    void testCallbackFiresOnCompletion() throws Exception {
        CountdownService cs = new CountdownService();
        cs.setDuration(1);
        AtomicBoolean callbackFired = new AtomicBoolean(false);
        CountDownLatch latch = new CountDownLatch(1);
        cs.setOnFinished(() -> {
            callbackFired.set(true);
            latch.countDown();
        });

        Platform.runLater(() -> {
            cs.setRemainingSeconds(1);
            cs.start();
        });

        assertTrue(latch.await(3, TimeUnit.SECONDS), "Callback should have fired");
        assertTrue(callbackFired.get());
        assertFalse(cs.isRunning());
        assertEquals("00:00", cs.remainingTimeProperty().get());
    }

    @Test
    void testSetDurationWhileRunningDoesNothing() throws Exception {
        CountdownService cs = new CountdownService();
        cs.setDuration(5);
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            cs.start();
            cs.setDuration(60); // Should be ignored
            cs.pause();
            latch.countDown();
        });

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        // Duration stays at 5 min (display "05:00" since we didn't run for long)
        assertTrue(cs.remainingTimeProperty().get().startsWith("05:"));
    }

    @Test
    void testSetRemainingSeconds() {
        CountdownService cs = new CountdownService();
        cs.setDuration(10);
        assertEquals("10:00", cs.remainingTimeProperty().get());

        cs.setRemainingSeconds(30);
        assertEquals("00:30", cs.remainingTimeProperty().get());

        cs.setRemainingSeconds(90);
        assertEquals("01:30", cs.remainingTimeProperty().get());
    }
}
