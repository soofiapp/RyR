package com.ryrcontrolcenter.controller;

import com.ryrcontrolcenter.dao.DashboardDao;
import com.ryrcontrolcenter.modelo.DashboardResumen;
import com.ryrcontrolcenter.modelo.Filtros;
import com.ryrcontrolcenter.reportes.ReporteDashboard;
import com.ryrcontrolcenter.util.AlertaUtil;
import com.ryrcontrolcenter.util.ExportacionUtil;
import com.ryrcontrolcenter.util.SceneManager;
import com.ryrcontrolcenter.util.SesionActual;
import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;


public class DashboardController implements Initializable {

    private static final String[] PERIODOS = {"Últimos 3 meses", "Últimos 6 meses", "Últimos 12 meses"};
    private static final int[] MESES = {3, 6, 12};

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
    private Label lblSemRojo;
    @FXML
    private Label lblSemAmarillo;
    @FXML
    private Label lblSemVerde;
    @FXML
    private ComboBox<String> cmbPeriodo;
    @FXML
    private PieChart chartPie;
    @FXML
    private BarChart<String, Number> chartBarras;
    @FXML
    private LineChart<String, Number> chartLinea;
    @FXML
    private PieChart chartSemaforo;
    @FXML
    private PieChart chartPrestamos;
    @FXML
    private BarChart<String, Number> chartMeses;

    private final DashboardDao dao = new DashboardDao();
    private DashboardResumen resumen;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        var usuario = SesionActual.getUsuario();
        if (usuario != null) {
            lblRolUsuario.setText(usuario.getRol());
            lblNombreUsuario.setText(usuario.getNombreCompleto());
        }
        chartPie.setAnimated(false);
        chartBarras.setAnimated(false);
        chartLinea.setAnimated(false);
        chartSemaforo.setAnimated(false);
        chartPrestamos.setAnimated(false);
        chartMeses.setAnimated(false);
        chartBarras.setLegendVisible(true);

        cmbPeriodo.setItems(FXCollections.observableArrayList(PERIODOS));
        cmbPeriodo.getSelectionModel().select(1); 
        cmbPeriodo.valueProperty().addListener((o, a, b) -> cargar());
        cargar();
    }

    private int mesesSeleccionados() {
        int i = Math.max(0, cmbPeriodo.getSelectionModel().getSelectedIndex());
        return MESES[i];
    }

    private void cargar() {
        resumen = dao.cargar(mesesSeleccionados());
        DashboardResumen r = resumen;

        lblTotalMaquinas.setText(String.valueOf(r.maquinas));
        lblTotalHerramientas.setText(String.valueOf(r.herramientas));
        lblTotalPrestamos.setText(String.valueOf(r.prestamosActivos));
        lblTotalDevoluciones.setText(String.valueOf(r.devoluciones));
        lblOcupacion.setText(r.ocupacionPorcentaje() + "%");
        lblTotalFiltros.setText(String.valueOf(r.filtros));
        lblTotalAprobados.setText(String.valueOf(r.aprobados));
        lblTotalEstados.setText(String.valueOf(r.bloqueadosMantenimiento));
        lblSemRojo.setText("Rojo: " + r.semRojo);
        lblSemAmarillo.setText("Amarillo: " + r.semAmarillo);
        lblSemVerde.setText("Verde: " + r.semVerde);

        pintarPie(r);
        pintarBarras(r);
        pintarLinea(r);
        pintarSemaforoYPrestamos(r);
        pintarMeses(r);
    }

    private void pintarPie(DashboardResumen r) {
        chartPie.setData(FXCollections.observableArrayList(
                new PieChart.Data("En bodega (" + r.enBodega() + ")", r.enBodega()),
                new PieChart.Data("Prestados (" + r.prestados + ")", r.prestados)));
    }

    private void pintarBarras(DashboardResumen r) {
        XYChart.Series<String, Number> stock = new XYChart.Series<>();
        stock.setName("Stock actual");
        XYChart.Series<String, Number> reorden = new XYChart.Series<>();
        reorden.setName("Punto de reorden");
        int n = Math.min(8, r.filtrosPorCriticidad.size());
        for (int i = 0; i < n; i++) {
            Filtros f = r.filtrosPorCriticidad.get(i);
            XYChart.Data<String, Number> ds = new XYChart.Data<>(f.getIdFiltro(), f.getStockActual());
            XYChart.Data<String, Number> dr = new XYChart.Data<>(f.getIdFiltro(), f.getPuntoReorden());
            valorSobreBarra(ds);
            valorSobreBarra(dr);
            tooltipAlCrear(ds, f.getDescripcion() + "\nStock: " + f.getStockActual());
            tooltipAlCrear(dr, f.getDescripcion() + "\nPunto de reorden: " + f.getPuntoReorden());
            stock.getData().add(ds);
            reorden.getData().add(dr);
        }
        chartBarras.getData().clear();
        chartBarras.getData().add(stock);
        chartBarras.getData().add(reorden);
    }

    private void pintarLinea(DashboardResumen r) {
        XYChart.Series<String, Number> entradas = new XYChart.Series<>();
        entradas.setName("Entradas (uds)");
        XYChart.Series<String, Number> salidas = new XYChart.Series<>();
        salidas.setName("Salidas (uds)");
        XYChart.Series<String, Number> prestamos = new XYChart.Series<>();
        prestamos.setName("Préstamos");
        for (int i = 0; i < r.meses.size(); i++) {
            String m = ReporteDashboard.etiquetaMes(r.meses.get(i));
            entradas.getData().add(new XYChart.Data<>(m, r.entradasMes.get(i)));
            salidas.getData().add(new XYChart.Data<>(m, r.salidasMes.get(i)));
            prestamos.getData().add(new XYChart.Data<>(m, r.prestamosMes.get(i)));
        }
        chartLinea.getData().clear();
        chartLinea.getData().add(entradas);
        chartLinea.getData().add(salidas);
        chartLinea.getData().add(prestamos);
    }

    private void pintarSemaforoYPrestamos(DashboardResumen r) {
        chartSemaforo.setData(FXCollections.observableArrayList(
                new PieChart.Data("Rojo - reponer (" + r.semRojo + ")", r.semRojo),
                new PieChart.Data("Amarillo - pedir pronto (" + r.semAmarillo + ")", r.semAmarillo),
                new PieChart.Data("Verde - OK (" + r.semVerde + ")", r.semVerde)));
        int enUso = Math.max(0, r.prestamosActivos - r.prestamosAtrasados);
        chartPrestamos.setData(FXCollections.observableArrayList(
                new PieChart.Data("En uso (" + enUso + ")", enUso),
                new PieChart.Data("Atrasados (" + r.prestamosAtrasados + ")", r.prestamosAtrasados),
                new PieChart.Data("Devueltos (" + r.devoluciones + ")", r.devoluciones)));
    }

    private void pintarMeses(DashboardResumen r) {
        XYChart.Series<String, Number> entradas = new XYChart.Series<>();
        entradas.setName("Entradas");
        XYChart.Series<String, Number> salidas = new XYChart.Series<>();
        salidas.setName("Salidas");
        for (int i = 0; i < r.meses.size(); i++) {
            String m = ReporteDashboard.etiquetaMes(r.meses.get(i));
            XYChart.Data<String, Number> de = new XYChart.Data<>(m, r.entradasMes.get(i));
            XYChart.Data<String, Number> ds = new XYChart.Data<>(m, r.salidasMes.get(i));
            valorSobreBarra(de);
            valorSobreBarra(ds);
            entradas.getData().add(de);
            salidas.getData().add(ds);
        }
        chartMeses.getData().clear();
        chartMeses.getData().add(entradas);
        chartMeses.getData().add(salidas);
    }

    
    private void valorSobreBarra(XYChart.Data<String, Number> d) {
        d.nodeProperty().addListener((o, a, nodo) -> {
            if (nodo instanceof javafx.scene.layout.StackPane sp && d.getYValue().doubleValue() > 0) {
                javafx.scene.text.Text l = new javafx.scene.text.Text(String.valueOf(d.getYValue().intValue()));
                l.getStyleClass().add("chart-bar-valor");
                l.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-fill: #111827;");
                sp.getChildren().add(l);
                javafx.scene.layout.StackPane.setAlignment(l, javafx.geometry.Pos.TOP_CENTER);
                l.setTranslateY(-15);
            }
        });
    }


    private void tooltipAlCrear(XYChart.Data<String, Number> d, String texto) {
        d.nodeProperty().addListener((o, a, nodo) -> {
            if (nodo != null) {
                Tooltip.install(nodo, new Tooltip(texto));
            }
        });
    }



    @FXML
    private void onExportarPdfClick(ActionEvent event) {
        exportar(true);
    }

    @FXML
    private void onExportarExcelClick(ActionEvent event) {
        exportar(false);
    }

    private void exportar(boolean pdf) {
        cargar();
        File destino = ExportacionUtil.elegirArchivo(chartPie.getScene().getWindow(), "Dashboard_RyR", pdf);
        if (destino == null) {
            return;
        }
        try {
            if (pdf) {
                ReporteDashboard.pdf(destino, resumen, ExportacionUtil.usuarioActual(), mesesSeleccionados());
            } else {
                ReporteDashboard.excel(destino, resumen, ExportacionUtil.usuarioActual(), mesesSeleccionados());
            }
            AlertaUtil.mostrar("Exportación exitosa", "El archivo se guardó en:\n" + destino.getAbsolutePath(), Alert.AlertType.INFORMATION);
        } catch (Exception e) {
            e.printStackTrace();
            AlertaUtil.mostrar("Error al exportar",
                    "No se pudo crear el archivo. Verifique que no esté abierto en otro programa.\n" + e.getMessage(), Alert.AlertType.ERROR);
        }
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
        SesionActual.cerrar();
        SceneManager.cambiarA("/com/ryrcontrolcenter/ui/LoginVista.fxml");
    }
}
