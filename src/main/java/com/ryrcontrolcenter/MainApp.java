package com.ryrcontrolcenter;

import com.ryrcontrolcenter.util.SceneManager;
import javafx.application.Application;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        SceneManager.setStage(stage);
        stage.setTitle("RyR ControlCenter");
        SceneManager.cambiarA("/com/ryrcontrolcenter/ui/LoginVista.fxml");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
