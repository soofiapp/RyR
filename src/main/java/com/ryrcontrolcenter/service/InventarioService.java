/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ryrcontrolcenter.service;

import com.ryrcontrolcenter.dao.InventarioDAO;
import com.ryrcontrolcenter.modelo.Inventario;



/**
 *
 * @author sofia
 */
public class InventarioService {
    
InventarioDAO dao;    
    
public InventarioService(){
    dao = new InventarioDAO(); 
    
}

public boolean agregarItemInventarioService (Inventario item){
    return dao.agregarItemInventario(item);
}
    
 public boolean eliminarItemInventarioService(String idActivo){
     return dao.eliminarItemInventario(idActivo);
 }   
    
 public boolean actualizarItemInventarioService(String idActivo, Inventario item){
     return dao.actualizarItemInventario(idActivo, item);
 }   
    
public Inventario[] verItemsInventario(){
    return dao.verItemsInventario();
}  
 
public Inventario verItemInventario(String idActivo){
    return dao.verItemInventario(idActivo);
}
    
}        

