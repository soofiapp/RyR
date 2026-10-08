package com.ryrcontrolcenter.util;

import java.util.List;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;


public class SceneManager {

    public static final String PRELOGIN = "/com/ryrcontrolcenter/ui/PreLoginVista.fxml";
    public static final String LOGIN = "/com/ryrcontrolcenter/ui/LoginVista.fxml";

    private static final double FIJO_ANCHO = 884;
    private static final double FIJO_ALTO_CONTENIDO = 505;
    private static final double FIJO_ALTO = FIJO_ALTO_CONTENIDO + BarraTitulo.ALTO;

    private static final double APP_MIN_ANCHO = 1024;
    private static final double APP_MIN_ALTO = 680;
    private static final double APP_ANCHO = 1280;
    private static final double APP_ALTO = 760;

    private static final double RADIO_ESQUINAS = 12;

    private static Stage stage;
    private static Rectangle recorte;
    private static Region borde;
    private static StackPane contenido;
    private static BarraTitulo barra;

    private static boolean modoApp = false;
    private static boolean maximizado = false;
    private static Rectangle2D boundsRestaurar;

    public static void setStage(Stage s) {
        stage = s;

        stage.initStyle(StageStyle.TRANSPARENT);

        contenido = new StackPane();
        barra = new BarraTitulo(stage);

        BorderPane marco = new BorderPane(contenido);
        marco.setTop(barra);
        marco.setStyle("-fx-background-color: #0b1118;");

        recorte = new Rectangle();
        recorte.widthProperty().bind(marco.widthProperty());
        recorte.heightProperty().bind(marco.heightProperty());
        marco.setClip(recorte);

        borde = new Region();
        borde.setMouseTransparent(true);

        StackPane ventana = new StackPane(marco, borde);
        ventana.setStyle("-fx-background-color: transparent;");
        aplicarEsquinas(true);

        Scene scene = new Scene(ventana, FIJO_ANCHO, FIJO_ALTO);
        scene.setFill(Color.TRANSPARENT);
        scene.getStylesheets().add(
                SceneManager.class.getResource("/com/ryrcontrolcenter/ui/titlebar.css").toExternalForm());
        stage.setScene(scene);

        try {
            stage.getIcons().add(new Image(
                    SceneManager.class.getResourceAsStream("/com/ryrcontrolcenter/images/ryr_logo.png")));
        } catch (Exception ignorado) {
        }

        RedimensionUtil.instalar(stage, ventana, () -> modoApp && !maximizado);
        configurarModo(false);
    }

    public static void cambiarA(String rutaFxml) {
        try {
            Parent root = FXMLLoader.load(SceneManager.class.getResource(rutaFxml));
            contenido.getChildren().setAll(root);

            boolean app = !(PRELOGIN.equals(rutaFxml) || LOGIN.equals(rutaFxml));
            if (!stage.isShowing() || app != modoApp) {
                configurarModo(app);
            }
            barra.setUsuario(app ? SesionActual.getUsuario() : null);
            if (!stage.isShowing()) {
                stage.show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (!stage.isShowing()) {
                stage.show();
            }
            AlertaUtil.mostrar("No se pudo abrir la pantalla",
                    rutaFxml + "\n\n" + (e.getCause() != null ? e.getCause() : e),
                    Alert.AlertType.ERROR);
        }
    }

    private static void aplicarEsquinas(boolean redondeadas) {
        double r = redondeadas ? RADIO_ESQUINAS : 0;
        recorte.setArcWidth(r * 2);
        recorte.setArcHeight(r * 2);
        borde.setStyle(redondeadas
                ? "-fx-border-color: rgba(255,255,255,0.16); -fx-border-width: 1; -fx-border-radius: " + r + ";"
                : "-fx-border-color: transparent;");
    }

    private static void configurarModo(boolean app) {
        modoApp = app;
        maximizado = false;
        aplicarEsquinas(true);
        barra.setMaximizado(false);
        barra.setPermiteMaximizar(app);

        contenido.setStyle(app ? "-fx-background-color: white;" : "-fx-background-color: #0b1118;");

        stage.setMinWidth(0);
        stage.setMinHeight(0);
        stage.setMaxWidth(Double.MAX_VALUE);
        stage.setMaxHeight(Double.MAX_VALUE);

        if (!app) {
            centrar(FIJO_ANCHO, FIJO_ALTO);
            stage.setMinWidth(FIJO_ANCHO);
            stage.setMinHeight(FIJO_ALTO);
            stage.setMaxWidth(FIJO_ANCHO);
            stage.setMaxHeight(FIJO_ALTO);
        } else {
            stage.setMinWidth(APP_MIN_ANCHO);
            stage.setMinHeight(APP_MIN_ALTO);
            Rectangle2D v = pantallaActual().getVisualBounds();
            double w = Math.min(APP_ANCHO, v.getWidth());
            double h = Math.min(APP_ALTO, v.getHeight());
            boundsRestaurar = new Rectangle2D(
                    v.getMinX() + (v.getWidth() - w) / 2,
                    v.getMinY() + (v.getHeight() - h) / 2, w, h);
            maximizar(false); 
        }
    }

    private static void centrar(double ancho, double alto) {
        Rectangle2D v = pantallaActual().getVisualBounds();
        stage.setWidth(ancho);
        stage.setHeight(alto);
        stage.setX(v.getMinX() + (v.getWidth() - ancho) / 2);
        stage.setY(v.getMinY() + (v.getHeight() - alto) / 2);
    }

    private static Screen pantallaActual() {
        double w = Double.isNaN(stage.getWidth()) ? FIJO_ANCHO : stage.getWidth();
        double h = Double.isNaN(stage.getHeight()) ? FIJO_ALTO : stage.getHeight();
        if (Double.isNaN(stage.getX()) || Double.isNaN(stage.getY())) {
            return Screen.getPrimary();
        }
        List<Screen> pantallas = Screen.getScreensForRectangle(stage.getX(), stage.getY(), w, h);
        return pantallas.isEmpty() ? Screen.getPrimary() : pantallas.get(0);
    }

    public static void cerrarSesion() {
        SesionActual.cerrar();
        cambiarA(LOGIN);
    }

    public static void minimizar() {
        stage.setIconified(true);
    }

    public static void cerrar() {
        stage.close();
    }

    public static boolean estaMaximizado() {
        return maximizado;
    }

    public static void alternarMaximizado() {
        if (!modoApp) {
            return;
        }
        if (maximizado) {
            restaurar();
        } else {
            maximizar(true);
        }
    }

    private static void maximizar(boolean recordarActual) {
        if (recordarActual && !maximizado && stage.isShowing()) {
            boundsRestaurar = new Rectangle2D(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());
        }
        Rectangle2D v = pantallaActual().getVisualBounds();
        stage.setX(v.getMinX());
        stage.setY(v.getMinY());
        stage.setWidth(v.getWidth());
        stage.setHeight(v.getHeight());
        maximizado = true;
        aplicarEsquinas(false); 
        barra.setMaximizado(true);
    }

    private static void restaurar() {
        Rectangle2D b = boundsRestaurar;
        if (b == null) {
            Rectangle2D v = pantallaActual().getVisualBounds();
            b = new Rectangle2D(v.getMinX() + 60, v.getMinY() + 40,
                    Math.min(APP_ANCHO, v.getWidth() - 120), Math.min(APP_ALTO, v.getHeight() - 80));
        }
        stage.setX(b.getMinX());
        stage.setY(b.getMinY());
        stage.setWidth(Math.max(b.getWidth(), APP_MIN_ANCHO));
        stage.setHeight(Math.max(b.getHeight(), APP_MIN_ALTO));
        maximizado = false;
        aplicarEsquinas(true);
        barra.setMaximizado(false);
    }
}
