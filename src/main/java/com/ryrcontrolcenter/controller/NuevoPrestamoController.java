package com.ryrcontrolcenter.controller;

import com.ryrcontrolcenter.dao.PrestamoDao;
import com.ryrcontrolcenter.modelo.Prestamos;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.stage.Stage;

public class NuevoPrestamoController implements Initializable {

    @FXML private RadioButton rbMaquinaria;
    @FXML private RadioButton rbHerramientaUnica;
    @FXML private RadioButton rbKitCompleto;
    @FXML private ToggleGroup grupoTipoItem;

    @FXML private TextField txtOperario;
    @FXML private ComboBox<String> cmbActivoDisponible;
    @FXML private DatePicker dpFechaSalida;
    @FXML private DatePicker dpDevolucionEstimada;
    @FXML private TextField txtUbicacion;
    @FXML private TextArea txtObservaciones;
    @FXML private Label lblError;

    @FXML private Button btnCancelar;
    @FXML private Button btnAutorizarSalida;

    private final PrestamoDao prestamoDao = new PrestamoDao();
    private Prestamos prestamoEdicion = null;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        try {
            if (dpFechaSalida != null) {
                dpFechaSalida.setValue(LocalDate.now());
            }

            if (cmbActivoDisponible != null) {
                cmbActivoDisponible.setEditable(false);
            }

            if (grupoTipoItem != null) {
                grupoTipoItem.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
                    actualizarOpcionesCombo();
                });
            }

            actualizarOpcionesCombo();

        } catch (Exception e) {
            System.err.println("Error en initialize(): " + e.getMessage());
        }
    }

    private void actualizarOpcionesCombo() {
        if (cmbActivoDisponible == null) return;

        cmbActivoDisponible.getItems().clear();

        // NOTA: Asegúrate de usar códigos de activos (IDs) que EXISTAN reales en tu tabla de inventario
        if (rbMaquinaria != null && rbMaquinaria.isSelected()) {
            cmbActivoDisponible.getItems().addAll(
                "MAQ-0001 - Mini Cargador Bobcat",
                "MAQ-0002 - Retroexcavadora CAT"
            );
        } else if (rbKitCompleto != null && rbKitCompleto.isSelected()) {
            cmbActivoDisponible.getItems().addAll(
                "KIT-0001 - Kit Herramientas Manuales",
                "KIT-0002 - Kit Soldadura Eléctrica"
            );
        } else {
            cmbActivoDisponible.getItems().addAll(
                "HERR-0089 - Taladro Percutor DeWalt",
                "HERR-0090 - Pulidora Industrial Bosch"
            );
        }

        if (!cmbActivoDisponible.getItems().isEmpty()) {
            cmbActivoDisponible.getSelectionModel().selectFirst();
        }
    }

    public void setPrestamoParaEditar(Prestamos prestamo) {
        this.prestamoEdicion = prestamo;
        if (prestamo != null) {
            if (txtOperario != null) txtOperario.setText(prestamo.getOperarioNombre());
            if (txtUbicacion != null) txtUbicacion.setText(prestamo.getUbicacionFrente());
            if (txtObservaciones != null) txtObservaciones.setText(prestamo.getObservacionesSalida());
            if (cmbActivoDisponible != null) cmbActivoDisponible.setValue(prestamo.getIdActivo());
            
            if (dpFechaSalida != null && prestamo.getFechaSalida() != null && !prestamo.getFechaSalida().isEmpty()) {
                try {
                    dpFechaSalida.setValue(LocalDate.parse(prestamo.getFechaSalida(), dateFormatter));
                } catch (Exception ignored) {}
            }
            if (dpDevolucionEstimada != null && prestamo.getFechaDevolucionEstimada() != null && !prestamo.getFechaDevolucionEstimada().isEmpty()) {
                try {
                    dpDevolucionEstimada.setValue(LocalDate.parse(prestamo.getFechaDevolucionEstimada(), dateFormatter));
                } catch (Exception ignored) {}
            }
        }
    }

    @FXML
    private void onAutorizarSalidaClick(ActionEvent event) {
        System.out.println(">>> CLIC DETECTADO EN AUTORIZAR SALIDA <<<");

        if (txtOperario == null || txtOperario.getText().trim().isEmpty()) {
            mostrarAlerta("Campo requerido", "Por favor ingrese el nombre del operario.", Alert.AlertType.WARNING);
            return;
        }

        String fechaSalida = (dpFechaSalida != null && dpFechaSalida.getValue() != null) 
                ? dpFechaSalida.getValue().format(dateFormatter) 
                : LocalDate.now().format(dateFormatter);

        String fechaDevolucion = (dpDevolucionEstimada != null && dpDevolucionEstimada.getValue() != null) 
                ? dpDevolucionEstimada.getValue().format(dateFormatter) 
                : "";

        String idActivoLimpio = "HERR-0089";
        if (cmbActivoDisponible != null && cmbActivoDisponible.getValue() != null) {
            String valorSeleccionado = cmbActivoDisponible.getValue();
            if (valorSeleccionado.contains(" - ")) {
                idActivoLimpio = valorSeleccionado.split(" - ")[0].trim();
            } else {
                idActivoLimpio = valorSeleccionado.trim();
            }
        }

        if (prestamoEdicion == null) {
            Prestamos nuevo = new Prestamos();
            nuevo.setIdPrestamo("PR-" + LocalDate.now().getYear() + "-" + String.format("%03d", (int)(Math.random() * 900 + 1)));
            nuevo.setOperarioNombre(txtOperario.getText().trim());
            nuevo.setOperarioCedula("1098765432");
            nuevo.setUbicacionFrente(txtUbicacion != null ? txtUbicacion.getText().trim() : "SEDE PRINCIPAL");
            nuevo.setFechaSalida(fechaSalida);
            nuevo.setFechaDevolucionEstimada(fechaDevolucion);
            nuevo.setObservacionesSalida(txtObservaciones != null ? txtObservaciones.getText().trim() : "");
            nuevo.setIdActivo(idActivoLimpio);
            nuevo.setEstado("En Uso"); 
            nuevo.setDescripcionEstadoDevolucion("PENDIENTE");
            nuevo.setIdUusarioRegistro(1); // Debe coincidir con un ID de usuario registrado (ej. 1)

            boolean exito = prestamoDao.guardar(nuevo);

            if (exito) {
                mostrarAlerta("Éxito", "El préstamo se ha registrado correctamente.", Alert.AlertType.INFORMATION);
                cerrarVentana(event);
            } else {
                mostrarAlerta("Error al Guardar", "El código de activo (" + idActivoLimpio + ") o el usuario no existe en la base de datos.", Alert.AlertType.ERROR);
            }
        } else {
            prestamoEdicion.setOperarioNombre(txtOperario.getText().trim());
            prestamoEdicion.setUbicacionFrente(txtUbicacion != null ? txtUbicacion.getText().trim() : "");
            prestamoEdicion.setFechaSalida(fechaSalida);
            prestamoEdicion.setFechaDevolucionEstimada(fechaDevolucion);
            prestamoEdicion.setObservacionesSalida(txtObservaciones != null ? txtObservaciones.getText().trim() : "");
            prestamoEdicion.setIdActivo(idActivoLimpio);
            
            if (prestamoEdicion.getEstado() == null || prestamoEdicion.getEstado().trim().isEmpty()) {
                prestamoEdicion.setEstado("En Uso");
            }

            boolean exito = prestamoDao.actualizar(prestamoEdicion);

            if (exito) {
                mostrarAlerta("Éxito", "El préstamo se ha actualizado correctamente.", Alert.AlertType.INFORMATION);
                cerrarVentana(event);
            } else {
                mostrarAlerta("Error", "No se pudo actualizar el préstamo.", Alert.AlertType.ERROR);
            }
        }
    }

    @FXML
    private void onCancelarClick(ActionEvent event) {
        System.out.println(">>> CLIC DETECTADO EN CANCELAR <<<");
        cerrarVentana(event);
    }

    private void cerrarVentana(ActionEvent event) {
        Node source = (Node) event.getSource();
        Stage stage = (Stage) source.getScene().getWindow();
        stage.close();
    }

    private void mostrarAlerta(String titulo, String mensaje, Alert.AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}