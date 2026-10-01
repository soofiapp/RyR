package com.ryrcontrolcenter.dao;

import com.ryrcontrolcenter.config.ConexionBD;
import com.ryrcontrolcenter.modelo.Filtros;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class FiltroDao {

    public boolean insertar(Filtros f) {
        String sql = "INSERT INTO filtros (id_filtro, descripcion, categoria_filtro, unidad_medida, stock_actual, punto_reorden, fecha_actualizacion) "
                + "VALUES (?,?,?,?,?,?, datetime('now', 'localtime'))";
        try (Connection conn = ConexionBD.conectar(); PreparedStatement st = conn.prepareStatement(sql)) {
            st.setString(1, f.getIdFiltro());
            st.setString(2, f.getDescripcion());
            st.setString(3, f.getCategoriaFiltro());
            st.setString(4, f.getUnidadMedida());
            st.setInt(5, f.getStockActual());
            st.setInt(6, f.getPuntoReorden());
            return st.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public Filtros buscarPorId(String idFiltro) {
        String sql = "SELECT * FROM filtros WHERE id_filtro = ?";
        try (Connection conn = ConexionBD.conectar(); PreparedStatement st = conn.prepareStatement(sql)) {
            st.setString(1, idFiltro);
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) {
                    return mapear(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Filtros> listarTodos() {
        List<Filtros> lista = new ArrayList<>();
        String sql = "SELECT * FROM filtros ORDER BY id_filtro";
        try (Connection conn = ConexionBD.conectar(); PreparedStatement st = conn.prepareStatement(sql); ResultSet rs = st.executeQuery()) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    public boolean actualizarStock(String idFiltro, int nuevoStock) {
        String sql = "UPDATE filtros SET stock_actual = ?, fecha_actualizacion = datetime('now', 'localtime') WHERE id_filtro = ?";
        try (Connection conn = ConexionBD.conectar(); PreparedStatement st = conn.prepareStatement(sql)) {
            st.setInt(1, nuevoStock);
            st.setString(2, idFiltro);
            return st.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean actualizarPuntoReorden(String idFiltro, int nuevoPuntoReorden) {
        String sql = "UPDATE filtros SET punto_reorden = ? WHERE id_filtro = ?";
        try (Connection conn = ConexionBD.conectar(); PreparedStatement st = conn.prepareStatement(sql)) {
            st.setInt(1, nuevoPuntoReorden);
            st.setString(2, idFiltro);
            return st.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void actualizarFechaActualizacion(String idFiltro) {
        String sql = "UPDATE filtros SET fecha_actualizacion = datetime('now', 'localtime') WHERE id_filtro = ?";
        try (Connection conn = ConexionBD.conectar(); PreparedStatement st = conn.prepareStatement(sql)) {
            st.setString(1, idFiltro);
            st.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private Filtros mapear(ResultSet rs) throws SQLException {
        Filtros f = new Filtros();
        f.setIdFiltro(rs.getString("id_filtro"));
        f.setDescripcion(rs.getString("descripcion"));
        f.setCategoriaFiltro(rs.getString("categoria_filtro"));
        f.setUnidadMedida(rs.getString("unidad_medida"));
        f.setStockActual(rs.getInt("stock_actual"));
        f.setPuntoReorden(rs.getInt("punto_reorden"));
        f.setFechaActualizacion(rs.getString("fecha_actualizacion"));
        return f;
    }
}
