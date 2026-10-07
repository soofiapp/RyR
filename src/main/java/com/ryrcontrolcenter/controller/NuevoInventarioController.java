package com.ryrcontrolcenter.controller;

import com.ryrcontrolcenter.dao.InventarioDAO;
import com.ryrcontrolcenter.dao.KitDetalleDao;
import com.ryrcontrolcenter.dao.MaquinaDao;
import com.ryrcontrolcenter.modelo.Inventario;
import com.ryrcontrolcenter.modelo.KitDetalle;
import com.ryrcontrolcenter.modelo.Maquina;
import com.ryrcontrolcenter.service.BitacoraService;
import com.ryrcontrolcenter.service.InventarioService;
import java.net.URL;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class NuevoInventarioController implements Initializable {

    private static final List<String> TIPOS_VALIDOS
            = Arrays.asList("Herramienta", "Maquinaria", "Kit Agrupado");
    private static final List<String> ESTADOS_VALIDOS
            = Arrays.asList("Operativa", "Bloqueada", "Mantenimiento", "Completo");

    @FXML
    private TextField txtIdActivo;
    @FXML
    private TextField txtDescripcion;
    @FXML
    private ComboBox<String> cmbTipoActivo;
    @FXML
    private TextField txtEstadoSst;
    @FXML
    private TextField txtUbicacion;
    @FXML
    private DatePicker dpFechaRegistro;
    @FXML
    private TextArea txtObservaciones;
    @FXML
    private TextField txtStockActual;
    @FXML
    private TextField txtPuntoReorden;
    @FXML
    private Button btnCancelar;
    @FXML
    private Button btnGuardar;
    @FXML
    private Label lblTitulo;
    @FXML
    private VBox seccionComponentesKit;
    @FXML
    private ComboBox<Inventario> cmbHerramientaDisponible;
    @FXML
    private TextField txtCantidadComponente;
    @FXML
    private TableView<KitDetalle> tablaComponentes;
    @FXML
    private TableColumn<KitDetalle, String> colComponente;
    @FXML
    private TableColumn<KitDetalle, Integer> colCantidadComponente;
    @FXML
    private TableColumn<KitDetalle, Void> colQuitar;
    @FXML
    private Button btnAgregarComponente;
    @FXML
    private VBox seccionFichaTecnica;
    @FXML
    private TextField txtMarca;
    @FXML
    private TextField txtModelo;
    @FXML
    private TextField txtNumeroSerie;
    @FXML
    private TextField txtHorometro;
    @FXML
    private TextField txtAreaDepartamento;

    private final InventarioService service = new InventarioService();
    private final InventarioDAO inventarioDao = new InventarioDAO();
    private final KitDetalleDao kitDetalleDao = new KitDetalleDao();
    private final List<KitDetalle> componentesActuales = new ArrayList<>();
    private final MaquinaDao maquinaDao = new MaquinaDao();

    private boolean modoEdicion = false;
    private String idOriginal;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        dpFechaRegistro.setValue(LocalDate.now());
        cmbTipoActivo.setItems(FXCollections.observableArrayList(TIPOS_VALIDOS));
        cmbTipoActivo.getSelectionModel().selectedItemProperty().addListener((obs, anterior, nuevo) -> {
            boolean esKit = "Kit Agrupado".equals(nuevo);
            seccionComponentesKit.setVisible(esKit);
            seccionComponentesKit.setManaged(esKit);
            if (esKit) {
                cargarHerramientasDisponibles();
            }
            boolean esMaquina = "Maquinaria".equals(nuevo);
            seccionFichaTecnica.setVisible(esMaquina);
            seccionFichaTecnica.setManaged(esMaquina);
        });
        configurarTablaComponentes();
    }

    private void configurarTablaComponentes() {
        colComponente.setCellValueFactory(new PropertyValueFactory<>("descripcionHerramienta"));
        colCantidadComponente.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
        colQuitar.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("Quitar");

            {
                btn.setOnAction(e -> {
                    KitDetalle item = getTableView().getItems().get(getIndex());
                    componentesActuales.remove(item);
                    tablaComponentes.setItems(FXCollections.observableArrayList(componentesActuales));
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });
        tablaComponentes.setItems(FXCollections.observableArrayList(componentesActuales));
    }

    private void cargarHerramientasDisponibles() {
        cmbHerramientaDisponible.setConverter(new javafx.util.StringConverter<Inventario>() {
            @Override
            public String toString(Inventario i) {
                return i == null ? "" : i.getIdActivo() + " - " + i.getDescripcion();
            }

            @Override
            public Inventario fromString(String s) {
                return null;
            }
        });
        try {
            List<Inventario> herramientas = inventarioDao.listarTodos().stream()
                    .filter(i -> "Herramienta".equals(i.getTipoActivo()))
                    .toList();
            cmbHerramientaDisponible.setItems(FXCollections.observableArrayList(herramientas));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onAgregarComponenteClick(ActionEvent event) {
        Inventario seleccionada = cmbHerramientaDisponible.getSelectionModel().getSelectedItem();
        if (seleccionada == null) {
            mostrarAlerta("Atención", "Seleccione una herramienta para agregar al kit.");
            return;
        }
        int cantidad;
        try {
            cantidad = Integer.parseInt(txtCantidadComponente.getText().trim());
            if (cantidad <= 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            mostrarAlerta("Cantidad inválida", "Ingrese una cantidad válida (mayor a 0).");
            return;
        }

        KitDetalle kd = new KitDetalle();
        kd.setIdHerramienta(seleccionada.getIdActivo());
        kd.setDescripcionHerramienta(seleccionada.getIdActivo() + " - " + seleccionada.getDescripcion());
        kd.setCantidad(cantidad);

        componentesActuales.add(kd);
        tablaComponentes.setItems(FXCollections.observableArrayList(componentesActuales));
        txtCantidadComponente.clear();
    }

    /**
     * Llama a este método antes de mostrar la ventana para entrar en modo
     * modificar.
     */
    public void setInventarioAEditar(Inventario item) {
        modoEdicion = true;
        idOriginal = item.getIdActivo();

        lblTitulo.setText("MODIFICAR ACTIVO");
        btnGuardar.setText("Actualizar");

        txtIdActivo.setText(item.getIdActivo());
        txtIdActivo.setDisable(true);
        txtDescripcion.setText(item.getDescripcion());
        cmbTipoActivo.getSelectionModel().select(item.getTipoActivo());
        txtEstadoSst.setText(item.getEstadoSst());
        txtUbicacion.setText(item.getUbicacion());
        txtObservaciones.setText(item.getObservaciones() == null ? "" : item.getObservaciones());
        txtStockActual.setText(String.valueOf(item.getStockActual()));
        txtPuntoReorden.setText(String.valueOf(item.getPuntoReorden()));

        try {
            String f = item.getFechaRegistro();
            if (f != null && f.length() >= 10) {
                dpFechaRegistro.setValue(LocalDate.parse(f.substring(0, 10)));
            }
        } catch (Exception e) {
            dpFechaRegistro.setValue(LocalDate.now());
        }

        if ("Kit Agrupado".equals(item.getTipoActivo())) {
            List<KitDetalle> existentes = kitDetalleDao.listarPorKit(idOriginal);
            componentesActuales.clear();
            componentesActuales.addAll(existentes);
            tablaComponentes.setItems(FXCollections.observableArrayList(componentesActuales));
        }
        if ("Maquinaria".equals(item.getTipoActivo())) {
            Maquina m = maquinaDao.buscarPorId(item.getIdActivo());
            if (m != null) {
                txtMarca.setText(m.getMarca());
                txtModelo.setText(m.getModelo());
                txtNumeroSerie.setText(m.getNumero_serie());
                txtHorometro.setText(String.valueOf(m.getHorometro()));
                txtAreaDepartamento.setText(m.getAreaDepartamento());
            }
        }
    }

    @FXML
    private void onCancelarClick(ActionEvent event) {
        cerrarVentana();
    }

    @FXML
    private void onGuardarClick(ActionEvent event) {
        String idActivo = txtIdActivo.getText().trim();
        String descripcion = txtDescripcion.getText().trim();
        String tipoActivo = cmbTipoActivo.getSelectionModel().getSelectedItem();
        String estadoIngresado = txtEstadoSst.getText().trim();
        String ubicacion = txtUbicacion.getText().trim();
        String observaciones = txtObservaciones.getText().trim();
        LocalDate fecha = dpFechaRegistro.getValue();

        if (idActivo.isEmpty() || descripcion.isEmpty() || tipoActivo == null
                || estadoIngresado.isEmpty() || ubicacion.isEmpty()) {
            mostrarAlerta("Campos incompletos",
                    "Código, descripción, tipo, estado y ubicación son obligatorios.");
            return;
        }

        String estadoSst = buscarValido(estadoIngresado, ESTADOS_VALIDOS);
        if (estadoSst == null) {
            mostrarAlerta("Estado no válido",
                    "El estado debe ser uno de: " + String.join(", ", ESTADOS_VALIDOS));
            return;
        }

        if (fecha == null) {
            mostrarAlerta("Fecha requerida", "Selecciona la fecha de registro.");
            return;
        }

        int stockActual, puntoReorden;
        try {
            stockActual = Integer.parseInt(txtStockActual.getText().trim());
            puntoReorden = Integer.parseInt(txtPuntoReorden.getText().trim());
            if (stockActual < 0 || puntoReorden < 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            mostrarAlerta("Valores inválidos", "Stock y punto de reorden deben ser números positivos.");
            return;
        }

        boolean esKit = "Kit Agrupado".equals(tipoActivo);
        if (esKit && componentesActuales.isEmpty()) {
            mostrarAlerta("Kit sin componentes", "Agregue al menos un componente al kit.");
            return;
        }
        boolean esMaquina = "Maquinaria".equals(tipoActivo);
        double horometro = 0.0;
        if (esMaquina) {
            if (txtMarca.getText().trim().isEmpty() || txtModelo.getText().trim().isEmpty()) {
                mostrarAlerta("Datos incompletos", "Marca y modelo son obligatorios para una máquina.");
                return;
            }
            try {
                horometro = Double.parseDouble(txtHorometro.getText().trim());
            } catch (NumberFormatException e) {
                mostrarAlerta("Horómetro inválido", "El horómetro debe ser un número.");
                return;
            }
        }
        Inventario item = new Inventario();
        item.setIdActivo(idActivo);
        item.setDescripcion(descripcion);
        item.setTipoActivo(tipoActivo);
        item.setEstadoSst(estadoSst);
        item.setUbicacion(ubicacion);
        item.setFechaRegistro(fecha.toString());
        item.setObservaciones(observaciones);
        item.setStockActual(esKit ? 1 : stockActual); // un kit es 1 unidad agrupada
        item.setPuntoReorden(puntoReorden);
        boolean ok = modoEdicion
                ? service.actualizarItemInventarioService(idOriginal, item)
                : service.agregarItemInventarioService(item);
        if (!ok) {
            mostrarAlerta("Error", modoEdicion
                    ? "No se pudo actualizar el activo."
                    : "No se pudo guardar. Revisa si el código ya existe.");
            return;
        }
        if (esMaquina) {

            Maquina maquina = new Maquina();

            maquina.setIdMaquina(idActivo);
            maquina.setMarca(txtMarca.getText().trim());
            maquina.setModelo(txtModelo.getText().trim());
            maquina.setNumero_serie(txtNumeroSerie.getText().trim());
            maquina.setHorometro(horometro);
            maquina.setAreaDepartamento(txtAreaDepartamento.getText().trim());

            boolean okMaquina;

            if (modoEdicion) {
                okMaquina = maquinaDao.actualizar(maquina);
            } else {
                okMaquina = maquinaDao.insertar(maquina);
            }

            if (!okMaquina) {
                mostrarAlerta(
                        "Error",
                        modoEdicion
                                ? "El inventario se actualizó, pero no se pudo actualizar la ficha de la máquina."
                                : "El inventario se creó, pero no se pudo guardar la ficha de la máquina."
                );
                return;
            }
        }
        if (esKit) {
            guardarComponentesKit(idActivo);
        }

        String accion = modoEdicion
                ? "Modificación de activo " + idActivo + " (" + descripcion + ")"
                : "Creación de activo " + idActivo + " (" + descripcion + ")";
        BitacoraService.registrar(accion, "Inventariado", idActivo);
        cerrarVentana();
    }

    /**
     * Reemplaza por completo los componentes guardados del kit por la lista
     * actual en pantalla (se borran los que ya no estan y se insertan los
     * nuevos).
     */
    private void guardarComponentesKit(String idKit) {
        List<KitDetalle> yaGuardados = kitDetalleDao.listarPorKit(idKit);
        for (KitDetalle existente : yaGuardados) {
            kitDetalleDao.eliminar(existente.getIdDetalle());
        }
        for (KitDetalle kd : componentesActuales) {
            kd.setIdKit(idKit);
            kitDetalleDao.insertar(kd);
        }
    }

    private String buscarValido(String texto, List<String> validos) {
        for (String v : validos) {
            if (v.equalsIgnoreCase(texto)) {
                return v;
            }
        }
        return null;
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle(titulo);
        a.setHeaderText(null);
        a.setContentText(mensaje);
        a.showAndWait();
    }

    private void cerrarVentana() {
        ((Stage) btnCancelar.getScene().getWindow()).close();
    }
}
