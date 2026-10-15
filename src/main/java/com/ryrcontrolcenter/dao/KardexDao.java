package com.ryrcontrolcenter.dao;

import com.ryrcontrolcenter.config.ConexionBD;
import com.ryrcontrolcenter.modelo.KardexFila;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;


public class KardexDao {


    public record Resultado(int saldoAnterior, int saldoNuevo, int cantidadRegistrada, String descripcionInsumo) { }


    public List<KardexFila> listarMovimientos() {
        String sql = """
            SELECT m.id_movimiento, m.fecha_hora, m.tipo_movimiento, m.id_filtro, m.cantidad, m.saldo_resultante,
                   f.descripcion, f.categoria_filtro, f.unidad_medida, f.punto_reorden,
                   u.nombre_completo AS responsable
            FROM movimientos_kardex m
            JOIN filtros f  ON f.id_filtro  = m.id_filtro
            JOIN usuarios u ON u.id_usuario = m.id_usuario
            ORDER BY m.fecha_hora DESC, m.id_movimiento DESC
            """;
        List<KardexFila> lista = new ArrayList<>();
        try (Connection conn = ConexionBD.conectar(); PreparedStatement st = conn.prepareStatement(sql); ResultSet rs = st.executeQuery()) {
            while (rs.next()) {
                KardexFila k = new KardexFila();
                k.setIdMovimiento(rs.getInt("id_movimiento"));
                k.setFechaHora(rs.getString("fecha_hora"));
                k.setTipo(rs.getString("tipo_movimiento"));
                k.setIdFiltro(rs.getString("id_filtro"));
                k.setDescripcion(rs.getString("descripcion"));
                k.setCategoria(rs.getString("categoria_filtro"));
                k.setUnidad(rs.getString("unidad_medida"));
                k.setPuntoReorden(rs.getInt("punto_reorden"));
                k.setSaldo(rs.getInt("saldo_resultante"));
                k.setResponsable(rs.getString("responsable"));
                int cantidad = rs.getInt("cantidad");
                String tipo = k.getTipo();
                if ("Entrada".equals(tipo)) {
                    k.setEntrada(cantidad);
                } else if ("Salida".equals(tipo)) {
                    k.setSalida(cantidad);
                } else if (cantidad >= 0) { 
                    k.setEntrada(cantidad);
                } else {
                    k.setSalida(-cantidad);
                }
                lista.add(k);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    
    public Resultado registrarMovimiento(String idFiltro, String tipo, int cantidad, int idUsuario) throws SQLException {
        try (Connection conn = ConexionBD.conectar()) {
            conn.setAutoCommit(false);
            try {
                int stock;
                String descripcion;
                try (PreparedStatement st = conn.prepareStatement("SELECT stock_actual, descripcion FROM filtros WHERE id_filtro = ?")) {
                    st.setString(1, idFiltro);
                    try (ResultSet rs = st.executeQuery()) {
                        if (!rs.next()) {
                            throw new IllegalStateException("El insumo " + idFiltro + " no existe.");
                        }
                        stock = rs.getInt("stock_actual");
                        descripcion = rs.getString("descripcion");
                    }
                }

                int delta;
                int cantidadGuardada;
                switch (tipo) {
                    case "Entrada" -> {
                        delta = cantidad;
                        cantidadGuardada = cantidad;
                    }
                    case "Salida" -> {
                        if (cantidad > stock) {
                            throw new IllegalStateException("Stock insuficiente: hay " + stock + " y se intenta sacar " + cantidad + ".");
                        }
                        delta = -cantidad;
                        cantidadGuardada = cantidad;
                    }
                    case "Ajuste" -> {
                        delta = cantidad - stock;
                        if (delta == 0) {
                            throw new IllegalStateException("El conteo coincide con el stock actual (" + stock + "); no hay nada que ajustar.");
                        }
                        cantidadGuardada = delta;
                    }
                    default -> throw new IllegalStateException("Tipo de movimiento no valido: " + tipo);
                }
                int nuevo = stock + delta;

                try (PreparedStatement st = conn.prepareStatement(
                        "UPDATE filtros SET stock_actual = ?, fecha_actualizacion = datetime('now', 'localtime') WHERE id_filtro = ?")) {
                    st.setInt(1, nuevo);
                    st.setString(2, idFiltro);
                    st.executeUpdate();
                }
                try (PreparedStatement st = conn.prepareStatement(
                        "INSERT INTO movimientos_kardex (fecha_hora, tipo_movimiento, id_filtro, cantidad, saldo_resultante, id_usuario) "
                        + "VALUES (datetime('now', 'localtime'), ?, ?, ?, ?, ?)")) {
                    st.setString(1, tipo);
                    st.setString(2, idFiltro);
                    st.setInt(3, cantidadGuardada);
                    st.setInt(4, nuevo);
                    st.setInt(5, idUsuario);
                    st.executeUpdate();
                }
                conn.commit();
                return new Resultado(stock, nuevo, cantidadGuardada, descripcion);
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
    }
}
