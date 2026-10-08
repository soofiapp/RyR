package com.ryrcontrolcenter.util;

import com.ryrcontrolcenter.modelo.Usuario;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import javafx.stage.Popup;
import javafx.stage.Stage;

public class BarraTitulo extends HBox {

    public static final double ALTO = 46;
    private static final String VERSION = "v1.0";

    private final Stage stage;
    private final Button btnMaximizar = new Button();
    private final SVGPath iconoMaximizar = icono("M0,0 H10 V10 H0 Z");
    private final SVGPath iconoRestaurar = icono("M2.5,0 H10 V7.5 M0,2.5 H7.5 V10 H0 Z");

    // perfil
    private final HBox chipPerfil = new HBox(9);
    private final Label avatarIniciales = new Label("?");
    private final Label lblPerfilSup = new Label();
    private final Label lblPerfilNombre = new Label();
    private final Tooltip tooltipPerfil = new Tooltip();
    private Popup popupPerfil;
    private long ultimoCierrePopup;
    private Usuario usuario;

    private double dragX;
    private double dragY;

    public BarraTitulo(Stage stage) {
        this.stage = stage;
        getStyleClass().add("rr-titlebar");
        setAlignment(Pos.CENTER_LEFT);
        setMinHeight(ALTO);
        setPrefHeight(ALTO);
        setMaxHeight(ALTO);
        setPadding(new Insets(0, 10, 0, 12));
        setSpacing(12);

        Region relleno = new Region();
        HBox.setHgrow(relleno, Priority.ALWAYS);

        Button btnMin = boton(icono("M0,5 H10"), "rr-wbtn");
        btnMin.setOnAction(e -> SceneManager.minimizar());

        btnMaximizar.getStyleClass().addAll("rr-wbtn");
        btnMaximizar.setGraphic(iconoMaximizar);
        btnMaximizar.setFocusTraversable(false);
        btnMaximizar.setCursor(Cursor.HAND);
        btnMaximizar.setOnAction(e -> SceneManager.alternarMaximizado());

        Button btnCerrar = boton(icono("M0,0 L10,10 M10,0 L0,10"), "rr-wbtn", "rr-wbtn-close");
        btnCerrar.setOnAction(e -> SceneManager.cerrar());

        HBox pastilla = new HBox(2, btnMin, btnMaximizar, btnCerrar);
        pastilla.getStyleClass().add("rr-wpill");
        pastilla.setAlignment(Pos.CENTER);

        construirChipPerfil();

        getChildren().addAll(crearLogo(), crearNombre(), relleno, chipPerfil, pastilla);
        setOnMousePressed(e -> {
            dragX = e.getScreenX() - stage.getX();
            dragY = e.getScreenY() - stage.getY();
        });
        setOnMouseDragged(e -> {
            if (!SceneManager.estaMaximizado()) {
                stage.setX(e.getScreenX() - dragX);
                stage.setY(e.getScreenY() - dragY);
            }
        });
        setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && btnMaximizar.isVisible() && e.getTarget() == this) {
                SceneManager.alternarMaximizado();
            }
        });
    }
    public void setPermiteMaximizar(boolean permite) {
        btnMaximizar.setVisible(permite);
        btnMaximizar.setManaged(permite);
    }

    public void setMaximizado(boolean maximizado) {
        btnMaximizar.setGraphic(maximizado ? iconoRestaurar : iconoMaximizar);
    }
    public void setUsuario(Usuario u) {
        this.usuario = u;
        if (popupPerfil != null) {
            popupPerfil.hide();
        }
        boolean hay = u != null;
        chipPerfil.setVisible(hay);
        chipPerfil.setManaged(hay);
        if (!hay) {
            return;
        }
        String nombre = nombreMostrado(u);
        avatarIniciales.setText(iniciales(nombre));
        lblPerfilSup.setText("Sesión iniciada · " + valor(u.getRol(), "Usuario"));
        lblPerfilNombre.setText(nombre);
        tooltipPerfil.setText("Sesión iniciada como " + valor(u.getRol(), "Usuario") + " " + nombre);
    }

    private void construirChipPerfil() {
        chipPerfil.getStyleClass().add("rr-perfil");
        chipPerfil.setAlignment(Pos.CENTER_LEFT);
        chipPerfil.setVisible(false);
        chipPerfil.setManaged(false);

        lblPerfilSup.getStyleClass().add("rr-perfil-sup");
        lblPerfilNombre.getStyleClass().add("rr-perfil-nombre");
        VBox textos = new VBox(0, lblPerfilSup, lblPerfilNombre);
        textos.setAlignment(Pos.CENTER_LEFT);

        SVGPath flecha = new SVGPath();
        flecha.setContent("M0,0 L4,4 L8,0");
        flecha.setFill(Color.TRANSPARENT);
        flecha.setStroke(Color.web("#9fb0c3"));
        flecha.setStrokeWidth(1.5);

        chipPerfil.getChildren().addAll(crearAvatar(avatarIniciales, 15, 12), textos, flecha);
        Tooltip.install(chipPerfil, tooltipPerfil);
        chipPerfil.setOnMouseClicked(e -> {
            e.consume();
            alternarPopupPerfil();
        });
    }

    private void alternarPopupPerfil() {
        if (popupPerfil != null && popupPerfil.isShowing()) {
            popupPerfil.hide();
            return;
        }

        if (System.currentTimeMillis() - ultimoCierrePopup < 250 || usuario == null) {
            return;
        }
        popupPerfil = new Popup();
        popupPerfil.setAutoHide(true);
        popupPerfil.setHideOnEscape(true);
        popupPerfil.setOnHidden(e -> ultimoCierrePopup = System.currentTimeMillis());
        popupPerfil.getContent().add(crearContenidoPerfil());

        Bounds b = chipPerfil.localToScreen(chipPerfil.getBoundsInLocal());
        double ancho = 290 + 24; 
        popupPerfil.show(chipPerfil, b.getMaxX() - ancho + 12, b.getMaxY() + 2);
    }

    private Node crearContenidoPerfil() {
        Usuario u = usuario;
        String nombre = nombreMostrado(u);

        Label cabecera = new Label("Sesión iniciada como");
        cabecera.setStyle("-fx-text-fill: #8fa1b5; -fx-font-size: 11px;");

        Label avatar = new Label(iniciales(nombre));
        StackPane avatarGrande = crearAvatar(avatar, 22, 17);

        Label lblNombre = new Label(nombre);
        lblNombre.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
        Label lblRol = new Label(valor(u.getRol(), "Usuario"));
        lblRol.setStyle("-fx-background-color: #2447c4; -fx-text-fill: white; -fx-font-size: 11px;"
                + "-fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 2 10 2 10;");
        VBox datos = new VBox(4, lblNombre, lblRol);
        datos.setAlignment(Pos.CENTER_LEFT);
        HBox cuerpo = new HBox(12, avatarGrande, datos);
        cuerpo.setAlignment(Pos.CENTER_LEFT);

        VBox contenido = new VBox(10, cabecera, cuerpo);
        contenido.getChildren().add(fila("Usuario", valor(u.getUsuarioLogin(), "-")));
        if (u.getFrentePlanta() != null && !u.getFrentePlanta().isBlank()) {
            contenido.getChildren().add(fila("Frente / planta", u.getFrentePlanta()));
        }
        if (u.getUltimoAcceso() != null && !u.getUltimoAcceso().isBlank()) {
            contenido.getChildren().add(fila("Último acceso", u.getUltimoAcceso()));
        }

        Separator sep = new Separator();
        Button cerrar = new Button("Cerrar sesión");
        cerrar.setMaxWidth(Double.MAX_VALUE);
        cerrar.setCursor(Cursor.HAND);
        cerrar.setStyle("-fx-background-color: rgba(209,69,59,0.18); -fx-text-fill: #ff9d96;"
                + "-fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 8 12 8 12;");
        cerrar.setOnMouseEntered(e -> cerrar.setStyle("-fx-background-color: #d1453b; -fx-text-fill: white;"
                + "-fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 8 12 8 12;"));
        cerrar.setOnMouseExited(e -> cerrar.setStyle("-fx-background-color: rgba(209,69,59,0.18); -fx-text-fill: #ff9d96;"
                + "-fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 8 12 8 12;"));
        cerrar.setOnAction(e -> {
            popupPerfil.hide();
            SceneManager.cerrarSesion();
        });
        contenido.getChildren().addAll(sep, cerrar);

        contenido.setPadding(new Insets(16));
        contenido.setPrefWidth(290);
        contenido.setMinWidth(290);
        contenido.setMaxWidth(290);
        contenido.setStyle("-fx-background-color: #0d141c; -fx-background-radius: 12;"
                + "-fx-border-color: rgba(255,255,255,0.12); -fx-border-radius: 12;");
        contenido.setEffect(new DropShadow(18, Color.web("#000000", 0.55)));

        StackPane margen = new StackPane(contenido);
        margen.setPadding(new Insets(6, 12, 18, 12));
        margen.setStyle("-fx-background-color: transparent;");
        return margen;
    }

    private Node fila(String etiqueta, String valor) {
        Label e = new Label(etiqueta);
        e.setStyle("-fx-text-fill: #8fa1b5; -fx-font-size: 12px;");
        e.setMinWidth(100);
        Label v = new Label(valor);
        v.setStyle("-fx-text-fill: #e6edf3; -fx-font-size: 12px; -fx-font-weight: bold;");
        v.setWrapText(true);
        HBox h = new HBox(8, e, v);
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }

    private static StackPane crearAvatar(Label iniciales, double radio, double fuente) {
        Circle c = new Circle(radio);
        c.setStyle("-fx-fill: linear-gradient(to bottom right, #3b82f6, #1d3f9e);");
        c.setStroke(Color.web("#ffffff", 0.35));
        iniciales.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: " + fuente + "px;");
        return new StackPane(c, iniciales);
    }

    private static String nombreMostrado(Usuario u) {
        if (u.getNombreCompleto() != null && !u.getNombreCompleto().isBlank()) {
            return u.getNombreCompleto().trim();
        }
        return valor(u.getUsuarioLogin(), "Usuario");
    }

    private static String iniciales(String nombre) {
        String[] partes = nombre.trim().split("\\s+");
        String r = partes[0].isEmpty() ? "?" : partes[0].substring(0, 1);
        if (partes.length > 1 && !partes[1].isEmpty()) {
            r += partes[1].substring(0, 1);
        }
        return r.toUpperCase();
    }

    private static String valor(String s, String porDefecto) {
        return s == null || s.isBlank() ? porDefecto : s;
    }

    private Node crearLogo() {
        StackPane insignia = new StackPane();
        insignia.getStyleClass().add("rr-logo-badge");
        insignia.setMinSize(34, 34);
        insignia.setPrefSize(34, 34);
        insignia.setMaxSize(34, 34);
        try {
            Image img = new Image(getClass().getResourceAsStream("/com/ryrcontrolcenter/images/ryr_logo.png"));
            ImageView iv = new ImageView(img);
            iv.setFitHeight(30);
            iv.setPreserveRatio(true);
            iv.setSmooth(true);
            insignia.getChildren().add(iv);
        } catch (Exception e) {
            Label l = new Label("R&R");
            l.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #2447c4;");
            insignia.getChildren().add(l);
        }
        return insignia;
    }

    private Node crearNombre() {
        Label ryr = new Label("RyR");
        ryr.getStyleClass().add("rr-app-nombre");
        Label cc = new Label("ControlCenter");
        cc.getStyleClass().addAll("rr-app-nombre", "rr-app-nombre2");
        Label ver = new Label(VERSION);
        ver.getStyleClass().add("rr-app-version");
        HBox h = new HBox(5, ryr, cc, ver);
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }

    private static Button boton(SVGPath icono, String... clases) {
        Button b = new Button();
        b.getStyleClass().addAll(clases);
        b.setGraphic(icono);
        b.setFocusTraversable(false);
        b.setCursor(Cursor.HAND);
        return b;
    }

    private static SVGPath icono(String contorno) {
        SVGPath p = new SVGPath();
        p.setContent(contorno);
        p.setFill(Color.TRANSPARENT);
        p.setStroke(Color.web("#e6edf3"));
        p.setStrokeWidth(1.3);
        return p;
    }
}
