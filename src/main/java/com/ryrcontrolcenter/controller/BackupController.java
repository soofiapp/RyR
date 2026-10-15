package com.ryrcontrolcenter.controller;

import com.ryrcontrolcenter.config.BackupConfig;
import com.ryrcontrolcenter.config.BackupConfig.Unidad;
import com.ryrcontrolcenter.dao.UsuarioDao;
import com.ryrcontrolcenter.modelo.CopiaSeguridad;
import com.ryrcontrolcenter.modelo.CopiaSeguridad.Tipo;
import com.ryrcontrolcenter.modelo.Usuario;
import com.ryrcontrolcenter.service.BackupException;
import com.ryrcontrolcenter.service.BackupScheduler;
import com.ryrcontrolcenter.service.BackupService;
import com.ryrcontrolcenter.service.BitacoraService;
import com.ryrcontrolcenter.util.AlertaUtil;
import com.ryrcontrolcenter.util.SceneManager;
import com.ryrcontrolcenter.util.SesionActual;
import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Cursor;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

public class BackupController implements Initializable {

    public static final String VISTA = "/com/ryrcontrolcenter/ui/BackupVista.fxml";

    private static final String MODULO_BITACORA = "Usuarios y Bitacora";
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    private BorderPane mainPane;
    @FXML
    private Label lblResumen;
    @FXML
    private Label lblProxima;
    @FXML
    private Label lblMensaje;
    @FXML
    private Label lblCarpetaListado;
    @FXML
    private ProgressIndicator indicador;
    @FXML
    private Button btnCrear;
    @FXML
    private Button btnGuardarEn;
    @FXML
    private Button btnRestaurar;
    @FXML
    private Button btnRestaurarArchivo;
    @FXML
    private Button btnVerificar;
    @FXML
    private Button btnEliminar;
    @FXML
    private Button btnAbrirCarpeta;
    @FXML
    private Button btnCambiarCarpeta;
    @FXML
    private Button btnGuardarConfig;
    @FXML
    private TableView<CopiaSeguridad> tablaCopias;
    @FXML
    private TableColumn<CopiaSeguridad, String> colFecha;
    @FXML
    private TableColumn<CopiaSeguridad, String> colTipo;
    @FXML
    private TableColumn<CopiaSeguridad, String> colTamano;
    @FXML
    private TableColumn<CopiaSeguridad, String> colArchivo;
    @FXML
    private CheckBox chkAutomatica;
    @FXML
    private CheckBox chkAlCerrar;
    @FXML
    private Spinner<Integer> spnIntervalo;
    @FXML
    private Spinner<Integer> spnRetener;
    @FXML
    private ComboBox<Unidad> cmbUnidad;
    @FXML
    private TextField tfCarpeta;

    private Timeline actualizador;


    //  Acceso
    public static boolean puedeUsar() {
        return SesionActual.puedeEditar();
    }

    public static void abrir() {
        if (!puedeUsar()) {
            AlertaUtil.mostrar("Acceso restringido",
                    "Las copias de seguridad solo pueden gestionarlas los usuarios con permisos de edición.\n"
                    + "Su perfil actual es de solo lectura.", Alert.AlertType.WARNING);
            return;
        }
        SceneManager.cambiarA(VISTA);
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fechaTexto"));
        colTipo.setCellValueFactory(new PropertyValueFactory<>("tipoTexto"));
        colTamano.setCellValueFactory(new PropertyValueFactory<>("tamanoTexto"));
        colArchivo.setCellValueFactory(new PropertyValueFactory<>("nombreArchivo"));
        tablaCopias.setPlaceholder(new Label("Todavía no hay copias en esta carpeta."));

        spnIntervalo.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 9999, 1));
        spnRetener.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(
                BackupConfig.RETENER_MIN, BackupConfig.RETENER_MAX, 10));
        confirmarAlPerderFoco(spnIntervalo);
        confirmarAlPerderFoco(spnRetener);
        cmbUnidad.setItems(FXCollections.observableArrayList(Unidad.values()));

        spnIntervalo.disableProperty().bind(chkAutomatica.selectedProperty().not());
        cmbUnidad.disableProperty().bind(chkAutomatica.selectedProperty().not());
        spnRetener.disableProperty().bind(chkAutomatica.selectedProperty().not());
        chkAlCerrar.disableProperty().bind(chkAutomatica.selectedProperty().not());

        var sinSeleccion = tablaCopias.getSelectionModel().selectedItemProperty().isNull();
        btnRestaurar.disableProperty().bind(sinSeleccion);
        btnVerificar.disableProperty().bind(sinSeleccion);
        btnEliminar.disableProperty().bind(sinSeleccion);

        cargarConfigEnPantalla();
        refrescar();

        actualizador = new Timeline(new KeyFrame(javafx.util.Duration.seconds(30), e -> {
            if (mainPane.getScene() == null) {
                actualizador.stop();
            } else {
                refrescar();
            }
        }));
        actualizador.setCycleCount(Timeline.INDEFINITE);
        actualizador.play();
    }

    private static void confirmarAlPerderFoco(Spinner<Integer> spinner) {
        spinner.focusedProperty().addListener((obs, antes, enfocado) -> {
            if (!enfocado) {
                confirmarTexto(spinner);
            }
        });
    }

    private static void confirmarTexto(Spinner<Integer> spinner) {
        try {
            spinner.increment(0);
        } catch (RuntimeException e) {
            spinner.getEditor().setText(String.valueOf(spinner.getValue()));
        }
    }

    private void cargarConfigEnPantalla() {
        BackupConfig cfg = BackupConfig.cargar();
        chkAutomatica.setSelected(cfg.isAutomatica());
        spnIntervalo.getValueFactory().setValue(cfg.getIntervaloValor());
        cmbUnidad.getSelectionModel().select(cfg.getIntervaloUnidad());
        spnRetener.getValueFactory().setValue(cfg.getRetener());
        chkAlCerrar.setSelected(cfg.isCopiaAlCerrar());
        tfCarpeta.setText(cfg.getCarpeta().toAbsolutePath().toString());
    }

    //  Lista y estado
    private void refrescar() {
        BackupConfig cfg = BackupConfig.cargar();
        Path carpeta = cfg.getCarpeta();
        CopiaSeguridad seleccionada = tablaCopias.getSelectionModel().getSelectedItem();
        List<CopiaSeguridad> copias = BackupService.listar(carpeta);
        tablaCopias.setItems(FXCollections.observableArrayList(copias));
        if (seleccionada != null) {
            copias.stream().filter(c -> c.getArchivo().equals(seleccionada.getArchivo())).findFirst()
                    .ifPresent(c -> tablaCopias.getSelectionModel().select(c));
        }
        lblCarpetaListado.setText("Copias disponibles en: " + carpeta.toAbsolutePath());

        if (copias.isEmpty()) {
            lblResumen.setText("⚠ Todavía no hay copias de seguridad. Cree una ahora para proteger la información.");
        } else {
            CopiaSeguridad ultima = copias.get(0);
            lblResumen.setText("Última copia: " + FMT.format(ultima.getFecha()) + " (" + ultima.getTipoTexto()
                    + ") · " + copias.size() + " copia(s) guardada(s)" + antiguedad(ultima.getFecha()));
        }

        if (cfg.isAutomatica()) {
            LocalDateTime proxima = BackupScheduler.proximaCopia(cfg, LocalDateTime.now());
            lblProxima.setText("Modo automático ACTIVADO · una copia cada " + cfg.getIntervaloValor() + " "
                    + cfg.getIntervaloUnidad().toString().toLowerCase() + " · próxima copia: "
                    + (proxima == null ? "—" : FMT.format(proxima)));
        } else {
            lblProxima.setText("Modo automático desactivado: las copias solo se hacen cuando usted lo pida.");
        }
    }

    private static String antiguedad(LocalDateTime fecha) {
        long horas = Duration.between(fecha, LocalDateTime.now()).toHours();
        if (horas < 24) {
            return "";
        }
        return " · hace " + (horas / 24) + " día(s)";
    }

    @FXML
    private void onCrearCopiaClick(ActionEvent event) {
        Path carpeta = BackupConfig.cargar().getCarpeta();
        ejecutar("Creando copia de seguridad…",
                () -> BackupService.crearCopia(Tipo.MANUAL, carpeta),
                copia -> {
                    BitacoraService.registrar("Copia de seguridad manual creada: " + copia.getNombreArchivo(),
                            MODULO_BITACORA, copia.getNombreArchivo());
                    refrescar();
                    exito("Copia creada: " + copia.getNombreArchivo() + " (" + copia.getTamanoTexto() + ")");
                });
    }

    @FXML
    private void onGuardarEnClick(ActionEvent event) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Guardar copia de seguridad en…");
        fc.setInitialFileName(CopiaSeguridad.nombreArchivo(LocalDateTime.now(), Tipo.MANUAL, 1));
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Base de datos SQLite (*.db)", "*.db"));
        File inicial = BackupConfig.cargar().getCarpeta().toAbsolutePath().toFile();
        if (inicial.isDirectory()) {
            fc.setInitialDirectory(inicial);
        }
        File destino = fc.showSaveDialog(mainPane.getScene().getWindow());
        if (destino == null) {
            return;
        }
        ejecutar("Guardando copia…",
                () -> BackupService.guardarCopiaEn(destino.toPath()),
                ruta -> {
                    BitacoraService.registrar("Copia de seguridad manual guardada en " + ruta,
                            MODULO_BITACORA, ruta.getFileName().toString());
                    refrescar();
                    exito("Copia guardada en: " + ruta);
                });
    }

    @FXML
    private void onRestaurarClick(ActionEvent event) {
        CopiaSeguridad sel = tablaCopias.getSelectionModel().getSelectedItem();
        if (sel == null) {
            return;
        }
        confirmarYRestaurar(sel.getArchivo(),
                "la copia del " + FMT.format(sel.getFecha()) + " (" + sel.getTipoTexto() + ")");
    }

    @FXML
    private void onRestaurarArchivoClick(ActionEvent event) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Seleccionar copia de seguridad a restaurar");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Bases de datos SQLite (*.db)", "*.db"),
                new FileChooser.ExtensionFilter("Todos los archivos", "*.*"));
        File inicial = BackupConfig.cargar().getCarpeta().toAbsolutePath().toFile();
        if (inicial.isDirectory()) {
            fc.setInitialDirectory(inicial);
        }
        File archivo = fc.showOpenDialog(mainPane.getScene().getWindow());
        if (archivo != null) {
            confirmarYRestaurar(archivo.toPath(), "el archivo \"" + archivo.getName() + "\"");
        }
    }

    private record Comparacion(Map<String, Integer> copia, Map<String, Integer> actual) {
    }

    private void confirmarYRestaurar(Path archivo, String descripcion) {
        ejecutar("Verificando la copia…", () -> {
            BackupService.verificar(archivo);
            return new Comparacion(BackupService.describir(archivo), BackupService.describirActual());
        }, comp -> {
            String texto = "Se restaurará " + descripcion + ".\n\n"
                    + "Contenido de la copia:\n   " + resumenConteo(comp.copia()) + "\n"
                    + "Contenido actual:\n   " + resumenConteo(comp.actual()) + "\n\n"
                    + "Se reemplazarán TODOS los datos actuales (usuarios, inventario, préstamos, kárdex y bitácora) "
                    + "por los de la copia. Lo registrado después de esa copia se perderá.\n\n"
                    + "Antes de restaurar se guardará automáticamente una copia del estado actual "
                    + "(tipo \"Previa a restaurar\") por si necesita deshacer.\n"
                    + "Al terminar se cerrará la sesión.";
            if (!confirmarRestauracion(texto)) {
                return;
            }
            Path carpetaPrevia = BackupConfig.cargar().getCarpeta();
            ejecutar("Restaurando… no cierre la aplicación.",
                    () -> BackupService.restaurar(archivo, carpetaPrevia),
                    previa -> alRestaurar(archivo, previa));
        });
    }

    private void alRestaurar(Path archivo, CopiaSeguridad previa) {
        Usuario u = SesionActual.getUsuario();
        if (u != null && new UsuarioDao().buscarPorId(u.getIdUsuario()) != null) {
            BitacoraService.registrar("Restauración de copia de seguridad: " + archivo.getFileName(),
                    MODULO_BITACORA, archivo.getFileName().toString());
        }
        AlertaUtil.mostrar("Restauración completada",
                "La base de datos se restauró correctamente.\n\n"
                + "Si necesita deshacer, restaure la copia \"" + previa.getNombreArchivo() + "\".\n"
                + "Se cerrará la sesión para cargar los datos restaurados.", Alert.AlertType.INFORMATION);
        SesionActual.cerrar();
        SceneManager.cambiarA(SceneManager.LOGIN);
    }

    private boolean confirmarRestauracion(String texto) {
        ButtonType restaurar = new ButtonType("Restaurar", ButtonBar.ButtonData.OK_DONE);
        Alert alerta = new Alert(Alert.AlertType.WARNING, texto, restaurar, ButtonType.CANCEL);
        alerta.setTitle("Restaurar copia de seguridad");
        alerta.setHeaderText("Esta acción reemplaza los datos actuales");
        alerta.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        alerta.initOwner(mainPane.getScene().getWindow());
        ((Button) alerta.getDialogPane().lookupButton(restaurar)).setDefaultButton(false);
        ((Button) alerta.getDialogPane().lookupButton(ButtonType.CANCEL)).setDefaultButton(true);
        return alerta.showAndWait().filter(b -> b == restaurar).isPresent();
    }

    private static String resumenConteo(Map<String, Integer> conteo) {
        return "usuarios " + n(conteo, "usuarios") + " · inventario " + n(conteo, "inventario")
                + " · préstamos " + n(conteo, "prestamos") + " · filtros " + n(conteo, "filtros")
                + " · kárdex " + n(conteo, "movimientos_kardex") + " · bitácora " + n(conteo, "bitacora");
    }

    private static String n(Map<String, Integer> conteo, String tabla) {
        Integer v = conteo.get(tabla);
        return v == null || v < 0 ? "—" : v.toString();
    }

    @FXML
    private void onVerificarClick(ActionEvent event) {
        CopiaSeguridad sel = tablaCopias.getSelectionModel().getSelectedItem();
        if (sel == null) {
            return;
        }
        ejecutar("Verificando la copia…", () -> {
            BackupService.verificar(sel.getArchivo());
            return BackupService.describir(sel.getArchivo());
        }, conteo -> {
            exito("La copia \"" + sel.getNombreArchivo() + "\" es válida y se puede restaurar.");
            AlertaUtil.mostrar("Copia verificada",
                    "La copia es válida (integridad correcta y compatible con esta versión).\n\nContiene:\n   "
                    + resumenConteo(conteo), Alert.AlertType.INFORMATION);
        });
    }

    @FXML
    private void onEliminarClick(ActionEvent event) {
        CopiaSeguridad sel = tablaCopias.getSelectionModel().getSelectedItem();
        if (sel == null) {
            return;
        }
        if (!AlertaUtil.confirmar("Eliminar copia",
                "¿Eliminar definitivamente la copia \"" + sel.getNombreArchivo() + "\"?")) {
            return;
        }
        ejecutar("Eliminando…", () -> {
            BackupService.eliminar(sel);
            return sel;
        }, copia -> {
            BitacoraService.registrar("Copia de seguridad eliminada: " + copia.getNombreArchivo(),
                    MODULO_BITACORA, copia.getNombreArchivo());
            refrescar();
            exito("Copia eliminada.");
        });
    }

    @FXML
    private void onAbrirCarpetaClick(ActionEvent event) {
        Path carpeta = BackupConfig.cargar().getCarpeta().toAbsolutePath();
        try {
            Files.createDirectories(carpeta);
            if (java.awt.Desktop.isDesktopSupported()
                    && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.OPEN)) {
                Thread t = new Thread(() -> {
                    try {
                        java.awt.Desktop.getDesktop().open(carpeta.toFile());
                    } catch (Exception e) {
                        Platform.runLater(() -> error("No se pudo abrir la carpeta: " + e.getMessage()));
                    }
                }, "ryr-abrir-carpeta");
                t.setDaemon(true);
                t.start();
            } else {
                exito("Carpeta de copias: " + carpeta);
            }
        } catch (Exception e) {
            error("No se pudo abrir la carpeta: " + e.getMessage());
        }
    }

    @FXML
    private void onCambiarCarpetaClick(ActionEvent event) {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Carpeta donde se guardarán las copias");
        File actual = new File(tfCarpeta.getText());
        if (actual.isDirectory()) {
            dc.setInitialDirectory(actual);
        }
        File elegida = dc.showDialog(mainPane.getScene().getWindow());
        if (elegida != null) {
            tfCarpeta.setText(elegida.getAbsolutePath());
            exito("Carpeta elegida. Pulse \"Guardar configuración\" para aplicarla.");
        }
    }

    @FXML
    private void onGuardarConfigClick(ActionEvent event) {
        confirmarTexto(spnIntervalo);
        confirmarTexto(spnRetener);
        BackupConfig cfg = BackupConfig.cargar();
        cfg.setAutomatica(chkAutomatica.isSelected());
        cfg.setIntervaloValor(spnIntervalo.getValue());
        cfg.setIntervaloUnidad(cmbUnidad.getValue() == null ? Unidad.DIAS : cmbUnidad.getValue());
        cfg.setRetener(spnRetener.getValue());
        cfg.setCopiaAlCerrar(chkAlCerrar.isSelected());
        cfg.setCarpeta(Path.of(tfCarpeta.getText()));

        String problema = cfg.validar();
        if (problema != null) {
            error(problema);
            return;
        }
        try {
            Path dir = cfg.getCarpeta().toAbsolutePath();
            Files.createDirectories(dir);
            if (!Files.isWritable(dir)) {
                error("No se puede escribir en la carpeta elegida: " + dir);
                return;
            }
            cfg.guardar();
        } catch (Exception e) {
            error("No se pudo guardar la configuración: " + e.getMessage());
            return;
        }
        BitacoraService.registrar("Configuración de copias de seguridad actualizada (automática: "
                + (cfg.isAutomatica() ? "sí, cada " + cfg.getIntervaloValor() + " " + cfg.getIntervaloUnidad() : "no") + ")",
                MODULO_BITACORA, "configuracion");
        if (cfg.isAutomatica()) {
            BackupScheduler.revisarAhora();
        }
        refrescar();
        exito(cfg.isAutomatica()
                ? "Configuración guardada. El modo automático está activado."
                : "Configuración guardada. El modo automático está desactivado.");
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
        SesionActual.cerrar();
        SceneManager.cambiarA(SceneManager.LOGIN);
    }

    private <T> void ejecutar(String textoProgreso, Callable<T> trabajo, Consumer<T> alExito) {
        ocupado(true, textoProgreso);
        Task<T> tarea = new Task<>() {
            @Override
            protected T call() throws Exception {
                return trabajo.call();
            }
        };
        tarea.setOnSucceeded(e -> {
            ocupado(false, null);
            alExito.accept(tarea.getValue());
        });
        tarea.setOnFailed(e -> {
            ocupado(false, null);
            Throwable ex = tarea.getException();
            String msg = ex instanceof BackupException ? ex.getMessage() : "Error inesperado: " + ex;
            error(msg);
            AlertaUtil.mostrar("Copias de seguridad", msg, Alert.AlertType.ERROR);
            refrescar();
        });
        Thread hilo = new Thread(tarea, "ryr-backup-ui");
        hilo.setDaemon(true);
        hilo.start();
    }

    private void ocupado(boolean ocupado, String texto) {
        mainPane.setDisable(ocupado);
        mainPane.setCursor(ocupado ? Cursor.WAIT : Cursor.DEFAULT);
        indicador.setVisible(ocupado);
        indicador.setManaged(ocupado);
        if (ocupado) {
            lblMensaje.setStyle("-fx-text-fill: #6b7280;");
            lblMensaje.setText(texto);
        }
    }

    private void exito(String texto) {
        lblMensaje.setStyle("-fx-text-fill: #2E7D32; -fx-font-weight: bold;");
        lblMensaje.setText(texto);
    }

    private void error(String texto) {
        lblMensaje.setStyle("-fx-text-fill: #d1453b; -fx-font-weight: bold;");
        lblMensaje.setText(texto);
    }
}
