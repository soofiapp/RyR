package com.ryrcontrolcenter;

import com.ryrcontrolcenter.config.InicializadorBD;
import com.ryrcontrolcenter.service.BackupScheduler;
import com.ryrcontrolcenter.util.SceneManager;
import java.sql.SQLException;
import javafx.application.Application;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        try {
            InicializadorBD.inicializar();
        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }
        stage.setTitle("RyR ControlCenter");
        SceneManager.setStage(stage);
        SceneManager.cambiarA(SceneManager.PRELOGIN);
        BackupScheduler.iniciar();
    }

    @Override
    public void stop() {
        BackupScheduler.alCerrar();
        BackupScheduler.detener();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
