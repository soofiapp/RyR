package com.ryrcontrolcenter.controller;

import com.ryrcontrolcenter.dao.UsuarioDao;
import com.ryrcontrolcenter.modelo.Usuario;
import com.ryrcontrolcenter.service.BitacoraService;
import com.ryrcontrolcenter.util.PasswordUtil;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class NuevoUsuarioController implements Initializable {

    @FXML
    private TextField txtNombreCompleto;
    @FXML
    private TextField txtUsuarioLogin;
    @FXML
    private PasswordField txtPassword;
    @FXML
    private ComboBox<String> cmbRol;
    @FXML
    private TextField txtFrentePlanta;
    @FXML
    private Button btnCancelar;
    @FXML
    private Button btnGuardar;
    @FXML
    private Label lblAyudaPassword;
    @FXML
    private Label lblError;

    private final UsuarioDao usuarioDao = new UsuarioDao();
    private Integer idUsuarioEdicion;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        cmbRol.setItems(FXCollections.observableArrayList("Administrador", "Auxiliar"));
    }

    @FXML
    private void onCancelarClick(ActionEvent event) {
        cerrarVentana();
    }

    public void cargarParaEdicion(Usuario u) {
        this.idUsuarioEdicion = u.getIdUsuario();
        txtNombreCompleto.setText(u.getNombreCompleto());
        txtUsuarioLogin.setText(u.getUsuarioLogin());
        cmbRol.getSelectionModel().select(u.getRol());
        txtFrentePlanta.setText(u.getFrentePlanta());
        lblAyudaPassword.setText("Contraseña (dejar vacío para no cambiarla)");
    }

    @FXML
    private void onGuardarClick(ActionEvent event) {
        String nombre = txtNombreCompleto.getText();
        String login = txtUsuarioLogin.getText();
        String password = txtPassword.getText();
        String rol = cmbRol.getSelectionModel().getSelectedItem();
        String frentePlanta = txtFrentePlanta.getText();
        if (nombre == null || nombre.isBlank()
                || login == null || login.isBlank()
                || rol == null) {
            lblError.setText("Nombre, usuario y rol son obligatorios");
            return;
        }
        Usuario u = new Usuario();
        u.setNombreCompleto(nombre);
        u.setUsuarioLogin(login);
        u.setRol(rol);
        u.setFrentePlanta(frentePlanta);

        boolean exito;
        if (idUsuarioEdicion == null) {
            if (password == null || password.isBlank()) {
                lblError.setText("La contraseña es obligatoria para un usuario nuevo");
                return;
            }
            String hash = PasswordUtil.generarHash(password);
            u.setPasswordHash(hash);
            exito = usuarioDao.insertar(u);
        } else {
            u.setIdUsuario(idUsuarioEdicion);
            if (password == null || password.isBlank()) {
                exito = usuarioDao.actualizar(u);
            } else {
                String nuevoHash = PasswordUtil.generarHash(password);
                exito = usuarioDao.actualizarConPassword(u,nuevoHash);
            }
        }
        if (exito) {
            String accion;
            if (idUsuarioEdicion == null) {
                accion = "Creación de usuario " + login + " (rol " + rol + ")";
            } else {
                accion = "Modificación de usuario " + login;
            }
            try {
                BitacoraService.registrar(accion,"Usuarios y Bitacora",login);
            } catch (Exception e) {
                e.printStackTrace();
            }
            cerrarVentana();
        } else {
            lblError.setText("No se pudo guardar. Verifique que el usuario " + (login) + " no esté repetido.");
        }
    }

    private void cerrarVentana() {
        Stage stage = (Stage) btnCancelar.getScene().getWindow();
        stage.close();
    }

}
