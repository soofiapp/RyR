package com.ryrcontrolcenter.controller;

import com.ryrcontrolcenter.modelo.Usuario;
import com.ryrcontrolcenter.service.AuthService;
import com.ryrcontrolcenter.util.SceneManager;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController implements Initializable {

    @FXML
    private TextField txtUsuario;
    @FXML
    private PasswordField txtPassword;
    @FXML
    private Button btnEntrar;
    @FXML
    private Label lblError;

    private final AuthService authService = new AuthService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // TODO
    }

    @FXML
    private void clickDeEntrar() {
        String usuario = txtUsuario.getText();
        String contrasena = txtPassword.getText();
        Usuario u = authService.iniciarSesion(usuario, contrasena);

        if (u != null) {
            lblError.setText("");
            System.out.println("Bienvenid@, " + u.getNombreCompleto() + " (" + u.getRol() + ")");
            SceneManager.cambiarA("/com/ryrcontrolcenter/ui/DashboardVista.fxml");
        } else {
            lblError.setText("Usuario o contraseña incorrectos");
        }
    }
}

