/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ryrcontrolcenter.service;

import com.ryrcontrolcenter.dao.InventarioDao;
import com.ryrcontrolcenter.modelo.Inventario;
import java.sql.SQLException;
import java.util.List;

/**
 *
 * @author sofia
 */
public class InventarioService {

    InventarioDao dao;

    public InventarioService() {
        dao = new InventarioDao();

    }

    public boolean agregarItemInventarioService(Inventario item) {
        return dao.agregarItemInventario(item);
    }

    public boolean eliminarItemInventarioService(String idActivo) {
        return dao.eliminarItemInventario(idActivo);
    }

    public boolean actualizarItemInventarioService(String idActivo, Inventario item) {
        return dao.actualizarItemInventario(idActivo, item);
    }

    public List<Inventario> verItemsInventario() throws SQLException {
        return dao.listarTodos();
    }
}
