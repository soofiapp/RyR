/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/javafx/FXMLController.java to edit this template
 */
package com.ryrcontrolcenter.controller;

import com.ryrcontrolcenter.dao.DashboardDAO;
import com.ryrcontrolcenter.modelo.DashboardResumen;
import com.ryrcontrolcenter.util.SceneManager;
import com.ryrcontrolcenter.util.SesionActual;
import java.net.URL;
import java.util.Map;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;

public class DashboardController implements Initializable {

    // ===== Encabezado =====
    @FXML
    private Label lblRolUsuario;
    @FXML
    private Label lblNombreUsuario;

    // ===== Tarjetas =====
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

    // ===== Gráficas =====
    @FXML
    private BarChart<String, Number> chartBarras;
    @FXML
    private LineChart<String, Number> chartLinea;

    private final DashboardDAO dao = new DashboardDAO();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        cargarDatos();
    }

    public void setUsuario(String rol, String nombre) {
        lblRolUsuario.setText(rol);
        lblNombreUsuario.setText(nombre);
    }

    public void cargarDatos() {
        DashboardResumen r = dao.obtenerResumen();

        lblTotalMaquinas.setText(String.valueOf(r.getMaquinas()));
        lblTotalPrestamos.setText(String.valueOf(r.getPrestamos()));
        lblTotalHerramientas.setText(String.valueOf(r.getHerramientas()));
        lblTotalEstados.setText(String.valueOf(r.getEstados()));
        lblOcupacion.setText(String.format("%.0f%%", r.getOcupacion()));
        lblTotalDevoluciones.setText(String.valueOf(r.getDevoluciones()));
        lblTotalFiltros.setText(String.valueOf(r.getFiltros()));
        lblTotalAprobados.setText(String.valueOf(r.getAprobados()));

        llenarGrafica(chartBarras, "Activos por estado", r.getActivosPorEstado());
        llenarGrafica(chartLinea, "Préstamos por mes", r.getPrestamosPorMes());
    }

    private void llenarGrafica(XYChart<String, Number> chart, String titulo, Map<String, Integer> datos) {
        chart.getData().clear();
        chart.setTitle(titulo);
        chart.setLegendVisible(false);
        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        datos.forEach((k, v) -> serie.getData().add(new XYChart.Data<>(k, v)));
        chart.getData().add(serie);
    }

    // ===== NAVEGACIÓN (usa SceneManager, como el resto del proyecto) =====
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
        SesionActual.cerrar();
        SceneManager.cambiarA("/com/ryrcontrolcenter/ui/LoginVista.fxml");
    }
}
