package com.example.todoflipclock.service;

import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class ClockServiceTest {

    @BeforeAll
    static void initToolkit() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException e) {
            // Already initialized
        }
    }

    @Test
    void testCurrentTimePropertyFormat() {
        ClockService cs = new ClockService();
        String time = cs.currentTimeProperty().get();
        assertNotNull(time);
        assertTrue(time.matches("\\d{2}:\\d{2}:\\d{2}"),
                "Expected HH:mm:ss but got: " + time);
    }

    @Test
    void testCurrentTimePropertyHasCorrectLength() {
        ClockService cs = new ClockService();
        assertEquals(8, cs.currentTimeProperty().get().length());
    }

    @Test
    void testCurrentDatePropertyFormat() {
        ClockService cs = new ClockService();
        String date = cs.currentDateProperty().get();
        assertNotNull(date);
        assertTrue(date.matches("\\d{4}年\\d{2}月\\d{2}日 .+"),
                "Expected 'yyyy年MM月dd日 EEEE' but got: " + date);
    }

    @Test
    void testCurrentDatePropertyContainsWeekdayName() {
        ClockService cs = new ClockService();
        String date = cs.currentDateProperty().get();
        // Date contains at least one CJK weekday character
        assertTrue(date.contains("星期"));
    }

    @Test
    void testStartStopDoesNotThrow() throws Exception {
        ClockService cs = new ClockService();
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            cs.start();
            cs.stop();
            latch.countDown();
        });

        assertTrue(latch.await(2, TimeUnit.SECONDS));
    }

    @Test
    void testTimeUpdatesWhileRunning() throws Exception {
        ClockService cs = new ClockService();
        String before = cs.currentTimeProperty().get();

        CountDownLatch started = new CountDownLatch(1);
        Platform.runLater(() -> {
            cs.start();
            started.countDown();
        });
        assertTrue(started.await(2, TimeUnit.SECONDS));

        // Wait on the test thread so the FX thread can fire Timeline ticks
        Thread.sleep(2000);

        CountDownLatch stopped = new CountDownLatch(1);
        Platform.runLater(() -> {
            cs.stop();
            stopped.countDown();
        });
        assertTrue(stopped.await(2, TimeUnit.SECONDS));

        String after = cs.currentTimeProperty().get();
        assertNotEquals(before, after, "Time should have changed after running");
    }
}
