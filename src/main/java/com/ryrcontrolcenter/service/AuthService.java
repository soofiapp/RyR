/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ryrcontrolcenter.service;

import com.ryrcontrolcenter.dao.UsuarioDao;
import com.ryrcontrolcenter.modelo.Usuario;

public class AuthService {
    private final UsuarioDao usuarioDao = new UsuarioDao();

    public Usuario iniciarSesion(String usuarioLogin, String passwordPlano) {
        Usuario u = usuarioDao.buscarPorLogin(usuarioLogin);
        if (u == null) {
            return null; // no existe o esta inactivo
        }
        // NOTA: comparacion simple por ahora. Mas adelante deberiamos
        // usar un hash real (BCrypt) en vez de comparar texto plano.
        if (u.getPasswordHash().equals(passwordPlano)) {
            usuarioDao.actualizarUltimoAcceso(u.getIdUsuario());
            return u;
        }
        return null; // password incorrecto
    }
}