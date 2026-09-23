/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ryrcontrolcenter.dao;

import com.ryrcontrolcenter.config.ConexionBD;
import com.ryrcontrolcenter.modelo.Inventario;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author sofia
 */
public class InventarioDAO {
    
    public boolean agregarItemInventario (Inventario item){
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
            
            try (ResultSet rs = st.executeQuery()) {
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;   
    }
    
    public boolean eliminarItemInventario(String idActivo){
        String sql = "DELETE FROM inventario WHERE id_activo = ? ";
        try (Connection conn = ConexionBD.conectar();
             PreparedStatement st = conn.prepareStatement(sql)) {

            st.setString(1, idActivo);
          
            try (ResultSet rs = st.executeQuery()) {
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false; 
    }
    
    public boolean actualizarItemInventario(String idActivo, Inventario item){
        String sql = "UPDATE inventario SET (descripcion, tipo_activo, estado_sst, ubicacion, fecha_registro, observaciones) "
                   + "VALUES (?, ?, ?, ?, ?, ?) WHERE idActivo=? ";
        try (Connection conn = ConexionBD.conectar();
             PreparedStatement st = conn.prepareStatement(sql)) {

            st.setString(1, item.getDescripcion());
            st.setString(2, item.getTipoActivo());
            st.setString(3, item.getEstadoSst());
            st.setString(4, item.getUbicacion());
            st.setString(5, item.getFechaRegistro());
            st.setString(6, item.getObservaciones());  
            
            //idActivo es para actualizar
            st.setString(7, item.getIdActivo()); 
          
            try (ResultSet rs = st.executeQuery()) {
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false; 
    }  
    
    public Inventario[] verItemsInventario() {
        String sql = "SELECT id_activo, descripcion, tipo_activo, estado_sst, ubicacion, fecha_registro, observaciones "
                   + "FROM inventario";

        List lista = new ArrayList<>();

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement st = conn.prepareStatement(sql);
             ResultSet rs = st.executeQuery()) {

            while (rs.next()) {
                lista.add(formatearData(rs));
            }

        } catch (SQLException e) {
            System.err.println("Error al consultar el inventario: " + e.getMessage());
            e.printStackTrace();
        }

        // Convierte la lista temporal al arreglo requerido
        return (Inventario[]) lista.toArray(new Inventario[0]);
    } 
        
    private Inventario formatearData(ResultSet rs) throws SQLException {
    Inventario i = new Inventario();
    i.setIdActivo(rs.getString("id_activo"));
    i.setDescripcion(rs.getString("descripcion"));
    i.setTipoActivo(rs.getString("tipo_activo"));
    i.setEstadoSst(rs.getString("estado_sst"));
    i.setUbicacion(rs.getString("ubicacion"));
    i.setFechaRegistro(rs.getString("fecha_registro"));
    i.setObservaciones(rs.getString("observacion"));  
        return i;
    }
    
    public Inventario verItemInventario(String idActivo) {
        String sql = "SELECT id_activo, descripcion, tipo_activo, estado_sst, ubicacion, fecha_registro, observaciones "
                   + "FROM inventario WHERE idActivo = ?";

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement st = conn.prepareStatement(sql)) {

            st.setString(1, idActivo);
         
            try (ResultSet rs = st.executeQuery()) {
                return formatearData(rs);
                
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }   
     
}
