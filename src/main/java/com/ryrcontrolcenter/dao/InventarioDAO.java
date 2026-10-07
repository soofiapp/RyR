package com.ryrcontrolcenter.dao;

import com.ryrcontrolcenter.config.ConexionBD;
import com.ryrcontrolcenter.modelo.Inventario;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class InventarioDAO {

    public boolean agregarItemInventario(Inventario item) {
        String sql = "INSERT INTO inventario "
                + "(id_activo, descripcion, tipo_activo, estado_sst, ubicacion, "
                + "fecha_registro, observaciones, stock_actual, punto_reorden) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = ConexionBD.conectar(); PreparedStatement st = conn.prepareStatement(sql)) {
            st.setString(1, item.getIdActivo());
            st.setString(2, item.getDescripcion());
            st.setString(3, item.getTipoActivo());
            st.setString(4, item.getEstadoSst());
            st.setString(5, item.getUbicacion());
            st.setString(6, item.getFechaRegistro());
            st.setString(7, item.getObservaciones());
            st.setInt(8, item.getStockActual());
            st.setInt(9, item.getPuntoReorden());
            int filasAfectadas = st.executeUpdate();
            return filasAfectadas > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean eliminarItemInventario(String idActivo) {
        String sql = "UPDATE inventario SET visible = 0 WHERE id_activo = ?";
        try (Connection conn = ConexionBD.conectar(); PreparedStatement st = conn.prepareStatement(sql)) {
            st.setString(1, idActivo);
            int filasAfectadas = st.executeUpdate();
            return filasAfectadas > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean actualizarItemInventario(String idActivo, Inventario item) {
        String sql = "UPDATE inventario SET "
                + "descripcion=?, "
                + "tipo_activo=?, "
                + "estado_sst=?, "
                + "ubicacion=?, "
                + "fecha_registro=?, "
                + "observaciones=?, "
                + "stock_actual=?, "
                + "punto_reorden=? "
                + "WHERE id_activo=?";
        try (Connection conn = ConexionBD.conectar(); PreparedStatement st = conn.prepareStatement(sql)) {
            st.setString(1, item.getDescripcion());
            st.setString(2, item.getTipoActivo());
            st.setString(3, item.getEstadoSst());
            st.setString(4, item.getUbicacion());
            st.setString(5, item.getFechaRegistro());
            st.setString(6, item.getObservaciones());
            st.setInt(7, item.getStockActual());
            st.setInt(8, item.getPuntoReorden());
            st.setString(9, idActivo);
            int filasAfectadas = st.executeUpdate();
            return filasAfectadas > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<Inventario> listarTodos() throws SQLException {
        String sql = """
        SELECT i.*,
               (
                   i.stock_actual - COALESCE(
                       (
                           SELECT SUM(p.cantidad)
                           FROM prestamos p
                           WHERE p.id_activo = i.id_activo
                             AND p.estado IN ('En Uso', 'Atrasado')
                       ), 0
                   )
               ) AS stock_disponible
        FROM inventario i
        WHERE i.visible = 1
        ORDER BY i.id_activo
        """;
        List<Inventario> lista = new ArrayList<>();
        try (Connection conn = ConexionBD.conectar(); PreparedStatement st = conn.prepareStatement(sql); ResultSet rs = st.executeQuery()) {

            while (rs.next()) {
                lista.add(mapear(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    /**
     * Activos disponibles para prestamo: Herramienta o Maquinaria (los Kits se
     * prestan por su propio flujo), que ademas no tengan ya un prestamo abierto
     * ni esten bloqueados/en mantenimiento por SST.
     */
    public List<Inventario> listarActivosDisponiblesParaPrestamo() {
        String sql = """
        SELECT *
        FROM (
            SELECT i.*, 
                   (i.stock_actual - COALESCE(
                       (SELECT SUM(p.cantidad)
                        FROM prestamos p
                        WHERE p.id_activo = i.id_activo
                          AND p.estado IN ('En Uso', 'Atrasado')), 0)
                   ) AS stock_disponible
            FROM inventario i
            WHERE i.tipo_activo IN ('Herramienta', 'Maquinaria', 'Kit Agrupado')
              AND i.estado_sst = 'Operativa'
              AND i.visible = 1
        )
        WHERE stock_disponible > 0
        ORDER BY id_activo
        """;
        List<Inventario> lista = new ArrayList<>();
        try (Connection conn = ConexionBD.conectar(); PreparedStatement st = conn.prepareStatement(sql); ResultSet rs = st.executeQuery()) {
            while (rs.next()) {
                Inventario i = mapear(rs);
                i.setStockActual(rs.getInt("stock_disponible"));
                lista.add(i);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    private Inventario mapear(ResultSet rs) throws SQLException {
        Inventario i = new Inventario();
        i.setIdActivo(rs.getString("id_activo"));
        i.setDescripcion(rs.getString("descripcion"));
        i.setTipoActivo(rs.getString("tipo_activo"));
        i.setEstadoSst(rs.getString("estado_sst"));
        i.setUbicacion(rs.getString("ubicacion"));
        i.setFechaRegistro(rs.getString("fecha_registro"));
        i.setObservaciones(rs.getString("observaciones"));
        i.setStockActual(rs.getInt("stock_actual"));
        i.setPuntoReorden(rs.getInt("punto_reorden"));
        i.setStockActual(rs.getInt("stock_disponible"));
        return i;
    }
}
