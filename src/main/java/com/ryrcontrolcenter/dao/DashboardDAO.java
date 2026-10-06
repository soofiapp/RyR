/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ryrcontrolcenter.dao;

import com.ryrcontrolcenter.config.ConexionBD;
import com.ryrcontrolcenter.modelo.DashboardResumen;
 
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
/**
 *
 * @author Lauren★
 */
public class DashboardDAO {
 
    private static final String TIPOS_HERRAMIENTA = "('Herramienta','Kit Agrupado')";
 
    public DashboardResumen obtenerResumen() {
        DashboardResumen r = new DashboardResumen();
 
        try (Connection c = ConexionBD.conectar()) {
 
            r.setMaquinas(escalar(c, "SELECT COUNT(*) FROM maquinas"));
 
            r.setPrestamos(escalar(c, "SELECT COUNT(*) FROM prestamos"));
 
            // 5 martillos = 5 ; un kit = 1
            int herramientas = escalar(c,
                "SELECT COUNT(*) FROM inventario WHERE tipo_activo IN " + TIPOS_HERRAMIENTA);
            r.setHerramientas(herramientas);
 
            r.setEstados(escalar(c, "SELECT COUNT(DISTINCT estado_sst) FROM inventario"));
 
            // Ocupación = herramientas/kits prestados ahora / total herramientas/kits
            int prestadas = escalar(c,
                "SELECT COUNT(DISTINCT p.id_activo) FROM prestamos p " +
                "JOIN inventario i ON i.id_activo = p.id_activo " +
                "WHERE p.estado IN ('En Uso','Atrasado') " +
                "AND i.tipo_activo IN " + TIPOS_HERRAMIENTA);
            r.setOcupacion(herramientas == 0 ? 0 : Math.min(100.0, prestadas * 100.0 / herramientas));
 
            r.setDevoluciones(escalar(c, "SELECT COUNT(*) FROM prestamos WHERE estado = 'Devuelto'"));
 
            r.setFiltros(escalar(c, "SELECT COUNT(*) FROM filtros"));
 
            // La tabla prestamos no tiene campo de aprobación: por ahora cuenta todos.
            r.setAprobados(escalar(c, "SELECT COUNT(*) FROM prestamos"));
 
            // Gráfica de barras: activos por estado SST
            llenarMapa(c,
                "SELECT estado_sst, COUNT(*) FROM inventario GROUP BY estado_sst",
                r.getActivosPorEstado());
 
            // Gráfica de línea: préstamos por mes
            llenarMapa(c,
                "SELECT strftime('%Y-%m', fecha_salida) AS mes, COUNT(*) " +
                "FROM prestamos GROUP BY mes ORDER BY mes",
                r.getPrestamosPorMes());
 
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return r;
    }
 
    private int escalar(Connection c, String sql) throws SQLException {
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }
 
    private void llenarMapa(Connection c, String sql, Map<String, Integer> destino) throws SQLException {
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                String clave = rs.getString(1);
                destino.put(clave == null ? "Sin fecha" : clave, rs.getInt(2));
            }
        }
    }
}
