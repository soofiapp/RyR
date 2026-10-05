/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/javafx/FXMLController.java to edit this template
 */
package com.ryrcontrolcenter.controller;

import com.ryrcontrolcenter.modelo.Inventario;
import com.ryrcontrolcenter.service.InventarioService;
import java.net.URL;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

/**
 * FXML Controller class
 *
 * @author Juanp
 */
public class NuevoInventarioController implements Initializable {
   
    private static final List<String> TIPOS_VALIDOS =
            Arrays.asList("Herramienta", "Maquinaria", "Kit Agrupado");
    private static final List<String> ESTADOS_VALIDOS =
            Arrays.asList("Operativa", "Bloqueada", "Mantenimiento", "Completo");
    
    
    
    @FXML private TextField txtIdActivo;
    @FXML private TextField txtDescripcion;
    @FXML private TextField txtTipoActivo;
    @FXML private TextField txtEstadoSst;
    @FXML private TextField txtUbicacion;
    @FXML private DatePicker dpFechaRegistro;
    @FXML private TextArea txtObservaciones;
    @FXML private Button btnCancelar;
    @FXML private Button btnGuardar;
    @FXML private Label lblTitulo;
    
    private final InventarioService service = new InventarioService();

    private boolean modoEdicion = false;
    private String idOriginal;
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        dpFechaRegistro.setValue(LocalDate.now());
    }   

    
        /** Llama a este método antes de mostrar la ventana para entrar en modo modificar. */
    public void setInventarioAEditar(Inventario item) {
        modoEdicion = true;
        idOriginal = item.getIdActivo();

        lblTitulo.setText("MODIFICAR ACTIVO");
        btnGuardar.setText("Actualizar");

        txtIdActivo.setText(item.getIdActivo());
        txtIdActivo.setDisable(true); // el código es la clave, no se cambia
        txtDescripcion.setText(item.getDescripcion());
        txtTipoActivo.setText(item.getTipoActivo());
        txtEstadoSst.setText(item.getEstadoSst());
        txtUbicacion.setText(item.getUbicacion());
        txtObservaciones.setText(item.getObservaciones() == null ? "" : item.getObservaciones());

        try {
            String f = item.getFechaRegistro();
            if (f != null && f.length() >= 10) {
                dpFechaRegistro.setValue(LocalDate.parse(f.substring(0, 10)));
            }
        } catch (Exception e) {
            dpFechaRegistro.setValue(LocalDate.now());
        }
    }
    
    @FXML
    private void onCancelarClick(ActionEvent event) {
        cerrarVentana();
    }

   
    @FXML
    private void onGuardarClick(ActionEvent event) {
        String idActivo = txtIdActivo.getText().trim();
        String descripcion = txtDescripcion.getText().trim();
        String tipoIngresado = txtTipoActivo.getText().trim();
        String estadoIngresado = txtEstadoSst.getText().trim();
        String ubicacion = txtUbicacion.getText().trim();
        String observaciones = txtObservaciones.getText().trim();
        LocalDate fecha = dpFechaRegistro.getValue();

        if (idActivo.isEmpty() || descripcion.isEmpty() || tipoIngresado.isEmpty()
                || estadoIngresado.isEmpty() || ubicacion.isEmpty()) {
            mostrarAlerta("Campos incompletos",
                    "Código, descripción, tipo, estado y ubicación son obligatorios.");
            return;
        }

        String tipoActivo = buscarValido(tipoIngresado, TIPOS_VALIDOS);
        if (tipoActivo == null) {
            mostrarAlerta("Tipo no válido",
                    "El tipo debe ser uno de: " + String.join(", ", TIPOS_VALIDOS));
            return;
        }

        String estadoSst = buscarValido(estadoIngresado, ESTADOS_VALIDOS);
        if (estadoSst == null) {
            mostrarAlerta("Estado no válido",
                    "El estado debe ser uno de: " + String.join(", ", ESTADOS_VALIDOS));
            return;
        }

        if (fecha == null) {
            mostrarAlerta("Fecha requerida", "Selecciona la fecha de registro.");
            return;
        }

        Inventario item = new Inventario();
        item.setIdActivo(idActivo);
        item.setDescripcion(descripcion);
        item.setTipoActivo(tipoActivo);
        item.setEstadoSst(estadoSst);
        item.setUbicacion(ubicacion);
        item.setFechaRegistro(fecha.toString());
        item.setObservaciones(observaciones);

        boolean ok = modoEdicion
                ? service.actualizarItemInventarioService(idOriginal, item)
                : service.agregarItemInventarioService(item);

        if (ok) {
            cerrarVentana();
        } else {
            mostrarAlerta("Error", modoEdicion
                    ? "No se pudo actualizar el activo."
                    : "No se pudo guardar. Revisa si el código ya existe.");
        }
    }

    private String buscarValido(String texto, List<String> validos) {
        for (String v : validos) {
            if (v.equalsIgnoreCase(texto)) return v;
        }
        return null;
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle(titulo);
        a.setHeaderText(null);
        a.setContentText(mensaje);
        a.showAndWait();
    }

    private void cerrarVentana() {
        ((Stage) btnCancelar.getScene().getWindow()).close();
    }
}