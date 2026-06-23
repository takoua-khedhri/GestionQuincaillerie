package com.myapp.ui;

import com.myapp.db.DatabaseManager;
import com.myapp.logic.FactureFournisseurManager;
import com.myapp.logic.FactureFournisseurManager.CalculTotauxAchat;
import com.myapp.print.FactureFournisseurImpression;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.table.*;
import javax.swing.text.JTextComponent;

import com.myapp.util.AppTheme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FactureFournisseurUI extends JFrame {

    private static final Logger log = LoggerFactory.getLogger(FactureFournisseurUI.class);

    // -- Palette (couleurs originales conservées) ----------------------
    private static final Color BG_PAGE        = new Color(0xF5F0E8);
    private static final Color BG_CARD        = new Color(0xFFFFFF);
    private static final Color HDR_DEEP       = new Color(0x2C1A0E);
    private static final Color HDR_MID        = new Color(0x4A2C1A);
    private static final Color HDR_ACCENT     = new Color(0xD4A017);
    private static final Color HDR_LIGHT      = new Color(0xF0C060);
    private static final Color SEC_FOURN_BDR  = new Color(0xB87333);
    private static final Color SEC_FOURN_TTL  = new Color(0x8B4513);
    private static final Color SEC_ART_BDR    = new Color(0xC09010);
    private static final Color SEC_ART_TTL    = new Color(0x7B5900);
    private static final Color BTN_ADD        = new Color(0x27AE60);
    private static final Color BTN_VALIDATE   = new Color(0x1A5276);
    private static final Color BTN_DELETE     = new Color(0xC0392B);
    private static final Color BTN_PRINT      = new Color(0x2471A3);
    private static final Color BTN_NEW        = new Color(0x6C3483);
    private static final Color BTN_BACK       = new Color(0x5D4037);
    private static final Color BTN_NEWFOUR    = new Color(0x117A65);
    private static final Color TBL_HDR_BG     = new Color(0x3E2010);
    private static final Color TBL_HDR_FG     = new Color(0xF0C060);
    private static final Color TBL_ROW_ODD    = new Color(0xFFFDF8);
    private static final Color TBL_ROW_EVN    = new Color(0xF5EFE3);
    private static final Color TBL_SEL_BG     = new Color(0xFFE4A0);
    private static final Color TBL_SEL_FG     = new Color(0x2C1A0E);
    private static final Color TBL_GRID       = new Color(0xDDD0BB);
    private static final Color LBL_DARK       = new Color(0x3E2010);
    private static final Color FIELD_BORDER   = new Color(0xC8B89A);
    private static final Color FIELD_BG       = new Color(0xFFFCF5);
    private static final Color FIELD_READ_BG  = new Color(0xEDE7D9);
    private static final Color FIELD_READ_FG  = new Color(0x5D4037);
    private static final Color SEARCH_BG      = new Color(0xFFF8EC);

    // -- Polices (uniquement modifiées pour utiliser Segoe UI) ---------
    private static final Font F_TITLE    = new Font("Segoe UI", Font.BOLD,   18);
    private static final Font F_SECTION  = new Font("Segoe UI", Font.BOLD,   13);
    private static final Font F_FIELD    = new Font("Segoe UI", Font.PLAIN,  13);
    private static final Font F_BTN      = new Font("Segoe UI", Font.BOLD,   12);
    private static final Font F_NUM      = new Font("Segoe UI", Font.BOLD,   15);
    private static final Font F_TOT_VAL  = new Font("Segoe UI", Font.BOLD,   15);
    private static final Font F_NET_LBL  = new Font("Segoe UI", Font.BOLD,   16);
    private static final Font F_NET_VAL  = new Font("Segoe UI", Font.BOLD,   22);
    private static final Font F_TBL_HDR  = new Font("Segoe UI", Font.BOLD,   12);
    private static final Font F_TBL_CELL = new Font("Segoe UI", Font.PLAIN,  12);

    // -- Constante pour le seuil de retenue à la source ----------------
    private static final double SEUIL_RETENUE_HT = 1000.0;

    // -- Composants -----------------------------------------------------
    private JComboBox<String> comboFournisseurs;
    private JComboBox<String> comboArticles;
    private JTextField txtMatriculeFiscale;
    private JTextField txtAdresse;
    private JTextField txtTel;
    private JTextField txtSearchFournisseur;
    private JTextField txtQuantite;
    private JTextField txtPrixAchat;
    private JTextField txtNumeroFactureFournisseur;
    private JTextField txtDateFacture;
    private JTextField txtRetenueSource;
    private JComboBox<String> comboStatut;  // MODIFICATION 1 : Nouveau champ comboStatut
    private JTable tableDetails;
    private DefaultTableModel model;
    private JLabel lblTotalHT;
    private JLabel lblTotalTVA;
    private JLabel lblTotalTTC;
    private JLabel lblMontantRetenu;
    private JLabel lblNetAPayer;
    private JLabel lblNumeroFacture;
    private FactureFournisseurManager factureManager;
    private ImageIcon logoIcon;
    private boolean miseAJourEnCours = false;
    private List<String[]> cacheFournisseursComplet = new ArrayList<>();
    private List<String>   cacheArticlesComplet     = new ArrayList<>();
    private boolean isAdjusting         = false;
    private int     fournisseurIdActuel = -1;
    private JButton btnValiderFacture;
    private boolean factureEnCours = false;
    private DocumentListener retenueListener;

    // ==================================================================
    //  CONSTRUCTEUR
    // ==================================================================
    public FactureFournisseurUI() {
        setupManagers();
        initializeUI();
        loadData();
        adjustComponentsForScreen();
    }

    // ==================================================================
    //  SETUP
    // ==================================================================
    private void setupManagers() {
        String[] cols = {"Ref.", "Désignation", "Qte", "P.U. HT", "Total HT", "TVA", "Total TTC"};
        this.model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return c == 2; }
        };
        this.model.addTableModelListener(e -> {
            if (e.getType() == TableModelEvent.UPDATE && !miseAJourEnCours && e.getColumn() == 2)
                SwingUtilities.invokeLater(() -> mettreAJourLigne(e.getFirstRow()));
        });
        this.factureManager = new FactureFournisseurManager(this.model);
    }

    // ==================================================================
    //  INIT UI (structure conservée)
    // ==================================================================
    private void initializeUI() {
        setTitle("Factures Fournisseurs - CHAA_ELECT");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        AppTheme.init();
        chargerLogo();

        JPanel content = new JPanel(new BorderLayout(0, 0));
        content.setBackground(BG_PAGE);
        content.add(buildHeader(), BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(12, 12));
        body.setBackground(BG_PAGE);
        body.setBorder(new EmptyBorder(14, 16, 14, 16));
        body.add(buildFormsRow(),     BorderLayout.NORTH);
        body.add(buildTableSection(), BorderLayout.CENTER);
        content.add(body, BorderLayout.CENTER);

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        scroll.getViewport().setBackground(BG_PAGE);
        getContentPane().add(scroll, BorderLayout.CENTER);

        setMinimumSize(new Dimension(1350, 780));
        setExtendedState(JFrame.MAXIMIZED_BOTH);
    }

    // ==================================================================
    //  HEADER (couleurs originales)
    // ==================================================================
    private JPanel buildHeader() {
        JPanel hdr = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, HDR_MID, getWidth(), getHeight(), HDR_DEEP));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(HDR_ACCENT);
                g2.setStroke(new BasicStroke(3f));
                g2.drawLine(0, getHeight()-3, getWidth(), getHeight()-3);
                g2.dispose();
            }
        };
        hdr.setBorder(new EmptyBorder(12, 18, 14, 18));
        hdr.setOpaque(false);

        // Gauche
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        left.setOpaque(false);
        JButton btnBack = buildButton("< Retour", BTN_BACK);
        btnBack.setPreferredSize(new Dimension(100, 34));
        btnBack.addActionListener(e -> { new AdminDashboard().setVisible(true); dispose(); });
        left.add(btnBack);
        JLabel lblLogo = (logoIcon != null) ? new JLabel(logoIcon) : mkLabel("[Logo]", Font.PLAIN, 14, HDR_ACCENT);
        lblLogo.setBorder(new EmptyBorder(0, 8, 0, 0));
        left.add(lblLogo);
        JPanel names = new JPanel(new GridLayout(3, 1, 0, 1));
        names.setOpaque(false);
        names.add(mkLabel("CHAA ELECT", Font.BOLD,   17, HDR_ACCENT));
        names.add(mkLabel("08, RUE 42500, Ezzahrouni, El Hrairia, Tunis, 2051",         Font.BOLD,   11, new Color(0xE0B860)));
        names.add(mkLabel(" 000/M/A/1981916C",  Font.ITALIC, 10, new Color(0xC09060)));
        left.add(names);

        // Centre
        JPanel center = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 8));
        center.setOpaque(false);
        JLabel titleLbl = mkLabel("FACTURE FOURNISSEUR", Font.BOLD, 20, new Color(0xFFE090));
        titleLbl.setFont(F_TITLE);
        center.add(titleLbl);

        // Droite
        JPanel right = new JPanel(new GridBagLayout());
        right.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(3, 6, 3, 6);
        gc.anchor = GridBagConstraints.WEST;
        gc.gridx = 0; gc.gridy = 0;
        right.add(mkLabel("N° Facture :", Font.BOLD, 11, HDR_ACCENT), gc);
        gc.gridx = 1;
        lblNumeroFacture = mkLabel("—", Font.BOLD, 15, HDR_LIGHT);
        lblNumeroFacture.setFont(F_NUM);
        right.add(lblNumeroFacture, gc);
        gc.gridx = 0; gc.gridy = 1;
        right.add(mkLabel("Date :", Font.BOLD, 11, HDR_ACCENT), gc);
        gc.gridx = 1;
        txtDateFacture = new JTextField(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
        styleFieldDark(txtDateFacture);
        txtDateFacture.setPreferredSize(new Dimension(130, 27));
        right.add(txtDateFacture, gc);
        gc.gridx = 0; gc.gridy = 2;
        right.add(mkLabel("N° Fact. Fourn. :", Font.BOLD, 11, HDR_ACCENT), gc);
        gc.gridx = 1;
        txtNumeroFactureFournisseur = new JTextField();
        styleFieldDark(txtNumeroFactureFournisseur);
        txtNumeroFactureFournisseur.setPreferredSize(new Dimension(160, 27));
        right.add(txtNumeroFactureFournisseur, gc);

        hdr.add(left,   BorderLayout.WEST);
        hdr.add(center, BorderLayout.CENTER);
        hdr.add(right,  BorderLayout.EAST);
        return hdr;
    }

    // ==================================================================
    //  LIGNE FORMULAIRES
    // ==================================================================
    private JPanel buildFormsRow() {
        JPanel row = new JPanel(new GridBagLayout());
        row.setBackground(BG_PAGE);
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.BOTH;
        gc.insets = new Insets(0, 0, 0, 10);
        gc.gridx = 0; gc.gridy = 0; gc.weightx = 0.42; gc.weighty = 1.0;
        row.add(buildFournisseurCard(), gc);
        gc.gridx = 1; gc.weightx = 0.58; gc.insets = new Insets(0, 0, 0, 0);
        row.add(buildArticleCard(), gc);
        return row;
    }

    // ==================================================================
    //  CARTE FOURNISSEUR (couleurs originales)
    // ==================================================================
    private JPanel buildFournisseurCard() {
        JPanel card = buildCard("INFORMATIONS FOURNISSEUR", SEC_FOURN_BDR, SEC_FOURN_TTL);
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(BG_CARD);
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 10, 6, 10);
        gc.anchor = GridBagConstraints.WEST;
        gc.fill   = GridBagConstraints.HORIZONTAL;

        int row = 0;
        addFormRow(form, gc, row++, "Recherche :", SEC_FOURN_TTL, () -> {
            txtSearchFournisseur = buildField(SEARCH_BG);
            txtSearchFournisseur.getDocument().addDocumentListener(new DocListener(this::rechercherFournisseur));
            return txtSearchFournisseur;
        });
        addFormRow(form, gc, row++, "Fournisseur * :", SEC_FOURN_TTL, () -> {
            comboFournisseurs = new JComboBox<>();
            comboFournisseurs.setEditable(true);
            comboFournisseurs.setFont(F_FIELD);
            styleCombo(comboFournisseurs);
            comboFournisseurs.addActionListener(e -> chargerInfosFournisseur());
            return comboFournisseurs;
        });
        addFormRow(form, gc, row++, "Matricule fiscale :", LBL_DARK, () -> {
            txtMatriculeFiscale = buildReadField();
            return txtMatriculeFiscale;
        });
        addFormRow(form, gc, row++, "Adresse :", LBL_DARK, () -> {
            txtAdresse = buildReadField();
            return txtAdresse;
        });
        addFormRow(form, gc, row++, "Telephone :", LBL_DARK, () -> {
            txtTel = buildReadField();
            return txtTel;
        });

        // Retenue a la source
        gc.gridx = 0; gc.gridy = row; gc.gridwidth = 1;
        form.add(mkLabel("Retenue source :", Font.BOLD, 12, LBL_DARK), gc);
        gc.gridx = 1; gc.gridwidth = 1;
        JPanel retRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        retRow.setBackground(BG_CARD);
        txtRetenueSource = buildField(FIELD_BG);
        txtRetenueSource.setText("1");
        txtRetenueSource.setPreferredSize(new Dimension(70, 34));
        retenueListener = new DocListener(this::calculerTotaux);
        txtRetenueSource.getDocument().addDocumentListener(retenueListener);
        retRow.add(txtRetenueSource);
        retRow.add(mkLabel("%", Font.BOLD, 14, SEC_FOURN_BDR));
        form.add(retRow, gc);
        row++;

        // MODIFICATION 2 : Ajout du comboStatut
        gc.gridx = 0; gc.gridy = row; gc.gridwidth = 1;
        gc.fill = GridBagConstraints.NONE;
        gc.anchor = GridBagConstraints.WEST;
        gc.insets = new Insets(6, 10, 6, 10);
        form.add(mkLabel("Statut :", Font.BOLD, 12, LBL_DARK), gc);
        gc.gridx = 1; gc.gridwidth = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        comboStatut = new JComboBox<>(new String[]{"Non payée", "Payée partiellement", "Payée"});
        comboStatut.setFont(F_FIELD);
        styleCombo(comboStatut);
        comboStatut.setSelectedIndex(0);
        form.add(comboStatut, gc);
        row++;

        // Bouton nouveau fournisseur
        gc.gridx = 0; gc.gridy = row; gc.gridwidth = 2;
        gc.fill = GridBagConstraints.NONE;
        gc.anchor = GridBagConstraints.CENTER;
        gc.insets = new Insets(14, 10, 6, 10);
        JButton btnNew = buildButton("+ Nouveau Fournisseur", BTN_NEWFOUR);
        btnNew.setPreferredSize(new Dimension(240, 38));
        btnNew.addActionListener(e -> { new AjouterFournisseur().setVisible(true); dispose(); });
        form.add(btnNew, gc);

        card.add(form, BorderLayout.CENTER);
        return card;
    }

    // ==================================================================
    //  CARTE ARTICLE (couleurs originales)
    // ==================================================================
    private JPanel buildArticleCard() {
        JPanel card = buildCard("SAISIE DES ARTICLES", SEC_ART_BDR, SEC_ART_TTL);
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(BG_CARD);
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 10, 6, 10);
        gc.anchor = GridBagConstraints.WEST;
        gc.fill   = GridBagConstraints.HORIZONTAL;

        gc.gridx = 0; gc.gridy = 0; gc.gridwidth = 1;
        form.add(mkLabel("Article :", Font.BOLD, 12, LBL_DARK), gc);
        gc.gridx = 1; gc.gridwidth = 3; gc.weightx = 1.0;
        comboArticles = new JComboBox<>();
        comboArticles.setEditable(true);
        comboArticles.setFont(F_FIELD);
        styleCombo(comboArticles);
        form.add(comboArticles, gc);

        gc.gridx = 0; gc.gridy = 1; gc.gridwidth = 1; gc.weightx = 0;
        form.add(mkLabel("Quantite :", Font.BOLD, 12, LBL_DARK), gc);
        gc.gridx = 1; gc.weightx = 0.3;
        txtQuantite = buildField(FIELD_BG);
        txtQuantite.setText("1");
        txtQuantite.setPreferredSize(new Dimension(80, 34));
        txtQuantite.addActionListener(e -> ajouterArticle());
        form.add(txtQuantite, gc);

        gc.gridx = 2; gc.weightx = 0;
        form.add(mkLabel("Prix achat HT :", Font.BOLD, 12, LBL_DARK), gc);
        gc.gridx = 3; gc.weightx = 0.5;
        txtPrixAchat = buildField(FIELD_BG);
        txtPrixAchat.setPreferredSize(new Dimension(110, 34));
        txtPrixAchat.addActionListener(e -> ajouterArticle());
        form.add(txtPrixAchat, gc);

        gc.gridx = 0; gc.gridy = 2; gc.gridwidth = 4;
        gc.fill = GridBagConstraints.NONE;
        gc.anchor = GridBagConstraints.CENTER;
        gc.insets = new Insets(12, 10, 6, 10);
        JButton btnAdd = buildButton("+ Ajouter a la facture", BTN_ADD);
        btnAdd.setPreferredSize(new Dimension(270, 42));
        btnAdd.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnAdd.addActionListener(e -> ajouterArticle());
        form.add(btnAdd, gc);

        card.add(form, BorderLayout.CENTER);

        JPanel info = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        info.setBackground(new Color(0xFFF8E1));
        info.setBorder(new MatteBorder(1, 0, 0, 0, new Color(0xF0C060)));
        info.add(mkLabel("Astuce : scannez le code-barres ou tapez les premieres lettres de l'article",
                Font.ITALIC, 11, new Color(0x8B6914)));
        card.add(info, BorderLayout.SOUTH);
        return card;
    }

    // ==================================================================
    //  SECTION TABLE + BOUTONS + TOTAUX
    // ==================================================================
    private JPanel buildTableSection() {
        JPanel section = new JPanel(new BorderLayout(0, 8));
        section.setBackground(BG_PAGE);
        section.add(buildActionButtons(), BorderLayout.NORTH);
        section.add(buildTable(),         BorderLayout.CENTER);
        section.add(buildTotalsPanel(),   BorderLayout.SOUTH);
        return section;
    }

    private JPanel buildActionButtons() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 8));
        p.setBackground(BG_PAGE);

        JButton btnSup = buildButton("Supprimer ligne", BTN_DELETE);
        btnSup.addActionListener(e -> supprimerLigne());

        JButton btnNewFact = buildButton("Nouvelle Facture", BTN_NEW);
        btnNewFact.addActionListener(e -> viderFacture());

        JButton btnPrint = buildButton("Imprimer", BTN_PRINT);
        btnPrint.addActionListener(e -> imprimerFacture());

        btnValiderFacture = buildButton("Valider & Entree en Stock", BTN_VALIDATE);
        btnValiderFacture.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnValiderFacture.setPreferredSize(new Dimension(260, 38));
        btnValiderFacture.addActionListener(e -> validerFacture());

        p.add(btnSup);
        p.add(btnNewFact);
        p.add(btnPrint);
        p.add(btnValiderFacture);
        return p;
    }

    private JScrollPane buildTable() {
        tableDetails = new JTable(model) {
            @Override public Component prepareRenderer(TableCellRenderer r, int row, int col) {
                Component c = super.prepareRenderer(r, row, col);
                if (!isRowSelected(row))
                    c.setBackground(row % 2 == 0 ? TBL_ROW_ODD : TBL_ROW_EVN);
                return c;
            }
        };
        tableDetails.setRowHeight(34);
        tableDetails.setFont(F_TBL_CELL);
        tableDetails.setSelectionBackground(TBL_SEL_BG);
        tableDetails.setSelectionForeground(TBL_SEL_FG);
        tableDetails.setGridColor(TBL_GRID);
        tableDetails.setShowHorizontalLines(true);
        tableDetails.setShowVerticalLines(false);
        tableDetails.setIntercellSpacing(new Dimension(0, 1));
        tableDetails.setFillsViewportHeight(true);

        int[] widths = {80, 300, 60, 110, 130, 65, 130};
        for (int i = 0; i < widths.length; i++)
            tableDetails.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(JLabel.CENTER);
        center.setFont(F_TBL_CELL);
        for (int i = 0; i < tableDetails.getColumnCount(); i++)
            if (i != 1) tableDetails.getColumnModel().getColumn(i).setCellRenderer(center);

        JTableHeader header = tableDetails.getTableHeader();
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(t, v, sel, foc, r, c);
                setBackground(TBL_HDR_BG);
                setForeground(TBL_HDR_FG);
                setFont(F_TBL_HDR);
                setHorizontalAlignment(JLabel.CENTER);
                setBorder(new CompoundBorder(
                    new MatteBorder(0, 0, 0, 1, new Color(0x6D4000)),
                    new EmptyBorder(8, 6, 8, 6)));
                return this;
            }
        });
        header.setPreferredSize(new Dimension(0, 42));
        header.setReorderingAllowed(false);

        JScrollPane sp = new JScrollPane(tableDetails);
        sp.setPreferredSize(new Dimension(0, 280));
        sp.getViewport().setBackground(TBL_ROW_ODD);
        sp.setBorder(new LineBorder(new Color(0xC8B89A), 1));
        return sp;
    }

    private JPanel buildTotalsPanel() {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(BG_CARD);
        outer.setBorder(new CompoundBorder(
            new MatteBorder(3, 0, 0, 0, HDR_ACCENT),
            new EmptyBorder(12, 18, 12, 18)));

        JPanel totals = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 4));
        totals.setBackground(BG_CARD);

        lblTotalHT       = mkLabel("0,000", Font.BOLD, 15, new Color(0x6D4C41));
        lblTotalHT.setFont(F_TOT_VAL);
        lblTotalTVA      = mkLabel("0,000", Font.BOLD, 15, new Color(0x795548));
        lblTotalTVA.setFont(F_TOT_VAL);
        lblTotalTTC      = mkLabel("0,000", Font.BOLD, 15, new Color(0x2E7D32));
        lblTotalTTC.setFont(F_TOT_VAL);
        lblMontantRetenu = mkLabel("0,000", Font.BOLD, 15, new Color(0xC62828));
        lblMontantRetenu.setFont(F_TOT_VAL);

        totals.add(buildTotalBox("TOTAL HT",  lblTotalHT,       new Color(0xFFF3E0), new Color(0xFFB300)));
        totals.add(buildTotalBox("TVA 19%",   lblTotalTVA,      new Color(0xF3E5F5), new Color(0x8E24AA)));
        totals.add(buildTotalBox("TOTAL TTC", lblTotalTTC,      new Color(0xE8F5E9), new Color(0x2E7D32)));
        totals.add(buildTotalBox("RETENUE",   lblMontantRetenu, new Color(0xFFEBEE), new Color(0xC62828)));

        // Net a payer
        JPanel netBox = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 6)) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, HDR_MID, getWidth(), getHeight(), HDR_DEEP));
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 10, 10));
                g2.dispose();
            }
        };
        netBox.setOpaque(false);
        netBox.setBorder(new EmptyBorder(6, 18, 6, 18));
        JLabel netLbl = mkLabel("NET A PAYER", Font.BOLD, 14, new Color(0xF0C060));
        netLbl.setFont(F_NET_LBL);
        lblNetAPayer = mkLabel("0,000", Font.BOLD, 22, new Color(0xFFE090));
        lblNetAPayer.setFont(F_NET_VAL);
        netBox.add(netLbl);
        netBox.add(mkLabel("DT", Font.BOLD, 12, new Color(0xD4A017)));
        netBox.add(lblNetAPayer);
        totals.add(netBox);

        outer.add(totals, BorderLayout.CENTER);
        return outer;
    }

    private JPanel buildTotalBox(String title, JLabel valueLabel, Color bg, Color accent) {
        JPanel box = new JPanel(new GridLayout(2, 1, 0, 2));
        box.setBackground(bg);
        box.setBorder(new CompoundBorder(
            new LineBorder(accent, 1, true),
            new EmptyBorder(6, 14, 6, 14)));
        JLabel lbl = mkLabel(title, Font.BOLD, 10, accent);
        lbl.setHorizontalAlignment(JLabel.CENTER);
        valueLabel.setHorizontalAlignment(JLabel.CENTER);
        box.add(lbl);
        box.add(valueLabel);
        return box;
    }

    // ==================================================================
    //  LOGIQUE METIER
    // ==================================================================

    private void loadData() {
        chargerFournisseurs();
        chargerArticles();
        genererNumeroFacture();
    }

    private void chargerFournisseurs() {
        cacheFournisseursComplet.clear();
        comboFournisseurs.removeAllItems();
        List<String[]> list = DatabaseManager.chargerFournisseursComplet();
        for (String[] f : list) {
            cacheFournisseursComplet.add(f);
            comboFournisseurs.addItem(f[0]);
        }
        comboFournisseurs.setSelectedIndex(-1);
    }

    private void rechercherFournisseur() {
        String q = txtSearchFournisseur.getText().toLowerCase();
        DefaultComboBoxModel<String> m = new DefaultComboBoxModel<>();
        for (String[] f : cacheFournisseursComplet) {
            if (f[0].toLowerCase().contains(q)
                || (f[1] != null && f[1].toLowerCase().contains(q))
                || (f[2] != null && f[2].toLowerCase().contains(q))) {
                m.addElement(f[0]);
            }
        }
        comboFournisseurs.setModel(m);
        if (m.getSize() > 0) comboFournisseurs.showPopup();
    }

    private void chargerInfosFournisseur() {
        if (isAdjusting) return;
        Object sel = comboFournisseurs.getSelectedItem();
        if (sel == null) return;
        String nom = sel.toString();
        for (String[] f : cacheFournisseursComplet) {
            if (f[0].equals(nom)) {
                fournisseurIdActuel = Integer.parseInt(f[4]);
                txtMatriculeFiscale.setText(f[2] != null ? f[2] : "");
                txtAdresse.setText(f[1] != null ? f[1] : "");
                txtTel.setText(f[3] != null ? f[3] : "");
                txtRetenueSource.getDocument().removeDocumentListener(retenueListener);
                txtRetenueSource.setText("1");
                txtRetenueSource.getDocument().addDocumentListener(retenueListener);
                calculerTotaux();
                break;
            }
        }
    }

    private void chargerArticles() {
        cacheArticlesComplet = DatabaseManager.chargerArticles();
        comboArticles.removeAllItems();
        for (String a : cacheArticlesComplet) comboArticles.addItem(a);
        setupArticleSearch();
    }

    private void setupArticleSearch() {
        JTextComponent editor = (JTextComponent) comboArticles.getEditor().getEditorComponent();
        editor.addKeyListener(new KeyAdapter() {
            @Override public void keyReleased(KeyEvent e) {
                int kc = e.getKeyCode();
                if (kc == KeyEvent.VK_UP || kc == KeyEvent.VK_DOWN
                 || kc == KeyEvent.VK_ENTER || kc == KeyEvent.VK_LEFT || kc == KeyEvent.VK_RIGHT)
                    return;
                SwingUtilities.invokeLater(() -> {
                    isAdjusting = true;
                    String text = editor.getText();
                    List<String> filtered = cacheArticlesComplet.stream()
                        .filter(s -> s.toLowerCase().contains(text.toLowerCase()))
                        .collect(Collectors.toList());
                    DefaultComboBoxModel<String> nm = new DefaultComboBoxModel<>(filtered.toArray(new String[0]));
                    comboArticles.setModel(nm);
                    editor.setText(text);
                    if (nm.getSize() > 0) comboArticles.showPopup();
                    else comboArticles.hidePopup();
                    isAdjusting = false;
                });
            }
        });
    }

    // MODIFICATION PRINCIPALE : Prix d'achat non obligatoire
    private void ajouterArticle() {
        try {
            String art = (comboArticles.getSelectedItem() != null)
                    ? comboArticles.getSelectedItem().toString().trim() : "";
            if (art.isEmpty()) { showMsg("Veuillez sélectionner un article."); return; }

            String qteStr = txtQuantite.getText().trim();
            int qte = 1;
            if (!qteStr.isEmpty()) {
                try {
                    qte = Integer.parseInt(qteStr);
                    if (qte <= 0) qte = 1;
                } catch (NumberFormatException ignored) {
                    qte = 1;
                }
            }

            // PRIX D'ACHAT NON OBLIGATOIRE - si vide, on met 0
            String prixStr = txtPrixAchat.getText().trim().replace(",", ".");
            double prix = 0;
            if (!prixStr.isEmpty()) {
                try {
                    prix = Double.parseDouble(prixStr);
                    if (prix < 0) {
                        showMsg("Le prix ne peut pas être négatif.");
                        return;
                    }
                } catch (NumberFormatException ex) {
                    showMsg("Format de prix invalide.");
                    return;
                }
            }
            
            // Vérification si le prix est 0 (optionnel mais recommandé)
            if (prix == 0) {
                Object[] optionsPrix = {"Oui", "Non"};
                int confirm = JOptionPane.showOptionDialog(this,
                    "Le prix d'achat est 0 DT. Voulez-vous continuer ?",
                    "Prix nul", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE,
                    null, optionsPrix, optionsPrix[1]);
                if (confirm != 0) {
                    return;
                }
            }

            factureManager.ajouterArticle(art, qte, prix);
            calculerTotaux();

            txtQuantite.setText("1");
            txtPrixAchat.setText("");
            comboArticles.setSelectedIndex(-1);
            ((JTextComponent) comboArticles.getEditor().getEditorComponent()).setText("");
            txtQuantite.requestFocus();

        } catch (Exception ex) {
            log.error("Erreur lors de l'ajout d'un article à la facture", ex);
            showMsg("Erreur : " + ex.getMessage());
        }
    }

    private void mettreAJourLigne(int row) {
        if (row < 0 || row >= model.getRowCount()) return;
        miseAJourEnCours = true;
        try {
            factureManager.recalculerLigne(row);
            calculerTotaux();
        } finally {
            miseAJourEnCours = false;
        }
    }

    // ==================================================================
    //  CALCUL TOTAUX AVEC SEUIL DE RETENUE (1000 DT)
    // ==================================================================
    private void calculerTotaux() {
        CalculTotauxAchat t = factureManager.calculerTotaux();

        double retPct = 0;
        try {
            String txt = txtRetenueSource.getText().trim().replace(",", ".");
            if (!txt.isEmpty()) retPct = Double.parseDouble(txt);
        } catch (NumberFormatException ignored) {}

        // ⚠️ RETENUE APPLIQUÉE UNIQUEMENT SI TOTAL HT >= 1000 DT
        double retenu = 0;
        if (t.totalHT >= SEUIL_RETENUE_HT) {
            retenu = t.totalHT * (retPct / 100.0);
        }
        double net = t.totalTTC - retenu;

        lblTotalHT.setText(      factureManager.formatMontant(t.totalHT));
        lblTotalTVA.setText(     factureManager.formatMontant(t.totalTVA));
        lblTotalTTC.setText(     factureManager.formatMontant(t.totalTTC));
        lblMontantRetenu.setText(factureManager.formatMontant(retenu));
        lblNetAPayer.setText(    factureManager.formatMontant(net));
        
        // Indication visuelle si la retenue n'est pas appliquée
        if (t.totalHT < SEUIL_RETENUE_HT && retPct > 0) {
            lblMontantRetenu.setForeground(Color.GRAY);
            lblMontantRetenu.setToolTipText("Retenue non appliquée : Total HT < " + (int)SEUIL_RETENUE_HT + " DT");
        } else {
            lblMontantRetenu.setForeground(new Color(0xC62828));
            lblMontantRetenu.setToolTipText(null);
        }
    }

    // ==================================================================
    //  METHODE VALIDER FACTURE AVEC SEUIL DE RETENUE
    // ==================================================================
    private void validerFacture() {
        if (factureEnCours) return;
        factureEnCours = true;
        btnValiderFacture.setEnabled(false);
        try {
            if (model.getRowCount() == 0) {
                showMsg("Impossible de valider : aucun article dans la facture.");
                return;
            }
            if (fournisseurIdActuel == -1) {
                showMsg("Sélectionnez un fournisseur.");
                return;
            }

            String numFact = lblNumeroFacture.getText();
            if (numeroExisteDejaEnBase(numFact)) {
                JOptionPane.showMessageDialog(this,
                    "Ce numéro de facture existe déjà.\nVeuillez actualiser.",
                    "Doublon", JOptionPane.ERROR_MESSAGE);
                return;
            }

            CalculTotauxAchat t = factureManager.calculerTotaux();
            double retPct = 0;
            try { retPct = Double.parseDouble(txtRetenueSource.getText().trim().replace(",", ".")); }
            catch (Exception ex) { log.warn("Format de retenue invalide, utilisation de 0%: {}", txtRetenueSource.getText()); }

            // ⚠️ RETENUE APPLIQUÉE UNIQUEMENT SI TOTAL HT >= 1000 DT
            double retenu = 0;
            if (t.totalHT >= SEUIL_RETENUE_HT) {
                retenu = t.totalHT * (retPct / 100.0);
            }
            double net = t.totalTTC - retenu;

            // MODIFICATION 3 : Récupération du statut et appel à insererFactureAchat avec statut
            String statut = comboStatut.getSelectedItem().toString();
            int factureId = DatabaseManager.insererFactureAchat(
                numFact, txtDateFacture.getText(),
                txtNumeroFactureFournisseur.getText(),
                fournisseurIdActuel,
                t.totalHT, t.totalTVA, t.totalTTC,
                retPct, retenu, net,
                statut);

            if (factureId == -1) { showMsg("Erreur lors de l'enregistrement."); return; }

            String utilisateur = System.getProperty("user.name");
            
            for (int i = 0; i < model.getRowCount(); i++) {
                try {
                    String desig   = model.getValueAt(i, 1).toString();
                    int    qte     = parseQte(model.getValueAt(i, 2).toString());
                    double pu      = parseMontant(model.getValueAt(i, 3).toString());
                    double totalHT = parseMontant(model.getValueAt(i, 4).toString());
                    int    tvaPct  = 19;
                    
                    int articleId = DatabaseManager.getArticleIdByDesignation(desig);
                    
                    if (DatabaseManager.insererDetailFactureAchat(factureId, desig, qte, pu, totalHT, tvaPct)) {
                        DatabaseManager.augmenterStock(desig, qte);
                        ajouterHistoriqueEntree(articleId, desig, qte, utilisateur);
                    }
                } catch (Exception ex) {
                    log.error("Erreur lors du traitement de la ligne {} de la facture", i, ex);
                }
            }

            // Message de confirmation avec indication du seuil
            String seuilMsg = (t.totalHT >= SEUIL_RETENUE_HT) ? 
                String.format("\nRetenue (%s%%): %s DT", 
                    factureManager.formatMontant(retPct), 
                    factureManager.formatMontant(retenu)) :
                "\nRetenue non appliquée (HT < " + (int)SEUIL_RETENUE_HT + " DT)";
            
            JOptionPane.showMessageDialog(this,
                String.format("✓ Facture enregistrée avec succès !\n\n" +
                              "Stock mis à jour\n" +
                              "HT: %s DT\n" +
                              "TTC: %s DT" +
                              "%s\n" +
                              "NET À PAYER: %s DT\n" +
                              "Statut: %s",
                              factureManager.formatMontant(t.totalHT),
                              factureManager.formatMontant(t.totalTTC),
                              seuilMsg,
                              factureManager.formatMontant(net),
                              statut),
                "Succès", JOptionPane.INFORMATION_MESSAGE);

            viderFacture();

        } catch (Exception ex) {
            log.error("Erreur lors de la validation de la facture", ex);
            showMsg("Erreur : " + ex.getMessage());
        } finally {
            factureEnCours = false;
            btnValiderFacture.setEnabled(true);
        }
    }

    // ==================================================================
    //  HISTORIQUE DES ENTREES
    // ==================================================================
    private void ajouterHistoriqueEntree(int articleId, String designation, int quantite, String utilisateur) {
        String sql = "INSERT INTO Historique_Entrees (article_id, designation, quantite, utilisateur) VALUES (?, ?, ?, ?)";
        try (Connection conn = com.myapp.db.ConnexionSQLite.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, articleId);
            pstmt.setString(2, designation);
            pstmt.setInt(3, quantite);
            pstmt.setString(4, utilisateur);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            log.error("Erreur lors de l'ajout dans Historique_Entrees", e);
        }
    }

    private boolean numeroExisteDejaEnBase(String num) {
        String sql = "SELECT COUNT(*) FROM FacturesAchat WHERE numero = ?";
        try (Connection c = com.myapp.db.ConnexionSQLite.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, num);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException ex) {
            log.error("Erreur lors de la vérification du numéro de facture en base", ex);
        }
        return false;
    }

    private void imprimerFacture() {
        if (model.getRowCount() == 0) { showMsg("Aucune facture à imprimer."); return; }
        try {
            String fourn = comboFournisseurs.getSelectedItem() != null
                    ? comboFournisseurs.getSelectedItem().toString() : "";
            CalculTotauxAchat t = factureManager.calculerTotaux();
            double retPct = 0;
            try { retPct = Double.parseDouble(txtRetenueSource.getText().trim().replace(",", ".")); }
            catch (Exception ignored) {}
            
            double retenu = 0;
            if (t.totalHT >= SEUIL_RETENUE_HT) {
                retenu = t.totalHT * (retPct / 100.0);
            }
            double net = t.totalTTC - retenu;

            new FactureFournisseurImpression(
                lblNumeroFacture.getText(), txtDateFacture.getText(),
                txtNumeroFactureFournisseur.getText(), fourn,
                txtMatriculeFiscale.getText(), txtAdresse.getText(), txtTel.getText(),
                model, retPct, retenu, net, logoIcon).imprimer();
        } catch (Exception ex) {
            log.error("Erreur lors de l'impression de la facture", ex);
            showMsg("Erreur impression : " + ex.getMessage());
        }
    }

    private void supprimerLigne() {
        int sel = tableDetails.getSelectedRow();
        if (sel != -1) {
            model.removeRow(sel);
            calculerTotaux();
        }
    }

    private void viderFacture() {
        model.setRowCount(0);
        txtNumeroFactureFournisseur.setText("");
        txtQuantite.setText("1");
        txtPrixAchat.setText("");
        comboFournisseurs.setSelectedIndex(-1);
        txtMatriculeFiscale.setText("");
        txtAdresse.setText("");
        txtTel.setText("");
        txtSearchFournisseur.setText("");
        txtRetenueSource.getDocument().removeDocumentListener(retenueListener);
        txtRetenueSource.setText("1");
        txtRetenueSource.getDocument().addDocumentListener(retenueListener);
        // MODIFICATION 4 : Reset du comboStatut
        comboStatut.setSelectedIndex(0);
        fournisseurIdActuel = -1;
        calculerTotaux();
        genererNumeroFacture();
        txtDateFacture.setText(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
    }

    private void genererNumeroFacture() {
        int annee = LocalDate.now().getYear();
        String prefix = "FA-" + annee + "-";
        int seq = 1;
        String sql = "SELECT numero FROM FacturesAchat WHERE numero LIKE ? ORDER BY id DESC LIMIT 1";
        try (Connection c = com.myapp.db.ConnexionSQLite.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, prefix + "%");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String[] parts = rs.getString("numero").split("-");
                    try { seq = Integer.parseInt(parts[parts.length - 1]) + 1; }
                    catch (NumberFormatException ignored) {}
                }
            }
        } catch (Exception ex) { log.error("Erreur lors de la génération du numéro de facture", ex); }
        lblNumeroFacture.setText(prefix + String.format("%04d", seq));
    }

    // ==================================================================
    //  HELPERS UI
    // ==================================================================

    private JPanel buildCard(String title, Color borderColor, Color titleColor) {
        JPanel card = new JPanel(new BorderLayout(0, 0));
        card.setBackground(BG_CARD);
        card.setBorder(new CompoundBorder(
            new LineBorder(borderColor, 2, true),
            new EmptyBorder(0, 0, 8, 0)));
        JPanel titleBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 7)) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setPaint(new GradientPaint(0, 0, borderColor.brighter(), getWidth(), 0, borderColor));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        titleBar.setOpaque(false);
        JLabel titleLabel = mkLabel(title, Font.BOLD, 13, Color.WHITE);
        titleLabel.setFont(F_SECTION);
        titleBar.add(titleLabel);
        card.add(titleBar, BorderLayout.NORTH);
        return card;
    }

    private void addFormRow(JPanel form, GridBagConstraints gc, int row,
                            String labelText, Color labelColor,
                            java.util.function.Supplier<JComponent> compSupplier) {
        gc.gridx = 0; gc.gridy = row; gc.gridwidth = 1; gc.weightx = 0;
        gc.fill = GridBagConstraints.NONE;
        form.add(mkLabel(labelText, Font.BOLD, 12, labelColor), gc);
        gc.gridx = 1; gc.gridwidth = 3; gc.weightx = 1.0;
        gc.fill = GridBagConstraints.HORIZONTAL;
        form.add(compSupplier.get(), gc);
    }

    private JTextField buildField(Color bg) {
        JTextField tf = new JTextField();
        tf.setFont(F_FIELD);
        tf.setBackground(bg);
        tf.setForeground(LBL_DARK);
        tf.setBorder(new CompoundBorder(
            new LineBorder(FIELD_BORDER, 1),
            new EmptyBorder(7, 10, 7, 10)));
        return tf;
    }

    private JTextField buildReadField() {
        JTextField tf = buildField(FIELD_READ_BG);
        tf.setEditable(false);
        tf.setForeground(FIELD_READ_FG);
        tf.setFont(F_FIELD);
        return tf;
    }

    private void styleFieldDark(JTextField tf) {
        tf.setFont(F_FIELD);
        tf.setBackground(new Color(0x3E2010));
        tf.setForeground(new Color(0xF0C060));
        tf.setCaretColor(new Color(0xF0C060));
        tf.setBorder(new EmptyBorder(5, 10, 5, 10));
    }

    @SuppressWarnings("unchecked")
    private void styleCombo(JComboBox<?> cb) {
        cb.setBackground(FIELD_BG);
        cb.setFont(F_FIELD);
        JTextField editor = (JTextField) cb.getEditor().getEditorComponent();
        editor.setBackground(FIELD_BG);
        editor.setFont(F_FIELD);
        editor.setBorder(new EmptyBorder(4, 6, 4, 6));
        cb.setBorder(new LineBorder(FIELD_BORDER, 1));
    }

    private JButton buildButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(F_BTN);
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new CompoundBorder(
            new LineBorder(darken(bg, 0.78f), 1),
            new EmptyBorder(8, 14, 8, 14)));
        Color hover = darken(bg, 0.88f);
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(hover); }
            public void mouseExited(MouseEvent e)  { btn.setBackground(bg);    }
        });
        return btn;
    }

    private JLabel mkLabel(String text, int style, int size, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", style, size));
        l.setForeground(color);
        return l;
    }

    private Color darken(Color c, float f) {
        return new Color(
            Math.max(0, (int)(c.getRed()   * f)),
            Math.max(0, (int)(c.getGreen() * f)),
            Math.max(0, (int)(c.getBlue()  * f)));
    }

    private void showMsg(String msg) {
        JOptionPane.showMessageDialog(this, msg);
    }

    private int parseQte(String s) {
        try { return Integer.parseInt(s.replaceAll("[^0-9]", "")); }
        catch (NumberFormatException e) { return 1; }
    }

    private double parseMontant(String s) {
        try { return Double.parseDouble(s.replaceAll("\\s", "").replace(",", ".").replaceAll("[^0-9.-]", "")); }
        catch (NumberFormatException e) { return 0.0; }
    }

    private void chargerLogo() {
        try {
            File f = new File("images/logo.png");
            if (f.exists())
                logoIcon = new ImageIcon(new ImageIcon(f.getAbsolutePath())
                    .getImage().getScaledInstance(60, 60, Image.SCALE_SMOOTH));
        } catch (Exception ignored) {}
    }

    private void adjustComponentsForScreen() {
        if (Toolkit.getDefaultToolkit().getScreenSize().getWidth() < 1366)
            tableDetails.getColumnModel().getColumn(1).setPreferredWidth(160);
    }

    // ==================================================================
    //  CLASSES INTERNES
    // ==================================================================
    private static class DocListener implements DocumentListener {
        private final Runnable action;
        DocListener(Runnable r) { this.action = r; }
        public void insertUpdate(DocumentEvent e)  { action.run(); }
        public void removeUpdate(DocumentEvent e)  { action.run(); }
        public void changedUpdate(DocumentEvent e) { action.run(); }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new FactureFournisseurUI().setVisible(true));
    }
}