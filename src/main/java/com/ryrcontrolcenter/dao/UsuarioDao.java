package com.ryrcontrolcenter.dao;

import com.ryrcontrolcenter.config.ConexionBD;
import com.ryrcontrolcenter.modelo.Usuario;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UsuarioDao {

    public Usuario buscarPorLogin(String usuarioLogin) {
        String sql = "SELECT * FROM usuarios WHERE usuario_login = ? AND activo = 1";
        try (Connection conn = ConexionBD.conectar(); PreparedStatement st = conn.prepareStatement(sql)) {
            st.setString(1, usuarioLogin);
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

    public Usuario buscarPorId(int idUsuario) {
        String sql = "SELECT * FROM usuarios WHERE id_usuario = ? AND activo = 1";
        try (Connection conn = ConexionBD.conectar(); PreparedStatement st = conn.prepareStatement(sql)) {
            st.setInt(1, idUsuario);
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

    public boolean actualizarPassword(int idUsuario, String nuevoHash) {
        String sql = "UPDATE usuarios SET password_hash = ? WHERE id_usuario = ?";
        try (Connection conn = ConexionBD.conectar(); PreparedStatement st = conn.prepareStatement(sql)) {
            st.setString(1, nuevoHash);
            st.setInt(2, idUsuario);
            return st.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void actualizarUltimoAcceso(int idUsuario) {
        String sql = "UPDATE usuarios SET ultimo_acceso = datetime('now', 'localtime') WHERE id_usuario = ?";
        try (Connection conn = ConexionBD.conectar(); PreparedStatement st = conn.prepareStatement(sql)) {
            st.setInt(1, idUsuario);
            st.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean insertar(Usuario u) {
        String sql = "INSERT INTO usuarios (nombre_completo, usuario_login, password_hash, rol, frente_planta, activo) VALUES (?,?,?,?,?,1)";
        try (Connection conn = ConexionBD.conectar(); PreparedStatement st = conn.prepareStatement(sql)) {
            st.setString(1, u.getNombreCompleto());
            st.setString(2, u.getUsuarioLogin());
            st.setString(3, u.getPasswordHash());
            st.setString(4, u.getRol());
            st.setString(5, u.getFrentePlanta());
            return st.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean actualizar(Usuario u) {
        String sql = "UPDATE usuarios SET nombre_completo=?, usuario_login=?, rol=?, frente_planta=? WHERE id_usuario=?";
        try (Connection conn = ConexionBD.conectar(); PreparedStatement st = conn.prepareStatement(sql)) {
            st.setString(1, u.getNombreCompleto());
            st.setString(2, u.getUsuarioLogin());
            st.setString(3, u.getRol());
            st.setString(4, u.getFrentePlanta());
            st.setInt(5, u.getIdUsuario());
            return st.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean actualizarConPassword(Usuario u, String nuevoHash) {
        String sql = "UPDATE usuarios SET nombre_completo=?, "
                + "usuario_login=?, password_hash=?, rol=?, "
                + "frente_planta=? WHERE id_usuario=?";
        try (Connection conn = ConexionBD.conectar(); PreparedStatement st = conn.prepareStatement(sql)) {
            st.setString(1, u.getNombreCompleto());
            st.setString(2, u.getUsuarioLogin());
            st.setString(3, nuevoHash);
            st.setString(4, u.getRol());
            st.setString(5, u.getFrentePlanta());
            st.setInt(6, u.getIdUsuario());
            int filas = st.executeUpdate();
            return filas > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean eliminar(int idUsuario) {
        String sql = "UPDATE usuarios SET activo = 0 WHERE id_usuario = ?";
        try (Connection conn = ConexionBD.conectar(); PreparedStatement st = conn.prepareStatement(sql)) {
            st.setInt(1, idUsuario);
            return st.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<Usuario> listarTodos() {
        String sql = "SELECT * FROM usuarios WHERE activo = 1 ORDER BY nombre_completo";
        List<Usuario> lista = new ArrayList<>();
        try (Connection conn = ConexionBD.conectar(); PreparedStatement st = conn.prepareStatement(sql); 
            ResultSet rs = st.executeQuery()) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    private Usuario mapear(ResultSet rs) throws SQLException {
        Usuario u = new Usuario();
        u.setIdUsuario(rs.getInt("id_usuario"));
        u.setNombreCompleto(rs.getString("nombre_completo"));
        u.setUsuarioLogin(rs.getString("usuario_login"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setRol(rs.getString("rol"));
        u.setFrentePlanta(rs.getString("frente_planta"));
        u.setUltimoAcceso(rs.getString("ultimo_acceso"));
        u.setActivo(rs.getInt("activo") == 1);
        return u;
    }
}
