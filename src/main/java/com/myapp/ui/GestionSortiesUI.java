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
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableRowSorter;

public class GestionSortiesUI extends JFrame {

    private JTable tableArticles;
    private DefaultTableModel model;
    private JTextField txtRecherche;
    private JTextField txtQuantite;
    private TableRowSorter<DefaultTableModel> sorter;
    private JLabel lblStatut;
    private JLabel lblStockInfo;
    private Font fontAwesomeSolid;

    // COULEURS THEME SORTIE (Rouge/Orange)
    private final Color COLOR_HEADER = new Color(231, 76, 60);  // Rouge vif
    private final Color COLOR_BG = new Color(240, 242, 245);    // Gris fond
    private final Color COLOR_BTN_VALID = new Color(192, 57, 43); // Rouge foncé
    private final Color COLOR_BTN_BACK = new Color(52, 73, 94);   // Bleu nuit
    
    // Couleurs spécifiques pour les boutons + et -
    private final Color COLOR_BTN_PLUS = new Color(46, 204, 113); // Vert
    private final Color COLOR_BTN_MINUS = new Color(231, 76, 60); // Rouge

    public GestionSortiesUI() {
        this.setTitle("Gestion des Sorties de Stock");
        this.setExtendedState(JFrame.MAXIMIZED_BOTH);
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.setLocationRelativeTo(null);
        this.setLayout(new BorderLayout(10, 10));
        this.getContentPane().setBackground(COLOR_BG);
        
        this.loadFontAwesome();
        this.initUI();
        this.chargerArticles();
        this.setVisible(true);
    }

    private void loadFontAwesome() {
        try {
            InputStream fontStream = this.getClass().getResourceAsStream("/fonts/fa.ttf");
            if (fontStream != null) {
                this.fontAwesomeSolid = Font.createFont(Font.TRUETYPE_FONT, fontStream).deriveFont(Font.PLAIN, 14);
                GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
                ge.registerFont(this.fontAwesomeSolid);
                fontStream.close();
            } else {
                this.fontAwesomeSolid = new Font("SansSerif", Font.PLAIN, 14);
            }
        } catch (IOException | FontFormatException e) {
            this.fontAwesomeSolid = new Font("SansSerif", Font.PLAIN, 14);
        }
    }

    private String getHtmlText(String iconCode, String text) {
        String fontName = (this.fontAwesomeSolid != null) ? this.fontAwesomeSolid.getFontName() : "SansSerif";
        return "<html><span style='font-family:" + fontName + ";'>" + iconCode + "</span> " + text + "</html>";
    }

    private void initUI() {
        JPanel headerPanel = this.createHeaderPanel();
        JPanel controlPanel = this.createControlPanel();
        JScrollPane scrollPane = this.createTablePanel();
        JPanel inputPanel = this.createInputPanel();
        JPanel statusPanel = this.createStatusPanel();

        this.add(headerPanel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        centerPanel.setBackground(COLOR_BG);
        centerPanel.add(controlPanel, BorderLayout.NORTH);
        centerPanel.add(scrollPane, BorderLayout.CENTER);
        this.add(centerPanel, BorderLayout.CENTER);

        JPanel southPanel = new JPanel(new BorderLayout(10, 10));
        southPanel.setBorder(BorderFactory.createEmptyBorder(0, 15, 15, 15));
        southPanel.setBackground(COLOR_BG);
        southPanel.add(inputPanel, BorderLayout.CENTER);
        southPanel.add(statusPanel, BorderLayout.SOUTH);
        this.add(southPanel, BorderLayout.SOUTH);
    }

    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(COLOR_HEADER);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        // Titre avec Icône "Sortie" (Sign-out)
        JLabel titleLabel = new JLabel(getHtmlText("\uf08b", " GESTION DES SORTIES"), JLabel.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        titleLabel.setForeground(Color.WHITE);

        JLabel subTitleLabel = new JLabel("Retirer des produits du stock (Recherche par ID, QR ou Désignation)", JLabel.CENTER);
        subTitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subTitleLabel.setForeground(new Color(250, 250, 250));
        subTitleLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));

        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(COLOR_HEADER);
        titlePanel.add(titleLabel, BorderLayout.CENTER);
        titlePanel.add(subTitleLabel, BorderLayout.SOUTH);

        // Bouton Retour
        final JButton btnRetour = new JButton(getHtmlText("\uf060", " Retour"));
        btnRetour.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnRetour.setBackground(COLOR_BTN_BACK);
        btnRetour.setForeground(Color.WHITE);
        btnRetour.setFocusPainted(false);
        btnRetour.setBorderPainted(false);
        btnRetour.setOpaque(true);
        btnRetour.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        btnRetour.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        btnRetour.addActionListener(e -> {
            new GestionStock().setVisible(true);
            this.dispose();
        });

        btnRetour.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent evt) { btnRetour.setBackground(COLOR_HEADER.darker()); }
            @Override
            public void mouseExited(MouseEvent evt) { btnRetour.setBackground(COLOR_BTN_BACK); }
        });

        headerPanel.add(titlePanel, BorderLayout.CENTER);
        headerPanel.add(btnRetour, BorderLayout.WEST);
        return headerPanel;
    }

    private JPanel createControlPanel() {
        JPanel controlPanel = new JPanel(new BorderLayout(10, 0));
        controlPanel.setBackground(Color.WHITE);
        controlPanel.setBorder(new CompoundBorder(
                new LineBorder(new Color(200, 200, 200), 1),
                new EmptyBorder(15, 20, 15, 20)));

        JLabel lblRecherche = new JLabel(getHtmlText("\uf002", " Recherche:"));
        lblRecherche.setFont(new Font("Segoe UI", Font.BOLD, 14));

        this.txtRecherche = new JTextField();
        this.txtRecherche.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        this.txtRecherche.setBorder(new CompoundBorder(
                new LineBorder(new Color(180, 180, 180)),
                new EmptyBorder(8, 12, 8, 12)));
        this.txtRecherche.setPreferredSize(new Dimension(350, 40));
        this.txtRecherche.putClientProperty("JTextField.placeholderText", "Désignation, Code QR...");
        
        this.txtRecherche.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                GestionSortiesUI.this.filtrerTableau();
            }
        });

        this.lblStockInfo = new JLabel("<html><i style='color:gray'>Sélectionnez un article pour voir les détails</i></html>");
        this.lblStockInfo.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftPanel.setBackground(Color.WHITE);
        leftPanel.add(lblRecherche);
        leftPanel.add(this.txtRecherche);

        controlPanel.add(leftPanel, BorderLayout.WEST);
        controlPanel.add(this.lblStockInfo, BorderLayout.EAST);
        return controlPanel;
    }

    private JScrollPane createTablePanel() {
        // Colonnes: ID, Code QR, Désignation, Prix Gros, Prix Détail, Stock actuel, TVA
        this.model = new DefaultTableModel(new String[]{"ID", "Code QR", "Désignation", "Prix Gros (DT)", "Prix Détail (DT)", "Stock actuel", "TVA (%)"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if(columnIndex == 0 || columnIndex == 5 || columnIndex == 6) return Integer.class;
                return String.class;
            }
        };

        this.tableArticles = new JTable(this.model);
        this.sorter = new TableRowSorter<>(this.model);
        this.tableArticles.setRowSorter(this.sorter);
        this.tableArticles.setRowHeight(40);
        this.tableArticles.setSelectionBackground(new Color(255, 230, 230)); // Fond rouge clair
        this.tableArticles.setSelectionForeground(Color.BLACK);
        this.tableArticles.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        this.tableArticles.setShowGrid(true);
        this.tableArticles.setGridColor(new Color(230, 230, 230));

        JTableHeader header = this.tableArticles.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 14));
        header.setBackground(COLOR_HEADER);
        header.setForeground(Color.WHITE);
        header.setReorderingAllowed(false);
        
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                this.setBackground(COLOR_HEADER);
                this.setForeground(Color.WHITE);
                this.setFont(new Font("Segoe UI", Font.BOLD, 14));
                this.setHorizontalAlignment(JLabel.CENTER);
                this.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(255, 255, 255, 50)));
                return this;
            }
        });

        // Largeurs des colonnes
        this.tableArticles.getColumnModel().getColumn(0).setPreferredWidth(50);   // ID
        this.tableArticles.getColumnModel().getColumn(1).setPreferredWidth(120);  // QR
        this.tableArticles.getColumnModel().getColumn(2).setPreferredWidth(300);  // Désignation
        this.tableArticles.getColumnModel().getColumn(3).setPreferredWidth(100);  // Prix Gros
        this.tableArticles.getColumnModel().getColumn(4).setPreferredWidth(100);  // Prix Détail
        this.tableArticles.getColumnModel().getColumn(5).setPreferredWidth(100);  // Stock
        this.tableArticles.getColumnModel().getColumn(6).setPreferredWidth(60);   // TVA

        // Alignement centré
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        this.tableArticles.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        this.tableArticles.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);
        this.tableArticles.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);
        this.tableArticles.getColumnModel().getColumn(4).setCellRenderer(centerRenderer);
        this.tableArticles.getColumnModel().getColumn(5).setCellRenderer(centerRenderer);
        this.tableArticles.getColumnModel().getColumn(6).setCellRenderer(centerRenderer);

        this.tableArticles.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                this.mettreAJourInfoStock();
            }
        });

        JScrollPane scrollPane = new JScrollPane(this.tableArticles);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        scrollPane.getViewport().setBackground(Color.WHITE);
        return scrollPane;
    }

    private JPanel createInputPanel() {
        JPanel inputPanel = new JPanel(new BorderLayout(15, 0));
        inputPanel.setBackground(Color.WHITE);
        inputPanel.setBorder(new CompoundBorder(
                new TitledBorder(new LineBorder(COLOR_HEADER, 1), " Enregistrer une Sortie ", TitledBorder.LEADING, TitledBorder.TOP, new Font("Segoe UI", Font.BOLD, 12), COLOR_HEADER),
                new EmptyBorder(15, 25, 15, 25)));

        JPanel fieldsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        fieldsPanel.setBackground(Color.WHITE);

        JLabel lblQuantite = new JLabel("Quantité à retirer:");
        lblQuantite.setFont(new Font("Segoe UI", Font.BOLD, 14));

        this.txtQuantite = new JTextField("1", 8);
        this.txtQuantite.setFont(new Font("Segoe UI", Font.BOLD, 16));
        this.txtQuantite.setBorder(new CompoundBorder(new LineBorder(new Color(180, 180, 180)), new EmptyBorder(5, 5, 5, 5)));
        this.txtQuantite.setHorizontalAlignment(JTextField.CENTER);

        JPanel quantityPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        quantityPanel.setBackground(Color.WHITE);

        // --- Bouton Moins (-) : ROUGE ---
        final JButton btnMoins = new JButton(getHtmlText("\uf068", ""));
        btnMoins.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnMoins.setBackground(COLOR_BTN_MINUS); 
        btnMoins.setForeground(Color.WHITE);
        btnMoins.setFocusPainted(false);
        btnMoins.setBorderPainted(false);
        btnMoins.setOpaque(true);
        btnMoins.setPreferredSize(new Dimension(45, 35));
        
        btnMoins.addActionListener(e -> this.ajusterQuantite(-1));
        btnMoins.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent evt) { btnMoins.setBackground(COLOR_BTN_MINUS.darker()); }
            @Override public void mouseExited(MouseEvent evt) { btnMoins.setBackground(COLOR_BTN_MINUS); }
        });

        // --- Bouton Plus (+) : VERT ---
        final JButton btnPlus = new JButton(getHtmlText("\uf067", ""));
        btnPlus.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnPlus.setBackground(COLOR_BTN_PLUS); 
        btnPlus.setForeground(Color.WHITE);
        btnPlus.setFocusPainted(false);
        btnPlus.setBorderPainted(false);
        btnPlus.setOpaque(true);
        btnPlus.setPreferredSize(new Dimension(45, 35));
        
        btnPlus.addActionListener(e -> this.ajusterQuantite(1));
        btnPlus.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent evt) { btnPlus.setBackground(COLOR_BTN_PLUS.darker()); }
            @Override public void mouseExited(MouseEvent evt) { btnPlus.setBackground(COLOR_BTN_PLUS); }
        });

        quantityPanel.add(btnMoins);
        quantityPanel.add(this.txtQuantite);
        quantityPanel.add(btnPlus);

        // Bouton Valider
        final JButton btnValider = new JButton(getHtmlText("\uf00c", " Valider la sortie"));
        btnValider.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnValider.setBackground(COLOR_BTN_VALID);
        btnValider.setForeground(Color.WHITE);
        btnValider.setFocusPainted(false);
        btnValider.setBorderPainted(false);
        btnValider.setOpaque(true);
        btnValider.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnValider.setBorder(BorderFactory.createEmptyBorder(10, 25, 10, 25));
        btnValider.addActionListener(e -> this.validerSortie());

        btnValider.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent evt) { btnValider.setBackground(COLOR_BTN_VALID.darker()); }
            @Override public void mouseExited(MouseEvent evt) { btnValider.setBackground(COLOR_BTN_VALID); }
        });

        fieldsPanel.add(lblQuantite);
        fieldsPanel.add(quantityPanel);
        fieldsPanel.add(btnValider);

        inputPanel.add(fieldsPanel, BorderLayout.CENTER);
        return inputPanel;
    }

    private JPanel createStatusPanel() {
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBackground(new Color(245, 245, 245));
        statusPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        this.lblStatut = new JLabel(getHtmlText("\uf05a", " Info : Sélectionnez un article et saisissez la quantité à retirer."), JLabel.CENTER);
        this.lblStatut.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        this.lblStatut.setForeground(new Color(100, 100, 100));

        statusPanel.add(this.lblStatut, BorderLayout.CENTER);
        return statusPanel;
    }

    private void ajusterQuantite(int delta) {
        try {
            int quantite = this.txtQuantite.getText().isEmpty() ? 0 : Integer.parseInt(this.txtQuantite.getText());
            quantite = Math.max(1, quantite + delta);
            this.txtQuantite.setText(String.valueOf(quantite));
        } catch (NumberFormatException e) {
            this.txtQuantite.setText("1");
        }
    }

    private void filtrerTableau() {
        String text = this.txtRecherche.getText().trim();
        if (text.length() == 0) {
            this.sorter.setRowFilter(null);
        } else {
            try {
                this.sorter.setRowFilter(RowFilter.regexFilter("(?i)" + text, 0, 1, 2));
            } catch (java.util.regex.PatternSyntaxException e) {
                // Ignorer
            }
        }
    }

    private void mettreAJourInfoStock() {
        int selectedRow = this.tableArticles.getSelectedRow();
        if (selectedRow != -1) {
            int modelRow = this.tableArticles.convertRowIndexToModel(selectedRow);
            String designation = (String) this.model.getValueAt(modelRow, 2);
            int stock = (Integer) this.model.getValueAt(modelRow, 5);
            String qr = (String) this.model.getValueAt(modelRow, 1);
            double prixGros = 0;
            double prixDetail = 0;
            
            try {
                String prixGrosStr = (String) this.model.getValueAt(modelRow, 3);
                prixGrosStr = prixGrosStr.replace(",", ".").replace(" DT", "");
                prixGros = Double.parseDouble(prixGrosStr);
                
                String prixDetailStr = (String) this.model.getValueAt(modelRow, 4);
                prixDetailStr = prixDetailStr.replace(",", ".").replace(" DT", "");
                prixDetail = Double.parseDouble(prixDetailStr);
            } catch (Exception e) {}

            String qrDisplay = (qr != null && !qr.isEmpty() && !qr.equals("-")) ? " [QR: " + qr + "]" : "";
            this.lblStockInfo.setText(String.format(
                "<html><b>Article:</b> %s%s<br><b>Prix Gros:</b> %.3f DT | <b>Prix Détail:</b> %.3f DT | <b>Stock actuel:</b> %d</html>",
                designation, qrDisplay, prixGros, prixDetail, stock
            ));
        } else {
            this.lblStockInfo.setText("Sélectionnez un article pour voir les détails");
        }
    }

    private void chargerArticles() {
        this.model.setRowCount(0);
        String sql = "SELECT id, code, designation, prix_gros, prix_detail, stock, tva FROM Articles ORDER BY designation";

        try (Connection conn = ConnexionSQLite.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String codeQr = rs.getString("code");
                if(codeQr == null || codeQr.isEmpty()) codeQr = "-";

                this.model.addRow(new Object[]{
                    rs.getInt("id"),
                    codeQr,
                    rs.getString("designation"),
                    String.format("%.3f", rs.getDouble("prix_gros")),
                    String.format("%.3f", rs.getDouble("prix_detail")),
                    rs.getInt("stock"),
                    rs.getInt("tva")
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erreur lors du chargement: " + e.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void validerSortie() {
        int selectedRow = this.tableArticles.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Veuillez sélectionner un article", "Aucune sélection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String quantiteText = this.txtQuantite.getText().trim();
        try {
            int quantite = Integer.parseInt(quantiteText);
            if (quantite <= 0) {
                JOptionPane.showMessageDialog(this, "La quantité doit être supérieure à 0", "Quantité invalide", JOptionPane.WARNING_MESSAGE);
                return;
            }

            int modelRow = this.tableArticles.convertRowIndexToModel(selectedRow);
            int articleId = (Integer) this.model.getValueAt(modelRow, 0);
            String designation = (String) this.model.getValueAt(modelRow, 2);
            int stockActuel = (Integer) this.model.getValueAt(modelRow, 5);

            if (quantite > stockActuel) {
                JOptionPane.showMessageDialog(this,
                    "<html><div style='width: 250px;'><b>Stock insuffisant!</b><br><br>Stock actuel: <b>" + stockActuel + "</b><br>Demandé: <b>" + quantite + "</b><br><br><span style='color:red'>Manquant: " + (quantite - stockActuel) + "</span></div></html>",
                    "Stock insuffisant", JOptionPane.ERROR_MESSAGE);
                return;
            }

            Object[] options = {"Oui", "Non"};
            
            int confirm = JOptionPane.showOptionDialog(this,
                "<html><b>Confirmer la sortie de stock ?</b><br><br>" +
                "Article: <b>" + designation + "</b><br>" +
                "Retrait: <b style='color:red'>-" + quantite + "</b><br>" +
                "Nouveau Stock: <b>" + (stockActuel - quantite) + "</b></html>",
                "Confirmation",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]);

            if (confirm == 0) {
                this.enregistrerSortie(articleId, designation, quantite);
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Quantité invalide", "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Enregistre une sortie manuelle dans l'historique
     * Le champ numero_fact est défini à "Manuelle"
     */
    private void enregistrerSortie(int articleId, String designation, int quantite) {
        Connection conn = null;
        try {
            conn = ConnexionSQLite.getConnection();
            conn.setAutoCommit(false);
 
            // 1. Mettre à jour le stock
            String updateStock = "UPDATE Articles SET stock = stock - ? WHERE id = ?";
            try (PreparedStatement pst = conn.prepareStatement(updateStock)) {
                pst.setInt(1, quantite);
                pst.setInt(2, articleId);
                pst.executeUpdate();
            }
 
            // 2. Insérer dans Historique_Sorties avec la bonne structure
            //    Colonnes : article_id, designation, quantite, numero_fact
            String insertSortie = "INSERT INTO Historique_Sorties " +
                                  "(article_id, designation, quantite, numero_fact) " +
                                  "VALUES (?, ?, ?, ?)";
            try (PreparedStatement pst = conn.prepareStatement(insertSortie)) {
                pst.setInt(1, articleId);
                pst.setString(2, designation);
                pst.setInt(3, quantite);
                pst.setString(4, "Manuelle");
                pst.executeUpdate();
            }
 
            conn.commit();
 
            JOptionPane.showMessageDialog(this, "Sortie enregistrée avec succès !", "Succès",
                                          JOptionPane.INFORMATION_MESSAGE);
 
            this.txtQuantite.setText("1");
            this.chargerArticles();
 
            String dateStr = java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
            this.lblStatut.setText(getHtmlText("\uf058",
                " Succès : " + designation + " (-" + quantite + ") à " + dateStr + " - Manuelle"));
 
        } catch (SQLException e) {
            try { if (conn != null) conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erreur transaction: " + e.getMessage(),
                                          "Erreur", JOptionPane.ERROR_MESSAGE);
        } finally {
            try {
                if (conn != null) { conn.setAutoCommit(true); conn.close(); }
            } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                new GestionSortiesUI();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}