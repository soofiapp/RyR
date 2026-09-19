
package com.ryrcontrolcenter.modelo;

public class Bitacora {
    private int idBitacora;
    private String fechaHora;
    private String accionRealizada;
    private String moduloAfectado;
    private String referenciaId;
    private int idUsuario;

    public Bitacora() {
    }

    public int getIdBitacora() {
        return idBitacora;
    }
    public void setIdBitacora(int idBitacora) {
        this.idBitacora = idBitacora;
    }

    public String getFechaHora() {
        return fechaHora;
    }
    public void setFechaHora(String fechaHora) {
        this.fechaHora = fechaHora;
    }

    public String getAccionRealizada() {
        return accionRealizada;
    }
    public void setAccionRealizada(String accionRealizada) {
        this.accionRealizada = accionRealizada;
    }

    public String getModuloAfectado() {
        return moduloAfectado;
    }
    public void setModuloAfectado(String moduloAfectado) {
        this.moduloAfectado = moduloAfectado;
    }

    public String getReferenciaId() {
        return referenciaId;
    }
   public void setReferenciaId(String referenciaId) {
        this.referenciaId = referenciaId;
    }

    public int getIdUsuario() {
        return idUsuario;
    }
    public void setIdUsuario(int idUsuario) {
        this.idUsuario = idUsuario;
    }   
}
