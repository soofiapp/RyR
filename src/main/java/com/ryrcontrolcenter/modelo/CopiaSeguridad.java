package com.ryrcontrolcenter.modelo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CopiaSeguridad {

    public enum Tipo {
        MANUAL("manual", "Manual"),
        AUTOMATICA("auto", "Automática"),
        CIERRE("cierre", "Al cerrar"),
        PRERESTAURACION("prerestauracion", "Previa a restaurar");

        private final String codigo;
        private final String etiqueta;

        Tipo(String codigo, String etiqueta) {
            this.codigo = codigo;
            this.etiqueta = etiqueta;
        }

        public String getCodigo() {
            return codigo;
        }

        public String getEtiqueta() {
            return etiqueta;
        }

        static Tipo deCodigo(String codigo) {
            for (Tipo t : values()) {
                if (t.codigo.equals(codigo)) {
                    return t;
                }
            }
            return null;
        }
    }

    public static final DateTimeFormatter FORMATO_NOMBRE = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final DateTimeFormatter FORMATO_VISTA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final Pattern PATRON = Pattern.compile(
            "RyR_backup_(\\d{8}_\\d{6})_(manual|auto|cierre|prerestauracion)(?:_\\d+)?\\.db");

    private final Path archivo;
    private final LocalDateTime fecha;
    private final Tipo tipo;
    private final long tamano;

    public CopiaSeguridad(Path archivo, LocalDateTime fecha, Tipo tipo, long tamano) {
        this.archivo = archivo;
        this.fecha = fecha;
        this.tipo = tipo;
        this.tamano = tamano;
    }


    public static Optional<CopiaSeguridad> desdeArchivo(Path archivo) {
        Matcher m = PATRON.matcher(archivo.getFileName().toString());
        if (!m.matches()) {
            return Optional.empty();
        }
        try {
            LocalDateTime fecha = LocalDateTime.parse(m.group(1), FORMATO_NOMBRE);
            Tipo tipo = Tipo.deCodigo(m.group(2));
            long tamano = Files.size(archivo);
            return Optional.of(new CopiaSeguridad(archivo, fecha, tipo, tamano));
        } catch (RuntimeException | IOException e) {
            return Optional.empty();
        }
    }

    public static String nombreArchivo(LocalDateTime momento, Tipo tipo, int repeticion) {
        return "RyR_backup_" + FORMATO_NOMBRE.format(momento) + "_" + tipo.getCodigo()
                + (repeticion > 1 ? "_" + repeticion : "") + ".db";
    }

    public static String formatearTamano(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024L * 1024L) {
            return String.format(java.util.Locale.ROOT, "%.1f KB", bytes / 1024.0);
        }
        return String.format(java.util.Locale.ROOT, "%.1f MB", bytes / (1024.0 * 1024.0));
    }

    public Path getArchivo() {
        return archivo;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public Tipo getTipo() {
        return tipo;
    }

    public long getTamano() {
        return tamano;
    }

    public String getFechaTexto() {
        return FORMATO_VISTA.format(fecha);
    }

    public String getTipoTexto() {
        return tipo.getEtiqueta();
    }

    public String getTamanoTexto() {
        return formatearTamano(tamano);
    }

    public String getNombreArchivo() {
        return archivo.getFileName().toString();
    }
}
