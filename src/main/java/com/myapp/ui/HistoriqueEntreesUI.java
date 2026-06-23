package com.myapp.ui;

import com.myapp.db.ConnexionSQLite;
import com.myapp.db.DatabaseManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

public class HistoriqueEntreesUI extends JFrame {

    private JTable tableEntrees;
    private DefaultTableModel model;
    private JLabel lblStatut;
    private JLabel[] lblStatsArray;
    private JTextField txtRecherche;
    private JFormattedTextField txtDateDebut;
    private JFormattedTextField txtDateFin;
    private Timer autoRefreshTimer;
    private Timer searchTimer;
    private boolean isDataLoaded = false;
    private boolean isLoading = false;

    public HistoriqueEntreesUI() {
        this.initializeUI();
    }

    private void initializeUI() {
        this.setTitle("Historique des Entrées de Stock - Système de Facturation");
        this.setExtendedState(JFrame.MAXIMIZED_BOTH);
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.setLocationRelativeTo(null);
        this.setLayout(new BorderLayout(10, 10));
        this.getContentPane().setBackground(new Color(240, 242, 245));

        this.initUI();
        this.setVisible(true);

        SwingUtilities.invokeLater(this::chargerHistoriqueEntrees);
        this.startAutoRefresh();
    }

    private void initUI() {
        JPanel headerPanel = this.createHeaderPanel();
        JPanel controlPanel = this.createControlPanel();
        JScrollPane scrollPane = this.createTablePanel();
        JPanel statsPanel = this.createStatsPanel();
        JPanel statusPanel = this.createStatusPanel();

        this.add(headerPanel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        centerPanel.setBackground(new Color(240, 242, 245));
        centerPanel.add(controlPanel, BorderLayout.NORTH);
        centerPanel.add(scrollPane, BorderLayout.CENTER);
        this.add(centerPanel, BorderLayout.CENTER);

        JPanel southPanel = new JPanel(new BorderLayout(10, 10));
        southPanel.setBorder(BorderFactory.createEmptyBorder(0, 15, 15, 15));
        southPanel.setBackground(new Color(240, 242, 245));
        southPanel.add(statsPanel, BorderLayout.NORTH);
        southPanel.add(statusPanel, BorderLayout.SOUTH);
        this.add(southPanel, BorderLayout.SOUTH);
    }

    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(41, 128, 185));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        JLabel titleLabel = new JLabel("HISTORIQUE DES ENTREES DE STOCK", JLabel.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        titleLabel.setForeground(Color.WHITE);

        JLabel subTitleLabel = new JLabel("Suivi complet des ajouts de stock", JLabel.CENTER);
        subTitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subTitleLabel.setForeground(new Color(220, 220, 220));
        subTitleLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));

        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(new Color(41, 128, 185));
        titlePanel.add(titleLabel, BorderLayout.CENTER);
        titlePanel.add(subTitleLabel, BorderLayout.SOUTH);

        final JButton btnRetour = new JButton("Retour au Stock");
        btnRetour.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnRetour.setBackground(new Color(52, 73, 94));
        btnRetour.setForeground(Color.WHITE);
        btnRetour.setFocusPainted(false);
        btnRetour.setBorderPainted(false);
        btnRetour.setOpaque(true);
        btnRetour.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        btnRetour.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        btnRetour.addActionListener(e -> this.dispose());

        btnRetour.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent evt) {
                btnRetour.setBackground(new Color(41, 128, 185));
            }
            @Override
            public void mouseExited(MouseEvent evt) {
                btnRetour.setBackground(new Color(52, 73, 94));
            }
        });

        headerPanel.add(titlePanel, BorderLayout.CENTER);
        headerPanel.add(btnRetour, BorderLayout.WEST);
        return headerPanel;
    }

    private JPanel createControlPanel() {
        JPanel controlPanel = new JPanel(new BorderLayout(10, 0));
        controlPanel.setBackground(Color.WHITE);
        controlPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1), 
                BorderFactory.createEmptyBorder(15, 20, 15, 20)));

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftPanel.setBackground(Color.WHITE);

        JLabel lblRecherche = new JLabel("Rechercher:");
        lblRecherche.setFont(new Font("Segoe UI", Font.BOLD, 14));

        this.txtRecherche = new JTextField(15);
        this.txtRecherche.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        this.txtRecherche.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(180, 180, 180)), 
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        
        this.txtRecherche.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { HistoriqueEntreesUI.this.startSearchTimer(); }
            @Override
            public void removeUpdate(DocumentEvent e) { HistoriqueEntreesUI.this.startSearchTimer(); }
            @Override
            public void changedUpdate(DocumentEvent e) { HistoriqueEntreesUI.this.startSearchTimer(); }
        });

        JLabel lblDateDebut = new JLabel("Date début:");
        lblDateDebut.setFont(new Font("Segoe UI", Font.BOLD, 14));

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        this.txtDateDebut = new JFormattedTextField(dateFormat);
        this.txtDateDebut.setColumns(10);
        this.txtDateDebut.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        this.txtDateDebut.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(180, 180, 180)), 
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        this.txtDateDebut.setValue(this.getFirstDayOfMonth());
        this.txtDateDebut.addPropertyChangeListener("value", evt -> this.startSearchTimer());

        JLabel lblDateFin = new JLabel("Date fin:");
        lblDateFin.setFont(new Font("Segoe UI", Font.BOLD, 14));

        this.txtDateFin = new JFormattedTextField(dateFormat);
        this.txtDateFin.setColumns(10);
        this.txtDateFin.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        this.txtDateFin.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(180, 180, 180)), 
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        this.txtDateFin.setValue(new Date());
        this.txtDateFin.addPropertyChangeListener("value", evt -> this.startSearchTimer());

        leftPanel.add(lblRecherche);
        leftPanel.add(this.txtRecherche);
        leftPanel.add(lblDateDebut);
        leftPanel.add(this.txtDateDebut);
        leftPanel.add(lblDateFin);
        leftPanel.add(this.txtDateFin);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightPanel.setBackground(Color.WHITE);

        JButton btnRafraichir = this.createActionButton("Rafraîchir", new Color(52, 152, 219));
        JButton btnVider = this.createActionButton("Vider Historique", new Color(231, 76, 60));

        btnRafraichir.addActionListener(e -> this.chargerHistoriqueEntrees());
        btnVider.addActionListener(e -> this.viderHistorique());

        rightPanel.add(btnRafraichir);
        rightPanel.add(btnVider);

        controlPanel.add(leftPanel, BorderLayout.WEST);
        controlPanel.add(rightPanel, BorderLayout.EAST);
        return controlPanel;
    }

    private Date getFirstDayOfMonth() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    private JScrollPane createTablePanel() {
        this.model = new DefaultTableModel(new String[]{"ID", "Désignation", "Quantité", "Date/Heure", "Utilisateur"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return Object.class;
            }
        };

        this.tableEntrees = new JTable(this.model);
        this.tableEntrees.setRowHeight(35);
        this.tableEntrees.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        this.tableEntrees.setSelectionBackground(new Color(220, 240, 255));
        this.tableEntrees.setSelectionForeground(Color.BLACK);
        this.tableEntrees.setGridColor(new Color(220, 220, 220));
        this.tableEntrees.setShowGrid(true);
        this.tableEntrees.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        this.tableEntrees.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                int row = HistoriqueEntreesUI.this.tableEntrees.rowAtPoint(evt.getPoint());
                int col = HistoriqueEntreesUI.this.tableEntrees.columnAtPoint(evt.getPoint());
                if (row >= 0 && col >= 0) {
                    HistoriqueEntreesUI.this.highlightRow(row);
                }
            }
        });

        JTableHeader header = this.tableEntrees.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 14));
        header.setBackground(new Color(41, 128, 185));
        header.setForeground(Color.WHITE);
        header.setReorderingAllowed(false);

        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                this.setBackground(new Color(41, 128, 185));
                this.setForeground(Color.WHITE);
                this.setFont(new Font("Segoe UI", Font.BOLD, 14));
                this.setHorizontalAlignment(JLabel.CENTER);
                this.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(30, 100, 160)), 
                        BorderFactory.createEmptyBorder(8, 5, 8, 5)));
                return this;
            }
        });

        this.tableEntrees.getColumnModel().getColumn(0).setPreferredWidth(60);
        this.tableEntrees.getColumnModel().getColumn(1).setPreferredWidth(300);
        this.tableEntrees.getColumnModel().getColumn(2).setPreferredWidth(100);
        this.tableEntrees.getColumnModel().getColumn(3).setPreferredWidth(150);
        this.tableEntrees.getColumnModel().getColumn(4).setPreferredWidth(120);

        JScrollPane scrollPane = new JScrollPane(this.tableEntrees);
        scrollPane.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1), 
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        scrollPane.getViewport().setBackground(Color.WHITE);
        return scrollPane;
    }

    private JPanel createStatsPanel() {
        JPanel statsPanel = new JPanel(new GridLayout(1, 3, 10, 0));
        statsPanel.setBackground(Color.WHITE);
        statsPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(41, 128, 185), 2), "Statistiques des Entrées"), 
                BorderFactory.createEmptyBorder(15, 20, 15, 20)));

        this.lblStatsArray = new JLabel[3];
        statsPanel.add(this.createStatCard("Total Entrées", "0", new Color(52, 152, 219), 0));
        statsPanel.add(this.createStatCard("Quantité Totale", "0 unités", new Color(46, 204, 113), 1));
        statsPanel.add(this.createStatCard("Dernière Entrée", "---", new Color(241, 196, 15), 2));
        return statsPanel;
    }

    private JPanel createStatCard(String title, String value, Color color, int index) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color, 2), 
                BorderFactory.createEmptyBorder(10, 15, 10, 15)));

        JLabel lblTitle = new JLabel(title, JLabel.CENTER);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblTitle.setForeground(new Color(100, 100, 100));

        this.lblStatsArray[index] = new JLabel(value, JLabel.CENTER);
        this.lblStatsArray[index].setFont(new Font("Segoe UI", Font.BOLD, 16));
        this.lblStatsArray[index].setForeground(color);

        card.add(lblTitle, BorderLayout.NORTH);
        card.add(this.lblStatsArray[index], BorderLayout.CENTER);
        return card;
    }

    private JPanel createStatusPanel() {
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBackground(new Color(240, 240, 240));
        statusPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        this.lblStatut = new JLabel("Prêt à charger l'historique...", JLabel.CENTER);
        this.lblStatut.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        this.lblStatut.setForeground(new Color(100, 100, 100));

        statusPanel.add(this.lblStatut, BorderLayout.CENTER);
        return statusPanel;
    }

    private JButton createActionButton(String text, final Color color) {
        final JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent evt) {
                button.setBackground(HistoriqueEntreesUI.this.darkenColor(color, 0.8F));
            }
            @Override
            public void mouseExited(MouseEvent evt) {
                button.setBackground(color);
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

    private void startSearchTimer() {
        if (this.searchTimer != null && this.searchTimer.isRunning()) {
            this.searchTimer.stop();
        }
        this.searchTimer = new Timer(500, e -> this.chargerHistoriqueEntrees());
        this.searchTimer.setRepeats(false);
        this.searchTimer.start();
    }

    private String buildQuery() {
        String recherche = this.txtRecherche.getText().trim();
        StringBuilder query = new StringBuilder();
        query.append("SELECT id, designation, quantite, date_entree, utilisateur ");
        query.append("FROM Historique_Entrees ");

        List<String> conditions = new ArrayList<>();
        if (!recherche.isEmpty()) {
            conditions.add("(designation LIKE ? OR utilisateur LIKE ?)");
        }
        conditions.add("date_entree BETWEEN ? AND ?");

        if (!conditions.isEmpty()) {
            query.append("WHERE ");
            for (int i = 0; i < conditions.size(); ++i) {
                if (i > 0) {
                    query.append(" AND ");
                }
                query.append(conditions.get(i));
            }
        }
        query.append(" ORDER BY date_entree DESC");
        return query.toString();
    }

    private void setQueryParameters(PreparedStatement pstmt) throws SQLException {
        String recherche = this.txtRecherche.getText().trim();
        SimpleDateFormat dbDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        int paramIndex = 1;

        if (!recherche.isEmpty()) {
            String searchPattern = "%" + recherche + "%";
            pstmt.setString(paramIndex++, searchPattern);
            pstmt.setString(paramIndex++, searchPattern);
        }

        Date dateDebut = (Date) this.txtDateDebut.getValue();
        Date dateFin = (Date) this.txtDateFin.getValue();

        if (dateDebut != null && dateFin != null) {
            String debutStr = dbDateFormat.format(dateDebut) + " 00:00:00";
            String finStr = dbDateFormat.format(dateFin) + " 23:59:59";
            pstmt.setString(paramIndex++, debutStr);
            pstmt.setString(paramIndex, finStr);
        }
    }

    private void chargerHistoriqueEntrees() {
        if (this.isLoading) {
            System.out.println("Chargement déjà en cours, ignoré...");
            return;
        }

        this.isLoading = true;
        Date dateDebut = (Date) this.txtDateDebut.getValue();
        Date dateFin = (Date) this.txtDateFin.getValue();

        if (dateDebut != null && dateFin != null) {
            if (dateDebut.after(dateFin)) {
                JOptionPane.showMessageDialog(this, "La date de début doit être antérieure à la date de fin", "Erreur de date", JOptionPane.WARNING_MESSAGE);
                this.isLoading = false;
                return;
            }

            // Reset UI
            SwingUtilities.invokeLater(() -> {
                while (this.model.getRowCount() > 0) {
                    this.model.removeRow(0);
                }
            });

            String recherche = this.txtRecherche.getText().trim();
            SimpleDateFormat displayFormat = new SimpleDateFormat("dd/MM/yyyy");
            String statutMessage = "Chargement de l'historique...";
            if (!recherche.isEmpty()) {
                statutMessage = statutMessage + " (Recherche: '" + recherche + "')";
            }
            statutMessage = statutMessage + " [Période: " + displayFormat.format(dateDebut) + " à " + displayFormat.format(dateFin) + "]";
            
            this.lblStatut.setText(statutMessage);
            this.lblStatut.setForeground(new Color(52, 152, 219));
            this.lblStatut.repaint();

            // SwingWorker pour le chargement en arrière-plan
            SwingWorker<Void, Object[]> worker = new SwingWorker<Void, Object[]>() {
                private int count = 0;
                private int totalQuantite = 0;
                private String derniereEntree = "---";

                @Override
                protected Void doInBackground() throws Exception {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

                    try (Connection conn = ConnexionSQLite.getConnection();
                         PreparedStatement pstmt = conn.prepareStatement(HistoriqueEntreesUI.this.buildQuery())) {
                        
                        System.out.println("Requête SQL: " + pstmt.toString());
                        HistoriqueEntreesUI.this.setQueryParameters(pstmt);

                        try (ResultSet rs = pstmt.executeQuery()) {
                            List<Object[]> buffer = new ArrayList<>();

                            while (rs.next()) {
                                Timestamp ts = rs.getTimestamp("date_entree");
                                String dateFormatee = (ts != null) ? dateFormat.format(ts) : "---";
                                int quantite = rs.getInt("quantite");

                                Object[] rowData = new Object[]{
                                    rs.getInt("id"),
                                    rs.getString("designation"),
                                    quantite,
                                    dateFormatee,
                                    rs.getString("utilisateur")
                                };
                                buffer.add(rowData);

                                count++;
                                totalQuantite += quantite;
                                if (derniereEntree.equals("---")) {
                                    derniereEntree = dateFormatee;
                                }

                                // Mise à jour par lots pour ne pas figer l'UI
                                if (buffer.size() >= 10) {
                                    publish(buffer.toArray(new Object[0][]));
                                    buffer.clear();
                                    Thread.sleep(10); // Petit délai pour fluidité
                                }
                            }

                            // Publier le reste
                            if (!buffer.isEmpty()) {
                                publish(buffer.toArray(new Object[0][]));
                            }
                        }
                    } catch (SQLException e) {
                        System.err.println("Erreur SQL: " + e.getMessage());
                        e.printStackTrace();
                        throw e;
                    }
                    return null;
                }

                @Override
                protected void process(List<Object[]> chunks) {
                    for (Object[] rowData : chunks) {
                        if (rowData != null && rowData.length > 0) {
                            HistoriqueEntreesUI.this.model.addRow(rowData);
                        }
                    }
                    HistoriqueEntreesUI.this.tableEntrees.revalidate();
                    HistoriqueEntreesUI.this.tableEntrees.repaint();
                }

                @Override
                protected void done() {
                    try {
                        get(); // Vérifie les exceptions
                        HistoriqueEntreesUI.this.mettreAJourStatistiques(count, totalQuantite, derniereEntree);

                        String recherche = HistoriqueEntreesUI.this.txtRecherche.getText().trim();
                        SimpleDateFormat displayFormat = new SimpleDateFormat("dd/MM/yyyy");
                        Date dateDebut = (Date) HistoriqueEntreesUI.this.txtDateDebut.getValue();
                        Date dateFin = (Date) HistoriqueEntreesUI.this.txtDateFin.getValue();

                        String statutFinal = "✓ " + count + " entrées trouvées";
                        if (!recherche.isEmpty()) {
                            statutFinal = statutFinal + " (Recherche: '" + recherche + "')";
                        }
                        if (dateDebut != null && dateFin != null) {
                            statutFinal = statutFinal + " [Période: " + displayFormat.format(dateDebut) + " à " + displayFormat.format(dateFin) + "]";
                        }
                        statutFinal = statutFinal + " - Total: " + totalQuantite + " unités";

                        HistoriqueEntreesUI.this.lblStatut.setText(statutFinal);
                        HistoriqueEntreesUI.this.lblStatut.setForeground(new Color(46, 204, 113));
                        HistoriqueEntreesUI.this.isDataLoaded = true;

                        SwingUtilities.invokeLater(() -> {
                            HistoriqueEntreesUI.this.tableEntrees.revalidate();
                            HistoriqueEntreesUI.this.tableEntrees.repaint();
                            if (HistoriqueEntreesUI.this.model.getRowCount() > 0) {
                                JViewport viewport = (JViewport) HistoriqueEntreesUI.this.tableEntrees.getParent();
                                if (viewport != null) {
                                    viewport.setViewPosition(new Point(0, 0));
                                }
                            }
                        });

                    } catch (Exception e) {
                        e.printStackTrace();
                        HistoriqueEntreesUI.this.lblStatut.setText("✗ Erreur lors du chargement de l'historique: " + e.getMessage());
                        HistoriqueEntreesUI.this.lblStatut.setForeground(new Color(231, 76, 60));
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(HistoriqueEntreesUI.this, "Erreur de chargement : " + e.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
                        });
                    } finally {
                        HistoriqueEntreesUI.this.isLoading = false;
                    }
                }
            };
            worker.execute();

        } else {
            JOptionPane.showMessageDialog(this, "Veuillez sélectionner des dates valides", "Erreur de date", JOptionPane.WARNING_MESSAGE);
            this.isLoading = false;
        }
    }

    private void mettreAJourStatistiques(int totalEntrees, int totalQuantite, String derniereEntree) {
        SwingUtilities.invokeLater(() -> {
            if (this.lblStatsArray != null && this.lblStatsArray.length >= 3) {
                this.lblStatsArray[0].setText(String.valueOf(totalEntrees));
                this.lblStatsArray[1].setText(totalQuantite + " unités");
                this.lblStatsArray[2].setText(derniereEntree != null ? derniereEntree : "---");

                for (JLabel label : this.lblStatsArray) {
                    if (label != null) {
                        label.revalidate();
                        label.repaint();
                    }
                }
            }
        });
    }

    private void startAutoRefresh() {
        if (this.autoRefreshTimer != null && this.autoRefreshTimer.isRunning()) {
            this.autoRefreshTimer.stop();
        }
        this.autoRefreshTimer = new Timer(30000, e -> {
            if (this.isVisible() && this.isActive() && !this.isLoading) {
                this.isDataLoaded = false;
                this.chargerHistoriqueEntrees();
            }
        });
        this.autoRefreshTimer.setInitialDelay(30000);
        this.autoRefreshTimer.start();
    }

    private void highlightRow(int row) {
        if (this.tableEntrees != null && row >= 0 && row < this.tableEntrees.getRowCount()) {
            this.tableEntrees.setRowSelectionInterval(row, row);
            this.tableEntrees.scrollRectToVisible(this.tableEntrees.getCellRect(row, 0, true));
            
            Timer highlightTimer = new Timer(800, e -> this.tableEntrees.clearSelection());
            highlightTimer.setRepeats(false);
            highlightTimer.start();
        }
    }

    /**
     * Vide définitivement l'historique des entrées (suppression physique en base de données)
     */
    private void viderHistorique() {
        Object[] options = {"Oui", "Non"};
        int confirm = JOptionPane.showOptionDialog(this,
                "<html><body style='text-align:center;'>" +
                "<h2>⚠️ Confirmation de suppression définitive ⚠️</h2>" +
                "<p style='font-size:14px;'>Êtes-vous sûr de vouloir vider TOUT l'historique des entrées ?</p>" +
                "<p style='font-size:12px;color:red;'><b>Cette action est IRRÉVERSIBLE !</b></p>" +
                "<p style='font-size:11px;'>Toutes les données d'historique seront définitivement supprimées de la base de données.</p>" +
                "</body></html>",
                "Vider l'historique - Confirmation",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null, options, options[1]);

        if (confirm == 0) {
            // Deuxième confirmation pour éviter les erreurs
            Object[] options2 = {"Oui", "Non"};
            int confirm2 = JOptionPane.showOptionDialog(this,
                    "⚠️ DERNIER AVERTISSEMENT ⚠️\n\n" +
                    "Cette action supprimera DÉFINITIVEMENT toutes les lignes d'historique.\n\n" +
                    "Voulez-vous vraiment continuer ?",
                    "Confirmation finale",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    null, options2, options2[1]);

            if (confirm2 == 0) {
                SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        // Appel à la méthode du DatabaseManager pour la suppression définitive
                        boolean success = DatabaseManager.viderHistoriqueEntrees();
                        if (!success) {
                            throw new SQLException("Échec de la suppression dans la base de données");
                        }
                        return null;
                    }

                    @Override
                    protected void done() {
                        try {
                            get(); // Vérifie les erreurs
                            HistoriqueEntreesUI.this.isDataLoaded = false;
                            HistoriqueEntreesUI.this.chargerHistoriqueEntrees();
                            
                            // Message de succès avec compteur
                            JOptionPane.showMessageDialog(HistoriqueEntreesUI.this, 
                                "✅ Historique des entrées vidé avec succès !\n\n" +
                                "Toutes les données ont été définitivement supprimées de la base de données.",
                                "Succès", 
                                JOptionPane.INFORMATION_MESSAGE);
                                
                        } catch (Exception e) {
                            e.printStackTrace();
                            JOptionPane.showMessageDialog(HistoriqueEntreesUI.this, 
                                "❌ Erreur lors du vidage de l'historique:\n" + e.getMessage(), 
                                "Erreur", 
                                JOptionPane.ERROR_MESSAGE);
                        }
                    }
                };
                worker.execute();
            }
        }
    }

    @Override
    public void dispose() {
        if (this.autoRefreshTimer != null && this.autoRefreshTimer.isRunning()) {
            this.autoRefreshTimer.stop();
        }
        if (this.searchTimer != null && this.searchTimer.isRunning()) {
            this.searchTimer.stop();
        }
        super.dispose();
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible && !this.isDataLoaded && !this.isLoading) {
            SwingUtilities.invokeLater(this::chargerHistoriqueEntrees);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                System.setProperty("awt.useSystemAAFontSettings", "on");
                System.setProperty("swing.aatext", "true");
                new HistoriqueEntreesUI();
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Erreur lors du démarrage de l'application: " + e.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}