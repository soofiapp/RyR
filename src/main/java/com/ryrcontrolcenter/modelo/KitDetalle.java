
package com.ryrcontrolcenter.modelo;

public class KitDetalle {
    private int idDetalle;
    private String idKit;
    private String descripcionComponente;        
    private String numeroSerieComponente;
    private int cantidad;

    public KitDetalle() {
    }

    public int getIdDetalle() {
        return idDetalle;
    }
    public void setIdDetalle(int idDetalle) {
        this.idDetalle = idDetalle;
    }

    public String getIdKit() {
        return idKit;
    }
    public void setIdKit(String idKit) {
        this.idKit = idKit;
    }

    public String getDescripcionComponente() {
        return descripcionComponente;
    }
    public void setDescripcionComponente(String descripcionComponente) {
        this.descripcionComponente = descripcionComponente;
    }

    public String getNumeroSerieComponente() {
        return numeroSerieComponente;
    }
    public void setNumeroSerieComponente(String numeroSerieComponente) {
        this.numeroSerieComponente = numeroSerieComponente;
    }

    public int getCantidad() {
        return cantidad;
    }
    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }      
}
