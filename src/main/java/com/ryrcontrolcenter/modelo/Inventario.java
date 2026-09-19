
package com.ryrcontrolcenter.modelo;

public class Inventario {
    private String idActivo;
    private String descripcion;
    private String tipoActivo;
    private String estadoSst;
    private String ubicacion;
    private String fechaRegistro;
    private String observaciones;

    public Inventario() {
    }

    public String getIdActivo() {
        return idActivo;
    }
    public void setIdActivo(String idActivo) {
        this.idActivo = idActivo;
    }

    public String getDescripcion() {
        return descripcion;
    }
    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getTipoActivo() {
        return tipoActivo;
    }
    public void setTipoActivo(String tipoActivo) {
        this.tipoActivo = tipoActivo;
    }

    public String getEstadoSst() {
        return estadoSst;
    }
    public void setEstadoSst(String estadoSst) {
        this.estadoSst = estadoSst;
    }

    public String getUbicacion() {
        return ubicacion;
    }
    public void setUbicacion(String ubicacion) {
        this.ubicacion = ubicacion;
    }

    public String getFechaRegistro() {
        return fechaRegistro;
    }
    public void setFechaRegistro(String fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public String getObservaciones() {
        return observaciones;
    }
    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }
}
