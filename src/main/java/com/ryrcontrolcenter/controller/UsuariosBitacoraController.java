/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/javafx/FXMLController.java to edit this template
 */
package com.ryrcontrolcenter.controller;

import com.ryrcontrolcenter.dao.BitacoraDao;
import com.ryrcontrolcenter.dao.UsuarioDao;
import com.ryrcontrolcenter.modelo.Bitacora;
import com.ryrcontrolcenter.modelo.Usuario;
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
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.effect.BoxBlur;
import javafx.scene.layout.BorderPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class UsuariosBitacoraController implements Initializable {

    @FXML
    private TableView<Usuario> tablaUsuarios;
    @FXML
    private TableColumn<Usuario, String> colID;
    @FXML
    private TableColumn<Usuario, String> colUsuario;
    @FXML
    private TableColumn<Usuario, String> colRol;
    @FXML
    private TableColumn<Usuario, String> colFrentePlanta;
    @FXML
    private TableColumn<Usuario, String> colUltimoAcceso;
    @FXML
    private TableView<Bitacora> tablaBitacora;
    @FXML
    private TableColumn<Bitacora, String> colFechaHora;
    @FXML
    private TableColumn<Bitacora, String> colUsuarioBitacora;
    @FXML
    private TableColumn<Bitacora, String> colAccion;
    @FXML
    private TableColumn<Bitacora, String> colModulo;
    @FXML
    private BorderPane mainPane;
    @FXML
    private Button btnRegistrarUsuario;
    @FXML
    private Button btnEliminarUsuario;
    @FXML
    private Button btnModificarUsuario;

    private final UsuarioDao usuarioD = new UsuarioDao();
    private final BitacoraDao bitacoraD = new BitacoraDao();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (!SesionActual.puedeEditar()) {
            btnRegistrarUsuario.setVisible(false);
            btnModificarUsuario.setVisible(false);
            btnEliminarUsuario.setVisible(false);
        }
        colID.setCellValueFactory(new PropertyValueFactory<>("idUsuario"));
        colUsuario.setCellValueFactory(new PropertyValueFactory<>("nombreCompleto"));
        colRol.setCellValueFactory(new PropertyValueFactory<>("rol"));
        colFrentePlanta.setCellValueFactory(new PropertyValueFactory<>("frentePlanta"));
        colUltimoAcceso.setCellValueFactory(new PropertyValueFactory<>("ultimoAcceso"));
        colFechaHora.setCellValueFactory(new PropertyValueFactory<>("fechaHora"));
        colUsuarioBitacora.setCellValueFactory(new PropertyValueFactory<>("nombreUsuario"));
        colAccion.setCellValueFactory(new PropertyValueFactory<>("accionRealizada"));
        colModulo.setCellValueFactory(new PropertyValueFactory<>("moduloAfectado"));
        cargarDatosTabla();
        cargarBitacora();
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

    private void irAUsuariosBitacora(ActionEvent event) {
        SceneManager.cambiarA("/com/ryrcontrolcenter/ui/UsuariosBitacoraVista.fxml");
    }

    @FXML
    private void onLogoutClick(ActionEvent event) {
        SesionActual.cerrar();
        SceneManager.cambiarA("/com/ryrcontrolcenter/ui/LoginVista.fxml");
    }

    @FXML
    private void onRegistrarUsuarioClick(ActionEvent event) {
        abrirDialogo(null);
    }

    private void cargarDatosTabla() {
        try {
            List<Usuario> lista1 = usuarioD.listarTodos();
            ObservableList<Usuario> datos = FXCollections.observableArrayList(lista1);
            tablaUsuarios.setItems(datos);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void cargarBitacora() {
        List<Bitacora> lista = bitacoraD.listarTodas();
        tablaBitacora.setItems(FXCollections.observableArrayList(lista));
    }

    @FXML
    private void onEliminarUsuarioClick(ActionEvent event) {
        Usuario seleccionado = tablaUsuarios.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            return;
        }
        usuarioD.eliminar(seleccionado.getIdUsuario());
        BitacoraService.registrar(
                "Desactivación de usuario " + seleccionado.getUsuarioLogin(),
                "Usuarios y Bitacora",
                seleccionado.getUsuarioLogin());
        cargarDatosTabla();
        cargarBitacora();
    }

    @FXML
    private void onModificarUsuarioClick(ActionEvent event) {
        Usuario seleccionado = tablaUsuarios.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            return;
        }
        abrirDialogo(seleccionado);
    }

    private void abrirDialogo(Usuario paraEditar) {
        BoxBlur blur = new BoxBlur(5, 5, 3);
        mainPane.setEffect(blur);
        mainPane.setOpacity(0.6);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/ryrcontrolcenter/ui/NuevoUsuarioVista.fxml"));
            Parent root = loader.load();
            NuevoUsuarioController dialogController = loader.getController();
            if (paraEditar != null) {
                dialogController.cargarParaEdicion(paraEditar);
            }
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(mainPane.getScene().getWindow());
            stage.setOnHidden(ev -> {
                mainPane.setEffect(null);
                mainPane.setOpacity(1.0);
                cargarDatosTabla(); 
                cargarBitacora();});
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            mainPane.setEffect(null);
            mainPane.setOpacity(1.0);
        }
    }
}
