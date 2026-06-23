package com.myapp.ui;

import com.myapp.db.ConnexionSQLite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;

public class GestionStock extends JFrame {
   private JTable tableArticles;
   private DefaultTableModel model;
   private JButton btnAjouter;
   private JButton btnModifier;
   private JButton btnSupprimer;
   private JButton btnGestionEntrees;
   private JButton btnGestionSorties;
   private JButton btnHistoriqueEntrees;
   private JButton btnHistoriqueSorties;
   private JButton btnResetFiltre;
   private JTextField txtRecherche;
   private TableRowSorter<DefaultTableModel> sorter;
   
   private Font fontAwesomeSolid;
   
   private int totalArticles = 0;
   private int alertesRupture = 0;
   private int alertesFaible = 0;
   private int stockNormal = 0;

   public GestionStock() {
      this.loadFontAwesome();
      this.setTitle("Gestion du Stock - Articles");
      this.setExtendedState(JFrame.MAXIMIZED_BOTH);
      this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      this.setLocationRelativeTo(null);
      this.setLayout(new BorderLayout(10, 10));
      this.initUI();
      this.chargerArticles();
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
      return this.fontAwesomeSolid != null ? this.fontAwesomeSolid.deriveFont(Font.PLAIN, (float)size) : new Font("SansSerif", Font.PLAIN, size);
   }

   private String getHtmlText(String iconCode, String text) {
      String fontName = (this.fontAwesomeSolid != null) ? this.fontAwesomeSolid.getFontName() : "SansSerif";
      return "<html><font face=\"" + fontName + "\">" + iconCode + "</font> " + text + "</html>";
   }

   private JButton createStyledButton(String icon, String text, final Color backgroundColor) {
      final JButton button = new JButton(this.getHtmlText(icon, text));
      button.setFont(new Font("Segoe UI", Font.BOLD, 13));
      button.setBackground(backgroundColor);
      button.setForeground(Color.WHITE);
      button.setFocusPainted(false);
      button.setBorderPainted(false);
      button.setOpaque(true);
      button.setCursor(new Cursor(Cursor.HAND_CURSOR));
      button.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
      button.addMouseListener(new MouseAdapter() {
         public void mouseEntered(MouseEvent evt) { button.setBackground(GestionStock.this.darkenColor(backgroundColor, 0.9F)); }
         public void mouseExited(MouseEvent evt) { button.setBackground(backgroundColor); }
      });
      return button;
   }

   private JButton createNavButtonWithMenu(String icon, String text, final boolean isActive, final String[] menuItems, final String[] menuIcons) {
      String fontName = (this.fontAwesomeSolid != null) ? this.fontAwesomeSolid.getFontName() : "SansSerif";
      String chevron = "\uf078";
      String htmlContent = "<html><font face=\"" + fontName + "\">" + icon + "</font> " + text + " <font face=\"" + fontName + "\" size=\"3\">" + chevron + "</font></html>";
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
      final Timer hoverTimer = new Timer(300, (e) -> { this.showMenuForButton(button, menuItems, menuIcons); });
      hoverTimer.setRepeats(false);
      button.addMouseListener(new MouseAdapter() {
         public void mouseEntered(MouseEvent evt) { if (!isActive) { button.setBackground(new Color(220, 220, 220)); } hoverTimer.start(); }
         public void mouseExited(MouseEvent evt) { if (!isActive) { button.setBackground(new Color(236, 240, 241)); } hoverTimer.stop(); }
         public void mouseClicked(MouseEvent evt) { GestionStock.this.showMenuForButton(button, menuItems, menuIcons); }
      });
      return button;
   }

   private void showMenuForButton(JButton button, String[] menuItems, String[] menuIcons) {
      JPopupMenu menu = new JPopupMenu();
      menu.setBackground(Color.WHITE);
      menu.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
      for(int i = 0; i < menuItems.length; ++i) {
         String htmlItem = this.getHtmlText(menuIcons[i], menuItems[i]);
         JMenuItem menuItem = new JMenuItem(htmlItem);
         menuItem.setFont(new Font("Segoe UI", Font.PLAIN, 13));
         String item = menuItems[i];
         menuItem.addActionListener((e) -> { this.handleMenuAction(item); });
         menu.add(menuItem);
         if (i < menuItems.length - 1) { menu.addSeparator(); }
      }
      menu.show(button, 0, button.getHeight());
   }

   private void handleMenuAction(String menuItem) {
      switch(menuItem) {
         case "Nouveau Client": this.ouvrirNouveauClient(); break;
         case "Historique Entrée": this.ouvrirHistoriqueEntrees(); break;
         case "Historique Sortie": this.ouvrirHistoriqueSorties(); break;
         case "Liste Clients": this.ouvrirListeClients(); break;
         case "Liste Factures": this.ouvrirListeFactures(); break;
         case "Gestion Entrée": this.ouvrirGestionEntrees(); break;
         case "Gestion Sortie": this.ouvrirGestionSorties(); break;
         case "Devis": this.ouvrirDevis(); break;
         case "Bon de Livraison": this.ouvrirBonLivraison(); break;
         case "Facture": this.ouvrirFacture(); break;
         case "Bon de Sortie": this.ouvrirBonSortie(); break;
         case "Consulter Stock":
            try {
               (new GestionStock()).setVisible(true);
               this.dispose();
            } catch (Exception var5) {
               JOptionPane.showMessageDialog(this, "Erreur: " + var5.getMessage());
            }
            break;
      }
   }

   private void ouvrirListeClients() { (new ListeClients()).setVisible(true); this.dispose(); }
   private void ouvrirNouveauClient() { (new AjouterClient(null)).setVisible(true); this.dispose(); }
   private void ouvrirListeFactures() { (new ListeFactures()).setVisible(true); this.dispose(); }
   private void ouvrirFacture() { (new FactureUI()).setVisible(true); this.dispose(); }
   private void ouvrirBonLivraison() { (new BLUI()).setVisible(true); this.dispose(); }
   private void ouvrirBonSortie() { (new BonSortieUI()).setVisible(true); this.dispose(); }
   private void ouvrirDevis() { (new DevisUI()).setVisible(true); this.dispose(); }
   private void ouvrirGestionEntrees() { (new GestionEntreesUI()).setVisible(true); }
   private void ouvrirGestionSorties() { (new GestionSortiesUI()).setVisible(true); }
   private void ouvrirHistoriqueEntrees() { (new HistoriqueEntreesUI()).setVisible(true); }
   private void ouvrirHistoriqueSorties() { (new HistoriqueSortiesUI()).setVisible(true); }

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
         new AdminDashboard().setVisible(true);
         this.dispose();
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

   private JPanel createStatCard(String icon, String title, String value, Color bgColor, Color iconColor, String subtitle) {
      JPanel card = new JPanel(new BorderLayout(10, 10));
      card.setBackground(bgColor);
      card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(this.darkenColor(bgColor, 0.8F), 1),
            BorderFactory.createEmptyBorder(15, 20, 15, 20)));
      card.setPreferredSize(new Dimension(250, 120));

      JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
      topPanel.setBackground(bgColor);
      JLabel iconLabel = new JLabel(icon);
      iconLabel.setFont(this.getFontAwesome(20));
      iconLabel.setForeground(iconColor);
      topPanel.add(iconLabel);
      JLabel titleLabel = new JLabel(" " + title);
      titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
      titleLabel.setForeground(this.darkenColor(bgColor, 0.3F));
      topPanel.add(titleLabel);

      JLabel valueLabel = new JLabel(value, JLabel.CENTER);
      valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
      valueLabel.setForeground(this.darkenColor(bgColor, 0.2F));
      JLabel subtitleLabel = new JLabel(subtitle, JLabel.CENTER);
      subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
      subtitleLabel.setForeground(this.darkenColor(bgColor, 0.4F));
      subtitleLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));

      JPanel centerPanel = new JPanel(new BorderLayout());
      centerPanel.setBackground(bgColor);
      centerPanel.add(valueLabel, BorderLayout.CENTER);
      centerPanel.add(subtitleLabel, BorderLayout.SOUTH);

      card.add(topPanel, BorderLayout.NORTH);
      card.add(centerPanel, BorderLayout.CENTER);
      return card;
   }

   private void rendreCarteCliquable(JPanel carte, int typeFiltre) {
      carte.setCursor(new Cursor(Cursor.HAND_CURSOR));
      carte.addMouseListener(new MouseAdapter() {
         @Override
         public void mouseClicked(MouseEvent e) {
            appliquerFiltreParStock(typeFiltre);
         }
      });
   }

   private void appliquerFiltreParStock(int typeFiltre) {
      txtRecherche.setText("");
      switch (typeFiltre) {
         case 0:
            sorter.setRowFilter(new RowFilter<DefaultTableModel, Integer>() {
               public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                  try {
                     Object stockObj = entry.getValue(6);
                     if (stockObj == null) return false;
                     int stock = Integer.parseInt(stockObj.toString().replaceAll("[^0-9]", ""));
                     return stock == 0;
                  } catch (Exception e) { return false; }
               }
            });
            txtRecherche.setText("[Filtre: Articles en rupture]");
            break;
         case 1:
            sorter.setRowFilter(new RowFilter<DefaultTableModel, Integer>() {
               public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                  try {
                     Object stockObj = entry.getValue(6);
                     if (stockObj == null) return false;
                     int stock = Integer.parseInt(stockObj.toString().replaceAll("[^0-9]", ""));
                     return stock > 0 && stock < 10;
                  } catch (Exception e) { return false; }
               }
            });
            txtRecherche.setText("[Filtre: Stocks faibles (<10)]");
            break;
         case 2:
            sorter.setRowFilter(new RowFilter<DefaultTableModel, Integer>() {
               public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                  try {
                     Object stockObj = entry.getValue(6);
                     if (stockObj == null) return false;
                     int stock = Integer.parseInt(stockObj.toString().replaceAll("[^0-9]", ""));
                     return stock >= 10;
                  } catch (Exception e) { return false; }
               }
            });
            txtRecherche.setText("[Filtre: Stocks normaux (≥10)]");
            break;
      }
   }

   private JPanel createDashboardPanel() {
      JPanel dashboardPanel = new JPanel(new GridBagLayout());
      dashboardPanel.setBackground(Color.WHITE);
      dashboardPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(10, 10, 10, 10),
            BorderFactory.createTitledBorder(
                  BorderFactory.createLineBorder(new Color(220, 220, 220)),
                  " TABLEAU DE BORD DU STOCK", 1, 2,
                  new Font("Segoe UI", Font.BOLD, 12), new Color(52, 73, 94))));

      GridBagConstraints gbc = new GridBagConstraints();
      gbc.fill = GridBagConstraints.BOTH;
      gbc.weightx = 1.0D;
      gbc.weighty = 1.0D;
      gbc.insets = new Insets(5, 5, 5, 5);

      this.calculerStatistiques();

      JPanel card1 = this.createStatCard("\uf1b3", "TOTAL ARTICLES", String.valueOf(this.totalArticles), new Color(52, 152, 219, 40), new Color(41, 128, 185), "Articles en stock");
      JPanel card2 = this.createStatCard("\uf071", "RUPTURES DE STOCK", String.valueOf(this.alertesRupture), new Color(231, 76, 60, 40), new Color(192, 57, 43), "Articles à réapprovisionner");
      JPanel card3 = this.createStatCard("\uf06a", "STOCKS FAIBLES", String.valueOf(this.alertesFaible), new Color(230, 126, 34, 40), new Color(211, 84, 0), "Stock < 10");
      JPanel card4 = this.createStatCard("\uf058", "STOCK NORMAL", String.valueOf(this.stockNormal), new Color(46, 204, 113, 40), new Color(39, 174, 96), "Stock suffisant");

      this.rendreCarteCliquable(card2, 0);
      this.rendreCarteCliquable(card3, 1);
      this.rendreCarteCliquable(card4, 2);

      gbc.gridx = 0; gbc.gridy = 0; dashboardPanel.add(card1, gbc);
      gbc.gridx = 1; dashboardPanel.add(card2, gbc);
      gbc.gridx = 2; dashboardPanel.add(card3, gbc);
      gbc.gridx = 3; dashboardPanel.add(card4, gbc);

      return dashboardPanel;
   }

   private void calculerStatistiques() {
      this.totalArticles = 0;
      this.alertesRupture = 0;
      this.alertesFaible = 0;
      this.stockNormal = 0;
      try (Connection conn = ConnexionSQLite.getConnection();
           Statement stmt = conn.createStatement();
           ResultSet rs = stmt.executeQuery("SELECT stock FROM Articles")) {
         while(rs.next()) {
            int stock = rs.getInt("stock");
            ++this.totalArticles;
            if (stock == 0) { ++this.alertesRupture; }
            else if (stock < 10) { ++this.alertesFaible; }
            else { ++this.stockNormal; }
         }
      } catch (SQLException e) { e.printStackTrace(); }
   }

   private void initUI() {
      JPanel navBarPanel = this.createNavigationBar();

      JPanel headerPanel = new JPanel(new BorderLayout());
      headerPanel.setBackground(new Color(44, 62, 80));
      headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
      JLabel titleLabel = new JLabel(this.getHtmlText("\uf466", "GESTION DU STOCK"), JLabel.CENTER);
      titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
      titleLabel.setForeground(Color.WHITE);
      headerPanel.add(titleLabel, BorderLayout.CENTER);

      JPanel dashboardPanel = this.createDashboardPanel();

      JPanel mouvementsPanel = new JPanel(new GridLayout(2, 2, 10, 10));
      mouvementsPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("Actions Rapides"),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)));
      mouvementsPanel.setBackground(new Color(240, 240, 240));
      this.btnGestionEntrees   = this.createStyledButton("\uf090", "Gérer les Entrées",   new Color(46, 204, 113));
      this.btnGestionSorties   = this.createStyledButton("\uf08b", "Gérer les Sorties",   new Color(52, 152, 219));
      this.btnHistoriqueEntrees = this.createStyledButton("\uf1da", "Historique Entrées", new Color(155, 89, 182));
      this.btnHistoriqueSorties = this.createStyledButton("\uf201", "Historique Sorties", new Color(231, 76, 60));
      mouvementsPanel.add(this.btnGestionEntrees);
      mouvementsPanel.add(this.btnGestionSorties);
      mouvementsPanel.add(this.btnHistoriqueEntrees);
      mouvementsPanel.add(this.btnHistoriqueSorties);

      JPanel searchPanel = new JPanel(new BorderLayout(10, 0));
      searchPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
      searchPanel.setBackground(Color.WHITE);

      JLabel lblRecherche = new JLabel(this.getHtmlText("\uf002", "Rechercher (Code ou Nom):"));
      lblRecherche.setForeground(Color.BLACK);

      this.txtRecherche = new JTextField();
      this.txtRecherche.setFont(new Font("Segoe UI", Font.PLAIN, 13));
      this.txtRecherche.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            BorderFactory.createEmptyBorder(5, 8, 5, 8)));
      this.txtRecherche.addKeyListener(new KeyAdapter() {
         public void keyReleased(KeyEvent e) {
            String text = GestionStock.this.txtRecherche.getText();
            if (text.startsWith("[Filtre:")) return;
            if (text.trim().length() == 0) {
               GestionStock.this.sorter.setRowFilter(null);
            } else {
               GestionStock.this.sorter.setRowFilter(RowFilter.regexFilter("(?i)" + text, new int[]{1, 2}));
            }
         }
      });

      this.btnResetFiltre = this.createStyledButton("\uf0e2", "Réinitialiser", new Color(155, 89, 182));
      this.btnResetFiltre.setPreferredSize(new Dimension(140, 35));
      this.btnResetFiltre.addActionListener(e -> {
         sorter.setRowFilter(null);
         txtRecherche.setText("");
      });

      JPanel searchAndResetPanel = new JPanel(new BorderLayout(10, 0));
      searchAndResetPanel.add(lblRecherche, BorderLayout.WEST);
      searchAndResetPanel.add(txtRecherche, BorderLayout.CENTER);
      searchAndResetPanel.add(btnResetFiltre, BorderLayout.EAST);

      // Modèle de table
      this.model = new DefaultTableModel(
            new String[]{"ID", "Code Barre", "Désignation", "Prix Gros (DT)", "Prix Détail (DT)", "TVA (%)", "Stock"}, 0) {
         public boolean isCellEditable(int row, int column) { return false; }
         public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == 1 || columnIndex == 2) return String.class;
            if (columnIndex == 3 || columnIndex == 4) return Double.class;
            return Integer.class;
         }
      };

      this.tableArticles = new JTable(this.model);
      this.sorter = new TableRowSorter<>(this.model);
      this.tableArticles.setRowSorter(this.sorter);
      this.tableArticles.setRowHeight(35);
      this.tableArticles.setFont(new Font("Segoe UI", Font.PLAIN, 13));
      this.tableArticles.setGridColor(new Color(240, 240, 240));
      this.tableArticles.setSelectionBackground(new Color(51, 153, 255));
      this.tableArticles.setSelectionForeground(Color.BLACK);

      this.tableArticles.getColumnModel().getColumn(0).setPreferredWidth(50);
      this.tableArticles.getColumnModel().getColumn(1).setPreferredWidth(120);
      this.tableArticles.getColumnModel().getColumn(2).setPreferredWidth(250);
      this.tableArticles.getColumnModel().getColumn(3).setPreferredWidth(120);
      this.tableArticles.getColumnModel().getColumn(4).setPreferredWidth(120);
      this.tableArticles.getColumnModel().getColumn(5).setPreferredWidth(70);
      this.tableArticles.getColumnModel().getColumn(6).setPreferredWidth(80);

      // Header
      JTableHeader header = this.tableArticles.getTableHeader();
      header.setDefaultRenderer(new DefaultTableCellRenderer() {
         public Component getTableCellRendererComponent(JTable table, Object value,
               boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            this.setBackground(new Color(52, 73, 94));
            this.setForeground(Color.WHITE);
            this.setFont(new Font("Segoe UI", Font.BOLD, 13));
            this.setHorizontalAlignment(JLabel.CENTER);
            this.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            return this;
         }
      });

      // ── RENDERER PRINCIPAL AVEC SÉLECTION CLAIRE ──────────────────────
      this.tableArticles.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
         public Component getTableCellRendererComponent(JTable table, Object value,
               boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            int stockValue = getStockValueForRow(row);

            if (isSelected) {
               // Sélection : bleu avec écriture NOIRE (lisible)
               setBackground(new Color(51, 153, 255));
               setForeground(Color.BLACK);
            } else if (stockValue == 0) {
               // Rupture de stock : fond rouge clair
               setBackground(new Color(255, 200, 200));
               setForeground(Color.RED);
            } else if (stockValue > 0 && stockValue < 10) {
               // Stock faible : fond jaune
               setBackground(new Color(255, 255, 150));
               setForeground(new Color(180, 120, 0));
            } else {
               // Stock normal : lignes alternées
               setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 248, 248));
               setForeground(Color.BLACK);
            }

            // Alignement selon la colonne
            if (column == 0 || column == 5 || column == 6) {
               setHorizontalAlignment(JLabel.CENTER);
            } else if (column == 1 || column == 2) {
               setHorizontalAlignment(JLabel.LEFT);
            } else {
               setHorizontalAlignment(JLabel.RIGHT);
               if (!isSelected && value instanceof Double) {
                  setText(String.format("%.3f", (Double) value).replace(".", ","));
               }
            }

            return this;
         }

         private int getStockValueForRow(int row) {
            try {
               int modelRow = GestionStock.this.tableArticles.convertRowIndexToModel(row);
               Object stockObj = GestionStock.this.model.getValueAt(modelRow, 6);
               if (stockObj == null) return -1;
               if (stockObj instanceof Integer) return (Integer) stockObj;
               return Integer.parseInt(stockObj.toString().replaceAll("[^0-9]", ""));
            } catch (Exception e) { return -1; }
         }
      });
      // ─────────────────────────────────────────────────────────────────────

      JScrollPane scrollPane = new JScrollPane(this.tableArticles);
      scrollPane.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)), "Liste des Articles"));

      JPanel articlesButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 15));
      articlesButtonPanel.setBackground(new Color(250, 250, 250));
      articlesButtonPanel.setBorder(BorderFactory.createTitledBorder("Gestion des Articles"));
      this.btnAjouter  = this.createStyledButton("\uf067", "Ajouter Article",   new Color(46, 204, 113));
      this.btnModifier = this.createStyledButton("\uf044", "Modifier Article",  new Color(52, 152, 219));
      this.btnSupprimer = this.createStyledButton("\uf1f8", "Supprimer Article", new Color(231, 76, 60));
      articlesButtonPanel.add(this.btnAjouter);
      articlesButtonPanel.add(this.btnModifier);
      articlesButtonPanel.add(this.btnSupprimer);

      // Assemblage
      this.add(navBarPanel, BorderLayout.NORTH);
      JPanel mainContentPanel = new JPanel(new BorderLayout());
      mainContentPanel.add(headerPanel, BorderLayout.NORTH);
      JPanel topPanel = new JPanel(new BorderLayout());
      topPanel.add(dashboardPanel, BorderLayout.NORTH);
      JPanel middlePanel = new JPanel(new BorderLayout());
      middlePanel.add(mouvementsPanel, BorderLayout.NORTH);
      middlePanel.add(searchAndResetPanel, BorderLayout.SOUTH);
      topPanel.add(middlePanel, BorderLayout.CENTER);
      mainContentPanel.add(topPanel, BorderLayout.NORTH);
      mainContentPanel.add(scrollPane, BorderLayout.CENTER);
      mainContentPanel.add(articlesButtonPanel, BorderLayout.SOUTH);
      this.add(mainContentPanel, BorderLayout.CENTER);

      // Listeners
      this.btnAjouter.addActionListener((e) -> this.ouvrirFormulaireAjout());
      this.btnModifier.addActionListener((e) -> this.modifierArticle());
      this.btnSupprimer.addActionListener((e) -> this.supprimerArticle());
      this.btnGestionEntrees.addActionListener((e) -> this.ouvrirGestionEntrees());
      this.btnGestionSorties.addActionListener((e) -> this.ouvrirGestionSorties());
      this.btnHistoriqueEntrees.addActionListener((e) -> this.ouvrirHistoriqueEntrees());
      this.btnHistoriqueSorties.addActionListener((e) -> this.ouvrirHistoriqueSorties());
   }

   private void ouvrirFormulaireAjout() {
      try {
         AjoutArticle ajoutArticlePage = new AjoutArticle();
         ajoutArticlePage.setVisible(true);
         this.dispose();
      } catch (Exception e) {
         JOptionPane.showMessageDialog(this, "Erreur: " + e.getMessage());
      }
   }

   private Color darkenColor(Color color, float factor) {
      int r = Math.max((int)((float)color.getRed()   * factor), 0);
      int g = Math.max((int)((float)color.getGreen() * factor), 0);
      int b = Math.max((int)((float)color.getBlue()  * factor), 0);
      return new Color(r, g, b);
   }

   private void chargerArticles() {
      this.model.setRowCount(0);
      try (Connection conn = ConnexionSQLite.getConnection();
           Statement stmt = conn.createStatement();
           ResultSet rs = stmt.executeQuery(
                 "SELECT id, code, designation, prix_gros, prix_detail, tva, stock " +
                 "FROM Articles ORDER BY designation")) {
         while(rs.next()) {
            String code = rs.getString("code");
            if (code == null) code = "";
            this.model.addRow(new Object[]{
               rs.getInt("id"),
               code,
               rs.getString("designation"),
               rs.getDouble("prix_gros"),
               rs.getDouble("prix_detail"),
               rs.getInt("tva"),
               rs.getInt("stock")
            });
         }
         this.calculerStatistiques();
         this.updateDashboard();
      } catch (SQLException e) {
         e.printStackTrace();
         JOptionPane.showMessageDialog(this, "Erreur DB: " + e.getMessage());
      }
      this.tableArticles.repaint();
   }

   private void updateDashboard() {
      SwingUtilities.invokeLater(() -> {
         Container contentPane = this.getContentPane();
         try {
            JPanel mainContent = (JPanel) ((BorderLayout) contentPane.getLayout()).getLayoutComponent(BorderLayout.CENTER);
            JPanel topPanel    = (JPanel) ((BorderLayout) mainContent.getLayout()).getLayoutComponent(BorderLayout.NORTH);
            JPanel oldDashboard = (JPanel) ((BorderLayout) topPanel.getLayout()).getLayoutComponent(BorderLayout.NORTH);
            if (topPanel != null && oldDashboard != null) {
               JPanel newDashboard = this.createDashboardPanel();
               topPanel.remove(oldDashboard);
               topPanel.add(newDashboard, BorderLayout.NORTH);
               topPanel.revalidate();
               topPanel.repaint();
            }
         } catch(Exception e) {}
      });
   }

   private void modifierArticle() {
      int selectedRow = this.tableArticles.getSelectedRow();
      if (selectedRow == -1) {
         JOptionPane.showMessageDialog(this, "Veuillez sélectionner un article.", "Info", JOptionPane.INFORMATION_MESSAGE);
         return;
      }
      int modelRow = this.tableArticles.convertRowIndexToModel(selectedRow);
      ModifierArticleUI form = new ModifierArticleUI(
            this,
            (Integer) this.model.getValueAt(modelRow, 0),
            (String)  this.model.getValueAt(modelRow, 2),
            (Double)  this.model.getValueAt(modelRow, 3),
            (Integer) this.model.getValueAt(modelRow, 5));
      form.setVisible(true);
   }

   private void supprimerArticle() {
      int selectedRow = this.tableArticles.getSelectedRow();
      if (selectedRow == -1) {
         JOptionPane.showMessageDialog(this, "Veuillez sélectionner un article.", "Info", JOptionPane.INFORMATION_MESSAGE);
         return;
      }
      int modelRow = this.tableArticles.convertRowIndexToModel(selectedRow);
      SupprimerArticleUI form = new SupprimerArticleUI(
            this,
            (Integer) this.model.getValueAt(modelRow, 0),
            (String)  this.model.getValueAt(modelRow, 2));
      form.setVisible(true);
   }

   public void rafraichirTableau() { this.chargerArticles(); }

   public static void main(String[] args) {
      SwingUtilities.invokeLater(() -> {
         try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception e) {}
         (new GestionStock()).setVisible(true);
      });
   }
}