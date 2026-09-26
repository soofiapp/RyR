/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ryrcontrolcenter.dao;

import com.ryrcontrolcenter.config.ConexionBD;
import com.ryrcontrolcenter.modelo.Maquina;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Juanp
 */
public class MaquinaDao {

    public boolean insertar(Maquina m) {
        String sql = "INSERT INTO maquinas (id_maquina, marca, modelo, numero_serie, horometro, area_departamento) VALUES (?,?,?,?,?,?)";
        try (Connection conn = ConexionBD.conectar(); PreparedStatement st = conn.prepareStatement(sql)) {
            st.setString(1, m.getIdMaquina());
            st.setString(2, m.getMarca());
            st.setString(3, m.getModelo());
            st.setString(4, m.getNumero_serie());
            st.setDouble(5, m.getHorometro());
            st.setString(6, m.getAreaDepartamento());
            return st.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean actualizar(Maquina m) {
        String sql = "UPDATE maquinas SET marca=?, modelo=?, numero_serie=?, horometro=?, area_departamento=? WHERE id_maquina=?";
        try (Connection conn = ConexionBD.conectar(); PreparedStatement st = conn.prepareStatement(sql)) {
            st.setString(1, m.getMarca());
            st.setString(2, m.getModelo());
            st.setString(3, m.getNumero_serie());
            st.setDouble(4, m.getHorometro());
            st.setString(5, m.getAreaDepartamento());
            st.setString(6, m.getIdMaquina());
            return st.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public Maquina buscarPorId(String idMaquina) {
        String sql = "SELECT * FROM maquinas WHERE id_maquina = ?";
        try (Connection conn = ConexionBD.conectar(); PreparedStatement st = conn.prepareStatement(sql)) {
            st.setString(1, idMaquina);
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

    /**
     * Trae las maquinas visibles solamente, cruzando con inventario para
     * respetar la baja logica (visible = 0 = oculta). maquinas no tiene su
     * propia columna 'visible': hereda la del activo en inventario.
     */
    public List<Maquina> listarTodas() {
        List<Maquina> lista = new ArrayList<>();
        String sql = """
            SELECT m.* FROM maquinas m
            JOIN inventario i ON m.id_maquina = i.id_activo
            WHERE i.visible = 1
            ORDER BY m.id_maquina
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

    private Maquina mapear(ResultSet rs) throws SQLException {
        Maquina m = new Maquina();
        m.setIdMaquina(rs.getString("id_maquina"));
        m.setMarca(rs.getString("marca"));
        m.setModelo(rs.getString("modelo"));
        m.setNumero_serie(rs.getString("numero_serie"));
        m.setHorometro(rs.getDouble("horometro"));
        m.setAreaDepartamento(rs.getString("area_departamento"));
        return m;
    }
}
