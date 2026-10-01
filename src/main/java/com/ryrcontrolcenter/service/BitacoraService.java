
package com.ryrcontrolcenter.service;

import com.ryrcontrolcenter.dao.BitacoraDao;
import com.ryrcontrolcenter.util.SesionActual;


public class BitacoraService {

    private static final BitacoraDao bitacoraDao = new BitacoraDao();

    public static void registrar(String accion, String modulo, String referenciaId) {
        var usuario = SesionActual.getUsuario();
        if (usuario == null) {
            return; 
        }
        bitacoraDao.registrar(usuario.getIdUsuario(), accion, modulo, referenciaId);
    }
}
