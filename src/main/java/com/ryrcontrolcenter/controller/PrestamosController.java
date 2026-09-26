/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/javafx/FXMLController.java to edit this template
 */
package com.ryrcontrolcenter.controller;

import com.ryrcontrolcenter.dao.PrestamoDao;
import com.ryrcontrolcenter.modelo.Prestamos;
import com.ryrcontrolcenter.util.SceneManager;
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
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.effect.BoxBlur;
import javafx.scene.layout.BorderPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * FXML Controller class
 *
 * @author Juanp
 */
public class PrestamosController implements Initializable {

    @FXML
    private ComboBox<?> cmbFiltro;
    @FXML
    private TextField txtBuscar;
    @FXML
    private Label lblTotalPrestamos;
    @FXML
    private Button btnAnadirPrestamo;
    @FXML
    private Button btnEliminarPrestamo;
    @FXML
    private Button btnModificarPrestamo;
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
    private TableColumn<?, ?> colDevolucion;
    @FXML
    private BorderPane mainPane;

    private final PrestamoDao prestamoD = new PrestamoDao();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        colIdPrestamo.setCellValueFactory(new PropertyValueFactory<>("idPrestamo"));
        colInventario.setCellValueFactory(new PropertyValueFactory<>("idActivo"));
        colOperario.setCellValueFactory(new PropertyValueFactory<>("operarioNombre"));
        colSalida.setCellValueFactory(new PropertyValueFactory<>("fechaSalida"));
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));
        colDevolucion.setCellValueFactory(new PropertyValueFactory<>("fechaDevolucionEstimada"));
        cargarDatosTabla();
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

    @FXML
    private void onAnadirPrestamoClick(ActionEvent event) {
        BoxBlur blur = new BoxBlur(5, 5, 3);
        mainPane.setEffect(blur);
        mainPane.setOpacity(0.6);
        try {
            Stage stage = new Stage();
            Parent root = FXMLLoader.load(getClass().getResource("/com/ryrcontrolcenter/ui/NuevoPrestamoVista.fxml"));
            stage.setScene(new Scene(root));
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(mainPane.getScene().getWindow());
            stage.setOnHidden(ev -> {
                mainPane.setEffect(null);
                mainPane.setOpacity(1.0);
            });
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            mainPane.setEffect(null);
            mainPane.setOpacity(1.0);
        }
    }

    private void cargarDatosTabla() {
        try {
            List<Prestamos> lista1 = prestamoD.listarTodos();
            ObservableList<Prestamos> datos = FXCollections.observableArrayList(lista1);
            tablaPrestamos.setItems(datos);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
