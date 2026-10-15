package com.ryrcontrolcenter.service;

/** Error de copia/restauracion con un mensaje pensado para mostrarse al usuario. */
public class BackupException extends Exception {

    public BackupException(String mensaje) {
        super(mensaje);
    }

    public BackupException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
