package com.ryrcontrolcenter;

import com.ryrcontrolcenter.util.SceneManager;
import javafx.application.Application;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        stage.setTitle("RyR ControlCenter");
        SceneManager.setStage(stage);
        SceneManager.cambiarA(SceneManager.PRELOGIN);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
