package com.ryrcontrolcenter.reportes;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class XlsxWriter {


    public static final int DEFECTO = 0;
    public static final int TITULO = 1;
    public static final int SUBTITULO = 2;
    public static final int CABECERA = 3;
    public static final int TXT = 4;
    public static final int NUM = 5;
    public static final int NUM_DEC = 6;
    public static final int VERDE = 7;
    public static final int AMARILLO = 8;
    public static final int ROJO = 9;
    public static final int ETIQUETA = 10;
    public static final int CENTRO = 11;
    public static final int NUM_VERDE = 12;
    public static final int NUM_ROJO = 13;
    public static final int SECCION = 14;
    public static final int NUM_NEGRITA = 15;
    public static final int PORCENTAJE = 16;
    public static final int FECHA = 17;
    public static final int FECHA_CORTA = 18;
    public static final int TXT_NEGRITA = 19;
    public static final int SELECTOR = 20;
    public static final int KPI_VALOR = 21;
    public static final int KPI_ETIQUETA = 22;
    public static final int NUM_CENTRO = 23;
    public static final int PORC_DEC = 24;
    public static final int CENTRO_NEGRITA = 25;
    public static final int ETIQUETA_NUM = 26;
    public static final int KPI_PORC = 27;
    public static final int NOTA = 28;
    public static final int ESTADO = 29; 

    public static final int DXF_ROJO = 0;
    public static final int DXF_AMARILLO = 1;
    public static final int DXF_VERDE = 2;
    public static final int DXF_ZEBRA = 3;
    public static final int DXF_TEXTO_VERDE = 4;
    public static final int DXF_TEXTO_ROJO = 5;
    public static final int DXF_TEXTO_OCRE = 6;

    private static final String FUENTE = "Times New Roman";
    private static final String RECURSO_LOGO = "/com/ryrcontrolcenter/images/ryr_logo.png";
    private static final double ALTO_FILA = 16.5;

    public record Formula(String expresion, double valor) { }

    public record FormulaTxt(String expresion, String valor) { }

    public record Ref(Hoja hoja, int col, int fila1, int fila2) {
        String formula() {
            String c = columna(col);
            return "'" + hoja.nombre.replace("'", "''") + "'!$" + c + "$" + fila1 + ":$" + c + "$" + fila2;
        }

        int tamano() {
            return fila2 - fila1 + 1;
        }

        Object valor(int i) {
            int fila = fila1 + i - 1;
            if (fila - 1 >= hoja.filas.size()) {
                return null;
            }
            Object[] celdas = (Object[]) hoja.filas.get(fila - 1)[0];
            if (col >= celdas.length) {
                return null;
            }
            Object v = celdas[col];
            if (v instanceof Formula f) {
                return f.valor();
            }
            if (v instanceof FormulaTxt f) {
                return f.valor();
            }
            return v;
        }
    }


    public enum TipoGrafico { COLUMNAS, BARRAS, LINEAS, TORTA }

    public static class Grafico {
        final TipoGrafico tipo;
        String titulo = "";
        Ref categorias;
        final List<Object[]> series = new ArrayList<>(); 
        boolean etiquetas = true;
        boolean apilado = false;
        boolean leyenda = true;
        String tituloEjeY;

        private Grafico(TipoGrafico t) {
            this.tipo = t;
        }

        public static Grafico columnas(String titulo) {
            Grafico g = new Grafico(TipoGrafico.COLUMNAS);
            g.titulo = titulo;
            return g;
        }

        public static Grafico barras(String titulo) {
            Grafico g = new Grafico(TipoGrafico.BARRAS);
            g.titulo = titulo;
            return g;
        }

        public static Grafico lineas(String titulo) {
            Grafico g = new Grafico(TipoGrafico.LINEAS);
            g.titulo = titulo;
            return g;
        }

        public static Grafico torta(String titulo) {
            Grafico g = new Grafico(TipoGrafico.TORTA);
            g.titulo = titulo;
            return g;
        }

        public Grafico categorias(Ref r) {
            this.categorias = r;
            return this;
        }

        public Grafico serie(String nombre, Ref valores, int color) {
            series.add(new Object[]{nombre, valores, color, null});
            return this;
        }

        public Grafico serie(String nombre, Ref valores, int[] coloresPuntos) {
            series.add(new Object[]{nombre, valores, coloresPuntos.length > 0 ? coloresPuntos[0] : 0, coloresPuntos});
            return this;
        }

        public Grafico sinEtiquetas() {
            this.etiquetas = false;
            return this;
        }

        public Grafico sinLeyenda() {
            this.leyenda = false;
            return this;
        }

        public Grafico apilado() {
            this.apilado = true;
            return this;
        }

        public Grafico ejeY(String t) {
            this.tituloEjeY = t;
            return this;
        }
    }


    public static class Hoja {
        private final String nombre;
        private final List<Object[]> filas = new ArrayList<>(); 
        private final List<double[]> alturas = new ArrayList<>(); 
        private double[] anchos = new double[0];
        private int filaCabecera = -1;
        private int columnasCabecera = 0;
        private int ultimaFilaFiltro = -1;
        private boolean conFiltro = true;
        private boolean conLogo = false;
        private int colTitulo = 2;
        private final List<String> combinadas = new ArrayList<>();
        private final List<String[]> condicionales = new ArrayList<>(); 
        private final List<String[]> listas = new ArrayList<>(); 
        private final List<Object[]> graficos = new ArrayList<>();
        private int ultimaColExtra = 0;
        private int ultimaFilaExtra = 0;
        private boolean vertical = false;
        private final List<Integer> saltos = new ArrayList<>();

        private Hoja(String nombre) {
            this.nombre = nombre;
        }

        public String nombre() {
            return nombre;
        }

        public Hoja anchos(double... a) {
            this.anchos = a;
            return this;
        }

       
        public int numeroFilaActual() {
            return filas.size() + 1;
        }

        public Ref ref(int col, int fila1, int fila2) {
            return new Ref(this, col, fila1, fila2);
        }


        public Hoja saltoPagina(int antesDeFila) {
            saltos.add(antesDeFila - 1);
            return this;
        }

        public Hoja vertical() {
            this.vertical = true;
            return this;
        }

        public Hoja titulo(String t) {
            return fila(new int[]{TITULO}, t);
        }

        public Hoja subtitulo(String t) {
            return fila(new int[]{SUBTITULO}, t);
        }

        public Hoja seccion(String t) {
            return fila(new int[]{SECCION}, t);
        }

        public Hoja encabezado(int columnaTexto, String titulo, String linea1, String linea2) {
            conLogo = true;
            colTitulo = columnaTexto;
            fila(estilosDesde(columnaTexto, TITULO), textoDesde(columnaTexto, titulo));
            fila(estilosDesde(columnaTexto, SUBTITULO), textoDesde(columnaTexto, linea1));
            fila(estilosDesde(columnaTexto, SUBTITULO), textoDesde(columnaTexto, linea2));
            alturaFila(1, 21);
            alturaFila(2, 17);
            alturaFila(3, 17);
            return vacia();
        }

        private static Object[] textoDesde(int col, String t) {
            Object[] o = new Object[col + 1];
            o[col] = t == null ? "" : t;
            return o;
        }

        private static int[] estilosDesde(int col, int estilo) {
            int[] e = new int[col + 1];
            e[col] = estilo;
            return e;
        }

        public Hoja sinFiltro() {
            conFiltro = false;
            return this;
        }

        public Hoja finDatos() {
            ultimaFilaFiltro = filas.size() - 1;
            return this;
        }

        public Hoja vacia() {
            filas.add(new Object[]{new Object[0], new int[0]});
            return this;
        }

        public Hoja reservar(int n) {
            for (int i = 0; i < n; i++) {
                vacia();
            }
            return this;
        }

        public Hoja alturaFila(int filaBase1, double pts) {
            alturas.add(new double[]{filaBase1, pts});
            return this;
        }

        public Hoja cabecera(String... cols) {
            filaCabecera = filas.size();
            columnasCabecera = cols.length;
            int[] est = new int[cols.length];
            java.util.Arrays.fill(est, CABECERA);
            filas.add(new Object[]{(Object[]) cols, est});
            alturaFila(filas.size(), 33);
            return this;
        }

        public Hoja cabeceraSecundaria(int desdeCol, String... cols) {
            Object[] celdas = new Object[desdeCol + cols.length];
            int[] est = new int[celdas.length];
            for (int i = 0; i < cols.length; i++) {
                celdas[desdeCol + i] = cols[i];
                est[desdeCol + i] = CABECERA;
            }
            filas.add(new Object[]{celdas, est});
            alturaFila(filas.size(), 33);
            return this;
        }

        public Hoja fila(Object... celdas) {
            int[] est = new int[celdas.length];
            for (int i = 0; i < celdas.length; i++) {
                est[i] = (celdas[i] instanceof Number || celdas[i] instanceof Formula) ? NUM : TXT;
            }
            filas.add(new Object[]{celdas, est});
            return this;
        }

        public Hoja fila(int[] estilos, Object... celdas) {
            filas.add(new Object[]{celdas, estilos});
            return this;
        }

        public Hoja combinar(String rango) {
            combinadas.add(rango);
            return this;
        }

        public Hoja condicional(String rango, String formula, int dxf) {
            condicionales.add(new String[]{rango, formula, String.valueOf(dxf)});
            return this;
        }

        public Hoja lista(String celda, String origen) {
            listas.add(new String[]{celda, origen});
            return this;
        }

        public Hoja grafico(Grafico g, int col1, int fila1, int col2, int fila2) {
            graficos.add(new Object[]{g, col1, fila1, col2, fila2});
            ultimaColExtra = Math.max(ultimaColExtra, col2);
            ultimaFilaExtra = Math.max(ultimaFilaExtra, fila2);
            return this;
        }

        private int columnasUsadas() {
            int max = anchos.length;
            for (Object[] f : filas) {
                max = Math.max(max, ((Object[]) f[0]).length);
            }
            return Math.max(max, ultimaColExtra);
        }

        private int filasUsadas() {
            return Math.max(filas.size(), ultimaFilaExtra);
        }
    }

    private final List<Hoja> hojas = new ArrayList<>();
    private byte[] logo;
    private int logoAncho;
    private int logoAlto;

    public Hoja hoja(String nombre) {
        String n = nombre.replaceAll("[\\[\\]:*?/\\\\]", " ");
        if (n.length() > 31) {
            n = n.substring(0, 31);
        }
        Hoja h = new Hoja(n);
        hojas.add(h);
        return h;
    }

    public void guardar(File destino) throws IOException {
        cargarLogo();
        int[] dibujoDeHoja = new int[hojas.size()];
        int dibujos = 0;
        int graficos = 0;
        List<Grafico> todosGraficos = new ArrayList<>();
        for (int i = 0; i < hojas.size(); i++) {
            Hoja h = hojas.get(i);
            if ((h.conLogo && logo != null) || !h.graficos.isEmpty()) {
                dibujoDeHoja[i] = ++dibujos;
                for (Object[] g : h.graficos) {
                    todosGraficos.add((Grafico) g[0]);
                    graficos++;
                }
            }
        }
        try (OutputStream os = Files.newOutputStream(destino.toPath());
                ZipOutputStream zip = new ZipOutputStream(os, StandardCharsets.UTF_8)) {
            escribir(zip, "[Content_Types].xml", contentTypes(dibujoDeHoja, graficos));
            escribir(zip, "_rels/.rels",
                    "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                    + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                    + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>"
                    + "</Relationships>");
            escribir(zip, "xl/workbook.xml", workbook());
            escribir(zip, "xl/_rels/workbook.xml.rels", workbookRels());
            escribir(zip, "xl/styles.xml", estilos());
            int numGrafico = 0;
            for (int i = 0; i < hojas.size(); i++) {
                Hoja h = hojas.get(i);
                escribir(zip, "xl/worksheets/sheet" + (i + 1) + ".xml", hojaXml(h, dibujoDeHoja[i] > 0 ? dibujoDeHoja[i] : 0));
                if (dibujoDeHoja[i] > 0) {
                    int d = dibujoDeHoja[i];
                    escribir(zip, "xl/worksheets/_rels/sheet" + (i + 1) + ".xml.rels",
                            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                            + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                            + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/drawing\" Target=\"../drawings/drawing" + d + ".xml\"/>"
                            + "</Relationships>");
                    boolean usaLogo = h.conLogo && logo != null;
                    StringBuilder rels = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                            + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">");
                    if (usaLogo) {
                        rels.append("<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/image\" Target=\"../media/logo.png\"/>");
                    }
                    int primerGrafico = numGrafico;
                    for (int g = 0; g < h.graficos.size(); g++) {
                        numGrafico++;
                        rels.append("<Relationship Id=\"rId").append(g + 2)
                                .append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/chart\" Target=\"../charts/chart")
                                .append(numGrafico).append(".xml\"/>");
                    }
                    rels.append("</Relationships>");
                    escribir(zip, "xl/drawings/_rels/drawing" + d + ".xml.rels", rels.toString());
                    escribir(zip, "xl/drawings/drawing" + d + ".xml", dibujoXml(h, usaLogo, h.conLogo ? i : -1));
                    for (int g = 0; g < h.graficos.size(); g++) {
                        escribir(zip, "xl/charts/chart" + (primerGrafico + g + 1) + ".xml", graficoXml((Grafico) h.graficos.get(g)[0]));
                    }
                }
            }
            if (logo != null) {
                zip.putNextEntry(new ZipEntry("xl/media/logo.png"));
                zip.write(logo);
                zip.closeEntry();
            }
        }
    }

    private void cargarLogo() {
        if (logo != null) {
            return;
        }
        try (InputStream in = XlsxWriter.class.getResourceAsStream(RECURSO_LOGO)) {
            if (in == null) {
                return;
            }
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            in.transferTo(buf);
            byte[] datos = buf.toByteArray();
            java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(datos));
            if (img != null) {
                logoAncho = img.getWidth();
                logoAlto = img.getHeight();
                logo = datos;
            }
        } catch (IOException | RuntimeException e) {
            logo = null;
        }
    }


    private static void escribir(ZipOutputStream zip, String nombre, String contenido) throws IOException {
        zip.putNextEntry(new ZipEntry(nombre));
        zip.write(contenido.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private String contentTypes(int[] dibujoDeHoja, int graficos) {
        StringBuilder sb = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
                + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
                + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
                + "<Default Extension=\"png\" ContentType=\"image/png\"/>"
                + "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>"
                + "<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>");
        for (int i = 0; i < hojas.size(); i++) {
            sb.append("<Override PartName=\"/xl/worksheets/sheet").append(i + 1)
                    .append(".xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>");
            if (dibujoDeHoja[i] > 0) {
                sb.append("<Override PartName=\"/xl/drawings/drawing").append(dibujoDeHoja[i])
                        .append(".xml\" ContentType=\"application/vnd.openxmlformats-officedocument.drawing+xml\"/>");
            }
        }
        for (int g = 1; g <= graficos; g++) {
            sb.append("<Override PartName=\"/xl/charts/chart").append(g)
                    .append(".xml\" ContentType=\"application/vnd.openxmlformats-officedocument.drawingml.chart+xml\"/>");
        }
        return sb.append("</Types>").toString();
    }

    private String workbook() {
        StringBuilder sb = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" "
                + "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">"
                + "<bookViews><workbookView activeTab=\"0\"/></bookViews><sheets>");
        for (int i = 0; i < hojas.size(); i++) {
            sb.append("<sheet name=\"").append(esc(hojas.get(i).nombre)).append("\" sheetId=\"").append(i + 1)
                    .append("\" r:id=\"rId").append(i + 1).append("\"/>");
        }
        sb.append("</sheets><definedNames>");
        for (int i = 0; i < hojas.size(); i++) {
            Hoja h = hojas.get(i);
            String q = "'" + h.nombre.replace("'", "''") + "'";
            int cols = h.columnasUsadas();
            int filasN = h.filasUsadas();
            if (cols > 0 && filasN > 0) {
                sb.append("<definedName name=\"_xlnm.Print_Area\" localSheetId=\"").append(i).append("\">").append(esc(q))
                        .append("!$A$1:$").append(columna(cols - 1)).append('$').append(filasN).append("</definedName>");
            }
            if (h.filaCabecera >= 0 && h.conFiltro) {
                sb.append("<definedName name=\"_xlnm.Print_Titles\" localSheetId=\"").append(i).append("\">").append(esc(q))
                        .append("!$").append(h.filaCabecera + 1).append(":$").append(h.filaCabecera + 1).append("</definedName>");
            }
        }
        return sb.append("</definedNames><calcPr calcId=\"191029\" fullCalcOnLoad=\"1\"/></workbook>").toString();
    }

    private String workbookRels() {
        StringBuilder sb = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">");
        for (int i = 0; i < hojas.size(); i++) {
            sb.append("<Relationship Id=\"rId").append(i + 1)
                    .append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet")
                    .append(i + 1).append(".xml\"/>");
        }
        sb.append("<Relationship Id=\"rId").append(hojas.size() + 1)
                .append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>");
        return sb.append("</Relationships>").toString();
    }

    private static String fuente(boolean negrita, boolean cursiva, String color) {
        return "<font>" + (negrita ? "<b/>" : "") + (cursiva ? "<i/>" : "") + "<sz val=\"12\"/>"
                + (color == null ? "" : "<color rgb=\"FF" + color + "\"/>") + "<name val=\"" + FUENTE + "\"/><family val=\"1\"/></font>";
    }

    private static String relleno(String color) {
        return "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FF" + color + "\"/><bgColor indexed=\"64\"/></patternFill></fill>";
    }

    private static String borde(String estilo, String color) {
        String lado = "style=\"" + estilo + "\"><color rgb=\"FF" + color + "\"/>";
        return "<border><left " + lado + "</left><right " + lado + "</right><top " + lado + "</top><bottom " + lado + "</bottom><diagonal/></border>";
    }

    private static String estilos() {
        String fuentes = fuente(false, false, null) + fuente(true, false, null) + fuente(true, false, "FFFFFF")
                + fuente(false, true, "595959") + fuente(true, false, "006100") + fuente(true, false, "C00000")
                + fuente(true, false, "7F6000");
        String rellenos = "<fill><patternFill patternType=\"none\"/></fill><fill><patternFill patternType=\"gray125\"/></fill>"
                + relleno("000000") + relleno("F2F2F2") + relleno("D9D9D9") + relleno("FFC7CE") + relleno("FFEB9C")
                + relleno("C6EFCE") + relleno("FFFFFF");
        String bordes = "<border><left/><right/><top/><bottom/><diagonal/></border>"
                + borde("thin", "808080") + borde("thin", "000000")
                + "<border><left/><right/><top/><bottom style=\"medium\"><color rgb=\"FF000000\"/></bottom><diagonal/></border>"
                + borde("medium", "000000");
        StringBuilder sb = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">"
                + "<numFmts count=\"3\"><numFmt numFmtId=\"164\" formatCode=\"#,##0.00\"/>"
                + "<numFmt numFmtId=\"165\" formatCode=\"dd/mm/yyyy\\ hh:mm\"/><numFmt numFmtId=\"166\" formatCode=\"0.0%\"/></numFmts>"
                + "<fonts count=\"7\">" + fuentes + "</fonts>"
                + "<fills count=\"9\">" + rellenos + "</fills>"
                + "<borders count=\"5\">" + bordes + "</borders>"
                + "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>"
                + "<cellXfs count=\"30\">");
        Object[][] xf = {
            {0, 0, 0, 0, "", false}, // 0 DEFECTO
            {1, 0, 0, 0, "", false}, // 1 TITULO
            {3, 0, 0, 0, "", false}, // 2 SUBTITULO
            {2, 2, 2, 0, "center", true}, // 3 CABECERA
            {0, 0, 1, 0, "", false}, // 4 TXT
            {0, 0, 1, 3, "right", false}, // 5 NUM
            {0, 0, 1, 164, "right", false}, // 6 NUM_DEC
            {4, 7, 1, 0, "center", false}, // 7 VERDE
            {6, 6, 1, 0, "center", false}, // 8 AMARILLO
            {5, 5, 1, 0, "center", false}, // 9 ROJO
            {1, 4, 1, 0, "", false}, // 10 ETIQUETA
            {0, 0, 1, 0, "center", false}, // 11 CENTRO
            {4, 0, 1, 3, "right", false}, // 12 NUM_VERDE
            {5, 0, 1, 3, "right", false}, // 13 NUM_ROJO
            {1, 0, 3, 0, "", false}, // 14 SECCION
            {1, 0, 1, 3, "right", false}, // 15 NUM_NEGRITA
            {0, 0, 1, 9, "right", false}, // 16 PORCENTAJE
            {0, 0, 1, 165, "center", false}, // 17 FECHA
            {0, 0, 1, 14, "center", false}, // 18 FECHA_CORTA (formato local de fecha)
            {1, 0, 1, 0, "", false}, // 19 TXT_NEGRITA
            {1, 8, 4, 0, "center", false}, // 20 SELECTOR
            {1, 3, 2, 3, "center", false}, // 21 KPI_VALOR
            {0, 4, 2, 0, "center", true}, // 22 KPI_ETIQUETA
            {0, 0, 1, 3, "center", false}, // 23 NUM_CENTRO
            {0, 0, 1, 166, "right", false}, // 24 PORC_DEC
            {1, 0, 1, 0, "center", false}, // 25 CENTRO_NEGRITA
            {1, 4, 1, 3, "right", false}, // 26 ETIQUETA_NUM
            {1, 3, 2, 9, "center", false}, // 27 KPI_PORC
            {3, 0, 0, 0, "", true}, // 28 NOTA
            {1, 0, 1, 0, "center", false}, // 29 ESTADO
        };
        for (Object[] x : xf) {
            sb.append("<xf numFmtId=\"").append(x[3]).append("\" fontId=\"").append(x[0])
                    .append("\" fillId=\"").append(x[1]).append("\" borderId=\"").append(x[2])
                    .append("\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\" applyNumberFormat=\"1\" applyAlignment=\"1\">")
                    .append("<alignment");
            if (!((String) x[4]).isEmpty()) {
                sb.append(" horizontal=\"").append(x[4]).append("\"");
            }
            sb.append(" vertical=\"center\"").append((Boolean) x[5] ? " wrapText=\"1\"" : "").append("/></xf>");
        }
        sb.append("</cellXfs><cellStyles count=\"1\"><cellStyle name=\"Normal\" xfId=\"0\" builtinId=\"0\"/></cellStyles>");
        sb.append("<dxfs count=\"7\">")
                .append(dxf("9C0006", "FFC7CE")).append(dxf("7F6000", "FFEB9C")).append(dxf("006100", "C6EFCE"))
                .append("<dxf><fill><patternFill><bgColor rgb=\"FFF2F2F2\"/></patternFill></fill></dxf>")
                .append("<dxf><font><b/><color rgb=\"FF006100\"/></font></dxf>")
                .append("<dxf><font><b/><color rgb=\"FFC00000\"/></font></dxf>")
                .append("<dxf><font><b/><color rgb=\"FF7F6000\"/></font></dxf>")
                .append("</dxfs><tableStyles count=\"0\" defaultTableStyle=\"TableStyleMedium9\" defaultPivotStyle=\"PivotStyleLight16\"/></styleSheet>");
        return sb.toString();
    }

    private static String dxf(String texto, String fondo) {
        return "<dxf><font><b/><color rgb=\"FF" + texto + "\"/></font><fill><patternFill><bgColor rgb=\"FF" + fondo + "\"/></patternFill></fill></dxf>";
    }


    private String hojaXml(Hoja h, int dibujo) {
        StringBuilder sb = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" "
                + "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">"
                + "<sheetPr><pageSetUpPr fitToPage=\"1\"/></sheetPr>");
        int cols = Math.max(1, h.columnasUsadas());
        int filasN = Math.max(1, h.filasUsadas());
        sb.append("<dimension ref=\"A1:").append(columna(cols - 1)).append(filasN).append("\"/>");
        sb.append("<sheetViews><sheetView workbookViewId=\"0\" showGridLines=\"0\"").append(h.nombre.equals(hojas.get(0).nombre) ? " tabSelected=\"1\"" : "").append(">");
        if (h.filaCabecera >= 0 && h.conFiltro) {
            int split = h.filaCabecera + 1;
            sb.append("<pane ySplit=\"").append(split).append("\" topLeftCell=\"A").append(split + 1)
                    .append("\" activePane=\"bottomLeft\" state=\"frozen\"/>")
                    .append("<selection pane=\"bottomLeft\"/>");
        }
        sb.append("</sheetView></sheetViews><sheetFormatPr defaultRowHeight=\"").append(ALTO_FILA)
                .append("\" customHeight=\"1\" zeroHeight=\"1\"/>");
        sb.append("<cols>");
        for (int i = 0; i < cols; i++) {
            double w = i < h.anchos.length ? h.anchos[i] : 12;
            sb.append("<col min=\"").append(i + 1).append("\" max=\"").append(i + 1).append("\" width=\"")
                    .append(String.format(Locale.US, "%.2f", w)).append("\" customWidth=\"1\"/>");
        }
        sb.append("<col min=\"").append(cols + 1).append("\" max=\"16384\" width=\"9\" hidden=\"1\" customWidth=\"1\"/>");
        sb.append("</cols><sheetData>");
        int ultimaFilaDatos = h.filaCabecera;
        for (int r = 0; r < filasN; r++) {
            Object[] celdas = r < h.filas.size() ? (Object[]) h.filas.get(r)[0] : new Object[0];
            int[] est = r < h.filas.size() ? (int[]) h.filas.get(r)[1] : new int[0];
            double alto = ALTO_FILA;
            for (double[] a : h.alturas) {
                if ((int) a[0] == r + 1) {
                    alto = a[1];
                }
            }
            sb.append("<row r=\"").append(r + 1).append("\" ht=\"").append(String.format(Locale.US, "%.2f", alto))
                    .append("\" customHeight=\"1\">");
            for (int c = 0; c < celdas.length; c++) {
                int s = c < est.length ? est[c] : (est.length > 0 ? est[est.length - 1] : 0);
                String ref = columna(c) + (r + 1);
                Object v = celdas[c];
                if (v == null) {
                    if (c < est.length) {
                        sb.append("<c r=\"").append(ref).append("\" s=\"").append(s).append("\"/>");
                    }
                } else if (v instanceof Formula f) {
                    sb.append("<c r=\"").append(ref).append("\" s=\"").append(s).append("\"><f>").append(esc(f.expresion()))
                            .append("</f><v>").append(num(f.valor())).append("</v></c>");
                } else if (v instanceof FormulaTxt f) {
                    sb.append("<c r=\"").append(ref).append("\" s=\"").append(s).append("\" t=\"str\"><f>").append(esc(f.expresion()))
                            .append("</f><v>").append(esc(f.valor())).append("</v></c>");
                } else if (v instanceof Number n) {
                    sb.append("<c r=\"").append(ref).append("\" s=\"").append(s).append("\"><v>").append(num(n.doubleValue()))
                            .append("</v></c>");
                } else {
                    sb.append("<c r=\"").append(ref).append("\" s=\"").append(s).append("\" t=\"inlineStr\"><is><t xml:space=\"preserve\">")
                            .append(esc(v.toString())).append("</t></is></c>");
                }
            }
            sb.append("</row>");
            if (r > h.filaCabecera && h.filaCabecera >= 0 && celdas.length >= h.columnasCabecera && celdas.length > 0) {
                ultimaFilaDatos = r;
            }
        }
        sb.append("</sheetData>");
        if (h.ultimaFilaFiltro >= 0) {
            ultimaFilaDatos = h.ultimaFilaFiltro;
        }
        if (h.conFiltro && h.filaCabecera >= 0 && h.columnasCabecera > 0 && ultimaFilaDatos > h.filaCabecera) {
            sb.append("<autoFilter ref=\"A").append(h.filaCabecera + 1).append(":").append(columna(h.columnasCabecera - 1))
                    .append(ultimaFilaDatos + 1).append("\"/>");
        }
        if (!h.combinadas.isEmpty()) {
            sb.append("<mergeCells count=\"").append(h.combinadas.size()).append("\">");
            for (String m : h.combinadas) {
                sb.append("<mergeCell ref=\"").append(m).append("\"/>");
            }
            sb.append("</mergeCells>");
        }
        int prioridad = 1;
        for (String[] c : h.condicionales) {
            sb.append("<conditionalFormatting sqref=\"").append(c[0]).append("\"><cfRule type=\"expression\" dxfId=\"").append(c[2])
                    .append("\" priority=\"").append(prioridad++).append("\"><formula>").append(esc(c[1])).append("</formula></cfRule></conditionalFormatting>");
        }
        if (!h.listas.isEmpty()) {
            sb.append("<dataValidations count=\"").append(h.listas.size()).append("\">");
            for (String[] l : h.listas) {
                sb.append("<dataValidation type=\"list\" allowBlank=\"0\" showInputMessage=\"1\" showErrorMessage=\"1\" ")
                        .append("errorTitle=\"Valor no válido\" error=\"Elija un valor de la lista.\" sqref=\"").append(l[0]).append("\">")
                        .append("<formula1>").append(esc(l[1])).append("</formula1></dataValidation>");
            }
            sb.append("</dataValidations>");
        }
        sb.append("<pageMargins left=\"0.5\" right=\"0.5\" top=\"0.6\" bottom=\"0.7\" header=\"0.3\" footer=\"0.3\"/>");
        sb.append("<pageSetup orientation=\"").append(h.vertical ? "portrait" : "landscape").append("\" fitToWidth=\"1\" fitToHeight=\"0\"/>");
        sb.append("<headerFooter><oddFooter>&amp;L&amp;\"Times New Roman,Regular\"&amp;12RyR ControlCenter&amp;C&amp;\"Times New Roman,Regular\"&amp;12&amp;A"
                + "&amp;R&amp;\"Times New Roman,Regular\"&amp;12Página &amp;P de &amp;N</oddFooter></headerFooter>");
        if (!h.saltos.isEmpty()) {
            sb.append("<rowBreaks count=\"").append(h.saltos.size()).append("\" manualBreakCount=\"").append(h.saltos.size()).append("\">");
            for (int b : h.saltos) {
                sb.append("<brk id=\"").append(b).append("\" max=\"16383\" man=\"1\"/>");
            }
            sb.append("</rowBreaks>");
        }
        if (dibujo > 0) {
            sb.append("<drawing r:id=\"rId1\"/>");
        }
        return sb.append("</worksheet>").toString();
    }


    private static long emu(double puntos) {
        return Math.round(puntos * 12700);
    }

    private String dibujoXml(Hoja h, boolean conLogo, int idx) {
        StringBuilder sb = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<xdr:wsDr xmlns:xdr=\"http://schemas.openxmlformats.org/drawingml/2006/spreadsheetDrawing\" "
                + "xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\" "
                + "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\" "
                + "xmlns:c=\"http://schemas.openxmlformats.org/drawingml/2006/chart\">");
        int id = 2;
        if (conLogo) {
            double alto = 52;
            double ancho = alto * logoAncho / logoAlto;
            long cx = emu(ancho), cy = emu(alto);
            sb.append("<xdr:oneCellAnchor><xdr:from><xdr:col>0</xdr:col><xdr:colOff>").append(emu(4)).append("</xdr:colOff><xdr:row>0</xdr:row><xdr:rowOff>")
                    .append(emu(1.5)).append("</xdr:rowOff></xdr:from><xdr:ext cx=\"").append(cx).append("\" cy=\"").append(cy).append("\"/>")
                    .append("<xdr:pic><xdr:nvPicPr><xdr:cNvPr id=\"").append(id++).append("\" name=\"Logo RyR ControlCenter\" descr=\"Logo de RyR ControlCenter\"/>")
                    .append("<xdr:cNvPicPr><a:picLocks noChangeAspect=\"1\"/></xdr:cNvPicPr></xdr:nvPicPr>")
                    .append("<xdr:blipFill><a:blip r:embed=\"rId1\"/><a:stretch><a:fillRect/></a:stretch></xdr:blipFill>")
                    .append("<xdr:spPr><a:xfrm><a:off x=\"0\" y=\"0\"/><a:ext cx=\"").append(cx).append("\" cy=\"").append(cy).append("\"/></a:xfrm>")
                    .append("<a:prstGeom prst=\"rect\"><a:avLst/></a:prstGeom></xdr:spPr></xdr:pic><xdr:clientData/></xdr:oneCellAnchor>");
        }
        for (int g = 0; g < h.graficos.size(); g++) {
            Object[] o = h.graficos.get(g);
            int c1 = (Integer) o[1], f1 = (Integer) o[2], c2 = (Integer) o[3], f2 = (Integer) o[4];
            sb.append("<xdr:twoCellAnchor><xdr:from><xdr:col>").append(c1).append("</xdr:col><xdr:colOff>").append(emu(3))
                    .append("</xdr:colOff><xdr:row>").append(f1 - 1).append("</xdr:row><xdr:rowOff>").append(emu(3))
                    .append("</xdr:rowOff></xdr:from><xdr:to><xdr:col>").append(c2).append("</xdr:col><xdr:colOff>0</xdr:colOff><xdr:row>")
                    .append(f2 - 1).append("</xdr:row><xdr:rowOff>0</xdr:rowOff></xdr:to>")
                    .append("<xdr:graphicFrame macro=\"\"><xdr:nvGraphicFramePr><xdr:cNvPr id=\"").append(id++).append("\" name=\"Gráfico ").append(g + 1)
                    .append("\"/><xdr:cNvGraphicFramePr/></xdr:nvGraphicFramePr>")
                    .append("<xdr:xfrm><a:off x=\"0\" y=\"0\"/><a:ext cx=\"0\" cy=\"0\"/></xdr:xfrm>")
                    .append("<a:graphic><a:graphicData uri=\"http://schemas.openxmlformats.org/drawingml/2006/chart\">")
                    .append("<c:chart r:id=\"rId").append(g + 2).append("\"/></a:graphicData></a:graphic></xdr:graphicFrame><xdr:clientData/></xdr:twoCellAnchor>");
        }
        return sb.append("</xdr:wsDr>").toString();
    }

    private static String txPr(int size, boolean negrita) {
        return "<c:txPr><a:bodyPr/><a:lstStyle/><a:p><a:pPr><a:defRPr sz=\"" + size * 100 + "\" b=\"" + (negrita ? 1 : 0)
                + "\"><a:solidFill><a:srgbClr val=\"000000\"/></a:solidFill><a:latin typeface=\"" + FUENTE + "\"/><a:cs typeface=\"" + FUENTE
                + "\"/></a:defRPr></a:pPr><a:endParaRPr lang=\"es-CO\"/></a:p></c:txPr>";
    }

    private static String titulo(String t, int size) {
        return "<c:title><c:tx><c:rich><a:bodyPr/><a:lstStyle/><a:p><a:pPr><a:defRPr sz=\"" + size * 100 + "\" b=\"1\"/></a:pPr><a:r><a:rPr lang=\"es-CO\" sz=\""
                + size * 100 + "\" b=\"1\"><a:solidFill><a:srgbClr val=\"000000\"/></a:solidFill><a:latin typeface=\"" + FUENTE + "\"/><a:cs typeface=\"" + FUENTE
                + "\"/></a:rPr><a:t>" + esc(t) + "</a:t></a:r></a:p></c:rich></c:tx><c:overlay val=\"0\"/></c:title>";
    }

    private static String color(int rgb) {
        return String.format("%06X", rgb & 0xFFFFFF);
    }

    private static String relleno(int rgb, boolean borde) {
        return "<c:spPr><a:solidFill><a:srgbClr val=\"" + color(rgb) + "\"/></a:solidFill>"
                + (borde ? "<a:ln w=\"9525\"><a:solidFill><a:srgbClr val=\"000000\"/></a:solidFill></a:ln>" : "") + "</c:spPr>";
    }

    private static String cacheTexto(Ref r) {
        StringBuilder sb = new StringBuilder("<c:strCache><c:ptCount val=\"").append(r.tamano()).append("\"/>");
        for (int i = 1; i <= r.tamano(); i++) {
            Object v = r.valor(i);
            String s = v instanceof Double d ? num(d) : v == null ? "" : v.toString();
            sb.append("<c:pt idx=\"").append(i - 1).append("\"><c:v>").append(esc(s)).append("</c:v></c:pt>");
        }
        return sb.append("</c:strCache>").toString();
    }

    private static String cacheNumeros(Ref r) {
        StringBuilder sb = new StringBuilder("<c:numCache><c:formatCode>General</c:formatCode><c:ptCount val=\"").append(r.tamano()).append("\"/>");
        for (int i = 1; i <= r.tamano(); i++) {
            Object v = r.valor(i);
            double d = v instanceof Number n ? n.doubleValue() : 0;
            sb.append("<c:pt idx=\"").append(i - 1).append("\"><c:v>").append(num(d)).append("</c:v></c:pt>");
        }
        return sb.append("</c:numCache>").toString();
    }

    private static String graficoXml(Grafico g) {
        StringBuilder sb = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<c:chartSpace xmlns:c=\"http://schemas.openxmlformats.org/drawingml/2006/chart\" "
                + "xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\" "
                + "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">"
                + "<c:roundedCorners val=\"0\"/><c:chart>");
        sb.append(titulo(g.titulo, 12)).append("<c:autoTitleDeleted val=\"0\"/><c:plotArea><c:layout/>");
        boolean torta = g.tipo == TipoGrafico.TORTA;
        boolean lineas = g.tipo == TipoGrafico.LINEAS;
        if (torta) {
            sb.append("<c:pieChart><c:varyColors val=\"1\"/>");
        } else if (lineas) {
            sb.append("<c:lineChart><c:grouping val=\"standard\"/><c:varyColors val=\"0\"/>");
        } else {
            sb.append("<c:barChart><c:barDir val=\"").append(g.tipo == TipoGrafico.BARRAS ? "bar" : "col").append("\"/><c:grouping val=\"")
                    .append(g.apilado ? "stacked" : "clustered").append("\"/><c:varyColors val=\"0\"/>");
        }
        String[] guiones = {"solid", "dash", "sysDot", "dashDot", "solid", "dash"};
        String[] marcas = {"square", "diamond", "triangle", "circle", "x", "plus"};
        for (int s = 0; s < g.series.size(); s++) {
            Object[] ser = g.series.get(s);
            String nombre = (String) ser[0];
            Ref valores = (Ref) ser[1];
            int col = (Integer) ser[2];
            int[] puntos = (int[]) ser[3];
            sb.append("<c:ser><c:idx val=\"").append(s).append("\"/><c:order val=\"").append(s).append("\"/>")
                    .append("<c:tx><c:v>").append(esc(nombre)).append("</c:v></c:tx>");
            if (lineas) {
                sb.append("<c:spPr><a:ln w=\"28575\" cap=\"rnd\"><a:solidFill><a:srgbClr val=\"").append(color(col)).append("\"/></a:solidFill><a:prstDash val=\"")
                        .append(guiones[s % guiones.length]).append("\"/><a:round/></a:ln></c:spPr>")
                        .append("<c:marker><c:symbol val=\"").append(marcas[s % marcas.length]).append("\"/><c:size val=\"7\"/><c:spPr><a:solidFill><a:srgbClr val=\"")
                        .append(color(col)).append("\"/></a:solidFill><a:ln w=\"9525\"><a:solidFill><a:srgbClr val=\"000000\"/></a:solidFill></a:ln></c:spPr></c:marker>");
            } else if (torta) {
                sb.append("<c:spPr><a:ln w=\"9525\"><a:solidFill><a:srgbClr val=\"000000\"/></a:solidFill></a:ln></c:spPr>");
            } else {
                sb.append(relleno(col, true)).append("<c:invertIfNegative val=\"0\"/>");
            }
            if (puntos != null && !lineas) {
                for (int p = 0; p < puntos.length; p++) {
                    sb.append("<c:dPt><c:idx val=\"").append(p).append("\"/>");
                    if (!torta) {
                        sb.append("<c:invertIfNegative val=\"0\"/>");
                    }
                    sb.append("<c:bubble3D val=\"0\"/>").append(relleno(puntos[p], true)).append("</c:dPt>");
                }
            }
            if (g.etiquetas) {
                sb.append("<c:dLbls>").append(torta ? "" : "<c:numFmt formatCode=\"#,##0;-#,##0;;\" sourceLinked=\"0\"/>").append("<c:spPr><a:noFill/><a:ln><a:noFill/></a:ln></c:spPr>").append(txPr(12, false));
                if (torta) {
                    sb.append("<c:dLblPos val=\"outEnd\"/>");
                } else if (!lineas && !g.apilado) {
                    sb.append("<c:dLblPos val=\"outEnd\"/>");
                } else if (lineas) {
                    sb.append("<c:dLblPos val=\"t\"/>");
                }
                sb.append("<c:showLegendKey val=\"0\"/><c:showVal val=\"").append(torta ? 0 : 1).append("\"/><c:showCatName val=\"0\"/>")
                        .append("<c:showSerName val=\"0\"/><c:showPercent val=\"").append(torta ? 1 : 0).append("\"/><c:showBubbleSize val=\"0\"/>");
                if (torta) {
                    sb.append("<c:showLeaderLines val=\"1\"/>");
                }
                sb.append("</c:dLbls>");
            }
            if (g.categorias != null) {
                sb.append("<c:cat><c:strRef><c:f>").append(esc(g.categorias.formula())).append("</c:f>").append(cacheTexto(g.categorias)).append("</c:strRef></c:cat>");
            }
            sb.append("<c:val><c:numRef><c:f>").append(esc(valores.formula())).append("</c:f>").append(cacheNumeros(valores)).append("</c:numRef></c:val>");
            if (lineas) {
                sb.append("<c:smooth val=\"0\"/>");
            }
            sb.append("</c:ser>");
        }
        if (torta) {
            sb.append("<c:firstSliceAng val=\"0\"/></c:pieChart>");
        } else {
            if (lineas) {
                sb.append("<c:marker val=\"1\"/>");
            } else {
                sb.append("<c:gapWidth val=\"70\"/>").append(g.apilado ? "<c:overlap val=\"100\"/>" : "<c:overlap val=\"-5\"/>");
            }
            sb.append("<c:axId val=\"111\"/><c:axId val=\"222\"/>").append(lineas ? "</c:lineChart>" : "</c:barChart>");
            boolean horizontal = g.tipo == TipoGrafico.BARRAS;

            sb.append("<c:catAx><c:axId val=\"111\"/><c:scaling><c:orientation val=\"").append(horizontal ? "maxMin" : "minMax").append("\"/></c:scaling>")
                    .append("<c:delete val=\"0\"/><c:axPos val=\"").append(horizontal ? "l" : "b").append("\"/><c:numFmt formatCode=\"General\" sourceLinked=\"0\"/>")
                    .append("<c:majorTickMark val=\"out\"/><c:minorTickMark val=\"none\"/><c:tickLblPos val=\"nextTo\"/>")
                    .append("<c:spPr><a:ln w=\"12700\"><a:solidFill><a:srgbClr val=\"000000\"/></a:solidFill></a:ln></c:spPr>").append(txPr(12, false))
                    .append("<c:crossAx val=\"222\"/><c:crosses val=\"autoZero\"/><c:auto val=\"1\"/><c:lblAlgn val=\"ctr\"/><c:lblOffset val=\"100\"/><c:noMultiLvlLbl val=\"0\"/></c:catAx>");
            sb.append("<c:valAx><c:axId val=\"222\"/><c:scaling><c:orientation val=\"minMax\"/><c:min val=\"0\"/></c:scaling><c:delete val=\"0\"/><c:axPos val=\"")
                    .append(horizontal ? "t" : "l").append("\"/><c:majorGridlines><c:spPr><a:ln w=\"6350\"><a:solidFill><a:srgbClr val=\"BFBFBF\"/></a:solidFill></a:ln></c:spPr></c:majorGridlines>");
            if (g.tituloEjeY != null) {
                sb.append(titulo(g.tituloEjeY, 12));
            }
            sb.append("<c:numFmt formatCode=\"#,##0\" sourceLinked=\"0\"/><c:majorTickMark val=\"out\"/><c:minorTickMark val=\"none\"/><c:tickLblPos val=\"nextTo\"/>")
                    .append("<c:spPr><a:ln w=\"12700\"><a:solidFill><a:srgbClr val=\"000000\"/></a:solidFill></a:ln></c:spPr>").append(txPr(12, false))
                    .append("<c:crossAx val=\"111\"/><c:crosses val=\"").append(horizontal ? "max" : "autoZero").append("\"/><c:crossBetween val=\"between\"/></c:valAx>");
        }
        sb.append("<c:spPr><a:noFill/></c:spPr></c:plotArea>");
        if (g.leyenda && (g.series.size() > 1 || torta)) {
            sb.append("<c:legend><c:legendPos val=\"b\"/><c:overlay val=\"0\"/>").append(txPr(12, false)).append("</c:legend>");
        }
        sb.append("<c:plotVisOnly val=\"1\"/><c:dispBlanksAs val=\"gap\"/></c:chart>")
                .append("<c:spPr><a:solidFill><a:srgbClr val=\"FFFFFF\"/></a:solidFill><a:ln w=\"12700\"><a:solidFill><a:srgbClr val=\"000000\"/></a:solidFill></a:ln></c:spPr>")
                .append(txPr(12, false)).append("</c:chartSpace>");
        return sb.toString();
    }


    public static String columna(int i) {
        StringBuilder sb = new StringBuilder();
        int n = i + 1;
        while (n > 0) {
            int m = (n - 1) % 26;
            sb.insert(0, (char) ('A' + m));
            n = (n - 1) / 26;
        }
        return sb.toString();
    }

    public static double serie(java.time.LocalDateTime t) {
        long dias = java.time.temporal.ChronoUnit.DAYS.between(java.time.LocalDate.of(1899, 12, 30), t.toLocalDate());
        return dias + t.toLocalTime().toSecondOfDay() / 86400.0;
    }

    private static String num(double d) {
        if (d == Math.rint(d) && Math.abs(d) < 1e15) {
            return Long.toString((long) d);
        }
        return String.format(Locale.US, "%s", d);
    }

    private static String esc(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '&') {
                sb.append("&amp;");
            } else if (c == '<') {
                sb.append("&lt;");
            } else if (c == '>') {
                sb.append("&gt;");
            } else if (c == '"') {
                sb.append("&quot;");
            } else if (c < 0x20 && c != '\t' && c != '\n' && c != '\r') {
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
