package com.ryrcontrolcenter.controller;

import com.ryrcontrolcenter.dao.FiltroDao;
import com.ryrcontrolcenter.dao.KardexDao;
import com.ryrcontrolcenter.modelo.Filtros;
import com.ryrcontrolcenter.modelo.KardexFila;
import com.ryrcontrolcenter.reportes.ReporteKardex;
import com.ryrcontrolcenter.service.KardexService;
import com.ryrcontrolcenter.util.AlertaUtil;
import com.ryrcontrolcenter.util.ExportacionUtil;
import com.ryrcontrolcenter.util.SceneManager;
import com.ryrcontrolcenter.util.SesionActual;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.ResourceBundle;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.effect.BoxBlur;
import javafx.scene.layout.BorderPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Pantalla de Kardex: libro de movimientos de insumos (entradas, salidas,
 * ajustes y saldo) y existencias con semaforo por punto de reorden, con
 * exportacion a PDF y Excel.
 */
public class KardexController implements Initializable {

    private static final String TODAS = "Todas";
    private static final String TODOS = "Todos";
    private static final String EST_ROJO = "Rojo - Reponer";
    private static final String EST_AMARILLO = "Amarillo - Pedir pronto";
    private static final String EST_VERDE = "Verde - OK";
    private static final String EST_CRITICOS = "Críticos (rojo + amarillo)";

    @FXML
    private BorderPane mainPane;
    @FXML
    private Button btnRegistrarMovimiento;
    @FXML
    private Label lblKpiExistencias;
    @FXML
    private Label lblKpiExistenciasSub;
    @FXML
    private Label lblKpiEntradas;
    @FXML
    private Label lblKpiEntradasSub;
    @FXML
    private Label lblKpiSalidas;
    @FXML
    private Label lblKpiSalidasSub;
    @FXML
    private Label lblKpiAlertas;
    @FXML
    private Label lblKpiAlertasSub;
    @FXML
    private TextField txtBuscar;
    @FXML
    private ComboBox<String> cmbCategoria;
    @FXML
    private TabPane tabs;
    @FXML
    private Tab tabMovimientos;
    @FXML
    private Tab tabExistencias;
    @FXML
    private BarChart<String, Number> chartMovMes;
    @FXML
    private PieChart chartSemaforo;
    @FXML
    private BarChart<Number, String> chartTopInsumos;
    @FXML
    private BarChart<Number, String> chartCriticos;

    // Pestaña Movimientos
    @FXML
    private ComboBox<String> cmbTipo;
    @FXML
    private DatePicker dpDesde;
    @FXML
    private DatePicker dpHasta;
    @FXML
    private TableView<KardexFila> tablaMovimientos;
    @FXML
    private TableColumn<KardexFila, String> colFecha;
    @FXML
    private TableColumn<KardexFila, String> colCodigo;
    @FXML
    private TableColumn<KardexFila, String> colInsumo;
    @FXML
    private TableColumn<KardexFila, String> colTipo;
    @FXML
    private TableColumn<KardexFila, Integer> colEntrada;
    @FXML
    private TableColumn<KardexFila, Integer> colSalida;
    @FXML
    private TableColumn<KardexFila, Integer> colSaldo;
    @FXML
    private TableColumn<KardexFila, String> colResponsable;
    @FXML
    private Label lblConteoMov;

    // Pestaña Existencias
    @FXML
    private ComboBox<String> cmbEstado;
    @FXML
    private TableView<Filtros> tablaExistencias;
    @FXML
    private TableColumn<Filtros, String> colExCodigo;
    @FXML
    private TableColumn<Filtros, String> colExDescripcion;
    @FXML
    private TableColumn<Filtros, String> colExCategoria;
    @FXML
    private TableColumn<Filtros, Integer> colExStock;
    @FXML
    private TableColumn<Filtros, Integer> colExReorden;
    @FXML
    private TableColumn<Filtros, Integer> colExFaltante;
    @FXML
    private TableColumn<Filtros, String> colExEstado;
    @FXML
    private TableColumn<Filtros, String> colExActualizado;
    @FXML
    private Label lblConteoEx;

    private final KardexDao kardexDao = new KardexDao();
    private final FiltroDao filtroDao = new FiltroDao();

    private final ObservableList<KardexFila> movimientos = FXCollections.observableArrayList();
    private final ObservableList<Filtros> existencias = FXCollections.observableArrayList();
    private FilteredList<KardexFila> movFiltrados;
    private FilteredList<Filtros> exFiltrados;
    private SortedList<KardexFila> movOrdenados;
    private SortedList<Filtros> exOrdenados;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Solo el rol que puede editar registra movimientos (RF-5 / RNF-5)
        if (!SesionActual.puedeEditar()) {
            btnRegistrarMovimiento.setVisible(false);
            btnRegistrarMovimiento.setManaged(false);
        }

        movFiltrados = new FilteredList<>(movimientos, k -> true);
        exFiltrados = new FilteredList<>(existencias, f -> true);
        movOrdenados = new SortedList<>(movFiltrados);
        exOrdenados = new SortedList<>(exFiltrados);
        movOrdenados.comparatorProperty().bind(tablaMovimientos.comparatorProperty());
        exOrdenados.comparatorProperty().bind(tablaExistencias.comparatorProperty());
        tablaMovimientos.setItems(movOrdenados);
        tablaExistencias.setItems(exOrdenados);
        tablaMovimientos.setPlaceholder(new Label("No hay movimientos para los filtros seleccionados."));
        tablaExistencias.setPlaceholder(new Label("No hay insumos para los filtros seleccionados."));

        configurarTablaMovimientos();
        configurarTablaExistencias();
        configurarFiltros();
        cargarDatos();

        // Doble clic en un insumo: abre su "tarjeta" (movimientos de ese insumo)
        tablaExistencias.setOnMouseClicked(ev -> {
            Filtros sel = tablaExistencias.getSelectionModel().getSelectedItem();
            if (ev.getClickCount() == 2 && sel != null) {
                txtBuscar.setText(sel.getIdFiltro());
                tabs.getSelectionModel().select(tabMovimientos);
            }
        });
    }

    // ------------------------------------------------------------- configuracion

    private void configurarTablaMovimientos() {
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fechaHora")); // orden cronologico correcto
        colFecha.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : ReporteKardex.fecha(item));
            }
        });
        colCodigo.setCellValueFactory(new PropertyValueFactory<>("idFiltro"));
        colInsumo.setCellValueFactory(new PropertyValueFactory<>("descripcion"));
        colTipo.setCellValueFactory(new PropertyValueFactory<>("tipo"));
        colTipo.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                String[] colores = switch (item) {
                    case "Entrada" -> new String[]{"#e8f5e9", "#2e7d32"};
                    case "Salida" -> new String[]{"#fdecea", "#c62828"};
                    default -> new String[]{"#fff3cd", "#8a5a00"};
                };
                Label pill = new Label(item.toUpperCase());
                pill.setStyle("-fx-background-color: " + colores[0] + "; -fx-text-fill: " + colores[1]
                        + "; -fx-font-weight: bold; -fx-font-size: 10px; -fx-background-radius: 10; -fx-padding: 2 8 2 8;");
                setText(null);
                setGraphic(pill);
                setAlignment(Pos.CENTER);
            }
        });
        colEntrada.setCellValueFactory(new PropertyValueFactory<>("entrada"));
        colEntrada.setCellFactory(c -> cantidadConSigno("+", "#2e7d32"));
        colSalida.setCellValueFactory(new PropertyValueFactory<>("salida"));
        colSalida.setCellFactory(c -> cantidadConSigno("-", "#c62828"));
        colSaldo.setCellValueFactory(new PropertyValueFactory<>("saldo"));
        colSaldo.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                KardexFila fila = empty || getTableRow() == null ? null : getTableRow().getItem();
                if (empty || item == null || fila == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(String.valueOf(item));
                setAlignment(Pos.CENTER_RIGHT);
                // Color segun el punto de reorden actual del insumo
                setStyle("-fx-font-weight: bold; -fx-text-fill: "
                        + colorSemaforo(KardexService.semaforo(item, fila.getPuntoReorden())) + ";");
            }
        });
        colResponsable.setCellValueFactory(new PropertyValueFactory<>("responsable"));
    }

    private TableCell<KardexFila, Integer> cantidadConSigno(String signo, String color) {
        return new TableCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item == 0) {
                    setText(null);
                    return;
                }
                setText(signo + item);
                setAlignment(Pos.CENTER_RIGHT);
                setStyle("-fx-font-weight: bold; -fx-text-fill: " + color + ";");
            }
        };
    }

    private void configurarTablaExistencias() {
        colExCodigo.setCellValueFactory(new PropertyValueFactory<>("idFiltro"));
        colExDescripcion.setCellValueFactory(new PropertyValueFactory<>("descripcion"));
        colExCategoria.setCellValueFactory(new PropertyValueFactory<>("categoriaFiltro"));
        colExStock.setCellValueFactory(new PropertyValueFactory<>("stockActual"));
        colExStock.setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-weight: bold;");
        colExReorden.setCellValueFactory(new PropertyValueFactory<>("puntoReorden"));
        colExReorden.setStyle("-fx-alignment: CENTER-RIGHT;");
        colExFaltante.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(
                Math.max(0, c.getValue().getPuntoReorden() - c.getValue().getStockActual())));
        colExFaltante.setStyle("-fx-alignment: CENTER-RIGHT;");
        colExEstado.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                KardexService.semaforo(c.getValue().getStockActual(), c.getValue().getPuntoReorden())));
        colExEstado.setComparator((a, b) -> Integer.compare(ordenSemaforo(a), ordenSemaforo(b)));
        colExEstado.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }
                Label pill = new Label(KardexService.etiquetaCorta(item));
                pill.setStyle("-fx-background-color: " + fondoSemaforo(item) + "; -fx-text-fill: " + colorSemaforo(item)
                        + "; -fx-font-weight: bold; -fx-font-size: 10px; -fx-background-radius: 10; -fx-padding: 2 8 2 8;");
                setGraphic(pill);
                setAlignment(Pos.CENTER_LEFT);
            }
        });
        colExActualizado.setCellValueFactory(new PropertyValueFactory<>("fechaActualizacion"));
        colExActualizado.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : ReporteKardex.fecha(item));
            }
        });
    }

    private void configurarFiltros() {
        cmbCategoria.setItems(FXCollections.observableArrayList(TODAS, "Aceite", "Aire", "Combustible", "Hidraulico", "Refrigerante"));
        cmbCategoria.setValue(TODAS);
        cmbTipo.setItems(FXCollections.observableArrayList(TODOS, "Entrada", "Salida", "Ajuste"));
        cmbTipo.setValue(TODOS);
        cmbEstado.setItems(FXCollections.observableArrayList(TODOS, EST_ROJO, EST_AMARILLO, EST_VERDE, EST_CRITICOS));
        cmbEstado.setValue(TODOS);

        txtBuscar.textProperty().addListener((o, a, b) -> aplicarFiltros());
        cmbCategoria.valueProperty().addListener((o, a, b) -> aplicarFiltros());
        cmbTipo.valueProperty().addListener((o, a, b) -> aplicarFiltros());
        cmbEstado.valueProperty().addListener((o, a, b) -> aplicarFiltros());
        dpDesde.valueProperty().addListener((o, a, b) -> aplicarFiltros());
        dpHasta.valueProperty().addListener((o, a, b) -> aplicarFiltros());
    }

    // ------------------------------------------------------------------- datos

    private void cargarDatos() {
        movimientos.setAll(kardexDao.listarMovimientos());
        existencias.setAll(filtroDao.listarTodos());
        aplicarFiltros();
    }

    private String textoBusqueda() {
        return txtBuscar.getText() == null ? "" : txtBuscar.getText().trim().toLowerCase();
    }

    private boolean coincideTexto(String q, String... campos) {
        if (q.isEmpty()) {
            return true;
        }
        for (String c : campos) {
            if (c != null && c.toLowerCase().contains(q)) {
                return true;
            }
        }
        return false;
    }

    private void aplicarFiltros() {
        final String q = textoBusqueda();
        final String cat = cmbCategoria.getValue() == null ? TODAS : cmbCategoria.getValue();
        final String tipo = cmbTipo.getValue() == null ? TODOS : cmbTipo.getValue();
        final String estado = cmbEstado.getValue() == null ? TODOS : cmbEstado.getValue();
        final LocalDate desde = dpDesde.getValue();
        final LocalDate hasta = dpHasta.getValue();

        movFiltrados.setPredicate(k -> {
            if (!coincideTexto(q, k.getIdFiltro(), k.getDescripcion())) {
                return false;
            }
            if (!TODAS.equals(cat) && !cat.equals(k.getCategoria())) {
                return false;
            }
            if (!TODOS.equals(tipo) && !tipo.equals(k.getTipo())) {
                return false;
            }
            String fh = k.getFechaHora() == null ? "" : k.getFechaHora();
            String dia = fh.length() >= 10 ? fh.substring(0, 10) : fh; // yyyy-MM-dd compara bien como texto
            if (desde != null && dia.compareTo(desde.toString()) < 0) {
                return false;
            }
            return hasta == null || dia.compareTo(hasta.toString()) <= 0;
        });

        exFiltrados.setPredicate(f -> {
            if (!coincideTexto(q, f.getIdFiltro(), f.getDescripcion())) {
                return false;
            }
            if (!TODAS.equals(cat) && !cat.equals(f.getCategoriaFiltro())) {
                return false;
            }
            String sem = KardexService.semaforo(f.getStockActual(), f.getPuntoReorden());
            return switch (estado) {
                case EST_ROJO -> KardexService.ROJO.equals(sem);
                case EST_AMARILLO -> KardexService.AMARILLO.equals(sem);
                case EST_VERDE -> KardexService.VERDE.equals(sem);
                case EST_CRITICOS -> !KardexService.VERDE.equals(sem);
                default -> true;
            };
        });
        actualizarIndicadores();
    }

    private int[] contarAlertas() {
        int rojos = 0, amarillos = 0;
        for (Filtros f : existencias) {
            String s = KardexService.semaforo(f.getStockActual(), f.getPuntoReorden());
            if (KardexService.ROJO.equals(s)) {
                rojos++;
            } else if (KardexService.AMARILLO.equals(s)) {
                amarillos++;
            }
        }
        return new int[]{rojos, amarillos};
    }

    private ReporteKardex.Resumen resumen() {
        int stockTotal = 0;
        for (Filtros f : existencias) {
            stockTotal += f.getStockActual();
        }
        int entradas = 0, salidas = 0;
        for (KardexFila k : movFiltrados) {
            entradas += k.getEntrada();
            salidas += k.getSalida();
        }
        int[] al = contarAlertas();
        return new ReporteKardex.Resumen(stockTotal, entradas, salidas, al[0] + al[1], al[0]);
    }

    private void actualizarIndicadores() {
        ReporteKardex.Resumen r = resumen();
        lblKpiExistencias.setText(r.existencias() + " uds");
        lblKpiExistenciasSub.setText(existencias.size() + " insumos registrados");
        lblKpiEntradas.setText("+" + r.entradas() + " uds");
        lblKpiEntradasSub.setText("incluye sobrantes de ajuste");
        lblKpiSalidas.setText("-" + r.salidas() + " uds");
        lblKpiSalidasSub.setText("incluye faltantes de ajuste");
        lblKpiAlertas.setText(r.insumosEnAlerta() + " insumos");
        lblKpiAlertasSub.setText(r.insumosEnRojo() + " en rojo, " + (r.insumosEnAlerta() - r.insumosEnRojo()) + " en amarillo");
        lblConteoMov.setText("Mostrando " + movFiltrados.size() + " de " + movimientos.size() + " movimientos registrados");
        lblConteoEx.setText("Mostrando " + exFiltrados.size() + " de " + existencias.size() + " insumos");
        actualizarGraficas();
    }

    // ------------------------------------------------------------------ graficas

    /** Recalcula las cuatro graficas con los datos que dejan pasar los filtros activos. */
    private void actualizarGraficas() {
        // 1) entradas y salidas por mes
        Map<String, int[]> porMes = new TreeMap<>();
        for (KardexFila k : movFiltrados) {
            String fh = k.getFechaHora();
            if (fh != null && fh.length() >= 7) {
                int[] a = porMes.computeIfAbsent(fh.substring(0, 7), x -> new int[2]);
                a[0] += k.getEntrada();
                a[1] += k.getSalida();
            }
        }
        XYChart.Series<String, Number> sEnt = new XYChart.Series<>();
        sEnt.setName("Entradas");
        XYChart.Series<String, Number> sSal = new XYChart.Series<>();
        sSal.setName("Salidas");
        for (Map.Entry<String, int[]> e : porMes.entrySet()) {
            String mes = com.ryrcontrolcenter.reportes.ReporteDashboard.etiquetaMes(e.getKey());
            sEnt.getData().add(new XYChart.Data<>(mes, e.getValue()[0]));
            sSal.getData().add(new XYChart.Data<>(mes, e.getValue()[1]));
        }
        chartMovMes.getData().setAll(List.of(sEnt, sSal));

        // 2) semaforo de existencias (siempre rojo, amarillo, verde: los colores van por posicion)
        int[] sem = new int[3];
        for (Filtros f : exFiltrados) {
            switch (KardexService.semaforo(f.getStockActual(), f.getPuntoReorden())) {
                case KardexService.ROJO -> sem[0]++;
                case KardexService.AMARILLO -> sem[1]++;
                default -> sem[2]++;
            }
        }
        chartSemaforo.getData().setAll(
                new PieChart.Data("Rojo - reponer (" + sem[0] + ")", sem[0]),
                new PieChart.Data("Amarillo - pedir pronto (" + sem[1] + ")", sem[1]),
                new PieChart.Data("Verde - OK (" + sem[2] + ")", sem[2]));

        // 3) insumos con mas movimiento (los 8 con mas salidas), el mayor arriba
        Map<String, int[]> porInsumo = new java.util.LinkedHashMap<>();
        for (KardexFila k : movFiltrados) {
            int[] a = porInsumo.computeIfAbsent(k.getIdFiltro(), x -> new int[2]);
            a[0] += k.getEntrada();
            a[1] += k.getSalida();
        }
        List<Map.Entry<String, int[]>> top = new ArrayList<>(porInsumo.entrySet());
        top.sort((a, b) -> Integer.compare(b.getValue()[1] + b.getValue()[0], a.getValue()[1] + a.getValue()[0]));
        if (top.size() > 8) {
            top = new ArrayList<>(top.subList(0, 8));
        }
        java.util.Collections.reverse(top);
        XYChart.Series<Number, String> tEnt = new XYChart.Series<>();
        tEnt.setName("Entradas");
        XYChart.Series<Number, String> tSal = new XYChart.Series<>();
        tSal.setName("Salidas");
        for (Map.Entry<String, int[]> e : top) {
            tEnt.getData().add(new XYChart.Data<>(e.getValue()[0], e.getKey()));
            tSal.getData().add(new XYChart.Data<>(e.getValue()[1], e.getKey()));
        }
        chartTopInsumos.getData().setAll(List.of(tEnt, tSal));

        // 4) stock vs punto de reorden de los 8 mas criticos (stock - reorden de menor a mayor)
        List<Filtros> crit = new ArrayList<>(exFiltrados);
        crit.sort(java.util.Comparator.comparingInt((Filtros f) -> f.getStockActual() - f.getPuntoReorden())
                .thenComparing(Filtros::getIdFiltro));
        if (crit.size() > 8) {
            crit = new ArrayList<>(crit.subList(0, 8));
        }
        java.util.Collections.reverse(crit);
        XYChart.Series<Number, String> cStock = new XYChart.Series<>();
        cStock.setName("Stock");
        XYChart.Series<Number, String> cReorden = new XYChart.Series<>();
        cReorden.setName("Punto de reorden");
        for (Filtros f : crit) {
            cStock.getData().add(new XYChart.Data<>(f.getStockActual(), f.getIdFiltro()));
            cReorden.getData().add(new XYChart.Data<>(f.getPuntoReorden(), f.getIdFiltro()));
        }
        chartCriticos.getData().setAll(List.of(cStock, cReorden));
    }

    // ------------------------------------------------------------------ colores

    private static String colorSemaforo(String s) {
        return switch (s) {
            case KardexService.ROJO -> "#c62828";
            case KardexService.AMARILLO -> "#8a5a00";
            default -> "#2e7d32";
        };
    }

    private static String fondoSemaforo(String s) {
        return switch (s) {
            case KardexService.ROJO -> "#fdecea";
            case KardexService.AMARILLO -> "#fff3cd";
            default -> "#e8f5e9";
        };
    }

    private static int ordenSemaforo(String s) {
        return KardexService.ROJO.equals(s) ? 0 : KardexService.AMARILLO.equals(s) ? 1 : 2;
    }

    // ------------------------------------------------------------------ acciones

    @FXML
    private void onLimpiarClick(ActionEvent event) {
        txtBuscar.clear();
        cmbCategoria.setValue(TODAS);
        cmbTipo.setValue(TODOS);
        cmbEstado.setValue(TODOS);
        dpDesde.setValue(null);
        dpHasta.setValue(null);
    }

    @FXML
    private void onRegistrarMovimientoClick(ActionEvent event) {
        BoxBlur blur = new BoxBlur(5, 5, 3);
        mainPane.setEffect(blur);
        mainPane.setOpacity(0.6);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/ryrcontrolcenter/ui/NuevoMovimientoVista.fxml"));
            Parent root = loader.load();
            NuevoMovimientoController dialogo = loader.getController();
            Filtros seleccionado = tablaExistencias.getSelectionModel().getSelectedItem();
            if (seleccionado != null && tabs.getSelectionModel().getSelectedItem() == tabExistencias) {
                dialogo.setFiltroInicial(seleccionado.getIdFiltro());
            }
            Stage stage = new Stage();
            stage.setTitle("Registrar movimiento de Kárdex");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(mainPane.getScene().getWindow());
            stage.setOnHidden(ev -> {
                mainPane.setEffect(null);
                mainPane.setOpacity(1.0);
                cargarDatos();
            });
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            mainPane.setEffect(null);
            mainPane.setOpacity(1.0);
            AlertaUtil.mostrar("Error", "No se pudo abrir el formulario de movimiento.", Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void onExportarPdfClick(ActionEvent event) {
        exportar(true);
    }

    @FXML
    private void onExportarExcelClick(ActionEvent event) {
        exportar(false);
    }

    /** Exporta la pestaña activa tal como se ve (filtros y orden actuales). */
    private void exportar(boolean pdf) {
        boolean enMovimientos = tabs.getSelectionModel().getSelectedItem() == tabMovimientos;
        File destino = ExportacionUtil.elegirArchivo(mainPane.getScene().getWindow(),
                enMovimientos ? "Kardex_movimientos" : "Kardex_existencias", pdf);
        if (destino == null) {
            return;
        }
        String usuario = ExportacionUtil.usuarioActual();
        String filtros = descripcionFiltros(enMovimientos);
        try {
            if (enMovimientos) {
                List<KardexFila> filas = new ArrayList<>(movOrdenados);
                if (pdf) {
                    ReporteKardex.movimientosPdf(destino, filas, new ArrayList<>(existencias), filtros, usuario, resumen());
                } else {
                    ReporteKardex.movimientosExcel(destino, filas, new ArrayList<>(existencias), filtros, usuario, resumen());
                }
            } else {
                List<Filtros> filas = new ArrayList<>(exOrdenados);
                if (pdf) {
                    ReporteKardex.existenciasPdf(destino, filas, filtros, usuario);
                } else {
                    ReporteKardex.existenciasExcel(destino, filas, filtros, usuario);
                }
            }
            AlertaUtil.mostrar("Exportación exitosa", "El archivo se guardó en:\n" + destino.getAbsolutePath(), Alert.AlertType.INFORMATION);
        } catch (Exception e) {
            e.printStackTrace();
            AlertaUtil.mostrar("Error al exportar",
                    "No se pudo crear el archivo. Verifique que no esté abierto en otro programa.\n" + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private String descripcionFiltros(boolean enMovimientos) {
        List<String> partes = new ArrayList<>();
        DateTimeFormatter df = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        if (!textoBusqueda().isEmpty()) {
            partes.add("búsqueda \"" + txtBuscar.getText().trim() + "\"");
        }
        if (cmbCategoria.getValue() != null && !TODAS.equals(cmbCategoria.getValue())) {
            partes.add("categoría " + cmbCategoria.getValue());
        }
        if (enMovimientos) {
            if (cmbTipo.getValue() != null && !TODOS.equals(cmbTipo.getValue())) {
                partes.add("tipo " + cmbTipo.getValue());
            }
            if (dpDesde.getValue() != null) {
                partes.add("desde " + df.format(dpDesde.getValue()));
            }
            if (dpHasta.getValue() != null) {
                partes.add("hasta " + df.format(dpHasta.getValue()));
            }
        } else if (cmbEstado.getValue() != null && !TODOS.equals(cmbEstado.getValue())) {
            partes.add("estado " + cmbEstado.getValue());
        }
        return partes.isEmpty() ? "Sin filtros (todos los registros)." : "Filtros aplicados: " + String.join("; ", partes) + ".";
    }

    // ---------------------------------------------------------------- navegacion

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
        SceneManager.cambiarA("/com/ryrcontrolcenter/ui/LoginVista.fxml");
    }
}
