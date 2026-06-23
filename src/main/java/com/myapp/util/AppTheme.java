package com.myapp.util;

import com.formdev.flatlaf.FlatLightLaf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Font;

public final class AppTheme {

    private static final Logger log = LoggerFactory.getLogger(AppTheme.class);

    public static final Color PRIMARY = new Color(41, 128, 185);
    public static final Color ACCENT = new Color(46, 204, 113);
    public static final Color ACCENT_DARK = new Color(39, 174, 96);
    public static final Color DANGER = new Color(231, 76, 60);
    public static final Color DANGER_DARK = new Color(192, 57, 43);
    public static final Color WARNING = new Color(241, 196, 15);
    public static final Color INFO = new Color(52, 152, 219);
    public static final Color INFO_DARK = new Color(41, 128, 185);
    public static final Color DARK = new Color(44, 62, 80);
    public static final Color LIGHT = new Color(236, 240, 241);
    public static final Color PURPLE = new Color(155, 89, 182);
    public static final Color DOCUMENTS = new Color(52, 73, 94);
    public static final Color FACTURES = new Color(39, 174, 96);
    public static final Color FOURNISSEUR = new Color(52, 152, 219);
    public static final Color CLIENT = new Color(46, 204, 113);
    public static final Color ACHATS = new Color(230, 126, 34);

    public static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 28);
    public static final Font FONT_SUBTITLE = new Font("Segoe UI", Font.PLAIN, 14);
    public static final Font FONT_BUTTON = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_TABLE = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_TABLE_HEADER = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_LABEL = new Font("Segoe UI", Font.BOLD, 12);
    public static final Font FONT_FIELD = new Font("Segoe UI", Font.PLAIN, 14);

    private AppTheme() {}

    public static void init() {
        try {
            UIManager.put("OptionPane.yesButtonText", "Oui");
            UIManager.put("OptionPane.noButtonText", "Non");
            UIManager.put("OptionPane.cancelButtonText", "Annuler");
            UIManager.put("OptionPane.okButtonText", "OK");
            UIManager.put("FileChooser.openButtonText", "Ouvrir");
            UIManager.put("FileChooser.saveButtonText", "Enregistrer");
            UIManager.put("FileChooser.cancelButtonText", "Annuler");

            FlatLightLaf.setup();
            log.info("Theme FlatLaf initialise");
        } catch (Exception e) {
            log.warn("Impossible d'initialiser FlatLaf, utilisation du theme systeme", e);
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ex) {
                log.error("Erreur initialisation theme systeme", ex);
            }
        }
    }

    public static Color darken(Color color, float factor) {
        int r = Math.max((int) (color.getRed() * factor), 0);
        int g = Math.max((int) (color.getGreen() * factor), 0);
        int b = Math.max((int) (color.getBlue() * factor), 0);
        return new Color(r, g, b);
    }
}
