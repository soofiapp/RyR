
package com.ryrcontrolcenter.modelo;

public class KitDetalle {
    private int idDetalle;
    private String idKit;
    private String idHerramienta;
    private int cantidad;
    private String descripcionHerramienta;
    private String estadoSstHerramienta;

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

    public String getIdHerramienta() {
        return idHerramienta;
    }
    public void setIdHerramienta(String idHerramienta) {
        this.idHerramienta = idHerramienta;
    }

    public int getCantidad() {
        return cantidad;
    }
    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    public String getDescripcionHerramienta() {
        return descripcionHerramienta;
    }
    public void setDescripcionHerramienta(String descripcionHerramienta) {
        this.descripcionHerramienta = descripcionHerramienta;
    }

    public String getEstadoSstHerramienta() {
        return estadoSstHerramienta;
    }
    public void setEstadoSstHerramienta(String estadoSstHerramienta) {
        this.estadoSstHerramienta = estadoSstHerramienta;
    }
}
