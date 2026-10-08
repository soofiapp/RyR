package com.ryrcontrolcenter.util;

import javafx.fxml.FXMLLoader;

public final class Precarga {

    private static final String UI = "/com/ryrcontrolcenter/ui/";
    private static final String[] VISTAS = {
        "DashboardVista", "KardexVista", "PrestamosVista",
        "InventariadoVista", "MatrizFiltroVista", "UsuariosBitacoraVista"
    };

    private static boolean iniciada = false;

    private Precarga() {
    }

    public static synchronized void iniciar() {
        if (iniciada) {
            return;
        }
        iniciada = true;
        Thread hilo = new Thread(() -> {
            for (String vista : VISTAS) {
                try {
                    FXMLLoader.load(Precarga.class.getResource(UI + vista + ".fxml"));
                } catch (Throwable e) {
                    System.err.println("Precarga: " + vista + " omitida (" + e + ")");
                }
            }
        }, "precarga-vistas");
        hilo.setDaemon(true);
        hilo.setPriority(Thread.MIN_PRIORITY);
        hilo.start();
    }
}
