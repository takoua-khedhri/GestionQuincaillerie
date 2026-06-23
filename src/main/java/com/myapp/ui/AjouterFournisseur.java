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
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AjouterFournisseur extends JFrame {

    private static final Logger log = LoggerFactory.getLogger(AjouterFournisseur.class);

    private JTextField txtNom;
    private JTextField txtPrenom;
    private JTextField txtTelephone;
    private JTextField txtAdresse;
    private JTextField txtMatriculeFiscale;
    private JTextField txtEmail;
    private JButton btnValider;
    private JButton btnAnnuler;
    
    private BonCommandeUI parent;
    private Font fontAwesomeSolid;

    // Définition des constantes pour les placeholders
    private final String HINT_NOM = "Entrez le nom du fournisseur";
    private final String HINT_PRENOM = "Entrez le prénom (optionnel)";
    private final String HINT_TELEPHONE = "Ex: 71234567";
    private final String HINT_ADRESSE = "Entrez l'adresse complète";
    private final String HINT_EMAIL = "exemple@domaine.com";

    public AjouterFournisseur(BonCommandeUI parent) {
        this.parent = parent;
        this.loadFontAwesome();
        
        this.setTitle("Ajouter un Nouveau Fournisseur");
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.setExtendedState(JFrame.MAXIMIZED_BOTH);
        this.setLocationRelativeTo(null);
        this.setLayout(new BorderLayout(10, 10));
        this.getContentPane().setBackground(new Color(240, 242, 245));
        
        this.initUI();
        this.setVisible(true);
    }
    
    public AjouterFournisseur() {
        this(null);
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

    private boolean validerEmail(String email) {
        if (email != null && !email.trim().isEmpty()) {
            return email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
        }
        return true;
    }

    private boolean validerTelephone(String telephone) {
        if (telephone != null && !telephone.trim().isEmpty()) {
            return telephone.matches("\\d{8,12}");
        }
        return true;
    }

    /**
     * Vérifie si un fournisseur existe déjà avec les mêmes nom, prénom, téléphone et adresse
     */
    private boolean fournisseurExiste(String nom, String prenom, String telephone, String adresse) {
        String sql = "SELECT COUNT(*) FROM Fournisseurs WHERE nom = ? AND (prenom = ? OR (prenom IS NULL AND ? IS NULL)) AND telephone = ? AND adresse = ?";
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            
            pst.setString(1, nom);
            
            // Gestion des valeurs null pour prenom
            if (prenom == null || prenom.isEmpty()) {
                pst.setNull(2, java.sql.Types.VARCHAR);
                pst.setNull(3, java.sql.Types.VARCHAR);
            } else {
                pst.setString(2, prenom);
                pst.setString(3, prenom);
            }
            
            pst.setString(4, telephone);
            pst.setString(5, adresse);
            
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            log.error("Error checking if fournisseur exists", e);
        }
        return false;
    }

    private void validerEmailEnTempsReel() {
        String email = getRealTextFieldValue(this.txtEmail, HINT_EMAIL);
        if (email.isEmpty()) {
            this.txtEmail.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(200, 200, 200)),
                    BorderFactory.createEmptyBorder(12, 15, 12, 15)));
        } else {
            Color borderCol = validerEmail(email) ? new Color(46, 204, 113) : new Color(231, 76, 60);
            this.txtEmail.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(borderCol, 2),
                    BorderFactory.createEmptyBorder(10, 13, 10, 13)));
        }
    }

    private void validerTelephoneEnTempsReel() {
        String telephone = getRealTextFieldValue(this.txtTelephone, HINT_TELEPHONE);
        if (telephone.isEmpty()) {
            this.txtTelephone.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(200, 200, 200)),
                    BorderFactory.createEmptyBorder(12, 15, 12, 15)));
        } else {
            Color borderCol = validerTelephone(telephone) ? new Color(46, 204, 113) : new Color(231, 76, 60);
            this.txtTelephone.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(borderCol, 2),
                    BorderFactory.createEmptyBorder(10, 13, 10, 13)));
        }
    }

    // Création d'un champ stylé avec gestion de Placeholder
    private JTextField createStyledTextField(final String placeholder) {
        final JTextField textField = new JTextField(25);
        textField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        textField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(12, 15, 12, 15)));
        
        // Initialisation avec le placeholder en gris
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

    // Récupérer la valeur sans le texte du placeholder
    private String getRealTextFieldValue(JTextField textField, String placeholder) {
        String value = textField.getText().trim();
        return value.equals(placeholder) ? "" : value;
    }

    private void initUI() {
        JPanel navBarPanel = this.createNavigationBar();

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(44, 62, 80));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 30, 10, 30));

        JLabel titleLabel = new JLabel("AJOUTER UN FOURNISSEUR", JLabel.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 26));
        titleLabel.setForeground(Color.WHITE);

        JLabel subTitleLabel = new JLabel("Remplissez les informations du nouveau fournisseur", JLabel.CENTER);
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
        gbc.insets = new Insets(8, 10, 8, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0D;

        Font labelFont = new Font("Segoe UI", Font.BOLD, 14);

        // Champs avec Placeholder
        this.addFormField(formPanel, gbc, 0, "Nom *", this.txtNom = this.createStyledTextField(HINT_NOM), labelFont);
        this.addFormField(formPanel, gbc, 1, "Prénom", this.txtPrenom = this.createStyledTextField(HINT_PRENOM), labelFont);
        
        // Téléphone avec validation
        this.addFormField(formPanel, gbc, 2, "Téléphone", this.txtTelephone = this.createStyledTextField(HINT_TELEPHONE), labelFont);
        this.txtTelephone.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { validerTelephoneEnTempsReel(); }
            @Override public void removeUpdate(DocumentEvent e) { validerTelephoneEnTempsReel(); }
            @Override public void changedUpdate(DocumentEvent e) { validerTelephoneEnTempsReel(); }
        });

        // Adresse
        this.addFormField(formPanel, gbc, 3, "Adresse", this.txtAdresse = this.createStyledTextField(HINT_ADRESSE), labelFont);

        // --- MATRICULE FISCALE (SANS PLACEHOLDER) ---
        gbc.gridx = 0; gbc.gridy = 4; gbc.ipadx = 0;
        JLabel lblMatricule = new JLabel("Matricule Fiscale");
        lblMatricule.setFont(labelFont);
        lblMatricule.setForeground(new Color(52, 73, 94));
        formPanel.add(lblMatricule, gbc);

        gbc.gridx = 1; gbc.ipadx = 200;
        this.txtMatriculeFiscale = new JTextField();
        this.txtMatriculeFiscale.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        this.txtMatriculeFiscale.setText("");
        this.txtMatriculeFiscale.setForeground(Color.BLACK);
        this.txtMatriculeFiscale.setPreferredSize(new Dimension(400, 45));
        this.txtMatriculeFiscale.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(12, 15, 12, 15)));
        formPanel.add(this.txtMatriculeFiscale, gbc);

        // Email
        this.addFormField(formPanel, gbc, 5, "Email", this.txtEmail = this.createStyledTextField(HINT_EMAIL), labelFont);
        this.txtEmail.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { validerEmailEnTempsReel(); }
            @Override public void removeUpdate(DocumentEvent e) { validerEmailEnTempsReel(); }
            @Override public void changedUpdate(DocumentEvent e) { validerEmailEnTempsReel(); }
        });

        JPanel formContainer = new JPanel(new GridBagLayout());
        formContainer.setBackground(new Color(240, 242, 245));
        GridBagConstraints formGbc = new GridBagConstraints();
        formGbc.insets = new Insets(5, 20, 5, 20);
        formContainer.add(formPanel, formGbc);
        centerPanel.add(formContainer);

        // Boutons
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

        this.btnValider.addActionListener((e) -> this.validerAjout());
        this.btnAnnuler.addActionListener((e) -> this.retourAuBonCommande());
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

    private void retourAuBonCommande() {
        if (this.parent != null) this.parent.setVisible(true);
        else new BonCommandeUI().setVisible(true);
        this.dispose();
    }

    private void validerAjout() {
        String nom = getRealTextFieldValue(this.txtNom, HINT_NOM);
        String prenom = getRealTextFieldValue(this.txtPrenom, HINT_PRENOM);
        String telephone = getRealTextFieldValue(this.txtTelephone, HINT_TELEPHONE);
        String adresse = getRealTextFieldValue(this.txtAdresse, HINT_ADRESSE);
        String email = getRealTextFieldValue(this.txtEmail, HINT_EMAIL);
        String matriculeFiscale = this.txtMatriculeFiscale.getText().trim();

        if (nom.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Le nom du fournisseur est obligatoire", "Erreur", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!telephone.isEmpty() && !this.validerTelephone(telephone)) {
            JOptionPane.showMessageDialog(this, "Format du téléphone invalide!", "Erreur", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!email.isEmpty() && !this.validerEmail(email)) {
            JOptionPane.showMessageDialog(this, "Format d'email invalide!", "Erreur", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Vérifier si le fournisseur existe déjà
        if (fournisseurExiste(nom, prenom, telephone, adresse)) {
    JOptionPane.showMessageDialog(this, "❌ Ce fournisseur existe déjà !");
    return;
}

        this.ajouterFournisseur(nom, prenom, telephone, adresse, matriculeFiscale.isEmpty() ? null : matriculeFiscale, email);
    }

    private void ajouterFournisseur(String nom, String prenom, String telephone, String adresse, String matriculeFiscale, String email) {
        String sql = "INSERT INTO Fournisseurs (nom, prenom, telephone, adresse, matricule_fiscale, email) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, nom);
            pst.setString(2, prenom.isEmpty() ? null : prenom);
            pst.setString(3, telephone.isEmpty() ? null : telephone);
            pst.setString(4, adresse.isEmpty() ? null : adresse);
            pst.setString(5, matriculeFiscale);
            pst.setString(6, email.isEmpty() ? null : email);
            pst.executeUpdate();
            JOptionPane.showMessageDialog(this, "✅ Fournisseur ajouté avec succès", "Succès", JOptionPane.INFORMATION_MESSAGE);
            this.retourAuBonCommande();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erreur lors de l'ajout: " + e.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JPanel createNavigationBar() {
        JPanel navPanel = new JPanel(new GridLayout(1, 4)); 
        navPanel.setBackground(new Color(52, 73, 94));
        JButton btnBack = new JButton(this.getHtmlText("\uf060", "Tableau de Bord"));
        btnBack.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnBack.setBackground(new Color(44, 62, 80));
        btnBack.setForeground(Color.WHITE);
        btnBack.setFocusPainted(false);
        btnBack.setBorderPainted(false);
        btnBack.addActionListener(e -> { new AdminDashboard().setVisible(true); this.dispose(); });
        navPanel.add(btnBack);

        navPanel.add(this.createNavButtonWithMenu("\uf0c0", "Clients", false, new String[]{"Liste Clients", "Nouveau Client"}, new String[]{"\uf0c0", "\uf067"}));
        navPanel.add(this.createNavButtonWithMenu("\uf15b", "Documents", false, new String[]{"Facture", "Liste Factures", "Bon de Livraison", "Bon de Sortie", "Devis"}, new String[]{"\uf15b", "\uf15b", "\uf15b", "\uf15b", "\uf15b"}));
        navPanel.add(this.createNavButtonWithMenu("\uf494", "Stock", false, new String[]{"Consulter Stock", "Gestion Entrée", "Gestion Sortie"}, new String[]{"\uf494", "\uf090", "\uf08b"}));
        return navPanel;
    }

    private JButton createNavButtonWithMenu(String icon, String text, final boolean isActive, final String[] menuItems, final String[] menuIcons) {
        String fontName = (this.fontAwesomeSolid != null) ? this.fontAwesomeSolid.getFontName() : "SansSerif";
        String htmlContent = "<html><font face=\"" + fontName + "\">" + icon + "</font> " + text + " <font face=\"" + fontName + "\" size=\"3\">\uf078</font></html>";
        
        final JButton button = new JButton(htmlContent);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setBackground(isActive ? new Color(41, 128, 185) : new Color(236, 240, 241));
        button.setForeground(isActive ? Color.WHITE : new Color(52, 73, 94));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));

        final Timer hoverTimer = new Timer(300, (e) -> this.showMenuForButton(button, menuItems, menuIcons));
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
                showMenuForButton(button, menuItems, menuIcons);
            }
        });
        return button;
    }

    private void showMenuForButton(JButton button, String[] menuItems, String[] menuIcons) {
        JPopupMenu menu = new JPopupMenu();
        for (int i = 0; i < menuItems.length; ++i) {
            JMenuItem menuItem = new JMenuItem(this.getHtmlText(menuIcons[i], menuItems[i]));
            menuItem.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            String item = menuItems[i];
            menuItem.addActionListener((e) -> this.handleMenuAction(item));
            menu.add(menuItem);
            if (i < menuItems.length - 1) menu.addSeparator();
        }
        menu.show(button, 0, button.getHeight());
    }

    private void handleMenuAction(String menuItem) {
        switch (menuItem) {
            case "Liste Clients": new ListeClients().setVisible(true); this.dispose(); break;
            case "Nouveau Client": new AjouterClient(null).setVisible(true); this.dispose(); break;
            case "Facture": new FactureUI().setVisible(true); this.dispose(); break;
            case "Liste Factures": new ListeFactures().setVisible(true); this.dispose(); break;
            case "Bon de Livraison": new BLUI().setVisible(true); this.dispose(); break;
            case "Bon de Sortie": new BonSortieUI().setVisible(true); this.dispose(); break;
            case "Devis": new DevisUI().setVisible(true); this.dispose(); break;
            case "Consulter Stock": new GestionStock().setVisible(true); this.dispose(); break;
            case "Gestion Entrée": new GestionEntreesUI().setVisible(true); this.dispose(); break;
            case "Gestion Sortie": new GestionSortiesUI().setVisible(true); this.dispose(); break;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                new AjouterFournisseur(null);
            } catch (Exception e) {
                log.error("Failed to start AjouterFournisseur", e);
            }
        });
    }
}