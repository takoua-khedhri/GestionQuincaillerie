package com.myapp.ui;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FontHelper {

    private static final Logger log = LoggerFactory.getLogger(FontHelper.class);
    
    private static Font fontAwesome;

    // Cette méthode charge la police une seule fois et la renvoie
    public static Font getIconFont(float size) {
        if (fontAwesome == null) {
            try (InputStream is = FontHelper.class.getResourceAsStream("/resources/fonts/fa.otf")) {
                if (is == null) {
                    log.error("Cannot find font resource /resources/fonts/fa.otf");
                    return new Font("SansSerif", Font.PLAIN, (int)size);
                }

                fontAwesome = Font.createFont(Font.TRUETYPE_FONT, is);
                GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
                ge.registerFont(fontAwesome);

            } catch (Exception e) {
                log.error("Failed to load FontAwesome", e);
                return new Font("SansSerif", Font.PLAIN, (int)size);
            }
        }
        // On renvoie la police à la taille demandée
        return fontAwesome.deriveFont(size);
    }
}