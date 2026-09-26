package com.ryrcontrolcenter.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class SceneManager {

    private static Stage stagePrincipal;

    public static void setStage(Stage stage) {
        stagePrincipal = stage;
    }

    public static void cambiarA(String rutaFxml) {
        try {
            Parent root = FXMLLoader.load(SceneManager.class.getResource(rutaFxml));

            Scene escenaActual = stagePrincipal.getScene();
            if (escenaActual == null) {
                stagePrincipal.setScene(new Scene(root));
            } else {
                // Reutiliza la misma Scene, solo cambia el contenido (root)
                escenaActual.setRoot(root);
            }
            stagePrincipal.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
