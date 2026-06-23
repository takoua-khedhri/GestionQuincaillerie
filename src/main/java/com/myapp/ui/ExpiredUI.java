package com.myapp.ui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class ExpiredUI extends JFrame {
    
    public ExpiredUI() {
        this.setTitle("Démo expirée");
        this.setExtendedState(JFrame.MAXIMIZED_BOTH); // Corrigé de '6'
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Corrigé de '3'
        this.setResizable(true);
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        this.initUI();
    }

    private void initUI() {
        JPanel panelPrincipal = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                Color color1 = new Color(220, 53, 69);
                Color color2 = new Color(200, 35, 51);
                GradientPaint gp = new GradientPaint(0.0F, 0.0F, color1, (float) this.getWidth(), (float) this.getHeight(), color2);
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, this.getWidth(), this.getHeight());
            }
        };
        panelPrincipal.setLayout(new GridBagLayout());
        panelPrincipal.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS)); // Corrigé de '1'
        contentPanel.setBackground(Color.WHITE);
        contentPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(180, 180, 180), 2),
                BorderFactory.createEmptyBorder(50, 80, 50, 80)));
        contentPanel.setPreferredSize(new Dimension(1000, 700));
        contentPanel.setMaximumSize(new Dimension(1100, 800));

        // --- Icône ---
        JPanel iconPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        iconPanel.setBackground(Color.WHITE);
        iconPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 30, 0));
        
        JLabel iconLabel = new JLabel("⏰");
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 70));
        iconLabel.setForeground(new Color(220, 53, 69));
        iconPanel.add(iconLabel);

        // --- Titre ---
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        titlePanel.setBackground(Color.WHITE);
        titlePanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 30, 0));
        
        JLabel lblTitle = new JLabel("<html><center><h1 style='color:#dc3545;'>Version Démonstration Expirée</h1></center></html>", JLabel.CENTER);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titlePanel.add(lblTitle);

        // --- Message ---
        JPanel messagePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        messagePanel.setBackground(Color.WHITE);
        messagePanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 40, 0));
        
        JLabel lblMessage = new JLabel("<html><center><div style='font-size:16px; color:#555; line-height:1.6; max-width:800px;'><b>Votre période d'essai de 7 jours est terminée.</b><br><br>Pour continuer à utiliser l'application, veuillez suivre ces étapes :<br><br><div style='text-align:left; margin-left:120px; margin-right:120px; line-height:1.8;'>1. <b>Contacter le développeur</b> pour obtenir une licence<br>2. <b>Procéder au paiement</b> de la licence complète<br>3. <b>Recevoir votre code d'activation</b> par email<br>4. <b>Réactiver le système</b> avec le code reçu</div></div></center></html>", JLabel.CENTER);
        lblMessage.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        messagePanel.add(lblMessage);

        // --- Bouton Quitter ---
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 40, 0));
        
        final JButton btnQuit = new JButton("QUITTER L'APPLICATION");
        btnQuit.setFont(new Font("Segoe UI", Font.BOLD, 18));
        btnQuit.setBackground(new Color(220, 53, 69));
        btnQuit.setForeground(Color.WHITE);
        btnQuit.setFocusPainted(false);
        btnQuit.setBorderPainted(false);
        btnQuit.setOpaque(true);
        btnQuit.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnQuit.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(180, 50, 50), 2),
                BorderFactory.createEmptyBorder(12, 40, 12, 40)));
        btnQuit.setPreferredSize(new Dimension(320, 55));
        btnQuit.setMinimumSize(new Dimension(320, 55));
        btnQuit.setMaximumSize(new Dimension(320, 55));

        btnQuit.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent evt) {
                btnQuit.setBackground(new Color(200, 35, 51));
                btnQuit.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(160, 30, 30), 2),
                        BorderFactory.createEmptyBorder(12, 40, 12, 40)));
            }

            @Override
            public void mouseExited(MouseEvent evt) {
                btnQuit.setBackground(new Color(220, 53, 69));
                btnQuit.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(180, 50, 50), 2),
                        BorderFactory.createEmptyBorder(12, 40, 12, 40)));
            }
        });

        btnQuit.addActionListener((e) -> {
            System.exit(0);
        });

        buttonPanel.add(btnQuit);

        // --- Contact ---
        JPanel contactPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        contactPanel.setBackground(Color.WHITE);
        
        JLabel lblContact = new JLabel("<html><center><div style='font-size:14px; color:#777;'>\ud83d\udce7 Contact : support@myapp.com | \ud83d\udcde Tél : +33 1 23 45 67 89</div></center></html>", JLabel.CENTER);
        lblContact.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        contactPanel.add(lblContact);

        // --- Assemblage ---
        contentPanel.add(Box.createVerticalStrut(10));
        contentPanel.add(iconPanel);
        contentPanel.add(Box.createVerticalStrut(20));
        contentPanel.add(titlePanel);
        contentPanel.add(Box.createVerticalStrut(25));
        contentPanel.add(messagePanel);
        contentPanel.add(Box.createVerticalStrut(30));
        contentPanel.add(buttonPanel);
        contentPanel.add(Box.createVerticalStrut(25));
        contentPanel.add(contactPanel);
        contentPanel.add(Box.createVerticalStrut(10));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0D;
        gbc.weighty = 1.0D;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        
        panelPrincipal.add(contentPanel, gbc);
        this.add(panelPrincipal);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new ExpiredUI().setVisible(true);
        });
    }
}