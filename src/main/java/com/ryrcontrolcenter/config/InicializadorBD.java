package com.ryrcontrolcenter.config;

import com.ryrcontrolcenter.util.PasswordUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public final class InicializadorBD {

    private static final String USUARIO_INICIAL = "admin";
    private static final String PASSWORD_INICIAL = "RyRAdmin#2026";

    private InicializadorBD() {
    }

    public static void inicializar() throws SQLException {
        try (Connection conn = ConexionBD.conectar()) {
            conn.setAutoCommit(false);
            try {
                crearTablas(conn);
                crearIndices(conn);
                crearAdministradorInicial(conn);
                conn.commit();
                System.out.println("Base de datos inicializada correctamente.");
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    private static void crearTablas(Connection conn) throws SQLException {
        String[] tablas = {
            """
            CREATE TABLE IF NOT EXISTS usuarios (
                id_usuario INTEGER PRIMARY KEY AUTOINCREMENT,
                nombre_completo TEXT NOT NULL,
                usuario_login TEXT NOT NULL UNIQUE,
                password_hash TEXT NOT NULL,
                rol TEXT NOT NULL
                    CHECK (rol IN ('Administrador','Auxiliar')),
                frente_planta TEXT,
                ultimo_acceso DATETIME,
                activo INTEGER NOT NULL DEFAULT 1
                    CHECK (activo IN (0,1))
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS inventario (
                id_activo TEXT PRIMARY KEY,
                descripcion TEXT NOT NULL,
                tipo_activo TEXT NOT NULL
                    CHECK (tipo_activo IN (
                        'Maquinaria','Herramienta','Kit Agrupado'
                    )),
                estado_sst TEXT NOT NULL DEFAULT 'Operativa'
                    CHECK (estado_sst IN (
                        'Operativa','Bloqueada','Mantenimiento','Completo'
                    )),
                ubicacion TEXT,
                fecha_registro DATE NOT NULL,
                observaciones TEXT,
                stock_actual INTEGER NOT NULL DEFAULT 1,
                punto_reorden INTEGER NOT NULL DEFAULT 0,
                visible INTEGER NOT NULL DEFAULT 1
                    CHECK (visible IN (0,1))
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS maquinas (
                id_maquina TEXT PRIMARY KEY,
                marca TEXT,
                modelo TEXT,
                numero_serie TEXT,
                horometro NUMERIC,
                area_departamento TEXT,
                FOREIGN KEY (id_maquina)
                    REFERENCES inventario(id_activo)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS kit_detalle (
                id_detalle INTEGER PRIMARY KEY AUTOINCREMENT,
                id_kit TEXT NOT NULL,
                id_herramienta TEXT NOT NULL,
                cantidad INTEGER NOT NULL DEFAULT 1,
                FOREIGN KEY (id_kit) REFERENCES inventario(id_activo),
                FOREIGN KEY (id_herramienta) REFERENCES inventario(id_activo),
                UNIQUE (id_kit, id_herramienta)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS filtros (
                id_filtro TEXT PRIMARY KEY,
                descripcion TEXT NOT NULL,
                categoria_filtro TEXT NOT NULL
                    CHECK (categoria_filtro IN (
                        'Aceite','Aire','Combustible',
                        'Hidraulico','Refrigerante'
                    )),
                unidad_medida TEXT NOT NULL DEFAULT 'Unidad',
                stock_actual INTEGER NOT NULL DEFAULT 0,
                punto_reorden INTEGER NOT NULL DEFAULT 0,
                fecha_actualizacion DATETIME
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS matriz_filtro (
                id_matriz INTEGER PRIMARY KEY AUTOINCREMENT,
                id_maquina TEXT NOT NULL,
                sistema_tipo_filtro TEXT NOT NULL,
                codigo_oem TEXT NOT NULL,
                id_filtro TEXT,
                visible INTEGER NOT NULL DEFAULT 1
                    CHECK (visible IN (0,1)),
                FOREIGN KEY (id_maquina) REFERENCES maquinas(id_maquina),
                FOREIGN KEY (id_filtro) REFERENCES filtros(id_filtro),
                UNIQUE (id_maquina, sistema_tipo_filtro)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS movimientos_kardex (
                id_movimiento INTEGER PRIMARY KEY AUTOINCREMENT,
                fecha_hora DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                tipo_movimiento TEXT NOT NULL
                    CHECK (tipo_movimiento IN ('Entrada','Salida','Ajuste')),
                id_filtro TEXT,
                id_activo TEXT,
                cantidad INTEGER NOT NULL,
                saldo_resultante INTEGER NOT NULL,
                id_usuario INTEGER NOT NULL,
                FOREIGN KEY (id_filtro) REFERENCES filtros(id_filtro),
                FOREIGN KEY (id_activo) REFERENCES inventario(id_activo),
                FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario),
                CHECK (
                    (id_filtro IS NOT NULL AND id_activo IS NULL)
                    OR
                    (id_filtro IS NULL AND id_activo IS NOT NULL)
                )
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS prestamos (
                id_prestamo TEXT PRIMARY KEY,
                id_activo TEXT NOT NULL,
                operario_nombre TEXT NOT NULL,
                operario_cedula TEXT NOT NULL,
                ubicacion_frente TEXT,
                fecha_salida DATETIME NOT NULL,
                fecha_devolucion_estimada DATETIME,
                fecha_devolucion_real DATETIME,
                estado TEXT NOT NULL DEFAULT 'En Uso'
                    CHECK (estado IN ('En Uso','Atrasado','Devuelto')),
                observaciones_salida TEXT,
                descripcion_estado_devolucion TEXT,
                cantidad INTEGER NOT NULL DEFAULT 1,
                id_usuario_registro INTEGER NOT NULL,
                visible INTEGER NOT NULL DEFAULT 1
                    CHECK (visible IN (0,1)),
                FOREIGN KEY (id_activo) REFERENCES inventario(id_activo),
                FOREIGN KEY (id_usuario_registro) REFERENCES usuarios(id_usuario)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS bitacora (
                id_bitacora INTEGER PRIMARY KEY AUTOINCREMENT,
                fecha_hora DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                id_usuario INTEGER NOT NULL,
                accion_realizada TEXT NOT NULL,
                modulo_afectado TEXT NOT NULL
                    CHECK (modulo_afectado IN (
                        'Prestamos','Inventariado','Insumos/Kardex',
                        'Matriz Filtro','Usuarios y Bitacora'
                    )),
                referencia_id TEXT,
                FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario)
            )
            """
        };
        try (Statement st = conn.createStatement()) {
            for (String sql : tablas) {
                st.execute(sql);
            }
        }
    }

    private static void crearIndices(Connection conn) throws SQLException {
        String[] indices = {
            "CREATE INDEX IF NOT EXISTS idx_inventario_tipo "
            + "ON inventario(tipo_activo)",
            "CREATE INDEX IF NOT EXISTS idx_inventario_estado "
            + "ON inventario(estado_sst)",
            "CREATE INDEX IF NOT EXISTS idx_prestamos_estado "
            + "ON prestamos(estado)",
            "CREATE INDEX IF NOT EXISTS idx_prestamos_activo "
            + "ON prestamos(id_activo)",
            "CREATE INDEX IF NOT EXISTS idx_kardex_filtro_fecha "
            + "ON movimientos_kardex(id_filtro, fecha_hora)",
            "CREATE INDEX IF NOT EXISTS idx_bitacora_fecha "
            + "ON bitacora(fecha_hora)",
            "CREATE INDEX IF NOT EXISTS idx_kitdetalle_herramienta "
            + "ON kit_detalle(id_herramienta)"
        };
        try (Statement st = conn.createStatement()) {
            for (String sql : indices) {
                st.execute(sql);
            }
        }
    }
    
    private static void crearAdministradorInicial(Connection conn)
            throws SQLException {
        String usuarioInicial = "auxiliar";
        String passwordInicial = "RyRAuxiliar#2026";
        String sqlBuscar = """
        SELECT COUNT(*)
        FROM usuarios
        WHERE usuario_login = ?
        """;
        try (PreparedStatement ps = conn.prepareStatement(sqlBuscar)) {
            ps.setString(1, usuarioInicial);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    // El usuario ya existe; no se vuelve a crear.
                    return;
                }
            }
        }
        String sqlInsertar = """
        INSERT INTO usuarios (
            nombre_completo,
            usuario_login,
            password_hash,
            rol,
            frente_planta,
            activo
        ) VALUES (?, ?, ?, ?, ?, 1)
        """;
        String hash = PasswordUtil.generarHash(passwordInicial);
        try (PreparedStatement ps = conn.prepareStatement(sqlInsertar)) {
            ps.setString(1, "Auxiliar inicial");
            ps.setString(2, usuarioInicial);
            ps.setString(3, hash);
            ps.setString(4, "Auxiliar");
            ps.setString(5, null);
            ps.executeUpdate();
        }
        System.out.println("Usuario auxiliar inicial creado.");
        System.out.println("Usuario: " + usuarioInicial);
        System.out.println("Contraseña: " + passwordInicial);
    }
}
