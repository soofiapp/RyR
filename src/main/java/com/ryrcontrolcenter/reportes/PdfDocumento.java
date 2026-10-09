package com.ryrcontrolcenter.reportes;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.zip.DeflaterOutputStream;
import javax.imageio.ImageIO;

public class PdfDocumento {

    public static final int NEGRO = 0x000000;
    public static final int GRIS_OSCURO = 0x404040;
    public static final int GRIS = 0x808080;
    public static final int GRIS_MEDIO = 0xA6A6A6;
    public static final int GRIS_CLARO = 0xD9D9D9;
    public static final int GRIS_ZEBRA = 0xF2F2F2;
    public static final int BLANCO = 0xFFFFFF;
    public static final int ROJO = 0xC00000;
    public static final int AMARILLO = 0xFFC000;
    public static final int VERDE = 0x00A650;
    public static final int FONDO_ROJO = 0xFFC7CE;
    public static final int FONDO_AMARILLO = 0xFFEB9C;
    public static final int FONDO_VERDE = 0xC6EFCE;
    public static final int TEXTO_ROJO = 0x9C0006;
    public static final int TEXTO_AMARILLO = 0x7F6000;
    public static final int TEXTO_VERDE = 0x006100;

    public static final double TAM = 12;
    private static final double LH = 14.5; 

    public interface EstiloCelda {
        default Integer fondo(int fila, int col) { return null; }
        default Integer texto(int fila, int col) { return null; }
        default boolean negrita(int fila, int col) { return false; }
    }

    private static final int[] W_REG = {
        250,333,408,500,500,833,778,180,333,333,500,564,250,333,250,278,500,500,500,500,500,500,500,500,500,500,278,
        278,564,564,564,444,921,722,667,667,722,611,556,722,722,333,389,722,611,889,722,722,556,722,667,556,611,722,
        722,944,722,722,611,333,278,333,469,500,333,444,500,444,500,444,333,500,500,278,278,500,278,778,500,500,500,
        500,333,389,278,500,500,722,500,500,444,480,200,480,541,761,500,0,333,500,444,1000,500,500,333,1000,556,333,
        889,0,611,0,0,333,333,444,444,350,500,1000,333,980,389,333,722,0,444,722,250,333,500,500,500,500,200,500,333,
        760,276,500,564,333,760,333,400,564,300,300,333,500,453,250,333,300,310,500,750,750,750,444,722,722,722,722,
        722,722,889,667,611,611,611,611,333,333,333,333,722,722,722,722,722,722,722,564,722,722,722,722,722,722,556,
        500,444,444,444,444,444,444,667,444,444,444,444,444,278,278,278,278,500,500,500,500,500,500,500,564,500,500,
        500,500,500,500,500,500
    };
    private static final int[] W_BOLD = {
        250,333,555,500,500,1000,833,278,333,333,500,570,250,333,250,278,500,500,500,500,500,500,500,500,500,500,333,
        333,570,570,570,500,930,722,667,722,722,667,611,778,778,389,500,778,667,944,722,778,611,778,722,556,667,722,
        722,1000,722,722,667,333,278,333,581,500,333,500,556,444,556,444,333,500,556,278,333,556,278,833,556,500,556,
        556,444,389,333,556,500,722,500,500,444,394,220,394,520,761,500,0,333,500,500,1000,500,500,333,1000,556,333,
        1000,0,667,0,0,333,333,500,500,350,500,1000,333,1000,389,333,722,0,444,722,250,333,500,500,500,500,220,500,
        333,747,300,500,570,333,747,333,400,570,300,300,333,556,540,250,333,300,330,500,750,750,750,500,722,722,722,
        722,722,722,1000,722,667,667,667,667,389,389,389,389,722,722,778,778,778,778,778,570,778,722,722,722,722,722,
        611,556,500,500,500,500,500,500,722,444,444,444,444,444,278,278,278,278,500,556,500,500,500,500,500,570,500,
        556,556,556,556,500,556,500
    };

    private static final Charset CP1252 = Charset.forName("windows-1252");
    private static final java.nio.charset.CharsetEncoder ENCODER = CP1252.newEncoder();
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.forLanguageTag("es-CO"));
    private static final String RECURSO_LOGO = "/com/ryrcontrolcenter/images/ryr_logo.png";

    private final double ancho;
    private final double alto;
    private final double margen = 36;
    private final String tituloReporte;
    private final String generadoPor;
    private final List<StringBuilder> paginas = new ArrayList<>();
    private StringBuilder cur;
    private double y; 


    private byte[] logoRgb;
    private int logoW, logoH;

    private double regX;
    private double regW;
    private boolean enFila = false;

    public PdfDocumento(String tituloReporte, String generadoPor, boolean horizontal) {
        this.tituloReporte = tituloReporte;
        this.generadoPor = generadoPor == null ? "" : generadoPor;
        this.ancho = horizontal ? 841.89 : 595.28;
        this.alto = horizontal ? 595.28 : 841.89;
        this.regX = margen;
        this.regW = ancho - 2 * margen;
        cargarLogo();
        nuevaPagina();
    }

    private void cargarLogo() {
        try (InputStream in = PdfDocumento.class.getResourceAsStream(RECURSO_LOGO)) {
            if (in == null) {
                return;
            }
            BufferedImage img = ImageIO.read(in);
            if (img == null) {
                return;
            }
            logoW = img.getWidth();
            logoH = img.getHeight();
            logoRgb = new byte[logoW * logoH * 3];
            int k = 0;
            for (int yy = 0; yy < logoH; yy++) {
                for (int xx = 0; xx < logoW; xx++) {
                    int argb = img.getRGB(xx, yy);
                    int a = (argb >>> 24) & 255;
                    int r = (argb >> 16) & 255, g = (argb >> 8) & 255, b = argb & 255;
                    r = (r * a + 255 * (255 - a)) / 255;
                    g = (g * a + 255 * (255 - a)) / 255;
                    b = (b * a + 255 * (255 - a)) / 255;
                    logoRgb[k++] = (byte) r;
                    logoRgb[k++] = (byte) g;
                    logoRgb[k++] = (byte) b;
                }
            }
        } catch (IOException | RuntimeException e) {
            logoRgb = null;
        }
    }


    public double anchoUtil() {
        return ancho - 2 * margen;
    }

    private double limiteInferior() {
        return alto - margen - 26;
    }

    private void nuevaPagina() {
        cur = new StringBuilder();
        paginas.add(cur);
        double hLogo = 44;
        if (logoRgb != null) {
            double wLogo = hLogo * logoW / logoH;
            cur.append("q ").append(n(wLogo)).append(" 0 0 ").append(n(hLogo)).append(' ').append(n(margen)).append(' ')
                    .append(n(alto - margen - hLogo)).append(" cm /Im1 Do Q\n");
            texto("RyR ControlCenter", margen + wLogo + 8, margen + 18, TAM, true, NEGRO);
            texto("Sistema de control de inventario", margen + wLogo + 8, margen + 33, TAM, false, GRIS_OSCURO);
        } else {
            texto("RyR ControlCenter", margen, margen + 14, TAM, true, NEGRO);
        }
        double w = ancho(tituloReporte, TAM, true);
        texto(tituloReporte, ancho - margen - w, margen + 18, TAM, true, NEGRO);
        linea(margen, margen + hLogo + 4, ancho - margen, margen + hLogo + 4, NEGRO, 1.2, null);
        y = margen + hLogo + 18;
    }

    private void asegurarEspacio(double h) {
        if (!enFila && y + h > limiteInferior()) {
            nuevaPagina();
        }
    }

    public void espacio(double pts) {
        y += pts;
    }


    public void titulo(String titulo, String subtitulo) {
        asegurarEspacio(60);
        texto(titulo.toUpperCase(Locale.forLanguageTag("es-CO")), margen, y + TAM, TAM, true, NEGRO);
        y += LH + 4;
        linea(margen, y - 2, margen + anchoUtil(), y - 2, GRIS, 0.6, null);
        y += 4;
        if (subtitulo != null && !subtitulo.isBlank()) {
            for (String l : partir(subtitulo, anchoUtil(), TAM, false)) {
                asegurarEspacio(LH);
                texto(l, margen, y + TAM, TAM, false, GRIS_OSCURO);
                y += LH;
            }
        }
        y += 8;
    }

    public void seccion(String texto) {
        seccion(texto, 150);
    }

    public void seccion(String texto, double reserva) {
        asegurarEspacio(reserva);
        y += 6;
        texto(texto, margen, y + TAM, TAM, true, NEGRO);
        y += LH + 2;
        linea(margen, y, margen + anchoUtil(), y, NEGRO, 0.8, null);
        y += 8;
    }

    public void parrafo(String texto, double size, int color) {
        for (String l : partir(texto, anchoUtil(), TAM, false)) {
            asegurarEspacio(LH);
            texto(l, margen, y + TAM, TAM, false, color);
            y += LH;
        }
        y += 2;
    }

    public void kpis(String[] etiquetas, String[] valores, int[] acentos) {
        int n = etiquetas.length;
        double gap = 8;
        double w = (anchoUtil() - gap * (n - 1)) / n;
        int lineasEt = 1, lineasVal = 1;
        for (int i = 0; i < n; i++) {
            lineasEt = Math.max(lineasEt, partir(etiquetas[i], w - 14, TAM, false).size());
            lineasVal = Math.max(lineasVal, partir(valores[i], w - 14, TAM, true).size());
        }
        double h = (lineasEt + lineasVal) * LH + 10;
        asegurarEspacio(h + 8);
        for (int i = 0; i < n; i++) {
            double x = margen + i * (w + gap);
            rect(x, y, w, h, BLANCO, NEGRO);
            rect(x, y, 4, h, acentos[i], null);
            double ty = y + 5 + TAM;
            for (String l : partir(etiquetas[i], w - 14, TAM, false)) {
                texto(l, x + 10, ty, TAM, false, GRIS_OSCURO);
                ty += LH;
            }
            ty = y + 5 + TAM + lineasEt * LH;
            for (String l : partir(valores[i], w - 14, TAM, true)) {
                texto(l, x + 10, ty, TAM, true, NEGRO);
                ty += LH;
            }
        }
        y += h + 8;
    }

    public void tabla(String[] cabeceras, double[] pesos, char[] alin, List<String[]> filas, EstiloCelda estilo) {
        if (estilo == null) {
            estilo = new EstiloCelda() { };
        }
        double suma = 0;
        for (double p : pesos) {
            suma += p;
        }
        double[] cw = new double[pesos.length];
        for (int i = 0; i < cw.length; i++) {
            cw[i] = pesos[i] / suma * anchoUtil();
        }
        final double pad = 4;
        pintarCabecera(cabeceras, cw, alin, pad);
        if (filas.isEmpty()) {
            asegurarEspacio(26);
            texto("Sin registros para los filtros seleccionados.", margen + pad, y + 17, TAM, false, GRIS_OSCURO);
            y += 26;
            return;
        }
        for (int r = 0; r < filas.size(); r++) {
            String[] fila = filas.get(r);
            List<List<String>> lineas = new ArrayList<>();
            int max = 1;
            for (int c = 0; c < cw.length; c++) {
                String v = c < fila.length && fila[c] != null ? fila[c] : "";
                List<String> ls = partir(v, cw[c] - 2 * pad, TAM, estilo.negrita(r, c));
                lineas.add(ls);
                max = Math.max(max, ls.size());
            }
            double rh = max * LH + 2 * pad - 2;
            if (y + rh > limiteInferior()) {
                nuevaPagina();
                pintarCabecera(cabeceras, cw, alin, pad);
            }
            Integer fondoFila = (r % 2 == 1) ? GRIS_ZEBRA : null;
            double x = margen;
            for (int c = 0; c < cw.length; c++) {
                Integer f = estilo.fondo(r, c);
                if (f != null) {
                    rect(x, y, cw[c], rh, f, null);
                } else if (fondoFila != null) {
                    rect(x, y, cw[c], rh, fondoFila, null);
                }
                Integer tc = estilo.texto(r, c);
                boolean b = estilo.negrita(r, c);
                double ty = y + pad - 1 + TAM;
                for (String l : lineas.get(c)) {
                    double tx = x + pad;
                    char a = c < alin.length ? alin[c] : 'L';
                    double tw = ancho(l, TAM, b);
                    if (a == 'R') {
                        tx = x + cw[c] - pad - tw;
                    } else if (a == 'C') {
                        tx = x + (cw[c] - tw) / 2;
                    }
                    texto(l, tx, ty, TAM, b, tc == null ? NEGRO : tc);
                    ty += LH;
                }
                x += cw[c];
            }
            linea(margen, y + rh, margen + anchoUtil(), y + rh, GRIS, 0.4, null);
            y += rh;
        }
        y += 8;
    }

    private void pintarCabecera(String[] cab, double[] cw, char[] alin, double pad) {
        int max = 1;
        List<List<String>> ls = new ArrayList<>();
        for (int c = 0; c < cw.length; c++) {
            List<String> l = partir(cab[c], cw[c] - 2 * pad, TAM, true);
            ls.add(l);
            max = Math.max(max, l.size());
        }
        double h = max * LH + 2 * pad - 2;
        asegurarEspacio(h + 2 * LH + 10);
        double x = margen;
        for (int c = 0; c < cw.length; c++) {
            rect(x, y, cw[c], h, NEGRO, null);
            double ty = y + pad - 1 + TAM;
            for (String t : ls.get(c)) {
                double tw = ancho(t, TAM, true);
                double tx = x + pad;
                char a = c < alin.length ? alin[c] : 'L';
                if (a == 'R') {
                    tx = x + cw[c] - pad - tw;
                } else if (a == 'C') {
                    tx = x + (cw[c] - tw) / 2;
                }
                texto(t, tx, ty, TAM, true, BLANCO);
                ty += LH;
            }
            x += cw[c];
        }
        y += h;
    }


    public void enColumnas(double altoGrafico, Runnable... graficos) {
        asegurarEspacio(altoGrafico + 6);
        double gap = 16;
        int k = graficos.length;
        double w = (anchoUtil() - gap * (k - 1)) / k;
        double yIni = y;
        enFila = true;
        for (int i = 0; i < k; i++) {
            regX = margen + i * (w + gap);
            regW = w;
            y = yIni;
            graficos[i].run();
        }
        enFila = false;
        regX = margen;
        regW = anchoUtil();
        y = yIni + altoGrafico + 6;
    }

    private static double[] ejeNice(double max, int divisiones) {
        if (max <= 0) {
            return new double[]{1, 1};
        }
        double bruto = max / divisiones;
        double mag = Math.pow(10, Math.floor(Math.log10(bruto)));
        double[] pasos = {1, 2, 2.5, 5, 10};
        double paso = mag;
        for (double p : pasos) {
            paso = p * mag;
            if (paso >= bruto) {
                break;
            }
        }
        double tope = Math.ceil(max / paso) * paso;
        return new double[]{tope, paso};
    }

    private void leyenda(String[] nombres, int[] colores, boolean[] esLinea, double x0, double maxAncho) {
        double lx = x0;
        double ly = y;
        for (int s = 0; s < nombres.length; s++) {
            double w = 18 + ancho(nombres[s], TAM, false) + 14;
            if (lx + w > x0 + maxAncho && lx > x0) {
                lx = x0;
                ly += LH;
            }
            if (esLinea != null && esLinea[s]) {
                linea(lx, ly + 7, lx + 14, ly + 7, colores[s], 2, null);
            } else {
                rect(lx, ly + 3, 10, 10, colores[s], NEGRO);
            }
            texto(nombres[s], lx + 16, ly + 12, TAM, false, NEGRO);
            lx += w;
        }
        y = ly + LH + 2;
    }

    public void barrasV(String titulo, List<String> cats, String[] series, double[][] valores, int[] colores,
            int[][] coloresBarra, double altoTotal) {
        asegurarEspacio(altoTotal);
        double yIni = y;
        texto(titulo, regX, y + TAM, TAM, true, NEGRO);
        y += LH + 2;
        leyenda(series, colores, null, regX, regW);
        double max = 0;
        for (double[] f : valores) {
            for (double v : f) {
                max = Math.max(max, v);
            }
        }
        double[] eje = ejeNice(max, 4);
        double etiqY = 2 * LH;
        double plotTop = y + 4;
        double plotBottom = yIni + altoTotal - etiqY;
        double plotH = Math.max(40, plotBottom - plotTop);
        double izq = 8 + ancho(formatoNumero(eje[0]), TAM, false);
        double plotX = regX + izq;
        double plotW = regW - izq - 4;
        if (cats.isEmpty()) {
            texto("Sin datos para graficar.", regX, plotTop + 20, TAM, false, GRIS_OSCURO);
            y = yIni + altoTotal;
            return;
        }
        // rejilla y eje Y
        for (double v = 0; v <= eje[0] + 1e-9; v += eje[1]) {
            double yy = plotBottom - v / eje[0] * plotH;
            linea(plotX, yy, plotX + plotW, yy, v == 0 ? NEGRO : GRIS_CLARO, v == 0 ? 1 : 0.5, null);
            String t = formatoNumero(v);
            texto(t, plotX - 4 - ancho(t, TAM, false), yy + 4, TAM, false, NEGRO);
        }
        linea(plotX, plotTop, plotX, plotBottom, NEGRO, 1, null);
        int nc = cats.size(), ns = series.length;
        double grupoW = plotW / nc;
        double barW = Math.min(34, grupoW * 0.78 / ns);
        for (int c = 0; c < nc; c++) {
            double gx = plotX + c * grupoW + (grupoW - barW * ns) / 2;
            for (int s = 0; s < ns; s++) {
                double v = valores[s][c];
                double h = v / eje[0] * plotH;
                int col = (coloresBarra != null && coloresBarra[s] != null) ? coloresBarra[s][c] : colores[s];
                if (h > 0) {
                    rect(gx + s * barW, plotBottom - h, barW, h, col, NEGRO);
                }
                if (v > 0) {
                    String t = formatoNumero(v);
                    double tw = ancho(t, TAM, false);
                    if (tw <= barW + 10) {
                        texto(t, gx + s * barW + (barW - tw) / 2, plotBottom - h - 3, TAM, false, NEGRO);
                    }
                }
            }
            List<String> ls = partir(cats.get(c), grupoW - 2, TAM, false);
            double ty = plotBottom + 3 + TAM;
            for (int i = 0; i < Math.min(2, ls.size()); i++) {
                String t = ls.get(i);
                texto(t, plotX + c * grupoW + (grupoW - ancho(t, TAM, false)) / 2, ty, TAM, false, NEGRO);
                ty += LH;
            }
        }
        y = yIni + altoTotal;
    }

    public void barrasH(String titulo, List<String> cats, String[] series, double[][] valores, int[] colores,
            int[][] coloresBarra) {
        int ns = series.length;
        double barH = 11, gapBar = 2, gapGrupo = 8;
        double grupoH = ns * (barH + gapBar) + gapGrupo;
        double total = 2 * LH + 6 + cats.size() * grupoH + 8;
        asegurarEspacio(Math.min(total, 220));
        texto(titulo, regX, y + TAM, TAM, true, NEGRO);
        y += LH + 2;
        leyenda(series, colores, null, regX, regW);
        if (cats.isEmpty()) {
            texto("Sin datos para graficar.", regX, y + TAM, TAM, false, GRIS_OSCURO);
            y += LH + 4;
            return;
        }
        double max = 0;
        for (double[] f : valores) {
            for (double v : f) {
                max = Math.max(max, v);
            }
        }
        double[] eje = ejeNice(max, 4);
        double etiqW = 0;
        for (String c : cats) {
            etiqW = Math.max(etiqW, ancho(c, TAM, false));
        }
        etiqW = Math.min(etiqW + 8, regW * 0.4);
        double areaW = regW - etiqW - 40;
        double x0 = regX + etiqW;
        for (int c = 0; c < cats.size(); c++) {
            if (!enFila && y + grupoH > limiteInferior()) {
                nuevaPagina();
            }
            texto(recortar(cats.get(c), etiqW - 6, TAM, false), regX, y + grupoH / 2 + 4, TAM, false, NEGRO);
            double by = y;
            for (int s = 0; s < ns; s++) {
                double v = valores[s][c];
                double w = Math.max(v > 0 ? 1.5 : 0, v / eje[0] * areaW);
                int col = (coloresBarra != null && coloresBarra[s] != null) ? coloresBarra[s][c] : colores[s];
                rect(x0, by, w, barH, col, NEGRO);
                if (v > 0) {
                    texto(formatoNumero(v), x0 + w + 4, by + barH - 1, TAM, false, NEGRO);
                }
                by += barH + gapBar;
            }
            y += grupoH;
        }
        linea(x0, y - gapGrupo - cats.size() * grupoH + gapGrupo - 2, x0, y - gapGrupo + 2, NEGRO, 1, null);
        y += 6;
    }
    public void lineas(String titulo, List<String> cats, String[] series, double[][] valores, int[] colores, double altoTotal) {
        asegurarEspacio(altoTotal);
        double yIni = y;
        texto(titulo, regX, y + TAM, TAM, true, NEGRO);
        y += LH + 2;
        leyenda(series, colores, new boolean[]{true, true, true, true, true, true}, regX, regW);
        double max = 0;
        for (double[] f : valores) {
            for (double v : f) {
                max = Math.max(max, v);
            }
        }
        double[] eje = ejeNice(max, 4);
        double etiqY = LH + 12;
        double plotTop = y + 8;
        double plotBottom = yIni + altoTotal - etiqY;
        double plotH = Math.max(40, plotBottom - plotTop);
        double izq = 8 + ancho(formatoNumero(eje[0]), TAM, false);
        double plotX = regX + izq;
        double plotW = regW - izq - 10;
        if (cats.isEmpty()) {
            texto("Sin datos para graficar.", regX, plotTop + 20, TAM, false, GRIS_OSCURO);
            y = yIni + altoTotal;
            return;
        }
        for (double v = 0; v <= eje[0] + 1e-9; v += eje[1]) {
            double yy = plotBottom - v / eje[0] * plotH;
            linea(plotX, yy, plotX + plotW, yy, v == 0 ? NEGRO : GRIS_CLARO, v == 0 ? 1 : 0.5, null);
            String t = formatoNumero(v);
            texto(t, plotX - 4 - ancho(t, TAM, false), yy + 4, TAM, false, NEGRO);
        }
        linea(plotX, plotTop, plotX, plotBottom, NEGRO, 1, null);
        int nc = cats.size();
        double paso = nc == 1 ? 0 : (plotW - 20) / (nc - 1);
        double x0 = plotX + (nc == 1 ? plotW / 2 : 10);
        String[] dashes = {null, "[5 3] 0 d", "[1.5 2.5] 0 d", "[8 3 2 3] 0 d", null, "[5 3] 0 d"};
        for (int c = 0; c < nc; c++) {
            String t = cats.get(c);
            double tw = ancho(t, TAM, false);
            texto(t, x0 + c * paso - tw / 2, plotBottom + 9 + TAM, TAM, false, NEGRO);
        }
        for (int s = 0; s < series.length; s++) {
            double px = 0, py = 0;
            for (int c = 0; c < nc; c++) {
                double cx = x0 + c * paso;
                double cy = plotBottom - valores[s][c] / eje[0] * plotH;
                if (c > 0) {
                    linea(px, py, cx, cy, colores[s], 1.6, dashes[s % dashes.length]);
                }
                px = cx;
                py = cy;
            }
            for (int c = 0; c < nc; c++) {
                marcador(x0 + c * paso, plotBottom - valores[s][c] / eje[0] * plotH, s, colores[s]);
            }
        }
        y = yIni + altoTotal;
    }

    private void marcador(double cx, double cy, int forma, int color) {
        double r = 3.6;
        switch (forma % 3) {
            case 0 -> rect(cx - r, cy - r, 2 * r, 2 * r, color, NEGRO);
            case 1 -> poligono(new double[]{cx, cx + r + 1, cx, cx - r - 1}, new double[]{cy - r - 1, cy, cy + r + 1, cy}, color, NEGRO);
            default -> poligono(new double[]{cx, cx + r + 1, cx - r - 1}, new double[]{cy - r - 1, cy + r, cy + r}, color, NEGRO);
        }
    }

    public void torta(String titulo, String[] etiquetas, double[] valores, int[] colores, double altoTotal) {
        asegurarEspacio(altoTotal);
        double yIni = y;
        texto(titulo, regX, y + TAM, TAM, true, NEGRO);
        y += LH + 6;
        double suma = 0;
        for (double v : valores) {
            suma += v;
        }
        double r = Math.min((altoTotal - LH - 14) / 2, regW * 0.22);
        double cx = regX + r + 4;
        double cy = y + r;
        if (suma <= 0) {
            texto("Sin datos para graficar.", regX, y + 20, TAM, false, GRIS_OSCURO);
            y = yIni + altoTotal;
            return;
        }
        double ang = 0;
        int validos = 0;
        for (double v : valores) {
            if (v > 0) {
                validos++;
            }
        }
        for (int i = 0; i < valores.length; i++) {
            if (valores[i] <= 0) {
                continue;
            }
            double fin = ang + valores[i] / suma * 2 * Math.PI;
            List<double[]> pts = new ArrayList<>();
            pts.add(new double[]{cx, cy});
            int pasos = Math.max(2, (int) Math.ceil((fin - ang) / (Math.PI / 60)));
            for (int k = 0; k <= pasos; k++) {
                double t = ang + (fin - ang) * k / pasos;
                pts.add(new double[]{cx + r * Math.sin(t), cy - r * Math.cos(t)});
            }
            double[] xs = new double[pts.size()], ys = new double[pts.size()];
            for (int k = 0; k < xs.length; k++) {
                xs[k] = pts.get(k)[0];
                ys[k] = pts.get(k)[1];
            }
            poligono(xs, ys, colores[i], validos > 1 ? NEGRO : NEGRO);
            ang = fin;
        }
        double lx = cx + r + 24;
        double ly = y + 4;
        for (int i = 0; i < etiquetas.length; i++) {
            String t = etiquetas[i] + ": " + formatoNumero(valores[i]) + " (" + Math.round(valores[i] * 100 / suma) + "%)";
            List<String> partes = partir(t, regX + regW - lx - 16, TAM, false);
            rect(lx, ly + 2, 10, 10, colores[i], NEGRO);
            for (String l : partes) {
                texto(l, lx + 16, ly + 11, TAM, false, NEGRO);
                ly += LH;
            }
            ly += 2;
        }
        y = yIni + altoTotal;
    }

    public void barraSegmentada(String titulo, String[] etiquetas, double[] valores, int[] colores) {
        asegurarEspacio(80);
        texto(titulo, regX, y + TAM, TAM, true, NEGRO);
        y += LH + 4;
        double suma = 0;
        for (double v : valores) {
            suma += v;
        }
        double x = regX;
        if (suma <= 0) {
            rect(regX, y, regW, 16, GRIS_ZEBRA, NEGRO);
        } else {
            for (int i = 0; i < valores.length; i++) {
                double w = valores[i] / suma * regW;
                if (w > 0) {
                    rect(x, y, w, 16, colores[i], NEGRO);
                }
                x += w;
            }
        }
        y += 22;
        String[] nombres = new String[etiquetas.length];
        for (int i = 0; i < etiquetas.length; i++) {
            nombres[i] = etiquetas[i] + ": " + formatoNumero(valores[i])
                    + (suma > 0 ? " (" + Math.round(valores[i] * 100 / suma) + "%)" : "");
        }
        leyenda(nombres, colores, null, regX, regW);
        y += 4;
    }

    public void guardar(File destino) throws IOException {
        int total = paginas.size();
        String pie = "Generado el " + FECHA_HORA.format(LocalDateTime.now())
                + (generadoPor.isBlank() ? "" : " por " + generadoPor);
        for (int i = 0; i < total; i++) {
            StringBuilder anterior = cur;
            cur = paginas.get(i);
            linea(margen, alto - margen - 16, ancho - margen, alto - margen - 16, NEGRO, 0.6, null);
            texto(pie, margen, alto - margen - 2, TAM, false, GRIS_OSCURO);
            String num = "Página " + (i + 1) + " de " + total;
            texto(num, ancho - margen - ancho(num, TAM, false), alto - margen - 2, TAM, false, GRIS_OSCURO);
            cur = anterior;
        }

        List<byte[]> objetos = new ArrayList<>();
        boolean conLogo = logoRgb != null;
        int primerPagina = conLogo ? 6 : 5;
        StringBuilder kids = new StringBuilder();
        for (int i = 0; i < total; i++) {
            kids.append(primerPagina + i * 2).append(" 0 R ");
        }
        objetos.add(bytes("<< /Type /Catalog /Pages 2 0 R >>"));
        objetos.add(bytes("<< /Type /Pages /Kids [" + kids + "] /Count " + total + " >>"));
        objetos.add(bytes("<< /Type /Font /Subtype /Type1 /BaseFont /Times-Roman /Encoding /WinAnsiEncoding >>"));
        objetos.add(bytes("<< /Type /Font /Subtype /Type1 /BaseFont /Times-Bold /Encoding /WinAnsiEncoding >>"));
        if (conLogo) {
            ByteArrayOutputStream comp = new ByteArrayOutputStream();
            try (DeflaterOutputStream d = new DeflaterOutputStream(comp)) {
                d.write(logoRgb);
            }
            ByteArrayOutputStream obj = new ByteArrayOutputStream();
            obj.write(bytes("<< /Type /XObject /Subtype /Image /Width " + logoW + " /Height " + logoH
                    + " /ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /FlateDecode /Length " + comp.size() + " >>\nstream\n"));
            obj.write(comp.toByteArray());
            obj.write(bytes("\nendstream"));
            objetos.add(obj.toByteArray());
        }
        String recursos = "/Resources << /Font << /F1 3 0 R /F2 4 0 R >>" + (conLogo ? " /XObject << /Im1 5 0 R >>" : "") + " >>";
        for (int i = 0; i < total; i++) {
            int contenido = primerPagina + i * 2 + 1;
            objetos.add(bytes("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 " + n(ancho) + " " + n(alto) + "] "
                    + recursos + " /Contents " + contenido + " 0 R >>"));
            byte[] datos = paginas.get(i).toString().getBytes(StandardCharsets.ISO_8859_1);
            ByteArrayOutputStream comp = new ByteArrayOutputStream();
            try (DeflaterOutputStream d = new DeflaterOutputStream(comp)) {
                d.write(datos);
            }
            ByteArrayOutputStream obj = new ByteArrayOutputStream();
            obj.write(bytes("<< /Length " + comp.size() + " /Filter /FlateDecode >>\nstream\n"));
            obj.write(comp.toByteArray());
            obj.write(bytes("\nendstream"));
            objetos.add(obj.toByteArray());
        }

        try (OutputStream out = Files.newOutputStream(destino.toPath())) {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            buf.write(bytes("%PDF-1.4\n%âãÏÓ\n"));
            int[] offsets = new int[objetos.size()];
            for (int i = 0; i < objetos.size(); i++) {
                offsets[i] = buf.size();
                buf.write(bytes((i + 1) + " 0 obj\n"));
                buf.write(objetos.get(i));
                buf.write(bytes("\nendobj\n"));
            }
            int xref = buf.size();
            buf.write(bytes("xref\n0 " + (objetos.size() + 1) + "\n0000000000 65535 f \n"));
            for (int off : offsets) {
                buf.write(bytes(String.format("%010d 00000 n \n", off)));
            }
            buf.write(bytes("trailer\n<< /Size " + (objetos.size() + 1) + " /Root 1 0 R >>\nstartxref\n" + xref + "\n%%EOF\n"));
            out.write(buf.toByteArray());
        }
    }

    private static byte[] bytes(String s) {
        return s.getBytes(StandardCharsets.ISO_8859_1);
    }

    private static String n(double v) {
        return String.format(Locale.US, "%.2f", v);
    }

    private static String rgb(int c) {
        return String.format(Locale.US, "%.3f %.3f %.3f", ((c >> 16) & 255) / 255.0, ((c >> 8) & 255) / 255.0, (c & 255) / 255.0);
    }

    private void rect(double x, double yTop, double w, double h, Integer relleno, Integer borde) {
        double py = alto - yTop - h;
        if (relleno != null) {
            cur.append(rgb(relleno)).append(" rg ").append(n(x)).append(' ').append(n(py)).append(' ')
                    .append(n(w)).append(' ').append(n(h)).append(" re f\n");
        }
        if (borde != null) {
            cur.append(rgb(borde)).append(" RG 0.6 w ").append(n(x)).append(' ').append(n(py)).append(' ')
                    .append(n(w)).append(' ').append(n(h)).append(" re S\n");
        }
    }

    private void poligono(double[] xs, double[] ysTop, Integer relleno, Integer borde) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < xs.length; i++) {
            sb.append(n(xs[i])).append(' ').append(n(alto - ysTop[i])).append(i == 0 ? " m " : " l ");
        }
        sb.append("h ");
        if (relleno != null) {
            cur.append(rgb(relleno)).append(" rg ");
        }
        if (borde != null) {
            cur.append(rgb(borde)).append(" RG 0.6 w ");
        }
        cur.append(sb).append(relleno != null && borde != null ? "B" : relleno != null ? "f" : "S").append('\n');
    }

    private void linea(double x1, double y1, double x2, double y2, int color, double grosor, String dash) {
        cur.append(rgb(color)).append(" RG ").append(n(grosor)).append(" w ");
        if (dash != null) {
            cur.append(dash).append(' ');
        }
        cur.append(n(x1)).append(' ').append(n(alto - y1)).append(" m ")
                .append(n(x2)).append(' ').append(n(alto - y2)).append(" l S");
        if (dash != null) {
            cur.append(" [] 0 d");
        }
        cur.append('\n');
    }

    private void texto(String s, double x, double yBase, double size, boolean negrita, int color) {
        cur.append("BT ").append(rgb(color)).append(" rg /").append(negrita ? "F2" : "F1").append(' ')
                .append(n(size)).append(" Tf ").append(n(x)).append(' ').append(n(alto - yBase)).append(" Td (")
                .append(escapar(s)).append(") Tj ET\n");
    }

    private static String escapar(String s) {
        byte[] b = limpiar(s).getBytes(CP1252);
        StringBuilder sb = new StringBuilder();
        for (byte x : b) {
            int c = x & 255;
            if (c == '(' || c == ')' || c == '\\') {
                sb.append('\\').append((char) c);
            } else if (c < 32 || c > 126) {
                sb.append(String.format("\\%03o", c));
            } else {
                sb.append((char) c);
            }
        }
        return sb.toString();
    }

    private static synchronized boolean representable(char ch) {
        return ENCODER.canEncode(ch);
    }

    private static String limpiar(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '\n' || ch == '\r' || ch == '\t') {
                sb.append(' ');
            } else if (representable(ch)) {
                sb.append(ch);
            } else {
                sb.append('?');
            }
        }
        return sb.toString();
    }

    public static double ancho(String s, double size, boolean negrita) {
        int[] w = negrita ? W_BOLD : W_REG;
        double total = 0;
        for (byte x : limpiar(s).getBytes(CP1252)) {
            int c = x & 255;
            total += (c >= 32 ? w[c - 32] : 0);
        }
        return total * size / 1000.0;
    }

    private static String recortar(String s, double max, double size, boolean negrita) {
        if (s == null) {
            return "";
        }
        if (ancho(s, size, negrita) <= max) {
            return s;
        }
        String t = s;
        while (t.length() > 1 && ancho(t + "...", size, negrita) > max) {
            t = t.substring(0, t.length() - 1);
        }
        return t + "...";
    }

    private static List<String> partir(String s, double max, double size, boolean negrita) {
        List<String> res = new ArrayList<>();
        String texto = limpiar(s).trim();
        if (texto.isEmpty()) {
            res.add("");
            return res;
        }
        StringBuilder linea = new StringBuilder();
        for (String palabra : texto.split("\\s+")) {
            while (ancho(palabra, size, negrita) > max && palabra.length() > 1) {
                int k = palabra.length();
                while (k > 1 && ancho(palabra.substring(0, k), size, negrita) > max) {
                    k--;
                }
                if (linea.length() > 0) {
                    res.add(linea.toString());
                    linea.setLength(0);
                }
                res.add(palabra.substring(0, k));
                palabra = palabra.substring(k);
            }
            String candidata = linea.length() == 0 ? palabra : linea + " " + palabra;
            if (ancho(candidata, size, negrita) <= max) {
                linea.setLength(0);
                linea.append(candidata);
            } else {
                res.add(linea.toString());
                linea.setLength(0);
                linea.append(palabra);
            }
        }
        if (linea.length() > 0) {
            res.add(linea.toString());
        }
        return res;
    }

    public static String formatoNumero(double v) {
        if (v == Math.rint(v)) {
            return String.format(Locale.forLanguageTag("es-CO"), "%,.0f", v);
        }
        return String.format(Locale.forLanguageTag("es-CO"), "%,.1f", v);
    }
}
