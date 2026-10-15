package com.ryrcontrolcenter.config;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Properties;


public final class BackupConfig {

    public enum Unidad {
        MINUTOS("Minutos", 1),
        HORAS("Horas", 60),
        DIAS("Días", 1440);

        private final String etiqueta;
        private final long minutos;

        Unidad(String etiqueta, long minutos) {
            this.etiqueta = etiqueta;
            this.minutos = minutos;
        }

        public long getMinutos() {
            return minutos;
        }

        @Override
        public String toString() {
            return etiqueta;
        }
    }

    public static final Path CARPETA_DEFECTO = Path.of("backups");
    public static final int MINIMO_MINUTOS = 5;
    public static final int RETENER_MIN = 1;
    public static final int RETENER_MAX = 500;

    private static final Path ARCHIVO = Path.of("data", "backup.properties");
    private static final Object CANDADO = new Object();

    private boolean automatica = false;
    private int intervaloValor = 1;
    private Unidad intervaloUnidad = Unidad.DIAS;
    private Path carpeta = CARPETA_DEFECTO;
    private int retener = 10;
    private boolean copiaAlCerrar = false;
    private LocalDateTime ultimaAutomatica;
    private LocalDateTime ultimoIntentoFallido;

    private BackupConfig() {
    }


    public static BackupConfig cargar() {
        synchronized (CANDADO) {
            BackupConfig c = new BackupConfig();
            if (!Files.isRegularFile(ARCHIVO)) {
                return c;
            }
            Properties p = new Properties();
            try (Reader r = Files.newBufferedReader(ARCHIVO, StandardCharsets.UTF_8)) {
                p.load(r);
            } catch (IOException | IllegalArgumentException e) {
                return c; 
            }
            c.automatica = Boolean.parseBoolean(p.getProperty("automatica", "false"));
            c.intervaloValor = entero(p.getProperty("intervaloValor"), 1, 1, 9999);
            try {
                c.intervaloUnidad = Unidad.valueOf(p.getProperty("intervaloUnidad", "DIAS"));
            } catch (IllegalArgumentException e) {
                c.intervaloUnidad = Unidad.DIAS;
            }
            String carpeta = p.getProperty("carpeta", "").trim();
            if (!carpeta.isEmpty()) {
                try {
                    c.carpeta = Path.of(carpeta);
                } catch (RuntimeException e) {
                    c.carpeta = CARPETA_DEFECTO;
                }
            }
            c.retener = entero(p.getProperty("retener"), 10, RETENER_MIN, RETENER_MAX);
            c.copiaAlCerrar = Boolean.parseBoolean(p.getProperty("copiaAlCerrar", "false"));
            c.ultimaAutomatica = fecha(p.getProperty("ultimaAutomatica"));
            c.ultimoIntentoFallido = fecha(p.getProperty("ultimoIntentoFallido"));
            return c;
        }
    }

    public void guardar() throws IOException {
        synchronized (CANDADO) {
            guardarInterno();
        }
    }

    private void guardarInterno() throws IOException {
        Properties p = new Properties();
        p.setProperty("automatica", Boolean.toString(automatica));
        p.setProperty("intervaloValor", Integer.toString(intervaloValor));
        p.setProperty("intervaloUnidad", intervaloUnidad.name());
        p.setProperty("carpeta", carpeta.toString());
        p.setProperty("retener", Integer.toString(retener));
        p.setProperty("copiaAlCerrar", Boolean.toString(copiaAlCerrar));
        if (ultimaAutomatica != null) {
            p.setProperty("ultimaAutomatica", ultimaAutomatica.toString());
        }
        if (ultimoIntentoFallido != null) {
            p.setProperty("ultimoIntentoFallido", ultimoIntentoFallido.toString());
        }
        Files.createDirectories(ARCHIVO.getParent());
        Path tmp = ARCHIVO.resolveSibling(ARCHIVO.getFileName() + ".tmp");
        try (Writer w = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
            p.store(w, "RyR ControlCenter - configuracion de copias de seguridad");
        }
        Files.move(tmp, ARCHIVO, StandardCopyOption.REPLACE_EXISTING);
    }

    public static void registrarAutomatica(LocalDateTime momento) throws IOException {
        synchronized (CANDADO) {
            BackupConfig c = cargar();
            c.ultimaAutomatica = momento;
            c.ultimoIntentoFallido = null;
            c.guardarInterno();
        }
    }

    public static void registrarFallo(LocalDateTime momento) throws IOException {
        synchronized (CANDADO) {
            BackupConfig c = cargar();
            c.ultimoIntentoFallido = momento;
            c.guardarInterno();
        }
    }


    public Duration getIntervalo() {
        return Duration.ofMinutes((long) intervaloValor * intervaloUnidad.getMinutos());
    }


    public String validar() {
        if (intervaloValor < 1) {
            return "La frecuencia debe ser de al menos 1.";
        }
        if (getIntervalo().toMinutes() < MINIMO_MINUTOS) {
            return "La frecuencia mínima es de " + MINIMO_MINUTOS + " minutos.";
        }
        if (retener < RETENER_MIN || retener > RETENER_MAX) {
            return "Las copias a conservar deben estar entre " + RETENER_MIN + " y " + RETENER_MAX + ".";
        }
        if (carpeta == null || carpeta.toString().isBlank()) {
            return "Seleccione una carpeta de destino.";
        }
        return null;
    }


    public boolean isAutomatica() {
        return automatica;
    }

    public void setAutomatica(boolean automatica) {
        this.automatica = automatica;
    }

    public int getIntervaloValor() {
        return intervaloValor;
    }

    public void setIntervaloValor(int intervaloValor) {
        this.intervaloValor = intervaloValor;
    }

    public Unidad getIntervaloUnidad() {
        return intervaloUnidad;
    }

    public void setIntervaloUnidad(Unidad intervaloUnidad) {
        this.intervaloUnidad = intervaloUnidad;
    }

    public Path getCarpeta() {
        return carpeta;
    }

    public void setCarpeta(Path carpeta) {
        this.carpeta = carpeta;
    }

    public int getRetener() {
        return retener;
    }

    public void setRetener(int retener) {
        this.retener = retener;
    }

    public boolean isCopiaAlCerrar() {
        return copiaAlCerrar;
    }

    public void setCopiaAlCerrar(boolean copiaAlCerrar) {
        this.copiaAlCerrar = copiaAlCerrar;
    }

    public LocalDateTime getUltimaAutomatica() {
        return ultimaAutomatica;
    }

    public LocalDateTime getUltimoIntentoFallido() {
        return ultimoIntentoFallido;
    }


    private static int entero(String texto, int defecto, int min, int max) {
        try {
            int v = Integer.parseInt(texto.trim());
            return Math.max(min, Math.min(max, v));
        } catch (RuntimeException e) {
            return defecto;
        }
    }

    private static LocalDateTime fecha(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(texto.trim());
        } catch (RuntimeException e) {
            return null;
        }
    }
}
