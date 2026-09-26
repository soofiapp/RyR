/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/javafx/FXMLController.java to edit this template
 */
package com.ryrcontrolcenter.controller;

import com.ryrcontrolcenter.util.SceneManager;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class DashboardController implements Initializable {

    @FXML
    private Button btnDashboard;
    @FXML
    private Button btnPrestamos;
    @FXML
    private Button btnInventariado;
    @FXML
    private Button btnMatrizFiltro;
    @FXML
    private Button btnKardex;
    @FXML
    private Button btnUsuarios;
    @FXML
    private Button btnLogout;
    @FXML
    private Label lblRolUsuario;
    @FXML
    private Label lblNombreUsuario;
    @FXML
    private Label lblTotalMaquinas;
    @FXML
    private Label lblTotalPrestamos;
    @FXML
    private Label lblTotalHerramientas;
    @FXML
    private Label lblTotalEstados;
    @FXML
    private Label lblOcupacion;
    @FXML
    private Label lblTotalDevoluciones;
    @FXML
    private Label lblTotalFiltros;
    @FXML
    private Label lblTotalAprobados;
    @FXML
    private BarChart<?, ?> chartBarras;
    @FXML
    private NumberAxis ejeYBarras;
    @FXML
    private CategoryAxis ejeXBarras;
    @FXML
    private LineChart<?, ?> chartLinea;
    @FXML
    private NumberAxis ejeYLinea;
    @FXML
    private CategoryAxis ejeXLinea;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // TODO
    }

    @FXML
    private void irADashboard(ActionEvent event) {
        SceneManager.cambiarA("/com/ryrcontrolcenter/ui/DashboardVista.fxml");
    }

    @FXML
    private void irAPrestamos(ActionEvent event) {
        SceneManager.cambiarA("/com/ryrcontrolcenter/ui/PrestamosVista.fxml");
    }

    @FXML
    private void irAInventariado(ActionEvent event) {
        SceneManager.cambiarA("/com/ryrcontrolcenter/ui/InventariadoVista.fxml");
    }

    @FXML
    private void irAMatrizFiltro(ActionEvent event) {
        SceneManager.cambiarA("/com/ryrcontrolcenter/ui/MatrizFiltroVista.fxml");
    }

    @FXML
    private void irAKardex(ActionEvent event) {
        SceneManager.cambiarA("/com/ryrcontrolcenter/ui/KardexVista.fxml");
    }

    @FXML
    private void irAUsuariosBitacora(ActionEvent event) {
        SceneManager.cambiarA("/com/ryrcontrolcenter/ui/UsuariosBitacoraVista.fxml");
    }

    @FXML
    private void onLogoutClick(ActionEvent event) {
        SceneManager.cambiarA("/com/ryrcontrolcenter/ui/LoginVista.fxml");
    }
}
