package com.ryrcontrolcenter.dao;

import com.ryrcontrolcenter.config.ConexionBD;
import com.ryrcontrolcenter.modelo.Prestamos;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PrestamoDao {

    // 1. LISTAR TODOS
    public List<Prestamos> listarTodos() throws SQLException {
        String sql = "SELECT * FROM prestamos ORDER BY id_prestamo";
        List<Prestamos> lista = new ArrayList<>();
        try (Connection conn = ConexionBD.conectar(); PreparedStatement st = conn.prepareStatement(sql); ResultSet rs = st.executeQuery()) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    // 2. LISTAR SOLO ACTIVOS (Excluye los devolucionados)
    public List<Prestamos> listarActivos() throws SQLException {
        String sql = "SELECT * FROM prestamos WHERE estado != 'Devuelto' ORDER BY id_prestamo";
        List<Prestamos> lista = new ArrayList<>();
        try (Connection conn = ConexionBD.conectar(); PreparedStatement st = conn.prepareStatement(sql); ResultSet rs = st.executeQuery()) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    // 3. GUARDAR (INSERTAR NUEVO PRÉSTAMO)
    public boolean guardar(Prestamos p) {
        String sql = "INSERT INTO prestamos (id_prestamo, id_activo, operario_nombre, operario_cedula, "
                + "ubicacion_frente, fecha_salida, fecha_devolucion_estimada, estado, "
                + "observaciones_salida, descripcion_estado_devolucion, id_usuario_registro, cantidad) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = ConexionBD.conectar(); PreparedStatement st = conn.prepareStatement(sql)) {

            st.setString(1, p.getIdPrestamo());
            st.setString(2, p.getIdActivo());
            st.setString(3, p.getOperarioNombre());
            st.setString(4, p.getOperarioCedula());
            st.setString(5, p.getUbicacionFrente());
            st.setString(6, p.getFechaSalida());
            st.setString(7, p.getFechaDevolucionEstimada());
            // Se valida que si no tiene estado asignado, por defecto sea 'En Uso'
            st.setString(8, (p.getEstado() != null && !p.getEstado().isEmpty()) ? p.getEstado() : "En Uso");
            st.setString(9, p.getObservacionesSalida());
            st.setString(10, p.getDescripcionEstadoDevolucion());
            st.setInt(11, p.getIdUusarioRegistro());
            st.setInt(12, p.getCantidad());

            return st.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println(">>> ERROR SQL AL GUARDAR: " + e.getMessage());
            if (e.getMessage().contains("FOREIGN KEY")) {
                System.err.println(">>> DETALLE: El código del activo (id_activo) o el usuario no existe en las tablas maestras de la BD.");
            }
            e.printStackTrace();
            return false;
        }
    }

    // 4. ACTUALIZAR (EDITAR PRÉSTAMO EXISTENTE)
    public boolean actualizar(Prestamos p) {
        String sql = "UPDATE prestamos SET id_activo = ?, operario_nombre = ?, operario_cedula = ?, "
                + "ubicacion_frente = ?, fecha_salida = ?, fecha_devolucion_estimada = ?, "
                + "observaciones_salida = ?, descripcion_estado_devolucion = ?, cantidad = ? "
                + "WHERE id_prestamo = ?";

        try (Connection conn = ConexionBD.conectar(); PreparedStatement st = conn.prepareStatement(sql)) {

            st.setString(1, p.getIdActivo());
            st.setString(2, p.getOperarioNombre());
            st.setString(3, p.getOperarioCedula());
            st.setString(4, p.getUbicacionFrente());
            st.setString(5, p.getFechaSalida());
            st.setString(6, p.getFechaDevolucionEstimada());
            st.setString(7, p.getObservacionesSalida());
            st.setString(8, p.getDescripcionEstadoDevolucion());
            st.setInt(9, p.getCantidad());
            st.setString(10, p.getIdPrestamo());

            return st.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println(">>> ERROR SQL AL ACTUALIZAR: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean tienePrestamoActivo(String idActivo) {
        String sql = "SELECT COUNT(*) FROM prestamos WHERE id_activo = ? AND estado IN ('En Uso', 'Atrasado')";
        try (Connection conn = ConexionBD.conectar(); PreparedStatement st = conn.prepareStatement(sql)) {
            st.setString(1, idActivo);
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

    // 5. ELIMINACIÓN LÓGICA (Marca el estado como 'Devuelto')
    public boolean eliminarLogico(String idPrestamo) {
        String sql = "UPDATE prestamos SET estado = 'Devuelto' WHERE id_prestamo = ?";
        try (Connection conn = ConexionBD.conectar(); PreparedStatement st = conn.prepareStatement(sql)) {
            st.setString(1, idPrestamo);
            return st.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println(">>> ERROR SQL AL ELIMINAR LÓGICO: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // MÉTODOS AUXILIARES
    private Prestamos mapear(ResultSet rs) throws SQLException {
        Prestamos p = new Prestamos();
        p.setIdPrestamo(rs.getString("id_prestamo"));
        p.setIdActivo(rs.getString("id_activo"));
        p.setOperarioNombre(rs.getString("operario_nombre"));
        p.setOperarioCedula(rs.getString("operario_cedula"));
        p.setUbicacionFrente(rs.getString("ubicacion_frente"));
        p.setFechaSalida(rs.getString("fecha_salida"));
        p.setFechaDevolucionEstimada(rs.getString("fecha_devolucion_estimada"));
        p.setEstado(rs.getString("estado"));
        p.setObservacionesSalida(rs.getString("observaciones_salida"));
        p.setDescripcionEstadoDevolucion(rs.getString("descripcion_estado_devolucion"));
        p.setIdUusarioRegistro(rs.getInt("id_usuario_registro"));
        p.setCantidad(rs.getInt("cantidad"));
        return p;
    }
}
