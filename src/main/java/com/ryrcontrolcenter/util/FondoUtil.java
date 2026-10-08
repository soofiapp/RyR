package com.ryrcontrolcenter.util;

import javafx.geometry.Rectangle2D;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;


public final class FondoUtil {

    private FondoUtil() {
    }

    public static Image cargar(String recurso) {
        return new Image(FondoUtil.class.getResourceAsStream(recurso));
    }


    public static ImageView aplicarFondo(StackPane raiz, Image imagen) {
        ImageView fondo = new ImageView(imagen);
        fondo.setMouseTransparent(true);
        fondo.setManaged(false);
        cubrir(fondo, raiz);
        raiz.getChildren().add(0, fondo);
        return fondo;
    }

    public static void aplicarFondoConVidrio(StackPane raiz, Region tarjeta, Image imagen,
            double desenfoque, double radioEsquinas) {
        aplicarFondo(raiz, imagen);

        ImageView borroso = new ImageView(imagen);
        borroso.setMouseTransparent(true);
        borroso.setManaged(false);
        borroso.setEffect(new GaussianBlur(desenfoque));
        cubrir(borroso, raiz);

        Rectangle recorte = new Rectangle();
        recorte.setArcWidth(radioEsquinas * 2);
        recorte.setArcHeight(radioEsquinas * 2);
        borroso.setClip(recorte);

        Runnable ajustar = () -> {
            recorte.setX(tarjeta.getLayoutX());
            recorte.setY(tarjeta.getLayoutY());
            recorte.setWidth(tarjeta.getWidth());
            recorte.setHeight(tarjeta.getHeight());
        };
        tarjeta.layoutXProperty().addListener((o, a, b) -> ajustar.run());
        tarjeta.layoutYProperty().addListener((o, a, b) -> ajustar.run());
        tarjeta.widthProperty().addListener((o, a, b) -> ajustar.run());
        tarjeta.heightProperty().addListener((o, a, b) -> ajustar.run());

        int pos = Math.max(1, raiz.getChildren().indexOf(tarjeta));
        raiz.getChildren().add(pos, borroso);
        ajustar.run();
    }

    private static void cubrir(ImageView iv, Region contenedor) {
        Runnable r = () -> {
            double ancho = contenedor.getWidth();
            double alto = contenedor.getHeight();
            Image im = iv.getImage();
            if (ancho <= 0 || alto <= 0 || im == null) {
                return;
            }
            double iw = im.getWidth(), ih = im.getHeight();
            double escala = Math.max(ancho / iw, alto / ih);
            double vw = ancho / escala, vh = alto / escala;
            iv.setViewport(new Rectangle2D((iw - vw) / 2, (ih - vh) / 2, vw, vh));
            iv.setFitWidth(ancho);
            iv.setFitHeight(alto);
        };
        contenedor.widthProperty().addListener((o, a, b) -> r.run());
        contenedor.heightProperty().addListener((o, a, b) -> r.run());
        r.run();
    }
}
