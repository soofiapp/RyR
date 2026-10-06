package com.ryrcontrolcenter.dao;

import com.ryrcontrolcenter.config.ConexionBD;
import com.ryrcontrolcenter.modelo.DashboardResumen;
import com.ryrcontrolcenter.modelo.Filtros;
import com.ryrcontrolcenter.service.KardexService;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DashboardDao {


    private static final String PRESTABLES = """
        FROM inventario i
        WHERE i.visible = 1
          AND i.id_activo NOT IN (SELECT id_herramienta FROM kit_detalle)
        """;

    public DashboardResumen cargar(int mesesTendencia) {
        DashboardResumen r = new DashboardResumen();
        try (Connection conn = ConexionBD.conectar()) {
            r.maquinas = contar(conn, "SELECT COUNT(*) FROM inventario WHERE visible = 1 AND tipo_activo = 'Maquinaria'");
            r.herramientas = contar(conn, "SELECT COUNT(*) FROM inventario WHERE visible = 1 AND tipo_activo = 'Herramienta'");
            r.kits = contar(conn, "SELECT COUNT(*) FROM inventario WHERE visible = 1 AND tipo_activo = 'Kit Agrupado'");
            r.bloqueadosMantenimiento = contar(conn,
                    "SELECT COUNT(*) FROM inventario WHERE visible = 1 AND estado_sst IN ('Bloqueada','Mantenimiento')");
            r.aprobados = contar(conn,
                    "SELECT COUNT(*) FROM inventario WHERE visible = 1 AND estado_sst IN ('Operativa','Completo')");
            r.prestamosActivos = contar(conn, "SELECT COUNT(*) FROM prestamos WHERE estado IN ('En Uso','Atrasado')");
            r.prestamosAtrasados = contar(conn, "SELECT COUNT(*) FROM prestamos WHERE estado = 'Atrasado'");
            r.devoluciones = contar(conn, "SELECT COUNT(*) FROM prestamos WHERE estado = 'Devuelto'");
            r.filtros = contar(conn, "SELECT COUNT(*) FROM filtros");

            r.prestables = contar(conn, "SELECT COUNT(*) " + PRESTABLES);
            r.prestados = contar(conn, "SELECT COUNT(*) " + PRESTABLES
                    + " AND i.id_activo IN (SELECT id_activo FROM prestamos WHERE estado IN ('En Uso','Atrasado'))");

            cargarTendencia(conn, r, mesesTendencia);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        List<Filtros> todos = new FiltroDao().listarTodos();
        todos.sort(Comparator.comparingInt((Filtros f) -> f.getStockActual() - f.getPuntoReorden())
                .thenComparing(Filtros::getIdFiltro));
        r.filtrosPorCriticidad = todos;
        for (Filtros f : todos) {
            switch (KardexService.semaforo(f.getStockActual(), f.getPuntoReorden())) {
                case KardexService.ROJO -> r.semRojo++;
                case KardexService.AMARILLO -> r.semAmarillo++;
                default -> r.semVerde++;
            }
        }
        return r;
    }

    private void cargarTendencia(Connection conn, DashboardResumen r, int meses) throws SQLException {
        YearMonth actual = YearMonth.now();
        YearMonth inicio = actual.minusMonths(Math.max(1, meses) - 1L);
        Map<String, int[]> acumulado = new HashMap<>(); 
        for (YearMonth m = inicio; !m.isAfter(actual); m = m.plusMonths(1)) {
            r.meses.add(m.toString());
            acumulado.put(m.toString(), new int[3]);
        }
        String desde = inicio.atDay(1).toString();

        String sqlKardex = """
            SELECT strftime('%Y-%m', fecha_hora) AS mes,
                   SUM(CASE WHEN tipo_movimiento = 'Entrada' THEN cantidad
                            WHEN tipo_movimiento = 'Ajuste' AND cantidad > 0 THEN cantidad ELSE 0 END) AS entradas,
                   SUM(CASE WHEN tipo_movimiento = 'Salida' THEN cantidad
                            WHEN tipo_movimiento = 'Ajuste' AND cantidad < 0 THEN -cantidad ELSE 0 END) AS salidas
            FROM movimientos_kardex
            WHERE date(fecha_hora) >= ?
            GROUP BY mes
            """;
        try (PreparedStatement st = conn.prepareStatement(sqlKardex)) {
            st.setString(1, desde);
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    int[] a = acumulado.get(rs.getString("mes"));
                    if (a != null) {
                        a[0] = rs.getInt("entradas");
                        a[1] = rs.getInt("salidas");
                    }
                }
            }
        }
        String sqlPrestamos = """
            SELECT strftime('%Y-%m', fecha_salida) AS mes, COUNT(*) AS total
            FROM prestamos
            WHERE date(fecha_salida) >= ?
            GROUP BY mes
            """;
        try (PreparedStatement st = conn.prepareStatement(sqlPrestamos)) {
            st.setString(1, desde);
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    int[] a = acumulado.get(rs.getString("mes"));
                    if (a != null) {
                        a[2] = rs.getInt("total");
                    }
                }
            }
        }
        for (String mes : r.meses) {
            int[] a = acumulado.get(mes);
            r.entradasMes.add(a[0]);
            r.salidasMes.add(a[1]);
            r.prestamosMes.add(a[2]);
        }
    }

    private int contar(Connection conn, String sql) throws SQLException {
        try (PreparedStatement st = conn.prepareStatement(sql); ResultSet rs = st.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }
}
