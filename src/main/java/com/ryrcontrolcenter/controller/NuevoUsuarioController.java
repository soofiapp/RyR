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
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/**
 * FXML Controller class
 *
 * @author Juanp
 */
public class NuevoUsuarioController implements Initializable {

    @FXML
    private TextField txtNombreCompleto;
    @FXML
    private TextField txtUsuarioLogin;
    @FXML
    private PasswordField txtPassword;
    @FXML
    private ComboBox<?> cmbRol;
    @FXML
    private ComboBox<?> cmbFrentePlanta;
    @FXML
    private CheckBox chkActivo;
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
