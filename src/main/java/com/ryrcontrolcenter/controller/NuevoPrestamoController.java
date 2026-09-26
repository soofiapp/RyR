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
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;

/**
 * FXML Controller class
 *
 * @author Juanp
 */
public class NuevoPrestamoController implements Initializable {

    @FXML
    private ToggleGroup grupoTipoItem;
    @FXML
    private RadioButton rbHerramientaUnica;
    @FXML
    private RadioButton rbKitCompleto;
    @FXML
    private TextField txtOperario;
    @FXML
    private ComboBox<?> cmbActivoDisponible;
    @FXML
    private DatePicker dpDevolucionEstimada;
    @FXML
    private TextArea txtObservaciones;
    @FXML
    private Button btnCancelar;
    @FXML
    private Button btnAutorizarSalida;
    @FXML
    private RadioButton rbMaquinaria;
    @FXML
    private DatePicker dpFechaSalida;
    @FXML
    private TextField txtUbicacion;

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
    private void onAutorizarSalidaClick(ActionEvent event) {
    }
    
}
