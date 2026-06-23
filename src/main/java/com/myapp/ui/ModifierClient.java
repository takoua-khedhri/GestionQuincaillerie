package com.myapp.ui;

import com.myapp.db.ConnexionSQLite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class ModifierClient extends JDialog {

    private JTextField txtNom;
    private JTextField txtPrenom;
    private JTextField txtNumero;
    private JTextField txtAdresse;
    private JTextField txtMatriculeFiscale;
    private JButton btnValider;
    private JButton btnAnnuler;
    private JButton btnRetour;
    
    private final ListeClients parent;
    private final int clientId;
    private Font fontAwesomeSolid;

    // Constantes de style
    private static final String DEFAULT_FONT = "SansSerif";
    private static final Color COLOR_ERROR = new Color(231, 76, 60);
    private static final Color COLOR_SUCCESS = new Color(46, 204, 113);
    private static final Color COLOR_BORDER = new Color(200, 200, 200);
    private static final Color COLOR_HEADER = new Color(44, 62, 80);
    private static final Color COLOR_BG = new Color(240, 242, 245);
    private static final Color COLOR_BTN_BACK = new Color(52, 152, 219);

    public ModifierClient(ListeClients parent, int id, String nom, String prenom, int numero, String adresse, String matriculeFiscale) {
        super(parent, "Modifier le Client", true);
        this.parent = parent;
        this.clientId = id;

        this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        this.setSize(900, 650);
        this.setLocationRelativeTo(parent);
        this.setLayout(new BorderLayout(0, 0)); // Padding géré par les panels

        this.loadFontAwesome7();
        this.initUI(nom, prenom, numero, adresse, matriculeFiscale);
        this.setVisible(true);
    }

    private void loadFontAwesome7() {
        String basePath = "/resources/fonts/fontawesome-free-7.1.0-desktop/otfs/";
        // Tente de charger la police (ajustez le chemin selon votre structure de projet)
        boolean solidLoaded = this.loadFontFile("/fonts/fa.ttf", "Solid"); 
        
        // Fallback si chemin spécifique
        if (!solidLoaded) {
             solidLoaded = this.loadFontFile(basePath + "Font Awesome 7 Free-Solid-900.otf", "Solid");
        }

        if (this.fontAwesomeSolid == null) {
            this.fontAwesomeSolid = new Font(DEFAULT_FONT, Font.PLAIN, 14);
        }
    }

    private boolean loadFontFile(String path, String fontType) {
        try (InputStream fontStream = this.getClass().getResourceAsStream(path)) {
            if (fontStream != null) {
                Font font = Font.createFont(Font.TRUETYPE_FONT, fontStream);
                GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
                ge.registerFont(font);
                this.fontAwesomeSolid = font.deriveFont(Font.PLAIN, 14);
                return true;
            }
        } catch (IOException | FontFormatException e) {
            // Ignorer silencieusement ou logger
        }
        return false;
    }

    // Helper pour afficher Icône + Texte proprement
    private String getHtmlText(String iconCode, String text) {
        String fontName = (this.fontAwesomeSolid != null) ? this.fontAwesomeSolid.getFontName() : DEFAULT_FONT;
        return "<html><span style='font-family:" + fontName + ";'>" + iconCode + "</span> " + text + "</html>";
    }

    private void validerMatriculeEnTempsReel() {
        // Fonction vide car plus de validation
    }

    private void appliquerMasqueMatricule() {
        // Fonction vide car plus de masque
    }

    private JTextField createTextFieldWithPlaceholder(final String placeholder, String initialValue) {
        final JTextField textField = new JTextField();
        textField.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        if (initialValue != null && !initialValue.isEmpty() && !initialValue.equals("0")) {
            textField.setText(initialValue);
            textField.setForeground(Color.BLACK);
        } else {
            textField.setText("");
            textField.setForeground(Color.BLACK);
        }

        textField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent evt) {
                // Rien à faire
            }

            @Override
            public void focusLost(FocusEvent evt) {
                // Rien à faire
            }
        });

        textField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BORDER),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        return textField;
    }

    private String getRealTextFieldValue(JTextField textField, String placeholder) {
        String value = textField.getText().trim();
        return !value.isEmpty() ? value : "";
    }

    private void initUI(String nom, String prenom, int numero, String adresse, String matriculeFiscale) {
        // --- 1. HEADER PANEL ---
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(COLOR_HEADER);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        // Titre avec icône Edit
        JLabel titleLabel = new JLabel(getHtmlText("\uf044", " MODIFIER LE CLIENT"));
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setHorizontalAlignment(JLabel.CENTER);

        // Bouton Retour avec icône Flèche
        this.btnRetour = new JButton(getHtmlText("\uf060", " Retour"));
        this.btnRetour.setFont(new Font("Segoe UI", Font.BOLD, 13));
        this.btnRetour.setBackground(COLOR_BTN_BACK);
        this.btnRetour.setForeground(Color.WHITE);
        this.btnRetour.setFocusPainted(false);
        this.btnRetour.setBorderPainted(false);
        this.btnRetour.setOpaque(true);
        this.btnRetour.setCursor(new Cursor(Cursor.HAND_CURSOR));
        this.btnRetour.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));

        this.btnRetour.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent evt) {
                btnRetour.setBackground(COLOR_BTN_BACK.darker());
            }
            @Override
            public void mouseExited(MouseEvent evt) {
                btnRetour.setBackground(COLOR_BTN_BACK);
            }
        });
        this.btnRetour.addActionListener(e -> {
            if (this.parent != null) this.parent.setVisible(true);
            this.dispose();
        });

        JPanel leftHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        leftHeader.setOpaque(false);
        leftHeader.add(this.btnRetour);

        // Dummy panel pour équilibrer le layout si nécessaire
        JPanel rightHeader = new JPanel(); 
        rightHeader.setOpaque(false);
        rightHeader.setPreferredSize(leftHeader.getPreferredSize());

        headerPanel.add(leftHeader, BorderLayout.WEST);
        headerPanel.add(titleLabel, BorderLayout.CENTER);
        headerPanel.add(rightHeader, BorderLayout.EAST);

        // --- 2. FORMULAIRE CENTRAL ---
        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setBackground(COLOR_BG);

        JPanel formPanel = new JPanel(new GridLayout(5, 2, 20, 20));
        formPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BORDER),
                BorderFactory.createEmptyBorder(40, 50, 40, 50)));
        formPanel.setBackground(Color.WHITE);
        formPanel.setPreferredSize(new Dimension(700, 400));

        Font labelFont = new Font("Segoe UI", Font.BOLD, 14);
        Color labelColor = new Color(52, 73, 94);

        // Nom (Icône User)
        JLabel lblNom = new JLabel(getHtmlText("\uf007", " Nom :"));
        lblNom.setFont(labelFont);
        lblNom.setForeground(labelColor);
        this.txtNom = this.createTextFieldWithPlaceholder("Entrez le nom du client", nom);

        // Prénom (Icône User)
        JLabel lblPrenom = new JLabel(getHtmlText("\uf007", " Prénom :"));
        lblPrenom.setFont(labelFont);
        lblPrenom.setForeground(labelColor);
        this.txtPrenom = this.createTextFieldWithPlaceholder("Entrez le prénom du client", prenom);

        // Numéro (Icône Phone)
        JLabel lblNumero = new JLabel(getHtmlText("\uf095", " Numéro :"));
        lblNumero.setFont(labelFont);
        lblNumero.setForeground(labelColor);
        this.txtNumero = this.createTextFieldWithPlaceholder("Ex: 12345678", String.valueOf(numero));

        // Adresse (Icône Map Marker)
        JLabel lblAdresse = new JLabel(getHtmlText("\uf3c5", " Adresse :"));
        lblAdresse.setFont(labelFont);
        lblAdresse.setForeground(labelColor);
        this.txtAdresse = this.createTextFieldWithPlaceholder("Entrez l'adresse complète", adresse != null ? adresse : "");

        // Matricule Fiscale (Icône ID Card) - sans placeholder, sans validation
        JLabel lblMatriculeFiscale = new JLabel(getHtmlText("\uf2c2", " Matricule Fiscale :"));
        lblMatriculeFiscale.setFont(labelFont);
        lblMatriculeFiscale.setForeground(labelColor);
        this.txtMatriculeFiscale = new JTextField();
        this.txtMatriculeFiscale.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        if (matriculeFiscale != null && !matriculeFiscale.isEmpty() && !matriculeFiscale.equals("0")) {
            this.txtMatriculeFiscale.setText(matriculeFiscale);
            this.txtMatriculeFiscale.setForeground(Color.BLACK);
        } else {
            this.txtMatriculeFiscale.setText("");
            this.txtMatriculeFiscale.setForeground(Color.BLACK);
        }

        this.txtMatriculeFiscale.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent evt) {
                // Rien à faire
            }
            @Override
            public void focusLost(FocusEvent evt) {
                // Rien à faire
            }
        });

        this.txtMatriculeFiscale.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BORDER),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        
        formPanel.add(lblMatriculeFiscale);
        formPanel.add(this.txtMatriculeFiscale);

        // Ajout des autres composants au formPanel
        formPanel.add(lblNom);
        formPanel.add(this.txtNom);
        formPanel.add(lblPrenom);
        formPanel.add(this.txtPrenom);
        formPanel.add(lblNumero);
        formPanel.add(this.txtNumero);
        formPanel.add(lblAdresse);
        formPanel.add(this.txtAdresse);

        centerPanel.add(formPanel);

        // --- 3. BOUTONS D'ACTION ---
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 25, 20));
        buttonPanel.setBackground(COLOR_BG);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 30, 0));

        // Bouton Valider avec icône Check (\uf00c)
        this.btnValider = this.createStyledButton(getHtmlText("\uf00c", " Valider"), COLOR_SUCCESS);
        
        // Bouton Annuler avec icône Times (\uf00d)
        this.btnAnnuler = this.createStyledButton(getHtmlText("\uf00d", " Annuler"), COLOR_ERROR);

        buttonPanel.add(this.btnValider);
        buttonPanel.add(this.btnAnnuler);

        // --- Assemblage Final ---
        JPanel mainContentPanel = new JPanel(new BorderLayout());
        mainContentPanel.add(headerPanel, BorderLayout.NORTH);
        mainContentPanel.add(centerPanel, BorderLayout.CENTER);
        mainContentPanel.add(buttonPanel, BorderLayout.SOUTH);

        this.add(mainContentPanel, BorderLayout.CENTER);

        // Listeners Actions
        this.btnValider.addActionListener(e -> this.validerModification());
        this.btnAnnuler.addActionListener(e -> {
            if (this.parent != null) this.parent.setVisible(true);
            this.dispose();
        });
    }

    private JButton createStyledButton(String text, final Color backgroundColor) {
        final JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setBackground(backgroundColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createEmptyBorder(10, 25, 10, 25));
        button.setPreferredSize(new Dimension(160, 45));

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent evt) {
                button.setBackground(ModifierClient.this.darkenColor(backgroundColor, 0.9F));
            }
            @Override
            public void mouseExited(MouseEvent evt) {
                button.setBackground(backgroundColor);
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
        String nouveauNom = this.getRealTextFieldValue(this.txtNom, "Entrez le nom du client");
        String nouveauPrenom = this.getRealTextFieldValue(this.txtPrenom, "Entrez le prénom du client");
        String nouveauNumeroStr = this.getRealTextFieldValue(this.txtNumero, "Ex: 12345678");
        String nouvelleAdresse = this.getRealTextFieldValue(this.txtAdresse, "Entrez l'adresse complète");
        String nouveauMatriculeFiscaleStr = this.getRealTextFieldValue(this.txtMatriculeFiscale, "");

        if (!nouveauNom.isEmpty() && !nouveauPrenom.isEmpty() && !nouveauNumeroStr.isEmpty()) {
            try {
                int nouveauNumero = Integer.parseInt(nouveauNumeroStr);
                String nouveauMatriculeFiscaleFormate = nouveauMatriculeFiscaleStr.isEmpty() ? null : nouveauMatriculeFiscaleStr;
                this.mettreAJourClient(nouveauNom, nouveauPrenom, nouveauNumero, nouvelleAdresse, nouveauMatriculeFiscaleFormate);
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Le numéro doit être un nombre valide", "Erreur", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(this, "Les champs Nom, Prénom et Numéro sont obligatoires", "Erreur", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void mettreAJourClient(String nom, String prenom, int numero, String adresse, String matriculeFiscale) {
        String sql = "UPDATE clients SET nom = ?, prenom = ?, numero = ?, adresse = ?, matricule_fiscale = ? WHERE id = ?";
        
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setString(1, nom);
            pst.setString(2, prenom);
            pst.setInt(3, numero);
            pst.setString(4, adresse);
            pst.setString(5, matriculeFiscale);
            pst.setInt(6, this.clientId);
            
            pst.executeUpdate();
            
            JOptionPane.showMessageDialog(this, "Client modifié avec succès", "Succès", JOptionPane.INFORMATION_MESSAGE);
            
            if (this.parent != null) {
                this.parent.rafraichirListeClients();
            }
            this.dispose();

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erreur lors de la modification du client: " + e.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }
}