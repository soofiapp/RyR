package com.ryrcontrolcenter.modelo;

import java.util.ArrayList;
import java.util.List;


public class DashboardResumen {


    public int maquinas;
    public int herramientas;
    public int kits;
    public int prestamosActivos;
    public int prestamosAtrasados;
    public int devoluciones;
    public int filtros;
    public int bloqueadosMantenimiento;
    public int aprobados;


    public int prestables;
    public int prestados;


    public int semRojo;
    public int semAmarillo;
    public int semVerde;

    public List<Filtros> filtrosPorCriticidad = new ArrayList<>();
    
    public List<String> meses = new ArrayList<>();
    public List<Integer> entradasMes = new ArrayList<>();
    public List<Integer> salidasMes = new ArrayList<>();
    public List<Integer> prestamosMes = new ArrayList<>();

    public int enBodega() {
        return Math.max(0, prestables - prestados);
    }

    public int ocupacionPorcentaje() {
        return prestables == 0 ? 0 : (int) Math.round(prestados * 100.0 / prestables);
    }
}
