package com.ryrcontrolcenter.service;

import com.ryrcontrolcenter.config.BackupConfig;
import com.ryrcontrolcenter.modelo.CopiaSeguridad.Tipo;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Programador de copias automaticas. Corre en segundo plano mientras la
 * aplicacion esta abierta: cada 30 segundos mira si ya toca una copia segun la
 * frecuencia configurada. Si el programa estuvo cerrado y la copia ya estaba
 * vencida, se hace al abrirlo (a los ~15 segundos).
 */
public final class BackupScheduler {

    private static final int REINTENTO_TRAS_FALLO_MIN = 5;
    private static ScheduledExecutorService ejecutor;

    private BackupScheduler() {
    }

    public static synchronized void iniciar() {
        if (ejecutor != null) {
            return;
        }
        ejecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ryr-backup-programador");
            t.setDaemon(true);
            return t;
        });
        ejecutor.scheduleWithFixedDelay(() -> revisar(LocalDateTime.now()), 15, 30, TimeUnit.SECONDS);
    }

    public static synchronized void detener() {
        if (ejecutor != null) {
            ejecutor.shutdownNow();
            ejecutor = null;
        }
    }

    /** Pide una revision inmediata (p. ej. justo despues de activar el modo automatico). */
    public static synchronized void revisarAhora() {
        if (ejecutor != null) {
            ejecutor.execute(() -> revisar(LocalDateTime.now()));
        }
    }

    /** Copia final al cerrar la aplicacion, si el modo automatico y la opcion estan activos. */
    public static void alCerrar() {
        try {
            BackupConfig cfg = BackupConfig.cargar();
            if (cfg.isAutomatica() && cfg.isCopiaAlCerrar()) {
                BackupService.crearCopia(Tipo.CIERRE, cfg.getCarpeta());
                BackupService.aplicarRetencion(cfg.getCarpeta(), cfg.getRetener());
            }
        } catch (Exception e) {
            BackupService.log("CIERRE", "ERROR", String.valueOf(e.getMessage()));
        }
    }

    /** Fecha prevista de la proxima copia automatica; null si el modo automatico esta apagado. */
    public static LocalDateTime proximaCopia(BackupConfig cfg, LocalDateTime ahora) {
        if (!cfg.isAutomatica()) {
            return null;
        }
        LocalDateTime ultima = cfg.getUltimaAutomatica();
        if (ultima == null) {
            return ahora;
        }
        LocalDateTime prevista = ultima.plus(cfg.getIntervalo());
        return prevista.isBefore(ahora) ? ahora : prevista;
    }

    static void revisar(LocalDateTime ahora) {
        try {
            BackupConfig cfg = BackupConfig.cargar();
            if (!cfg.isAutomatica() || cfg.validar() != null) {
                return;
            }
            LocalDateTime ultima = cfg.getUltimaAutomatica();
            if (ultima != null && ahora.isBefore(ultima.plus(cfg.getIntervalo()))) {
                return; // todavia no toca
            }
            LocalDateTime fallo = cfg.getUltimoIntentoFallido();
            if (fallo != null && ahora.isBefore(fallo.plusMinutes(REINTENTO_TRAS_FALLO_MIN))) {
                return; // se reintenta mas tarde, sin insistir cada 30 s
            }
            try {
                BackupService.crearCopia(Tipo.AUTOMATICA, cfg.getCarpeta(), ahora);
                BackupConfig.registrarAutomatica(ahora);
                BackupService.aplicarRetencion(cfg.getCarpeta(), cfg.getRetener());
            } catch (BackupException e) {
                BackupConfig.registrarFallo(ahora);
            }
        } catch (IOException | RuntimeException e) {
            BackupService.log("AUTO", "ERROR", String.valueOf(e.getMessage()));
        }
    }
}
