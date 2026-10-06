package com.ryrcontrolcenter.util;

import java.io.File;
import javafx.stage.FileChooser;
import javafx.stage.Window;


public class ExportacionUtil {


    public static File elegirArchivo(Window ventana, String nombreBase, boolean pdf) {
        String ext = pdf ? ".pdf" : ".xlsx";
        FileChooser fc = new FileChooser();
        fc.setTitle(pdf ? "Guardar reporte en PDF" : "Guardar reporte en Excel");
        fc.setInitialFileName(nombreBase + "_" + java.time.LocalDate.now() + ext);
        fc.getExtensionFilters().add(pdf
                ? new FileChooser.ExtensionFilter("Documento PDF (*.pdf)", "*.pdf")
                : new FileChooser.ExtensionFilter("Libro de Excel (*.xlsx)", "*.xlsx"));
        File home = new File(System.getProperty("user.home"));
        File documentos = new File(home, "Documents");
        File inicial = documentos.isDirectory() ? documentos : home;
        if (inicial.isDirectory()) {
            fc.setInitialDirectory(inicial);
        }
        File f = fc.showSaveDialog(ventana);
        if (f != null && !f.getName().toLowerCase().endsWith(ext)) {
            f = new File(f.getParentFile(), f.getName() + ext);
        }
        return f;
    }

    public static String usuarioActual() {
        var u = SesionActual.getUsuario();
        return u == null ? "" : u.getNombreCompleto();
    }
}
