package com.ryrcontrolcenter.controller;

import com.ryrcontrolcenter.modelo.Usuario;
import com.ryrcontrolcenter.service.AuthService;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

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
    private void clickDeEntrar(ActionEvent event) {
        try {
            String usuario = txtUsuario.getText();
            String contraseña = txtPassword.getText();
            Usuario u = authService.iniciarSesion(usuario, contraseña);
            if (u != null) {
                lblError.setText("Bienvenid@");
                System.out.println("Bienvenid@, " + u.getNombreCompleto() + " (" + u.getRol() + ")");
                Stage stage = new Stage();
                stage.setFullScreen(true);
                Parent root = FXMLLoader.load(getClass().getResource("/com/ryrcontrolcenter/ui/DashboardVista.fxml"));
                stage.setTitle("Dashboard");
                stage.setScene(new Scene(root));
                stage.show();
                Stage stageLogin = (Stage) ((Node) event.getSource()).getScene().getWindow();
                stageLogin.close();
            } else {
                lblError.setText("Usuario o contraseña incorrectos");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
