/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ryrcontrolcenter.dao;

import com.ryrcontrolcenter.config.ConexionBD;
import com.ryrcontrolcenter.modelo.KitDetalle;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class KitDetalleDao {

    public void insertar(KitDetalle k) {
        String sql = "INSERT INTO kit_detalle (id_kit, id_herramienta, cantidad) VALUES (?,?,?)";
        try (Connection conn = ConexionBD.conectar(); PreparedStatement st = conn.prepareStatement(sql)) {
            st.setString(1, k.getIdKit());
            st.setString(2, k.getIdHerramienta());
            st.setInt(3, k.getCantidad());
            st.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void eliminar(int idDetalle) {
        String sql = "DELETE FROM kit_detalle WHERE id_detalle = ?";
        try (Connection conn = ConexionBD.conectar(); PreparedStatement st = conn.prepareStatement(sql)) {
            st.setInt(1, idDetalle);
            st.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Trae los componentes de un kit, ya con la descripcion y el estado_sst de
     * cada herramienta (via JOIN con inventario), para no tener que consultar
     * InventarioDAO por separado en cada componente.
     */
    public List<KitDetalle> listarPorKit(String idKit) {
        List<KitDetalle> lista = new ArrayList<>();
        String sql = """
            SELECT kd.id_detalle, kd.id_kit, kd.id_herramienta, kd.cantidad,
                   i.descripcion AS descripcion_herramienta,
                   i.estado_sst  AS estado_sst_herramienta
            FROM kit_detalle kd
            JOIN inventario i ON kd.id_herramienta = i.id_activo
            WHERE kd.id_kit = ?
            """;
        try (Connection conn = ConexionBD.conectar(); PreparedStatement st = conn.prepareStatement(sql)) {
            st.setString(1, idKit);
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    /**
     * Revisa si algun componente del kit esta bloqueado o en mantenimiento. Se
     * usa antes de autorizar el prestamo de un kit completo.
     */
    public boolean tieneComponenteNoOperativo(String idKit) {
        String sql = """
            SELECT COUNT(*) FROM kit_detalle kd
            JOIN inventario i ON kd.id_herramienta = i.id_activo
            WHERE kd.id_kit = ? AND i.estado_sst != 'Operativa'
            """;
        try (Connection conn = ConexionBD.conectar(); PreparedStatement st = conn.prepareStatement(sql)) {
            st.setString(1, idKit);
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private KitDetalle mapear(ResultSet rs) throws SQLException {
        KitDetalle k = new KitDetalle();
        k.setIdDetalle(rs.getInt("id_detalle"));
        k.setIdKit(rs.getString("id_kit"));
        k.setIdHerramienta(rs.getString("id_herramienta"));
        k.setCantidad(rs.getInt("cantidad"));
        k.setDescripcionHerramienta(rs.getString("descripcion_herramienta"));
        k.setEstadoSstHerramienta(rs.getString("estado_sst_herramienta"));
        return k;
    }
}
