/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/javafx/FXMLController.java to edit this template
 */
package com.ryrcontrolcenter.controller;

import com.ryrcontrolcenter.service.AuthService;
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

/**
 * FXML Controller class
 *
 * @author Juanp
 */
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

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // TODO
    }

    @FXML
    private void irADashboard(ActionEvent event) {
    }

    @FXML
    private void irAPrestamos(ActionEvent event) {
        try {
            Stage stage = new Stage();
            Parent root = FXMLLoader.load(getClass().getResource("/com/ryrcontrolcenter/ui/PrestamosVista.fxml"));
            stage.setScene(new Scene(root));
            stage.setFullScreen(true);
            stage.show();
            Stage stageLogin = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stageLogin.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    @FXML
    private void irAInventariado(ActionEvent event) {
        try {
            Stage stage = new Stage();
            Parent root = FXMLLoader.load(getClass().getResource("/com/ryrcontrolcenter/ui/InventariadoVista.fxml"));
            stage.setScene(new Scene(root));
            stage.setFullScreen(true);
            stage.show();
            Stage stageLogin = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stageLogin.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    @FXML
    private void irAMatrizFiltro(ActionEvent event) {
        try {
            Stage stage = new Stage();
            Parent root = FXMLLoader.load(getClass().getResource("/com/ryrcontrolcenter/ui/MatrizFiltroVista.fxml"));
            stage.setScene(new Scene(root));
            stage.setFullScreen(true);
            stage.show();
            Stage stageLogin = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stageLogin.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    @FXML
    private void irAKardex(ActionEvent event) {
        try {
            Stage stage = new Stage();
            Parent root = FXMLLoader.load(getClass().getResource("/com/ryrcontrolcenter/ui/KardexVista.fxml"));
            stage.setScene(new Scene(root));
            stage.show();
            stage.setFullScreen(true);
            Stage stageLogin = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stageLogin.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    @FXML
    private void irAUsuariosBitacora(ActionEvent event) {
        try {
            Stage stage = new Stage();
            Parent root = FXMLLoader.load(getClass().getResource("/com/ryrcontrolcenter/ui/UsuariosBitacoraVista.fxml"));
            stage.setScene(new Scene(root));
            stage.show();
            stage.setFullScreen(true);
            Stage stageLogin = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stageLogin.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    @FXML
    private void onLogoutClick(ActionEvent event) {
        try {
            Stage stage = new Stage();
            Parent root = FXMLLoader.load(getClass().getResource("/com/ryrcontrolcenter/ui/LoginVista.fxml"));
            stage.setFullScreen(true);
            stage.setScene(new Scene(root));
            stage.show();
            Stage stageLogin = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stageLogin.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}
