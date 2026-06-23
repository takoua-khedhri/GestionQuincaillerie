package com.myapp.ui.components;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;

public class UIComponentFactory {

    public static JButton createStyledButton(String text, final Color backgroundColor) {
        final JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setBackground(backgroundColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(darkenColor(backgroundColor, 0.8f), 1),
                BorderFactory.createEmptyBorder(10, 15, 10, 15)));

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent evt) {
                button.setBackground(UIComponentFactory.darkenColor(backgroundColor, 0.9f));
            }

            @Override
            public void mouseExited(MouseEvent evt) {
                button.setBackground(backgroundColor);
            }
        });

        return button;
    }

    public static JPanel createTitledPanel(String title, Color borderColor, Component... components) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        panel.setBorder(new TitledBorder(
                new LineBorder(borderColor, 2), title,
                TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION,
                new Font("Segoe UI", Font.BOLD, 12), borderColor));
        panel.setBackground(Color.WHITE);

        for (Component comp : components) {
            panel.add(comp);
        }

        return panel;
    }

    public static JTextField createStyledTextField(int columns) {
        JTextField textField = new JTextField(columns);
        textField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        textField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(5, 8, 5, 8)));

        return textField;
    }

    private static Color darkenColor(Color color, float factor) {
        int r = Math.max((int) (color.getRed() * factor), 0);
        int g = Math.max((int) (color.getGreen() * factor), 0);
        int b = Math.max((int) (color.getBlue() * factor), 0);
        return new Color(r, g, b);
    }
}
