
package com.ryrcontrolcenter.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.io.IOException;

public class ConexionBD {

    private static final Path CARPETA_DATOS =
            Path.of(System.getenv("APPDATA"),
                    "RyRControlCenter", "data");

    public static final Path RUTA_ARCHIVO =
            CARPETA_DATOS.resolve("ryr_controlcenter.db");

    private static final String URL =
            "jdbc:sqlite:" + RUTA_ARCHIVO.toAbsolutePath();

    public static Connection conectar() throws SQLException {
        try {
            Files.createDirectories(CARPETA_DATOS);
        } catch (IOException e) {
            throw new SQLException(
                    "No se pudo crear la carpeta de la base de datos.", e);
        }

        Connection conn = DriverManager.getConnection(URL);

        try (Statement st = conn.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON;");
        } catch (SQLException e) {
            conn.close();
            throw e;
        }

        return conn;
    }
}
