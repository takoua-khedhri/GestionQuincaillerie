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
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
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

public class ListeFournisseurs extends JFrame {
   private JTable tableFournisseurs;
   private DefaultTableModel model;
   private JButton btnAjouter;
   private JButton btnModifier;
   private JButton btnSupprimer;
   private JButton btnRafraichir;
   private JLabel lblStatut;
   private JTextField txtRecherche;
   private TableRowSorter<DefaultTableModel> sorter;
   
   private int totalFournisseurs = 0;
   private int fournisseursActifs = 0;
   private int fournisseursAvecEmail = 0;
   private int fournisseursAvecMatricule = 0;
   
   private Font fontAwesomeSolid;

   public ListeFournisseurs() {
      // 1. Chargement Police
      this.loadFontAwesome();
       
      this.setTitle("Liste des Fournisseurs");
      this.setExtendedState(JFrame.MAXIMIZED_BOTH);
      this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      this.setLocationRelativeTo(null);
      this.setLayout(new BorderLayout(10, 10));
      
      this.initUI();
      this.chargerFournisseurs();
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

   // Générateur HTML pour Icône + Texte
   private String getHtmlText(String iconCode, String text) {
       String fontName = (this.fontAwesomeSolid != null) ? this.fontAwesomeSolid.getFontName() : "SansSerif";
       return "<html><font face=\"" + fontName + "\">" + iconCode + "</font> " + text + "</html>";
   }

   private JButton createNavButtonWithMenu(String icon, String text, final boolean isActive, final String[] menuItems, final String[] menuIcons) {
      String fontName = (this.fontAwesomeSolid != null) ? this.fontAwesomeSolid.getFontName() : "SansSerif";
      String chevron = "\uf078"; // Flèche bas
      
      String htmlContent = "<html><font face=\"" + fontName + "\">" + icon + "</font> " + 
                           text + 
                           " <font face=\"" + fontName + "\" size=\"3\">" + chevron + "</font></html>";
      
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
         public void mouseEntered(MouseEvent evt) {
            if (!isActive) button.setBackground(new Color(220, 220, 220));
            hoverTimer.start();
         }
         public void mouseExited(MouseEvent evt) {
            if (!isActive) button.setBackground(new Color(236, 240, 241));
            hoverTimer.stop();
         }
         public void mouseClicked(MouseEvent evt) {
            ListeFournisseurs.this.showMenuForButton(button, menuItems, menuIcons);
         }
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

   private void handleMenuAction(String menuItem) {
      switch(menuItem) {
          case "Nouveau Fournisseur": this.ouvrirNouveauFournisseur(); break;
          case "Liste Fournisseurs": 
              // Déjà ici
              break;
          case "Liste Clients": this.ouvrirListeClients(); break;
          case "Liste Factures": this.ouvrirListeFactures(); break;
          case "Gestion Entrée": this.ouvrirGestionEntrees(); break;
          case "Gestion Sortie": this.ouvrirGestionSorties(); break;
          case "Devis": this.ouvrirDevis(); break;
          case "Bon de Livraison": this.ouvrirBonLivraison(); break;
          case "Facture": this.ouvrirFacture(); break;
          case "Bon de Sortie": this.ouvrirBonSortie(); break;
          case "Consulter Stock": this.ouvrirGestionStock(); break;
          case "Historique Entrée": this.ouvrirHistoriqueEntrees(); break;
          case "Historique Sortie": this.ouvrirHistoriqueSorties(); break;
      }
   }

   // --- Méthodes d'ouverture ---
   private void ouvrirNouveauFournisseur() { 
      try { 
         new AjouterFournisseur(null).setVisible(true); 
         this.dispose();
      } catch (Exception e) {} 
   }
   private void ouvrirListeClients() { try { (new ListeClients()).setVisible(true); this.dispose(); } catch (Exception e) {} }
   private void ouvrirFacture() { try { (new FactureUI()).setVisible(true); this.dispose(); } catch (Exception e) {} }
   private void ouvrirListeFactures() { try { (new ListeFactures()).setVisible(true); this.dispose(); } catch (Exception e) {} }
   private void ouvrirBonLivraison() { try { (new BLUI()).setVisible(true); this.dispose(); } catch (Exception e) {} }
   private void ouvrirBonSortie() { try { (new BonSortieUI()).setVisible(true); this.dispose(); } catch (Exception e) {} }
   private void ouvrirDevis() { try { (new DevisUI()).setVisible(true); this.dispose(); } catch (Exception e) {} }
   private void ouvrirGestionStock() { try { (new GestionStock()).setVisible(true); this.dispose(); } catch (Exception e) {} }
   private void ouvrirGestionEntrees() { try { (new GestionEntreesUI()).setVisible(true); this.dispose(); } catch (Exception e) {} }
   private void ouvrirGestionSorties() { try { (new GestionSortiesUI()).setVisible(true); this.dispose(); } catch (Exception e) {} }
   private void ouvrirHistoriqueEntrees() { try { (new HistoriqueEntreesUI()).setVisible(true); this.dispose(); } catch (Exception e) {} }
   private void ouvrirHistoriqueSorties() { try { (new HistoriqueSortiesUI()).setVisible(true); this.dispose(); } catch (Exception e) {} }

   private JPanel createNavigationBar() {
      // 4 colonnes : Retour + 3 menus
      JPanel navPanel = new JPanel(new GridLayout(1, 4));
      navPanel.setBackground(new Color(52, 73, 94));
      
      // 1. Bouton Retour Dashboard
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

      // 2. Menu Fournisseurs
      String[] fournisseursMenuItems = new String[]{"Liste Fournisseurs", "Nouveau Fournisseur"};
      String[] fournisseursMenuIcons = new String[]{"\uf0c0", "\uf067"};
      JButton btnFournisseurs = this.createNavButtonWithMenu("\uf0c0", "Fournisseurs", true, fournisseursMenuItems, fournisseursMenuIcons);
      navPanel.add(btnFournisseurs);

      // 3. Menu Documents
      String[] documentsMenuItems = new String[]{"Facture", "Liste Factures", "Bon de Livraison", "Bon de Sortie", "Devis"};
      String[] documentsMenuIcons = new String[]{"\uf15b", "\uf15b", "\uf15b", "\uf15b", "\uf15b"};
      JButton btnDocuments = this.createNavButtonWithMenu("\uf15b", "Documents", false, documentsMenuItems, documentsMenuIcons);
      navPanel.add(btnDocuments);

      // 4. Menu Stock
      String[] stockMenuItems = new String[]{"Consulter Stock", "Gestion Entrée", "Gestion Sortie", "Historique Entrée", "Historique Sortie"};
      String[] stockMenuIcons = new String[]{"\uf494", "\uf090", "\uf08b", "\uf1da", "\uf201"};
      JButton btnStock = this.createNavButtonWithMenu("\uf494", "Stock", false, stockMenuItems, stockMenuIcons);
      navPanel.add(btnStock);
      
      return navPanel;
   }

   private JPanel createStatCard(String icon, String title, String value, Color bgColor, Color iconColor, String subtitle) {
      JPanel card = new JPanel(new BorderLayout(10, 10));
      card.setBackground(bgColor);
      card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(this.darkenColor(bgColor, 0.8F), 1), BorderFactory.createEmptyBorder(15, 20, 15, 20)));
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

   private JPanel createDashboardPanel() {
      JPanel dashboardPanel = new JPanel(new GridBagLayout());
      dashboardPanel.setBackground(Color.WHITE);
      dashboardPanel.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createEmptyBorder(10, 10, 10, 10), 
              BorderFactory.createTitledBorder(
                      BorderFactory.createLineBorder(new Color(220, 220, 220)), 
                      " TABLEAU DE BORD DES FOURNISSEURS", 
                      1, 
                      2, 
                      new Font("Segoe UI", Font.BOLD, 12), 
                      new Color(52, 73, 94))));
      
      GridBagConstraints gbc = new GridBagConstraints();
      gbc.fill = GridBagConstraints.BOTH;
      gbc.weightx = 1.0D;
      gbc.weighty = 1.0D;
      gbc.insets = new Insets(5, 5, 5, 5);
      
      this.calculerStatistiques();
      
      JPanel card1 = this.createStatCard("\uf0c0", "TOTAL FOURNISSEURS", String.valueOf(this.totalFournisseurs), new Color(52, 152, 219, 40), new Color(41, 128, 185), "Fournisseurs enregistrés");
      JPanel card2 = this.createStatCard("\uf4fc", "ACTIFS", String.valueOf(this.fournisseursActifs), new Color(46, 204, 113, 40), new Color(39, 174, 96), "Avec commandes");
      JPanel card3 = this.createStatCard("\uf0e0", "AVEC EMAIL", String.valueOf(this.fournisseursAvecEmail), new Color(155, 89, 182, 40), new Color(142, 68, 173), "Email renseigné");
      JPanel card4 = this.createStatCard("\uf02b", "MATRICULE", String.valueOf(this.fournisseursAvecMatricule), new Color(230, 126, 34, 40), new Color(211, 84, 0), "Matricule fiscal");
      
      gbc.gridx = 0; gbc.gridy = 0; dashboardPanel.add(card1, gbc);
      gbc.gridx = 1; dashboardPanel.add(card2, gbc);
      gbc.gridx = 2; dashboardPanel.add(card3, gbc);
      gbc.gridx = 3; dashboardPanel.add(card4, gbc);
      
      return dashboardPanel;
   }

   private void calculerStatistiques() {
      this.totalFournisseurs = 0;
      this.fournisseursAvecEmail = 0;
      this.fournisseursAvecMatricule = 0;
      
      try (Connection conn = ConnexionSQLite.getConnection();
           Statement stmt = conn.createStatement();
           ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as total, " +
               "SUM(CASE WHEN email IS NOT NULL AND email != '' THEN 1 ELSE 0 END) as avec_email, " +
               "SUM(CASE WHEN matricule_fiscale IS NOT NULL AND matricule_fiscale != '' THEN 1 ELSE 0 END) as avec_matricule " +
               "FROM Fournisseurs")) {
         if (rs.next()) {
             this.totalFournisseurs = rs.getInt("total");
             this.fournisseursAvecEmail = rs.getInt("avec_email");
             this.fournisseursAvecMatricule = rs.getInt("avec_matricule");
         }
      } catch (SQLException e) {
         e.printStackTrace();
      }
      
      // Simulation fournisseurs actifs (avec commandes)
      this.fournisseursActifs = (int)((double)this.totalFournisseurs * 0.7D);
   }

   private void updateDashboard() {
      SwingUtilities.invokeLater(() -> {
         try {
             JPanel mainContent = (JPanel) ((BorderLayout)this.getContentPane().getLayout()).getLayoutComponent(BorderLayout.CENTER);
             JPanel topPanel = (JPanel) ((BorderLayout)mainContent.getLayout()).getLayoutComponent(BorderLayout.NORTH);
             JPanel oldDashboard = (JPanel) ((BorderLayout)topPanel.getLayout()).getLayoutComponent(BorderLayout.NORTH);
             
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

   private void initUI() {
      JPanel navBarPanel = this.createNavigationBar();
      
      JPanel headerPanel = new JPanel(new BorderLayout());
      headerPanel.setBackground(new Color(44, 62, 80));
      headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
      
      JLabel titleLabel = new JLabel(this.getHtmlText("\uf0c0", "LISTE DES FOURNISSEURS"), JLabel.CENTER);
      titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
      titleLabel.setForeground(Color.WHITE);
      headerPanel.add(titleLabel, BorderLayout.CENTER);

      JPanel dashboardPanel = this.createDashboardPanel();
      
      JPanel searchPanel = new JPanel(new BorderLayout(10, 0));
      searchPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
      searchPanel.setBackground(Color.WHITE);
      
      JLabel lblRecherche = new JLabel(this.getHtmlText("\uf002", "Rechercher:"));
      lblRecherche.setForeground(Color.BLACK);
      
      this.txtRecherche = new JTextField();
      this.txtRecherche.setFont(new Font("Segoe UI", Font.PLAIN, 13));
      this.txtRecherche.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)), BorderFactory.createEmptyBorder(8, 10, 8, 10)));
      this.txtRecherche.setToolTipText("Rechercher par nom, prénom, téléphone, adresse...");
      this.txtRecherche.addKeyListener(new KeyAdapter() {
         public void keyReleased(KeyEvent e) {
            String text = ListeFournisseurs.this.txtRecherche.getText();
            if (text.trim().length() == 0) {
               ListeFournisseurs.this.sorter.setRowFilter((RowFilter)null);
            } else {
               ListeFournisseurs.this.sorter.setRowFilter(RowFilter.regexFilter("(?i)" + text, new int[]{1, 2, 3, 4, 5}));
            }
         }
      });
      searchPanel.add(lblRecherche, BorderLayout.WEST);
      searchPanel.add(this.txtRecherche, BorderLayout.CENTER);
      
      this.model = new DefaultTableModel(new String[]{"ID", "Nom", "Prénom", "Téléphone", "Adresse", "Matricule Fiscale", "Email"}, 0) {
         public boolean isCellEditable(int row, int column) { return false; }
      };
      
      this.tableFournisseurs = new JTable(this.model);
      this.sorter = new TableRowSorter(this.model);
      this.tableFournisseurs.setRowSorter(this.sorter);
      this.tableFournisseurs.setRowHeight(35);
      this.tableFournisseurs.setSelectionBackground(new Color(220, 240, 255));
      this.tableFournisseurs.setFont(new Font("Segoe UI", Font.PLAIN, 13));
      this.tableFournisseurs.setGridColor(new Color(240, 240, 240));
      
      this.tableFournisseurs.getColumnModel().getColumn(0).setPreferredWidth(50);
      this.tableFournisseurs.getColumnModel().getColumn(1).setPreferredWidth(150);
      this.tableFournisseurs.getColumnModel().getColumn(2).setPreferredWidth(150);
      this.tableFournisseurs.getColumnModel().getColumn(3).setPreferredWidth(100);
      this.tableFournisseurs.getColumnModel().getColumn(4).setPreferredWidth(200);
      this.tableFournisseurs.getColumnModel().getColumn(5).setPreferredWidth(120);
      this.tableFournisseurs.getColumnModel().getColumn(6).setPreferredWidth(180);
      
      JTableHeader header = this.tableFournisseurs.getTableHeader();
      TableCellRenderer headerRenderer = new DefaultTableCellRenderer() {
         public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            this.setBackground(new Color(52, 73, 94));
            this.setForeground(Color.WHITE);
            this.setFont(new Font("Segoe UI", Font.BOLD, 13));
            this.setHorizontalAlignment(JLabel.CENTER);
            this.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            return this;
         }
      };
      header.setDefaultRenderer(headerRenderer);
      
      this.tableFournisseurs.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
         public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (!isSelected) {
               c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 248, 248));
            }

            if(column == 0 || column == 3) this.setHorizontalAlignment(JLabel.CENTER);
            else this.setHorizontalAlignment(JLabel.LEFT);

            return c;
         }
      });
      
      JScrollPane scrollPane = new JScrollPane(this.tableFournisseurs);
      scrollPane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)), "Liste des Fournisseurs"));
      
      JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 15));
      buttonPanel.setBackground(new Color(250, 250, 250));
      
      this.btnAjouter = this.createStyledButton("\uf067", "Ajouter Fournisseur", new Color(46, 204, 113));
      this.btnModifier = this.createStyledButton("\uf044", "Modifier Fournisseur", new Color(52, 152, 219));
      this.btnSupprimer = this.createStyledButton("\uf1f8", "Supprimer Fournisseur", new Color(231, 76, 60));
      this.btnRafraichir = this.createStyledButton("\uf021", "Rafraîchir", new Color(155, 89, 182));
      
      buttonPanel.add(this.btnAjouter);
      buttonPanel.add(this.btnModifier);
      buttonPanel.add(this.btnSupprimer);
      buttonPanel.add(this.btnRafraichir);
      
      this.lblStatut = new JLabel("Chargement des fournisseurs...", JLabel.CENTER);
      this.lblStatut.setFont(new Font("Segoe UI", Font.PLAIN, 12));
      this.lblStatut.setForeground(Color.GRAY);
      this.lblStatut.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
      this.lblStatut.setOpaque(true);
      this.lblStatut.setBackground(new Color(240, 240, 240));
      
      this.add(navBarPanel, BorderLayout.NORTH);
      
      JPanel mainContentPanel = new JPanel(new BorderLayout());
      mainContentPanel.add(headerPanel, BorderLayout.NORTH);
      
      JPanel topPanel = new JPanel(new BorderLayout());
      topPanel.add(dashboardPanel, BorderLayout.NORTH);
      mainContentPanel.add(topPanel, BorderLayout.NORTH);
      
      JPanel centerPanel = new JPanel(new BorderLayout());
      centerPanel.add(searchPanel, BorderLayout.NORTH);
      centerPanel.add(scrollPane, BorderLayout.CENTER);
      mainContentPanel.add(centerPanel, BorderLayout.CENTER);
      
      JPanel southPanel = new JPanel(new BorderLayout());
      southPanel.add(buttonPanel, BorderLayout.CENTER);
      southPanel.add(this.lblStatut, BorderLayout.SOUTH);
      mainContentPanel.add(southPanel, BorderLayout.SOUTH);
      
      this.add(mainContentPanel, BorderLayout.CENTER);
      
      this.btnAjouter.addActionListener((e) -> this.ouvrirNouveauFournisseur());
      this.btnModifier.addActionListener((e) -> this.modifierFournisseur());
      this.btnSupprimer.addActionListener((e) -> this.supprimerFournisseur());
      this.btnRafraichir.addActionListener((e) -> {
         this.chargerFournisseurs();
         this.updateDashboard();
      });
   }

   private JButton createStyledButton(String iconCode, String text, final Color backgroundColor) {
      final JButton button = new JButton(this.getHtmlText(iconCode, text));
      button.setFont(new Font("Segoe UI", Font.BOLD, 13));

      button.setBackground(backgroundColor);
      button.setForeground(Color.WHITE);
      button.setFocusPainted(false);
      button.setBorderPainted(false);
      button.setOpaque(true);
      button.setCursor(new Cursor(Cursor.HAND_CURSOR));
      button.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
      button.addMouseListener(new MouseAdapter() {
         public void mouseEntered(MouseEvent evt) {
            button.setBackground(ListeFournisseurs.this.darkenColor(backgroundColor, 0.9F));
         }
         public void mouseExited(MouseEvent evt) {
            button.setBackground(backgroundColor);
         }
      });
      return button;
   }

   private Color darkenColor(Color color, float factor) {
      int r = Math.max((int)((float)color.getRed() * factor), 0);
      int g = Math.max((int)((float)color.getGreen() * factor), 0);
      int b = Math.max((int)((float)color.getBlue() * factor), 0);
      return new Color(r, g, b);
   }

   private void chargerFournisseurs() {
      this.model.setRowCount(0);
      this.lblStatut.setText("Chargement en cours...");
      
      try (Connection conn = ConnexionSQLite.getConnection();
           Statement stmt = conn.createStatement();
           ResultSet rs = stmt.executeQuery("SELECT * FROM Fournisseurs ORDER BY nom, prenom")) {

         int count = 0;
         while(rs.next()) {
            this.model.addRow(new Object[]{
                rs.getInt("id"),
                rs.getString("nom"),
                rs.getString("prenom") != null ? rs.getString("prenom") : "",
                rs.getString("telephone") != null ? rs.getString("telephone") : "",
                rs.getString("adresse") != null ? rs.getString("adresse") : "",
                rs.getString("matricule_fiscale") != null ? rs.getString("matricule_fiscale") : "",
                rs.getString("email") != null ? rs.getString("email") : ""
            });
            count++;
         }
         this.totalFournisseurs = count;
         this.lblStatut.setText("✓ " + count + " fournisseurs chargés avec succès");
         this.lblStatut.setForeground(new Color(46, 204, 113));
         
      } catch (SQLException e) {
         e.printStackTrace();
         this.lblStatut.setText("✗ Erreur lors du chargement des fournisseurs");
         this.lblStatut.setForeground(Color.RED);
      }
      this.calculerStatistiques();
   }

   private void modifierFournisseur() {
      int selectedRow = this.tableFournisseurs.getSelectedRow();
      if (selectedRow == -1) {
         JOptionPane.showMessageDialog(this, "Veuillez sélectionner un fournisseur.", "Info", JOptionPane.INFORMATION_MESSAGE);
         return;
      }
      int modelRow = this.tableFournisseurs.convertRowIndexToModel(selectedRow);
      new ModifierFournisseur(
          this, 
          (Integer)this.model.getValueAt(modelRow, 0),
          (String)this.model.getValueAt(modelRow, 1),
          (String)this.model.getValueAt(modelRow, 2),
          (String)this.model.getValueAt(modelRow, 3),
          (String)this.model.getValueAt(modelRow, 4),
          (String)this.model.getValueAt(modelRow, 5),
          (String)this.model.getValueAt(modelRow, 6)
      ).setVisible(true);
   }

   private void supprimerFournisseur() {
      int selectedRow = this.tableFournisseurs.getSelectedRow();
      if (selectedRow == -1) {
         JOptionPane.showMessageDialog(this, "Veuillez sélectionner un fournisseur.", "Info", JOptionPane.INFORMATION_MESSAGE);
         return;
      }
      int modelRow = this.tableFournisseurs.convertRowIndexToModel(selectedRow);
      int id = (Integer)this.model.getValueAt(modelRow, 0);
      String nom = (String)this.model.getValueAt(modelRow, 1);
      
      int result = JOptionPane.showConfirmDialog(this, "Supprimer définitivement le fournisseur " + nom + " ?\nCette action supprimera également l'historique des commandes.", 
            "Confirmation", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
      if (result == JOptionPane.YES_OPTION) {
         this.supprimerFournisseurDirect(id);
      }
   }

   private void supprimerFournisseurDirect(int fournisseurId) {
      try (Connection conn = ConnexionSQLite.getConnection();
           PreparedStatement pst = conn.prepareStatement("DELETE FROM Fournisseurs WHERE id = ?")) {
           
           pst.setInt(1, fournisseurId);
           if (pst.executeUpdate() > 0) {
              JOptionPane.showMessageDialog(this, "✅ Fournisseur supprimé avec succès.");
              this.rafraichirListeFournisseurs();
           }
      } catch (SQLException e) {
         JOptionPane.showMessageDialog(this, "❌ Erreur: " + e.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
      }
   }

   public void rafraichirListeFournisseurs() {
      this.chargerFournisseurs();
      this.txtRecherche.setText("");
      this.updateDashboard();
   }

   public static void main(String[] args) {
      SwingUtilities.invokeLater(() -> {
         try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
         } catch (Exception e) {}
         (new ListeFournisseurs()).setVisible(true);
      });
   }
}