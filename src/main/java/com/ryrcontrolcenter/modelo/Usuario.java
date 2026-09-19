
package com.ryrcontrolcenter.modelo;

public class Usuario {
    private int idUsuario;
    private String nombreCompleto;
    private String usuarioLogin;
    private String passwordHash;
    private String rol;
    private String frentePlanta;
    private String ultimoAcceso;
    private boolean activo;

    public Usuario() {
    }

    public int getIdUsuario() {
        return idUsuario;
    }
    public void setIdUsuario(int idUsuario) {
        this.idUsuario = idUsuario;
    }
    
    public String getNombreCompleto() {
        return nombreCompleto;
    }
    public void setNombreCompleto(String nombreCompleto) {
        this.nombreCompleto = nombreCompleto;
    }

    public String getUsuarioLogin() {
        return usuarioLogin;
    }
    public void setUsuarioLogin(String usuarioLogin) {
        this.usuarioLogin = usuarioLogin;
    }

    public String getPasswordHash() {
        return passwordHash;
    }
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getRol() {
        return rol;
    }
    public void setRol(String rol) {
        this.rol = rol;
    }

    public String getFrentePlanta() {
        return frentePlanta;
    }
    public void setFrentePlanta(String frentePlanta) {
        this.frentePlanta = frentePlanta;
    }

    public String getUltimoAcceso() {
        return ultimoAcceso;
    }
    public void setUltimoAcceso(String ultimoAcceso) {
        this.ultimoAcceso = ultimoAcceso;
    }

    public boolean isActivo() {
        return activo;
    }
    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    
    
    
}
