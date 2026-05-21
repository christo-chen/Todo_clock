package com.example.todoflipclock;

import com.example.todoflipclock.controller.MainController;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) {
        MainController controller = new MainController();
        Scene scene = new Scene(controller.getView(), 760, 480);

        stage.setTitle("Todo Flip Clock");
        stage.setMinWidth(680);
        stage.setMinHeight(420);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
