package com.myapp.ui;

import com.myapp.db.ConnexionSQLite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

public class ArticlesRuptureUI extends JFrame {

    private JTable tableArticles;
    private DefaultTableModel model;
    private JLabel lblTitre;
    private JButton btnRetour;
    private Font fontAwesomeSolid;
    private String typeRupture; // "rupture" ou "faible"
    private int seuil;

    public ArticlesRuptureUI(String type, int seuil) {
        this.typeRupture = type;
        this.seuil = seuil;
        
        this.loadFontAwesome();
        
        this.setTitle(type.equals("rupture") ? "Articles en Rupture de Stock" : "Articles à Stock Faible");
        this.setExtendedState(JFrame.MAXIMIZED_BOTH);
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.setLocationRelativeTo(null);
        this.setLayout(new BorderLayout(10, 10));
        this.getContentPane().setBackground(new Color(240, 242, 245));
        
        this.initUI();
        this.chargerArticles();
    }

    private void loadFontAwesome() {
        try {
            String path = "/fonts/fa.ttf";
            InputStream fontStream = this.getClass().getResourceAsStream(path);
            if (fontStream != null) {
                Font font = Font.createFont(Font.TRUETYPE_FONT, fontStream);
                GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
                ge.registerFont(font);
                this.fontAwesomeSolid = font;
                fontStream.close();
            } else {
                this.fontAwesomeSolid = new Font("SansSerif", Font.PLAIN, 12);
            }
        } catch (Exception e) {
            e.printStackTrace();
            this.fontAwesomeSolid = new Font("SansSerif", Font.PLAIN, 12);
        }
    }
    
    private Font getFontAwesome(int size) {
        return this.fontAwesomeSolid != null ? this.fontAwesomeSolid.deriveFont(Font.PLAIN, (float)size) : new Font("SansSerif", Font.PLAIN, size);
    }

    private String getHtmlText(String iconCode, String text) {
        String fontName = (this.fontAwesomeSolid != null) ? this.fontAwesomeSolid.getFontName() : "SansSerif";
        return "<html><font face=\"" + fontName + "\">" + iconCode + "</font> " + text + "</html>";
    }

    private void initUI() {
        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(44, 62, 80));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        
        // Bouton retour
        btnRetour = new JButton(this.getHtmlText("\uf060", "Retour au Dashboard"));
        btnRetour.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnRetour.setBackground(new Color(52, 152, 219));
        btnRetour.setForeground(Color.WHITE);
        btnRetour.setFocusPainted(false);
        btnRetour.setBorderPainted(false);
        btnRetour.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnRetour.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        btnRetour.addActionListener(e -> {
            new AdminDashboard().setVisible(true);
            this.dispose();
        });
        
        // Titre
        String titreTexte = typeRupture.equals("rupture") ? "ARTICLES EN RUPTURE DE STOCK" : "ARTICLES À STOCK FAIBLE (≤ " + seuil + ")";
        String iconCode = typeRupture.equals("rupture") ? "\uf071" : "\uf06a";
        Color titreCouleur = typeRupture.equals("rupture") ? new Color(231, 76, 60) : new Color(241, 196, 15);
        
        lblTitre = new JLabel(this.getHtmlText(iconCode, titreTexte), JLabel.CENTER);
        lblTitre.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTitre.setForeground(titreCouleur);
        
        headerPanel.add(btnRetour, BorderLayout.WEST);
        headerPanel.add(lblTitre, BorderLayout.CENTER);
        
        // Table
        String[] colonnes = {"ID", "Code", "Désignation", "Prix", "Stock", "TVA", "Seuil"};
        model = new DefaultTableModel(colonnes, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        tableArticles = new JTable(model);
        tableArticles.setRowHeight(35);
        tableArticles.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tableArticles.setSelectionBackground(new Color(220, 240, 255));
        tableArticles.setGridColor(new Color(230, 230, 230));
        
        // Style des colonnes
        tableArticles.getColumnModel().getColumn(0).setPreferredWidth(60);  // ID
        tableArticles.getColumnModel().getColumn(1).setPreferredWidth(100); // Code
        tableArticles.getColumnModel().getColumn(2).setPreferredWidth(300); // Désignation
        tableArticles.getColumnModel().getColumn(3).setPreferredWidth(100); // Prix
        tableArticles.getColumnModel().getColumn(4).setPreferredWidth(80);  // Stock
        tableArticles.getColumnModel().getColumn(5).setPreferredWidth(60);  // TVA
        tableArticles.getColumnModel().getColumn(6).setPreferredWidth(80);  // Seuil
        
        // Renderer pour colorer la ligne selon le stock
        tableArticles.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                
                if (!isSelected) {
                    int stock = 0;
                    try {
                        stock = Integer.parseInt(table.getValueAt(row, 4).toString());
                    } catch (Exception e) {}
                    
                    if (stock == 0) {
                        c.setBackground(new Color(255, 235, 235)); // Rouge très clair pour rupture
                    } else if (stock <= seuil) {
                        c.setBackground(new Color(255, 250, 225)); // Jaune très clair pour stock faible
                    } else {
                        c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 248, 248));
                    }
                } else {
                    c.setBackground(table.getSelectionBackground());
                }
                
                // Alignement
                if (column >= 3 && column <= 6) {
                    setHorizontalAlignment(JLabel.RIGHT);
                } else {
                    setHorizontalAlignment(JLabel.LEFT);
                }
                
                return c;
            }
        });
        
        // Style de l'en-tête
        JTableHeader header = tableArticles.getTableHeader();
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                this.setBackground(new Color(52, 73, 94));
                this.setForeground(Color.WHITE);
                this.setFont(new Font("Segoe UI", Font.BOLD, 13));
                this.setHorizontalAlignment(JLabel.CENTER);
                this.setBorder(BorderFactory.createEmptyBorder(8, 5, 8, 5));
                return this;
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(tableArticles);
        scrollPane.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)), 
            "Liste des articles"
        ));
        
        // Panel des statistiques
        JPanel statsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        statsPanel.setBackground(Color.WHITE);
        statsPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        
        JLabel lblTotal = new JLabel("Total : 0 article");
        lblTotal.setFont(new Font("Segoe UI", Font.BOLD, 14));
        statsPanel.add(lblTotal);
        
        // Mettre à jour le total après chargement
        // On le fera dans chargerArticles()
        
        // Assemblage
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        mainPanel.setBackground(new Color(240, 242, 245));
        mainPanel.add(statsPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        
        this.add(headerPanel, BorderLayout.NORTH);
        this.add(mainPanel, BorderLayout.CENTER);
    }

    private void chargerArticles() {
        model.setRowCount(0);
        
        String sql;
        if (typeRupture.equals("rupture")) {
            sql = "SELECT id, code, designation, prix, stock, tva FROM Articles WHERE stock = 0 ORDER BY designation";
        } else {
            sql = "SELECT id, code, designation, prix, stock, tva FROM Articles WHERE stock > 0 AND stock <= ? ORDER BY stock, designation";
        }
        
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            
            if (!typeRupture.equals("rupture")) {
                pst.setInt(1, seuil);
            }
            
            try (ResultSet rs = pst.executeQuery()) {
                int count = 0;
                while (rs.next()) {
                    model.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("code") != null ? rs.getString("code") : "",
                        rs.getString("designation"),
                        String.format("%.3f", rs.getDouble("prix")),
                        rs.getInt("stock"),
                        rs.getInt("tva") + " %",
                        typeRupture.equals("rupture") ? "0" : String.valueOf(seuil)
                    });
                    count++;
                }
                
                // Mettre à jour le label de total
                JPanel mainPanel = (JPanel) this.getContentPane().getComponent(1);
                JPanel statsPanel = (JPanel) mainPanel.getComponent(0);
                JLabel lblTotal = (JLabel) statsPanel.getComponent(0);
                lblTotal.setText("Total : " + count + " article" + (count > 1 ? "s" : ""));
                
                if (count == 0) {
                    JOptionPane.showMessageDialog(this, 
                        "Aucun article " + (typeRupture.equals("rupture") ? "en rupture" : "à stock faible"), 
                        "Information", 
                        JOptionPane.INFORMATION_MESSAGE);
                }
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erreur lors du chargement des articles: " + e.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                // Test avec rupture
                new ArticlesRuptureUI("rupture", 0).setVisible(true);
                // Test avec stock faible (seuil = 5)
                // new ArticlesRuptureUI("faible", 5).setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}