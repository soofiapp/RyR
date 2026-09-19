
package com.ryrcontrolcenter.modelo;

public class MovimientoKardex {
    private int idMovimiento;
    private String fechaHora;
    private String tipooMovimiento;
    private int cantidad;
    private int saldoResultante;
    private String idFiltro;
    private int idUsuario;

    public MovimientoKardex() {
    }

    public int getIdMovimiento() {
        return idMovimiento;
    }
    public void setIdMovimiento(int idMovimiento) {
        this.idMovimiento = idMovimiento;
    }

    public String getFechaHora() {
        return fechaHora;
    }
    public void setFechaHora(String fechaHora) {
        this.fechaHora = fechaHora;
    }

    public String getTipooMovimiento() {
        return tipooMovimiento;
    }
    public void setTipooMovimiento(String tipooMovimiento) {
        this.tipooMovimiento = tipooMovimiento;
    }

    public int getCantidad() {
        return cantidad;
    }
    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    public int getSaldoResultante() {
        return saldoResultante;
    }
    public void setSaldoResultante(int saldoResultante) {
        this.saldoResultante = saldoResultante;
    }

    public String getIdFiltro() {
        return idFiltro;
    }
    public void setIdFiltro(String idFiltro) {
        this.idFiltro = idFiltro;
    }

    public int getIdUsuario() {
        return idUsuario;
    }
    public void setIdUsuario(int idUsuario) {
        this.idUsuario = idUsuario;
    }
}
