package com.ryrcontrolcenter.util;

import com.ryrcontrolcenter.modelo.Usuario;

public class SesionActual {

    private static Usuario usuarioActual;

    public static void iniciar(Usuario usuario) {
        usuarioActual = usuario;
    }

    public static void cerrar() {
        usuarioActual = null;
    }

    public static Usuario getUsuario() {
        return usuarioActual;
    }

    public static boolean puedeEditar() {
        return usuarioActual != null && "Auxiliar".equals(usuarioActual.getRol());
    }
}
