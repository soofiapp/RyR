
package com.ryrcontrolcenter.modelo;

public class Maquinas{
    private String idMaquina;
    private String marca;
    private String modelo;
    private String numero_serie;
    private Double horometro;
    private String areaDepartamento;

    public Maquinas() {
    }

    public String getIdMaquina() {
        return idMaquina;
    }
    public void setIdMaquina(String id_maquina) {
        this.idMaquina = id_maquina;
    }

    public String getMarca() {
        return marca;
    }
    public void setMarca(String marca) {
        this.marca = marca;
    }

    public String getModelo() {
        return modelo;
    }
    public void setModelo(String modelo) {
        this.modelo = modelo;
    }

    public String getNumero_serie() {
        return numero_serie;
    }
    public void setNumero_serie(String numero_serie) {
        this.numero_serie = numero_serie;
    }

    public Double getHorometro() {
        return horometro;
    }
    public void setHorometro(Double horometro) {
        this.horometro = horometro;
    }

    public String getAreaDepartamento() {
        return areaDepartamento;
    }
    public void setAreaDepartamento(String area_departamento) {
        this.areaDepartamento = area_departamento;
    }
}
