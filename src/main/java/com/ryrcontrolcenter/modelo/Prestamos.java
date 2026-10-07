package com.ryrcontrolcenter.modelo;

public class Prestamos {

    private String idPrestamo;
    private String operarioNombre;
    private String operarioCedula;
    private String ubicacionFrente;
    private String fechaSalida;
    private String fechaDevolucionEstimada;
    private String fechaDevolucionReal;
    private String estado;
    private String observacionesSalida;
    private String descripcionEstadoDevolucion;
    private String idActivo;
    private int idUusarioRegistro;
    private int cantidad;

    // Constructor vacío
    public Prestamos() {
    }

    // Constructor completo (para cuando leemos de la BD)
    public Prestamos(String idPrestamo, String operarioNombre, String operarioCedula,
            String ubicacionFrente, String fechaSalida, String fechaDevolucionEstimada,
            String fechaDevolucionReal, String estado, String observacionesSalida,
            String descripcionEstadoDevolucion, String idActivo, int idUusarioRegistro) {
        this.idPrestamo = idPrestamo;
        this.operarioNombre = operarioNombre;
        this.operarioCedula = operarioCedula;
        this.ubicacionFrente = ubicacionFrente;
        this.fechaSalida = fechaSalida;
        this.fechaDevolucionEstimada = fechaDevolucionEstimada;
        this.fechaDevolucionReal = fechaDevolucionReal;
        this.estado = estado;
        this.observacionesSalida = observacionesSalida;
        this.descripcionEstadoDevolucion = descripcionEstadoDevolucion;
        this.idActivo = idActivo;
        this.idUusarioRegistro = idUusarioRegistro;
    }

    // Getters y Setters
    public String getIdPrestamo() {
        return idPrestamo;
    }

    public void setIdPrestamo(String idPrestamo) {
        this.idPrestamo = idPrestamo;
    }

    public String getOperarioNombre() {
        return operarioNombre;
    }

    public void setOperarioNombre(String operarioNombre) {
        this.operarioNombre = operarioNombre;
    }

    public String getOperarioCedula() {
        return operarioCedula;
    }

    public void setOperarioCedula(String operarioCedula) {
        this.operarioCedula = operarioCedula;
    }

    public String getUbicacionFrente() {
        return ubicacionFrente;
    }

    public void setUbicacionFrente(String ubicacionFrente) {
        this.ubicacionFrente = ubicacionFrente;
    }

    public String getFechaSalida() {
        return fechaSalida;
    }

    public void setFechaSalida(String fechaSalida) {
        this.fechaSalida = fechaSalida;
    }

    public String getFechaDevolucionEstimada() {
        return fechaDevolucionEstimada;
    }

    public void setFechaDevolucionEstimada(String fechaDevolucionEstimada) {
        this.fechaDevolucionEstimada = fechaDevolucionEstimada;
    }

    public String getFechaDevolucionReal() {
        return fechaDevolucionReal;
    }

    public void setFechaDevolucionReal(String fechaDevolucionReal) {
        this.fechaDevolucionReal = fechaDevolucionReal;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getObservacionesSalida() {
        return observacionesSalida;
    }

    public void setObservacionesSalida(String observacionesSalida) {
        this.observacionesSalida = observacionesSalida;
    }

    public String getDescripcionEstadoDevolucion() {
        return descripcionEstadoDevolucion;
    }

    public void setDescripcionEstadoDevolucion(String descripcionEstadoDevolucion) {
        this.descripcionEstadoDevolucion = descripcionEstadoDevolucion;
    }

    public String getIdActivo() {
        return idActivo;
    }

    public void setIdActivo(String idActivo) {
        this.idActivo = idActivo;
    }

    public int getIdUusarioRegistro() {
        return idUusarioRegistro;
    }

    public void setIdUusarioRegistro(int idUusarioRegistro) {
        this.idUusarioRegistro = idUusarioRegistro;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }
}
