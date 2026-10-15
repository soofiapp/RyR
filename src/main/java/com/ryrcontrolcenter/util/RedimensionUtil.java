package com.ryrcontrolcenter.util;

import java.util.function.BooleanSupplier;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

public final class RedimensionUtil {

    private static final double BORDE = 6;
    private static final int IZQ = 1, DER = 2, ARR = 4, ABA = 8;

    private RedimensionUtil() {
    }

    public static void instalar(Stage stage, Node raiz, BooleanSupplier permitido) {
        final double[] ini = new double[6]; // mouseX, mouseY, x, y, ancho, alto
        final int[] dir = {0};
        final boolean[] arrastrando = {false};

        raiz.addEventFilter(MouseEvent.MOUSE_MOVED, e -> {
            Scene sc = raiz.getScene();
            if (sc == null) {
                return;
            }
            if (!permitido.getAsBoolean()) {
                sc.setCursor(Cursor.DEFAULT);
                return;
            }
            sc.setCursor(cursorPara(direccion(e, sc)));
        });

        raiz.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            Scene sc = raiz.getScene();
            if (sc == null || !permitido.getAsBoolean()) {
                return;
            }
            dir[0] = direccion(e, sc);
            if (dir[0] != 0) {
                arrastrando[0] = true;
                ini[0] = e.getScreenX();
                ini[1] = e.getScreenY();
                ini[2] = stage.getX();
                ini[3] = stage.getY();
                ini[4] = stage.getWidth();
                ini[5] = stage.getHeight();
                e.consume();
            }
        });

        raiz.addEventFilter(MouseEvent.MOUSE_DRAGGED, e -> {
            if (!arrastrando[0]) {
                return;
            }
            double dx = e.getScreenX() - ini[0];
            double dy = e.getScreenY() - ini[1];
            double minW = stage.getMinWidth();
            double minH = stage.getMinHeight();
            double x = ini[2], y = ini[3], w = ini[4], h = ini[5];

            if ((dir[0] & DER) != 0) {
                w = Math.max(minW, ini[4] + dx);
            }
            if ((dir[0] & IZQ) != 0) {
                w = Math.max(minW, ini[4] - dx);
                x = ini[2] + (ini[4] - w);
            }
            if ((dir[0] & ABA) != 0) {
                h = Math.max(minH, ini[5] + dy);
            }
            if ((dir[0] & ARR) != 0) {
                h = Math.max(minH, ini[5] - dy);
                y = ini[3] + (ini[5] - h);
            }
            stage.setX(x);
            stage.setY(y);
            stage.setWidth(w);
            stage.setHeight(h);
            e.consume();
        });

        raiz.addEventFilter(MouseEvent.MOUSE_RELEASED, e -> {
            if (arrastrando[0]) {
                arrastrando[0] = false;
                e.consume();
            }
        });
    }

    private static int direccion(MouseEvent e, Scene sc) {
        double x = e.getSceneX(), y = e.getSceneY();
        int d = 0;
        if (x < BORDE) {
            d |= IZQ;
        } else if (x > sc.getWidth() - BORDE) {
            d |= DER;
        }
        if (y < BORDE) {
            d |= ARR;
        } else if (y > sc.getHeight() - BORDE) {
            d |= ABA;
        }
        return d;
    }

    private static Cursor cursorPara(int d) {
        switch (d) {
            case IZQ:
                return Cursor.W_RESIZE;
            case DER:
                return Cursor.E_RESIZE;
            case ARR:
                return Cursor.N_RESIZE;
            case ABA:
                return Cursor.S_RESIZE;
            case IZQ | ARR:
                return Cursor.NW_RESIZE;
            case DER | ARR:
                return Cursor.NE_RESIZE;
            case IZQ | ABA:
                return Cursor.SW_RESIZE;
            case DER | ABA:
                return Cursor.SE_RESIZE;
            default:
                return Cursor.DEFAULT;
        }
    }
}
