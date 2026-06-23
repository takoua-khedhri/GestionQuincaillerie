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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AjouterClient extends JFrame {

    private static final Logger log = LoggerFactory.getLogger(AjouterClient.class);

    private JTextField txtNom;
    private JTextField txtPrenom;
    private JTextField txtNumero;
    private JTextField txtAdresse;
    private JTextField txtMatriculeFiscale;
    private JButton btnValider;
    private JButton btnAnnuler;
    private JButton btnAjouterVoiture;
    
    private ListeClients parent;
    private Font fontAwesomeSolid;

    // Définition des placeholders pour les réutiliser dans la validation
    private final String PLACEHOLDER_NOM = "Entrez le nom du client";
    private final String PLACEHOLDER_PRENOM = "Entrez le prénom du client";
    private final String PLACEHOLDER_NUMERO = "Ex: 12345678";
    private final String PLACEHOLDER_ADRESSE = "Entrez l'adresse complète";

    public AjouterClient(ListeClients parent) {
        this.parent = parent;
        this.loadFontAwesome();
        
        this.setTitle("Ajouter un Nouveau Client");
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.setExtendedState(JFrame.MAXIMIZED_BOTH);
        this.setLocationRelativeTo(null);
        this.setLayout(new BorderLayout(10, 10));
        this.getContentPane().setBackground(new Color(240, 242, 245));
        
        this.initUI();
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
    
    private String getHtmlText(String iconCode, String text) {
        String fontName = (this.fontAwesomeSolid != null) ? this.fontAwesomeSolid.getFontName() : "SansSerif";
        return "<html><font face=\"" + fontName + "\">" + iconCode + "</font> " + text + "</html>";
    }

    // Méthode modifiée pour gérer les Placeholders
    private JTextField createStyledTextField(final String placeholder) {
        final JTextField textField = new JTextField(25);
        textField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        textField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(12, 15, 12, 15)));
        
        // État initial du placeholder
        textField.setText(placeholder);
        textField.setForeground(Color.GRAY);
        textField.setPreferredSize(new Dimension(400, 45));

        textField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent evt) {
                if (textField.getText().equals(placeholder)) {
                    textField.setText("");
                    textField.setForeground(Color.BLACK);
                }
            }
            @Override
            public void focusLost(FocusEvent evt) {
                if (textField.getText().isEmpty()) {
                    textField.setText(placeholder);
                    textField.setForeground(Color.GRAY);
                }
            }
        });
        return textField;
    }

    // Récupère la valeur réelle (vide si c'est le placeholder)
    private String getRealTextFieldValue(JTextField textField, String placeholder) {
        String value = textField.getText().trim();
        if (value.equals(placeholder)) {
            return "";
        }
        return value;
    }

    /**
     * Vérifie si un client existe déjà avec les mêmes nom, prénom et numéro
     */
    private boolean clientExiste(String nom, String prenom, int numero) {
        String sql = "SELECT COUNT(*) FROM clients WHERE nom = ? AND prenom = ? AND numero = ?";
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, nom);
            pst.setString(2, prenom);
            pst.setInt(3, numero);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            log.error("Error checking if client exists", e);
        }
        return false;
    }

    private void initUI() {
        JPanel navBarPanel = this.createNavigationBar();

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(44, 62, 80));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 30, 10, 30));

        JLabel titleLabel = new JLabel("AJOUTER UN CLIENT", JLabel.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 26));
        titleLabel.setForeground(Color.WHITE);

        JLabel subTitleLabel = new JLabel("Remplissez les informations du nouveau client", JLabel.CENTER);
        subTitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subTitleLabel.setForeground(new Color(200, 200, 200));

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
                BorderFactory.createEmptyBorder(15, 40, 20, 40)));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 15, 8, 15);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        Font labelFont = new Font("Segoe UI", Font.BOLD, 14);

        // Champs avec Placeholder
        this.addFormField(formPanel, gbc, 0, "Nom *", this.txtNom = this.createStyledTextField(PLACEHOLDER_NOM), labelFont);
        this.addFormField(formPanel, gbc, 1, "Prénom *", this.txtPrenom = this.createStyledTextField(PLACEHOLDER_PRENOM), labelFont);
        this.addFormField(formPanel, gbc, 2, "Numéro *", this.txtNumero = this.createStyledTextField(PLACEHOLDER_NUMERO), labelFont);
        this.addFormField(formPanel, gbc, 3, "Adresse", this.txtAdresse = this.createStyledTextField(PLACEHOLDER_ADRESSE), labelFont);

        // Matricule Fiscale (SANS PLACEHOLDER comme demandé)
        gbc.gridx = 0; gbc.gridy = 4; gbc.ipadx = 0;
        JLabel lblMatricule = new JLabel("Matricule Fiscale");
        lblMatricule.setFont(labelFont);
        lblMatricule.setForeground(new Color(52, 73, 94));
        formPanel.add(lblMatricule, gbc);

        gbc.gridx = 1; gbc.ipadx = 200;
        this.txtMatriculeFiscale = new JTextField(25);
        this.txtMatriculeFiscale.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        this.txtMatriculeFiscale.setText(""); // Vide par défaut
        this.txtMatriculeFiscale.setForeground(Color.BLACK);
        this.txtMatriculeFiscale.setPreferredSize(new Dimension(400, 45));
        this.txtMatriculeFiscale.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(12, 15, 12, 15)));
        formPanel.add(this.txtMatriculeFiscale, gbc);

        // Bouton Ajouter Voiture
        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2; gbc.ipadx = 0;
        gbc.insets = new Insets(5, 15, 5, 15);
        JPanel buttonVoiturePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonVoiturePanel.setBackground(Color.WHITE);
        this.btnAjouterVoiture = new JButton(this.getHtmlText("\uf1b9", " Ajouter une Voiture"));
        this.btnAjouterVoiture.setFont(new Font("Segoe UI", Font.BOLD, 14));
        this.btnAjouterVoiture.setBackground(new Color(52, 152, 219));
        this.btnAjouterVoiture.setForeground(Color.WHITE);
        this.btnAjouterVoiture.setFocusPainted(false);
        this.btnAjouterVoiture.setBorderPainted(false);
        this.btnAjouterVoiture.setCursor(new Cursor(Cursor.HAND_CURSOR));
        this.btnAjouterVoiture.setPreferredSize(new Dimension(280, 50));
        this.btnAjouterVoiture.addActionListener(e -> ouvrirAjoutVoiture());
        buttonVoiturePanel.add(this.btnAjouterVoiture);
        formPanel.add(buttonVoiturePanel, gbc);

        JPanel formContainer = new JPanel(new GridBagLayout());
        formContainer.setBackground(new Color(240, 242, 245));
        GridBagConstraints formGbc = new GridBagConstraints();
        formGbc.insets = new Insets(5, 20, 5, 20);
        formContainer.add(formPanel, formGbc);
        centerPanel.add(formContainer);

        // Boutons de validation
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        buttonPanel.setBackground(new Color(240, 242, 245));
        this.btnValider = this.createModernButton(this.getHtmlText("\uf00c", " Valider"), new Color(46, 204, 113));
        this.btnAnnuler = this.createModernButton(this.getHtmlText("\uf00d", " Annuler"), new Color(231, 76, 60));
        buttonPanel.add(this.btnValider);
        buttonPanel.add(this.btnAnnuler);

        this.add(navBarPanel, BorderLayout.NORTH);
        JPanel mainContentPanel = new JPanel(new BorderLayout());
        mainContentPanel.add(headerPanel, BorderLayout.NORTH);
        mainContentPanel.add(centerPanel, BorderLayout.CENTER);
        mainContentPanel.add(buttonPanel, BorderLayout.SOUTH);
        this.add(mainContentPanel, BorderLayout.CENTER);

        this.btnValider.addActionListener(e -> validerAjout());
        this.btnAnnuler.addActionListener(e -> ouvrirListeClients());
    }
    
    private void addFormField(JPanel panel, GridBagConstraints gbc, int y, String label, Component field, Font font) {
        gbc.gridx = 0; gbc.gridy = y; gbc.ipadx = 0;
        JLabel lbl = new JLabel(label);
        lbl.setFont(font);
        lbl.setForeground(new Color(52, 73, 94));
        panel.add(lbl, gbc);
        gbc.gridx = 1; gbc.ipadx = 200;
        panel.add(field, gbc);
    }

    private JButton createModernButton(String text, final Color backgroundColor) {
        final JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setBackground(backgroundColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(180, 50));
        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent evt) { button.setBackground(darkenColor(backgroundColor, 0.9F)); }
            public void mouseExited(MouseEvent evt) { button.setBackground(backgroundColor); }
        });
        return button;
    }

    private Color darkenColor(Color color, float factor) {
        return new Color(Math.max((int)(color.getRed()*factor), 0), 
                         Math.max((int)(color.getGreen()*factor), 0), 
                         Math.max((int)(color.getBlue()*factor), 0));
    }

    private void validerAjout() {
        // Utilisation de la méthode de nettoyage des placeholders
        String nom = this.getRealTextFieldValue(this.txtNom, PLACEHOLDER_NOM);
        String prenom = this.getRealTextFieldValue(this.txtPrenom, PLACEHOLDER_PRENOM);
        String numeroStr = this.getRealTextFieldValue(this.txtNumero, PLACEHOLDER_NUMERO);
        String adresse = this.getRealTextFieldValue(this.txtAdresse, PLACEHOLDER_ADRESSE);
        String matriculeFiscaleStr = this.txtMatriculeFiscale.getText().trim();

        if (!nom.isEmpty() && !prenom.isEmpty() && !numeroStr.isEmpty()) {
            try {
                int numero = Integer.parseInt(numeroStr);
                
                // Vérifier si le client existe déjà
              if (clientExiste(nom, prenom, numero)) {
    JOptionPane.showMessageDialog(this, "❌ Ce client existe déjà !");
    return;
}
                
                this.ajouterClient(nom, prenom, numero, adresse, matriculeFiscaleStr.isEmpty() ? null : matriculeFiscaleStr);
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Le numéro doit être un nombre valide", "Erreur", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(this, "Les champs Nom, Prénom et Numéro sont obligatoires", "Erreur", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void ajouterClient(String nom, String prenom, int numero, String adresse, String matriculeFiscale) {
        String sql = "INSERT INTO clients (nom, prenom, numero, adresse, matricule_fiscale) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, nom);
            pst.setString(2, prenom);
            pst.setInt(3, numero);
            pst.setString(4, adresse.isEmpty() ? null : adresse);
            pst.setString(5, matriculeFiscale);
            pst.executeUpdate();
            JOptionPane.showMessageDialog(this, "✅ Client ajouté avec succès", "Succès", JOptionPane.INFORMATION_MESSAGE);
            if (this.parent != null) this.parent.rafraichirListeClients();
            this.ouvrirListeClients();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erreur: " + e.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    // --- Navigation ---
    private JPanel createNavigationBar() {
        JPanel navPanel = new JPanel(new GridLayout(1, 4)); 
        navPanel.setBackground(new Color(52, 73, 94));
        JButton btnBack = new JButton(this.getHtmlText("\uf060", "Tableau de Bord"));
        btnBack.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnBack.setBackground(new Color(44, 62, 80));
        btnBack.setForeground(Color.WHITE);
        btnBack.setFocusPainted(false);
        btnBack.setBorderPainted(false);
        btnBack.addActionListener(e -> { this.dispose(); new AdminDashboard().setVisible(true); });
        navPanel.add(btnBack);

        navPanel.add(createNavButtonWithMenu("\uf0c0", "Clients", true, new String[]{"Liste Clients", "Nouveau Client"}, new String[]{"\uf0c0", "\uf067"}));
        navPanel.add(createNavButtonWithMenu("\uf15b", "Documents", false, new String[]{"Facture", "Liste Factures", "Bon de Livraison", "Bon de Sortie", "Devis"}, new String[]{"\uf15b", "\uf15b", "\uf15b", "\uf15b", "\uf15b"}));
        navPanel.add(createNavButtonWithMenu("\uf494", "Stock", false, new String[]{"Consulter Stock", "Gestion Entrée", "Gestion Sortie", "Historique Entrée", "Historique Sortie"}, new String[]{"\uf494", "\uf090", "\uf08b", "\uf1da", "\uf201"}));
        return navPanel;
    }

    private JButton createNavButtonWithMenu(String icon, String text, boolean isActive, String[] menuItems, String[] menuIcons) {
        String fontName = (this.fontAwesomeSolid != null) ? this.fontAwesomeSolid.getFontName() : "SansSerif";
        String htmlContent = "<html><font face=\"" + fontName + "\">" + icon + "</font> " + text + " <font face=\"" + fontName + "\" size=\"3\">\uf078</font></html>";
        JButton button = new JButton(htmlContent);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setBackground(isActive ? new Color(41, 128, 185) : new Color(236, 240, 241));
        button.setForeground(isActive ? Color.WHITE : new Color(52, 73, 94));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent evt) { if (!isActive) button.setBackground(new Color(220, 220, 220)); }
            public void mouseExited(MouseEvent evt) { if (!isActive) button.setBackground(new Color(236, 240, 241)); }
            public void mouseClicked(MouseEvent evt) { showMenuForButton(button, menuItems, menuIcons); }
        });
        return button;
    }

    private void showMenuForButton(JButton button, String[] menuItems, String[] menuIcons) {
        JPopupMenu menu = new JPopupMenu();
        for (int i = 0; i < menuItems.length; ++i) {
            JMenuItem menuItem = new JMenuItem(getHtmlText(menuIcons[i], menuItems[i]));
            String item = menuItems[i];
            menuItem.addActionListener(e -> handleMenuAction(item));
            menu.add(menuItem);
        }
        menu.show(button, 0, button.getHeight());
    }

    private void handleMenuAction(String menuItem) {
        switch (menuItem) {
            case "Liste Clients": ouvrirListeClients(); break;
            case "Nouveau Client": new AjouterClient(null).setVisible(true); this.dispose(); break;
            case "Facture": new FactureUI().setVisible(true); this.dispose(); break;
            case "Liste Factures": new ListeFactures().setVisible(true); this.dispose(); break;
            case "Bon de Livraison": new BLUI().setVisible(true); this.dispose(); break;
            case "Bon de Sortie": new BonSortieUI().setVisible(true); this.dispose(); break;
            case "Devis": new DevisUI().setVisible(true); this.dispose(); break;
            case "Consulter Stock": new GestionStock().setVisible(true); this.dispose(); break;
            case "Gestion Entrée": new GestionEntreesUI().setVisible(true); this.dispose(); break;
            case "Gestion Sortie": new GestionSortiesUI().setVisible(true); this.dispose(); break;
            case "Historique Entrée": new HistoriqueEntreesUI().setVisible(true); this.dispose(); break;
            case "Historique Sortie": new HistoriqueSortiesUI().setVisible(true); this.dispose(); break;
        }
    }

    private void ouvrirListeClients() {
        if (this.parent != null) this.parent.setVisible(true);
        else new ListeClients().setVisible(true);
        this.dispose();
    }

    private void ouvrirAjoutVoiture() {
        new AjouterVoiture(this).setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                new AjouterClient(null);
            } catch (Exception e) {
                log.error("Failed to start AjouterClient", e);
            }
        });
    }
}