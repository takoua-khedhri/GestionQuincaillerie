package com.myapp.ui;

import com.myapp.db.ConnexionSQLite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AvoirUI extends JDialog {

    private static final Logger log = LoggerFactory.getLogger(AvoirUI.class);

    private final int factureId;
    private JTable tableArticles;
    private DefaultTableModel tableModel;
    private JTextField txtMotif;
    private JLabel lblTotalHT;
    private JLabel lblTVA;
    private JLabel lblTotalTTC;
    private DecimalFormat df;

    // Facture info
    private String factureNumero;
    private String factureDate;
    private String factureClient;
    private double factureMontantTTC;

    public AvoirUI(int factureId) {
        this.factureId = factureId;
        this.df = new DecimalFormat("#,##0.000", new DecimalFormatSymbols(Locale.FRANCE));
        log.info("Ouverture du dialogue Avoir pour facture id={}", factureId);

        setTitle("Création d'un Avoir");
        setModal(true);
        setSize(900, 650);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        loadFactureInfo();
        initializeUI();
    }

    private void loadFactureInfo() {
        String sql = "SELECT numero, date, client_nom, montant_ttc FROM Factures WHERE id = ?";
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, factureId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                factureNumero = rs.getString("numero");
                factureDate = rs.getString("date");
                factureClient = rs.getString("client_nom");
                factureMontantTTC = rs.getDouble("montant_ttc");
            }
        } catch (Exception e) {
            log.error("Erreur chargement facture id={}", factureId, e);
        }
    }

    private void initializeUI() {
        JPanel mainPanel = new JPanel(new BorderLayout(0, 0));
        mainPanel.setBackground(Color.WHITE);

        mainPanel.add(createHeaderPanel(), BorderLayout.NORTH);
        mainPanel.add(createContentPanel(), BorderLayout.CENTER);
        mainPanel.add(createFooterPanel(), BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(44, 62, 80));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JLabel lblTitle = new JLabel("  Création d'un Avoir");
        lblTitle.setFont(FontHelper.getIconFont(14f));
        JLabel lblTitleText = new JLabel("Création d'un Avoir");
        lblTitleText.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitleText.setForeground(Color.WHITE);

        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        titlePanel.setBackground(new Color(44, 62, 80));

        JLabel iconLabel = new JLabel("");
        iconLabel.setFont(FontHelper.getIconFont(18f));
        iconLabel.setForeground(Color.WHITE);
        titlePanel.add(iconLabel);
        titlePanel.add(lblTitleText);

        // Facture info
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 0));
        infoPanel.setBackground(new Color(44, 62, 80));

        infoPanel.add(createHeaderInfoLabel("N°: " + (factureNumero != null ? factureNumero : "")));
        infoPanel.add(createHeaderInfoLabel("Date: " + (factureDate != null ? factureDate : "")));
        infoPanel.add(createHeaderInfoLabel("Client: " + (factureClient != null ? factureClient : "")));
        infoPanel.add(createHeaderInfoLabel("Montant TTC: " + formatMontant(factureMontantTTC)));

        JPanel headerContent = new JPanel();
        headerContent.setLayout(new BoxLayout(headerContent, BoxLayout.Y_AXIS));
        headerContent.setBackground(new Color(44, 62, 80));
        headerContent.add(titlePanel);
        headerContent.add(Box.createVerticalStrut(8));
        headerContent.add(infoPanel);

        panel.add(headerContent, BorderLayout.CENTER);
        return panel;
    }

    private JLabel createHeaderInfoLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lbl.setForeground(new Color(189, 195, 199));
        return lbl;
    }

    private JPanel createContentPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(15, 20, 10, 20));

        // Table
        String[] columns = {"Article", "Qté facturée", "Qté à retourner", "Prix unitaire", "Montant retour"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 2; // Only "Qté à retourner" editable
            }
        };

        tableArticles = new JTable(tableModel);
        tableArticles.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tableArticles.setRowHeight(35);
        tableArticles.setShowGrid(true);
        tableArticles.setGridColor(new Color(220, 220, 220));
        tableArticles.setSelectionBackground(new Color(52, 152, 219, 40));

        // Header styling
        JTableHeader header = tableArticles.getTableHeader();
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setBackground(new Color(52, 73, 94));
                setForeground(Color.WHITE);
                setFont(new Font("Segoe UI", Font.BOLD, 12));
                setHorizontalAlignment(JLabel.CENTER);
                return this;
            }
        });

        // Spinner editor for "Qté à retourner" column
        loadFactureDetails();
        setupSpinnerEditor();

        // Cell renderer for amounts
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(JLabel.RIGHT);
        rightRenderer.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tableArticles.getColumnModel().getColumn(3).setCellRenderer(rightRenderer);
        tableArticles.getColumnModel().getColumn(4).setCellRenderer(rightRenderer);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        centerRenderer.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tableArticles.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);

        JScrollPane scrollPane = new JScrollPane(tableArticles);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 1));
        scrollPane.getViewport().setBackground(Color.WHITE);

        // Motif panel
        JPanel motifPanel = new JPanel(new BorderLayout(10, 0));
        motifPanel.setBackground(Color.WHITE);
        motifPanel.setBorder(new EmptyBorder(10, 0, 0, 0));

        JLabel lblMotif = new JLabel("Motif de l'avoir :");
        lblMotif.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblMotif.setForeground(new Color(44, 62, 80));
        motifPanel.add(lblMotif, BorderLayout.WEST);

        txtMotif = new JTextField();
        txtMotif.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtMotif.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        txtMotif.setBackground(Color.WHITE);
        motifPanel.add(txtMotif, BorderLayout.CENTER);

        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(motifPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createFooterPanel() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(Color.WHITE);
        footer.setBorder(new EmptyBorder(10, 20, 15, 20));

        // Totals panel
        JPanel totalsPanel = new JPanel(new GridBagLayout());
        totalsPanel.setBackground(new Color(245, 245, 245));
        totalsPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
            BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 5, 3, 5);
        gbc.anchor = GridBagConstraints.EAST;

        gbc.gridx = 0; gbc.gridy = 0;
        totalsPanel.add(createLabel("Total HT Retour:", Font.BOLD, 13, new Color(44, 62, 80)), gbc);
        gbc.gridx = 1;
        lblTotalHT = createLabel("0.000", Font.BOLD, 13, new Color(41, 128, 185));
        totalsPanel.add(lblTotalHT, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        totalsPanel.add(createLabel("TVA Retour:", Font.BOLD, 13, new Color(44, 62, 80)), gbc);
        gbc.gridx = 1;
        lblTVA = createLabel("0.000", Font.BOLD, 13, new Color(41, 128, 185));
        totalsPanel.add(lblTVA, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        totalsPanel.add(createLabel("Total TTC Retour:", Font.BOLD, 14, new Color(44, 62, 80)), gbc);
        gbc.gridx = 1;
        lblTotalTTC = createLabel("0.000", Font.BOLD, 14, new Color(231, 76, 60));
        totalsPanel.add(lblTotalTTC, gbc);

        // Buttons panel
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonsPanel.setBackground(Color.WHITE);

        JButton btnAnnuler = createStyledButton("Annuler", new Color(149, 165, 166));
        btnAnnuler.addActionListener(e -> dispose());

        JButton btnValider = createStyledButton("Valider l'avoir", new Color(41, 128, 185));
        btnValider.addActionListener(e -> validerAvoir());

        buttonsPanel.add(btnAnnuler);
        buttonsPanel.add(btnValider);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(Color.WHITE);
        bottomPanel.add(totalsPanel, BorderLayout.CENTER);
        bottomPanel.add(buttonsPanel, BorderLayout.SOUTH);

        footer.add(bottomPanel, BorderLayout.CENTER);
        return footer;
    }

    private void loadFactureDetails() {
        String sql = "SELECT article_designation, quantite, prix_unitaire, tva FROM DetailsFacture WHERE facture_id = ?";
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, factureId);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                String designation = rs.getString("article_designation");
                int quantite = rs.getInt("quantite");
                double prixUnitaire = rs.getDouble("prix_unitaire");
                Object[] row = {
                    designation,
                    quantite,
                    0,
                    formatMontant(prixUnitaire),
                    formatMontant(0.0)
                };
                tableModel.addRow(row);
            }
        } catch (Exception e) {
            log.error("Erreur chargement détails facture id={}", factureId, e);
        }
    }

    private void setupSpinnerEditor() {
        TableColumn colRetour = tableArticles.getColumnModel().getColumn(2);

        colRetour.setCellEditor(new javax.swing.DefaultCellEditor(new JTextField()) {
            private JSpinner spinner;

            @Override
            public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
                int qteFacturee = 0;
                try {
                    qteFacturee = Integer.parseInt(table.getValueAt(row, 1).toString());
                } catch (NumberFormatException ignored) {}

                spinner = new JSpinner(new SpinnerNumberModel(
                    value instanceof Integer ? (Integer) value : 0,
                    0, qteFacturee, 1
                ));
                spinner.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                return spinner;
            }

            @Override
            public Object getCellEditorValue() {
                return spinner.getValue();
            }

            @Override
            public boolean stopCellEditing() {
                boolean result = super.stopCellEditing();
                recalculerTotaux();
                return result;
            }
        });

        // Center renderer for spinner column
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        centerRenderer.setFont(new Font("Segoe UI", Font.BOLD, 13));
        centerRenderer.setForeground(new Color(41, 128, 185));
        colRetour.setCellRenderer(centerRenderer);
    }

    private void recalculerTotaux() {
        double totalHT = 0;
        double totalTVA = 0;

        // Load TVA rates from DB for each line
        String sql = "SELECT article_designation, tva FROM DetailsFacture WHERE facture_id = ?";
        java.util.Map<String, Double> tvaMap = new java.util.HashMap<>();
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, factureId);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                tvaMap.put(rs.getString("article_designation"), rs.getDouble("tva"));
            }
        } catch (Exception e) {
            log.error("Erreur chargement TVA pour facture id={}", factureId, e);
        }

        for (int i = 0; i < tableModel.getRowCount(); i++) {
            int qteRetour = 0;
            try {
                qteRetour = Integer.parseInt(tableModel.getValueAt(i, 2).toString());
            } catch (NumberFormatException ignored) {}

            double prixUnitaire = parseMontant(tableModel.getValueAt(i, 3).toString());
            double montantHT = qteRetour * prixUnitaire;

            String designation = tableModel.getValueAt(i, 0).toString();
            double tvaPct = tvaMap.getOrDefault(designation, 0.0);
            double montantTVA = montantHT * tvaPct / 100.0;

            totalHT += montantHT;
            totalTVA += montantTVA;

            tableModel.setValueAt(formatMontant(montantHT), i, 4);
        }

        double totalTTC = totalHT + totalTVA;

        lblTotalHT.setText(formatMontant(totalHT));
        lblTVA.setText(formatMontant(totalTVA));
        lblTotalTTC.setText(formatMontant(totalTTC));
    }

    private void validerAvoir() {
        // Check at least one item has qty > 0
        boolean hasRetour = false;
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            int qte = 0;
            try {
                qte = Integer.parseInt(tableModel.getValueAt(i, 2).toString());
            } catch (NumberFormatException ignored) {}
            if (qte > 0) {
                hasRetour = true;
                break;
            }
        }

        if (!hasRetour) {
            JOptionPane.showMessageDialog(this,
                "Veuillez saisir au moins une quantité à retourner.",
                "Aucun retour", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String motif = txtMotif.getText().trim();
        if (motif.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Veuillez saisir un motif pour l'avoir.",
                "Motif requis", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            // Build items list
            java.util.List<java.util.Map<String, Object>> items = new java.util.ArrayList<>();
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                int qteRetour = 0;
                try {
                    qteRetour = Integer.parseInt(tableModel.getValueAt(i, 2).toString());
                } catch (NumberFormatException ignored) {}

                if (qteRetour > 0) {
                    java.util.Map<String, Object> item = new java.util.HashMap<>();
                    item.put("designation", tableModel.getValueAt(i, 0).toString());
                    item.put("quantite", qteRetour);
                    item.put("prix_unitaire", parseMontant(tableModel.getValueAt(i, 3).toString()));
                    items.add(item);
                }
            }

            // Call AvoirService
            com.myapp.service.AvoirService.creerAvoir(factureId, motif, items);

            log.info("Avoir créé avec succès pour facture id={}", factureId);

            JOptionPane.showMessageDialog(this,
                "L'avoir a été créé avec succès.",
                "Avoir créé", JOptionPane.INFORMATION_MESSAGE);
            dispose();

        } catch (Exception e) {
            log.error("Erreur lors de la création de l'avoir pour facture id={}", factureId, e);
            JOptionPane.showMessageDialog(this,
                "Erreur lors de la création de l'avoir : " + e.getMessage(),
                "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JButton createStyledButton(String text, final Color backgroundColor) {
        final JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setBackground(backgroundColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(darkenColor(backgroundColor, 0.8f), 1),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent evt) { button.setBackground(darkenColor(backgroundColor, 0.9f)); }
            public void mouseExited(MouseEvent evt) { button.setBackground(backgroundColor); }
        });
        return button;
    }

    private JLabel createLabel(String text, int style, int size, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", style, size));
        l.setForeground(color);
        return l;
    }

    private Color darkenColor(Color color, float factor) {
        return new Color(
            Math.max((int) (color.getRed() * factor), 0),
            Math.max((int) (color.getGreen() * factor), 0),
            Math.max((int) (color.getBlue() * factor), 0)
        );
    }

    private String formatMontant(double montant) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.FRANCE);
        symbols.setGroupingSeparator(' ');
        DecimalFormat fmt = new DecimalFormat("#,##0.000", symbols);
        return fmt.format(montant);
    }

    private double parseMontant(String text) {
        try {
            String cleaned = text.replace(" ", "").replace(",", ".");
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}
