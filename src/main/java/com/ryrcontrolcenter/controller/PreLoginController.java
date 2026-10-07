package com.ryrcontrolcenter.controller;

import com.ryrcontrolcenter.config.ConexionBD;
import com.ryrcontrolcenter.util.FondoUtil;
import com.ryrcontrolcenter.util.SceneManager;
import com.ryrcontrolcenter.util.SpriteAnimacion;
import java.io.InputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.ResourceBundle;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.PauseTransition;
import javafx.animation.RotateTransition;
import javafx.animation.Timeline;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import javafx.util.Duration;

/**
 * Pantalla de carga previa al login.
 *
 * - Fondo del login + recuadro azul oscuro translucido (ver PreLoginVista.fxml).
 * - Logo animado: hoja de sprites logo_ryr_sprite.png/.properties (video sin fondo).
 *   Si no existe, dibuja un globo animado de reemplazo.
 * - Circulo de carga: Carga.gif. Si no existe, dibuja uno de reemplazo.
 * - Debajo del titulo van cambiando frases; cada una corresponde a una
 *   verificacion real de la base de datos SQLite.
 */
public class PreLoginController implements Initializable {

    private static final String IMG = "/com/ryrcontrolcenter/images/";
    private static final long MS_POR_PASO = 600;   // cuanto se muestra cada frase como minimo
    private static final long MS_FINAL = 400;      // pausa tras el ultimo paso
    private static final boolean MOSTRAR_ICONOS_CRUD = true; // + ojo recargar papelera (wireframe)

    /** Un paso de la carga: frase a mostrar y, opcionalmente, una consulta real a la BD. */
    private record Paso(String frase, String sql) {
    }

    private static final List<Paso> PASOS = List.of(
            new Paso("Cargando base de datos local SQLite...", null),
            new Paso("Verificando usuarios y permisos...", "SELECT COUNT(*) FROM usuarios"),
            new Paso("Cargando inventario de herramientas y maquinaria...", "SELECT COUNT(*) FROM inventario"),
            new Paso("Revisando préstamos y devoluciones...", "SELECT COUNT(*) FROM prestamos"),
            new Paso("Calculando semáforo de insumos y filtros...", "SELECT COUNT(*) FROM filtros"),
            new Paso("Preparando tableros y reportes...", "SELECT COUNT(*) FROM bitacora"),
            new Paso("¡Todo listo! Bienvenido.", null));

    @FXML
    private StackPane raiz;
    @FXML
    private Pane globo;
    @FXML
    private StackPane spinnerBox;
    @FXML
    private Label lblEstado;
    @FXML
    private Label lblDetalle;
    @FXML
    private Button btnReintentar;

    private final List<Animation> animaciones = new ArrayList<>();
    private Animation transicionFrase;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        FondoUtil.aplicarFondo(raiz, FondoUtil.cargar(IMG + "Login_carga.png"));
        if (!cargarLogoAnimado()) {
            construirGloboDeReemplazo();
        }
        if (MOSTRAR_ICONOS_CRUD) {
            agregarIconosCrud();
        }
        if (!cargarSpinnerGif()) {
            construirSpinnerDeReemplazo();
        }
        iniciarCarga();
    }

    // ------------------------------------------------------------------ carga
    private void iniciarCarga() {
        btnReintentar.setVisible(false);
        btnReintentar.setManaged(false);
        lblDetalle.setStyle("-fx-text-fill: rgba(255,255,255,0.80); -fx-font-size: 14px;");
        lblDetalle.setOpacity(1);
        lblDetalle.setText(PASOS.get(0).frase());

        Task<Void> carga = new Task<>() {
            @Override
            protected Void call() throws Exception {
                try (Connection c = ConexionBD.conectar()) {
                    for (Paso p : PASOS) {
                        long t0 = System.currentTimeMillis();
                        updateMessage(p.frase());
                        if (p.sql() != null) {
                            try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(p.sql())) {
                                rs.next();
                            }
                        }
                        long resto = MS_POR_PASO - (System.currentTimeMillis() - t0);
                        if (resto > 0) {
                            Thread.sleep(resto);
                        }
                    }
                    Thread.sleep(MS_FINAL);
                }
                return null;
            }
        };
        carga.messageProperty().addListener((o, anterior, nueva) -> {
            if (nueva != null && !nueva.equals(lblDetalle.getText())) {
                cambiarFrase(nueva);
            }
        });
        carga.setOnSucceeded(e -> irAlLogin());
        carga.setOnFailed(e -> {
            Throwable ex = carga.getException();
            if (transicionFrase != null) {
                transicionFrase.stop();
            }
            lblDetalle.setOpacity(1);
            lblDetalle.setStyle("-fx-text-fill: #ffb4ae; -fx-font-size: 14px; -fx-font-weight: bold;");
            lblDetalle.setText("No se pudo abrir la base de datos: " + (ex != null ? ex.getMessage() : "error desconocido"));
            btnReintentar.setVisible(true);
            btnReintentar.setManaged(true);
        });
        Thread hilo = new Thread(carga, "carga-bd");
        hilo.setDaemon(true);
        hilo.start();
    }

    /** Cambia la frase con un pequeno fundido para que no sea brusco. */
    private void cambiarFrase(String frase) {
        if (transicionFrase != null) {
            transicionFrase.stop();
        }
        FadeTransition salida = new FadeTransition(Duration.millis(130), lblDetalle);
        salida.setToValue(0);
        salida.setOnFinished(e -> {
            lblDetalle.setText(frase);
            FadeTransition entrada = new FadeTransition(Duration.millis(190), lblDetalle);
            entrada.setToValue(1);
            transicionFrase = entrada;
            entrada.play();
        });
        transicionFrase = salida;
        salida.play();
    }

    @FXML
    private void onReintentarClick() {
        iniciarCarga();
    }

    private void irAlLogin() {
        FadeTransition salida = new FadeTransition(Duration.millis(350), raiz);
        salida.setFromValue(1);
        salida.setToValue(0);
        salida.setOnFinished(e -> {
            animaciones.forEach(Animation::stop);
            SceneManager.cambiarA(SceneManager.LOGIN);
        });
        salida.play();
    }

    // ------------------------------------------------------- logo (video) / gif
    /** Reproduce logo_ryr_sprite.png (video sin fondo negro, convertido con tools/convertir_logo.py). */
    private boolean cargarLogoAnimado() {
        URL png = getClass().getResource(IMG + "logo_ryr_sprite.png");
        URL cfg = getClass().getResource(IMG + "logo_ryr_sprite.properties");
        if (png == null || cfg == null) {
            return false;
        }
        try (InputStream in = cfg.openStream()) {
            Properties p = new Properties();
            p.load(in);
            int cols = Integer.parseInt(p.getProperty("cols").trim());
            int cuadros = Integer.parseInt(p.getProperty("frames").trim());
            int fw = Integer.parseInt(p.getProperty("frameWidth").trim());
            int fh = Integer.parseInt(p.getProperty("frameHeight").trim());
            double fps = Double.parseDouble(p.getProperty("fps").trim());

            ImageView iv = new ImageView(new Image(png.toExternalForm()));
            iv.setSmooth(true);
            iv.setPreserveRatio(true);
            iv.setViewport(new Rectangle2D(0, 0, fw, fh));
            double escala = Math.min(globo.getPrefWidth() / fw, globo.getPrefHeight() / fh);
            iv.setFitWidth(fw * escala);
            iv.setLayoutX((globo.getPrefWidth() - fw * escala) / 2);
            iv.setLayoutY((globo.getPrefHeight() - fh * escala) / 2);
            globo.getChildren().add(iv);

            SpriteAnimacion anim = new SpriteAnimacion(iv, cols, cuadros, fw, fh, fps);
            anim.play();
            animaciones.add(anim);
            return true;
        } catch (Exception e) {
            System.err.println("No se pudo cargar el logo animado: " + e);
            globo.getChildren().clear();
            return false;
        }
    }

    /** Carga.gif (JavaFX reproduce los GIF animados dentro de un ImageView). */
    private boolean cargarSpinnerGif() {
        URL gif = getClass().getResource(IMG + "Carga.gif");
        if (gif == null) {
            return false;
        }
        try {
            ImageView iv = new ImageView(new Image(gif.toExternalForm()));
            iv.setFitWidth(spinnerBox.getPrefWidth());
            iv.setFitHeight(spinnerBox.getPrefHeight());
            iv.setPreserveRatio(true);
            iv.setSmooth(true);
            spinnerBox.getChildren().add(iv);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ------------------------------------------------- iconos del wireframe
    private void agregarIconosCrud() {
        double cx = globo.getPrefWidth() / 2;
        double cy = globo.getPrefHeight() / 2;
        globo.getChildren().addAll(
                icono("M-6,0 H6 M0,-6 V6", cx - 20, cy - 12),
                ojo(cx + 20, cy - 12),
                icono("M5.4,-2.6 A6,6 0 1 0 6,1 M6.8,-6.4 L5.4,-2.6 L1.6,-3.6", cx - 20, cy + 16),
                icono("M-6.5,-4 H6.5 M-2,-4 V-6.5 H2 V-4 M-4.8,-4 L-3.6,6.5 H3.6 L4.8,-4 M-1.3,-1 V3.5 M1.3,-1 V3.5", cx + 20, cy + 16));
    }

    private SVGPath icono(String contorno, double x, double y) {
        SVGPath p = new SVGPath();
        p.setContent(contorno);
        p.setFill(Color.TRANSPARENT);
        p.setStroke(Color.WHITE);
        p.setStrokeWidth(2);
        p.setStrokeLineCap(StrokeLineCap.ROUND);
        p.setLayoutX(x);
        p.setLayoutY(y);
        p.setEffect(new DropShadow(4, Color.web("#000000", 0.55)));
        return p;
    }

    private Node ojo(double x, double y) {
        SVGPath contorno = icono("M-8,0 Q0,-8 8,0 Q0,8 -8,0 Z", 0, 0);
        Circle pupila = new Circle(0, 0, 2.6, Color.WHITE);
        Group g = new Group(contorno, pupila);
        g.setLayoutX(x);
        g.setLayoutY(y);
        return g;
    }

    // --------------------------------------- alternativas dibujadas por codigo
    private void construirSpinnerDeReemplazo() {
        Circle pista = new Circle(15);
        pista.setFill(Color.TRANSPARENT);
        pista.setStroke(Color.web("#ffffff", 0.22));
        pista.setStrokeWidth(4);

        Arc arco = new Arc(0, 0, 15, 15, 90, 270);
        arco.setType(ArcType.OPEN);
        arco.setFill(Color.TRANSPARENT);
        arco.setStroke(Color.web("#e8f0ff"));
        arco.setStrokeWidth(4);
        arco.setStrokeLineCap(StrokeLineCap.ROUND);

        Group grupo = new Group(arco);
        spinnerBox.getChildren().addAll(pista, grupo);

        RotateTransition giro = new RotateTransition(Duration.seconds(1), grupo);
        giro.setByAngle(360);
        giro.setCycleCount(Animation.INDEFINITE);
        giro.setInterpolator(Interpolator.LINEAR);
        giro.play();
        animaciones.add(giro);
    }

    private void construirGloboDeReemplazo() {
        final double cx = globo.getPrefWidth() / 2;
        final double cy = globo.getPrefHeight() / 2;
        final double r = 64;

        Color cian = Color.web("#22d3ee");
        Color rojo = Color.web("#ef4444");
        Color azul = Color.web("#3b82f6");

        Circle esfera = new Circle(cx, cy, r);
        esfera.setFill(new RadialGradient(0, 0, 0.35, 0.3, 0.9, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#6aa8ff", 0.45)),
                new Stop(1, Color.web("#0a2a6e", 0.18))));
        esfera.setStroke(Color.web("#7db4ff", 0.7));
        esfera.setStrokeWidth(1.2);
        globo.getChildren().add(esfera);

        for (int dy : new int[]{-36, -12, 12, 36}) {
            double rx = Math.sqrt(r * r - dy * dy);
            Ellipse p = new Ellipse(cx, cy + dy, rx, rx * 0.16);
            p.setFill(Color.TRANSPARENT);
            p.setStroke(Color.web("#a8cdff", 0.45));
            p.setStrokeWidth(0.9);
            globo.getChildren().add(p);
        }

        for (int i = 0; i < 4; i++) {
            Ellipse m = new Ellipse(cx, cy, r, r);
            m.setFill(Color.TRANSPARENT);
            m.setStroke(Color.web("#a8cdff", 0.55));
            m.setStrokeWidth(0.9);
            Timeline t = new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(m.radiusXProperty(), r, Interpolator.EASE_BOTH)),
                    new KeyFrame(Duration.seconds(2.4), new KeyValue(m.radiusXProperty(), 2, Interpolator.EASE_BOTH)));
            t.setAutoReverse(true);
            t.setCycleCount(Animation.INDEFINITE);
            t.play();
            t.jumpTo(Duration.seconds(i * 0.6));
            animaciones.add(t);
            globo.getChildren().add(m);
        }

        globo.getChildren().add(anillo(cx, cy, 112, 30, 10, 250, cian, -22, 7));
        globo.getChildren().add(anillo(cx, cy, 108, 26, 30, 235, rojo, 18, -9));
        globo.getChildren().add(anillo(cx, cy, 100, 22, 200, 240, azul, -6, 11));
    }

    private Group anillo(double cx, double cy, double rx, double ry, double inicio, double largo,
            Color color, double inclinacion, double segundosPorVuelta) {
        Arc arco = new Arc(cx, cy, rx, ry, inicio, largo);
        arco.setType(ArcType.OPEN);
        arco.setFill(Color.TRANSPARENT);
        arco.setStroke(color);
        arco.setStrokeWidth(5);
        arco.setStrokeLineCap(StrokeLineCap.ROUND);
        arco.setEffect(new DropShadow(10, color));

        Group inclinado = new Group(arco);
        inclinado.setRotate(inclinacion);
        double sentido = segundosPorVuelta < 0 ? -360 : 360;
        Timeline orbita = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(arco.startAngleProperty(), inicio, Interpolator.LINEAR)),
                new KeyFrame(Duration.seconds(Math.abs(segundosPorVuelta)),
                        new KeyValue(arco.startAngleProperty(), inicio + sentido, Interpolator.LINEAR)));
        orbita.setCycleCount(Animation.INDEFINITE);
        orbita.play();
        animaciones.add(orbita);
        return inclinado;
    }
}
