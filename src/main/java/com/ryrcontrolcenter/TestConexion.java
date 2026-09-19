/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ryrcontrolcenter;

import com.ryrcontrolcenter.modelo.Usuario;
import com.ryrcontrolcenter.service.AuthService;

public class TestConexion {
        public static void main(String[] args) {
        AuthService auth = new AuthService();
        Usuario u = auth.iniciarSesion("natalia", "hash122");

        if (u != null) {
            System.out.println("Login exitoso: " + u.getNombreCompleto() + " - Rol: " + u.getRol());
        } else {
            System.out.println("Usuario o contraseña incorrectos");
        }
    }
}
