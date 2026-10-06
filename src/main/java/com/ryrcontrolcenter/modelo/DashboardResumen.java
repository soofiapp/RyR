/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ryrcontrolcenter.modelo;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 * @author Lauren★
 */
public class DashboardResumen {
    private int maquinas;
    private int prestamosAprobados;
     private int prestamosTotales;
    private int herramientas;   
    private int estados;
    private double ocupacion;   
    private int devoluciones;
    private int filtros;
    private int aprobados;
 
    // Datos para las gráficas
    private Map<String, Integer> activosPorEstado = new LinkedHashMap<>(); // barras
    private Map<String, Integer> prestamosPorMes = new LinkedHashMap<>();  // línea
 
    public int getMaquinas() { return maquinas; }
    public void setMaquinas(int v) { this.maquinas = v; }
    public int getPrestamos() { return prestamosAprobados; }
    public void setPrestamos(int v) { this.prestamosAprobados = v; }
    public int getHerramientas() { return herramientas; }
    public void setHerramientas(int v) { this.herramientas = v; }
    public int getEstados() { return estados; }
    public void setEstados(int v) { this.estados = v; }
    public double getOcupacion() { return ocupacion; }
    public void setOcupacion(double v) { this.ocupacion = v; }
    public int getDevoluciones() { return devoluciones; }
    public void setDevoluciones(int v) { this.devoluciones = v; }
    public int getFiltros() { return filtros; }
    public void setFiltros(int v) { this.filtros = v; }
    public int getAprobados() { return aprobados; }
    public void setAprobados(int v) { this.aprobados = v; }
    public Map<String, Integer> getActivosPorEstado() { return activosPorEstado; }
    public Map<String, Integer> getPrestamosPorMes() { return prestamosPorMes; }
}
