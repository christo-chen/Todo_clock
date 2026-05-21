package com.example.todoflipclock.service;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.util.Duration;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class ClockService {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy年MM月dd日 EEEE");

    private final ReadOnlyStringWrapper currentTime = new ReadOnlyStringWrapper();
    private final ReadOnlyStringWrapper currentDate = new ReadOnlyStringWrapper();
    private final Timeline timeline;

    public ClockService() {
        updateTime();
        updateDate();
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            updateTime();
            updateDate();
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
    }

    public void start() {
        timeline.play();
    }

    public void stop() {
        timeline.stop();
    }

    public ReadOnlyStringProperty currentTimeProperty() {
        return currentTime.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty currentDateProperty() {
        return currentDate.getReadOnlyProperty();
    }

    private void updateTime() {
        currentTime.set(LocalTime.now().format(TIME_FORMAT));
    }

    private void updateDate() {
        currentDate.set(LocalDate.now().format(DATE_FORMAT));
    }
}
