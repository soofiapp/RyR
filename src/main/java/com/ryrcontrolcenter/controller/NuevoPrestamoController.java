package com.ryrcontrolcenter.controller;

import com.ryrcontrolcenter.dao.InventarioDAO;
import com.ryrcontrolcenter.dao.KitDetalleDao;
import com.ryrcontrolcenter.dao.PrestamoDao;
import com.ryrcontrolcenter.modelo.Inventario;
import com.ryrcontrolcenter.modelo.Prestamos;
import com.ryrcontrolcenter.service.BitacoraService;
import com.ryrcontrolcenter.util.AlertaUtil;
import com.ryrcontrolcenter.util.SesionActual;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class NuevoPrestamoController implements Initializable {

    @FXML
    private RadioButton rbMaquinaria;
    @FXML
    private RadioButton rbHerramientaUnica;
    @FXML
    private RadioButton rbKitCompleto;
    @FXML
    private ToggleGroup grupoTipoItem;
    @FXML
    private TextField txtOperario;
    @FXML
    private TextField txtOperarioCedula;
    @FXML
    private ComboBox<String> cmbActivoDisponible;
    @FXML
    private DatePicker dpFechaSalida;
    @FXML
    private DatePicker dpDevolucionEstimada;
    @FXML
    private TextField txtUbicacion;
    @FXML
    private TextArea txtObservaciones;
    @FXML
    private Label lblError;
    @FXML
    private Button btnCancelar;
    @FXML
    private Button btnAutorizarSalida;
    @FXML
    private TextField txtCantidad;

    private final PrestamoDao prestamoDao = new PrestamoDao();
    private final InventarioDAO inventarioDao = new InventarioDAO();
    private Prestamos prestamoEdicion = null;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final KitDetalleDao kitDetalleDao = new KitDetalleDao();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        try {
            if (dpFechaSalida != null) {
                dpFechaSalida.setValue(LocalDate.now());
            }
            if (cmbActivoDisponible != null) {
                cmbActivoDisponible.setEditable(false);
            }
            if (grupoTipoItem != null) {
                grupoTipoItem.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
                    actualizarOpcionesCombo();
                });
            }
            actualizarOpcionesCombo();
        } catch (Exception e) {
            System.err.println("Error en initialize(): " + e.getMessage());
        }
    }

    private void actualizarOpcionesCombo() {
        if (cmbActivoDisponible == null) {
            return;
        }
        cmbActivoDisponible.getItems().clear();
        try {
            List<Inventario> listaInventario = inventarioDao.listarActivosDisponiblesParaPrestamo();
            if (listaInventario == null || listaInventario.isEmpty()) {
                listaInventario = inventarioDao.listarTodos();
            }
            if (listaInventario != null) {
                for (Inventario item : listaInventario) {
                    String idActivo = item.getIdActivo() != null ? item.getIdActivo().toUpperCase().trim() : "";
                    String tipoActivo = item.getTipoActivo() != null ? item.getTipoActivo().toLowerCase().trim() : "";
                    String estadoSst = item.getEstadoSst() != null ? item.getEstadoSst().toLowerCase().trim() : "operativa";
                    boolean esOperativo = estadoSst.contains("operativa") || estadoSst.contains("disponible") || estadoSst.isEmpty();

                    if (esOperativo && !idActivo.isEmpty()) {
                        boolean coincideCategoria = false;
                        if (rbMaquinaria != null && rbMaquinaria.isSelected()) {
                            coincideCategoria = tipoActivo.contains("maquinaria") || idActivo.startsWith("MAQ");
                        } else if (rbKitCompleto != null && rbKitCompleto.isSelected()) {
                            coincideCategoria = tipoActivo.contains("kit");
                        } else if (rbHerramientaUnica != null && rbHerramientaUnica.isSelected()) {
                            coincideCategoria = tipoActivo.contains("herramienta") || idActivo.startsWith("HERR") || idActivo.startsWith("HER");
                        } else {
                            coincideCategoria = true;
                        }
                        if (coincideCategoria) {
                            String itemTexto = idActivo + " - " + (item.getDescripcion() != null ? item.getDescripcion() : "");
                            if (!cmbActivoDisponible.getItems().contains(itemTexto)) {
                                cmbActivoDisponible.getItems().add(itemTexto);
                            }
                        }
                    }
                }
            }

            if (!cmbActivoDisponible.getItems().isEmpty()) {
                cmbActivoDisponible.getSelectionModel().selectFirst();
            } else {
                cmbActivoDisponible.setPromptText("No hay activos disponibles para esta categoría");
            }
        } catch (Exception e) {
            System.err.println("Error al cargar y filtrar activos desde inventario DAO: " + e.getMessage());
        }
    }

    public void setPrestamoParaEditar(Prestamos prestamo) {
        this.prestamoEdicion = prestamo;
        if (prestamo != null) {
            if (txtOperario != null) {
                txtOperario.setText(prestamo.getOperarioNombre());
            }
            if (txtOperarioCedula != null) {
                txtOperarioCedula.setText(prestamo.getOperarioCedula());
            }
            if (txtUbicacion != null) {
                txtUbicacion.setText(prestamo.getUbicacionFrente());
            }
            if (txtObservaciones != null) {
                txtObservaciones.setText(prestamo.getObservacionesSalida());
            }
            if (cmbActivoDisponible != null) {
                cmbActivoDisponible.setValue(prestamo.getIdActivo());
            }
            if (dpFechaSalida != null && prestamo.getFechaSalida() != null && !prestamo.getFechaSalida().isEmpty()) {
                try {
                    dpFechaSalida.setValue(LocalDate.parse(prestamo.getFechaSalida(), dateFormatter));
                } catch (Exception ignored) {
                }
            }
            if (dpDevolucionEstimada != null && prestamo.getFechaDevolucionEstimada() != null && !prestamo.getFechaDevolucionEstimada().isEmpty()) {
                try {
                    dpDevolucionEstimada.setValue(LocalDate.parse(prestamo.getFechaDevolucionEstimada(), dateFormatter));
                } catch (Exception ignored) {
                }
            }
        }
    }

    @FXML
    private void onAutorizarSalidaClick(ActionEvent event) {
        if (txtOperario == null || txtOperario.getText().trim().isEmpty()) {
            AlertaUtil.mostrar("Campo requerido", "Por favor ingrese el nombre del operario.", Alert.AlertType.WARNING);
            return;
        }
        if (txtOperarioCedula == null || txtOperarioCedula.getText().trim().isEmpty()) {
            AlertaUtil.mostrar("Campo requerido", "Por favor ingrese la cédula del operario.", Alert.AlertType.WARNING);
            return;
        }
        String cedulaOperario = txtOperarioCedula.getText().trim();
        if (cmbActivoDisponible == null || cmbActivoDisponible.getValue() == null || cmbActivoDisponible.getValue().trim().isEmpty()) {
            AlertaUtil.mostrar("Activo requerido", "Debe seleccionar un activo válido del inventario.", Alert.AlertType.WARNING);
            return;
        }
        LocalDate fechaSalidaLD = (dpFechaSalida != null && dpFechaSalida.getValue() != null)
                ? dpFechaSalida.getValue()
                : LocalDate.now();

        LocalDate fechaDevolucionLD = (dpDevolucionEstimada != null) ? dpDevolucionEstimada.getValue() : null;
        if (fechaDevolucionLD != null && fechaDevolucionLD.isBefore(fechaSalidaLD)) {
            AlertaUtil.mostrar("Fechas inválidas", "La fecha de devolución estimada no puede ser anterior a la fecha de salida.", Alert.AlertType.WARNING);
            return;
        }
        String fechaSalida = fechaSalidaLD.format(dateFormatter);
        String fechaDevolucion = (fechaDevolucionLD != null) ? fechaDevolucionLD.format(dateFormatter) : "";
        String valorSeleccionado = cmbActivoDisponible.getValue();
        String idActivoLimpio = valorSeleccionado.contains(" - ") ? valorSeleccionado.split(" - ")[0].trim() : valorSeleccionado.trim();
        if (idActivoLimpio.toUpperCase().startsWith("KIT")) {
            if (kitDetalleDao.tieneComponenteNoOperativo(idActivoLimpio)) {
                AlertaUtil.mostrar("Kit no disponible",
                        "Este kit no se puede prestar porque al menos uno de sus componentes "
                        + "está bloqueado o en mantenimiento.",
                        Alert.AlertType.WARNING);
                return;
            }
        }
        int cantidadSolicitada;
        try {
            cantidadSolicitada = Integer.parseInt(txtCantidad.getText().trim());
            if (cantidadSolicitada <= 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            AlertaUtil.mostrar("Cantidad inválida", "Ingrese una cantidad válida (mayor a 0).", Alert.AlertType.WARNING);
            return;
        }

        List<Inventario> disponibles = inventarioDao.listarActivosDisponiblesParaPrestamo();
        Inventario activoSeleccionado = disponibles.stream()
                .filter(i -> i.getIdActivo().equalsIgnoreCase(idActivoLimpio))
                .findFirst()
                .orElse(null);

        if (activoSeleccionado != null && cantidadSolicitada > activoSeleccionado.getStockActual()) {
            AlertaUtil.mostrar("Stock insuficiente",
                    "Solo hay " + activoSeleccionado.getStockActual() + " unidades disponibles.",
                    Alert.AlertType.WARNING);
            return;
        }
        if (prestamoEdicion == null) {
            Prestamos nuevo = new Prestamos();
            String nuevoIdCorrelativo = "PR-" + System.currentTimeMillis();
            nuevo.setIdPrestamo(nuevoIdCorrelativo);
            nuevo.setOperarioNombre(txtOperario.getText().trim());
            nuevo.setOperarioCedula(cedulaOperario);
            nuevo.setUbicacionFrente(txtUbicacion != null ? txtUbicacion.getText().trim() : "SEDE PRINCIPAL");
            nuevo.setFechaSalida(fechaSalida);
            nuevo.setFechaDevolucionEstimada(fechaDevolucion);
            nuevo.setObservacionesSalida(txtObservaciones != null ? txtObservaciones.getText().trim() : "");
            nuevo.setIdActivo(idActivoLimpio);
            nuevo.setEstado("En Uso");
            nuevo.setDescripcionEstadoDevolucion("PENDIENTE");
            nuevo.setIdUusarioRegistro(SesionActual.getUsuario().getIdUsuario());
            nuevo.setCantidad(cantidadSolicitada);
            boolean exito = prestamoDao.guardar(nuevo);
            if (exito) {
                BitacoraService.registrar(
                        "Creación de Préstamo " + nuevo.getIdPrestamo() + " (" + idActivoLimpio + ") para " + nuevo.getOperarioNombre(),
                        "Prestamos",
                        nuevo.getIdPrestamo()
                );
                AlertaUtil.mostrar("Éxito", "El préstamo se ha registrado correctamente.", Alert.AlertType.INFORMATION);
                cerrarVentana(event);
            } else {
                AlertaUtil.mostrar("Error al Guardar", "No se pudo registrar el préstamo en la base de datos.", Alert.AlertType.ERROR);
            }
        } else {
            prestamoEdicion.setOperarioNombre(txtOperario.getText().trim());
            prestamoEdicion.setOperarioCedula(cedulaOperario);
            prestamoEdicion.setUbicacionFrente(txtUbicacion != null ? txtUbicacion.getText().trim() : "");
            prestamoEdicion.setFechaSalida(fechaSalida);
            prestamoEdicion.setFechaDevolucionEstimada(fechaDevolucion);
            prestamoEdicion.setObservacionesSalida(txtObservaciones != null ? txtObservaciones.getText().trim() : "");
            prestamoEdicion.setIdActivo(idActivoLimpio);
            prestamoEdicion.setCantidad(cantidadSolicitada);
            if (prestamoEdicion.getEstado() == null || prestamoEdicion.getEstado().trim().isEmpty()) {
                prestamoEdicion.setEstado("En Uso");
            }
            boolean exito = prestamoDao.actualizar(prestamoEdicion);
            if (exito) {
                BitacoraService.registrar(
                        "Modificación de Préstamo " + prestamoEdicion.getIdPrestamo(),
                        "Prestamos",
                        prestamoEdicion.getIdPrestamo()
                );
                AlertaUtil.mostrar("Éxito", "El préstamo se ha actualizado correctamente.", Alert.AlertType.INFORMATION);
                cerrarVentana(event);
            } else {
                AlertaUtil.mostrar("Error", "No se pudo actualizar el préstamo.", Alert.AlertType.ERROR);
            }
        }
    }

    @FXML
    private void onCancelarClick(ActionEvent event) {
        cerrarVentana(event);
    }

    private void cerrarVentana(ActionEvent event) {
        Node source = (Node) event.getSource();
        Stage stage = (Stage) source.getScene().getWindow();
        stage.close();
    }
}
