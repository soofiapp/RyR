
package com.ryrcontrolcenter.util;

import javafx.scene.control.Alert;


public class AlertaUtil {

    public static void mostrar(String titulo, String mensaje, Alert.AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    public static boolean confirmar(String titulo, String mensaje) {
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle(titulo);
        confirmacion.setHeaderText(null);
        confirmacion.setContentText(mensaje);
        return confirmacion.showAndWait()
                .filter(resp -> resp == javafx.scene.control.ButtonType.OK)
                .isPresent();
    }
}
