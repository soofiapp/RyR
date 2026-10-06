package com.ryrcontrolcenter.reportes;

import com.ryrcontrolcenter.modelo.Filtros;
import com.ryrcontrolcenter.modelo.KardexFila;
import com.ryrcontrolcenter.reportes.XlsxWriter.Formula;
import com.ryrcontrolcenter.reportes.XlsxWriter.FormulaTxt;
import com.ryrcontrolcenter.reportes.XlsxWriter.Grafico;
import com.ryrcontrolcenter.reportes.XlsxWriter.Hoja;
import com.ryrcontrolcenter.service.KardexService;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;


public class ReporteKardex {

    public record Resumen(int existencias, int entradas, int salidas, int insumosEnAlerta, int insumosEnRojo) { }

    static final String H_MOV = "Kárdex";
    static final String H_EX = "Existencias";
    static final String H_PIV = "Tablas dinámicas";
    static final String H_GRA = "Gráficas";

    private static final DateTimeFormatter SALIDA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    static final int G_OSCURO = 0x404040;
    static final int G_MEDIO = 0x8C8C8C;
    static final int G_CLARO = 0xCFCFCF;

    public static String fecha(String bd) {
        LocalDateTime t = parse(bd);
        return t == null ? (bd == null ? "" : bd) : SALIDA.format(t);
    }

    static LocalDateTime parse(String bd) {
        if (bd == null || bd.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(bd.trim().replace(' ', 'T'));
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static String signo(int v, char s) {
        return v == 0 ? "" : (s + String.valueOf(v));
    }

    static String nz(String s) {
        return s == null || s.isBlank() ? "Sin categoría" : s;
    }

    static int fondoSemaforo(String s) {
        return switch (s) {
            case KardexService.ROJO -> PdfDocumento.FONDO_ROJO;
            case KardexService.AMARILLO -> PdfDocumento.FONDO_AMARILLO;
            default -> PdfDocumento.FONDO_VERDE;
        };
    }

    static int colorSemaforo(String s) {
        return switch (s) {
            case KardexService.ROJO -> PdfDocumento.TEXTO_ROJO;
            case KardexService.AMARILLO -> PdfDocumento.TEXTO_AMARILLO;
            default -> PdfDocumento.TEXTO_VERDE;
        };
    }

    static int colorPleno(String s) {
        return switch (s) {
            case KardexService.ROJO -> PdfDocumento.ROJO;
            case KardexService.AMARILLO -> PdfDocumento.AMARILLO;
            default -> PdfDocumento.VERDE;
        };
    }


    private static class PorMes {
        final List<YearMonth> meses = new ArrayList<>();
        final List<Integer> entradas = new ArrayList<>();
        final List<Integer> salidas = new ArrayList<>();
        final List<Integer> cuenta = new ArrayList<>();
    }

    private static PorMes agruparPorMes(List<KardexFila> filas) {
        TreeSet<YearMonth> set = new TreeSet<>();
        for (KardexFila k : filas) {
            LocalDateTime t = parse(k.getFechaHora());
            if (t != null) {
                set.add(YearMonth.from(t));
            }
        }
        if (set.isEmpty()) {
            set.add(YearMonth.now());
        }

        List<YearMonth> lista = new ArrayList<>(set);
        if (lista.size() > 12) {
            lista = lista.subList(lista.size() - 12, lista.size());
        }
        PorMes r = new PorMes();
        Map<YearMonth, int[]> acc = new HashMap<>();
        for (YearMonth m : lista) {
            acc.put(m, new int[3]);
        }
        for (KardexFila k : filas) {
            LocalDateTime t = parse(k.getFechaHora());
            int[] a = t == null ? null : acc.get(YearMonth.from(t));
            if (a != null) {
                a[0] += k.getEntrada();
                a[1] += k.getSalida();
                a[2]++;
            }
        }
        for (YearMonth m : lista) {
            r.meses.add(m);
            r.entradas.add(acc.get(m)[0]);
            r.salidas.add(acc.get(m)[1]);
            r.cuenta.add(acc.get(m)[2]);
        }
        return r;
    }

    static List<Filtros> porCriticidad(List<Filtros> ex) {
        List<Filtros> l = new ArrayList<>(ex);
        l.sort(Comparator.comparingInt((Filtros f) -> f.getStockActual() - f.getPuntoReorden()).thenComparing(Filtros::getIdFiltro));
        return l;
    }

    static int[] contarSemaforo(List<Filtros> ex) {
        int[] c = new int[3];
        for (Filtros f : ex) {
            switch (KardexService.semaforo(f.getStockActual(), f.getPuntoReorden())) {
                case KardexService.ROJO -> c[0]++;
                case KardexService.AMARILLO -> c[1]++;
                default -> c[2]++;
            }
        }
        return c;
    }


    public static void movimientosPdf(File destino, List<KardexFila> filas, List<Filtros> existencias, String filtros,
            String usuario, Resumen r) throws IOException {
        PdfDocumento pdf = new PdfDocumento("Kárdex de insumos y filtros", usuario, true);
        pdf.titulo("Kárdex de insumos y filtros", "Movimientos de inventario (entradas, salidas y ajustes). " + filtros);
        pdf.kpis(new String[]{"Existencias totales", "Entradas del periodo", "Salidas del periodo", "Insumos en alerta"},
                new String[]{r.existencias() + " uds", "+" + r.entradas() + " uds", "-" + r.salidas() + " uds",
                    r.insumosEnAlerta() + " (" + r.insumosEnRojo() + " en rojo)"},
                new int[]{PdfDocumento.GRIS_OSCURO, PdfDocumento.VERDE, PdfDocumento.ROJO, PdfDocumento.AMARILLO});

        PorMes pm = agruparPorMes(filas);
        List<String> nombresMes = new ArrayList<>();
        double[] ent = new double[pm.meses.size()];
        double[] sal = new double[pm.meses.size()];
        for (int i = 0; i < pm.meses.size(); i++) {
            nombresMes.add(ReporteDashboard.etiquetaMes(pm.meses.get(i).toString()));
            ent[i] = pm.entradas.get(i);
            sal[i] = pm.salidas.get(i);
        }
        int[] sem = contarSemaforo(existencias);

        pdf.seccion("Resumen gráfico", 260);
        pdf.enColumnas(250,
                () -> pdf.barrasV("Entradas y salidas por mes (uds)", nombresMes, new String[]{"Entradas", "Salidas"},
                        new double[][]{ent, sal}, new int[]{G_OSCURO, G_CLARO}, null, 250),
                () -> pdf.torta("Semáforo de existencias", new String[]{"Rojo - reponer", "Amarillo - pedir pronto", "Verde - OK"},
                        new double[]{sem[0], sem[1], sem[2]}, new int[]{PdfDocumento.ROJO, PdfDocumento.AMARILLO, PdfDocumento.VERDE}, 250));

        Map<String, int[]> porInsumo = new LinkedHashMap<>();
        for (KardexFila k : filas) {
            porInsumo.computeIfAbsent(k.getIdFiltro(), x -> new int[2]);
            porInsumo.get(k.getIdFiltro())[0] += k.getEntrada();
            porInsumo.get(k.getIdFiltro())[1] += k.getSalida();
        }
        List<Map.Entry<String, int[]>> top = new ArrayList<>(porInsumo.entrySet());
        top.sort((a, b) -> Integer.compare(b.getValue()[1], a.getValue()[1]));
        int nTop = Math.min(6, top.size());
        List<String> catTop = new ArrayList<>();
        double[][] valTop = new double[2][nTop];
        for (int i = 0; i < nTop; i++) {
            catTop.add(top.get(i).getKey());
            valTop[0][i] = top.get(i).getValue()[0];
            valTop[1][i] = top.get(i).getValue()[1];
        }
        List<Filtros> crit = porCriticidad(existencias);
        int nCrit = Math.min(6, crit.size());
        List<String> catCrit = new ArrayList<>();
        double[][] valCrit = new double[2][nCrit];
        int[] colCrit = new int[nCrit];
        for (int i = 0; i < nCrit; i++) {
            Filtros f = crit.get(i);
            catCrit.add(f.getIdFiltro());
            valCrit[0][i] = f.getStockActual();
            valCrit[1][i] = f.getPuntoReorden();
            colCrit[i] = colorPleno(KardexService.semaforo(f.getStockActual(), f.getPuntoReorden()));
        }
        pdf.enColumnas(270,
                () -> pdf.barrasH("Movimiento por insumo (uds, más activos)", catTop, new String[]{"Entradas", "Salidas"}, valTop,
                        new int[]{G_OSCURO, G_CLARO}, null),
                () -> pdf.barrasH("Stock vs. punto de reorden (más críticos)", catCrit, new String[]{"Stock", "Reorden"}, valCrit,
                        new int[]{G_OSCURO, G_CLARO}, null));

        pdf.seccion("Detalle de movimientos", 120);
        List<String[]> datos = new ArrayList<>();
        int totEntradas = 0, totSalidas = 0;
        for (KardexFila k : filas) {
            datos.add(new String[]{
                String.valueOf(k.getIdMovimiento()), fecha(k.getFechaHora()), k.getIdFiltro(), k.getDescripcion(),
                nz(k.getCategoria()), k.getTipo(), signo(k.getEntrada(), '+'), signo(k.getSalida(), '-'),
                String.valueOf(k.getSaldo()), String.valueOf(k.getPuntoReorden()), k.getResponsable()});
            totEntradas += k.getEntrada();
            totSalidas += k.getSalida();
        }
        final int filasReales = datos.size();
        if (!datos.isEmpty()) {
            datos.add(new String[]{"", "", "", "TOTAL DEL PERIODO", "", "", "+" + totEntradas, "-" + totSalidas, "", "", ""});
        }
        PdfDocumento.EstiloCelda estilo = new PdfDocumento.EstiloCelda() {
            @Override
            public Integer fondo(int fila, int col) {
                if (fila == filasReales) {
                    return PdfDocumento.GRIS_CLARO;
                }
                if (col == 8) {
                    KardexFila k = filas.get(fila);
                    return fondoSemaforo(KardexService.semaforo(k.getSaldo(), k.getPuntoReorden()));
                }
                return null;
            }

            @Override
            public Integer texto(int fila, int col) {
                if (fila == filasReales) {
                    return col == 6 ? Integer.valueOf(PdfDocumento.TEXTO_VERDE) : col == 7 ? Integer.valueOf(PdfDocumento.ROJO) : null;
                }
                KardexFila k = filas.get(fila);
                if (col == 5) {
                    if ("Entrada".equals(k.getTipo())) {
                        return PdfDocumento.TEXTO_VERDE;
                    }
                    return "Salida".equals(k.getTipo()) ? PdfDocumento.ROJO : Integer.valueOf(PdfDocumento.TEXTO_AMARILLO);
                }
                if (col == 6) {
                    return PdfDocumento.TEXTO_VERDE;
                }
                if (col == 7) {
                    return PdfDocumento.ROJO;
                }
                if (col == 8) {
                    return colorSemaforo(KardexService.semaforo(k.getSaldo(), k.getPuntoReorden()));
                }
                return null;
            }

            @Override
            public boolean negrita(int fila, int col) {
                return fila == filasReales || col == 5 || col == 6 || col == 7 || col == 8;
            }
        };
        pdf.tabla(new String[]{"ID", "Fecha y hora", "Código", "Insumo / descripción", "Categoría", "Tipo", "Entrada", "Salida",
            "Saldo", "Reorden", "Responsable"},
                new double[]{26, 98, 90, 112, 68, 54, 54, 50, 46, 56, 108},
                new char[]{'R', 'L', 'L', 'L', 'L', 'C', 'R', 'R', 'R', 'R', 'L'}, datos, estilo);
        pdf.parrafo("Color del saldo según el punto de reorden actual: rojo = por debajo (reponer), amarillo = en el punto o hasta 25% por encima, "
                + "verde = stock sano. En los ajustes, la diferencia contra el conteo físico se muestra como entrada (sobrante) o salida (faltante).",
                PdfDocumento.TAM, PdfDocumento.GRIS_OSCURO);
        pdf.guardar(destino);
    }

    public static void existenciasPdf(File destino, List<Filtros> filas, String filtros, String usuario) throws IOException {
        PdfDocumento pdf = new PdfDocumento("Existencias y reposición de insumos", usuario, true);
        pdf.titulo("Existencias y reposición de insumos", "Stock actual frente al punto de reorden. " + filtros);

        int total = 0;
        List<String[]> datos = new ArrayList<>();
        List<String> semaforos = new ArrayList<>();
        for (Filtros f : filas) {
            String sem = KardexService.semaforo(f.getStockActual(), f.getPuntoReorden());
            semaforos.add(sem);
            total += f.getStockActual();
            datos.add(new String[]{f.getIdFiltro(), f.getDescripcion(), nz(f.getCategoriaFiltro()), f.getUnidadMedida(),
                String.valueOf(f.getStockActual()), String.valueOf(f.getPuntoReorden()),
                String.valueOf(Math.max(0, f.getPuntoReorden() - f.getStockActual())),
                KardexService.etiquetaSemaforo(sem), fecha(f.getFechaActualizacion())});
        }
        int[] sem = contarSemaforo(filas);
        pdf.kpis(new String[]{"Insumos listados", "Rojo - reponer", "Amarillo - pedir pronto", "Verde - OK"},
                new String[]{filas.size() + " (" + total + " uds)", String.valueOf(sem[0]), String.valueOf(sem[1]), String.valueOf(sem[2])},
                new int[]{PdfDocumento.GRIS_OSCURO, PdfDocumento.ROJO, PdfDocumento.AMARILLO, PdfDocumento.VERDE});

        List<Filtros> crit = porCriticidad(filas);
        int n = Math.min(6, crit.size());
        List<String> cats = new ArrayList<>();
        double[][] val = new double[2][n];
        int[] col = new int[n];
        for (int i = 0; i < n; i++) {
            Filtros f = crit.get(i);
            cats.add(f.getIdFiltro());
            val[0][i] = f.getStockActual();
            val[1][i] = f.getPuntoReorden();
            col[i] = colorPleno(KardexService.semaforo(f.getStockActual(), f.getPuntoReorden()));
        }

        Map<String, Integer> porCat = new LinkedHashMap<>();
        for (Filtros f : filas) {
            porCat.merge(nz(f.getCategoriaFiltro()), f.getStockActual(), Integer::sum);
        }
        List<String> catsCat = new ArrayList<>(porCat.keySet());
        double[] stockCat = new double[catsCat.size()];
        for (int i = 0; i < stockCat.length; i++) {
            stockCat[i] = porCat.get(catsCat.get(i));
        }
        pdf.seccion("Resumen gráfico", 260);
        pdf.enColumnas(250,
                () -> pdf.torta("Semáforo de existencias", new String[]{"Rojo - reponer", "Amarillo - pedir pronto", "Verde - OK"},
                        new double[]{sem[0], sem[1], sem[2]}, new int[]{PdfDocumento.ROJO, PdfDocumento.AMARILLO, PdfDocumento.VERDE}, 250),
                () -> pdf.barrasV("Stock por categoría (uds)", catsCat, new String[]{"Stock"}, new double[][]{stockCat},
                        new int[]{G_OSCURO}, null, 250));
        pdf.enColumnas(270,
                () -> pdf.barrasH("Stock vs. punto de reorden (más críticos)", cats, new String[]{"Stock", "Reorden"}, val,
                        new int[]{G_OSCURO, G_CLARO}, null));

        pdf.seccion("Detalle de existencias", 120);
        PdfDocumento.EstiloCelda estilo = new PdfDocumento.EstiloCelda() {
            @Override
            public Integer fondo(int fila, int c) {
                return c == 7 ? fondoSemaforo(semaforos.get(fila)) : null;
            }

            @Override
            public Integer texto(int fila, int c) {
                return c == 7 ? colorSemaforo(semaforos.get(fila)) : null;
            }

            @Override
            public boolean negrita(int fila, int c) {
                return c == 7 || c == 4;
            }
        };
        pdf.tabla(new String[]{"Código", "Descripción", "Categoría", "Unidad", "Stock", "Reorden", "Faltante", "Estado", "Última actualización"},
                new double[]{80, 160, 70, 54, 46, 58, 58, 136, 104},
                new char[]{'L', 'L', 'L', 'L', 'R', 'R', 'R', 'L', 'L'}, datos, estilo);
        pdf.parrafo("Faltante = unidades que faltan para llegar al punto de reorden. Rojo: agotado o por debajo del punto de reorden. "
                + "Amarillo: en el punto de reorden o hasta 25% por encima.", PdfDocumento.TAM, PdfDocumento.GRIS_OSCURO);
        pdf.guardar(destino);
    }

    static String q(String hoja) {
        return "'" + hoja + "'!";
    }
    static String formulaEstado(String stock, String reorden) {
        return "IF(OR(" + stock + "<=0," + stock + "<" + reorden + "),\"ROJO - Reponer\",IF(" + stock + "<=ROUNDUP(" + reorden
                + "*1.25,0),\"AMARILLO - Pedir pronto\",\"VERDE - OK\"))";
    }

    static void semaforoDinamico(Hoja h, String rango, String stock, String reorden) {
        h.condicional(rango, "OR(" + stock + "<=0," + stock + "<" + reorden + ")", XlsxWriter.DXF_ROJO);
        h.condicional(rango, stock + "<=ROUNDUP(" + reorden + "*1.25,0)", XlsxWriter.DXF_AMARILLO);
        h.condicional(rango, stock + ">ROUNDUP(" + reorden + "*1.25,0)", XlsxWriter.DXF_VERDE);
    }

    record Rango(Hoja hoja, int ini, int fin) { }

    static Rango hojaExistencias(XlsxWriter wb, List<Filtros> ex, String titulo, String filtros, String usuario) {
        Hoja h = wb.hoja(H_EX);
        h.anchos(16, 38, 18, 11, 11, 11, 11, 27, 20);
        h.encabezado(1, titulo, "Generado el " + SALIDA.format(LocalDateTime.now()) + " por " + usuario, filtros);
        h.cabecera("Código", "Descripción", "Categoría", "Unidad", "Stock actual", "Reorden", "Faltante", "Estado", "Última actualización");
        int ini = h.numeroFilaActual();
        int totalStock = 0;
        for (Filtros f : ex) {
            String sem = KardexService.semaforo(f.getStockActual(), f.getPuntoReorden());
            int fila = h.numeroFilaActual();
            LocalDateTime t = parse(f.getFechaActualizacion());
            h.fila(new int[]{XlsxWriter.TXT, XlsxWriter.TXT, XlsxWriter.TXT, XlsxWriter.CENTRO, XlsxWriter.NUM_NEGRITA, XlsxWriter.NUM,
                XlsxWriter.NUM, XlsxWriter.ESTADO, XlsxWriter.FECHA},
                    f.getIdFiltro(), f.getDescripcion(), nz(f.getCategoriaFiltro()), f.getUnidadMedida(), f.getStockActual(), f.getPuntoReorden(),
                    new Formula("MAX(0,F" + fila + "-E" + fila + ")", Math.max(0, f.getPuntoReorden() - f.getStockActual())),
                    new FormulaTxt(formulaEstado("E" + fila, "F" + fila), KardexService.etiquetaSemaforo(sem)),
                    t == null ? null : (Object) XlsxWriter.serie(t));
            totalStock += f.getStockActual();
        }
        int fin = h.numeroFilaActual() - 1;
        if (!ex.isEmpty()) {
            h.finDatos();
            h.fila(new int[]{XlsxWriter.ETIQUETA, XlsxWriter.ETIQUETA, XlsxWriter.ETIQUETA, XlsxWriter.ETIQUETA, XlsxWriter.ETIQUETA_NUM,
                XlsxWriter.ETIQUETA_NUM, XlsxWriter.ETIQUETA_NUM},
                    "TOTAL (filas visibles)", "", "", "",
                    new Formula("SUBTOTAL(109,E" + ini + ":E" + fin + ")", totalStock), "",
                    new Formula("SUBTOTAL(109,G" + ini + ":G" + fin + ")", sumaFaltante(ex)));
            semaforoDinamico(h, "E" + ini + ":E" + fin + " H" + ini + ":H" + fin, "$E" + ini, "$F" + ini);
            h.condicional("A" + ini + ":I" + fin, "MOD(SUBTOTAL(103,$A$" + ini + ":$A" + ini + "),2)=0", XlsxWriter.DXF_ZEBRA);
        }
        return new Rango(h, ini, Math.max(fin, ini));
    }

    private static int sumaFaltante(List<Filtros> ex) {
        int s = 0;
        for (Filtros f : ex) {
            s += Math.max(0, f.getPuntoReorden() - f.getStockActual());
        }
        return s;
    }

    static Rango hojaMovimientos(XlsxWriter wb, List<KardexFila> filas, String filtros, String usuario) {
        Hoja h = wb.hoja(H_MOV);
        h.anchos(8, 19, 15, 36, 18, 11, 10, 10, 10, 10, 24);
        h.encabezado(2, "Kárdex de insumos y filtros", "Generado el " + SALIDA.format(LocalDateTime.now()) + " por " + usuario, filtros);
        h.cabecera("ID", "Fecha y hora", "Código", "Insumo / descripción", "Categoría", "Tipo", "Entrada", "Salida", "Saldo", "Reorden", "Responsable");
        int ini = h.numeroFilaActual();
        int totEntradas = 0, totSalidas = 0;
        for (KardexFila k : filas) {
            LocalDateTime t = parse(k.getFechaHora());
            h.fila(new int[]{XlsxWriter.CENTRO, XlsxWriter.FECHA, XlsxWriter.TXT, XlsxWriter.TXT, XlsxWriter.TXT, XlsxWriter.CENTRO_NEGRITA,
                XlsxWriter.NUM_VERDE, XlsxWriter.NUM_ROJO, XlsxWriter.NUM_NEGRITA, XlsxWriter.NUM, XlsxWriter.TXT},
                    k.getIdMovimiento(), t == null ? null : (Object) XlsxWriter.serie(t), k.getIdFiltro(), k.getDescripcion(), nz(k.getCategoria()),
                    k.getTipo(), k.getEntrada() == 0 ? null : (Object) k.getEntrada(), k.getSalida() == 0 ? null : (Object) k.getSalida(),
                    k.getSaldo(), k.getPuntoReorden(), k.getResponsable());
            totEntradas += k.getEntrada();
            totSalidas += k.getSalida();
        }
        int fin = h.numeroFilaActual() - 1;
        if (!filas.isEmpty()) {
            h.finDatos();

            h.fila(new int[]{XlsxWriter.ETIQUETA, XlsxWriter.ETIQUETA, XlsxWriter.ETIQUETA, XlsxWriter.ETIQUETA, XlsxWriter.ETIQUETA,
                XlsxWriter.ETIQUETA, XlsxWriter.ETIQUETA_NUM, XlsxWriter.ETIQUETA_NUM},
                    "", "", "", "TOTAL (filas visibles)", "", "",
                    new Formula("SUBTOTAL(109,G" + ini + ":G" + fin + ")", totEntradas),
                    new Formula("SUBTOTAL(109,H" + ini + ":H" + fin + ")", totSalidas));
            semaforoDinamico(h, "I" + ini + ":I" + fin, "$I" + ini, "$J" + ini);
            h.condicional("F" + ini + ":F" + fin, "$F" + ini + "=\"Entrada\"", XlsxWriter.DXF_TEXTO_VERDE);
            h.condicional("F" + ini + ":F" + fin, "$F" + ini + "=\"Salida\"", XlsxWriter.DXF_TEXTO_ROJO);
            h.condicional("F" + ini + ":F" + fin, "$F" + ini + "=\"Ajuste\"", XlsxWriter.DXF_TEXTO_OCRE);
            h.condicional("A" + ini + ":K" + fin, "MOD(SUBTOTAL(103,$A$" + ini + ":$A" + ini + "),2)=0", XlsxWriter.DXF_ZEBRA);
        }
        return new Rango(h, ini, Math.max(fin, ini));
    }

    static void seccion(Hoja h, String texto, int columnas) {
        int[] est = new int[columnas];
        java.util.Arrays.fill(est, XlsxWriter.SECCION);
        Object[] celdas = new Object[columnas];
        celdas[0] = texto;
        h.fila(est, celdas);
    }

    static class Piv {
        Hoja hoja;
        XlsxWriter.Ref mesesCat, mesesEnt, mesesSal;
        XlsxWriter.Ref insCat, insEnt, insSal;
        XlsxWriter.Ref semCat, semVal;
        XlsxWriter.Ref catCat, catRojo, catAmarillo, catVerde, catStock;
        XlsxWriter.Ref critCat, critStock, critReorden;
        XlsxWriter.Ref fichaCat, fichaEnt, fichaSal;
    }

    static Piv hojaPivotes(XlsxWriter wb, List<KardexFila> mov, List<Filtros> ex, String usuario, String selector, String filtros) {
        Piv pv = new Piv();
        Hoja p = wb.hoja(H_PIV);
        pv.hoja = p;
        p.sinFiltro().anchos(20, 38, 14, 14, 14, 14);
        p.encabezado(1, "Tablas dinámicas", "Se recalculan solas al editar las hojas " + (mov != null ? "Kárdex y " : "") + "Existencias",
                "Generado el " + SALIDA.format(LocalDateTime.now()) + " por " + usuario);
        final int cols = 6;
        String E = q(H_EX);
        String K = q(H_MOV);
        boolean conMov = mov != null;

        TreeSet<String> cats = new TreeSet<>();
        for (Filtros f : ex) {
            cats.add(nz(f.getCategoriaFiltro()));
        }
        if (conMov) {
            for (KardexFila k : mov) {
                cats.add(nz(k.getCategoria()));
            }
        }
        Map<String, int[]> exCat = new HashMap<>();
        Map<String, Filtros> exPorCodigo = new HashMap<>();
        for (Filtros f : ex) {
            int[] a = exCat.computeIfAbsent(nz(f.getCategoriaFiltro()), x -> new int[5]);
            a[0]++;
            switch (KardexService.semaforo(f.getStockActual(), f.getPuntoReorden())) {
                case KardexService.ROJO -> a[1]++;
                case KardexService.AMARILLO -> a[2]++;
                default -> a[3]++;
            }
            a[4] += f.getStockActual();
            exPorCodigo.put(f.getIdFiltro(), f);
        }
        int bloque = 1;

        if (conMov) {

            Map<String, int[]> movCat = new HashMap<>();
            for (KardexFila k : mov) {
                int[] a = movCat.computeIfAbsent(nz(k.getCategoria()), x -> new int[3]);
                a[0] += k.getEntrada();
                a[1] += k.getSalida();
                a[2]++;
            }
            seccion(p, (bloque++) + ". Movimientos por categoría", cols);
            p.cabeceraSecundaria(0, "Categoría", "Insumos registrados", "Entradas", "Salidas", "Neto", "N.º de movimientos");
            int ini = p.numeroFilaActual();
            for (String c : cats) {
                int r = p.numeroFilaActual();
                int[] m = movCat.getOrDefault(c, new int[3]);
                int[] e = exCat.getOrDefault(c, new int[5]);
                p.fila(new int[]{XlsxWriter.TXT, XlsxWriter.NUM, XlsxWriter.NUM_VERDE, XlsxWriter.NUM_ROJO, XlsxWriter.NUM_NEGRITA, XlsxWriter.NUM},
                        c, new Formula("COUNTIFS(" + E + "$C:$C,$A" + r + ")", e[0]),
                        new Formula("SUMIFS(" + K + "$G:$G," + K + "$E:$E,$A" + r + ")", m[0]),
                        new Formula("SUMIFS(" + K + "$H:$H," + K + "$E:$E,$A" + r + ")", m[1]),
                        new Formula("C" + r + "-D" + r, m[0] - m[1]),
                        new Formula("COUNTIFS(" + K + "$E:$E,$A" + r + ")", m[2]));
            }
            int fin = p.numeroFilaActual() - 1;
            totales(p, ini, fin, cols, 1, 2, 3, 4, 5);
            p.vacia();

            PorMes pm = agruparPorMes(mov);
            seccion(p, (bloque++) + ". Movimientos por mes", cols);
            p.cabeceraSecundaria(0, "Mes", "Desde", "Entradas", "Salidas", "Neto", "N.º de movimientos");
            int ini2 = p.numeroFilaActual();
            for (int i = 0; i < pm.meses.size(); i++) {
                int r = p.numeroFilaActual();
                YearMonth ym = pm.meses.get(i);
                String criterio = K + "$B:$B,\">=\"&$B" + r + "," + K + "$B:$B,\"<\"&EDATE($B" + r + ",1)";
                p.fila(new int[]{XlsxWriter.TXT, XlsxWriter.FECHA_CORTA, XlsxWriter.NUM_VERDE, XlsxWriter.NUM_ROJO, XlsxWriter.NUM_NEGRITA, XlsxWriter.NUM},
                        ReporteDashboard.etiquetaMes(ym.toString()), XlsxWriter.serie(ym.atDay(1).atStartOfDay()),
                        new Formula("SUMIFS(" + K + "$G:$G," + criterio + ")", pm.entradas.get(i)),
                        new Formula("SUMIFS(" + K + "$H:$H," + criterio + ")", pm.salidas.get(i)),
                        new Formula("C" + r + "-D" + r, pm.entradas.get(i) - pm.salidas.get(i)),
                        new Formula("COUNTIFS(" + criterio + ")", pm.cuenta.get(i)));
            }
            int fin2 = p.numeroFilaActual() - 1;
            totales(p, ini2, fin2, cols, 2, 3, 4, 5);
            pv.mesesCat = p.ref(0, ini2, fin2);
            pv.mesesEnt = p.ref(2, ini2, fin2);
            pv.mesesSal = p.ref(3, ini2, fin2);
            p.vacia();

            Map<String, int[]> movIns = new LinkedHashMap<>();
            Map<String, String> descIns = new HashMap<>();
            for (KardexFila k : mov) {
                int[] a = movIns.computeIfAbsent(k.getIdFiltro(), x -> new int[3]);
                a[0] += k.getEntrada();
                a[1] += k.getSalida();
                a[2]++;
                descIns.put(k.getIdFiltro(), k.getDescripcion());
            }
            List<Map.Entry<String, int[]>> orden = new ArrayList<>(movIns.entrySet());
            orden.sort((a, b) -> Integer.compare(b.getValue()[1], a.getValue()[1]));
            if (orden.size() > 15) {
                orden = orden.subList(0, 15);
            }
            seccion(p, (bloque++) + ". Movimientos por insumo (los 15 con más salidas)", cols);
            p.cabeceraSecundaria(0, "Código", "Insumo", "Entradas", "Salidas", "Neto", "N.º de movimientos");
            int ini3 = p.numeroFilaActual();
            for (Map.Entry<String, int[]> en : orden) {
                int r = p.numeroFilaActual();
                int[] m = en.getValue();
                p.fila(new int[]{XlsxWriter.TXT, XlsxWriter.TXT, XlsxWriter.NUM_VERDE, XlsxWriter.NUM_ROJO, XlsxWriter.NUM_NEGRITA, XlsxWriter.NUM},
                        en.getKey(),
                        new FormulaTxt("IFERROR(INDEX(" + E + "$B:$B,MATCH($A" + r + "," + E + "$A:$A,0)),\"\")",
                                exPorCodigo.containsKey(en.getKey()) ? exPorCodigo.get(en.getKey()).getDescripcion() : descIns.get(en.getKey())),
                        new Formula("SUMIFS(" + K + "$G:$G," + K + "$C:$C,$A" + r + ")", m[0]),
                        new Formula("SUMIFS(" + K + "$H:$H," + K + "$C:$C,$A" + r + ")", m[1]),
                        new Formula("C" + r + "-D" + r, m[0] - m[1]),
                        new Formula("COUNTIFS(" + K + "$C:$C,$A" + r + ")", m[2]));
            }
            int fin3 = p.numeroFilaActual() - 1;
            if (orden.isEmpty()) {
                p.fila(new int[]{XlsxWriter.TXT}, "Sin movimientos");
                fin3 = p.numeroFilaActual() - 1;
            }
            int topFin = Math.min(fin3, ini3 + 9);
            pv.insCat = p.ref(0, ini3, topFin);
            pv.insEnt = p.ref(2, ini3, topFin);
            pv.insSal = p.ref(3, ini3, topFin);
            p.vacia();
        }

        int[] sem = contarSemaforo(ex);
        seccion(p, (bloque++) + ". Semáforo global de existencias", cols);
        p.cabeceraSecundaria(0, "Estado", "Significado", "Insumos");
        int iniS = p.numeroFilaActual();
        String[][] estados = {{"Rojo", "Agotado o por debajo del punto de reorden", "ROJO - Reponer"},
            {"Amarillo", "En el punto de reorden o hasta 25% por encima", "AMARILLO - Pedir pronto"},
            {"Verde", "Stock sano", "VERDE - OK"}};
        for (int i = 0; i < 3; i++) {
            p.fila(new int[]{XlsxWriter.TXT_NEGRITA, XlsxWriter.TXT, XlsxWriter.NUM_NEGRITA},
                    estados[i][0], estados[i][1], new Formula("COUNTIFS(" + E + "$H:$H,\"" + estados[i][2] + "\")", sem[i]));
        }
        p.condicional("A" + iniS, "TRUE", XlsxWriter.DXF_ROJO);
        p.condicional("A" + (iniS + 1), "TRUE", XlsxWriter.DXF_AMARILLO);
        p.condicional("A" + (iniS + 2), "TRUE", XlsxWriter.DXF_VERDE);
        pv.semCat = p.ref(0, iniS, iniS + 2);
        pv.semVal = p.ref(2, iniS, iniS + 2);
        p.vacia();

        seccion(p, (bloque++) + ". Existencias por categoría", cols);
        p.cabeceraSecundaria(0, "Categoría", "Insumos", "Rojo", "Amarillo", "Verde", "Stock total (uds)");
        int iniC = p.numeroFilaActual();
        for (String c : cats) {
            int r = p.numeroFilaActual();
            int[] e = exCat.getOrDefault(c, new int[5]);
            p.fila(new int[]{XlsxWriter.TXT, XlsxWriter.NUM, XlsxWriter.NUM_ROJO, XlsxWriter.NUM_NEGRITA, XlsxWriter.NUM_VERDE, XlsxWriter.NUM_NEGRITA},
                    c, new Formula("COUNTIFS(" + E + "$C:$C,$A" + r + ")", e[0]),
                    new Formula("COUNTIFS(" + E + "$C:$C,$A" + r + "," + E + "$H:$H,\"ROJO - Reponer\")", e[1]),
                    new Formula("COUNTIFS(" + E + "$C:$C,$A" + r + "," + E + "$H:$H,\"AMARILLO - Pedir pronto\")", e[2]),
                    new Formula("COUNTIFS(" + E + "$C:$C,$A" + r + "," + E + "$H:$H,\"VERDE - OK\")", e[3]),
                    new Formula("SUMIFS(" + E + "$E:$E," + E + "$C:$C,$A" + r + ")", e[4]));
        }
        int finC = p.numeroFilaActual() - 1;
        totales(p, iniC, finC, cols, 1, 2, 3, 4, 5);
        pv.catCat = p.ref(0, iniC, finC);
        pv.catRojo = p.ref(2, iniC, finC);
        pv.catAmarillo = p.ref(3, iniC, finC);
        pv.catVerde = p.ref(4, iniC, finC);
        pv.catStock = p.ref(5, iniC, finC);
        p.vacia();

        List<Filtros> crit = porCriticidad(ex);
        if (crit.size() > 10) {
            crit = crit.subList(0, 10);
        }
        seccion(p, (bloque++) + ". Insumos más críticos (stock frente al punto de reorden)", cols);
        p.cabeceraSecundaria(0, "Código", "Insumo", "Stock", "Reorden", "Faltante", "Estado");
        int iniK = p.numeroFilaActual();
        for (Filtros f : crit) {
            int r = p.numeroFilaActual();
            String sm = KardexService.semaforo(f.getStockActual(), f.getPuntoReorden());
            p.fila(new int[]{XlsxWriter.TXT, XlsxWriter.TXT, XlsxWriter.NUM_NEGRITA, XlsxWriter.NUM, XlsxWriter.NUM, XlsxWriter.ESTADO},
                    f.getIdFiltro(),
                    new FormulaTxt("IFERROR(INDEX(" + E + "$B:$B,MATCH($A" + r + "," + E + "$A:$A,0)),\"\")", f.getDescripcion()),
                    new Formula("IFERROR(INDEX(" + E + "$E:$E,MATCH($A" + r + "," + E + "$A:$A,0)),0)", f.getStockActual()),
                    new Formula("IFERROR(INDEX(" + E + "$F:$F,MATCH($A" + r + "," + E + "$A:$A,0)),0)", f.getPuntoReorden()),
                    new Formula("MAX(0,D" + r + "-C" + r + ")", Math.max(0, f.getPuntoReorden() - f.getStockActual())),
                    new FormulaTxt(formulaEstado("C" + r, "D" + r), KardexService.etiquetaSemaforo(sm)));
        }
        int finK = p.numeroFilaActual() - 1;
        if (crit.isEmpty()) {
            p.fila(new int[]{XlsxWriter.TXT}, "Sin insumos");
            finK = p.numeroFilaActual() - 1;
        }
        semaforoDinamico(p, "C" + iniK + ":C" + finK + " F" + iniK + ":F" + finK, "$C" + iniK, "$D" + iniK);
        pv.critCat = p.ref(0, iniK, finK);
        pv.critStock = p.ref(2, iniK, finK);
        pv.critReorden = p.ref(3, iniK, finK);
        p.vacia();

        if (conMov) {
            PorMes base = agruparPorMes(mov);
            String codigoSel = selector;
            seccion(p, (bloque++) + ". Ficha del insumo elegido en la hoja Gráficas (movimientos por mes)", cols);
            p.cabeceraSecundaria(0, "Mes", "Desde", "Entradas", "Salidas", "Neto", "N.º de movimientos");
            int iniF = p.numeroFilaActual();
            String sel = q(H_GRA) + "$B$6";
            for (int i = 0; i < base.meses.size(); i++) {
                int r = p.numeroFilaActual();
                YearMonth ym = base.meses.get(i);
                int en = 0, sa = 0, n = 0;
                for (KardexFila k : mov) {
                    LocalDateTime t = parse(k.getFechaHora());
                    if (t != null && YearMonth.from(t).equals(ym) && k.getIdFiltro().equals(codigoSel)) {
                        en += k.getEntrada();
                        sa += k.getSalida();
                        n++;
                    }
                }
                String criterio = K + "$C:$C," + sel + "," + K + "$B:$B,\">=\"&$B" + r + "," + K + "$B:$B,\"<\"&EDATE($B" + r + ",1)";
                p.fila(new int[]{XlsxWriter.TXT, XlsxWriter.FECHA_CORTA, XlsxWriter.NUM_VERDE, XlsxWriter.NUM_ROJO, XlsxWriter.NUM_NEGRITA, XlsxWriter.NUM},
                        ReporteDashboard.etiquetaMes(ym.toString()), XlsxWriter.serie(ym.atDay(1).atStartOfDay()),
                        new Formula("SUMIFS(" + K + "$G:$G," + criterio + ")", en),
                        new Formula("SUMIFS(" + K + "$H:$H," + criterio + ")", sa),
                        new Formula("C" + r + "-D" + r, en - sa),
                        new Formula("COUNTIFS(" + criterio + ")", n));
            }
            int finF = p.numeroFilaActual() - 1;
            pv.fichaCat = p.ref(0, iniF, finF);
            pv.fichaEnt = p.ref(2, iniF, finF);
            pv.fichaSal = p.ref(3, iniF, finF);
        }
        return pv;
    }

    private static void totales(Hoja p, int ini, int fin, int cols, int... sumar) {
        Object[] celdas = new Object[cols];
        int[] est = new int[cols];
        java.util.Arrays.fill(est, XlsxWriter.ETIQUETA);
        celdas[0] = "TOTAL";
        for (int c : sumar) {
            double suma = 0;
            for (int r = ini; r <= fin; r++) {
                if (p.ref(c, r, r).valor(1) instanceof Number n) {
                    suma += n.doubleValue();
                }
            }
            String col = XlsxWriter.columna(c);
            celdas[c] = new Formula("SUM(" + col + ini + ":" + col + fin + ")", suma);
            est[c] = XlsxWriter.ETIQUETA_NUM;
        }
        p.fila(est, celdas);
    }

    private static void hojaGraficas(XlsxWriter wb, Hoja gr, Piv pv, Rango rEx, List<Filtros> ex, List<KardexFila> mov, String usuario,
            String codigoSel) {
        boolean conMov = mov != null;
        String E = q(H_EX);
        String K = q(H_MOV);
        gr.sinFiltro().anchos(21, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14);
        gr.encabezado(1, "Gráficas y ficha dinámica", "Elija un insumo en la celda sombreada: la ficha y su gráfica se actualizan solas",
                "Generado el " + SALIDA.format(LocalDateTime.now()) + " por " + usuario);
        seccion(gr, "Ficha dinámica del insumo", 4);
        Filtros f = null;
        for (Filtros x : ex) {
            if (x.getIdFiltro().equals(codigoSel)) {
                f = x;
            }
        }
        int stock = f == null ? 0 : f.getStockActual();
        int reorden = f == null ? 0 : f.getPuntoReorden();
        int en = 0, sa = 0, nm = 0;
        if (conMov) {
            for (KardexFila k : mov) {
                if (k.getIdFiltro().equals(codigoSel)) {
                    en += k.getEntrada();
                    sa += k.getSalida();
                    nm++;
                }
            }
        }
        String sel = "$B$6";
        String idx = "MATCH(" + sel + "," + E + "$A:$A,0)";

        gr.fila(new int[]{XlsxWriter.ETIQUETA, XlsxWriter.SELECTOR, XlsxWriter.SELECTOR}, "Insumo (elija)", codigoSel == null ? "" : codigoSel, null);
        gr.combinar("B6:C6");
        if (!ex.isEmpty()) {
            gr.lista("B6", E + "$A$" + rEx.ini() + ":$A$" + rEx.fin());
        }
        String[] et = {"Descripción", "Categoría", "Stock actual", "Punto de reorden", "Faltante", "Estado"};
        Object[] val = {
            new FormulaTxt("IFERROR(INDEX(" + E + "$B:$B," + idx + "),\"\")", f == null ? "" : f.getDescripcion()),
            new FormulaTxt("IFERROR(INDEX(" + E + "$C:$C," + idx + "),\"\")", f == null ? "" : nz(f.getCategoriaFiltro())),
            new Formula("IFERROR(INDEX(" + E + "$E:$E," + idx + "),0)", stock),
            new Formula("IFERROR(INDEX(" + E + "$F:$F," + idx + "),0)", reorden),
            new Formula("MAX(0,B10-B9)", Math.max(0, reorden - stock)),
            new FormulaTxt(formulaEstado("B9", "B10"), KardexService.etiquetaSemaforo(KardexService.semaforo(stock, reorden)))};
        int[] est = {XlsxWriter.CENTRO_NEGRITA, XlsxWriter.CENTRO_NEGRITA, XlsxWriter.KPI_VALOR, XlsxWriter.KPI_VALOR, XlsxWriter.KPI_VALOR, XlsxWriter.ESTADO};
        for (int i = 0; i < et.length; i++) {
            gr.fila(new int[]{XlsxWriter.ETIQUETA, est[i], est[i]}, et[i], val[i], null);
            gr.combinar("B" + (gr.numeroFilaActual() - 1) + ":C" + (gr.numeroFilaActual() - 1));
        }
        if (conMov) {
            String[] et2 = {"Entradas (total)", "Salidas (total)", "N.º de movimientos"};
            Object[] val2 = {
                new Formula("SUMIFS(" + K + "$G:$G," + K + "$C:$C," + sel + ")", en),
                new Formula("SUMIFS(" + K + "$H:$H," + K + "$C:$C," + sel + ")", sa),
                new Formula("COUNTIFS(" + K + "$C:$C," + sel + ")", nm)};
            for (int i = 0; i < 3; i++) {
                gr.fila(new int[]{XlsxWriter.ETIQUETA, XlsxWriter.KPI_VALOR, XlsxWriter.KPI_VALOR}, et2[i], val2[i], null);
                gr.combinar("B" + (gr.numeroFilaActual() - 1) + ":C" + (gr.numeroFilaActual() - 1));
            }
        }

        semaforoDinamico(gr, "B12:C12", "$B$9", "$B$10");
        while (gr.numeroFilaActual() <= 17) {
            gr.vacia();
        }

        if (conMov) {
            gr.grafico(Grafico.columnas("Entradas y salidas por mes del insumo elegido")
                    .categorias(pv.fichaCat).serie("Entradas", pv.fichaEnt, G_OSCURO).serie("Salidas", pv.fichaSal, G_CLARO),
                    4, 6, 12, 18);
        } else {
            gr.grafico(Grafico.columnas("Stock frente al punto de reorden del insumo elegido")
                    .categorias(gr.ref(0, 9, 10)).serie("Unidades", gr.ref(1, 9, 10), new int[]{G_OSCURO, G_CLARO}).sinLeyenda(),
                    4, 6, 12, 18);
        }
        gr.vacia();
        seccion(gr, "Panel general", 12);
        int inicioPanel = gr.numeroFilaActual();
        gr.reservar(19);
        gr.vacia();
        gr.reservar(19);
        int p1 = inicioPanel, p2 = inicioPanel + 20;
        gr.saltoPagina(p2);
        if (conMov) {
            gr.grafico(Grafico.columnas("Entradas y salidas por mes (uds)").categorias(pv.mesesCat)
                    .serie("Entradas", pv.mesesEnt, G_OSCURO).serie("Salidas", pv.mesesSal, G_CLARO), 0, p1, 6, p1 + 19);
            gr.grafico(Grafico.torta("Semáforo de existencias").categorias(pv.semCat)
                    .serie("Insumos", pv.semVal, new int[]{C_ROJO, C_AMARILLO, C_VERDE}), 6, p1, 12, p1 + 19);
            gr.grafico(Grafico.barras("Movimiento por insumo (los más activos)").categorias(pv.insCat)
                    .serie("Entradas", pv.insEnt, G_OSCURO).serie("Salidas", pv.insSal, G_CLARO), 0, p2, 6, p2 + 19);
            gr.grafico(Grafico.barras("Stock vs. punto de reorden (más críticos)").categorias(pv.critCat)
                    .serie("Stock", pv.critStock, G_OSCURO).serie("Reorden", pv.critReorden, G_CLARO), 6, p2, 12, p2 + 19);
        } else {
            gr.grafico(Grafico.torta("Semáforo de existencias").categorias(pv.semCat)
                    .serie("Insumos", pv.semVal, new int[]{C_ROJO, C_AMARILLO, C_VERDE}), 0, p1, 6, p1 + 19);
            gr.grafico(Grafico.columnas("Semáforo por categoría (insumos)").categorias(pv.catCat).apilado()
                    .serie("Rojo", pv.catRojo, C_ROJO).serie("Amarillo", pv.catAmarillo, C_AMARILLO)
                    .serie("Verde", pv.catVerde, C_VERDE), 6, p1, 12, p1 + 19);
            gr.grafico(Grafico.barras("Stock vs. punto de reorden (más críticos)").categorias(pv.critCat)
                    .serie("Stock", pv.critStock, G_OSCURO).serie("Reorden", pv.critReorden, G_CLARO), 0, p2, 6, p2 + 19);
            gr.grafico(Grafico.columnas("Stock total por categoría (uds)").categorias(pv.catCat)
                    .serie("Stock", pv.catStock, G_OSCURO).sinLeyenda(), 6, p2, 12, p2 + 19);
        }
    }

    private static final int C_ROJO = PdfDocumento.ROJO;
    private static final int C_AMARILLO = PdfDocumento.AMARILLO;
    private static final int C_VERDE = PdfDocumento.VERDE;


    public static void movimientosExcel(File destino, List<KardexFila> filas, List<Filtros> existencias, String filtros,
            String usuario, Resumen r) throws IOException {
        XlsxWriter wb = new XlsxWriter();
        hojaMovimientos(wb, filas, filtros, usuario);
        Rango rEx = hojaExistencias(wb, existencias, "Existencias y reposición de insumos",
                "Stock actual de todos los insumos frente a su punto de reorden", usuario);
        String sel = elegirInsumo(filas, existencias, true);
        Piv pv = hojaPivotes(wb, filas, existencias, usuario, sel, filtros);
        Hoja gr = wb.hoja(H_GRA);
        hojaGraficas(wb, gr, pv, rEx, existencias, filas, usuario, sel);
        wb.guardar(destino);
    }

    public static void existenciasExcel(File destino, List<Filtros> filas, String filtros, String usuario) throws IOException {
        XlsxWriter wb = new XlsxWriter();
        Rango rEx = hojaExistencias(wb, filas, "Existencias y reposición de insumos", filtros, usuario);
        String sel = elegirInsumo(null, filas, false);
        Piv pv = hojaPivotes(wb, null, filas, usuario, sel, filtros);
        Hoja gr = wb.hoja(H_GRA);
        hojaGraficas(wb, gr, pv, rEx, filas, null, usuario, sel);
        wb.guardar(destino);
    }

    private static String elegirInsumo(List<KardexFila> mov, List<Filtros> ex, boolean porMovimientos) {
        if (ex.isEmpty()) {
            return "";
        }
        if (porMovimientos && mov != null && !mov.isEmpty()) {
            Map<String, Integer> n = new HashMap<>();
            for (KardexFila k : mov) {
                n.merge(k.getIdFiltro(), 1, Integer::sum);
            }
            String mejor = null;
            for (Filtros f : porCriticidad(ex)) {
                if (n.containsKey(f.getIdFiltro()) && (mejor == null || n.get(f.getIdFiltro()) > n.get(mejor))) {
                    mejor = f.getIdFiltro();
                }
            }
            if (mejor != null) {
                return mejor;
            }
        }
        return porCriticidad(ex).get(0).getIdFiltro();
    }
}
