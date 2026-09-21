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
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;

/**
 * FXML Controller class
 *
 * @author Juanp
 */
public class NuevoMovimientoController implements Initializable {

    @FXML
    private ToggleGroup grupoTipoMovimiento;
    @FXML
    private RadioButton rbEntrada;
    @FXML
    private RadioButton rbSalida;
    @FXML
    private RadioButton rbAjuste;
    @FXML
    private ComboBox<?> cmbFiltro;
    @FXML
    private Label lblStockActual;
    @FXML
    private TextField txtCantidad;
    @FXML
    private Label lblError;
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
