/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/javafx/FXMLController.java to edit this template
 */
package com.ryrcontrolcenter.controller;

import com.ryrcontrolcenter.modelo.Inventario;
import com.ryrcontrolcenter.service.InventarioService;
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
    
    InventarioService service;
    
    public NuevoInventarioController(){
        service = new InventarioService();
    }

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
        // 1. Extraer los textos y valores seleccionados
        String idActivo = txtIdActivo.getText().trim();
        String descripcion = txtDescripcion.getText().trim();
        //String tipoActivo = cmbTipoActivo.getValue(); 
        //String estadoSst = cmbEstadoSst.getValue();   
        //String ubicacion = cmbUbicacion.getValue();   

        // El DatePicker retorna un LocalDate, lo convertimos a String o null si está vacío
        //LocalDate fecha = dpFechaRegistro.getValue();
        String fechaRegistro = ("fecha"); 

        String observaciones = txtObservaciones.getText().trim();

        // 2. Validar campos obligatorios (según restricciones NOT NULL de la base de datos)
        
        // 3. Crear el objeto Inventario con la información extraída
        Inventario nuevoInventario = new Inventario();
        nuevoInventario.setIdActivo(idActivo);
        nuevoInventario.setDescripcion(descripcion);
        nuevoInventario.setTipoActivo("tipoActivo");
        nuevoInventario.setEstadoSst("estadoSst"); // Valor por defecto en DB[cite: 1]
        nuevoInventario.setUbicacion("ubicacion");
        nuevoInventario.setFechaRegistro(fechaRegistro);
        nuevoInventario.setObservaciones(observaciones);

       if (service.agregarItemInventarioService(nuevoInventario)){
           System.out.println("Se ha agregado correctamente ");   
       } else {
           System.out.println("Hubo un error ");   
       }
}
    
}
