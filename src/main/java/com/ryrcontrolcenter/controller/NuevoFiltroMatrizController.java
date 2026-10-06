package com.ryrcontrolcenter.controller;

import com.ryrcontrolcenter.dao.FiltroDao;
import com.ryrcontrolcenter.dao.MatrizFiltroDao;
import com.ryrcontrolcenter.modelo.Filtros;
import com.ryrcontrolcenter.modelo.MatrizFiltro;
import com.ryrcontrolcenter.service.BitacoraService;
import com.ryrcontrolcenter.util.AlertaUtil;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class NuevoFiltroMatrizController implements Initializable {

    @FXML
    private Label lblMaquinaSeleccionada;
    @FXML
    private TextField txtSistemaTipoFiltro;
    @FXML
    private TextField txtCodigoOem;
    @FXML
    private ComboBox<String> cmbCodigoFiltro;
    @FXML
    private TextField txtDescripcionFiltro;
    @FXML
    private ComboBox<String> cmbCategoria;
    @FXML
    private TextField txtUnidadMedida;
    @FXML
    private TextField txtStockActual;
    @FXML
    private TextField txtPuntoReorden;
    @FXML
    private Label lblError;
    @FXML
    private Button btnCancelar;
    @FXML
    private Button btnGuardar;

    private final MatrizFiltroDao matrizDao = new MatrizFiltroDao();
    private final FiltroDao filtroDao = new FiltroDao();
    private List<Filtros> filtrosExistentes;
    private String idMaquina;
    private Integer idMatrizEdicion;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        cmbCategoria.setItems(FXCollections.observableArrayList("Aceite", "Aire", "Combustible", "Hidraulico", "Refrigerante"));
        filtrosExistentes = filtroDao.listarTodos();
        cmbCodigoFiltro.setItems(FXCollections.observableArrayList(
                filtrosExistentes.stream().map(Filtros::getIdFiltro).toList()
        ));
        cmbCodigoFiltro.getEditor().textProperty().addListener((obs, anterior, nuevoTexto) -> {
            actualizarCamposSegunCodigo(nuevoTexto);
        });
    }

    private void actualizarCamposSegunCodigo(String codigo) {
        Filtros existente = filtrosExistentes.stream()
                .filter(f -> f.getIdFiltro().equalsIgnoreCase(codigo))
                .findFirst().orElse(null);
        boolean yaExiste = existente != null;
        txtDescripcionFiltro.setDisable(yaExiste);
        cmbCategoria.setDisable(yaExiste);
        txtUnidadMedida.setDisable(yaExiste);
        txtStockActual.setDisable(true);
        txtStockActual.setEditable(false);
        txtPuntoReorden.setDisable(false);
        if (yaExiste) {
            txtDescripcionFiltro.setText(existente.getDescripcion());
            cmbCategoria.getSelectionModel().select(existente.getCategoriaFiltro());
            txtUnidadMedida.setText(existente.getUnidadMedida());
            txtPuntoReorden.setText(String.valueOf(existente.getPuntoReorden()));
            txtStockActual.setText(String.valueOf(existente.getStockActual()));
        } else {
            txtDescripcionFiltro.clear();
            cmbCategoria.getSelectionModel().clearSelection();
            txtUnidadMedida.clear();
            txtPuntoReorden.clear();
            txtStockActual.clear();
            txtStockActual.setDisable(false);
            txtStockActual.setEditable(true);
        }
    }

    public void setMaquinaContexto(String idMaquina, String nombreMaquinaMostrar) {
        this.idMaquina = idMaquina;
        lblMaquinaSeleccionada.setText(nombreMaquinaMostrar);
    }

    public void cargarParaEdicion(MatrizFiltro mf, String nombreMaquinaMostrar) {
        this.idMaquina = mf.getIdMaquina();
        this.idMatrizEdicion = mf.getIdMatriz();
        lblMaquinaSeleccionada.setText(nombreMaquinaMostrar);
        txtSistemaTipoFiltro.setText(mf.getSistemaTipoFiltro());
        txtCodigoOem.setText(mf.getCodigoOem());
        if (mf.getIdFiltro() != null) {
            cmbCodigoFiltro.getEditor().setText(mf.getIdFiltro());
            actualizarCamposSegunCodigo(mf.getIdFiltro());
        }
    }

    @FXML
    private void onCancelarClick(ActionEvent event) {
        cerrarVentana();
    }

    @FXML
    private void onGuardarClick(ActionEvent event) {
        String sistema = txtSistemaTipoFiltro.getText();
        String codigoOem = txtCodigoOem.getText();
        String codigoFiltro = cmbCodigoFiltro.getEditor().getText();
        if (sistema == null || sistema.isBlank() || codigoOem == null || codigoOem.isBlank()
                || codigoFiltro == null || codigoFiltro.isBlank()) {
            lblError.setText("Sistema, código OEM y código de filtro son obligatorios");
            return;
        }
        Filtros existente = filtroDao.buscarPorId(codigoFiltro);
        if (existente == null) {
            Filtros nuevo = new Filtros();
            nuevo.setIdFiltro(codigoFiltro);
            nuevo.setDescripcion(txtDescripcionFiltro.getText());
            nuevo.setCategoriaFiltro(cmbCategoria.getSelectionModel().getSelectedItem());
            nuevo.setUnidadMedida(txtUnidadMedida.getText().isBlank() ? "Unidad" : txtUnidadMedida.getText());
            try {
                nuevo.setStockActual(Integer.parseInt(txtStockActual.getText().isBlank() ? "0" : txtStockActual.getText()));
                nuevo.setPuntoReorden(Integer.parseInt(txtPuntoReorden.getText().isBlank() ? "0" : txtPuntoReorden.getText()));
            } catch (NumberFormatException e) {
                lblError.setText("Stock y punto de reorden deben ser números");
                return;
            }
            if (nuevo.getCategoriaFiltro() == null) {
                lblError.setText("Seleccione una categoría para el filtro nuevo");
                return;
            }
            filtroDao.insertar(nuevo);
        } else {
            try {
                int nuevoPuntoReorden = Integer.parseInt(txtPuntoReorden.getText());
                if (nuevoPuntoReorden != existente.getPuntoReorden()) {
                    filtroDao.actualizarPuntoReorden(codigoFiltro, nuevoPuntoReorden);
                    filtroDao.actualizarFechaActualizacion(codigoFiltro);
                }
            } catch (NumberFormatException e) {
                lblError.setText("Punto de reorden debe ser un número");
                return;
            }
        }

        MatrizFiltro mf = new MatrizFiltro();
        mf.setIdMaquina(idMaquina);
        mf.setSistemaTipoFiltro(sistema);
        mf.setCodigoOem(codigoOem);
        mf.setIdFiltro(codigoFiltro);
        boolean exito;
        if (idMatrizEdicion == null) {
            exito = matrizDao.insertar(mf);
        } else {
            mf.setIdMatriz(idMatrizEdicion);
            exito = matrizDao.actualizar(mf);
        }
        if (exito) {
            String accion = (idMatrizEdicion == null)
                    ? "Asociación de filtro " + codigoFiltro + " (" + sistema + ") a máquina " + idMaquina
                    : "Modificación de filtro " + sistema + " en máquina " + idMaquina;
            BitacoraService.registrar(accion, "Matriz Filtro", idMaquina);
            AlertaUtil.mostrar("Éxito", "El filtro fue guardado correctamente.", Alert.AlertType.INFORMATION);
            cerrarVentana();
        } else {
            lblError.setText("No se pudo guardar. Verifique que no exista ya un filtro para ese sistema en esta máquina.");
        }
    }

    private void cerrarVentana() {
        Stage stage = (Stage) btnCancelar.getScene().getWindow();
        stage.close();
    }
}
