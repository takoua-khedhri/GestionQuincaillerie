package com.myapp.ui;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.InputStream;

public class FontHelper {
    
    private static Font fontAwesome;

    // Cette méthode charge la police une seule fois et la renvoie
    public static Font getIconFont(float size) {
        if (fontAwesome == null) {
            try {
                // On cherche le fichier dans le dossier qu'on a créé à l'étape 1
                InputStream is = FontHelper.class.getResourceAsStream("/resources/fonts/fa.otf");
                
                if (is == null) {
                    System.err.println("ERREUR : Impossible de trouver src/resources/fonts/fa.otf");
                    // En cas d'erreur, on renvoie une police standard
                    return new Font("SansSerif", Font.PLAIN, (int)size);
                }
                
                fontAwesome = Font.createFont(Font.TRUETYPE_FONT, is);
                GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
                ge.registerFont(fontAwesome);
                is.close();
                
            } catch (Exception e) {
                e.printStackTrace();
                return new Font("SansSerif", Font.PLAIN, (int)size);
            }
        }
        // On renvoie la police à la taille demandée
        return fontAwesome.deriveFont(size);
    }
}