package com.ryrcontrolcenter.controller;

import com.ryrcontrolcenter.dao.FiltroDao;
import com.ryrcontrolcenter.modelo.Filtros;
import com.ryrcontrolcenter.service.KardexService;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.ToggleGroup;
import javafx.stage.Stage;
import javafx.util.StringConverter;

/** Dialogo para registrar una Entrada, Salida o Ajuste en el Kardex. */
public class NuevoMovimientoController implements Initializable {

    @FXML
    private ToggleGroup grupoTipoMovimiento;
    @FXML
    private RadioButton rbEntrada;
    @FXML
    private RadioButton rbSalida;
    @FXML
    private RadioButton rbAjuste;
    @FXML
    private ComboBox<Filtros> cmbFiltro;
    @FXML
    private Label lblStockActual;
    @FXML
    private Label lblCantidad;
    @FXML
    private Label lblSaldoResultante;
    @FXML
    private TextField txtCantidad;
    @FXML
    private Label lblError;
    @FXML
    private Button btnCancelar;
    @FXML
    private Button btnGuardar;

    private final KardexService servicio = new KardexService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        cmbFiltro.setItems(FXCollections.observableArrayList(new FiltroDao().listarTodos()));
        cmbFiltro.setConverter(new StringConverter<Filtros>() {
            @Override
            public String toString(Filtros f) {
                return f == null ? "" : f.getIdFiltro() + " - " + f.getDescripcion();
            }

            @Override
            public Filtros fromString(String s) {
                return null;
            }
        });
        // Solo digitos en la cantidad
        txtCantidad.setTextFormatter(new TextFormatter<String>(c -> c.getControlNewText().matches("\\d{0,7}") ? c : null));

        cmbFiltro.valueProperty().addListener((o, a, b) -> actualizarVista());
        grupoTipoMovimiento.selectedToggleProperty().addListener((o, a, b) -> actualizarVista());
        txtCantidad.textProperty().addListener((o, a, b) -> actualizarVista());
        actualizarVista();
    }

    /** Deja el insumo ya seleccionado (por ejemplo, el que estaba marcado en la tabla). */
    public void setFiltroInicial(String idFiltro) {
        if (idFiltro == null) {
            return;
        }
        for (Filtros f : cmbFiltro.getItems()) {
            if (idFiltro.equals(f.getIdFiltro())) {
                cmbFiltro.setValue(f);
                break;
            }
        }
    }

    private String tipoSeleccionado() {
        if (rbEntrada.isSelected()) {
            return "Entrada";
        }
        return rbAjuste.isSelected() ? "Ajuste" : "Salida";
    }

    private Integer cantidadIngresada() {
        String t = txtCantidad.getText();
        if (t == null || t.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(t);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void actualizarVista() {
        String tipo = tipoSeleccionado();
        lblCantidad.setText(switch (tipo) {
            case "Entrada" -> "Cantidad que ingresa";
            case "Salida" -> "Cantidad a sacar";
            default -> "Stock real contado (conteo físico)";
        });
        Filtros f = cmbFiltro.getValue();
        lblError.setText("");
        if (f == null) {
            lblStockActual.setText("Stock actual: -");
            lblSaldoResultante.setText(" ");
            return;
        }
        lblStockActual.setText("Stock actual: " + f.getStockActual() + " " + f.getUnidadMedida()
                + "   (punto de reorden: " + f.getPuntoReorden() + ")");
        Integer q = cantidadIngresada();
        if (q == null) {
            lblSaldoResultante.setText(" ");
            return;
        }
        int nuevo = switch (tipo) {
            case "Entrada" -> f.getStockActual() + q;
            case "Salida" -> f.getStockActual() - q;
            default -> q;
        };
        String semaforo = KardexService.semaforo(Math.max(nuevo, 0), f.getPuntoReorden());
        lblSaldoResultante.setText("Saldo resultante: " + nuevo + " - " + KardexService.etiquetaSemaforo(semaforo));
        lblSaldoResultante.setStyle("-fx-font-weight: bold; -fx-text-fill: "
                + (nuevo < 0 || KardexService.ROJO.equals(semaforo) ? "#c62828"
                        : KardexService.AMARILLO.equals(semaforo) ? "#8a5a00" : "#2e7d32") + ";");
    }

    @FXML
    private void onCancelarClick(ActionEvent event) {
        cerrar();
    }

    @FXML
    private void onGuardarClick(ActionEvent event) {
        Filtros f = cmbFiltro.getValue();
        if (f == null) {
            lblError.setText("Seleccione un insumo.");
            return;
        }
        Integer q = cantidadIngresada();
        if (q == null) {
            lblError.setText("Ingrese la cantidad.");
            return;
        }
        String error = servicio.registrarMovimiento(f.getIdFiltro(), tipoSeleccionado(), q);
        if (error != null) {
            lblError.setText(error);
            return;
        }
        cerrar();
    }

    private void cerrar() {
        ((Stage) btnCancelar.getScene().getWindow()).close();
    }
}
