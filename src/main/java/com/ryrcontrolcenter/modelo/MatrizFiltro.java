
package com.ryrcontrolcenter.modelo;

public class MatrizFiltro {
    private int idMatriz;
    private String sistemaTipoFiltro;
    private String codigoOem;
    private String idMaquina;
    private String idFiltro;
    private String descripcionFiltro;
    private int stockActual;

    public MatrizFiltro() {
    }

    public int getIdMatriz() { return idMatriz; }
    public void setIdMatriz(int idMatriz) { this.idMatriz = idMatriz; }

    public String getSistemaTipoFiltro() { return sistemaTipoFiltro; }
    public void setSistemaTipoFiltro(String sistemaTipoFiltro) { this.sistemaTipoFiltro = sistemaTipoFiltro; }

    public String getCodigoOem() { return codigoOem; }
    public void setCodigoOem(String codigoOem) { this.codigoOem = codigoOem; }

    public String getIdMaquina() { return idMaquina; }
    public void setIdMaquina(String idMaquina) { this.idMaquina = idMaquina; }

    public String getIdFiltro() { return idFiltro; }
    public void setIdFiltro(String idFiltro) { this.idFiltro = idFiltro; }

    public String getDescripcionFiltro() { return descripcionFiltro; }
    public void setDescripcionFiltro(String descripcionFiltro) { this.descripcionFiltro = descripcionFiltro; }

    public int getStockActual() { return stockActual; }
    public void setStockActual(int stockActual) { this.stockActual = stockActual; }

    public String getReferenciaStock() {
        if (idFiltro == null) return "Sin asignar";
        return idFiltro + " (Stock: " + stockActual + ")";
    }
}
