package com.ryrcontrolcenter.reportes;

import com.ryrcontrolcenter.modelo.DashboardResumen;
import com.ryrcontrolcenter.modelo.Filtros;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

public class ReporteDashboard {

    private static final DateTimeFormatter SALIDA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final int G_OSCURO = ReporteKardex.G_OSCURO;
    private static final int G_MEDIO = ReporteKardex.G_MEDIO;
    private static final int G_CLARO = ReporteKardex.G_CLARO;

    private static final String[] MESES = {"Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"};

    public static String etiquetaMes(String yyyyMm) {
        try {
            YearMonth ym = YearMonth.parse(yyyyMm);
            return MESES[ym.getMonthValue() - 1] + " " + ym.getYear();
        } catch (RuntimeException e) {
            return yyyyMm;
        }
    }

    public static String etiquetaPeriodo(int meses) {
        return meses == 1 ? "mes actual" : "últimos " + meses + " meses";
    }


    public static void pdf(File destino, DashboardResumen r, String usuario, int meses) throws IOException {
        PdfDocumento pdf = new PdfDocumento("Dashboard gerencial", usuario, false);
        pdf.titulo("Dashboard gerencial",
                "Inventario rotativo, estado de insumos y actividad reciente (" + etiquetaPeriodo(meses) + ").");

        int neutro = PdfDocumento.GRIS_OSCURO;
        pdf.kpis(new String[]{"Máquinas", "Herramientas", "Kits", "Préstamos activos"},
                new String[]{String.valueOf(r.maquinas), String.valueOf(r.herramientas), String.valueOf(r.kits),
                    r.prestamosActivos + (r.prestamosAtrasados > 0 ? " (" + r.prestamosAtrasados + " atrasados)" : "")},
                new int[]{neutro, neutro, neutro, r.prestamosAtrasados > 0 ? PdfDocumento.ROJO : neutro});
        pdf.kpis(new String[]{"Ocupación", "Devoluciones", "Aprobados SST", "Bloq. / Mant. SST"},
                new String[]{r.ocupacionPorcentaje() + "%", String.valueOf(r.devoluciones), String.valueOf(r.aprobados),
                    String.valueOf(r.bloqueadosMantenimiento)},
                new int[]{neutro, neutro, PdfDocumento.VERDE, r.bloqueadosMantenimiento > 0 ? PdfDocumento.ROJO : PdfDocumento.VERDE});
        pdf.kpis(new String[]{"Insumos (referencias)", "Stock en rojo", "Stock en amarillo", "Stock en verde"},
                new String[]{String.valueOf(r.filtros), String.valueOf(r.semRojo), String.valueOf(r.semAmarillo), String.valueOf(r.semVerde)},
                new int[]{neutro, PdfDocumento.ROJO, PdfDocumento.AMARILLO, PdfDocumento.VERDE});


        pdf.seccion("Resumen gráfico", 260);
        int enUso = Math.max(0, r.prestamosActivos - r.prestamosAtrasados);
        pdf.enColumnas(230,
                () -> pdf.torta("Bodega vs. prestado", new String[]{"En bodega", "Prestados"},
                        new double[]{r.enBodega(), r.prestados}, new int[]{G_CLARO, G_OSCURO}, 230),
                () -> pdf.torta("Semáforo de existencias", new String[]{"Rojo", "Amarillo", "Verde"},
                        new double[]{r.semRojo, r.semAmarillo, r.semVerde},
                        new int[]{PdfDocumento.ROJO, PdfDocumento.AMARILLO, PdfDocumento.VERDE}, 230));

        List<String> nombresMes = new ArrayList<>();
        double[] ent = new double[r.meses.size()];
        double[] sal = new double[r.meses.size()];
        double[] pre = new double[r.meses.size()];
        List<String[]> filasMes = new ArrayList<>();
        for (int i = 0; i < r.meses.size(); i++) {
            String m = etiquetaMes(r.meses.get(i));
            nombresMes.add(m);
            ent[i] = r.entradasMes.get(i);
            sal[i] = r.salidasMes.get(i);
            pre[i] = r.prestamosMes.get(i);
            filasMes.add(new String[]{m, String.valueOf(r.entradasMes.get(i)), String.valueOf(r.salidasMes.get(i)),
                String.valueOf(r.entradasMes.get(i) - r.salidasMes.get(i)), String.valueOf(r.prestamosMes.get(i))});
        }
        pdf.seccion("Actividad mensual (" + etiquetaPeriodo(meses) + ")", 260);
        pdf.barrasV("Entradas y salidas de insumos por mes (uds)", nombresMes, new String[]{"Entradas", "Salidas"},
                new double[][]{ent, sal}, new int[]{G_OSCURO, G_CLARO}, null, 230);
        pdf.espacio(6);
        pdf.lineas("Tendencia: entradas, salidas y préstamos", nombresMes, new String[]{"Entradas", "Salidas", "Préstamos"},
                new double[][]{ent, sal, pre}, new int[]{0x000000, G_MEDIO, 0x595959}, 220);
        pdf.espacio(6);
        pdf.tabla(new String[]{"Mes", "Entradas", "Salidas", "Neto", "Préstamos"}, new double[]{30, 18, 18, 18, 18},
                new char[]{'L', 'R', 'R', 'R', 'R'}, filasMes, null);

        pdf.seccion("Préstamos y reposición", 260);
        pdf.enColumnas(230,
                () -> pdf.torta("Préstamos por estado", new String[]{"En uso", "Atrasados", "Devueltos"},
                        new double[]{enUso, r.prestamosAtrasados, r.devoluciones},
                        new int[]{G_CLARO, PdfDocumento.ROJO, G_OSCURO}, 230),
                () -> {
                    int n = Math.min(6, r.filtrosPorCriticidad.size());
                    List<String> cats = new ArrayList<>();
                    double[][] v = new double[2][n];
                    int[] col = new int[n];
                    for (int i = 0; i < n; i++) {
                        Filtros f = r.filtrosPorCriticidad.get(i);
                        cats.add(f.getIdFiltro());
                        v[0][i] = f.getStockActual();
                        v[1][i] = f.getPuntoReorden();
                        col[i] = ReporteKardex.colorPleno(KardexService.semaforo(f.getStockActual(), f.getPuntoReorden()));
                    }
                    pdf.barrasH("Stock vs. reorden (más críticos)", cats, new String[]{"Stock", "Reorden"}, v,
                            new int[]{G_OSCURO, G_CLARO}, null);
                });

        pdf.seccion("Insumos que requieren compra");
        List<String[]> compra = new ArrayList<>();
        List<String> sems = new ArrayList<>();
        for (Filtros f : r.filtrosPorCriticidad) {
            String s = KardexService.semaforo(f.getStockActual(), f.getPuntoReorden());
            if (!KardexService.VERDE.equals(s)) {
                sems.add(s);
                compra.add(new String[]{f.getIdFiltro(), f.getDescripcion(), String.valueOf(f.getStockActual()),
                    String.valueOf(f.getPuntoReorden()), String.valueOf(Math.max(0, f.getPuntoReorden() - f.getStockActual())),
                    KardexService.etiquetaSemaforo(s)});
            }
        }
        pdf.tabla(new String[]{"Código", "Descripción", "Stock", "Reorden", "Faltante", "Estado"},
                new double[]{20, 30, 10, 13, 13, 42}, new char[]{'L', 'L', 'R', 'R', 'R', 'L'}, compra,
                new PdfDocumento.EstiloCelda() {
            @Override
            public Integer fondo(int fila, int col) {
                return col == 5 ? ReporteKardex.fondoSemaforo(sems.get(fila)) : null;
            }

            @Override
            public Integer texto(int fila, int col) {
                return col == 5 ? ReporteKardex.colorSemaforo(sems.get(fila)) : null;
            }

            @Override
            public boolean negrita(int fila, int col) {
                return col == 5;
            }
        });
        pdf.guardar(destino);
    }



    private static final String H_DASH = "Dashboard";
    private static final String H_IND = "Indicadores";

    public static void excel(File destino, DashboardResumen r, String usuario, int meses) throws IOException {
        XlsxWriter wb = new XlsxWriter();
        Hoja d = wb.hoja(H_DASH);
        Hoja ind = wb.hoja(H_IND);
        List<Filtros> ex = r.filtrosPorCriticidad;
        String periodo = "Tendencia: " + etiquetaPeriodo(meses);
        String generado = "Generado el " + SALIDA.format(LocalDateTime.now()) + " por " + usuario;


        ind.sinFiltro().anchos(34, 14, 60);
        ind.encabezado(1, "Indicadores del dashboard", periodo, generado);
        ReporteKardex.seccion(ind, "Indicadores generales", 3);
        ind.cabeceraSecundaria(0, "Indicador", "Valor", "Detalle");
        int base = ind.numeroFilaActual(); // fila de "Máquinas"
        int[] st = {XlsxWriter.TXT_NEGRITA, XlsxWriter.NUM, XlsxWriter.TXT};
        ind.fila(st, "Máquinas", r.maquinas, "Activos de tipo Maquinaria visibles en inventario");
        ind.fila(st, "Herramientas", r.herramientas, "Activos de tipo Herramienta (incluye componentes de kit)");
        ind.fila(st, "Kits agrupados", r.kits, "Activos de tipo Kit Agrupado");
        int enUso = Math.max(0, r.prestamosActivos - r.prestamosAtrasados);
        ind.fila(st, "Préstamos en uso (a tiempo)", enUso, "Préstamos con estado En Uso");
        ind.fila(st, "Préstamos atrasados", r.prestamosAtrasados, "Préstamos con estado Atrasado");
        ind.fila(st, "Devoluciones", r.devoluciones, "Préstamos con estado Devuelto");
        int rEnUso = base + 3, rAtras = base + 4, rDev = base + 5;
        ind.fila(st, "Préstamos activos", new Formula("B" + rEnUso + "+B" + rAtras, r.prestamosActivos), "En uso + atrasados");
        ind.fila(st, "Aprobados SST", r.aprobados, "Estado SST Operativa o Completo");
        ind.fila(st, "Bloqueados / mantenimiento SST", r.bloqueadosMantenimiento, "Estado SST Bloqueada o Mantenimiento");
        int rInsumos = ind.numeroFilaActual();
        ind.fila(st, "Insumos (referencias de filtro)",
                new Formula(ex.isEmpty() ? "0" : "COUNTA('" + ReporteKardex.H_EX + "'!$A$6:$A$" + (5 + ex.size()) + ")", ex.size()),
                "Registros de la hoja Existencias");
        ind.fila(st, "Activos prestables", r.prestables, "Máquinas, herramientas sueltas y kits (sin componentes de kit)");
        int rPrestados = ind.numeroFilaActual();
        ind.fila(st, "Prestados", r.prestados, "Activos prestables con préstamo activo");
        int rBodega = ind.numeroFilaActual();
        int rPrestables = rPrestados - 1;
        ind.fila(st, "En bodega", new Formula("B" + rPrestables + "-B" + rPrestados, r.enBodega()), "Prestables disponibles");
        int rOcup = ind.numeroFilaActual();
        ind.fila(new int[]{XlsxWriter.TXT_NEGRITA, XlsxWriter.PORCENTAJE, XlsxWriter.TXT}, "Ocupación",
                new Formula("IF(B" + rPrestables + "=0,0,B" + rPrestados + "/B" + rPrestables + ")", r.prestables == 0 ? 0 : (double) r.prestados / r.prestables),
                "Prestados / prestables");
        ind.vacia();

        ReporteKardex.seccion(ind, "Actividad mensual (" + etiquetaPeriodo(meses) + ")", 5);
        ind.cabeceraSecundaria(0, "Mes", "Entradas", "Salidas", "Neto", "Préstamos");
        int iniM = ind.numeroFilaActual();
        int te = 0, ts = 0, tp = 0;
        for (int i = 0; i < r.meses.size(); i++) {
            int fila = ind.numeroFilaActual();
            ind.fila(new int[]{XlsxWriter.TXT, XlsxWriter.NUM_VERDE, XlsxWriter.NUM_ROJO, XlsxWriter.NUM_NEGRITA, XlsxWriter.NUM},
                    etiquetaMes(r.meses.get(i)), r.entradasMes.get(i), r.salidasMes.get(i),
                    new Formula("B" + fila + "-C" + fila, r.entradasMes.get(i) - r.salidasMes.get(i)), r.prestamosMes.get(i));
            te += r.entradasMes.get(i);
            ts += r.salidasMes.get(i);
            tp += r.prestamosMes.get(i);
        }
        int finM = ind.numeroFilaActual() - 1;
        ind.fila(new int[]{XlsxWriter.ETIQUETA, XlsxWriter.ETIQUETA_NUM, XlsxWriter.ETIQUETA_NUM, XlsxWriter.ETIQUETA_NUM, XlsxWriter.ETIQUETA_NUM},
                "TOTAL",
                new Formula("SUM(B" + iniM + ":B" + finM + ")", te), new Formula("SUM(C" + iniM + ":C" + finM + ")", ts),
                new Formula("SUM(D" + iniM + ":D" + finM + ")", te - ts), new Formula("SUM(E" + iniM + ":E" + finM + ")", tp));
        ind.condicional("A" + iniM + ":E" + finM, "MOD(ROW(),2)=0", XlsxWriter.DXF_ZEBRA);
        ind.alturaFila(1, 21);

        ReporteKardex.Rango rEx = ReporteKardex.hojaExistencias(wb, ex, "Existencias y reposición de insumos",
                "Stock actual de todos los insumos frente a su punto de reorden", usuario);
        String sel = ex.isEmpty() ? "" : ex.get(0).getIdFiltro();
        ReporteKardex.Piv pv = ReporteKardex.hojaPivotes(wb, null, ex, usuario, sel, periodo);

        String E = ReporteKardex.q(ReporteKardex.H_EX);
        String I = ReporteKardex.q(H_IND);
        d.sinFiltro().anchos(21, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14);
        d.encabezado(1, "Dashboard gerencial", periodo, generado);
        ReporteKardex.seccion(d, "Indicadores generales", 12);

        int[] semaforoCount = ReporteKardex.contarSemaforo(ex);
        Object[][][] tarjetas = {
            {{"Máquinas", "B" + base, r.maquinas}, {"Herramientas", "B" + (base + 1), r.herramientas}, {"Kits", "B" + (base + 2), r.kits},
                {"Préstamos activos", "B" + (base + 6), r.prestamosActivos}},
            {{"Ocupación", "B" + rOcup, r.prestables == 0 ? 0.0 : (double) r.prestados / r.prestables}, {"Devoluciones", "B" + rDev, r.devoluciones},
                {"Aprobados SST", "B" + (base + 7), r.aprobados}, {"Bloqueados / mant. SST", "B" + (base + 8), r.bloqueadosMantenimiento}},
            {{"Insumos (referencias)", "B" + rInsumos, r.filtros}, {"Stock en rojo", "@R", semaforoCount[0]},
                {"Stock en amarillo", "@A", semaforoCount[1]}, {"Stock en verde", "@V", semaforoCount[2]}}};
        int[] filaValor = new int[3];
        for (int g = 0; g < 3; g++) {
            int fl = d.numeroFilaActual();
            int[] estL = new int[12];
            java.util.Arrays.fill(estL, XlsxWriter.KPI_ETIQUETA);
            Object[] celdasL = new Object[12];
            for (int k = 0; k < 4; k++) {
                celdasL[k * 3] = tarjetas[g][k][0];
            }
            d.fila(estL, celdasL);
            d.alturaFila(fl, 20);
            int fv = d.numeroFilaActual();
            filaValor[g] = fv;
            int[] estV = new int[12];
            Object[] celdasV = new Object[12];
            for (int k = 0; k < 4; k++) {
                String ref = (String) tarjetas[g][k][1];
                double val = ((Number) tarjetas[g][k][2]).doubleValue();
                String formula;
                if (ref.startsWith("@")) {
                    String estado = ref.equals("@R") ? "ROJO - Reponer" : ref.equals("@A") ? "AMARILLO - Pedir pronto" : "VERDE - OK";
                    formula = "COUNTIFS(" + E + "$H:$H,\"" + estado + "\")";
                } else {
                    formula = I + "$" + ref.substring(0, 1) + "$" + ref.substring(1);
                }
                int s = (g == 1 && k == 0) ? XlsxWriter.KPI_PORC : XlsxWriter.KPI_VALOR;
                java.util.Arrays.fill(estV, k * 3, k * 3 + 3, s);
                celdasV[k * 3] = new Formula(formula, val);
            }
            d.fila(estV, celdasV);
            d.alturaFila(fv, 26);
            for (int k = 0; k < 4; k++) {
                d.combinar(XlsxWriter.columna(k * 3) + fl + ":" + XlsxWriter.columna(k * 3 + 2) + fl);
                d.combinar(XlsxWriter.columna(k * 3) + fv + ":" + XlsxWriter.columna(k * 3 + 2) + fv);
            }
            d.vacia();
        }
        d.condicional("J" + filaValor[1] + ":L" + filaValor[1], "$J$" + filaValor[1] + ">0", XlsxWriter.DXF_ROJO);
        d.condicional("D" + filaValor[2] + ":F" + filaValor[2], "$D$" + filaValor[2] + ">0", XlsxWriter.DXF_ROJO);
        d.condicional("G" + filaValor[2] + ":I" + filaValor[2], "$G$" + filaValor[2] + ">0", XlsxWriter.DXF_AMARILLO);
        d.condicional("J" + filaValor[2] + ":L" + filaValor[2], "$J$" + filaValor[2] + ">0", XlsxWriter.DXF_VERDE);

        ReporteKardex.seccion(d, "Explorar un insumo (elija en la celda sombreada)", 12);
        int selRow = d.numeroFilaActual();
        Filtros f0 = ex.isEmpty() ? null : ex.get(0);
        int stock = f0 == null ? 0 : f0.getStockActual();
        int reorden = f0 == null ? 0 : f0.getPuntoReorden();
        String idx = "MATCH($B$" + selRow + "," + E + "$A:$A,0)";
        d.fila(new int[]{XlsxWriter.ETIQUETA, XlsxWriter.SELECTOR, XlsxWriter.SELECTOR}, "Insumo (elija)", sel, null);
        d.combinar("B" + selRow + ":C" + selRow);
        if (!ex.isEmpty()) {
            d.lista("B" + selRow, E + "$A$" + rEx.ini() + ":$A$" + rEx.fin());
        }
        int rStock = selRow + 3, rReorden = selRow + 4;
        String[] et = {"Descripción", "Categoría", "Stock actual", "Punto de reorden", "Faltante", "Estado"};
        Object[] val = {
            new FormulaTxt("IFERROR(INDEX(" + E + "$B:$B," + idx + "),\"\")", f0 == null ? "" : f0.getDescripcion()),
            new FormulaTxt("IFERROR(INDEX(" + E + "$C:$C," + idx + "),\"\")", f0 == null ? "" : ReporteKardex.nz(f0.getCategoriaFiltro())),
            new Formula("IFERROR(INDEX(" + E + "$E:$E," + idx + "),0)", stock),
            new Formula("IFERROR(INDEX(" + E + "$F:$F," + idx + "),0)", reorden),
            new Formula("MAX(0,B" + rReorden + "-B" + rStock + ")", Math.max(0, reorden - stock)),
            new FormulaTxt(ReporteKardex.formulaEstado("B" + rStock, "B" + rReorden),
                    KardexService.etiquetaSemaforo(KardexService.semaforo(stock, reorden)))};
        int[] est = {XlsxWriter.CENTRO_NEGRITA, XlsxWriter.CENTRO_NEGRITA, XlsxWriter.KPI_VALOR, XlsxWriter.KPI_VALOR, XlsxWriter.KPI_VALOR,
            XlsxWriter.ESTADO};
        for (int i = 0; i < et.length; i++) {
            d.fila(new int[]{XlsxWriter.ETIQUETA, est[i], est[i]}, et[i], val[i], null);
            d.combinar("B" + (d.numeroFilaActual() - 1) + ":C" + (d.numeroFilaActual() - 1));
        }
        int rEstado = selRow + 6;
        ReporteKardex.semaforoDinamico(d, "B" + rEstado + ":C" + rEstado, "$B$" + rStock, "$B$" + rReorden);
        while (d.numeroFilaActual() < selRow + 10) {
            d.vacia();
        }
        d.grafico(Grafico.columnas("Stock frente al punto de reorden del insumo elegido")
                .categorias(d.ref(0, rStock, rReorden)).serie("Unidades", d.ref(1, rStock, rReorden), new int[]{G_OSCURO, G_CLARO}).sinLeyenda(),
                4, selRow, 12, selRow + 10);
        d.vacia();


        ReporteKardex.seccion(d, "Semáforo por categoría (elija una categoría o \"(Todas)\")", 12);
        TreeSet<String> cats = new TreeSet<>();
        Map<String, int[]> porCat = new LinkedHashMap<>();
        for (Filtros f : ex) {
            String c = ReporteKardex.nz(f.getCategoriaFiltro());
            cats.add(c);
            int[] a = porCat.computeIfAbsent(c, x -> new int[3]);
            switch (KardexService.semaforo(f.getStockActual(), f.getPuntoReorden())) {
                case KardexService.ROJO -> a[0]++;
                case KardexService.AMARILLO -> a[1]++;
                default -> a[2]++;
            }
        }
        int cRow = d.numeroFilaActual();
        d.fila(new int[]{XlsxWriter.ETIQUETA, XlsxWriter.SELECTOR, XlsxWriter.SELECTOR}, "Categoría (elija)", "(Todas)", null);
        d.combinar("B" + cRow + ":C" + cRow);
        StringBuilder lit = new StringBuilder("(Todas)");
        for (String c : cats) {
            lit.append(',').append(c.replace("\"", ""));
        }
        if (lit.length() <= 250) {
            d.lista("B" + cRow, "\"" + lit + "\"");
        } else {
            d.lista("B" + cRow, ReporteKardex.q(ReporteKardex.H_PIV) + "$A$" + pv.catCat.fila1() + ":$A$" + pv.catCat.fila2());
        }
        String[][] filasSem = {{"Rojo", "ROJO - Reponer"}, {"Amarillo", "AMARILLO - Pedir pronto"}, {"Verde", "VERDE - OK"}};
        for (int i = 0; i < 3; i++) {
            String estadoTxt = filasSem[i][1];
            String formula = "IF($B$" + cRow + "=\"(Todas)\",COUNTIFS(" + E + "$H:$H,\"" + estadoTxt + "\"),COUNTIFS(" + E + "$C:$C,$B$" + cRow + ","
                    + E + "$H:$H,\"" + estadoTxt + "\"))";
            d.fila(new int[]{XlsxWriter.ETIQUETA, XlsxWriter.KPI_VALOR, XlsxWriter.KPI_VALOR}, filasSem[i][0],
                    new Formula(formula, semaforoCount[i]), null);
            d.combinar("B" + (d.numeroFilaActual() - 1) + ":C" + (d.numeroFilaActual() - 1));
        }
        int rSemIni = cRow + 1;
        d.condicional("B" + rSemIni + ":C" + rSemIni, "$B$" + rSemIni + ">0", XlsxWriter.DXF_ROJO);
        d.condicional("B" + (rSemIni + 1) + ":C" + (rSemIni + 1), "$B$" + (rSemIni + 1) + ">0", XlsxWriter.DXF_AMARILLO);
        d.condicional("B" + (rSemIni + 2) + ":C" + (rSemIni + 2), "$B$" + (rSemIni + 2) + ">0", XlsxWriter.DXF_VERDE);
        d.fila(new int[]{XlsxWriter.ETIQUETA, XlsxWriter.ETIQUETA_NUM, XlsxWriter.ETIQUETA_NUM}, "Total",
                new Formula("SUM(B" + rSemIni + ":B" + (rSemIni + 2) + ")", ex.size()), null);
        d.combinar("B" + (d.numeroFilaActual() - 1) + ":C" + (d.numeroFilaActual() - 1));
        while (d.numeroFilaActual() < cRow + 9) {
            d.vacia();
        }
        d.grafico(Grafico.torta("Semáforo de la selección")
                .categorias(d.ref(0, rSemIni, rSemIni + 2))
                .serie("Insumos", d.ref(1, rSemIni, rSemIni + 2), new int[]{PdfDocumento.ROJO, PdfDocumento.AMARILLO, PdfDocumento.VERDE}),
                4, cRow, 12, cRow + 9);
        d.vacia();


        ReporteKardex.seccion(d, "Panel general", 12);
        int p0 = d.numeroFilaActual();
        int alto = 19;
        d.reservar(alto);
        d.vacia();
        int p1 = d.numeroFilaActual();
        d.reservar(alto);
        d.vacia();
        int p2 = d.numeroFilaActual();
        d.reservar(alto);
        d.saltoPagina(p0 - 1);
        d.saltoPagina(p2);
        d.grafico(Grafico.torta("Bodega vs. prestado").categorias(ind.ref(0, rPrestados, rBodega))
                .serie("Activos", ind.ref(1, rPrestados, rBodega), new int[]{G_OSCURO, G_CLARO}), 0, p0, 6, p0 + alto);
        d.grafico(Grafico.torta("Semáforo de existencias").categorias(pv.semCat)
                .serie("Insumos", pv.semVal, new int[]{PdfDocumento.ROJO, PdfDocumento.AMARILLO, PdfDocumento.VERDE}), 6, p0, 12, p0 + alto);
        d.grafico(Grafico.columnas("Entradas y salidas de insumos por mes (uds)").categorias(ind.ref(0, iniM, finM))
                .serie("Entradas", ind.ref(1, iniM, finM), G_OSCURO).serie("Salidas", ind.ref(2, iniM, finM), G_CLARO), 0, p1, 6, p1 + alto);
        d.grafico(Grafico.lineas("Tendencia mensual").categorias(ind.ref(0, iniM, finM))
                .serie("Entradas", ind.ref(1, iniM, finM), 0x000000).serie("Salidas", ind.ref(2, iniM, finM), G_MEDIO)
                .serie("Préstamos", ind.ref(4, iniM, finM), 0x595959).sinEtiquetas(), 6, p1, 12, p1 + alto);
        d.grafico(Grafico.barras("Stock vs. punto de reorden (más críticos)").categorias(pv.critCat)
                .serie("Stock", pv.critStock, G_OSCURO).serie("Reorden", pv.critReorden, G_CLARO), 0, p2, 6, p2 + alto);
        d.grafico(Grafico.torta("Préstamos por estado").categorias(ind.ref(0, rEnUso, rDev))
                .serie("Préstamos", ind.ref(1, rEnUso, rDev), new int[]{G_CLARO, PdfDocumento.ROJO, G_OSCURO}), 6, p2, 12, p2 + alto);
        wb.guardar(destino);
    }
}
