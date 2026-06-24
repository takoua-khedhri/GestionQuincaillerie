package com.myapp.ui;

import com.myapp.db.ConnexionSQLite;
import java.awt.BorderLayout;
import java.awt.Color;
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
import java.time.LocalDate;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PaiementUI extends JDialog {

    private static final Logger log = LoggerFactory.getLogger(PaiementUI.class);

    private static final Color COLOR_HEADER = new Color(44, 62, 80);
    private static final Color COLOR_BLUE = new Color(41, 128, 185);
    private static final Color COLOR_GREEN = new Color(46, 204, 113);
    private static final Color COLOR_ORANGE = new Color(243, 156, 18);
    private static final Color COLOR_RED = new Color(231, 76, 60);

    private final int factureId;
    private JLabel lblNumero, lblClient, lblMontantTotal, lblMontantPaye, lblResteAPayer, lblStatut;
    private DefaultTableModel tableModel;
    private JTable tablePaiements;
    private JTextField txtMontant, txtReference, txtNotes;
    private JComboBox<String> comboMode;

    public PaiementUI(int factureId) {
        this.factureId = factureId;
        setTitle("Suivi des Paiements");
        setModal(true);
        setSize(750, 650);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel mainPanel = new JPanel(new BorderLayout(0, 0));
        mainPanel.setBackground(Color.WHITE);

        mainPanel.add(buildHeaderPanel(), BorderLayout.NORTH);
        mainPanel.add(buildCenterPanel(), BorderLayout.CENTER);
        mainPanel.add(buildBottomPanel(), BorderLayout.SOUTH);

        setContentPane(mainPanel);
        chargerDonnees();
    }

    // ==================== HEADER ====================

    private JPanel buildHeaderPanel() {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBackground(COLOR_HEADER);
        header.setBorder(new EmptyBorder(15, 20, 15, 20));

        JLabel titleLabel = new JLabel("  Suivi des Paiements");
        titleLabel.setFont(FontHelper.getIconFont(18f));
        titleLabel.setForeground(Color.WHITE);
        header.add(titleLabel);

        header.add(Box.createVerticalStrut(10));

        JPanel infoGrid = new JPanel(new GridBagLayout());
        infoGrid.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 0, 2, 15);
        gbc.anchor = GridBagConstraints.WEST;

        lblNumero = createHeaderValue("");
        lblClient = createHeaderValue("");
        lblMontantTotal = createHeaderValue("");
        lblMontantPaye = createHeaderValue("");
        lblResteAPayer = createHeaderValue("");
        lblStatut = new JLabel();
        lblStatut.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblStatut.setOpaque(true);
        lblStatut.setBorder(BorderFactory.createEmptyBorder(3, 10, 3, 10));

        int row = 0;
        addHeaderRow(infoGrid, gbc, row++, "N° Facture :", lblNumero);
        addHeaderRow(infoGrid, gbc, row++, "Client :", lblClient);
        addHeaderRow(infoGrid, gbc, row++, "Montant Total :", lblMontantTotal);
        addHeaderRow(infoGrid, gbc, row++, "Montant Payé :", lblMontantPaye);
        addHeaderRow(infoGrid, gbc, row++, "Reste à Payer :", lblResteAPayer);

        gbc.gridx = 0; gbc.gridy = row;
        JLabel lblStatutTitle = new JLabel("Statut :");
        lblStatutTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblStatutTitle.setForeground(new Color(189, 195, 199));
        infoGrid.add(lblStatutTitle, gbc);
        gbc.gridx = 1;
        infoGrid.add(lblStatut, gbc);

        header.add(infoGrid);
        return header;
    }

    private void addHeaderRow(JPanel panel, GridBagConstraints gbc, int row, String label, JLabel value) {
        gbc.gridx = 0; gbc.gridy = row;
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lbl.setForeground(new Color(189, 195, 199));
        panel.add(lbl, gbc);
        gbc.gridx = 1;
        panel.add(value, gbc);
    }

    private JLabel createHeaderValue(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lbl.setForeground(Color.WHITE);
        return lbl;
    }

    // ==================== TABLE ====================

    private JPanel buildCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(10, 15, 5, 15));

        JLabel lblTitle = new JLabel("Historique des paiements");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblTitle.setForeground(COLOR_HEADER);
        lblTitle.setBorder(new EmptyBorder(0, 0, 8, 0));
        panel.add(lblTitle, BorderLayout.NORTH);

        String[] columns = {"Date", "Montant", "Mode", "Référence", "Notes"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tablePaiements = new JTable(tableModel);
        tablePaiements.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tablePaiements.setRowHeight(30);
        tablePaiements.setGridColor(new Color(230, 230, 230));
        tablePaiements.setSelectionBackground(new Color(214, 234, 248));

        JTableHeader tableHeader = tablePaiements.getTableHeader();
        tableHeader.setFont(new Font("Segoe UI", Font.BOLD, 12));
        tableHeader.setBackground(COLOR_HEADER);
        tableHeader.setForeground(Color.WHITE);
        tableHeader.setPreferredSize(new Dimension(0, 35));

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 0; i < columns.length; i++) {
            tablePaiements.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        JScrollPane scrollPane = new JScrollPane(tablePaiements);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    // ==================== FORM ====================

    private JPanel buildBottomPanel() {
        JPanel wrapper = new JPanel();
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
        wrapper.setBackground(Color.WHITE);
        wrapper.setBorder(new EmptyBorder(5, 15, 10, 15));

        // Form title
        JLabel lblForm = new JLabel("Ajouter un paiement");
        lblForm.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblForm.setForeground(COLOR_HEADER);
        lblForm.setBorder(new EmptyBorder(0, 0, 8, 0));
        lblForm.setAlignmentX(LEFT_ALIGNMENT);
        wrapper.add(lblForm);

        // Form fields
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(Color.WHITE);
        formPanel.setAlignmentX(LEFT_ALIGNMENT);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 5, 4, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        txtMontant = createStyledTextField();
        comboMode = createStyledComboBox();
        comboMode.addItem("Espèces");
        comboMode.addItem("Chèque");
        comboMode.addItem("Virement");
        comboMode.addItem("Carte bancaire");
        comboMode.addItem("Traite");
        txtReference = createStyledTextField();
        txtNotes = createStyledTextField();

        int col = 0;
        gbc.gridy = 0;

        gbc.gridx = col++; gbc.weightx = 0;
        formPanel.add(createLabel("Montant :"), gbc);
        gbc.gridx = col++; gbc.weightx = 0.2;
        formPanel.add(txtMontant, gbc);

        gbc.gridx = col++; gbc.weightx = 0;
        formPanel.add(createLabel("Mode :"), gbc);
        gbc.gridx = col++; gbc.weightx = 0.2;
        formPanel.add(comboMode, gbc);

        gbc.gridx = col++; gbc.weightx = 0;
        formPanel.add(createLabel("Réf :"), gbc);
        gbc.gridx = col++; gbc.weightx = 0.15;
        formPanel.add(txtReference, gbc);

        gbc.gridx = col++; gbc.weightx = 0;
        formPanel.add(createLabel("Notes :"), gbc);
        gbc.gridx = col++; gbc.weightx = 0.2;
        formPanel.add(txtNotes, gbc);

        wrapper.add(formPanel);
        wrapper.add(Box.createVerticalStrut(8));

        // Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnPanel.setBackground(Color.WHITE);
        btnPanel.setAlignmentX(LEFT_ALIGNMENT);

        JButton btnEnregistrer = createStyledButton("  Enregistrer le paiement", COLOR_GREEN);
        btnEnregistrer.addActionListener(e -> enregistrerPaiement());

        JButton btnFermer = createStyledButton("Fermer", new Color(149, 165, 166));
        btnFermer.addActionListener(e -> dispose());

        btnPanel.add(btnEnregistrer);
        btnPanel.add(btnFermer);
        wrapper.add(btnPanel);

        return wrapper;
    }

    // ==================== DATA ====================

    private void chargerDonnees() {
        // Load facture info
        String sql = "SELECT numero, client_nom, net_a_payer, " +
                "COALESCE(montant_paye, 0) AS montant_paye, " +
                "COALESCE(statut_paiement, 'Impayé') AS statut_paiement " +
                "FROM Factures WHERE id = ?";
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, factureId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                String numero = rs.getString("numero");
                String client = rs.getString("client_nom");
                double total = rs.getDouble("net_a_payer");
                double paye = rs.getDouble("montant_paye");
                double reste = total - paye;
                String statut = rs.getString("statut_paiement");

                lblNumero.setText(numero);
                lblClient.setText(client);
                lblMontantTotal.setText(formatMontant(total) + " DT");
                lblMontantPaye.setText(formatMontant(paye) + " DT");
                lblResteAPayer.setText(formatMontant(reste) + " DT");

                // Statut badge
                if ("Payé".equalsIgnoreCase(statut)) {
                    lblStatut.setText("  Payé  ");
                    lblStatut.setBackground(COLOR_GREEN);
                    lblStatut.setForeground(Color.WHITE);
                } else if ("Partiel".equalsIgnoreCase(statut)) {
                    lblStatut.setText("  Partiel  ");
                    lblStatut.setBackground(COLOR_ORANGE);
                    lblStatut.setForeground(Color.WHITE);
                } else {
                    lblStatut.setText("  Impayé  ");
                    lblStatut.setBackground(COLOR_RED);
                    lblStatut.setForeground(Color.WHITE);
                }

                // Pre-fill remaining amount
                if (reste > 0) {
                    txtMontant.setText(formatMontant(reste));
                } else {
                    txtMontant.setText("0.000");
                }
            }
        } catch (Exception e) {
            log.error("Erreur chargement facture id={}", factureId, e);
        }

        // Load paiements
        tableModel.setRowCount(0);
        String sqlP = "SELECT date_paiement, montant, mode_paiement, reference, notes " +
                "FROM Paiements WHERE facture_id = ? ORDER BY date_paiement DESC";
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(sqlP)) {
            pst.setInt(1, factureId);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                Object[] row = {
                    rs.getString("date_paiement"),
                    formatMontant(rs.getDouble("montant")) + " DT",
                    rs.getString("mode_paiement"),
                    rs.getString("reference"),
                    rs.getString("notes")
                };
                tableModel.addRow(row);
            }
        } catch (Exception e) {
            log.error("Erreur chargement paiements pour facture id={}", factureId, e);
        }
    }

    private void enregistrerPaiement() {
        String montantStr = txtMontant.getText().trim().replace(" ", "").replace(",", ".");
        double montant;
        try {
            montant = Double.parseDouble(montantStr);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Montant invalide.", "Erreur", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (montant <= 0) {
            JOptionPane.showMessageDialog(this, "Le montant doit être supérieur à 0.", "Erreur", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String mode = (String) comboMode.getSelectedItem();
        String reference = txtReference.getText().trim();
        String notes = txtNotes.getText().trim();
        String datePaiement = LocalDate.now().toString();

        try (Connection conn = ConnexionSQLite.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // INSERT paiement
                String sqlInsert = "INSERT INTO Paiements (facture_id, date_paiement, montant, mode_paiement, reference, notes) " +
                        "VALUES (?, ?, ?, ?, ?, ?)";
                try (PreparedStatement pst = conn.prepareStatement(sqlInsert)) {
                    pst.setInt(1, factureId);
                    pst.setString(2, datePaiement);
                    pst.setDouble(3, montant);
                    pst.setString(4, mode);
                    pst.setString(5, reference.isEmpty() ? null : reference);
                    pst.setString(6, notes.isEmpty() ? null : notes);
                    pst.executeUpdate();
                }

                // UPDATE facture
                String sqlUpdate = "UPDATE Factures SET montant_paye = COALESCE(montant_paye, 0) + ?, " +
                        "statut_paiement = CASE " +
                        "  WHEN COALESCE(montant_paye, 0) + ? >= net_a_payer THEN 'Payé' " +
                        "  WHEN COALESCE(montant_paye, 0) + ? > 0 THEN 'Partiel' " +
                        "  ELSE 'Impayé' END " +
                        "WHERE id = ?";
                try (PreparedStatement pst = conn.prepareStatement(sqlUpdate)) {
                    pst.setDouble(1, montant);
                    pst.setDouble(2, montant);
                    pst.setDouble(3, montant);
                    pst.setInt(4, factureId);
                    pst.executeUpdate();
                }

                conn.commit();
                log.info("Paiement enregistré: facture_id={}, montant={}, mode={}", factureId, montant, mode);

                // Reset form
                txtReference.setText("");
                txtNotes.setText("");

                // Refresh
                chargerDonnees();

                JOptionPane.showMessageDialog(this, "Paiement enregistré avec succès.",
                        "Succès", JOptionPane.INFORMATION_MESSAGE);

            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        } catch (Exception e) {
            log.error("Erreur enregistrement paiement pour facture id={}", factureId, e);
            JOptionPane.showMessageDialog(this, "Erreur lors de l'enregistrement : " + e.getMessage(),
                    "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ==================== HELPERS ====================

    private String formatMontant(double montant) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.FRANCE);
        symbols.setGroupingSeparator(' ');
        DecimalFormat df = new DecimalFormat("#,##0.000", symbols);
        return df.format(montant).replace(",", ".");
    }

    private JLabel createLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setForeground(COLOR_HEADER);
        return lbl;
    }

    private JTextField createStyledTextField() {
        JTextField textField = new JTextField();
        textField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        textField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        textField.setBackground(Color.WHITE);
        return textField;
    }

    private JComboBox<String> createStyledComboBox() {
        JComboBox<String> comboBox = new JComboBox<>();
        comboBox.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        comboBox.setBackground(Color.WHITE);
        return comboBox;
    }

    private JButton createStyledButton(String text, final Color backgroundColor) {
        final JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setBackground(backgroundColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(darkenColor(backgroundColor, 0.8f), 1),
                BorderFactory.createEmptyBorder(8, 15, 8, 15)));
        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent evt) { button.setBackground(darkenColor(backgroundColor, 0.9f)); }
            public void mouseExited(MouseEvent evt) { button.setBackground(backgroundColor); }
        });
        return button;
    }

    private Color darkenColor(Color color, float factor) {
        return new Color(
            Math.max((int)(color.getRed() * factor), 0),
            Math.max((int)(color.getGreen() * factor), 0),
            Math.max((int)(color.getBlue() * factor), 0)
        );
    }
}
