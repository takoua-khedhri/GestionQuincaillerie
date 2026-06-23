package com.myapp.util;

import javax.swing.*;
import java.awt.*;

public final class UIUtils {

    private UIUtils() {}

    public static boolean showConfirmDialog(Component parent, String message, String title) {
        Object[] options = {"Oui", "Non"};
        int result = JOptionPane.showOptionDialog(
                parent, message, title,
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE,
                null, options, options[1]);
        return result == 0;
    }

    public static void showSuccessMessage(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Succes", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void showErrorMessage(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Erreur", JOptionPane.ERROR_MESSAGE);
    }

    public static void showInfoMessage(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Information", JOptionPane.INFORMATION_MESSAGE);
    }

    public static Color darkenColor(Color color, float factor) {
        int r = Math.max(0, (int) (color.getRed() * (1.0f - factor)));
        int g = Math.max(0, (int) (color.getGreen() * (1.0f - factor)));
        int b = Math.max(0, (int) (color.getBlue() * (1.0f - factor)));
        return new Color(r, g, b);
    }

    public static JButton createStyledButton(String text, Color backgroundColor) {
        JButton button = new JButton(text);
        button.setFont(AppTheme.FONT_BUTTON);
        button.setBackground(backgroundColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(AppTheme.darken(backgroundColor, 0.85f));
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(backgroundColor);
            }
        });
        return button;
    }

    public static JTextField createStyledTextField(int columns) {
        JTextField field = new JTextField(columns);
        field.setFont(AppTheme.FONT_FIELD);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(5, 8, 5, 8)));
        return field;
    }
}
