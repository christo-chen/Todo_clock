package com.example.todoflipclock;

import com.example.todoflipclock.controller.MainController;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class MainApp extends Application {

    private static final Path CONFIG_DIR =
            Path.of(System.getProperty("user.home"), ".todo-flip-clock");
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("config.properties");

    private double stageX = -1, stageY = -1, stageW = 760, stageH = 480;
    private boolean darkTheme;

    @Override
    public void start(Stage stage) {
        loadConfig();

        MainController controller = new MainController(darkTheme);
        Scene scene = controller.createScene();

        stage.setTitle("Todo Flip Clock");
        stage.setMinWidth(680);
        stage.setMinHeight(420);
        stage.setScene(scene);

        // Restore window position / size
        if (stageX >= 0 && stageY >= 0) {
            stage.setX(stageX);
            stage.setY(stageY);
        }
        stage.setWidth(stageW);
        stage.setHeight(stageH);

        stage.setOnCloseRequest(e -> saveConfig(stage, controller));
        stage.show();
    }

    private void loadConfig() {
        if (!Files.exists(CONFIG_FILE)) return;
        try (InputStream in = Files.newInputStream(CONFIG_FILE)) {
            Properties p = new Properties();
            p.load(in);
            darkTheme = "dark".equals(p.getProperty("theme"));
            stageX = Double.parseDouble(p.getProperty("window.x", "-1"));
            stageY = Double.parseDouble(p.getProperty("window.y", "-1"));
            stageW = Double.parseDouble(p.getProperty("window.w", "760"));
            stageH = Double.parseDouble(p.getProperty("window.h", "480"));
        } catch (IOException | NumberFormatException e) {
            // Use defaults
        }
    }

    private void saveConfig(Stage stage, MainController controller) {
        try {
            Files.createDirectories(CONFIG_DIR);
            Properties p = new Properties();
            p.setProperty("theme", controller.isDarkTheme() ? "dark" : "light");
            p.setProperty("window.x", String.valueOf((int) stage.getX()));
            p.setProperty("window.y", String.valueOf((int) stage.getY()));
            p.setProperty("window.w", String.valueOf((int) stage.getWidth()));
            p.setProperty("window.h", String.valueOf((int) stage.getHeight()));
            try (OutputStream out = Files.newOutputStream(CONFIG_FILE)) {
                p.store(out, "Todo Flip Clock config");
            }
        } catch (IOException e) {
            System.err.println("Could not save config: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
