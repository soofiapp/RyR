
package com.ryrcontrolcenter.modelo;

public class Filtros {
    private String idFiltro;
    private String descripcion;
    private String categoriaFiltro;
    private String unidadMedida;
    private int stockActual;
    private int puntoReorden;
    private String fechaActualizacion;

    public Filtros() {
    }

    public String getIdFiltro() {
        return idFiltro;
    }
    public void setIdFiltro(String idFiltro) {
        this.idFiltro = idFiltro;
    }

    public String getDescripcion() {
        return descripcion;
    }
    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getCategoriaFiltro() {
        return categoriaFiltro;
    }
    public void setCategoriaFiltro(String categoriaFiltro) {
        this.categoriaFiltro = categoriaFiltro;
    }

    public String getUnidadMedida() {
        return unidadMedida;
    }
    public void setUnidadMedida(String unidadMedida) {
        this.unidadMedida = unidadMedida;
    }

    public int getStockActual() {
        return stockActual;
    }
    public void setStockActual(int stockActual) {
        this.stockActual = stockActual;
    }

    public int getPuntoReorden() {
        return puntoReorden;
    }
    public void setPuntoReorden(int puntoReorden) {
        this.puntoReorden = puntoReorden;
    }

    public String getFechaActualizacion() {
        return fechaActualizacion;
    }
    public void setFechaActualizacion(String fechaActualizacion) {
        this.fechaActualizacion = fechaActualizacion;
    }
}
