package com.ryrcontrolcenter;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {
    
    @Override
    public void start(Stage stage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/com/ryrcontrolcenter/ui/LoginVista.fxml"));
        stage.setScene(new Scene(root, 900, 600));
        stage.setTitle("RyR ControlCenter");
        stage.show();
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
