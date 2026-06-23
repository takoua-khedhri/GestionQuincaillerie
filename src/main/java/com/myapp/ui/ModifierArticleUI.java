package com.myapp.ui;

import com.myapp.db.ConnexionSQLite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class ModifierArticleUI extends JDialog {

    // Composants
    private JTextField txtDesignation;
    private JTextField txtPrixGros;
    private JTextField txtPrixDetail;
    private JTextField txtTVA;
    private JLabel lblPrixGrosTTCAffichage;
    private JLabel lblPrixDetailTTCAffichage;
    private JButton btnValider;
    private JButton btnAnnuler;
    private JButton btnRetour;

    // Données
    private int articleId;
    private GestionStock parent;
    private Font fontAwesomeSolid;

    // Couleurs
    private final Color COLOR_HEADER    = new Color(44, 62, 80);
    private final Color COLOR_BG        = new Color(240, 242, 245);
    private final Color COLOR_SUCCESS   = new Color(46, 204, 113);
    private final Color COLOR_ERROR     = new Color(231, 76, 60);
    private final Color COLOR_TEXT      = new Color(52, 73, 94);
    private final Color COLOR_BORDER    = new Color(200, 200, 200);

    public ModifierArticleUI(GestionStock parent, int articleId,
                             String designation, double prixGros, int tva) {
        super(parent, "Modifier l'Article", true);
        this.parent    = parent;
        this.articleId = articleId;

        this.loadFontAwesome();
        this.setSize(900, 700);
        this.setLocationRelativeTo(parent);
        this.setLayout(new BorderLayout(0, 0));
        this.setResizable(true);

        this.initUI(designation, prixGros, tva);
    }

    // =========================================================================
    // FONT AWESOME
    // =========================================================================
    private void loadFontAwesome() {
        try {
            InputStream fontStream = this.getClass().getResourceAsStream("/fonts/fa.ttf");
            if (fontStream != null) {
                Font font = Font.createFont(Font.TRUETYPE_FONT, fontStream);
                GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
                this.fontAwesomeSolid = font.deriveFont(Font.PLAIN, 14f);
                fontStream.close();
            } else {
                this.fontAwesomeSolid = new Font("SansSerif", Font.PLAIN, 14);
            }
        } catch (IOException | FontFormatException e) {
            this.fontAwesomeSolid = new Font("SansSerif", Font.PLAIN, 14);
        }
    }

    private String getHtmlText(String iconCode, String text) {
        String fn = (fontAwesomeSolid != null) ? fontAwesomeSolid.getFontName() : "SansSerif";
        return "<html><font face=\"" + fn + "\">" + iconCode + "</font> " + text + "</html>";
    }

    // =========================================================================
    // INIT UI
    // =========================================================================
    private void initUI(String designation, double prixGros, int tva) {

        // ── HEADER ────────────────────────────────────────────────────────────
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(COLOR_HEADER);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

        this.btnRetour = new JButton(getHtmlText("\uf060", " Retour"));
        this.btnRetour.setFont(new Font("Segoe UI", Font.BOLD, 12));
        this.btnRetour.setForeground(Color.WHITE);
        this.btnRetour.setBackground(new Color(255, 255, 255, 30));
        this.btnRetour.setFocusPainted(false);
        this.btnRetour.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(255, 255, 255, 100), 1),
            BorderFactory.createEmptyBorder(8, 15, 8, 15)
        ));
        this.btnRetour.setContentAreaFilled(false);
        this.btnRetour.setOpaque(false);
        this.btnRetour.setCursor(new Cursor(Cursor.HAND_CURSOR));
        this.btnRetour.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btnRetour.setOpaque(true);  btnRetour.setBackground(new Color(255,255,255,50)); }
            public void mouseExited (MouseEvent e) { btnRetour.setOpaque(false); btnRetour.setBackground(new Color(255,255,255,30)); }
        });
        this.btnRetour.addActionListener(e -> this.dispose());

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        leftPanel.setOpaque(false);
        leftPanel.add(this.btnRetour);

        JLabel titleLabel = new JLabel(getHtmlText("\uf044", " MODIFICATION D'ARTICLE"), JLabel.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        titleLabel.setForeground(Color.WHITE);

        JLabel subTitleLabel = new JLabel("Modifiez les informations de l'article sélectionné", JLabel.CENTER);
        subTitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subTitleLabel.setForeground(new Color(200, 200, 200));

        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setOpaque(false);
        titlePanel.add(titleLabel,    BorderLayout.CENTER);
        titlePanel.add(subTitleLabel, BorderLayout.SOUTH);

        JPanel rightPanel = new JPanel();
        rightPanel.setOpaque(false);
        rightPanel.setPreferredSize(leftPanel.getPreferredSize());

        headerPanel.add(leftPanel,  BorderLayout.WEST);
        headerPanel.add(titlePanel, BorderLayout.CENTER);
        headerPanel.add(rightPanel, BorderLayout.EAST);

        // ── FORMULAIRE ────────────────────────────────────────────────────────
        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setBackground(COLOR_BG);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(Color.WHITE);
        formPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(225, 225, 225), 1),
            BorderFactory.createEmptyBorder(25, 40, 25, 40)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill    = GridBagConstraints.HORIZONTAL;
        gbc.anchor  = GridBagConstraints.WEST;
        gbc.weightx = 1.0;
        gbc.weighty = 0.0;
        gbc.ipadx   = 200;

        Font labelFont = new Font("Segoe UI", Font.BOLD, 14);

        // Titre section
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        gbc.insets = new Insets(0, 0, 20, 0);
        gbc.ipadx  = 0;
        JLabel lblSection = new JLabel("Informations détaillées");
        lblSection.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblSection.setForeground(COLOR_TEXT);
        formPanel.add(lblSection, gbc);
        gbc.gridwidth = 1;

        // 1. Désignation
        double prixDetailActuel = getPrixDetailActuel();
        this.txtDesignation = createStyledTextField(designation);
        addFormRow(formPanel, gbc, 1, getHtmlText("\uf02b", " Désignation *"),
                   this.txtDesignation, labelFont);

        // 2. Prix Gros TTC
        this.txtPrixGros = createStyledTextField(
            String.format(java.util.Locale.US, "%.3f", prixGros));
        addFormRow(formPanel, gbc, 2, getHtmlText("\uf155", " Prix unitaire gros TTC (DT) *"),
                   this.txtPrixGros, labelFont);

        // 3. Prix Détail TTC
        this.txtPrixDetail = createStyledTextField(
            String.format(java.util.Locale.US, "%.3f", prixDetailActuel));
        addFormRow(formPanel, gbc, 3, getHtmlText("\uf155", " Prix unitaire détail TTC (DT) *"),
                   this.txtPrixDetail, labelFont);

        // 4. TVA (JTextField simple comme AjoutArticle)
        gbc.gridx = 0; gbc.gridy = 4;
        gbc.insets = new Insets(8, 15, 8, 15);
        gbc.ipadx  = 0;
        JLabel lblTVA = new JLabel(getHtmlText("\uf295", " TVA (%)"));
        lblTVA.setFont(labelFont);
        lblTVA.setForeground(COLOR_TEXT);
        formPanel.add(lblTVA, gbc);

        gbc.gridx = 1;
        gbc.ipadx = 200;
        this.txtTVA = new JTextField(String.valueOf(tva), 25);
        this.txtTVA.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        this.txtTVA.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(COLOR_BORDER),
            BorderFactory.createEmptyBorder(12, 15, 12, 15)
        ));
        this.txtTVA.setPreferredSize(new Dimension(400, 45));
        formPanel.add(this.txtTVA, gbc);

        // 5. Prix TTC Gros (affichage calculé)
        gbc.gridx = 0; gbc.gridy = 5; gbc.ipadx = 0;
        gbc.insets = new Insets(8, 15, 8, 15);
        JLabel lblPrixGrosTTC = new JLabel(getHtmlText("\uf1ec", " Prix HT gros (DT)"));
        lblPrixGrosTTC.setFont(labelFont);
        lblPrixGrosTTC.setForeground(COLOR_TEXT);
        formPanel.add(lblPrixGrosTTC, gbc);

        gbc.gridx = 1; gbc.ipadx = 200;
        double prixGrosTTCInit = prixGros / (1.0 + (double) tva / 100.0);
        this.lblPrixGrosTTCAffichage = new JLabel(String.format("%.3f DT", prixGrosTTCInit));
        this.lblPrixGrosTTCAffichage.setFont(new Font("Segoe UI", Font.BOLD, 18));
        this.lblPrixGrosTTCAffichage.setForeground(COLOR_SUCCESS);
        this.lblPrixGrosTTCAffichage.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
        formPanel.add(this.lblPrixGrosTTCAffichage, gbc);

        // 6. Prix TTC Détail (affichage calculé)
        gbc.gridx = 0; gbc.gridy = 6; gbc.ipadx = 0;
        JLabel lblPrixDetailTTC = new JLabel(getHtmlText("\uf1ec", " Prix HT détail (DT)"));
        lblPrixDetailTTC.setFont(labelFont);
        lblPrixDetailTTC.setForeground(COLOR_TEXT);
        formPanel.add(lblPrixDetailTTC, gbc);

        gbc.gridx = 1; gbc.ipadx = 200;
        double prixDetailHTInit = prixDetailActuel / (1.0 + (double) tva / 100.0);
        this.lblPrixDetailTTCAffichage = new JLabel(String.format("%.3f DT", prixDetailHTInit));
        this.lblPrixDetailTTCAffichage.setFont(new Font("Segoe UI", Font.BOLD, 18));
        this.lblPrixDetailTTCAffichage.setForeground(COLOR_SUCCESS);
        this.lblPrixDetailTTCAffichage.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
        formPanel.add(this.lblPrixDetailTTCAffichage, gbc);

        // Note stock
        gbc.gridx = 0; gbc.gridy = 7; gbc.gridwidth = 2;
        gbc.insets = new Insets(20, 15, 5, 15); gbc.ipadx = 0;
        JLabel lblStockInfo = new JLabel(
            "<html><i style='color:#95a5a6'>Note: Le stock ne peut pas être modifié ici. Utilisez 'Gestion Entrée/Sortie'.</i></html>");
        lblStockInfo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        formPanel.add(lblStockInfo, gbc);

        centerPanel.add(formPanel);

        // ── BOUTONS ───────────────────────────────────────────────────────────
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 15));
        buttonPanel.setBackground(COLOR_BG);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 25, 0));

        this.btnValider = createModernButton(getHtmlText("\uf00c", " Enregistrer"), COLOR_SUCCESS);
        this.btnAnnuler = createModernButton(getHtmlText("\uf00d", " Annuler"),     COLOR_ERROR);

        this.btnValider.setPreferredSize(new Dimension(180, 50));
        this.btnAnnuler.setPreferredSize(new Dimension(180, 50));

        this.btnValider.addActionListener(e -> this.validerModification());
        this.btnAnnuler.addActionListener(e -> this.dispose());

        buttonPanel.add(this.btnValider);
        buttonPanel.add(this.btnAnnuler);

        // ── ASSEMBLAGE ────────────────────────────────────────────────────────
        this.add(headerPanel,  BorderLayout.NORTH);
        this.add(centerPanel,  BorderLayout.CENTER);
        this.add(buttonPanel,  BorderLayout.SOUTH);

        this.getRootPane().setDefaultButton(this.btnValider);
        this.setupAutoCalculation();
        SwingUtilities.invokeLater(() -> this.txtDesignation.requestFocus());
    }

    // =========================================================================
    // HELPERS UI
    // =========================================================================

    /** Ajoute une ligne label + champ dans le GridBagLayout. */
    private void addFormRow(JPanel panel, GridBagConstraints gbc,
                            int row, String labelText, Component field, Font labelFont) {
        gbc.gridx = 0; gbc.gridy = row;
        gbc.insets = new Insets(8, 15, 8, 15);
        gbc.ipadx  = 0;
        JLabel lbl = new JLabel(labelText);
        lbl.setFont(labelFont);
        lbl.setForeground(COLOR_TEXT);
        panel.add(lbl, gbc);

        gbc.gridx = 1;
        gbc.ipadx = 200;
        panel.add(field, gbc);
    }

    /** Crée un JTextField stylisé (même style que AjoutArticle). */
    private JTextField createStyledTextField(String value) {
        JTextField tf = new JTextField(25);
        tf.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        tf.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(COLOR_BORDER),
            BorderFactory.createEmptyBorder(12, 15, 12, 15)
        ));
        tf.setText(value);
        tf.setForeground(Color.BLACK);
        tf.setPreferredSize(new Dimension(400, 45));
        return tf;
    }

    private JButton createModernButton(String text, final Color bg) {
        final JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(12, 30, 12, 30));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(darkenColor(bg, 0.9f)); }
            public void mouseExited (MouseEvent e) { btn.setBackground(bg); }
            public void mousePressed(MouseEvent e) { btn.setBackground(darkenColor(bg, 0.8f)); }
            public void mouseReleased(MouseEvent e){ btn.setBackground(darkenColor(bg, 0.9f)); }
        });
        return btn;
    }

    private Color darkenColor(Color c, float f) {
        return new Color(
            Math.max((int)(c.getRed()   * f), 0),
            Math.max((int)(c.getGreen() * f), 0),
            Math.max((int)(c.getBlue()  * f), 0)
        );
    }

    // =========================================================================
    // CALCUL AUTOMATIQUE HT depuis TTC
    // =========================================================================
    private void setupAutoCalculation() {
        DocumentListener dl = new DocumentListener() {
            public void changedUpdate(DocumentEvent e) { updateAffichages(); }
            public void removeUpdate (DocumentEvent e) { updateAffichages(); }
            public void insertUpdate (DocumentEvent e) { updateAffichages(); }
        };
        this.txtPrixGros.getDocument().addDocumentListener(dl);
        this.txtPrixDetail.getDocument().addDocumentListener(dl);
        this.txtTVA.getDocument().addDocumentListener(dl);
    }

    private void updateAffichages() {
        int tva = parseTVA();
        double diviseur = 1.0 + tva / 100.0;

        // Prix gros HT
        try {
            String s = txtPrixGros.getText().trim();
            double ttc = s.isEmpty() ? 0 : Double.parseDouble(s.replace(",", "."));
            lblPrixGrosTTCAffichage.setText(String.format("%.3f DT", ttc / diviseur));
        } catch (NumberFormatException ex) {
            lblPrixGrosTTCAffichage.setText("—");
        }

        // Prix détail HT
        try {
            String s = txtPrixDetail.getText().trim();
            double ttc = s.isEmpty() ? 0 : Double.parseDouble(s.replace(",", "."));
            lblPrixDetailTTCAffichage.setText(String.format("%.3f DT", ttc / diviseur));
        } catch (NumberFormatException ex) {
            lblPrixDetailTTCAffichage.setText("—");
        }
    }

    private int parseTVA() {
        try {
            int v = Integer.parseInt(txtTVA.getText().trim());
            return (v >= 0 && v <= 100) ? v : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // =========================================================================
    // BASE DE DONNÉES
    // =========================================================================
    private double getPrixDetailActuel() {
        String sql = "SELECT prix_detail FROM Articles WHERE id = ?";
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, this.articleId);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) return rs.getDouble("prix_detail");
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return 0.0;
    }

    // =========================================================================
    // VALIDATION & ENREGISTREMENT
    // =========================================================================
    private void validerModification() {
        String designation   = this.txtDesignation.getText().trim();
        String prixGrosText  = this.txtPrixGros.getText().trim();
        String prixDetailText = this.txtPrixDetail.getText().trim();

        // Désignation
        if (designation.isEmpty()) {
            showError("La désignation est obligatoire", this.txtDesignation); return;
        }
        if (designation.length() < 2) {
            showError("La désignation doit contenir au moins 2 caractères", this.txtDesignation); return;
        }

        // Prix gros
        if (prixGrosText.isEmpty()) {
            showError("Le prix gros est obligatoire", this.txtPrixGros); return;
        }

        // Prix détail
        if (prixDetailText.isEmpty()) {
            showError("Le prix détail est obligatoire", this.txtPrixDetail); return;
        }

        // TVA
        int nouvelleTVA;
        try {
            nouvelleTVA = Integer.parseInt(txtTVA.getText().trim());
            if (nouvelleTVA < 0 || nouvelleTVA > 100) {
                showError("La TVA doit être entre 0 et 100", this.txtTVA); return;
            }
        } catch (NumberFormatException e) {
            showError("La TVA doit être un nombre entier (ex: 19)", this.txtTVA); return;
        }

        // Parse prix
        double nouveauPrixGros;
        double nouveauPrixDetail;
        try {
            nouveauPrixGros = Double.parseDouble(prixGrosText.replace(",", "."));
            if (nouveauPrixGros <= 0) { showError("Le prix gros doit être supérieur à 0", this.txtPrixGros); return; }
        } catch (NumberFormatException e) {
            showError("Format de prix gros invalide (utilisez un point, ex: 12.500)", this.txtPrixGros); return;
        }
        try {
            nouveauPrixDetail = Double.parseDouble(prixDetailText.replace(",", "."));
            if (nouveauPrixDetail <= 0) { showError("Le prix détail doit être supérieur à 0", this.txtPrixDetail); return; }
        } catch (NumberFormatException e) {
            showError("Format de prix détail invalide (utilisez un point, ex: 12.500)", this.txtPrixDetail); return;
        }

        // Désignation dupliquée
        if (!verifierDesignationUnique(designation)) {
            int opt = JOptionPane.showOptionDialog(this,
                "<html><div style='text-align:center;'><b>⚠️ Désignation dupliquée</b><br><br>" +
                "Un article avec ce nom existe déjà.<br>Voulez-vous quand même continuer ?</div></html>",
                "Attention", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE,
                null, new Object[]{"Oui","Non"}, "Non");
            if (opt != 0) { this.txtDesignation.requestFocus(); return; }
        }

        // Confirmation
        double diviseur   = 1.0 + nouvelleTVA / 100.0;
        double grosHT     = nouveauPrixGros   / diviseur;
        double detailHT   = nouveauPrixDetail / diviseur;

        int confirm = JOptionPane.showOptionDialog(this,
            "<html><div style='text-align:center; width:380px;'>" +
            "<b>📝 Confirmer la modification</b><br><br>" +
            "</div></html>",
            "Confirmation", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
            null, new Object[]{"Oui","Non"}, "Oui");

        if (confirm == 0) {
            enregistrerModification(designation, nouveauPrixGros, nouveauPrixDetail, nouvelleTVA);
        }
    }

    private void showError(String message, JComponent field) {
        JOptionPane.showMessageDialog(this, message, "Validation", JOptionPane.WARNING_MESSAGE);
        field.requestFocus();
        if (field instanceof JTextField) ((JTextField) field).selectAll();
    }

    private boolean verifierDesignationUnique(String designation) {
        String sql = "SELECT COUNT(*) FROM Articles WHERE LOWER(designation) = LOWER(?) AND id != ?";
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, designation.trim());
            pst.setInt(2, this.articleId);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) return rs.getInt(1) == 0;
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return true;
    }

    private void enregistrerModification(String designation, double prixGros,
                                         double prixDetail, int tva) {
        String sql = "UPDATE Articles SET designation=?, prix_gros=?, prix_detail=?, tva=? WHERE id=?";
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setString(1, designation.trim());
            pst.setDouble(2, prixGros);
            pst.setDouble(3, prixDetail);
            pst.setInt(4, tva);
            pst.setInt(5, this.articleId);

            if (pst.executeUpdate() > 0) {
                JOptionPane.showMessageDialog(this,
                    "<html><div style='text-align:center;'>" +
                    "<b style='font-size:14px; color:#27AE60;'>✔ Article modifié avec succès !</b>" +
                    "</div></html>",
                    "Succès", JOptionPane.PLAIN_MESSAGE);
                if (this.parent != null) this.parent.rafraichirTableau();
                this.dispose();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Erreur base de données: " + e.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }
}