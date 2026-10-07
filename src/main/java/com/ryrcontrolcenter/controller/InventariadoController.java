package com.ryrcontrolcenter.controller;

import com.ryrcontrolcenter.dao.PrestamoDao;
import com.ryrcontrolcenter.modelo.Inventario;
import com.ryrcontrolcenter.service.BitacoraService;
import com.ryrcontrolcenter.service.InventarioService;
import com.ryrcontrolcenter.util.SceneManager;
import com.ryrcontrolcenter.util.SesionActual;
import java.io.IOException;
import java.net.URL;
import java.text.Normalizer;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
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

public class InventariadoController implements Initializable {

    @FXML
    private ComboBox<String> cmbFiltro;
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
    private TableColumn<Inventario, Integer> colStockActual;
    @FXML
    private TableColumn<Inventario, Integer> colPuntoReorden;
    @FXML
    private TableColumn<Inventario, String> colEstadoSst;
    @FXML
    private TableColumn<Inventario, String> colObservaciones;
    @FXML
    private BorderPane mainPane;

    private final InventarioService service = new InventarioService();
    private final ObservableList<Inventario> datosMaestros = FXCollections.observableArrayList();
    private FilteredList<Inventario> datosFiltrados;
    private final PrestamoDao prestamoDao = new PrestamoDao();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        colCodigo.setCellValueFactory(new PropertyValueFactory<>("idActivo"));
        colDescripcion.setCellValueFactory(new PropertyValueFactory<>("descripcion"));
        colTipo.setCellValueFactory(new PropertyValueFactory<>("tipoActivo"));
        colStockActual.setCellValueFactory(new PropertyValueFactory<>("stockDisponible"));
        colPuntoReorden.setCellValueFactory(new PropertyValueFactory<>("puntoReorden"));
        colEstadoSst.setCellValueFactory(new PropertyValueFactory<>("estadoSst"));
        colObservaciones.setCellValueFactory(new PropertyValueFactory<>("observaciones"));
        // Filtro: lista filtrada + ordenable al hacer clic en los encabezados
        datosFiltrados = new FilteredList<>(datosMaestros, i -> true);
        SortedList<Inventario> ordenados = new SortedList<>(datosFiltrados);
        ordenados.comparatorProperty().bind(tablaInventario.comparatorProperty());
        tablaInventario.setItems(ordenados);
        cmbFiltro.setItems(FXCollections.observableArrayList(
                "Todos", "Código", "Descripción", "Tipo", "Estado SST", "Observaciones"));
        cmbFiltro.getSelectionModel().selectFirst();
        txtBuscar.textProperty().addListener((obs, anterior, nuevo) -> aplicarFiltro());
        cmbFiltro.valueProperty().addListener((obs, anterior, nuevo) -> aplicarFiltro());

        if (!SesionActual.puedeEditar()) {
            btnAgregarActivo.setVisible(false);
            btnModificarActivo.setVisible(false);
            btnEliminarActivo.setVisible(false);
        }
        cargarDatosTabla();
    }

    // ---------- Filtro ----------
    private void aplicarFiltro() {
        String texto = normalizar(txtBuscar.getText());
        String campo = cmbFiltro.getValue() == null ? "Todos" : cmbFiltro.getValue();
        datosFiltrados.setPredicate(item -> {
            if (texto.isEmpty()) {
                return true;
            }
            return switch (campo) {
                case "Código" ->
                    coincide(item.getIdActivo(), texto);
                case "Descripción" ->
                    coincide(item.getDescripcion(), texto);
                case "Tipo" ->
                    coincide(item.getTipoActivo(), texto);
                case "Estado SST" ->
                    coincide(item.getEstadoSst(), texto);
                case "Observaciones" ->
                    coincide(item.getObservaciones(), texto);
                default ->
                    coincide(item.getIdActivo(), texto)
                    || coincide(item.getDescripcion(), texto)
                    || coincide(item.getTipoActivo(), texto)
                    || coincide(item.getEstadoSst(), texto)
                    || coincide(item.getUbicacion(), texto)
                    || coincide(item.getObservaciones(), texto);
            };
        });

        actualizarContador();
    }

    private boolean coincide(String valor, String textoNormalizado) {
        return normalizar(valor).contains(textoNormalizado);
    }

    /**
     * Minúsculas y sin tildes, para que "maquina" encuentre "Máquina".
     */
    private String normalizar(String s) {
        if (s == null) {
            return "";
        }
        return Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .trim();
    }

    private void actualizarContador() {
        lblTotalActivos.setText("Inventario General Registrado ("
                + datosFiltrados.size() + " de " + datosMaestros.size() + ")");
    }

    // ---------- Navegación ----------
    @FXML
    private void irADashboard(ActionEvent e) {
        SceneManager.cambiarA("/com/ryrcontrolcenter/ui/DashboardVista.fxml");
    }

    @FXML
    private void irAPrestamos(ActionEvent e) {
        SceneManager.cambiarA("/com/ryrcontrolcenter/ui/PrestamosVista.fxml");
    }

    @FXML
    private void irAInventariado(ActionEvent e) {
        SceneManager.cambiarA("/com/ryrcontrolcenter/ui/InventariadoVista.fxml");
    }

    @FXML
    private void irAMatrizFiltro(ActionEvent e) {
        SceneManager.cambiarA("/com/ryrcontrolcenter/ui/MatrizFiltroVista.fxml");
    }

    @FXML
    private void irAKardex(ActionEvent e) {
        SceneManager.cambiarA("/com/ryrcontrolcenter/ui/KardexVista.fxml");
    }

    @FXML
    private void irAUsuariosBitacora(ActionEvent e) {
        SceneManager.cambiarA("/com/ryrcontrolcenter/ui/UsuariosBitacoraVista.fxml");
    }

    @FXML
    private void onLogoutClick(ActionEvent e) {
        SesionActual.cerrar();
        SceneManager.cambiarA("/com/ryrcontrolcenter/ui/LoginVista.fxml");
    }

    // ---------- Tabla ----------
    private void cargarDatosTabla() {
        try {
            List<Inventario> lista = service.verItemsInventario();
            datosMaestros.setAll(lista);   // el filtro activo se mantiene
            aplicarFiltro();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ---------- Agregar ----------
    @FXML
    private void onAgregarActivoClick(ActionEvent event) {
        abrirFormulario(null);
    }

    // ---------- Modificar ----------
    @FXML
    private void onModificarActivoClick(ActionEvent event) {
        Inventario seleccionado = tablaInventario.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Sin selección",
                    "Selecciona un activo de la tabla para modificarlo.");
            return;
        }
        abrirFormulario(seleccionado);
    }

    // ---------- Eliminar ----------
    @FXML
    private void onEliminarActivoClick(ActionEvent event) {
        Inventario seleccionado = tablaInventario.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Sin selección",
                    "Selecciona un activo de la tabla para eliminarlo.");
            return;
        }

        if (prestamoDao.tienePrestamoActivo(seleccionado.getIdActivo())) {
            mostrarAlerta(Alert.AlertType.WARNING, "No se puede eliminar",
                    "Este activo tiene un préstamo abierto actualmente. Debe registrarse "
                    + "la devolución antes de poder eliminarlo.");
            return;
        }

        Alert confirmar = new Alert(Alert.AlertType.CONFIRMATION);
        confirmar.setTitle("Confirmar eliminación");
        confirmar.setHeaderText(null);
        confirmar.setContentText("¿Eliminar el activo " + seleccionado.getIdActivo()
                + " (" + seleccionado.getDescripcion() + ")?\nEsta acción no se puede deshacer.");

        Optional<ButtonType> respuesta = confirmar.showAndWait();
        if (respuesta.isPresent() && respuesta.get() == ButtonType.OK) {
            if (service.eliminarItemInventarioService(seleccionado.getIdActivo())) {
                BitacoraService.registrar(
                        "Eliminación de activo " + seleccionado.getIdActivo() + " (" + seleccionado.getDescripcion() + ")",
                        "Inventariado",
                        seleccionado.getIdActivo()
                );
                cargarDatosTabla();
            } else {
                mostrarAlerta(Alert.AlertType.ERROR, "No se pudo eliminar",
                        "Es posible que el activo tenga préstamos u otros registros asociados.");
            }
        }
    }

    // ---------- Ventana de formulario (agregar o modificar) ----------
    private void abrirFormulario(Inventario aEditar) {
        BoxBlur blur = new BoxBlur(5, 5, 3);
        mainPane.setEffect(blur);
        mainPane.setOpacity(0.6);
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/ryrcontrolcenter/ui/NuevoInventarioVista.fxml"));
            Parent root = loader.load();

            if (aEditar != null) {
                NuevoInventarioController ctrl = loader.getController();
                ctrl.setInventarioAEditar(aEditar);
            }

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(mainPane.getScene().getWindow());
            stage.setOnHidden(ev -> {
                mainPane.setEffect(null);
                mainPane.setOpacity(1.0);
                cargarDatosTabla();
            });
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            mainPane.setEffect(null);
            mainPane.setOpacity(1.0);
        }
    }
    
    private void mostrarAlerta(Alert.AlertType tipo, String titulo, String mensaje) {
        Alert a = new Alert(tipo);
        a.setTitle(titulo);
        a.setHeaderText(null);
        a.setContentText(mensaje);
        a.showAndWait();
    }
}
