package com.ryrcontrolcenter.service;

import com.ryrcontrolcenter.dao.KardexDao;
import com.ryrcontrolcenter.modelo.Usuario;
import com.ryrcontrolcenter.util.SesionActual;
import java.sql.SQLException;


public class KardexService {

    public static final String ROJO = "Rojo";
    public static final String AMARILLO = "Amarillo";
    public static final String VERDE = "Verde";

    private final KardexDao dao = new KardexDao();


    public static String semaforo(int stock, int puntoReorden) {
        if (stock <= 0 || stock < puntoReorden) {
            return ROJO;
        }
        if (stock <= Math.ceil(puntoReorden * 1.25)) {
            return AMARILLO;
        }
        return VERDE;
    }


    public static String etiquetaSemaforo(String semaforo) {
        return switch (semaforo) {
            case ROJO -> "ROJO - Reponer";
            case AMARILLO -> "AMARILLO - Pedir pronto";
            default -> "VERDE - OK";
        };
    }

    public static String etiquetaCorta(String semaforo) {
        return switch (semaforo) {
            case ROJO -> "Reponer";
            case AMARILLO -> "Pedir pronto";
            default -> "OK";
        };
    }

    public String registrarMovimiento(String idFiltro, String tipo, int cantidad) {
        Usuario usuario = SesionActual.getUsuario();
        if (usuario == null) {
            return "No hay una sesion activa.";
        }
        if (!SesionActual.puedeEditar()) {
            return "Su rol es de solo lectura: no puede registrar movimientos.";
        }
        if (idFiltro == null || idFiltro.isBlank()) {
            return "Seleccione un insumo.";
        }
        if ("Ajuste".equals(tipo) ? cantidad < 0 : cantidad <= 0) {
            return "Ajuste".equals(tipo)
                    ? "El stock contado no puede ser negativo."
                    : "La cantidad debe ser mayor que cero.";
        }
        try {
            KardexDao.Resultado r = dao.registrarMovimiento(idFiltro, tipo, cantidad, usuario.getIdUsuario());
            String accion = switch (tipo) {
                case "Entrada" -> "Entrada de Kárdex: +" + r.cantidadRegistrada() + " de " + r.descripcionInsumo()
                        + " (saldo " + r.saldoNuevo() + ")";
                case "Salida" -> "Salida de Kárdex: -" + r.cantidadRegistrada() + " de " + r.descripcionInsumo()
                        + " (saldo " + r.saldoNuevo() + ")";
                default -> "Ajuste de Kárdex: " + r.descripcionInsumo() + " de " + r.saldoAnterior()
                        + " a " + r.saldoNuevo();
            };
            BitacoraService.registrar(accion, "Insumos/Kardex", idFiltro);
            return null;
        } catch (IllegalStateException e) {
            return e.getMessage();
        } catch (SQLException e) {
            e.printStackTrace();
            return "Error de base de datos: " + e.getMessage();
        }
    }
}
