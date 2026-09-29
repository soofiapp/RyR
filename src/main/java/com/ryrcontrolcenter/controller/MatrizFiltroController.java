package com.ryrcontrolcenter.controller;

import com.ryrcontrolcenter.dao.MaquinaDao;
import com.ryrcontrolcenter.dao.MatrizFiltroDao;
import com.ryrcontrolcenter.modelo.Maquina;
import com.ryrcontrolcenter.modelo.MatrizFiltro;
import com.ryrcontrolcenter.service.BitacoraService;
import com.ryrcontrolcenter.util.SceneManager;
import com.ryrcontrolcenter.util.SesionActual;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.effect.BoxBlur;
import javafx.scene.layout.BorderPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class MatrizFiltroController implements Initializable {

    @FXML
    private ComboBox<Maquina> cmbMaquina;
    @FXML
    private Button btnAgregarFiltro;
    @FXML
    private Button btnEliminarFiltro;
    @FXML
    private Button btnModificarFiltro;
    @FXML
    private TableView<MatrizFiltro> tablaMatriz;
    @FXML
    private TableColumn<MatrizFiltro, String> colTipoFiltro;
    @FXML
    private TableColumn<MatrizFiltro, String> colCodigoOem;
    @FXML
    private TableColumn<MatrizFiltro, String> colRefStock;
    @FXML
    private TableColumn<MatrizFiltro, String> colFechaActualizacion;
    @FXML
    private BorderPane mainPane;

    private final MatrizFiltroDao matrizD = new MatrizFiltroDao();
    private final MaquinaDao maquinaD = new MaquinaDao();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (!SesionActual.puedeEditar()) {
            btnAgregarFiltro.setVisible(false);
            btnEliminarFiltro.setVisible(false);
            btnModificarFiltro.setVisible(false);
        }
        colTipoFiltro.setCellValueFactory(new PropertyValueFactory<>("sistemaTipoFiltro"));
        colCodigoOem.setCellValueFactory(new PropertyValueFactory<>("codigoOem"));
        colRefStock.setCellValueFactory(new PropertyValueFactory<>("referenciaStock"));
        colFechaActualizacion.setCellValueFactory(new PropertyValueFactory<>("fechaActualizacion"));
        cmbMaquina.setConverter(new javafx.util.StringConverter<Maquina>() {
            @Override
            public String toString(Maquina m) {
                return m == null ? "" : m.getIdMaquina() + " | " + m.getMarca() + " " + m.getModelo();
            }

            @Override
            public Maquina fromString(String s) {
                return null;
            }
        });
        List<Maquina> maquinas = maquinaD.listarTodas();
        cmbMaquina.setItems(FXCollections.observableArrayList(maquinas));
        cmbMaquina.getSelectionModel().selectedItemProperty().addListener((obs, anterior, nueva) -> {
            if (nueva != null) {
                cargarDatosTabla(nueva.getIdMaquina());
            }
        });
        if (!maquinas.isEmpty()) {
            cmbMaquina.getSelectionModel().selectFirst();
        }
    }

    private void cargarDatosTabla(String idMaquina) {
        List<MatrizFiltro> lista = matrizD.listarPorMaquina(idMaquina);
        ObservableList<MatrizFiltro> datos = FXCollections.observableArrayList(lista);
        tablaMatriz.setItems(datos);
    }

    private void recargarTablaActual() {
        Maquina seleccionada = cmbMaquina.getSelectionModel().getSelectedItem();
        if (seleccionada != null) {
            cargarDatosTabla(seleccionada.getIdMaquina());
        }
    }

    @FXML
    private void onAgregarFiltroClick(ActionEvent event) {
        Maquina seleccionada = cmbMaquina.getSelectionModel().getSelectedItem();
        if (seleccionada == null) {
            return;
        }
        abrirDialogo(null, seleccionada);
    }

    @FXML
    private void onModificarFiltroClick(ActionEvent event) {
        MatrizFiltro seleccionado = tablaMatriz.getSelectionModel().getSelectedItem();
        Maquina maquinaActual = cmbMaquina.getSelectionModel().getSelectedItem();
        if (seleccionado == null || maquinaActual == null) {
            return;
        }
        abrirDialogo(seleccionado, maquinaActual);
    }

    private void abrirDialogo(MatrizFiltro paraEditar, Maquina maquina) {
        BoxBlur blur = new BoxBlur(5, 5, 3);
        mainPane.setEffect(blur);
        mainPane.setOpacity(0.6);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/ryrcontrolcenter/ui/NuevoFiltroMatrizVista.fxml"));
            Parent root = loader.load();
            NuevoFiltroMatrizController dialogController = loader.getController();
            String nombreMostrar = maquina.getIdMaquina() + " | " + maquina.getMarca() + " " + maquina.getModelo();

            if (paraEditar == null) {
                dialogController.setMaquinaContexto(maquina.getIdMaquina(), nombreMostrar);
            } else {
                dialogController.cargarParaEdicion(paraEditar, nombreMostrar);
            }

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(mainPane.getScene().getWindow());
            stage.setOnHidden(ev -> {
                mainPane.setEffect(null);
                mainPane.setOpacity(1.0);
                recargarTablaActual();
            });
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            mainPane.setEffect(null);
            mainPane.setOpacity(1.0);
        }
    }

    @FXML
    private void onEliminarFiltroClick(ActionEvent event) {
        MatrizFiltro seleccionado = tablaMatriz.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            return;
        }
        matrizD.eliminar(seleccionado.getIdMatriz());
        BitacoraService.registrar(
                "Eliminación de filtro " + seleccionado.getSistemaTipoFiltro() + " de máquina " + seleccionado.getIdMaquina(),
                "Matriz Filtro",
                seleccionado.getIdMaquina());
        recargarTablaActual();
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
