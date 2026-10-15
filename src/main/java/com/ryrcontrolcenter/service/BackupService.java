package com.ryrcontrolcenter.service;

import com.ryrcontrolcenter.config.ConexionBD;
import com.ryrcontrolcenter.modelo.CopiaSeguridad;
import com.ryrcontrolcenter.modelo.CopiaSeguridad.Tipo;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.sqlite.SQLiteConfig;

/**
 * Copias de seguridad y restauracion de la base de datos SQLite (RF-6).
 *
 * Copia:  VACUUM INTO de SQLite. Genera una copia consistente aunque la
 *         aplicacion este en uso; se escribe a un temporal, se verifica y solo
 *         entonces recibe su nombre definitivo (nunca queda una copia a medias).
 * Restauracion: se valida la copia (formato, integridad y que su esquema sea
 *         compatible), se guarda antes una copia del estado actual y los datos
 *         se reemplazan en UNA sola transaccion: o se aplica todo o no cambia nada.
 */
public final class BackupService {

    private static final byte[] CABECERA_SQLITE = "SQLite format 3\0".getBytes(StandardCharsets.ISO_8859_1);
    private static final List<String> TABLAS_RESUMEN = List.of(
            "usuarios", "inventario", "prestamos", "filtros", "movimientos_kardex", "bitacora");
    private static final Path ARCHIVO_LOG = Path.of("data", "backup.log");
    private static final long LOG_MAX_BYTES = 1_000_000L;
    private static final DateTimeFormatter FORMATO_LOG = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final ReentrantLock CANDADO = new ReentrantLock();

    private BackupService() {
    }

    // =====================================================================
    //  CREAR COPIAS
    // =====================================================================

    /** Crea una copia con nombre automatico dentro de la carpeta indicada. */
    public static CopiaSeguridad crearCopia(Tipo tipo, Path carpeta) throws BackupException {
        return crearCopia(tipo, carpeta, LocalDateTime.now());
    }

    static CopiaSeguridad crearCopia(Tipo tipo, Path carpeta, LocalDateTime momento) throws BackupException {
        bloquear();
        try {
            Path dir = prepararCarpeta(carpeta);
            Path destino = nombreUnico(dir, momento, tipo);
            copiarBD(destino, false);
            CopiaSeguridad copia = CopiaSeguridad.desdeArchivo(destino)
                    .orElseThrow(() -> new BackupException("No se pudo registrar la copia creada."));
            log(tipo.getCodigo().toUpperCase(), "OK", destino + " (" + copia.getTamanoTexto() + ")");
            return copia;
        } catch (BackupException e) {
            log(tipo.getCodigo().toUpperCase(), "ERROR", e.getMessage());
            throw e;
        } finally {
            CANDADO.unlock();
        }
    }

    /** Copia manual hacia un archivo elegido por el usuario (cualquier carpeta, p. ej. una USB). */
    public static Path guardarCopiaEn(Path archivoDestino) throws BackupException {
        bloquear();
        try {
            Path destino = archivoDestino.toAbsolutePath();
            if (!destino.getFileName().toString().toLowerCase().endsWith(".db")) {
                destino = destino.resolveSibling(destino.getFileName() + ".db");
            }
            if (destino.equals(ConexionBD.RUTA_ARCHIVO.toAbsolutePath())) {
                throw new BackupException("No se puede guardar la copia encima de la base de datos en uso.");
            }
            prepararCarpeta(destino.getParent());
            copiarBD(destino, true);
            log("MANUAL", "OK", destino + " (guardada en ubicación elegida)");
            return destino;
        } catch (BackupException e) {
            log("MANUAL", "ERROR", e.getMessage());
            throw e;
        } finally {
            CANDADO.unlock();
        }
    }

    private static void copiarBD(Path destino, boolean reemplazar) throws BackupException {
        Path tmp = destino.resolveSibling(destino.getFileName() + ".tmp");
        try {
            Files.deleteIfExists(tmp);
            try (Connection c = ConexionBD.conectar(); Statement st = c.createStatement()) {
                st.execute("PRAGMA busy_timeout = 10000");
                st.execute("VACUUM INTO '" + literal(tmp) + "'");
            }
            verificarArchivo(tmp, false);
            if (reemplazar) {
                Files.move(tmp, destino, StandardCopyOption.REPLACE_EXISTING);
            } else {
                Files.move(tmp, destino);
            }
        } catch (SQLException e) {
            throw new BackupException("No se pudo generar la copia de seguridad: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new BackupException("No se pudo escribir la copia en \"" + destino + "\": " + e.getMessage(), e);
        } finally {
            try {
                Files.deleteIfExists(tmp);
            } catch (IOException ignorado) {
                // el temporal queda huerfano pero no se lista como copia
            }
        }
    }

    // =====================================================================
    //  VERIFICAR
    // =====================================================================

    /** Verifica que el archivo sea una copia valida y compatible con esta version del programa. */
    public static void verificar(Path archivo) throws BackupException {
        verificarArchivo(archivo, true);
    }

    static void verificarArchivo(Path archivo, boolean exigirCompatibilidad) throws BackupException {
        if (!Files.isRegularFile(archivo)) {
            throw new BackupException("El archivo no existe: " + archivo);
        }
        try {
            if (Files.size(archivo) < 512) {
                throw new BackupException("El archivo no es una base de datos SQLite válida (demasiado pequeño).");
            }
            byte[] cabecera = new byte[CABECERA_SQLITE.length];
            try (InputStream in = Files.newInputStream(archivo)) {
                if (in.readNBytes(cabecera, 0, cabecera.length) < cabecera.length
                        || !Arrays.equals(cabecera, CABECERA_SQLITE)) {
                    throw new BackupException("El archivo no es una base de datos SQLite válida.");
                }
            }
        } catch (IOException e) {
            throw new BackupException("No se pudo leer el archivo: " + e.getMessage(), e);
        }

        try (Connection c = abrirSoloLectura(archivo); Statement st = c.createStatement()) {
            try (ResultSet rs = st.executeQuery("PRAGMA integrity_check")) {
                String resultado = rs.next() ? rs.getString(1) : "sin respuesta";
                if (!"ok".equalsIgnoreCase(resultado)) {
                    throw new BackupException("La copia está dañada (verificación de integridad: " + resultado + ").");
                }
            }
            if (exigirCompatibilidad) {
                comprobarEsquema(c);
            }
        } catch (SQLException e) {
            throw new BackupException("El archivo no se pudo abrir como base de datos: " + e.getMessage(), e);
        }
    }

    /** Toda tabla y columna de la base actual debe existir en la copia. */
    private static void comprobarEsquema(Connection copia) throws SQLException, BackupException {
        Map<String, List<String>> actual;
        try (Connection c = ConexionBD.conectar()) {
            actual = esquema(c);
        }
        Map<String, List<String>> enCopia = esquema(copia);
        List<String> problemas = new ArrayList<>();
        for (Map.Entry<String, List<String>> e : actual.entrySet()) {
            List<String> columnasCopia = enCopia.get(e.getKey());
            if (columnasCopia == null) {
                problemas.add("falta la tabla \"" + e.getKey() + "\"");
                continue;
            }
            for (String col : e.getValue()) {
                if (!columnasCopia.contains(col)) {
                    problemas.add("la tabla \"" + e.getKey() + "\" no tiene la columna \"" + col + "\"");
                }
            }
        }
        if (!problemas.isEmpty()) {
            throw new BackupException("La copia no es compatible con esta versión del programa ("
                    + String.join("; ", problemas)
                    + "). Probablemente fue creada con una versión anterior.");
        }
    }

    private static Map<String, List<String>> esquema(Connection c) throws SQLException {
        Map<String, List<String>> mapa = new LinkedHashMap<>();
        List<String> tablas = new ArrayList<>();
        try (Statement st = c.createStatement();
                ResultSet rs = st.executeQuery(
                        "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' ORDER BY name")) {
            while (rs.next()) {
                tablas.add(rs.getString(1));
            }
        }
        for (String t : tablas) {
            List<String> columnas = new ArrayList<>();
            try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery("PRAGMA table_info(\"" + t + "\")")) {
                while (rs.next()) {
                    columnas.add(rs.getString("name"));
                }
            }
            mapa.put(t, columnas);
        }
        return mapa;
    }

    // =====================================================================
    //  DESCRIBIR (conteo de registros)
    // =====================================================================

    /** Cantidad de registros de las tablas principales de una copia. */
    public static Map<String, Integer> describir(Path archivo) throws BackupException {
        try (Connection c = abrirSoloLectura(archivo)) {
            return contar(c, "");
        } catch (SQLException e) {
            throw new BackupException("No se pudo leer la copia: " + e.getMessage(), e);
        }
    }

    /** Cantidad de registros de las tablas principales de la base actual. */
    public static Map<String, Integer> describirActual() throws BackupException {
        try (Connection c = ConexionBD.conectar()) {
            return contar(c, "");
        } catch (SQLException e) {
            throw new BackupException("No se pudo leer la base de datos actual: " + e.getMessage(), e);
        }
    }

    private static Map<String, Integer> contar(Connection c, String esquema) throws SQLException {
        Map<String, Integer> conteo = new LinkedHashMap<>();
        for (String t : TABLAS_RESUMEN) {
            try (Statement st = c.createStatement();
                    ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + esquema + "\"" + t + "\"")) {
                conteo.put(t, rs.next() ? rs.getInt(1) : 0);
            } catch (SQLException e) {
                conteo.put(t, -1); // la tabla no existe en esa copia
            }
        }
        return conteo;
    }

    // =====================================================================
    //  RESTAURAR
    // =====================================================================

    /**
     * Restaura la base de datos desde una copia. Devuelve la copia "previa a
     * restaurar" que se creo para poder deshacer la operacion.
     */
    public static CopiaSeguridad restaurar(Path origen, Path carpetaCopiaPrevia) throws BackupException {
        bloquear();
        try {
            verificarArchivo(origen, true);

            CopiaSeguridad previa = crearCopia(Tipo.PRERESTAURACION, carpetaCopiaPrevia);

            try {
                aplicar(origen);
            } catch (BackupException e) {
                log("RESTAURAR", "ERROR", origen + " -> " + e.getMessage());
                throw new BackupException(e.getMessage() + "\nLa base de datos actual no fue modificada.", e);
            }

            try {
                verificarBaseActual();
            } catch (BackupException e) {
                // muy improbable: se vuelve al estado anterior
                try {
                    aplicar(previa.getArchivo());
                } catch (BackupException ignorado) {
                    log("RESTAURAR", "ERROR", "No se pudo volver al estado anterior: " + ignorado.getMessage());
                }
                log("RESTAURAR", "ERROR", "Verificación posterior fallida, se revirtió: " + e.getMessage());
                throw new BackupException("La restauración no pasó la verificación final y se volvió al estado anterior.", e);
            }

            log("RESTAURAR", "OK", origen + " (copia previa: " + previa.getNombreArchivo() + ")");
            return previa;
        } finally {
            CANDADO.unlock();
        }
    }

    private static void verificarBaseActual() throws BackupException {
        try (Connection c = ConexionBD.conectar(); Statement st = c.createStatement();
                ResultSet rs = st.executeQuery("PRAGMA integrity_check")) {
            String r = rs.next() ? rs.getString(1) : "sin respuesta";
            if (!"ok".equalsIgnoreCase(r)) {
                throw new BackupException("Integridad de la base de datos: " + r);
            }
        } catch (SQLException e) {
            throw new BackupException("No se pudo verificar la base de datos: " + e.getMessage(), e);
        }
    }

    /** Reemplaza los datos de la base actual por los de la copia, en una sola transaccion. */
    private static void aplicar(Path origen) throws BackupException {
        try (Connection c = ConexionBD.conectar()) {
            Map<String, List<String>> tablas = esquema(c);
            try (Statement st = c.createStatement()) {
                st.execute("PRAGMA busy_timeout = 10000");
                st.execute("PRAGMA foreign_keys = OFF"); // no se puede cambiar dentro de una transaccion
                st.execute("ATTACH DATABASE '" + literal(origen) + "' AS bk");
            }
            try {
                c.setAutoCommit(false);
                try (Statement st = c.createStatement()) {
                    for (String t : tablas.keySet()) {
                        st.executeUpdate("DELETE FROM main.\"" + t + "\"");
                    }
                    for (Map.Entry<String, List<String>> e : tablas.entrySet()) {
                        String cols = e.getValue().stream().map(x -> "\"" + x + "\"").collect(Collectors.joining(", "));
                        st.executeUpdate("INSERT INTO main.\"" + e.getKey() + "\" (" + cols + ") SELECT "
                                + cols + " FROM bk.\"" + e.getKey() + "\"");
                    }
                    if (existeTablaSecuencias(st)) {
                        st.executeUpdate("DELETE FROM main.sqlite_sequence");
                        st.executeUpdate("INSERT INTO main.sqlite_sequence (name, seq) SELECT name, seq FROM bk.sqlite_sequence");
                    }
                    try (ResultSet rs = st.executeQuery("PRAGMA main.foreign_key_check")) {
                        if (rs.next()) {
                            throw new BackupException("La copia tiene datos inconsistentes (referencias rotas en la tabla \""
                                    + rs.getString(1) + "\").");
                        }
                    }
                    for (String t : tablas.keySet()) {
                        int enBase = contarTabla(st, "main", t);
                        int enCopia = contarTabla(st, "bk", t);
                        if (enBase != enCopia) {
                            throw new BackupException("La tabla \"" + t + "\" quedó con " + enBase
                                    + " registros y la copia tiene " + enCopia + ".");
                        }
                    }
                }
                c.commit();
            } catch (SQLException e) {
                rollback(c);
                throw new BackupException("No se pudo aplicar la copia: " + e.getMessage(), e);
            } catch (BackupException e) {
                rollback(c);
                throw e;
            } finally {
                try {
                    c.setAutoCommit(true);
                } catch (SQLException ignorado) {
                    // se cierra la conexion de todos modos
                }
                try (Statement st = c.createStatement()) {
                    st.execute("DETACH DATABASE bk");
                    st.execute("PRAGMA foreign_keys = ON");
                } catch (SQLException ignorado) {
                    // la conexion se descarta
                }
            }
        } catch (SQLException e) {
            throw new BackupException("No se pudo abrir la base de datos: " + e.getMessage(), e);
        }
    }

    private static boolean existeTablaSecuencias(Statement st) throws SQLException {
        try (ResultSet rs = st.executeQuery("SELECT 1 FROM bk.sqlite_master WHERE name = 'sqlite_sequence'")) {
            return rs.next();
        }
    }

    private static int contarTabla(Statement st, String esquema, String tabla) throws SQLException {
        try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + esquema + ".\"" + tabla + "\"")) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private static void rollback(Connection c) {
        try {
            c.rollback();
        } catch (SQLException ignorado) {
            // nada que hacer
        }
    }

    // =====================================================================
    //  LISTAR / ELIMINAR / RETENCION
    // =====================================================================

    /** Copias con formato RyR dentro de la carpeta, de la mas reciente a la mas antigua. */
    public static List<CopiaSeguridad> listar(Path carpeta) {
        if (carpeta == null || !Files.isDirectory(carpeta)) {
            return new ArrayList<>();
        }
        try (Stream<Path> archivos = Files.list(carpeta)) {
            return archivos
                    .map(CopiaSeguridad::desdeArchivo)
                    .flatMap(java.util.Optional::stream)
                    .sorted(Comparator.comparing(CopiaSeguridad::getFecha).reversed()
                            .thenComparing(CopiaSeguridad::getNombreArchivo, Comparator.reverseOrder()))
                    .collect(Collectors.toCollection(ArrayList::new));
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    public static void eliminar(CopiaSeguridad copia) throws BackupException {
        try {
            Files.deleteIfExists(copia.getArchivo());
            log("ELIMINAR", "OK", copia.getArchivo().toString());
        } catch (IOException e) {
            throw new BackupException("No se pudo eliminar la copia: " + e.getMessage(), e);
        }
    }

    /** Borra las copias automaticas y "al cerrar" mas antiguas, dejando las ultimas N. Las manuales nunca se borran. */
    public static int aplicarRetencion(Path carpeta, int conservar) {
        List<CopiaSeguridad> automaticas = listar(carpeta).stream()
                .filter(c -> c.getTipo() == Tipo.AUTOMATICA || c.getTipo() == Tipo.CIERRE)
                .collect(Collectors.toList()); // ya vienen de la mas reciente a la mas antigua
        int borradas = 0;
        for (int i = Math.max(conservar, 0); i < automaticas.size(); i++) {
            try {
                Files.deleteIfExists(automaticas.get(i).getArchivo());
                borradas++;
            } catch (IOException ignorado) {
                // se intentara de nuevo en la siguiente copia
            }
        }
        if (borradas > 0) {
            log("RETENCION", "OK", borradas + " copia(s) antigua(s) eliminada(s) en " + carpeta);
        }
        return borradas;
    }

    // =====================================================================
    //  UTILIDADES
    // =====================================================================

    private static void bloquear() throws BackupException {
        try {
            if (!CANDADO.tryLock(30, TimeUnit.SECONDS)) {
                throw new BackupException("Hay otra copia o restauración en curso. Inténtelo de nuevo en unos segundos.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BackupException("Operación interrumpida.");
        }
    }

    private static Path prepararCarpeta(Path carpeta) throws BackupException {
        try {
            Path dir = carpeta.toAbsolutePath();
            Files.createDirectories(dir);
            if (!Files.isDirectory(dir) || !Files.isWritable(dir)) {
                throw new BackupException("No se puede escribir en la carpeta de copias: " + dir);
            }
            return dir;
        } catch (IOException | RuntimeException e) {
            throw new BackupException("No se pudo usar la carpeta de copias \"" + carpeta + "\": " + e.getMessage(), e);
        }
    }

    private static Path nombreUnico(Path dir, LocalDateTime momento, Tipo tipo) {
        int n = 1;
        Path p = dir.resolve(CopiaSeguridad.nombreArchivo(momento, tipo, n));
        while (Files.exists(p)) {
            n++;
            p = dir.resolve(CopiaSeguridad.nombreArchivo(momento, tipo, n));
        }
        return p;
    }

    private static Connection abrirSoloLectura(Path archivo) throws SQLException {
        SQLiteConfig cfg = new SQLiteConfig();
        cfg.setReadOnly(true);
        return DriverManager.getConnection("jdbc:sqlite:" + archivo.toAbsolutePath(), cfg.toProperties());
    }

    private static String literal(Path ruta) {
        return ruta.toAbsolutePath().toString().replace("'", "''");
    }

    /** Registro de operaciones en data/backup.log (sobrevive a una restauracion, a diferencia de la bitacora). */
    static synchronized void log(String operacion, String resultado, String detalle) {
        try {
            Files.createDirectories(ARCHIVO_LOG.getParent());
            if (Files.exists(ARCHIVO_LOG) && Files.size(ARCHIVO_LOG) > LOG_MAX_BYTES) {
                Files.move(ARCHIVO_LOG, ARCHIVO_LOG.resolveSibling("backup.log.old"), StandardCopyOption.REPLACE_EXISTING);
            }
            String linea = FORMATO_LOG.format(LocalDateTime.now()) + " | " + operacion + " | " + resultado + " | "
                    + (detalle == null ? "" : detalle.replace('\n', ' ')) + System.lineSeparator();
            Files.writeString(ARCHIVO_LOG, linea, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignorado) {
            // el log es informativo; nunca debe romper una copia
        }
    }
}
