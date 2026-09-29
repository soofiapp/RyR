package com.ryrcontrolcenter.dao;

import com.ryrcontrolcenter.config.ConexionBD;
import com.ryrcontrolcenter.modelo.Bitacora;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BitacoraDao {

    public boolean registrar(int idUsuario, String accion, String modulo, String referenciaId) {
        String sql = "INSERT INTO bitacora (id_usuario, accion_realizada, modulo_afectado, referencia_id) VALUES (?,?,?,?)";
        try (Connection conn = ConexionBD.conectar(); PreparedStatement st = conn.prepareStatement(sql)) {
            st.setInt(1, idUsuario);
            st.setString(2, accion);
            st.setString(3, modulo);
            st.setString(4, referenciaId);
            return st.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<Bitacora> listarTodas() {
        List<Bitacora> lista = new ArrayList<>();
        String sql = """
            SELECT b.id_bitacora, b.fecha_hora, b.id_usuario, b.accion_realizada, b.modulo_afectado, b.referencia_id,
                   u.nombre_completo AS nombre_usuario
            FROM bitacora b
            JOIN usuarios u ON b.id_usuario = u.id_usuario
            ORDER BY b.fecha_hora DESC
            """;
        try (Connection conn = ConexionBD.conectar(); PreparedStatement st = conn.prepareStatement(sql); ResultSet rs = st.executeQuery()) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    private Bitacora mapear(ResultSet rs) throws SQLException {
        Bitacora b = new Bitacora();
        b.setIdBitacora(rs.getInt("id_bitacora"));
        b.setFechaHora(rs.getString("fecha_hora"));
        b.setIdUsuario(rs.getInt("id_usuario"));
        b.setAccionRealizada(rs.getString("accion_realizada"));
        b.setModuloAfectado(rs.getString("modulo_afectado"));
        b.setReferenciaId(rs.getString("referencia_id"));
        b.setNombreUsuario(rs.getString("nombre_usuario"));
        return b;
    }
}
