
package com.ryrcontrolcenter.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class ConexionBD {
    public static final java.nio.file.Path RUTA_ARCHIVO = java.nio.file.Path.of("data", "ryr_controlcenter.db");
    private static final String URL = "jdbc:sqlite:data/ryr_controlcenter.db";

    public static Connection conectar() throws SQLException {
        Connection conn = DriverManager.getConnection(URL);
        try (Statement st = conn.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON;");
        }
        return conn;
    }
}
