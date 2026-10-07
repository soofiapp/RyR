package com.ryrcontrolcenter.util;

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.Transition;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.ImageView;
import javafx.util.Duration;

/**
 * Reproduce una animacion guardada como "hoja de sprites" (todos los cuadros en
 * una sola imagen, en cuadricula) cambiando el viewport de un ImageView.
 * Los cuadros conservan su transparencia (PNG con canal alfa).
 */
public class SpriteAnimacion extends Transition {

    private final ImageView vista;
    private final int columnas;
    private final int cuadros;
    private final int ancho;
    private final int alto;
    private int ultimo = -1;

    public SpriteAnimacion(ImageView vista, int columnas, int cuadros, int ancho, int alto, double fps) {
        this.vista = vista;
        this.columnas = columnas;
        this.cuadros = cuadros;
        this.ancho = ancho;
        this.alto = alto;
        setCycleDuration(Duration.millis(cuadros * 1000.0 / fps));
        setCycleCount(Animation.INDEFINITE);
        setInterpolator(Interpolator.LINEAR);
        mostrar(0);
    }

    @Override
    protected void interpolate(double k) {
        mostrar(Math.min(cuadros - 1, (int) (k * cuadros)));
    }

    private void mostrar(int i) {
        if (i == ultimo) {
            return;
        }
        ultimo = i;
        vista.setViewport(new Rectangle2D((i % columnas) * ancho, (i / columnas) * alto, ancho, alto));
    }
}
