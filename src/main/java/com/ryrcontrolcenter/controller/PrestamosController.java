package com.ryrcontrolcenter.controller;

import com.ryrcontrolcenter.dao.PrestamoDao;
import com.ryrcontrolcenter.modelo.Prestamos;
import com.ryrcontrolcenter.service.BitacoraService;
import com.ryrcontrolcenter.util.AlertaUtil;
import com.ryrcontrolcenter.util.SceneManager;
import com.ryrcontrolcenter.util.SesionActual;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class PrestamosController implements Initializable {

    @FXML
    private BorderPane mainPane;
    @FXML
    private Label lblTotalPrestamos;
    @FXML
    private ComboBox<String> cmbFiltro;
    @FXML
    private TextField txtBuscar;
    @FXML
    private Button btnEliminarPrestamo;
    @FXML
    private Button btnModificarPrestamo;
    @FXML
    private Button btnAnadirPrestamo;
    @FXML
    private TableView<Prestamos> tablaPrestamos;
    @FXML
    private TableColumn<Prestamos, String> colIdPrestamo;
    @FXML
    private TableColumn<Prestamos, String> colInventario;
    @FXML
    private TableColumn<Prestamos, String> colOperario;
    @FXML
    private TableColumn<Prestamos, String> colSalida;
    @FXML
    private TableColumn<Prestamos, String> colEstado;
    @FXML
    private TableColumn<Prestamos, String> colDevolucion;

    private final PrestamoDao prestamoDao = new PrestamoDao();
    private ObservableList<Prestamos> listaPrestamos = FXCollections.observableArrayList();
    private FilteredList<Prestamos> filtroPrestamos;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (!SesionActual.puedeEditar()) {
            btnModificarPrestamo.setVisible(false);
            btnEliminarPrestamo.setVisible(false);
            btnAnadirPrestamo.setVisible(false);
        }
        configurarTabla();
        configurarFiltros();
        cargarDatosBD();
    }

    private void configurarTabla() {
        colIdPrestamo.setCellValueFactory(new PropertyValueFactory<>("idPrestamo"));
        colInventario.setCellValueFactory(new PropertyValueFactory<>("idActivo"));
        colOperario.setCellValueFactory(new PropertyValueFactory<>("operarioNombre"));
        colSalida.setCellValueFactory(new PropertyValueFactory<>("fechaSalida"));
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));
        colDevolucion.setCellValueFactory(new PropertyValueFactory<>("fechaDevolucionEstimada"));
    }

    private void configurarFiltros() {
        cmbFiltro.setItems(FXCollections.observableArrayList("Todos", "Máquina", "Herramienta", "Kit"));
        cmbFiltro.setValue("Todos");
    }

    public void cargarDatosBD() {
        try {
            listaPrestamos.clear();
            List<Prestamos> prestamos = prestamoDao.listarActivos();
            if (prestamos != null) {
                listaPrestamos.addAll(prestamos);
            }
            filtroPrestamos = new FilteredList<>(listaPrestamos, b -> true);
            txtBuscar.textProperty().addListener((observable, oldValue, newValue) -> aplicarFiltros());
            cmbFiltro.valueProperty().addListener((observable, oldValue, newValue) -> aplicarFiltros());

            SortedList<Prestamos> sortedData = new SortedList<>(filtroPrestamos);
            sortedData.comparatorProperty().bind(tablaPrestamos.comparatorProperty());
            tablaPrestamos.setItems(sortedData);
            actualizarContador(sortedData.size());
        } catch (Exception e) {
            System.err.println("Error al obtener los préstamos desde la BD: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void aplicarFiltros() {
        String textoBusqueda = txtBuscar.getText() == null ? "" : txtBuscar.getText().toLowerCase().trim();
        String opcionFiltro = cmbFiltro.getValue() == null ? "Todos" : cmbFiltro.getValue();
        filtroPrestamos.setPredicate(prestamo -> {
            boolean cumpleFiltroCombo = true;
            String idActivo = prestamo.getIdActivo() != null ? prestamo.getIdActivo().toUpperCase() : "";
            if ("Máquina".equals(opcionFiltro)) {
                cumpleFiltroCombo = idActivo.startsWith("MAQ") || idActivo.contains("MAQUINA");
            } else if ("Herramienta".equals(opcionFiltro)) {
                cumpleFiltroCombo = idActivo.startsWith("HERR") || idActivo.contains("HERRAMIENTA");
            } else if ("Kit".equals(opcionFiltro)) {
                cumpleFiltroCombo = idActivo.startsWith("KIT");
            }
            if (textoBusqueda.isEmpty()) {
                return cumpleFiltroCombo;
            }
            boolean cumpleBusqueda = false;
            if (prestamo.getIdPrestamo() != null && String.valueOf(prestamo.getIdPrestamo()).toLowerCase().contains(textoBusqueda)) {
                cumpleBusqueda = true;
            } else if (prestamo.getIdActivo() != null && prestamo.getIdActivo().toLowerCase().contains(textoBusqueda)) {
                cumpleBusqueda = true;
            } else if (prestamo.getOperarioNombre() != null && prestamo.getOperarioNombre().toLowerCase().contains(textoBusqueda)) {
                cumpleBusqueda = true;
            } else if (prestamo.getEstado() != null && prestamo.getEstado().toLowerCase().contains(textoBusqueda)) {
                cumpleBusqueda = true;
            }
            return cumpleFiltroCombo && cumpleBusqueda;
        });

        actualizarContador(filtroPrestamos.size());
    }

    private void actualizarContador(int total) {
        if (lblTotalPrestamos != null) {
            lblTotalPrestamos.setText("Mostrando " + total + " préstamos");
        }
    }

    @FXML
    private void onAnadirPrestamoClick(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/ryrcontrolcenter/ui/NuevoPrestamoVista.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Registrar Nuevo Préstamo");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();
            cargarDatosBD();
        } catch (IOException e) {
            System.err.println("Error al abrir NuevoPrestamoVista.fxml: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void onModificarPrestamoClick(ActionEvent event) {
        Prestamos seleccionado = tablaPrestamos.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            AlertaUtil.mostrar("Atención", "Por favor seleccione un préstamo de la tabla para modificar.", Alert.AlertType.WARNING);
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/ryrcontrolcenter/ui/NuevoPrestamoVista.fxml"));
            Parent root = loader.load();
            NuevoPrestamoController controller = loader.getController();
            controller.setPrestamoParaEditar(seleccionado);
            Stage stage = new Stage();
            stage.setTitle("Modificar Préstamo - " + seleccionado.getIdPrestamo());
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();
            cargarDatosBD();
        } catch (IOException e) {
            System.err.println("Error al abrir modal para editar: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void onEliminarPrestamoClick(ActionEvent event) {
        Prestamos seleccionado = tablaPrestamos.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            AlertaUtil.mostrar("Atención", "Por favor seleccione un préstamo de la tabla para eliminar.", Alert.AlertType.WARNING);
            return;
        }
        boolean confirmado = AlertaUtil.confirmar(
                "Confirmar devolución",
                "¿Está seguro de marcar como devuelto el préstamo " + seleccionado.getIdPrestamo() + "?"
        );
        if (!confirmado) {
            return;
        }
        boolean exito = prestamoDao.eliminarLogico(seleccionado.getIdPrestamo());
        if (exito) {
            BitacoraService.registrar(
                    "Registro de devolución del préstamo " + seleccionado.getIdPrestamo(),
                    "Prestamos",
                    seleccionado.getIdPrestamo()
            );
            AlertaUtil.mostrar("Éxito", "El préstamo ha sido marcado como devuelto.", Alert.AlertType.INFORMATION);
            cargarDatosBD();
        } else {
            AlertaUtil.mostrar("Error", "No se pudo actualizar el estado del préstamo en la base de datos.", Alert.AlertType.ERROR);
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
