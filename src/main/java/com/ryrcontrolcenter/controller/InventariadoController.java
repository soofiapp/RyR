/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/javafx/FXMLController.java to edit this template
 */
package com.ryrcontrolcenter.controller;

import com.ryrcontrolcenter.dao.InventarioDao;
import com.ryrcontrolcenter.modelo.Inventario;
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
public class InventariadoController implements Initializable {

    @FXML
    private ComboBox<?> cmbFiltro;
    @FXML
    private TextField txtBuscar;
    @FXML
    private Label lblTotalActivos;
    @FXML
    private Button btnAgregarActivo;
     @FXML
    private Button btnEliminarActivo;
    @FXML
    private Button btnModificarActivo;
    @FXML
    private TableView<Inventario> tablaInventario;
    @FXML
    private TableColumn<Inventario, String> colCodigo;
    @FXML
    private TableColumn<Inventario, String> colDescripcion;
    @FXML
    private TableColumn<Inventario, String> colTipo;
    @FXML
    private TableColumn<Inventario, String> colEstadoSst;
    @FXML
    private TableColumn<Inventario, String> colObservaciones;
    @FXML
    private BorderPane mainPane;
    
    private final InventarioDao inventarioD = new InventarioDao();
  
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        colCodigo.setCellValueFactory(new PropertyValueFactory<>("idActivo"));
        colDescripcion.setCellValueFactory(new PropertyValueFactory<>("descripcion"));
        colTipo.setCellValueFactory(new PropertyValueFactory<>("tipoActivo"));
        colEstadoSst.setCellValueFactory(new PropertyValueFactory<>("estadoSst"));
        colObservaciones.setCellValueFactory(new PropertyValueFactory<>("observaciones"));
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

    private void cargarDatosTabla() {
        try {
            List<Inventario> lista = inventarioD.listarTodos();
            ObservableList<Inventario> datos = FXCollections.observableArrayList(lista);
            tablaInventario.setItems(datos);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onAgregarActivoClick(ActionEvent event) {
        BoxBlur blur = new BoxBlur(5, 5, 3);
        mainPane.setEffect(blur);
        mainPane.setOpacity(0.6);
        try {
            Stage stage = new Stage();
            Parent root = FXMLLoader.load(getClass().getResource("/com/ryrcontrolcenter/ui/NuevoInventarioVista.fxml"));
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

}
