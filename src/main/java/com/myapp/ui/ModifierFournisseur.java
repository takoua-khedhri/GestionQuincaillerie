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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
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
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModifierFournisseur extends JFrame {

    private static final Logger log = LoggerFactory.getLogger(ModifierFournisseur.class);

    private JTextField txtNom;
    private JTextField txtPrenom;
    private JTextField txtTelephone;
    private JTextField txtAdresse;
    private JTextField txtMatriculeFiscale;
    private JTextField txtEmail;
    private JButton btnValider;
    private JButton btnAnnuler;
    
    private ListeFournisseurs parent;
    private int fournisseurId;
    
    private Font fontAwesomeSolid;

    public ModifierFournisseur(ListeFournisseurs parent, int id, String nom, String prenom, 
                               String telephone, String adresse, String matricule, String email) {
        this.parent = parent;
        this.fournisseurId = id;
        
        // 1. Chargement Police
        this.loadFontAwesome();
        
        this.setTitle("Modifier Fournisseur");
        this.setExtendedState(JFrame.MAXIMIZED_BOTH);
        this.setLocationRelativeTo(null);
        this.setLayout(new BorderLayout(10, 10));
        this.getContentPane().setBackground(new Color(240, 242, 245));
        
        this.initUI(nom, prenom, telephone, adresse, matricule, email);
        this.setVisible(true);
    }

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

    private boolean validerMatriculeFiscal(String matricule) {
        if (matricule != null && !matricule.trim().isEmpty()) {
            String pattern = "^\\d{9}[A-Za-z]\\d{3}$";
            return matricule.matches(pattern);
        }
        return true; 
    }

    private String formaterMatriculeFiscal(String matricule) {
        return (matricule != null && !matricule.trim().isEmpty()) ? matricule.replaceAll("\\s+", "").toUpperCase() : "";
    }

    private boolean validerEmail(String email) {
        if (email != null && !email.trim().isEmpty()) {
            String pattern = "^[A-Za-z0-9+_.-]+@(.+)$";
            return email.matches(pattern);
        }
        return true;
    }

    private boolean validerTelephone(String telephone) {
        if (telephone != null && !telephone.trim().isEmpty()) {
            return telephone.matches("\\d{8,12}");
        }
        return true;
    }

    private void validerMatriculeEnTempsReel() {
        String matricule = this.txtMatriculeFiscale.getText().trim();
        if (matricule.isEmpty()) {
            this.txtMatriculeFiscale.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(200, 200, 200)),
                    BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        } else {
            if (this.validerMatriculeFiscal(matricule)) {
                this.txtMatriculeFiscale.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(46, 204, 113), 2),
                        BorderFactory.createEmptyBorder(6, 8, 6, 8)));
            } else {
                this.txtMatriculeFiscale.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(231, 76, 60), 2),
                        BorderFactory.createEmptyBorder(6, 8, 6, 8)));
            }
        }
    }

    private void validerEmailEnTempsReel() {
        String email = this.txtEmail.getText().trim();
        if (email.isEmpty()) {
            this.txtEmail.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(200, 200, 200)),
                    BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        } else {
            if (this.validerEmail(email)) {
                this.txtEmail.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(46, 204, 113), 2),
                        BorderFactory.createEmptyBorder(6, 8, 6, 8)));
            } else {
                this.txtEmail.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(231, 76, 60), 2),
                        BorderFactory.createEmptyBorder(6, 8, 6, 8)));
            }
        }
    }

    private void appliquerMasqueMatricule() {
        this.txtMatriculeFiscale.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                char c = e.getKeyChar();
                String text = ModifierFournisseur.this.txtMatriculeFiscale.getText();
                
                int length = text.length();
                if (length < 9) {
                    if (!Character.isDigit(c)) e.consume();
                } else if (length == 9) {
                    if (!Character.isLetter(c)) e.consume();
                } else if (length < 13) {
                    if (!Character.isDigit(c)) e.consume();
                } else {
                    e.consume();
                }

                if (length == 9 && Character.isLetter(c)) {
                    e.setKeyChar(Character.toUpperCase(c));
                }
            }
        });
    }

    private JPanel createNavigationBar() {
        // Barre de navigation standardisée (4 colonnes)
        JPanel navPanel = new JPanel(new GridLayout(1, 4)); 
        navPanel.setBackground(new Color(52, 73, 94));
        
        // 1. Bouton Retour Dashboard
        JButton btnBack = new JButton(this.getHtmlText("\uf060", "Tableau de Bord"));
        btnBack.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnBack.setBackground(new Color(44, 62, 80));
        btnBack.setForeground(Color.WHITE);
        btnBack.setFocusPainted(false);
        btnBack.setBorderPainted(false);
        btnBack.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnBack.addActionListener(e -> {
            this.dispose();
            new AdminDashboard().setVisible(true);
        });
        navPanel.add(btnBack);

        // 2. Menu Fournisseurs
        String[] fournisseursMenuItems = new String[]{"Liste Fournisseurs"};
        String[] fournisseursMenuIcons = new String[]{"\uf0c0"};
        JButton btnFournisseurs = this.createNavButtonWithMenu("\uf0c0", "Fournisseurs", true, fournisseursMenuItems, fournisseursMenuIcons);
        navPanel.add(btnFournisseurs);

        // 3. Menu Documents
        String[] documentsMenuItems = new String[]{"Facture", "Liste Factures", "Bon de Livraison", "Bon de Sortie", "Devis"};
        String[] documentsMenuIcons = new String[]{"\uf15b", "\uf15b", "\uf15b", "\uf15b", "\uf15b"};
        JButton btnDocuments = this.createNavButtonWithMenu("\uf15b", "Documents", false, documentsMenuItems, documentsMenuIcons);
        navPanel.add(btnDocuments);

        // 4. Menu Stock
        String[] stockMenuItems = new String[]{"Consulter Stock", "Gestion Entrée", "Gestion Sortie"};
        String[] stockMenuIcons = new String[]{"\uf494", "\uf090", "\uf08b"};
        JButton btnStock = this.createNavButtonWithMenu("\uf494", "Stock", false, stockMenuItems, stockMenuIcons);
        navPanel.add(btnStock);
        
        return navPanel;
    }

    private JButton createNavButtonWithMenu(String icon, String text, final boolean isActive, final String[] menuItems, final String[] menuIcons) {
        String fontName = (this.fontAwesomeSolid != null) ? this.fontAwesomeSolid.getFontName() : "SansSerif";
        String chevron = "\uf078"; // Flèche bas
        
        String htmlContent = "<html><font face=\"" + fontName + "\">" + icon + "</font> " + 
                             text + 
                             " <font face=\"" + fontName + "\" size=\"3\">" + chevron + "</font></html>";
        
        final JButton button = new JButton(htmlContent);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));

        if (isActive) {
            button.setBackground(new Color(41, 128, 185));
            button.setForeground(Color.WHITE);
        } else {
            button.setBackground(new Color(236, 240, 241));
            button.setForeground(new Color(52, 73, 94));
        }

        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));

        final Timer hoverTimer = new Timer(300, (e) -> {
            this.showMenuForButton(button, menuItems, menuIcons);
        });
        hoverTimer.setRepeats(false);

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent evt) {
                if (!isActive) button.setBackground(new Color(220, 220, 220));
                hoverTimer.start();
            }
            @Override
            public void mouseExited(MouseEvent evt) {
                if (!isActive) button.setBackground(new Color(236, 240, 241));
                hoverTimer.stop();
            }
            @Override
            public void mouseClicked(MouseEvent evt) {
                ModifierFournisseur.this.showMenuForButton(button, menuItems, menuIcons);
            }
        });
        return button;
    }

    private void showMenuForButton(JButton button, String[] menuItems, String[] menuIcons) {
        JPopupMenu menu = new JPopupMenu();
        menu.setBackground(Color.WHITE);
        menu.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));

        for (int i = 0; i < menuItems.length; ++i) {
            String htmlItem = this.getHtmlText(menuIcons[i], menuItems[i]);
            JMenuItem menuItem = new JMenuItem(htmlItem);
            menuItem.setFont(new Font("Segoe UI", Font.PLAIN, 13));

            String item = menuItems[i];
            menuItem.addActionListener((e) -> {
                this.handleMenuAction(item);
            });
            menu.add(menuItem);
            if (i < menuItems.length - 1) {
                menu.addSeparator();
            }
        }
        menu.show(button, 0, button.getHeight());
    }

    private void handleMenuAction(String menuItem) {
        switch (menuItem) {
            case "Liste Fournisseurs": 
                this.retourListeFournisseurs();
                break;
            case "Facture": this.ouvrirFacture(); break;
            case "Liste Factures": this.ouvrirListeFacture(); break;
            case "Bon de Livraison": this.ouvrirBonLivraison(); break;
            case "Bon de Sortie": this.ouvrirBonSortie(); break;
            case "Devis": this.ouvrirDevis(); break;
            case "Consulter Stock": this.ouvrirGestionStock(); break;
            case "Gestion Entrée": this.ouvrirGestionEntrees(); break;
            case "Gestion Sortie": this.ouvrirGestionSorties(); break;
        }
    }

    // --- Méthodes de Navigation ---
    private void retourListeFournisseurs() {
        if (this.parent != null) {
            this.parent.setVisible(true);
        } else {
            new ListeFournisseurs().setVisible(true);
        }
        this.dispose();
    }
    private void ouvrirFacture() { new FactureUI().setVisible(true); this.dispose(); }
    private void ouvrirListeFacture() { new ListeFactures().setVisible(true); this.dispose(); }
    private void ouvrirBonLivraison() { new BLUI().setVisible(true); this.dispose(); }
    private void ouvrirBonSortie() { new BonSortieUI().setVisible(true); this.dispose(); }
    private void ouvrirDevis() { new DevisUI().setVisible(true); this.dispose(); }
    private void ouvrirGestionStock() { new GestionStock().setVisible(true); this.dispose(); }
    private void ouvrirGestionEntrees() { new GestionEntreesUI().setVisible(true); this.dispose(); }
    private void ouvrirGestionSorties() { new GestionSortiesUI().setVisible(true); this.dispose(); }

    private void initUI(String nom, String prenom, String telephone, String adresse, String matricule, String email) {
        JPanel navBarPanel = this.createNavigationBar();

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(44, 62, 80));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        JLabel titleLabel = new JLabel("MODIFIER FOURNISSEUR", JLabel.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        titleLabel.setForeground(Color.WHITE);

        JLabel subTitleLabel = new JLabel("Modifiez les informations du fournisseur", JLabel.CENTER);
        subTitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subTitleLabel.setForeground(new Color(200, 200, 200));
        subTitleLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));

        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(new Color(44, 62, 80));
        titlePanel.add(titleLabel, BorderLayout.CENTER);
        titlePanel.add(subTitleLabel, BorderLayout.SOUTH);

        headerPanel.add(titlePanel, BorderLayout.CENTER);

        // Formulaire
        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setBackground(new Color(240, 242, 245));

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(Color.WHITE);
        formPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(225, 225, 225), 1),
                BorderFactory.createEmptyBorder(30, 40, 30, 40)));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0D;

        Font labelFont = new Font("Segoe UI", Font.BOLD, 14);
        Font fieldFont = new Font("Segoe UI", Font.PLAIN, 14);

        // Champs avec valeurs pré-remplies
        this.txtNom = this.createStyledTextField(nom != null ? nom : "");
        this.txtPrenom = this.createStyledTextField(prenom != null ? prenom : "");
        this.txtTelephone = this.createStyledTextField(telephone != null ? telephone : "");
        this.txtAdresse = this.createStyledTextField(adresse != null ? adresse : "");
        this.txtMatriculeFiscale = this.createStyledTextField(matricule != null ? matricule : "");
        this.txtEmail = this.createStyledTextField(email != null ? email : "");

        this.addFormField(formPanel, gbc, 0, "Nom *", this.txtNom, labelFont);
        this.addFormField(formPanel, gbc, 1, "Prénom", this.txtPrenom, labelFont);
        this.addFormField(formPanel, gbc, 2, "Téléphone", this.txtTelephone, labelFont);
        this.addFormField(formPanel, gbc, 3, "Adresse", this.txtAdresse, labelFont);

        // Matricule Fiscale
        gbc.gridx = 0; gbc.gridy = 4;
        JLabel lblMatricule = new JLabel("Matricule Fiscale");
        lblMatricule.setFont(labelFont);
        lblMatricule.setForeground(new Color(52, 73, 94));
        formPanel.add(lblMatricule, gbc);

        gbc.gridx = 1;
        this.txtMatriculeFiscale.setFont(fieldFont);
        this.txtMatriculeFiscale.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(10, 15, 10, 15)));
        
        this.txtMatriculeFiscale.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { validerMatriculeEnTempsReel(); }
            @Override public void removeUpdate(DocumentEvent e) { validerMatriculeEnTempsReel(); }
            @Override public void changedUpdate(DocumentEvent e) { validerMatriculeEnTempsReel(); }
        });
        
        this.appliquerMasqueMatricule();
        formPanel.add(this.txtMatriculeFiscale, gbc);

        // Email
        gbc.gridx = 0; gbc.gridy = 5;
        JLabel lblEmail = new JLabel("Email");
        lblEmail.setFont(labelFont);
        lblEmail.setForeground(new Color(52, 73, 94));
        formPanel.add(lblEmail, gbc);

        gbc.gridx = 1;
        this.txtEmail.setFont(fieldFont);
        this.txtEmail.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(10, 15, 10, 15)));
        
        this.txtEmail.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { validerEmailEnTempsReel(); }
            @Override public void removeUpdate(DocumentEvent e) { validerEmailEnTempsReel(); }
            @Override public void changedUpdate(DocumentEvent e) { validerEmailEnTempsReel(); }
        });
        
        formPanel.add(this.txtEmail, gbc);

        // Conteneur formulaire
        JPanel formContainer = new JPanel(new GridBagLayout());
        formContainer.setBackground(new Color(240, 242, 245));
        GridBagConstraints formGbc = new GridBagConstraints();
        formGbc.gridx = 0;
        formGbc.gridy = 0;
        formGbc.insets = new Insets(20, 20, 20, 20);
        formContainer.add(formPanel, formGbc);
        centerPanel.add(formContainer);

        // Boutons bas de page
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 15));
        buttonPanel.setBackground(new Color(240, 242, 245));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 30, 0));

        this.btnValider = this.createModernButton(this.getHtmlText("\uf00c", " Modifier"), new Color(46, 204, 113));
        this.btnAnnuler = this.createModernButton(this.getHtmlText("\uf00d", " Annuler"), new Color(231, 76, 60));

        buttonPanel.add(this.btnValider);
        buttonPanel.add(this.btnAnnuler);

        // Assemblage final
        this.add(navBarPanel, BorderLayout.NORTH);

        JPanel mainContentPanel = new JPanel(new BorderLayout());
        mainContentPanel.setBackground(new Color(240, 242, 245));
        mainContentPanel.add(headerPanel, BorderLayout.NORTH);
        mainContentPanel.add(centerPanel, BorderLayout.CENTER);
        mainContentPanel.add(buttonPanel, BorderLayout.SOUTH);

        this.add(mainContentPanel, BorderLayout.CENTER);

        this.btnValider.addActionListener((e) -> {
            this.validerModification();
        });

        this.btnAnnuler.addActionListener((e) -> {
            this.retourListeFournisseurs();
        });
    }
    
    private void addFormField(JPanel panel, GridBagConstraints gbc, int y, String label, JTextField field, Font font) {
        gbc.gridx = 0; gbc.gridy = y;
        JLabel lbl = new JLabel(label);
        lbl.setFont(font);
        lbl.setForeground(new Color(52, 73, 94));
        panel.add(lbl, gbc);
        
        gbc.gridx = 1;
        field.setFont(font);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(10, 15, 10, 15)));
        panel.add(field, gbc);
    }

    private JTextField createStyledTextField(String text) {
        JTextField textField = new JTextField(text);
        textField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        textField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(10, 15, 10, 15)));
        textField.setForeground(Color.BLACK);
        return textField;
    }

    private JButton createModernButton(String text, final Color backgroundColor) {
        final JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setBackground(backgroundColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createEmptyBorder(12, 30, 12, 30));
        button.setPreferredSize(new Dimension(150, 45));

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent evt) {
                button.setBackground(ModifierFournisseur.this.darkenColor(backgroundColor, 0.9F));
                button.setBorder(BorderFactory.createLineBorder(ModifierFournisseur.this.darkenColor(backgroundColor, 0.8F), 1));
            }
            @Override
            public void mouseExited(MouseEvent evt) {
                button.setBackground(backgroundColor);
                button.setBorder(BorderFactory.createEmptyBorder(12, 30, 12, 30));
            }
            @Override
            public void mousePressed(MouseEvent evt) {
                button.setBackground(ModifierFournisseur.this.darkenColor(backgroundColor, 0.8F));
            }
            @Override
            public void mouseReleased(MouseEvent evt) {
                button.setBackground(ModifierFournisseur.this.darkenColor(backgroundColor, 0.9F));
            }
        });
        return button;
    }

    private Color darkenColor(Color color, float factor) {
        int r = Math.max((int) ((float) color.getRed() * factor), 0);
        int g = Math.max((int) ((float) color.getGreen() * factor), 0);
        int b = Math.max((int) ((float) color.getBlue() * factor), 0);
        return new Color(r, g, b);
    }

    private void validerModification() {
        String nom = this.txtNom.getText().trim();
        String prenom = this.txtPrenom.getText().trim();
        String telephone = this.txtTelephone.getText().trim();
        String adresse = this.txtAdresse.getText().trim();
        String matricule = this.txtMatriculeFiscale.getText().trim();
        String email = this.txtEmail.getText().trim();

        if (nom.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Le nom du fournisseur est obligatoire", "Erreur", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!telephone.isEmpty() && !this.validerTelephone(telephone)) {
            JOptionPane.showMessageDialog(this, "Format du téléphone invalide!\nDoit contenir 8 à 12 chiffres", "Erreur", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!matricule.isEmpty() && !this.validerMatriculeFiscal(matricule)) {
            JOptionPane.showMessageDialog(this, "Format du matricule fiscal invalide!\nFormat: 9 chiffres + 1 lettre + 3 chiffres", "Erreur", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!email.isEmpty() && !this.validerEmail(email)) {
            JOptionPane.showMessageDialog(this, "Format d'email invalide!", "Erreur", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String matriculeFormate = this.formaterMatriculeFiscal(matricule);
        this.modifierFournisseur(nom, prenom, telephone, adresse, matriculeFormate, email);
    }

    private void modifierFournisseur(String nom, String prenom, String telephone, String adresse, String matricule, String email) {
        String sql = "UPDATE Fournisseurs SET nom = ?, prenom = ?, telephone = ?, adresse = ?, matricule_fiscale = ?, email = ? WHERE id = ?";
        
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setString(1, nom);
            pst.setString(2, prenom.isEmpty() ? null : prenom);
            pst.setString(3, telephone.isEmpty() ? null : telephone);
            pst.setString(4, adresse.isEmpty() ? null : adresse);
            pst.setString(5, matricule.isEmpty() ? null : matricule);
            pst.setString(6, email.isEmpty() ? null : email);
            pst.setInt(7, this.fournisseurId);

            int rowsAffected = pst.executeUpdate();
            
            if (rowsAffected > 0) {
                JOptionPane.showMessageDialog(this, "✅ Fournisseur modifié avec succès", "Succès", JOptionPane.INFORMATION_MESSAGE);

                if (this.parent != null) {
                    this.parent.rafraichirListeFournisseurs();
                }
                
                this.retourListeFournisseurs();
            } else {
                JOptionPane.showMessageDialog(this, "❌ Aucune modification effectuée", "Info", JOptionPane.INFORMATION_MESSAGE);
            }

        } catch (SQLException e) {
            log.error("Error updating fournisseur id={}", this.fournisseurId, e);
            JOptionPane.showMessageDialog(this, "Erreur lors de la modification du fournisseur: " + e.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                System.setProperty("awt.useSystemAAFontSettings", "on");
                System.setProperty("swing.aatext", "true");
                // Exemple de test
                new ModifierFournisseur(null, 1, "DISTRIBUTION PLUS", "SARL", "71234567", 
                    "Zone Industrielle", "123456789A123", "contact@distribution.tn");
            } catch (Exception e) {
                log.error("Failed to start ModifierFournisseur", e);
            }
        });
    }
}