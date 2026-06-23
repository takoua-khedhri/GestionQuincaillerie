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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
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

public class AjoutArticle extends JFrame {

    // Composants du formulaire
    private JTextField txtCode;
    private JTextField txtDesignation;
    private JTextField txtPrixGros;
    private JTextField txtPrixDetail;
    private JTextField txtQuantite;
    private JTextField txtTVA; // Remplacé JSpinner par JTextField
    private JButton btnValider;
    private JButton btnAnnuler;

    // Police d'icônes
    private Font fontAwesomeSolid;

    // Couleurs du thème
    private final Color COLOR_NAV_BG = new Color(52, 73, 94);
    private final Color COLOR_ACTIVE = new Color(41, 128, 185);
    private final Color COLOR_HEADER = new Color(44, 62, 80);
    private final Color COLOR_BG_MAIN = new Color(240, 242, 245);
    private final Color COLOR_BTN_VALID = new Color(46, 204, 113);
    private final Color COLOR_BTN_CANCEL = new Color(231, 76, 60);

    public AjoutArticle() {
        this.loadFontAwesome();
        this.setTitle("Gestion Stock - Ajouter un Article");
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.setExtendedState(JFrame.MAXIMIZED_BOTH);
        this.setLocationRelativeTo(null);

        this.setLayout(new BorderLayout(10, 10));
        this.getContentPane().setBackground(COLOR_BG_MAIN);

        this.initUI();

        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                txtCode.requestFocus();
            }
        });

        this.setVisible(true);
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
        return this.fontAwesomeSolid != null ? this.fontAwesomeSolid.deriveFont(Font.PLAIN, (float) size) : new Font("SansSerif", Font.PLAIN, size);
    }

    private String getHtmlText(String iconCode, String text) {
        String fontName = (this.fontAwesomeSolid != null) ? this.fontAwesomeSolid.getFontName() : "SansSerif";
        return "<html><font face=\"" + fontName + "\">" + iconCode + "</font> " + text + "</html>";
    }

    // --- BARRE DE NAVIGATION ---
    private JPanel createNavigationBar() {
        JPanel navPanel = new JPanel(new GridLayout(1, 4));
        navPanel.setBackground(new Color(52, 73, 94));

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

        String[] clientsMenuItems = new String[]{"Liste Clients", "Nouveau Client"};
        String[] clientsMenuIcons = new String[]{"\uf0c0", "\uf067"};
        JButton btnClients = this.createNavButtonWithMenu("\uf0c0", "Clients", false, clientsMenuItems, clientsMenuIcons);
        navPanel.add(btnClients);

        String[] documentsMenuItems = new String[]{"Facture", "Liste Factures", "Bon de Livraison", "Bon de Sortie", "Devis"};
        String[] documentsMenuIcons = new String[]{"\uf15b", "\uf15b", "\uf15b", "\uf15b", "\uf15b"};
        JButton btnDocuments = this.createNavButtonWithMenu("\uf15b", "Documents", false, documentsMenuItems, documentsMenuIcons);
        navPanel.add(btnDocuments);

        String[] stockMenuItems = new String[]{"Consulter Stock", "Gestion Entrée", "Gestion Sortie", "Historique Entrée", "Historique Sortie"};
        String[] stockMenuIcons = new String[]{"\uf494", "\uf090", "\uf08b", "\uf1da", "\uf201"};
        JButton btnStock = this.createNavButtonWithMenu("\uf494", "Stock", true, stockMenuItems, stockMenuIcons);
        navPanel.add(btnStock);

        return navPanel;
    }

    private JButton createNavButtonWithMenu(String icon, String text, final boolean isActive, final String[] menuItems, final String[] menuIcons) {
        String fontName = (this.fontAwesomeSolid != null) ? this.fontAwesomeSolid.getFontName() : "SansSerif";
        String chevron = "\uf078";

        String htmlContent = "<html><font face=\"" + fontName + "\">" + icon + "</font> "
                + text
                + " <font face=\"" + fontName + "\" size=\"3\">" + chevron + "</font></html>";

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
                AjoutArticle.this.showMenuForButton(button, menuItems, menuIcons);
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

    // Navigation methods
    private void ouvrirListeClients() { new ListeClients().setVisible(true); this.dispose(); }
    private void ouvrirAjouterClient() { new AjouterClient(null).setVisible(true); this.dispose(); }
    private void ouvrirFacture() { new FactureUI().setVisible(true); this.dispose(); }
    private void ouvrirListeFacture() { new ListeFactures().setVisible(true); this.dispose(); }
    private void ouvrirBonLivraison() { new BLUI().setVisible(true); this.dispose(); }
    private void ouvrirBonSortie() { new BonSortieUI().setVisible(true); this.dispose(); }
    private void ouvrirDevis() { new DevisUI().setVisible(true); this.dispose(); }
    private void ouvrirGestionStock() { new GestionStock().setVisible(true); this.dispose(); }
    private void ouvrirGestionEntrees() { new GestionEntreesUI().setVisible(true); this.dispose(); }
    private void ouvrirGestionSorties() { new GestionSortiesUI().setVisible(true); this.dispose(); }
    private void ouvrirHistoriqueEntrees() { new HistoriqueEntreesUI().setVisible(true); this.dispose(); }
    private void ouvrirHistoriqueSorties() { new HistoriqueSortiesUI().setVisible(true); this.dispose(); }

    private void handleMenuAction(String menuItem) {
        switch (menuItem) {
            case "Liste Clients": this.ouvrirListeClients(); break;
            case "Nouveau Client": this.ouvrirAjouterClient(); break;
            case "Facture": this.ouvrirFacture(); break;
            case "Liste Factures": this.ouvrirListeFacture(); break;
            case "Bon de Livraison": this.ouvrirBonLivraison(); break;
            case "Bon de Sortie": this.ouvrirBonSortie(); break;
            case "Devis": this.ouvrirDevis(); break;
            case "Consulter Stock": this.ouvrirGestionStock(); break;
            case "Gestion Entrée": this.ouvrirGestionEntrees(); break;
            case "Gestion Sortie": this.ouvrirGestionSorties(); break;
            case "Historique Entrée": this.ouvrirHistoriqueEntrees(); break;
            case "Historique Sortie": this.ouvrirHistoriqueSorties(); break;
        }
    }

    // --- INTERFACE PRINCIPALE ---
    private void initUI() {
        JPanel navBarPanel = this.createNavigationBar();

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(COLOR_HEADER);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 30, 10, 30));

        JLabel titleLabel = new JLabel("AJOUTER UN ARTICLE", JLabel.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 26));
        titleLabel.setForeground(Color.WHITE);

        JLabel subTitleLabel = new JLabel("Scannez le code barre ou saisissez les informations", JLabel.CENTER);
        subTitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subTitleLabel.setForeground(new Color(200, 200, 200));
        subTitleLabel.setBorder(BorderFactory.createEmptyBorder(3, 0, 0, 0));

        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(COLOR_HEADER);
        titlePanel.add(titleLabel, BorderLayout.CENTER);
        titlePanel.add(subTitleLabel, BorderLayout.SOUTH);

        headerPanel.add(titlePanel, BorderLayout.CENTER);

        // Formulaire
        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setBackground(COLOR_BG_MAIN);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(Color.WHITE);
        formPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(225, 225, 225), 1),
                BorderFactory.createEmptyBorder(15, 40, 20, 40)));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 15, 8, 15);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.weighty = 0.0;
        gbc.gridwidth = 1;
        gbc.ipadx = 200;

        Font labelFont = new Font("Segoe UI", Font.BOLD, 14);

        // 1. Code Barre
        this.addFormField(formPanel, gbc, 0, "Code Barre / Scan", this.txtCode = this.createStyledTextField("Scannez le code ici..."), labelFont);
        this.txtCode.addActionListener(e -> this.txtDesignation.requestFocus());

        // 2. Désignation
        this.addFormField(formPanel, gbc, 1, "Désignation *", this.txtDesignation = this.createStyledTextField("Ex: Ordinateur Portable HP"), labelFont);

        // 3. Prix unitaire gros TTC (label modifié)
        this.addFormField(formPanel, gbc, 2, "Prix unitaire gros TTC (DT) *", this.txtPrixGros = this.createStyledTextField("Ex: 1250.000"), labelFont);

        // 4. Prix unitaire détail TTC (label modifié)
        this.addFormField(formPanel, gbc, 3, "Prix unitaire détail TTC (DT) *", this.txtPrixDetail = this.createStyledTextField("Ex: 1500.000"), labelFont);

        // 5. Quantité initiale
        this.addFormField(formPanel, gbc, 4, "Quantité initiale", this.txtQuantite = this.createStyledTextField("0"), labelFont);

        // 6. TVA — JTextField simple initialisé à 0 (remplace JSpinner)
        gbc.gridx = 0; gbc.gridy = 5;
        gbc.ipadx = 0;
        JLabel lblTVA = new JLabel("TVA (%)");
        lblTVA.setFont(labelFont);
        lblTVA.setForeground(new Color(52, 73, 94));
        formPanel.add(lblTVA, gbc);

        gbc.gridx = 1;
        gbc.ipadx = 200;
        JPanel tvaPanel = this.createTVAField();
        formPanel.add(tvaPanel, gbc);

        // Conteneur formulaire
        JPanel formContainer = new JPanel(new GridBagLayout());
        formContainer.setBackground(COLOR_BG_MAIN);
        GridBagConstraints formGbc = new GridBagConstraints();
        formGbc.gridx = 0;
        formGbc.gridy = 0;
        formGbc.insets = new Insets(5, 20, 5, 20);
        formContainer.add(formPanel, formGbc);
        centerPanel.add(formContainer);

        // Boutons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        buttonPanel.setBackground(COLOR_BG_MAIN);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 15, 0));

        this.btnValider = this.createModernButton(this.getHtmlText("\uf00c", " Valider"), COLOR_BTN_VALID);
        this.btnAnnuler = this.createModernButton(this.getHtmlText("\uf00d", " Annuler"), COLOR_BTN_CANCEL);

        this.btnValider.setPreferredSize(new Dimension(180, 50));
        this.btnAnnuler.setPreferredSize(new Dimension(180, 50));

        buttonPanel.add(this.btnValider);
        buttonPanel.add(this.btnAnnuler);

        // Assemblage final
        this.add(navBarPanel, BorderLayout.NORTH);

        JPanel mainContentPanel = new JPanel(new BorderLayout());
        mainContentPanel.setBackground(COLOR_BG_MAIN);
        mainContentPanel.add(headerPanel, BorderLayout.NORTH);
        mainContentPanel.add(centerPanel, BorderLayout.CENTER);
        mainContentPanel.add(buttonPanel, BorderLayout.SOUTH);

        this.add(mainContentPanel, BorderLayout.CENTER);

        this.btnValider.addActionListener((e) -> this.validerAjout());
        this.btnAnnuler.addActionListener((e) -> this.ouvrirGestionStock());

        this.setupPrixValidation();
        this.setupQuantiteValidation();
    }

    private void addFormField(JPanel panel, GridBagConstraints gbc, int y, String label, Component field, Font font) {
        gbc.gridx = 0; gbc.gridy = y;
        gbc.ipadx = 0;
        JLabel lbl = new JLabel(label);
        lbl.setFont(font);
        lbl.setForeground(new Color(52, 73, 94));
        panel.add(lbl, gbc);

        gbc.gridx = 1;
        gbc.ipadx = 200;
        panel.add(field, gbc);
    }

    // TVA : JTextField simple initialisé à "0" (remplace createTVASpinner)
    private JPanel createTVAField() {
        JPanel tvaPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        tvaPanel.setBackground(Color.WHITE);

        this.txtTVA = new JTextField("0", 25);
        this.txtTVA.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        this.txtTVA.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(12, 15, 12, 15)));
        this.txtTVA.setPreferredSize(new Dimension(400, 45));

        tvaPanel.add(this.txtTVA);
        return tvaPanel;
    }

    private JTextField createStyledTextField(final String placeholder) {
        final JTextField textField = new JTextField(25);
        textField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        textField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(12, 15, 12, 15)));
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

    private void setupPrixValidation() {
        this.txtPrixGros.getDocument().addDocumentListener(new DocumentListener() {
            private void valider() {
                String text = txtPrixGros.getText().trim();
                if (!text.isEmpty() && !text.startsWith("Ex:")) {
                    try {
                        Double.parseDouble(text.replace(',', '.'));
                        txtPrixGros.setBorder(BorderFactory.createCompoundBorder(
                                BorderFactory.createLineBorder(COLOR_BTN_VALID, 2),
                                BorderFactory.createEmptyBorder(10, 13, 10, 13)));
                    } catch (NumberFormatException e) {
                        txtPrixGros.setBorder(BorderFactory.createCompoundBorder(
                                BorderFactory.createLineBorder(COLOR_BTN_CANCEL, 2),
                                BorderFactory.createEmptyBorder(10, 13, 10, 13)));
                    }
                } else {
                    txtPrixGros.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(new Color(200, 200, 200)),
                            BorderFactory.createEmptyBorder(12, 15, 12, 15)));
                }
            }
            public void insertUpdate(DocumentEvent e) { valider(); }
            public void removeUpdate(DocumentEvent e) { valider(); }
            public void changedUpdate(DocumentEvent e) { valider(); }
        });

        this.txtPrixDetail.getDocument().addDocumentListener(new DocumentListener() {
            private void valider() {
                String text = txtPrixDetail.getText().trim();
                if (!text.isEmpty() && !text.startsWith("Ex:")) {
                    try {
                        Double.parseDouble(text.replace(',', '.'));
                        txtPrixDetail.setBorder(BorderFactory.createCompoundBorder(
                                BorderFactory.createLineBorder(COLOR_BTN_VALID, 2),
                                BorderFactory.createEmptyBorder(10, 13, 10, 13)));
                    } catch (NumberFormatException e) {
                        txtPrixDetail.setBorder(BorderFactory.createCompoundBorder(
                                BorderFactory.createLineBorder(COLOR_BTN_CANCEL, 2),
                                BorderFactory.createEmptyBorder(10, 13, 10, 13)));
                    }
                } else {
                    txtPrixDetail.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(new Color(200, 200, 200)),
                            BorderFactory.createEmptyBorder(12, 15, 12, 15)));
                }
            }
            public void insertUpdate(DocumentEvent e) { valider(); }
            public void removeUpdate(DocumentEvent e) { valider(); }
            public void changedUpdate(DocumentEvent e) { valider(); }
        });
    }

    private void setupQuantiteValidation() {
        this.txtQuantite.getDocument().addDocumentListener(new DocumentListener() {
            private void valider() {
                String text = txtQuantite.getText().trim();
                if (!text.isEmpty() && !text.startsWith("Ex:") && !text.equals("0")) {
                    try {
                        int quantite = Integer.parseInt(text);
                        if (quantite >= 0) {
                            txtQuantite.setBorder(BorderFactory.createCompoundBorder(
                                    BorderFactory.createLineBorder(COLOR_BTN_VALID, 2),
                                    BorderFactory.createEmptyBorder(10, 13, 10, 13)));
                        } else {
                            txtQuantite.setBorder(BorderFactory.createCompoundBorder(
                                    BorderFactory.createLineBorder(COLOR_BTN_CANCEL, 2),
                                    BorderFactory.createEmptyBorder(10, 13, 10, 13)));
                        }
                    } catch (NumberFormatException e) {
                        txtQuantite.setBorder(BorderFactory.createCompoundBorder(
                                BorderFactory.createLineBorder(COLOR_BTN_CANCEL, 2),
                                BorderFactory.createEmptyBorder(10, 13, 10, 13)));
                    }
                } else {
                    txtQuantite.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(new Color(200, 200, 200)),
                            BorderFactory.createEmptyBorder(12, 15, 12, 15)));
                }
            }
            public void insertUpdate(DocumentEvent e) { valider(); }
            public void removeUpdate(DocumentEvent e) { valider(); }
            public void changedUpdate(DocumentEvent e) { valider(); }
        });
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
        button.setPreferredSize(new Dimension(180, 50));

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent evt) {
                button.setBackground(AjoutArticle.this.darkenColor(backgroundColor, 0.9F));
                button.setBorder(BorderFactory.createLineBorder(AjoutArticle.this.darkenColor(backgroundColor, 0.8F), 1));
            }
            @Override
            public void mouseExited(MouseEvent evt) {
                button.setBackground(backgroundColor);
                button.setBorder(BorderFactory.createEmptyBorder(12, 30, 12, 30));
            }
            @Override
            public void mousePressed(MouseEvent evt) {
                button.setBackground(AjoutArticle.this.darkenColor(backgroundColor, 0.8F));
            }
            @Override
            public void mouseReleased(MouseEvent evt) {
                button.setBackground(AjoutArticle.this.darkenColor(backgroundColor, 0.9F));
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

    private String getRealTextFieldValue(JTextField textField, String placeholder) {
        String value = textField.getText().trim();
        return !value.equals(placeholder) && !value.isEmpty() ? value : "";
    }

    // --- LOGIQUE METIER & BASE DE DONNEES ---

    private void validerAjout() {
        // 1. Récupération des valeurs
        String code = this.getRealTextFieldValue(this.txtCode, "Scannez le code ici...");
        String designation = this.getRealTextFieldValue(this.txtDesignation, "Ex: Ordinateur Portable HP");
        String prixGrosStr = this.getRealTextFieldValue(this.txtPrixGros, "Ex: 1250.000");
        String prixDetailStr = this.getRealTextFieldValue(this.txtPrixDetail, "Ex: 1500.000");
        String quantiteStr = this.getRealTextFieldValue(this.txtQuantite, "0");

        // 2. Récupération et validation de la TVA
        int tva = 0;
        try {
            String tvaStr = this.txtTVA.getText().trim();
            if (!tvaStr.isEmpty()) {
                tva = Integer.parseInt(tvaStr);
                if (tva < 0 || tva > 100) {
                    JOptionPane.showMessageDialog(this, "La TVA doit être entre 0 et 100.", "TVA invalide", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "La TVA doit être un nombre entier (ex: 19).", "Erreur TVA", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 3. Vérifications de base
        if (designation.isEmpty() || prixGrosStr.isEmpty() || prixDetailStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Les champs Désignation, Prix unitaire gros et Prix unitaire détail sont obligatoires", "Erreur", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 4. Vérification unicité du Code Barre
        if (!code.isEmpty() && !checkCodeUnique(code)) {
            JOptionPane.showMessageDialog(this, "Ce Code Barre est déjà utilisé pour un autre article !", "Code en doublon", JOptionPane.ERROR_MESSAGE);
            this.txtCode.requestFocus();
            this.txtCode.selectAll();
            return;
        }

        // 5. Vérification unicité de la Désignation
        if (!checkDesignationUnique(designation)) {
            JOptionPane.showMessageDialog(this, "Cette désignation existe déjà.", "Doublon", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 6. Traitement et Enregistrement
        try {
            double prixGros = Double.parseDouble(prixGrosStr.replace(',', '.'));
            double prixDetail = Double.parseDouble(prixDetailStr.replace(',', '.'));
            int quantite = 0;

            if (!quantiteStr.isEmpty() && !quantiteStr.equals("0")) {
                quantite = Integer.parseInt(quantiteStr);
            }

            if (prixGros <= 0 || prixDetail <= 0) {
                JOptionPane.showMessageDialog(this, "Les prix doivent être supérieurs à 0.", "Prix invalide", JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (quantite < 0) {
                JOptionPane.showMessageDialog(this, "La quantité ne peut pas être négative.", "Quantité invalide", JOptionPane.WARNING_MESSAGE);
                return;
            }

            this.ajouterArticle(code, designation, prixGros, prixDetail, quantite, tva);

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                    "Format invalide.\nPrix : utilisez le point comme séparateur (ex: 12.500)\nQuantité : nombre entier",
                    "Erreur Format", JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean checkCodeUnique(String code) {
        String sql = "SELECT COUNT(*) FROM Articles WHERE code = ?";
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, code);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) return rs.getInt(1) == 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return true;
    }

    private boolean checkDesignationUnique(String designation) {
        String sql = "SELECT COUNT(*) FROM Articles WHERE LOWER(designation) = LOWER(?)";
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, designation);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) return rs.getInt(1) == 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return true;
    }

    private void ajouterArticle(String code, String designation, double prixGros, double prixDetail, int quantite, int tva) {
        String sql = "INSERT INTO Articles (code, designation, prix_gros, prix_detail, stock, tva) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setString(1, code);
            pst.setString(2, designation);
            pst.setDouble(3, prixGros);
            pst.setDouble(4, prixDetail);
            pst.setInt(5, quantite);
            pst.setInt(6, tva);

            pst.executeUpdate();

            String message = "Article ajouté avec succès";
            message += quantite > 0
                    ? "\nQuantité initiale : " + quantite + " unités"
                    : "\nQuantité initiale : 0 unités";

            JOptionPane.showMessageDialog(this, message, "Succès", JOptionPane.INFORMATION_MESSAGE);
            this.ouvrirGestionStock();

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erreur lors de l'ajout de l'article: " + e.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                System.setProperty("awt.useSystemAAFontSettings", "on");
                System.setProperty("swing.aatext", "true");
                new AjoutArticle();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}