package com.ryrcontrolcenter.controller;

import com.ryrcontrolcenter.modelo.Usuario;
import com.ryrcontrolcenter.service.AuthService;
import com.ryrcontrolcenter.service.BitacoraService;
import com.ryrcontrolcenter.util.AlertaUtil;
import com.ryrcontrolcenter.util.FondoUtil;
import com.ryrcontrolcenter.util.SceneManager;
import com.ryrcontrolcenter.util.SesionActual;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class LoginController implements Initializable {

    @FXML
    private StackPane raiz;
    @FXML
    private VBox tarjeta;
    @FXML
    private TextField txtUsuario;
    @FXML
    private PasswordField txtPassword;
    @FXML
    private Button btnEntrar;
    @FXML
    private Label lblError;
    @FXML
    private Label lblOlvido;

    private final AuthService authService = new AuthService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        lblError.managedProperty().bind(lblError.textProperty().isNotEmpty());
        FondoUtil.aplicarFondoConVidrio(raiz,
                tarjeta,
                FondoUtil.cargar("/com/ryrcontrolcenter/images/Login_carga.png"),
                18, 16);
    }

    @FXML
    private void clickDeEntrar() {
        String usuario = txtUsuario.getText();
        String contrasena = txtPassword.getText();
        Usuario u = authService.iniciarSesion(usuario, contrasena);
        if (u != null) {
            lblError.setText("");
            System.out.println("Bienvenid@, " + u.getNombreCompleto() + " (" + u.getRol() + ")");
            SesionActual.iniciar(u);
            SceneManager.cambiarA("/com/ryrcontrolcenter/ui/DashboardVista.fxml");
            BitacoraService.registrar(
                    "Inicio de sesión de " + u.getUsuarioLogin(),
                    "Usuarios y Bitacora",
                    u.getUsuarioLogin());
        } else {
            lblError.setText("Usuario o contraseña incorrectos");
        }
    }

    @FXML
    private void onOlvidoClick() {
        AlertaUtil.mostrar("Recuperar contraseña",
                "Comuníquese con el administrador del sistema para restablecer su contraseña.",
                Alert.AlertType.INFORMATION);
    }
}
