package com.myapp.ui;

import com.myapp.db.ConnexionSQLite;
import com.myapp.db.DatabaseManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AjouterVoiture extends JDialog {

    private static final Logger log = LoggerFactory.getLogger(AjouterVoiture.class);

    private JTextField txtMatricule;
    private JButton btnValider;
    private JButton btnAnnuler;
    private JLabel lblExemple;
    private JLabel lblAide;
    
    private Font fontAwesomeSolid;
    private int voitureId = -1;         // ID de la voiture ajoutée
    private String matricule = null;     // Matricule de la voiture ajoutée

    public AjouterVoiture(JFrame parent) {
        super(parent, "Ajouter une Voiture", true);
        
        // Chargement de la police FontAwesome
        this.loadFontAwesome();
        
        // Configuration de la boîte de dialogue - TAILLE AGRANDIE
        this.setSize(600, 450); // Augmenté de 500x350 à 600x450
        this.setLocationRelativeTo(parent);
        this.setResizable(false);
        this.setLayout(new BorderLayout(10, 10));
        this.getContentPane().setBackground(new Color(240, 242, 245));
        
        this.initUI();
    }
    
    // =========================================================================
    // CHARGEMENT DE LA POLICE FONTAWESOME
    // =========================================================================
    private void loadFontAwesome() {
        try (InputStream fontStream = this.getClass().getResourceAsStream("/fonts/fa.ttf")) {
            if (fontStream != null) {
                Font font = Font.createFont(Font.TRUETYPE_FONT, fontStream);
                GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
                ge.registerFont(font);
                this.fontAwesomeSolid = font;
            } else {
                this.fontAwesomeSolid = new Font("SansSerif", Font.PLAIN, 12);
            }
        } catch (Exception e) {
            log.error("Failed to load FontAwesome", e);
            this.fontAwesomeSolid = new Font("SansSerif", Font.PLAIN, 12);
        }
    }
    
    private Font getFontAwesome(int size) {
        return this.fontAwesomeSolid != null ? this.fontAwesomeSolid.deriveFont(Font.PLAIN, (float)size) : new Font("SansSerif", Font.PLAIN, size);
    }

    // Générateur HTML pour Icône + Texte
    private String getHtmlText(String iconCode, String text) {
        String fontName = (this.fontAwesomeSolid != null) ? this.fontAwesomeSolid.getFontName() : "SansSerif";
        return "<html><font face=\"" + fontName + "\">" + iconCode + "</font> " + text + "</html>";
    }

    // =========================================================================
    // INITIALISATION DE L'INTERFACE - VERSION AGRANDIE
    // =========================================================================
    private void initUI() {
        // ===== HEADER =====
        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(new Color(52, 152, 219));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(25, 20, 25, 20)); // Plus de padding
        
        JLabel titleLabel = new JLabel(this.getHtmlText("\uf1b9", " AJOUTER UNE VOITURE"));
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22)); // Police plus grande
        titleLabel.setForeground(Color.WHITE);
        headerPanel.add(titleLabel);
        
        // ===== PANEL PRINCIPAL =====
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBackground(Color.WHITE);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40)); // Plus de padding
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(15, 15, 15, 15); // Plus d'espace
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        
        // ===== 1. TEXTE EXPLICATIF =====
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        JLabel infoLabel = new JLabel("Veuillez saisir la matricule de la voiture");
        infoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16)); // Police plus grande
        infoLabel.setForeground(new Color(100, 100, 100));
        infoLabel.setHorizontalAlignment(JLabel.CENTER);
        mainPanel.add(infoLabel, gbc);
        
        // ===== 2. EXEMPLE VISUEL (GUIDE) =====
        gbc.gridy = 1;
        lblExemple = new JLabel("1234 TN 56");
        lblExemple.setFont(new Font("Segoe UI", Font.BOLD, 32)); // Police beaucoup plus grande
        lblExemple.setForeground(new Color(52, 152, 219));
        lblExemple.setHorizontalAlignment(JLabel.CENTER);
        lblExemple.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(52, 152, 219), 3), // Bordure plus épaisse
            BorderFactory.createEmptyBorder(20, 40, 20, 40) // Plus de padding
        ));
        mainPanel.add(lblExemple, gbc);
        
        // ===== 3. CHAMP DE SAISIE =====
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbc.gridx = 0;
        JLabel lblMatricule = new JLabel("Matricule :");
        lblMatricule.setFont(new Font("Segoe UI", Font.BOLD, 16)); // Police plus grande
        lblMatricule.setForeground(new Color(52, 73, 94));
        mainPanel.add(lblMatricule, gbc);
        
        gbc.gridx = 1;
        txtMatricule = new JTextField(20); // Plus de colonnes
        txtMatricule.setFont(new Font("Segoe UI", Font.PLAIN, 18)); // Police plus grande
        txtMatricule.setHorizontalAlignment(JTextField.CENTER);
        txtMatricule.setPreferredSize(new Dimension(300, 50)); // Taille fixe plus grande
        txtMatricule.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 2),
            BorderFactory.createEmptyBorder(15, 20, 15, 20) // Plus de padding
        ));
        
        // Placeholder
        txtMatricule.setText("1234 TN 56");
        txtMatricule.setForeground(Color.GRAY);
        
        // Gestion du placeholder
        txtMatricule.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (txtMatricule.getText().equals("1234 TN 56")) {
                    txtMatricule.setText("");
                    txtMatricule.setForeground(Color.BLACK);
                }
            }
            
            @Override
            public void focusLost(FocusEvent e) {
                if (txtMatricule.getText().isEmpty()) {
                    txtMatricule.setText("1234 TN 56");
                    txtMatricule.setForeground(Color.GRAY);
                }
            }
        });
        
        // Validation en temps réel
        txtMatricule.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                validerFormat();
            }
            
            @Override
            public void removeUpdate(DocumentEvent e) {
                validerFormat();
            }
            
            @Override
            public void changedUpdate(DocumentEvent e) {
                validerFormat();
            }
        });
        
        // Formatage automatique en majuscules
        txtMatricule.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                char c = e.getKeyChar();
                if (Character.isLowerCase(c)) {
                    e.setKeyChar(Character.toUpperCase(c));
                }
            }
        });
        
        mainPanel.add(txtMatricule, gbc);
        
        // ===== 4. TEXTE D'AIDE =====
        gbc.gridy = 3;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        lblAide = new JLabel("Format suggéré : chiffres + lettres (ex: 1234TN56, 1234 TN 56)");
        lblAide.setFont(new Font("Segoe UI", Font.PLAIN, 14)); // Police plus grande
        lblAide.setForeground(new Color(150, 150, 150));
        lblAide.setHorizontalAlignment(JLabel.CENTER);
        mainPanel.add(lblAide, gbc);
        
        // ===== 5. BOUTONS =====
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 15)); // Plus d'espace
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 25, 0)); // Plus de padding en bas
        
        btnValider = createButton(this.getHtmlText("\uf00c", " Valider"), new Color(46, 204, 113));
        btnAnnuler = createButton(this.getHtmlText("\uf00d", " Annuler"), new Color(231, 76, 60));
        
        btnValider.addActionListener(e -> validerEtAjouter());
        btnAnnuler.addActionListener(e -> {
            voitureId = -1;
            matricule = null;
            dispose();
        });
        
        buttonPanel.add(btnValider);
        buttonPanel.add(btnAnnuler);
        
        // ===== ASSEMBLAGE FINAL =====
        this.add(headerPanel, BorderLayout.NORTH);
        this.add(mainPanel, BorderLayout.CENTER);
        this.add(buttonPanel, BorderLayout.SOUTH);
    }
    
    // =========================================================================
    // CRÉATION DE BOUTONS STYLISÉS - VERSION PLUS GRANDE
    // =========================================================================
    private JButton createButton(String text, Color backgroundColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 16)); // Police plus grande
        button.setBackground(backgroundColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createEmptyBorder(15, 40, 15, 40)); // Plus de padding
        button.setPreferredSize(new Dimension(180, 60)); // Boutons plus grands
        
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent evt) {
                button.setBackground(darkenColor(backgroundColor, 0.9f));
            }
            
            @Override
            public void mouseExited(MouseEvent evt) {
                button.setBackground(backgroundColor);
            }
            
            @Override
            public void mousePressed(MouseEvent evt) {
                button.setBackground(darkenColor(backgroundColor, 0.8f));
            }
            
            @Override
            public void mouseReleased(MouseEvent evt) {
                button.setBackground(darkenColor(backgroundColor, 0.9f));
            }
        });
        
        return button;
    }
    
    private Color darkenColor(Color color, float factor) {
        int r = Math.max((int) (color.getRed() * factor), 0);
        int g = Math.max((int) (color.getGreen() * factor), 0);
        int b = Math.max((int) (color.getBlue() * factor), 0);
        return new Color(r, g, b);
    }
    
    // =========================================================================
    // VALIDATION DU FORMAT EN TEMPS RÉEL
    // =========================================================================
    private void validerFormat() {
        String texte = txtMatricule.getText();
        
        // Ignorer le placeholder
        if (texte.isEmpty() || texte.equals("1234 TN 56")) {
            txtMatricule.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 2),
                BorderFactory.createEmptyBorder(15, 20, 15, 20)
            ));
            lblAide.setForeground(new Color(150, 150, 150));
            lblAide.setText("Format suggéré : chiffres + lettres (ex: 1234TN56, 1234 TN 56)");
            return;
        }
        
        // Supprimer les espaces et tirets pour la validation
        String matricule = texte.replaceAll("[\\s-]", "");
        
        // Vérifier si le format est valide (chiffres et lettres uniquement)
        if (matricule.matches("[A-Z0-9]+")) {
            // Format valide
            txtMatricule.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(46, 204, 113), 3),
                BorderFactory.createEmptyBorder(13, 18, 13, 18)
            ));
            lblAide.setForeground(new Color(46, 204, 113));
            lblAide.setText("✓ Format valide - Vous pouvez enregistrer");
        } else {
            // Format invalide
            txtMatricule.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(231, 76, 60), 3),
                BorderFactory.createEmptyBorder(13, 18, 13, 18)
            ));
            lblAide.setForeground(new Color(231, 76, 60));
            lblAide.setText("✗ Format invalide - Utilisez chiffres et lettres uniquement");
        }
    }
    
    // =========================================================================
    // VALIDATION FINALE ET AJOUT EN BASE DE DONNÉES
    // =========================================================================
    private void validerEtAjouter() {
        String saisie = txtMatricule.getText().trim();
        
        // Vérifier que le champ n'est pas vide ou placeholder
        if (saisie.isEmpty() || saisie.equals("1234 TN 56")) {
            JOptionPane.showMessageDialog(this,
                "Veuillez saisir la matricule de la voiture",
                "Champ requis",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Nettoyer la matricule (enlever espaces et tirets, mettre en majuscules)
        String matriculePropre = saisie.replaceAll("[\\s-]", "").toUpperCase();
        
        // Vérifier le format (chiffres et lettres uniquement)
        if (!matriculePropre.matches("[A-Z0-9]+")) {
            JOptionPane.showMessageDialog(this,
                "Format de matricule invalide !\nUtilisez uniquement des chiffres et des lettres.",
                "Erreur de format",
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Vérifier si la matricule existe déjà dans la base
        if (DatabaseManager.matriculeExiste(matriculePropre)) {
            JOptionPane.showMessageDialog(this,
                "Cette matricule existe déjà dans la base de données !",
                "Matricule en double",
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Ajouter la voiture à la base de données et récupérer son ID
        int id = DatabaseManager.ajouterVoitureRetourId(matriculePropre);
        
        if (id > 0) {
            // Succès
            this.voitureId = id;
            this.matricule = matriculePropre;
            
            JOptionPane.showMessageDialog(this,
                "Voiture ajoutée avec succès !\nMatricule: " + matriculePropre,
                "Succès",
                JOptionPane.INFORMATION_MESSAGE);
            
            dispose(); // Fermer la boîte de dialogue
        } else {
            // Erreur
            JOptionPane.showMessageDialog(this,
                "Erreur lors de l'ajout de la voiture dans la base de données.",
                "Erreur",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // =========================================================================
    // GETTERS POUR RÉCUPÉRER LES RÉSULTATS
    // =========================================================================
    public int getVoitureId() {
        return voitureId;
    }
    
    public String getMatricule() {
        return matricule;
    }
    
    // =========================================================================
    // MAIN POUR TEST
    // =========================================================================
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                JFrame testFrame = new JFrame();
                testFrame.setSize(800, 600);
                testFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                testFrame.setLocationRelativeTo(null);
                
                AjouterVoiture dialog = new AjouterVoiture(testFrame);
                dialog.setVisible(true);
                
                if (dialog.getVoitureId() > 0) {
                    log.info("Voiture ajoutée - ID: {}, Matricule: {}", dialog.getVoitureId(), dialog.getMatricule());
                }
            } catch (Exception e) {
                log.error("Failed to start AjouterVoiture", e);
            }
        });
    }
}