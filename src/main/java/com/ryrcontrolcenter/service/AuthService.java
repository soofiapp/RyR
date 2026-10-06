/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ryrcontrolcenter.service;

import com.ryrcontrolcenter.dao.UsuarioDao;
import com.ryrcontrolcenter.modelo.Usuario;
import com.ryrcontrolcenter.util.PasswordUtil;
import com.ryrcontrolcenter.util.SesionActual;

public class AuthService {

    private final UsuarioDao usuarioDao = new UsuarioDao();

    public Usuario iniciarSesion(String usuarioLogin, String passwordPlano) {
        Usuario u = usuarioDao.buscarPorLogin(usuarioLogin);
        if (u == null) {
            return null;
        }
        if (PasswordUtil.verificarPassword(passwordPlano,u.getPasswordHash())) {
            usuarioDao.actualizarUltimoAcceso(u.getIdUsuario());
            SesionActual.iniciar(u);
            return u;
        }
        return null;
    }

    public boolean cambiarPassword(int idUsuario,String passwordActual,String nuevaPassword) {
        Usuario u = usuarioDao.buscarPorId(idUsuario);
        if (u == null) {
            return false;
        }
        if (!PasswordUtil.verificarPassword(passwordActual,u.getPasswordHash())) {
            return false;
        }
        String nuevoHash = PasswordUtil.generarHash(nuevaPassword);
        return usuarioDao.actualizarPassword(idUsuario,nuevoHash);
    }
    
    public void cerrarSesion() {
        SesionActual.cerrar();
    }
}
