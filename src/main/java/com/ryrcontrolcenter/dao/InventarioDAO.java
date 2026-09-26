
package com.ryrcontrolcenter.dao;

import com.ryrcontrolcenter.config.ConexionBD;
import com.ryrcontrolcenter.modelo.Inventario;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;


public class InventarioDao {
    
    public boolean agregarItemInventario(Inventario item) {
        String sql = "INSERT INTO inventario (id_activo, descripcion, tipo_activo, estado_sst, ubicacion, fecha_registro, observaciones) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = ConexionBD.conectar();
             PreparedStatement st = conn.prepareStatement(sql)) {

            st.setString(1, item.getIdActivo());
            st.setString(2, item.getDescripcion());
            st.setString(3, item.getTipoActivo());
            st.setString(4, item.getEstadoSst());
            st.setString(5, item.getUbicacion());
            st.setString(6, item.getFechaRegistro());
            st.setString(7, item.getObservaciones());

            int filasAfectadas = st.executeUpdate();
            return filasAfectadas > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean eliminarItemInventario(String idActivo) {
        String sql = "DELETE FROM inventario WHERE id_activo = ?";
        try (Connection conn = ConexionBD.conectar();
             PreparedStatement st = conn.prepareStatement(sql)) {

            st.setString(1, idActivo);
            int filasAfectadas = st.executeUpdate();
            return filasAfectadas > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean actualizarItemInventario(String idActivo, Inventario item) {
        String sql = "UPDATE inventario SET descripcion=?, tipo_activo=?, estado_sst=?, ubicacion=?, fecha_registro=?, observaciones=? "
                   + "WHERE id_activo=?";
        try (Connection conn = ConexionBD.conectar();
             PreparedStatement st = conn.prepareStatement(sql)) {

            st.setString(1, item.getDescripcion());
            st.setString(2, item.getTipoActivo());
            st.setString(3, item.getEstadoSst());
            st.setString(4, item.getUbicacion());
            st.setString(5, item.getFechaRegistro());
            st.setString(6, item.getObservaciones());
            st.setString(7, idActivo); // se usa el parametro recibido, no item.getIdActivo()

            int filasAfectadas = st.executeUpdate();
            return filasAfectadas > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<Inventario> listarTodos() throws SQLException {
        String sql = "SELECT * FROM inventario ORDER BY id_activo";
        List<Inventario> lista = new ArrayList<>();
        try (Connection conn = ConexionBD.conectar();
             PreparedStatement st = conn.prepareStatement(sql);
             ResultSet rs = st.executeQuery()) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    /**
     * Activos disponibles para prestamo: Herramienta o Maquinaria (los Kits
     * se prestan por su propio flujo), que ademas no tengan ya un prestamo
     * abierto ni esten bloqueados/en mantenimiento por SST.
     */
    public List<Inventario> listarActivosDisponiblesParaPrestamo() {
        String sql = """
            SELECT * FROM inventario
            WHERE tipo_activo IN ('Herramienta', 'Maquinaria')
              AND estado_sst = 'Operativa'
              AND id_activo NOT IN (SELECT id_activo FROM prestamos WHERE estado = 'En Uso')
            ORDER BY id_activo
            """;
        List<Inventario> lista = new ArrayList<>();
        try (Connection conn = ConexionBD.conectar();
             PreparedStatement st = conn.prepareStatement(sql);
             ResultSet rs = st.executeQuery()) {
            while (rs.next()) {
                lista.add(mapear(rs));
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
        return i;
    }
}
