package com.myapp.ui;

import com.myapp.db.ConnexionSQLite;
import com.myapp.util.AppTheme;
import com.myapp.print.FactureFournisseurImpression;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.text.*;
import java.time.LocalDate;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HistoriqueAchats extends JFrame {

    private static final Logger log = LoggerFactory.getLogger(HistoriqueAchats.class);

    // ── Palette ──────────────────────────────────────────────────────────────
    private static final Color C_HEADER_BG = new Color(44,  62,  80);
    private static final Color C_ACCENT    = new Color(52, 152, 219);
    private static final Color C_SUCCESS   = new Color(39, 174,  96);
    private static final Color C_WARNING   = new Color(243, 156,  18);
    private static final Color C_DANGER    = new Color(231,  76,  60);
    private static final Color C_PURPLE    = new Color(142,  68, 173);
    private static final Color C_PAGE_BG   = new Color(236, 240, 241);
    private static final Color C_PANEL_BG  = Color.WHITE;
    private static final Color C_TABLE_HDR = new Color(52,  73,  94);
    private static final Color C_ROW_ALT   = new Color(245, 248, 252);
    private static final Color C_GRID      = new Color(220, 220, 220);
    private static final Color C_BORDER    = new Color(189, 195, 199);

    // ── Widgets ───────────────────────────────────────────────────────────────
    private JTable            tableAchats;
    private DefaultTableModel model;
    private TableRowSorter<DefaultTableModel> sorter;

    private JTextField        txtDateDebut;
    private JTextField        txtDateFin;
    private JComboBox<String> comboFournisseur;
    private JTextField        txtArticle;
    private JComboBox<String> comboStatut;
    private JLabel            lblCompteur;

    private final List<String[]> fournisseursList = new ArrayList<>();
    private final DecimalFormat  df;

    // ─────────────────────────────────────────────────────────────────────────
    public HistoriqueAchats() {
        DecimalFormatSymbols sym = new DecimalFormatSymbols(Locale.FRANCE);
        sym.setGroupingSeparator(' ');
        df = new DecimalFormat("#,##0.000", sym);

        applyLookAndFeel();
        buildUI();
        loadFournisseurs();
        loadAchatsData();
    }

    private void applyLookAndFeel() {
        AppTheme.init();
    }

    // ── Main UI ───────────────────────────────────────────────────────────────
    private void buildUI() {
        setTitle("Historique des Achats");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1380, 750);
        setMinimumSize(new Dimension(1100, 600));
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(C_PAGE_BG);
        setContentPane(root);

        root.add(buildHeader(), BorderLayout.NORTH);
        root.add(buildBody(),   BorderLayout.CENTER);
    }

    // ── Header ────────────────────────────────────────────────────────────────
    private JPanel buildHeader() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(C_HEADER_BG);
        p.setBorder(new EmptyBorder(12, 20, 12, 20));

        JButton btnRetour = styledButton("← Retour", C_ACCENT);
        btnRetour.setPreferredSize(new Dimension(110, 34));
        btnRetour.addActionListener(e -> { new AdminDashboard().setVisible(true); dispose(); });

        JLabel title = new JLabel("  Historique des Achats");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(Color.WHITE);

        lblCompteur = new JLabel("0 facture(s)");
        lblCompteur.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblCompteur.setForeground(new Color(189, 195, 199));
        lblCompteur.setBorder(new EmptyBorder(0, 0, 0, 4));

        p.add(btnRetour,   BorderLayout.WEST);
        p.add(title,       BorderLayout.CENTER);
        p.add(lblCompteur, BorderLayout.EAST);
        return p;
    }

    // ── Body ──────────────────────────────────────────────────────────────────
    private JPanel buildBody() {
        JPanel p = new JPanel(new BorderLayout(0, 12));
        p.setBackground(C_PAGE_BG);
        p.setBorder(new EmptyBorder(14, 16, 14, 16));

        p.add(buildSearchPanel(),  BorderLayout.NORTH);
        p.add(buildTableSection(), BorderLayout.CENTER);
        return p;
    }

    // ── Search panel ──────────────────────────────────────────────────────────
    private JPanel buildSearchPanel() {
        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(C_PANEL_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(C_BORDER, 1, true),
            new EmptyBorder(14, 18, 14, 18)
        ));

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(5, 8, 5, 8);
        g.fill   = GridBagConstraints.HORIZONTAL;
        g.anchor = GridBagConstraints.WEST;

        JLabel sectionLabel = new JLabel("Recherche avancée");
        sectionLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        sectionLabel.setForeground(new Color(127, 140, 141));
        g.gridx = 0; g.gridy = 0; g.gridwidth = 8;
        card.add(sectionLabel, g);
        g.gridwidth = 1;

        g.gridy = 1;
        g.gridx = 0; g.weightx = 0;
        card.add(fieldLabel("Date début"), g);
        g.gridx = 1; g.weightx = 1;
        txtDateDebut = field(LocalDate.now().withDayOfMonth(1).toString());
        card.add(txtDateDebut, g);

        g.gridx = 2; g.weightx = 0;
        card.add(fieldLabel("Date fin"), g);
        g.gridx = 3; g.weightx = 1;
        txtDateFin = field(LocalDate.now().toString());
        card.add(txtDateFin, g);

        g.gridx = 4; g.weightx = 0;
        card.add(fieldLabel("Fournisseur"), g);
        g.gridx = 5; g.weightx = 1.5;
        comboFournisseur = new JComboBox<>();
        comboFournisseur.addItem("TOUS");
        styleCombo(comboFournisseur);
        card.add(comboFournisseur, g);

        g.gridy = 2;
        g.gridx = 0; g.weightx = 0;
        card.add(fieldLabel("Article"), g);
        g.gridx = 1; g.weightx = 1;
        txtArticle = field("");
        txtArticle.setToolTipText("Nom ou désignation de l'article");
        card.add(txtArticle, g);

        g.gridx = 2; g.weightx = 0;
        card.add(fieldLabel("Statut"), g);
        g.gridx = 3; g.weightx = 0.8;
        comboStatut = new JComboBox<>(new String[]{"TOUS", "Payée", "Non payée", "Payée partiellement"});
        styleCombo(comboStatut);
        card.add(comboStatut, g);

        g.gridx = 4; g.weightx = 0;
        JButton btnSearch = styledButton("Rechercher", C_SUCCESS);
        btnSearch.setPreferredSize(new Dimension(140, 32));
        btnSearch.addActionListener(e -> rechercherAchats());
        card.add(btnSearch, g);

        g.gridx = 5; g.weightx = 0;
        JButton btnReset = styledButton("Réinitialiser", C_PURPLE);
        btnReset.setPreferredSize(new Dimension(140, 32));
        btnReset.addActionListener(e -> resetRecherche());
        card.add(btnReset, g);

        return card;
    }

    // ── Table section ─────────────────────────────────────────────────────────
    private JPanel buildTableSection() {
        JPanel card = new JPanel(new BorderLayout(0, 0));
        card.setBackground(C_PANEL_BG);
        card.setBorder(new LineBorder(C_BORDER, 1, true));

        card.add(buildTable(),     BorderLayout.CENTER);
        card.add(buildActionBar(), BorderLayout.SOUTH);
        return card;
    }

    // ── Table ────────────────────────────────────────────────────────────────
    private JScrollPane buildTable() {
        String[] cols = {
            "ID", "N° Facture", "N° Fournisseur", "Date",
            "Fournisseur", "Total HT", "TVA", "Total TTC",
            "Retenue %", "Net à payer", "Statut"
        };

        model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int c) {
                return (c == 0) ? Integer.class : Object.class;
            }
        };

        tableAchats = new JTable(model) {
            @Override
            public Component prepareRenderer(TableCellRenderer r, int row, int col) {
                Component c = super.prepareRenderer(r, row, col);
                if (!isRowSelected(row)) {
                    c.setBackground(row % 2 == 0 ? C_PANEL_BG : C_ROW_ALT);
                }
                return c;
            }
        };

        tableAchats.setRowHeight(34);
        tableAchats.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tableAchats.setSelectionBackground(new Color(210, 234, 255));
        tableAchats.setSelectionForeground(C_TABLE_HDR);
        tableAchats.setGridColor(C_GRID);
        tableAchats.setShowHorizontalLines(true);
        tableAchats.setShowVerticalLines(false);
        tableAchats.setIntercellSpacing(new Dimension(0, 1));
        tableAchats.setFillsViewportHeight(true);

        // Double-clic pour voir détails
        tableAchats.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) voirDetails();
            }
        });

        sorter = new TableRowSorter<>(model);
        tableAchats.setRowSorter(sorter);

        JTableHeader header = tableAchats.getTableHeader();
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                setBackground(C_TABLE_HDR);
                setForeground(Color.WHITE);
                setFont(new Font("Segoe UI", Font.BOLD, 12));
                setHorizontalAlignment(JLabel.CENTER);
                setBorder(new EmptyBorder(8, 6, 8, 6));
                return this;
            }
        });
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 38));
        header.setReorderingAllowed(false);

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(JLabel.CENTER);
        DefaultTableCellRenderer right = new DefaultTableCellRenderer();
        right.setHorizontalAlignment(JLabel.RIGHT);
        right.setBorder(new EmptyBorder(0, 0, 0, 10));

        int[] rightCols  = {5, 6, 7, 8, 9};
        int[] centerCols = {0, 1, 2, 3, 4, 10};
        for (int c : rightCols)  tableAchats.getColumnModel().getColumn(c).setCellRenderer(right);
        for (int c : centerCols) tableAchats.getColumnModel().getColumn(c).setCellRenderer(center);

        tableAchats.getColumnModel().getColumn(10).setCellRenderer(new StatusRenderer());

        int[] widths = {45, 120, 120, 95, 190, 100, 80, 105, 80, 110, 110};
        for (int i = 0; i < widths.length; i++)
            tableAchats.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        tableAchats.getColumnModel().getColumn(0).setMaxWidth(55);

        JScrollPane sp = new JScrollPane(tableAchats);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getViewport().setBackground(C_PANEL_BG);
        return sp;
    }

    // ── Action bar ────────────────────────────────────────────────────────────
    private JPanel buildActionBar() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 10));
        p.setBackground(new Color(248, 249, 250));
        p.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, C_BORDER));

        JButton btnDetails   = styledButton("Voir détails",  C_ACCENT);
        JButton btnModifier  = styledButton("Modifier statut", C_WARNING);
        JButton btnSupprimer = styledButton("Supprimer",     C_DANGER);
        JButton btnImprimer  = styledButton("Imprimer",      C_SUCCESS);
        JButton btnRefresh   = styledButton("Actualiser",    C_PURPLE);

        btnDetails  .addActionListener(e -> voirDetails());
        btnModifier .addActionListener(e -> modifierStatut());
        btnSupprimer.addActionListener(e -> supprimerAchat());
        btnImprimer .addActionListener(e -> imprimerFacture());
        btnRefresh  .addActionListener(e -> loadAchatsData());

        Dimension btnSize = new Dimension(150, 34);
        for (JButton b : new JButton[]{btnDetails, btnModifier, btnSupprimer, btnImprimer, btnRefresh}) {
            b.setPreferredSize(btnSize);
            p.add(b);
        }
        return p;
    }

    // ── Data loading ──────────────────────────────────────────────────────────
    private void loadFournisseurs() {
        fournisseursList.clear();
        comboFournisseur.removeAllItems();
        comboFournisseur.addItem("TOUS");

        String sql = "SELECT id, nom, prenom, matricule_fiscale FROM Fournisseurs ORDER BY nom";
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {
            while (rs.next()) {
                String nom    = rs.getString("nom");
                String prenom = rs.getString("prenom");
                String full   = nom + (prenom != null && !prenom.isEmpty() ? " " + prenom : "");
                fournisseursList.add(new String[]{
                    String.valueOf(rs.getInt("id")), full, rs.getString("matricule_fiscale")
                });
                comboFournisseur.addItem(full);
            }
        } catch (SQLException e) { log.error("Erreur lors du chargement des fournisseurs", e); }
    }

    private void loadAchatsData() {
        model.setRowCount(0);
        String sql =
            "SELECT fa.id, fa.numero, fa.numero_fournisseur, fa.date_facture, " +
            "f.nom, f.prenom, fa.total_ht, fa.tva, fa.total_ttc, " +
            "fa.retenue_source_pourcentage, fa.net_a_payer, fa.statut " +
            "FROM FacturesAchat fa " +
            "JOIN Fournisseurs f ON fa.fournisseur_id = f.id " +
            "ORDER BY fa.date_facture DESC";
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {
            while (rs.next()) { model.addRow(buildRow(rs)); }
            updateCompteur();
        } catch (SQLException e) {
            log.error("Erreur lors du chargement des achats", e);
            JOptionPane.showMessageDialog(this, "Erreur chargement : " + e.getMessage(),
                "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void rechercherAchats() {
        String dateDebut   = txtDateDebut.getText().trim();
        String dateFin     = txtDateFin.getText().trim();
        String fournisseur = Objects.toString(comboFournisseur.getSelectedItem(), "TOUS");
        String article     = txtArticle.getText().trim();
        String statut      = Objects.toString(comboStatut.getSelectedItem(), "TOUS");

        // Convertir l'affichage du statut en valeur BDD
        String statutBDD = null;
        if ("Payée".equals(statut)) statutBDD = "PAYEE";
        else if ("Non payée".equals(statut)) statutBDD = "EN_ATTENTE";
        else if ("Payée partiellement".equals(statut)) statutBDD = "ANNULEE";

        StringBuilder sql = new StringBuilder(
            "SELECT fa.id, fa.numero, fa.numero_fournisseur, fa.date_facture, " +
            "f.nom, f.prenom, fa.total_ht, fa.tva, fa.total_ttc, " +
            "fa.retenue_source_pourcentage, fa.net_a_payer, fa.statut " +
            "FROM FacturesAchat fa " +
            "JOIN Fournisseurs f ON fa.fournisseur_id = f.id WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (!dateDebut.isEmpty())        { sql.append("AND fa.date_facture >= ? "); params.add(dateDebut); }
        if (!dateFin.isEmpty())          { sql.append("AND fa.date_facture <= ? "); params.add(dateFin); }
        if (!"TOUS".equals(fournisseur)) { sql.append("AND (f.nom || ' ' || COALESCE(f.prenom,'')) = ? "); params.add(fournisseur); }
        if (!article.isEmpty())          { sql.append("AND EXISTS (SELECT 1 FROM DetailsAchat da WHERE da.facture_achat_id = fa.id AND da.article_designation LIKE ?) "); params.add("%" + article + "%"); }
        if (statutBDD != null)           { sql.append("AND fa.statut = ? "); params.add(statutBDD); }
        sql.append("ORDER BY fa.date_facture DESC");

        model.setRowCount(0);
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) pst.setObject(i + 1, params.get(i));
            ResultSet rs = pst.executeQuery();
            while (rs.next()) { model.addRow(buildRow(rs)); }
            updateCompteur();
            JOptionPane.showMessageDialog(this,
                model.getRowCount() + " facture(s) trouvée(s)", "Résultat", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException e) {
            log.error("Erreur lors de la recherche des achats", e);
            JOptionPane.showMessageDialog(this, "Erreur recherche : " + e.getMessage(),
                "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private Object[] buildRow(ResultSet rs) throws SQLException {
        String prenom = rs.getString("prenom");
        String nom    = rs.getString("nom") + (prenom != null && !prenom.isEmpty() ? " " + prenom : "");
        
        String statutBDD = rs.getString("statut");
        String statutAff = formatStatutLabel(statutBDD);
        
        return new Object[]{
            rs.getInt("id"),
            rs.getString("numero"),
            rs.getString("numero_fournisseur"),
            rs.getString("date_facture"),
            nom,
            formatMontant(rs.getDouble("total_ht")),
            formatMontant(rs.getDouble("tva")),
            formatMontant(rs.getDouble("total_ttc")),
            rs.getDouble("retenue_source_pourcentage") + " %",
            formatMontant(rs.getDouble("net_a_payer")),
            statutAff
        };
    }

    private void resetRecherche() {
        txtDateDebut.setText(LocalDate.now().withDayOfMonth(1).toString());
        txtDateFin.setText(LocalDate.now().toString());
        comboFournisseur.setSelectedIndex(0);
        txtArticle.setText("");
        comboStatut.setSelectedIndex(0);
        loadAchatsData();
    }

    // =========================================================================
    // VOIR DÉTAILS — affiche la facture complète dans une boîte de dialogue
    // =========================================================================
    private void voirDetails() {
        int row = requireSelection();
        if (row == -1) return;

        int    factureId = (int)    model.getValueAt(row, 0);
        String numero    = (String) model.getValueAt(row, 1);
        String date      = (String) model.getValueAt(row, 3);
        String fourn     = (String) model.getValueAt(row, 4);
        String totalHT   = (String) model.getValueAt(row, 5);
        String tva       = (String) model.getValueAt(row, 6);
        String totalTTC  = (String) model.getValueAt(row, 7);
        String retPct    = (String) model.getValueAt(row, 8);
        String net       = (String) model.getValueAt(row, 9);
        String statut    = (String) model.getValueAt(row, 10);

        // Récupérer infos fournisseur + articles depuis la BDD
        String[]   fournisseurInfo = getFournisseurInfo(factureId);
        Object[][] articles        = getArticlesFacture(factureId);
        String     numFourn        = getNumeroFournisseur(factureId);

        // ── Dialogue ────────────────────────────────────────────────────────
        JDialog dlg = new JDialog(this, "Détails — Facture " + numero, true);
        dlg.setSize(900, 680);
        dlg.setLocationRelativeTo(this);
        dlg.setLayout(new BorderLayout(0, 0));

        // ── Panel principal avec scroll ──────────────────────────────────────
        JPanel main = new JPanel();
        main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));
        main.setBackground(new Color(245, 248, 252));
        main.setBorder(new EmptyBorder(20, 24, 20, 24));

        // ── Titre ────────────────────────────────────────────────────────────
        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setBackground(C_HEADER_BG);
        titleBar.setBorder(new EmptyBorder(12, 18, 12, 18));
        JLabel lblTitre = new JLabel("FACTURE D'ACHAT  —  " + numero);
        lblTitre.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitre.setForeground(Color.WHITE);

        // Badge statut
        JLabel lblStatutBadge = makeStatutBadge(statut);
        titleBar.add(lblTitre,      BorderLayout.WEST);
        titleBar.add(lblStatutBadge, BorderLayout.EAST);

        // ── Section fournisseur ──────────────────────────────────────────────
        JPanel secFourn = buildDetailsSection("Informations Fournisseur", new Color(52, 73, 94));
        JPanel gridFourn = new JPanel(new GridLayout(0, 2, 10, 6));
        gridFourn.setBackground(Color.WHITE);
        gridFourn.setBorder(new EmptyBorder(10, 14, 10, 14));
        addDetailLine(gridFourn, "Fournisseur :",     fourn);
        addDetailLine(gridFourn, "N° Fact. Fourn. :", numFourn != null ? numFourn : "—");
        addDetailLine(gridFourn, "Date :",            date);
        if (fournisseurInfo != null) {
            addDetailLine(gridFourn, "Matricule fiscale :", fournisseurInfo[0] != null ? fournisseurInfo[0] : "—");
            addDetailLine(gridFourn, "Adresse :",          fournisseurInfo[1] != null ? fournisseurInfo[1] : "—");
            addDetailLine(gridFourn, "Téléphone :",        fournisseurInfo[2] != null ? fournisseurInfo[2] : "—");
        }
        secFourn.add(gridFourn, BorderLayout.CENTER);

        // ── Section articles ─────────────────────────────────────────────────
        JPanel secArt = buildDetailsSection("Articles de la Facture", new Color(39, 174, 96));
        String[] artCols = {"Désignation", "Qté", "Prix U. HT", "Total HT", "TVA %", "Total TTC"};
        DefaultTableModel artModel = new DefaultTableModel(artCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        for (Object[] art : articles) artModel.addRow(art);

        JTable artTable = new JTable(artModel);
        artTable.setRowHeight(28);
        artTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        artTable.setGridColor(C_GRID);
        artTable.setShowHorizontalLines(true);
        artTable.setShowVerticalLines(false);
        artTable.setFillsViewportHeight(true);

        JTableHeader artHeader = artTable.getTableHeader();
        artHeader.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean s, boolean f, int r, int c) {
                super.getTableCellRendererComponent(t, v, s, f, r, c);
                setBackground(C_TABLE_HDR); setForeground(Color.WHITE);
                setFont(new Font("Segoe UI", Font.BOLD, 11));
                setHorizontalAlignment(JLabel.CENTER);
                setBorder(new EmptyBorder(6, 4, 6, 4));
                return this;
            }
        });
        artHeader.setPreferredSize(new Dimension(0, 32));

        DefaultTableCellRenderer cr = new DefaultTableCellRenderer();
        cr.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 0; i < artCols.length; i++)
            artTable.getColumnModel().getColumn(i).setCellRenderer(cr);

        int[] artWidths = {260, 55, 110, 110, 60, 110};
        for (int i = 0; i < artWidths.length; i++)
            artTable.getColumnModel().getColumn(i).setPreferredWidth(artWidths[i]);

        JScrollPane artScroll = new JScrollPane(artTable);
        artScroll.setPreferredSize(new Dimension(0, 200));
        artScroll.setBorder(BorderFactory.createEmptyBorder());
        secArt.add(artScroll, BorderLayout.CENTER);

        // ── Section totaux ───────────────────────────────────────────────────
        JPanel secTotaux = buildDetailsSection("Récapitulatif Financier", new Color(142, 68, 173));
        JPanel gridTot = new JPanel(new GridLayout(0, 2, 10, 8));
        gridTot.setBackground(Color.WHITE);
        gridTot.setBorder(new EmptyBorder(12, 14, 12, 14));

        addTotalLine(gridTot, "Total HT :",    totalHT, new Color(80, 80, 80));
        addTotalLine(gridTot, "TVA (19%) :",   tva,     new Color(80, 80, 80));
        addTotalLine(gridTot, "Total TTC :",   totalTTC, new Color(39, 174, 96));
        addTotalLine(gridTot, "Retenue :",     retPct,  new Color(231, 76, 60));
        addTotalLine(gridTot, "Net à payer :", net,     new Color(41, 128, 185));
        addTotalLine(gridTot, "Statut :",      statut, getStatutColor(statut));
        secTotaux.add(gridTot, BorderLayout.CENTER);

        // ── Assemblage ───────────────────────────────────────────────────────
        main.add(secFourn);
        main.add(Box.createVerticalStrut(12));
        main.add(secArt);
        main.add(Box.createVerticalStrut(12));
        main.add(secTotaux);

        JScrollPane scroll = new JScrollPane(main);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        // ── Boutons bas ──────────────────────────────────────────────────────
        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 10));
        btnBar.setBackground(new Color(248, 249, 250));
        btnBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, C_BORDER));

        JButton btnPrint = styledButton("Imprimer cette facture", C_SUCCESS);
        btnPrint.setPreferredSize(new Dimension(220, 36));
        btnPrint.addActionListener(e -> {
            dlg.dispose();
            imprimerFacture();
        });

        JButton btnClose = styledButton("Fermer", C_DANGER);
        btnClose.setPreferredSize(new Dimension(110, 36));
        btnClose.addActionListener(e -> dlg.dispose());

        btnBar.add(btnPrint);
        btnBar.add(btnClose);

        dlg.add(titleBar, BorderLayout.NORTH);
        dlg.add(scroll,   BorderLayout.CENTER);
        dlg.add(btnBar,   BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    // =========================================================================
    // MODIFIER STATUT
    // =========================================================================
    private void modifierStatut() {
        int row = requireSelection();
        if (row == -1) return;

        int    factureId     = (int)    model.getValueAt(row, 0);
        String numero        = (String) model.getValueAt(row, 1);
        String statutActuel  = (String) model.getValueAt(row, 10);

        // ── Dialogue ────────────────────────────────────────────────────────
        JDialog dlg = new JDialog(this, "Modifier le statut — " + numero, true);
        dlg.setSize(420, 260);
        dlg.setLocationRelativeTo(this);
        dlg.setLayout(new BorderLayout());

        // Header
        JPanel hdr = new JPanel(new BorderLayout());
        hdr.setBackground(C_WARNING);
        hdr.setBorder(new EmptyBorder(12, 18, 12, 18));
        JLabel lbl = new JLabel("Modifier le statut de la facture");
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lbl.setForeground(Color.WHITE);
        hdr.add(lbl, BorderLayout.CENTER);

        // Body
        JPanel body = new JPanel(new GridBagLayout());
        body.setBackground(Color.WHITE);
        body.setBorder(new EmptyBorder(20, 24, 20, 24));
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(8, 6, 8, 6);
        gc.anchor = GridBagConstraints.WEST;
        gc.fill   = GridBagConstraints.HORIZONTAL;

        gc.gridx = 0; gc.gridy = 0;
        body.add(new JLabel("Facture :"), gc);
        gc.gridx = 1;
        JLabel lblNum = new JLabel(numero);
        lblNum.setFont(new Font("Segoe UI", Font.BOLD, 13));
        body.add(lblNum, gc);

        gc.gridx = 0; gc.gridy = 1;
        body.add(new JLabel("Statut actuel :"), gc);
        gc.gridx = 1;
        JLabel lblActuel = makeStatutBadge(statutActuel);
        body.add(lblActuel, gc);

        gc.gridx = 0; gc.gridy = 2;
        body.add(new JLabel("Nouveau statut :"), gc);
        gc.gridx = 1;
        String[] statutLabels = {"Non payée", "Payée", "Payée partiellement"};
        String[] statutValues = {"EN_ATTENTE", "PAYEE", "ANNULEE"};
        JComboBox<String> comboNouveauStatut = new JComboBox<>(statutLabels);
        comboNouveauStatut.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        
        // Pré-sélectionner le statut actuel
        String statutActuelBDD = convertirStatutAffichageVersBDD(statutActuel);
        for (int i = 0; i < statutValues.length; i++) {
            if (statutValues[i].equals(statutActuelBDD)) {
                comboNouveauStatut.setSelectedIndex(i);
                break;
            }
        }
        body.add(comboNouveauStatut, gc);

        // Boutons
        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 10));
        btnBar.setBackground(new Color(248, 249, 250));
        btnBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, C_BORDER));

        JButton btnSave = styledButton("Enregistrer", C_SUCCESS);
        btnSave.setPreferredSize(new Dimension(140, 34));
        JButton btnCancel = styledButton("Annuler", C_DANGER);
        btnCancel.setPreferredSize(new Dimension(110, 34));

        btnCancel.addActionListener(e -> dlg.dispose());
        btnSave.addActionListener(e -> {
            int idx = comboNouveauStatut.getSelectedIndex();
            String nouveauStatut = statutValues[idx];

            if (nouveauStatut.equals(statutActuelBDD)) {
                JOptionPane.showMessageDialog(dlg,
                    "Le statut sélectionné est identique au statut actuel.",
                    "Aucun changement", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            Object[] optionsConfirm = {"Oui", "Non"};
            int ok = JOptionPane.showOptionDialog(dlg,
                "Confirmer le changement de statut ?\n\n" +
                "  " + statutActuel + "  →  " + statutLabels[idx],
                "Confirmation", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE,
                null, optionsConfirm, optionsConfirm[1]);

            if (ok == 0) {
                if (mettreAJourStatut(factureId, nouveauStatut)) {
                    JOptionPane.showMessageDialog(dlg,
                        "Statut mis à jour avec succès !", "Succès", JOptionPane.INFORMATION_MESSAGE);
                    dlg.dispose();
                    loadAchatsData();
                } else {
                    JOptionPane.showMessageDialog(dlg,
                        "Erreur lors de la mise à jour.", "Erreur", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        btnBar.add(btnSave);
        btnBar.add(btnCancel);

        dlg.add(hdr,    BorderLayout.NORTH);
        dlg.add(body,   BorderLayout.CENTER);
        dlg.add(btnBar, BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    private boolean mettreAJourStatut(int factureId, String nouveauStatut) {
        String sql = "UPDATE FacturesAchat SET statut = ? WHERE id = ?";
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, nouveauStatut);
            pst.setInt(2, factureId);
            return pst.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Erreur lors de la mise a jour du statut de la facture {}", factureId, e);
            return false;
        }
    }

    // =========================================================================
    // SUPPRIMER (message simplifié)
    // =========================================================================
    private void supprimerAchat() {
        int row = requireSelection();
        if (row == -1) return;

        int    factureId = (int)    model.getValueAt(row, 0);
        String numero    = (String) model.getValueAt(row, 1);

        Object[] optionsSuppr = {"Oui", "Non"};
        int ok = JOptionPane.showOptionDialog(this,
            "Supprimer la facture N° " + numero + " ?",
            "Confirmation suppression", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE,
            null, optionsSuppr, optionsSuppr[1]);

        if (ok != 0) return;

        restaurerStockAvantSuppression(factureId);

        String sql = "DELETE FROM FacturesAchat WHERE id = ?";
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, factureId);
            pst.executeUpdate();
            JOptionPane.showMessageDialog(this, "Facture supprimée avec succès.", "Succès", JOptionPane.INFORMATION_MESSAGE);
            loadAchatsData();
        } catch (SQLException e) {
            log.error("Erreur lors de la suppression de la facture", e);
            JOptionPane.showMessageDialog(this, "Erreur suppression : " + e.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void restaurerStockAvantSuppression(int factureId) {
        String sql = "SELECT article_designation, quantite FROM DetailsAchat WHERE facture_achat_id = ?";
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, factureId);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                String designation = rs.getString("article_designation");
                int    quantite    = rs.getInt("quantite");
                String upd = "UPDATE Articles SET stock = stock - ? WHERE designation = ?";
                try (PreparedStatement p2 = conn.prepareStatement(upd)) {
                    p2.setInt(1, quantite);
                    p2.setString(2, designation);
                    p2.executeUpdate();
                }
            }
        } catch (SQLException e) { log.error("Erreur lors de la restauration du stock avant suppression", e); }
    }

    // =========================================================================
    // IMPRIMER
    // =========================================================================
    private void imprimerFacture() {
        int row = requireSelection();
        if (row == -1) return;

        int    factureId = (int)    model.getValueAt(row, 0);
        String numero    = (String) model.getValueAt(row, 1);
        String date      = (String) model.getValueAt(row, 3);
        String fourn     = (String) model.getValueAt(row, 4);

        // Récupération info fournisseur
        String[] fournInfo  = getFournisseurInfo(factureId);
        String   numFourn   = getNumeroFournisseur(factureId);

        // Reconstruction du DefaultTableModel pour l'impression
        String[] cols = {"Ref.", "Désignation", "Qte", "P.U. HT", "Total HT", "TVA", "Total TTC"};
        DefaultTableModel printModel = new DefaultTableModel(cols, 0);
        Object[][] articles = getArticlesFacture(factureId);
        for (Object[] art : articles) {
            printModel.addRow(new Object[]{
                "—",          // Ref (non disponible dans DetailsAchat)
                art[0],       // Désignation
                art[1],       // Qté
                art[2],       // Prix U. HT
                art[3],       // Total HT
                art[4],       // TVA %
                art[5]        // Total TTC
            });
        }

        // Récupérer retenue et net depuis la table principale
        double retPct  = 0.0;
        double retenu  = 0.0;
        double net     = 0.0;
        String retPctStr = model.getValueAt(row, 8).toString().replace(" %", "").trim();
        try { retPct = Double.parseDouble(retPctStr); } catch (Exception ignored) {}
        try { retenu = parseMontantBrut((String) model.getValueAt(row, 8)); } catch (Exception ignored) {}
        String netStr = (String) model.getValueAt(row, 9);
        try { net = parseMontantBrut(netStr); } catch (Exception ignored) {}

        // Recalcul correct : retenue = totalHT * (retPct/100) si HT >= 1000
        double totalHT = 0.0;
        try { totalHT = parseMontantBrut((String) model.getValueAt(row, 5)); } catch (Exception ignored) {}
        if (totalHT >= 1000.0) retenu = totalHT * (retPct / 100.0);
        else retenu = 0.0;
        double totalTTC = 0.0;
        try { totalTTC = parseMontantBrut((String) model.getValueAt(row, 7)); } catch (Exception ignored) {}
        net = totalTTC - retenu;

        String matricule = fournInfo != null && fournInfo[0] != null ? fournInfo[0] : "";
        String adresse   = fournInfo != null && fournInfo[1] != null ? fournInfo[1] : "";
        String tel       = fournInfo != null && fournInfo[2] != null ? fournInfo[2] : "";

        // Chargement logo (même logique que FactureFournisseurUI)
        javax.swing.ImageIcon logoIcon = null;
        try {
            java.io.File f = new java.io.File("images/logo.png");
            if (f.exists())
                logoIcon = new javax.swing.ImageIcon(
                    new javax.swing.ImageIcon(f.getAbsolutePath())
                        .getImage().getScaledInstance(60, 60, java.awt.Image.SCALE_SMOOTH));
        } catch (Exception ignored) {}

        try {
            new FactureFournisseurImpression(
                numero, date,
                numFourn != null ? numFourn : "",
                fourn,
                matricule, adresse, tel,
                printModel,
                retPct, retenu, net,
                logoIcon
            ).imprimer();
        } catch (Exception e) {
            if (!e.getMessage().contains("annul")) {
                JOptionPane.showMessageDialog(this,
                    "Erreur d'impression : " + e.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // =========================================================================
    // REQUÊTES BDD HELPERS
    // =========================================================================

    /** Retourne [matricule, adresse, tel] du fournisseur lié à la facture. */
    private String[] getFournisseurInfo(int factureId) {
        String sql =
            "SELECT f.matricule_fiscale, f.adresse, f.telephone " +
            "FROM FacturesAchat fa " +
            "JOIN Fournisseurs f ON fa.fournisseur_id = f.id " +
            "WHERE fa.id = ?";
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, factureId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                return new String[]{
                    rs.getString("matricule_fiscale"),
                    rs.getString("adresse"),
                    rs.getString("telephone")
                };
            }
        } catch (SQLException e) { log.error("Erreur lors de la recuperation des infos fournisseur pour la facture {}", factureId, e); }
        return null;
    }

    /** Retourne le numéro de facture fournisseur (externe). */
    private String getNumeroFournisseur(int factureId) {
        String sql = "SELECT numero_fournisseur FROM FacturesAchat WHERE id = ?";
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, factureId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) return rs.getString("numero_fournisseur");
        } catch (SQLException e) { log.error("Erreur lors de la recuperation du numero fournisseur pour la facture {}", factureId, e); }
        return null;
    }

    /**
     * Retourne les articles de la facture :
     * [Désignation, Qté, Prix U. HT, Total HT, TVA %, Total TTC]
     */
    private Object[][] getArticlesFacture(int factureId) {
        String sql =
            "SELECT article_designation, quantite, prix_unitaire_ht, total_ht, tva_pourcentage " +
            "FROM DetailsAchat WHERE facture_achat_id = ? ORDER BY id";
        List<Object[]> rows = new ArrayList<>();
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, factureId);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                double ht     = rs.getDouble("total_ht");
                int    tvaPct = rs.getInt("tva_pourcentage");
                double ttc    = ht * (1 + tvaPct / 100.0);
                rows.add(new Object[]{
                    rs.getString("article_designation"),
                    rs.getInt("quantite"),
                    formatMontant(rs.getDouble("prix_unitaire_ht")),
                    formatMontant(ht),
                    tvaPct + " %",
                    formatMontant(ttc)
                });
            }
        } catch (SQLException e) { log.error("Erreur lors de la recuperation des articles de la facture {}", factureId, e); }
        return rows.toArray(new Object[0][]);
    }

    // =========================================================================
    // HELPERS UI
    // =========================================================================

    private int requireSelection() {
        int row = tableAchats.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Veuillez sélectionner une facture.",
                "Sélection requise", JOptionPane.WARNING_MESSAGE);
        } else {
            // Convertir index view → model si tri actif
            row = tableAchats.convertRowIndexToModel(row);
        }
        return row;
    }

    private void updateCompteur() {
        int n = model.getRowCount();
        lblCompteur.setText(n + " facture" + (n > 1 ? "s" : ""));
    }

    private String formatMontant(double v) { return df.format(v); }

    private double parseMontantBrut(String s) {
        if (s == null) return 0.0;
        try {
            return Double.parseDouble(
                s.replaceAll("\\s", "").replace(",", ".").replaceAll("[^0-9.-]", ""));
        } catch (NumberFormatException e) { return 0.0; }
    }

    private String formatStatutLabel(String s) {
        if (s == null) return s;
        switch (s) {
            case "PAYEE":      return "Payée";
            case "EN_ATTENTE": return "Non payée";
            case "ANNULEE":    return "Payée partiellement";
            default:           return s;
        }
    }
    
    private String convertirStatutAffichageVersBDD(String statutAff) {
        if ("Payée".equals(statutAff)) return "PAYEE";
        if ("Non payée".equals(statutAff)) return "EN_ATTENTE";
        if ("Payée partiellement".equals(statutAff)) return "ANNULEE";
        return "EN_ATTENTE";
    }

    private Color getStatutColor(String s) {
        if (s == null) return Color.BLACK;
        if ("Payée".equals(s)) return new Color(39, 174, 96);
        if ("Non payée".equals(s)) return new Color(183, 121, 31);
        if ("Payée partiellement".equals(s)) return new Color(192, 57, 43);
        return Color.BLACK;
    }

    private JLabel makeStatutBadge(String statut) {
        JLabel lbl = new JLabel(statut);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setForeground(Color.WHITE);
        lbl.setOpaque(true);
        lbl.setBorder(new EmptyBorder(4, 12, 4, 12));
        if ("Payée".equals(statut)) lbl.setBackground(new Color(39, 174, 96));
        else if ("Non payée".equals(statut)) lbl.setBackground(new Color(243, 156, 18));
        else if ("Payée partiellement".equals(statut)) lbl.setBackground(new Color(231, 76, 60));
        else lbl.setBackground(Color.GRAY);
        return lbl;
    }

    /** Construit un panel de section avec titre coloré et corps blanc. */
    private JPanel buildDetailsSection(String title, Color color) {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(color.darker(), 1, true),
            BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));

        JPanel titleBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 7));
        titleBar.setBackground(color);
        JLabel lbl = new JLabel(title);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lbl.setForeground(Color.WHITE);
        titleBar.add(lbl);
        panel.add(titleBar, BorderLayout.NORTH);
        return panel;
    }

    private void addDetailLine(JPanel panel, String label, String value) {
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setForeground(new Color(80, 80, 80));
        JLabel val = new JLabel(value != null ? value : "—");
        val.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        val.setForeground(new Color(30, 30, 30));
        panel.add(lbl);
        panel.add(val);
    }

    private void addTotalLine(JPanel panel, String label, String value, Color valueColor) {
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lbl.setForeground(new Color(80, 80, 80));
        JLabel val = new JLabel(value != null ? value : "—");
        val.setFont(new Font("Segoe UI", Font.BOLD, 14));
        val.setForeground(valueColor);
        panel.add(lbl);
        panel.add(val);
    }

    // ── UI factory helpers ────────────────────────────────────────────────────
    private static JLabel fieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        l.setForeground(new Color(80, 80, 80));
        return l;
    }

    private static JTextField field(String text) {
        JTextField f = new JTextField(text, 12);
        f.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        f.setPreferredSize(new Dimension(f.getPreferredSize().width, 30));
        f.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(new Color(189, 195, 199), 1, true),
            new EmptyBorder(3, 7, 3, 7)
        ));
        return f;
    }

    private static void styleCombo(JComboBox<String> cb) {
        cb.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cb.setPreferredSize(new Dimension(cb.getPreferredSize().width, 30));
        cb.setBackground(Color.WHITE);
    }

    private static JButton styledButton(String text, Color bg) {
        JButton b = new JButton(text);
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setOpaque(true);
        b.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { b.setBackground(bg.darker()); }
            @Override public void mouseExited(MouseEvent e)  { b.setBackground(bg); }
        });
        return b;
    }

    // ── Status cell renderer ──────────────────────────────────────────────────
    private static class StatusRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
            setHorizontalAlignment(JLabel.CENTER);
            setFont(new Font("Segoe UI", Font.BOLD, 11));

            String s = value == null ? "" : value.toString();
            if (!isSelected) {
                switch (s) {
                    case "Payée":
                        setBackground(new Color(232, 248, 241));
                        setForeground(new Color(39, 174, 96));
                        break;
                    case "Non payée":
                        setBackground(new Color(254, 249, 231));
                        setForeground(new Color(183, 121, 31));
                        break;
                    case "Payée partiellement":
                        setBackground(new Color(253, 236, 234));
                        setForeground(new Color(192, 57, 43));
                        break;
                    default:
                        setBackground(row % 2 == 0 ? Color.WHITE : C_ROW_ALT);
                        setForeground(Color.BLACK);
                }
            }
            setText(s);
            return this;
        }
    }

    // ── Entry point ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new HistoriqueAchats().setVisible(true));
    }
}