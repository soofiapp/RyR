package com.ryrcontrolcenter.dao;

import com.ryrcontrolcenter.config.ConexionBD;
import com.ryrcontrolcenter.modelo.Prestamos;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PrestamoDao {

    public List<Prestamos> listarTodos() throws SQLException {
        String sql = "SELECT * FROM prestamos ORDER BY id_prestamo";
        List<Prestamos> lista = new ArrayList<>();
        try (Connection conn = ConexionBD.conectar(); PreparedStatement st = conn.prepareStatement(sql); ResultSet rs = st.executeQuery()) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    private Prestamos mapear(ResultSet rs) throws SQLException {
        Prestamos p = new Prestamos();
        p.setIdPrestamo(rs.getString("id_prestamo"));
        p.setIdActivo(rs.getString("id_activo"));
        p.setOperarioNombre(rs.getString("operario_nombre"));
        p.setOperarioCedula(rs.getString("operario_cedula"));
        p.setUbicacionFrente(rs.getString("ubicacion_frente"));
        p.setFechaSalida(rs.getString("fecha_salida"));
        p.setFechaDevolucionEstimada(rs.getString("fecha_devolucion_estimada"));
        p.setEstado(rs.getString("estado"));
        p.setObservacionesSalida(rs.getString("observaciones_salida"));
        p.setDescripcionEstadoDevolucion(rs.getString("descripcion_estado_devolucion"));
        p.setIdUusarioRegistro(rs.getInt("id_usuario_registro"));
        return p;
    }
}
