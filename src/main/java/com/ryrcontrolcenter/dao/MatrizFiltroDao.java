/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ryrcontrolcenter.dao;

import com.ryrcontrolcenter.config.ConexionBD;
import com.ryrcontrolcenter.modelo.MatrizFiltro;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;


public class MatrizFiltroDao {

    public boolean insertar(MatrizFiltro mf) {
        String sql = "INSERT INTO matriz_filtro (id_maquina, sistema_tipo_filtro, codigo_oem, id_filtro) VALUES (?,?,?,?)";
        try (Connection conn = ConexionBD.conectar();
             PreparedStatement st = conn.prepareStatement(sql)) {
            st.setString(1, mf.getIdMaquina());
            st.setString(2, mf.getSistemaTipoFiltro());
            st.setString(3, mf.getCodigoOem());
            st.setString(4, mf.getIdFiltro());
            return st.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean eliminar(int idMatriz) {
        String sql = "UPDATE matriz_filtro SET visible = 0 WHERE id_matriz = ?";
        try (Connection conn = ConexionBD.conectar();
             PreparedStatement st = conn.prepareStatement(sql)) {
            st.setInt(1, idMatriz);
            return st.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean actualizar(MatrizFiltro mf) {
        String sql = "UPDATE matriz_filtro SET sistema_tipo_filtro=?, codigo_oem=?, id_filtro=? WHERE id_matriz=?";
        try (Connection conn = ConexionBD.conectar();
             PreparedStatement st = conn.prepareStatement(sql)) {
            st.setString(1, mf.getSistemaTipoFiltro());
            st.setString(2, mf.getCodigoOem());
            st.setString(3, mf.getIdFiltro());
            st.setInt(4, mf.getIdMatriz());
            return st.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<MatrizFiltro> listarPorMaquina(String idMaquina) {
        List<MatrizFiltro> lista = new ArrayList<>();
        String sql = """
            SELECT mf.id_matriz, mf.id_maquina, mf.sistema_tipo_filtro, mf.codigo_oem, mf.id_filtro,
                   f.descripcion AS descripcion_filtro, f.stock_actual
            FROM matriz_filtro mf
            LEFT JOIN filtros f ON mf.id_filtro = f.id_filtro
            WHERE mf.id_maquina = ? AND mf.visible = 1
            ORDER BY mf.sistema_tipo_filtro
            """;
        try (Connection conn = ConexionBD.conectar();
             PreparedStatement st = conn.prepareStatement(sql)) {
            st.setString(1, idMaquina);
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    private MatrizFiltro mapear(ResultSet rs) throws SQLException {
        MatrizFiltro mf = new MatrizFiltro();
        mf.setIdMatriz(rs.getInt("id_matriz"));
        mf.setIdMaquina(rs.getString("id_maquina"));
        mf.setSistemaTipoFiltro(rs.getString("sistema_tipo_filtro"));
        mf.setCodigoOem(rs.getString("codigo_oem"));
        mf.setIdFiltro(rs.getString("id_filtro"));
        mf.setDescripcionFiltro(rs.getString("descripcion_filtro"));
        mf.setStockActual(rs.getInt("stock_actual"));
        return mf;
    }
}