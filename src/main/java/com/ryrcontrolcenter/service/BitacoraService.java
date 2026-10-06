
package com.ryrcontrolcenter.service;

import com.ryrcontrolcenter.dao.BitacoraDAO;
import com.ryrcontrolcenter.util.SesionActual;


public class BitacoraService {

    private static final BitacoraDAO bitacoraDao = new BitacoraDAO();

    public static void registrar(String accion, String modulo, String referenciaId) {
        var usuario = SesionActual.getUsuario();
        if (usuario == null) {
            return; 
        }
        bitacoraDao.registrar(usuario.getIdUsuario(), accion, modulo, referenciaId);
    }
}
