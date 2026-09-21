/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/javafx/FXMLController.java to edit this template
 */
package com.ryrcontrolcenter.controller;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

/**
 * FXML Controller class
 *
 * @author Juanp
 */
public class NuevoInventarioController implements Initializable {

    @FXML
    private TextField txtIdActivo;
    @FXML
    private TextField txtDescripcion;
    @FXML
    private ComboBox<?> cmbTipoActivo;
    @FXML
    private ComboBox<?> cmbEstadoSst;
    @FXML
    private ComboBox<?> cmbUbicacion;
    @FXML
    private DatePicker dpFechaRegistro;
    @FXML
    private TextArea txtObservaciones;
    @FXML
    private Button btnCancelar;
    @FXML
    private Button btnGuardar;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // TODO
    }    

    @FXML
    private void onCancelarClick(ActionEvent event) {
    }

    @FXML
    private void onGuardarClick(ActionEvent event) {
    }
    
}
