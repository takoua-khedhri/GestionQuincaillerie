package com.myapp.ui;

import com.myapp.db.ConnexionSQLite;
import com.myapp.util.AppTheme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
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

public class ListeClients extends JFrame {
   private static final Logger log = LoggerFactory.getLogger(ListeClients.class);
   private JTable tableClients;
   private DefaultTableModel model;
   private JButton btnAjouter;
   private JButton btnModifier;
   private JButton btnSupprimer;
   private JButton btnRafraichir;
   private JButton btnRetour;
   private JLabel lblStatut;
   private JTextField txtRecherche;
   private TableRowSorter<DefaultTableModel> sorter;
   
   // Statistiques clients
   private int totalClients = 0;
   private int clientsActifs = 0;
   private int clientsSansAchats = 0;
   private int clientsFidelises = 0;
   
   // Cache des IDs clients pour les filtres
   private List<Integer> idsClientsActifs = new ArrayList<>();
   private List<Integer> idsClientsSansAchats = new ArrayList<>();
   private List<Integer> idsClientsFidelises = new ArrayList<>();
   
   private Font fontAwesomeSolid;

   public ListeClients() {
      // 1. Chargement Police
      this.loadFontAwesome();

      this.setTitle("Liste des Clients");
      this.setExtendedState(JFrame.MAXIMIZED_BOTH);
      this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      this.setLocationRelativeTo(null);
      this.setMinimumSize(new Dimension(900, 600));
      this.setLayout(new BorderLayout(10, 10));
      
      this.initUI();
      this.chargerClients();
   }

   private void loadFontAwesome() {
        try (InputStream fontStream = this.getClass().getResourceAsStream("/fonts/fa.ttf")) {
            if (fontStream != null) {
                Font font = Font.createFont(Font.TRUETYPE_FONT, fontStream);
                GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
                ge.registerFont(font);
                this.fontAwesomeSolid = font;
            } else {
                this.fontAwesomeSolid = new Font("SansSerif", Font.PLAIN, 12);
            }
        } catch (Exception e) {
            log.error("Erreur lors du chargement de FontAwesome", e);
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
      
      // HTML pour Icône début + Texte + Flèche fin
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
            ListeClients.this.showMenuForButton(button, menuItems, menuIcons);
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
          case "Nouveau Client": this.ouvrirNouveauClient(); break;
          case "Historique Entrée": this.ouvrirHistoriqueEntrees(); break;
          case "Historique Sortie": this.ouvrirHistoriqueSorties(); break;
          case "Liste Clients": 
              // Déjà ici
              break;
          case "Liste Factures": this.ouvrirListeFactures(); break;
          case "Gestion Entrée": this.ouvrirGestionEntrees(); break;
          case "Gestion Sortie": this.ouvrirGestionSorties(); break;
          case "Devis": this.ouvrirDevis(); break;
          case "Bon de Livraison": this.ouvrirBonLivraison(); break;
          case "Facture": this.ouvrirFacture(); break;
          case "Bon de Sortie": this.ouvrirBonSortie(); break;
          case "Consulter Stock": this.ouvrirGestionStock(); break;
      }
   }

   // --- Méthodes d'ouverture ---
   private void ouvrirNouveauClient() { try { (new AjouterClient(this)).setVisible(true); } catch (Exception e) {} }
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

      // 2. Menus
      String[] clientsMenuItems = new String[]{"Liste Clients", "Nouveau Client"};
      String[] clientsMenuIcons = new String[]{"\uf0c0", "\uf067"};
      JButton btnClients = this.createNavButtonWithMenu("\uf0c0", "Clients", true, clientsMenuItems, clientsMenuIcons);
      navPanel.add(btnClients);

      String[] documentsMenuItems = new String[]{"Facture", "Liste Factures", "Bon de Livraison", "Bon de Sortie", "Devis"};
      String[] documentsMenuIcons = new String[]{"\uf15b", "\uf15b", "\uf15b", "\uf15b", "\uf15b"};
      JButton btnDocuments = this.createNavButtonWithMenu("\uf15b", "Documents", false, documentsMenuItems, documentsMenuIcons);
      navPanel.add(btnDocuments);

      String[] stockMenuItems = new String[]{"Consulter Stock", "Gestion Entrée", "Gestion Sortie", "Historique Entrée", "Historique Sortie"};
      String[] stockMenuIcons = new String[]{"\uf494", "\uf090", "\uf08b", "\uf1da", "\uf201"};
      JButton btnStock = this.createNavButtonWithMenu("\uf494", "Stock", false, stockMenuItems, stockMenuIcons);
      navPanel.add(btnStock);
      
      return navPanel;
   }

   private JPanel createStatCard(String icon, String title, String value, Color bgColor, Color iconColor, String subtitle) {
      JPanel card = new JPanel(new BorderLayout(10, 10));
      card.setBackground(bgColor);
      // Bordure fixe sans changement
      card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 1), BorderFactory.createEmptyBorder(15, 20, 15, 20)));
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

   // Méthode pour rendre une carte cliquable - VERSION SIMPLIFIÉE (sans effets de survol)
   private void rendreCarteCliquable(JPanel carte, int typeFiltre) {
      carte.setCursor(new Cursor(Cursor.HAND_CURSOR));
      carte.addMouseListener(new MouseAdapter() {
         @Override
         public void mouseClicked(MouseEvent e) {
            appliquerFiltreClient(typeFiltre);
         }
         // NE PAS redéfinir mouseEntered/mouseExited - les cartes restent inchangées
      });
   }

   // Méthode pour appliquer un filtre selon le type de client
   private void appliquerFiltreClient(int typeFiltre) {
      txtRecherche.setText("");
      
      switch (typeFiltre) {
         case 0: // Tous les clients
            sorter.setRowFilter(null);
            break;
            
         case 1: // Clients actifs (avec achats)
            sorter.setRowFilter(new RowFilter<DefaultTableModel, Integer>() {
               public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                  Integer id = (Integer) entry.getValue(0); // Colonne 0 = ID
                  return idsClientsActifs.contains(id);
               }
            });
            break;
            
         case 2: // Nouveaux clients (sans achats)
            sorter.setRowFilter(new RowFilter<DefaultTableModel, Integer>() {
               public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                  Integer id = (Integer) entry.getValue(0);
                  return idsClientsSansAchats.contains(id);
               }
            });
            break;
            
         case 3: // Clients fidèles (3+ achats)
            sorter.setRowFilter(new RowFilter<DefaultTableModel, Integer>() {
               public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                  Integer id = (Integer) entry.getValue(0);
                  return idsClientsFidelises.contains(id);
               }
            });
            break;
      }
   }

   private JPanel createDashboardPanel() {
      JPanel dashboardPanel = new JPanel(new GridBagLayout());
      dashboardPanel.setBackground(Color.WHITE);
      // Titre avec Bordure
      dashboardPanel.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createEmptyBorder(10, 10, 10, 10), 
              BorderFactory.createTitledBorder(
                      BorderFactory.createLineBorder(new Color(220, 220, 220)), 
                      " TABLEAU DE BORD DES CLIENTS", 
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
      
      JPanel card1 = this.createStatCard("\uf0c0", "TOTAL CLIENTS", String.valueOf(this.totalClients), new Color(52, 152, 219, 40), new Color(41, 128, 185), "Clients enregistrés");
      JPanel card2 = this.createStatCard("\uf4fc", "CLIENTS ACTIFS", String.valueOf(this.clientsActifs), new Color(46, 204, 113, 40), new Color(39, 174, 96), "Avec historique d'achats");
      JPanel card3 = this.createStatCard("\uf234", "NOUVEAUX CLIENTS", String.valueOf(this.clientsSansAchats), new Color(155, 89, 182, 40), new Color(142, 68, 173), "Sans historique d'achats");
      JPanel card4 = this.createStatCard("\uf091", "CLIENTS FIDÈLES", String.valueOf(this.clientsFidelises), new Color(230, 126, 34, 40), new Color(211, 84, 0), "Achats répétés (3+)");
      
      // Rendre les cartes cliquables (sans effets de survol)
      this.rendreCarteCliquable(card1, 0); // Tous
      this.rendreCarteCliquable(card2, 1); // Actifs
      this.rendreCarteCliquable(card3, 2); // Nouveaux (sans achats)
      this.rendreCarteCliquable(card4, 3); // Fidèles
      
      gbc.gridx = 0; gbc.gridy = 0; dashboardPanel.add(card1, gbc);
      gbc.gridx = 1; dashboardPanel.add(card2, gbc);
      gbc.gridx = 2; dashboardPanel.add(card3, gbc);
      gbc.gridx = 3; dashboardPanel.add(card4, gbc);
      
      return dashboardPanel;
   }

   // Méthode améliorée pour calculer les statistiques réelles depuis la base
   private void calculerStatistiques() {
      // Réinitialiser les listes d'IDs
      idsClientsActifs.clear();
      idsClientsSansAchats.clear();
      idsClientsFidelises.clear();
      
      // Requête pour compter le nombre d'achats par client
      String sqlAchats = "SELECT c.id, COUNT(f.id) as nbAchats " +
                         "FROM clients c " +
                         "LEFT JOIN Factures f ON c.id = f.client_id " +
                         "GROUP BY c.id";
      
      try (Connection conn = ConnexionSQLite.getConnection();
           Statement stmt = conn.createStatement()) {
         
         // Total des clients
         ResultSet rsTotal = stmt.executeQuery("SELECT COUNT(*) as total FROM clients");
         if (rsTotal.next()) {
             this.totalClients = rsTotal.getInt("total");
         }
         
         // Compter les clients par catégorie
         ResultSet rsAchats = stmt.executeQuery(sqlAchats);
         
         int actifs = 0;
         int fideles = 0;
         int sansAchats = 0;
         
         while (rsAchats.next()) {
            int id = rsAchats.getInt("id");
            int nbAchats = rsAchats.getInt("nbAchats");
            
            if (nbAchats == 0) {
               sansAchats++;
               idsClientsSansAchats.add(id);
            } else {
               actifs++;
               idsClientsActifs.add(id);
               
               if (nbAchats >= 3) {
                  fideles++;
                  idsClientsFidelises.add(id);
               }
            }
         }
         
         this.clientsActifs = actifs;
         this.clientsSansAchats = sansAchats;
         this.clientsFidelises = fideles;
         
      } catch (SQLException e) {
         log.error("Erreur lors du calcul des statistiques clients", e);
         // Fallback aux valeurs simulées en cas d'erreur
         this.clientsActifs = (int)((double)this.totalClients * 0.65D);
         this.clientsSansAchats = this.totalClients - this.clientsActifs;
         this.clientsFidelises = (int)((double)this.clientsActifs * 0.4D);
      }
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
      
      JLabel titleLabel = new JLabel(this.getHtmlText("\uf0c0", "LISTE DES CLIENTS"), JLabel.CENTER);
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
      this.txtRecherche.setToolTipText("Rechercher par nom, prénom, numéro ou adresse...");
      this.txtRecherche.addKeyListener(new KeyAdapter() {
         public void keyReleased(KeyEvent e) {
            String text = ListeClients.this.txtRecherche.getText();
            if (text.trim().length() == 0) {
               ListeClients.this.sorter.setRowFilter((RowFilter)null);
            } else {
               ListeClients.this.sorter.setRowFilter(RowFilter.regexFilter("(?i)" + text, new int[]{1, 2, 3, 4, 5}));
            }
         }
      });
      searchPanel.add(lblRecherche, BorderLayout.WEST);
      searchPanel.add(this.txtRecherche, BorderLayout.CENTER);
      
      this.model = new DefaultTableModel(new String[]{"ID", "Nom", "Prénom", "Numéro", "Adresse", "Matricule Fiscale"}, 0) {
         public boolean isCellEditable(int row, int column) { return false; }
      };
      
      this.tableClients = new JTable(this.model);
      this.sorter = new TableRowSorter(this.model);
      this.tableClients.setRowSorter(this.sorter);
      this.tableClients.setRowHeight(35);
      this.tableClients.setSelectionBackground(new Color(220, 240, 255));
      this.tableClients.setFont(new Font("Segoe UI", Font.PLAIN, 13));
      this.tableClients.setGridColor(new Color(240, 240, 240));
      
      this.tableClients.getColumnModel().getColumn(0).setPreferredWidth(50);
      this.tableClients.getColumnModel().getColumn(1).setPreferredWidth(150);
      
      JTableHeader header = this.tableClients.getTableHeader();
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
      
      this.tableClients.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
         public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            
            // Récupérer l'ID pour déterminer la couleur de fond
            int id = 0;
            try {
               id = (Integer) table.getValueAt(row, 0);
            } catch (Exception e) {}
            
            if (!isSelected) {
               // Alternance de couleurs de base
               c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 248, 248));
               
               // Surligner en fonction du statut
               if (idsClientsFidelises.contains(id)) {
                  c.setBackground(new Color(255, 235, 215)); // Orange très clair pour fidèles
               } else if (idsClientsActifs.contains(id)) {
                  c.setBackground(new Color(235, 255, 235)); // Vert très clair pour actifs
               } else if (idsClientsSansAchats.contains(id)) {
                  c.setBackground(new Color(235, 245, 255)); // Bleu très clair pour nouveaux
               }
            }

            if(column == 0 || column == 3) this.setHorizontalAlignment(JLabel.CENTER);
            else this.setHorizontalAlignment(JLabel.LEFT);

            return c;
         }
      });
      
      JScrollPane scrollPane = new JScrollPane(this.tableClients);
      scrollPane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)), "Liste des Clients"));
      
      JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 15));
      buttonPanel.setBackground(new Color(250, 250, 250));
      
      this.btnAjouter = this.createStyledButton("\uf067", "Ajouter Client", new Color(46, 204, 113));
      this.btnModifier = this.createStyledButton("\uf044", "Modifier Client", new Color(52, 152, 219));
      this.btnSupprimer = this.createStyledButton("\uf1f8", "Supprimer Client", new Color(231, 76, 60));
      this.btnRafraichir = this.createStyledButton("\uf021", "Rafraîchir", new Color(155, 89, 182));
      
      buttonPanel.add(this.btnAjouter);
      buttonPanel.add(this.btnModifier);
      buttonPanel.add(this.btnSupprimer);
      buttonPanel.add(this.btnRafraichir);
      
      this.lblStatut = new JLabel("Chargement des clients...", JLabel.CENTER);
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
      
      this.btnAjouter.addActionListener((e) -> this.ouvrirNouveauClient());
      this.btnModifier.addActionListener((e) -> this.modifierClient());
      this.btnSupprimer.addActionListener((e) -> this.supprimerClient());
      this.btnRafraichir.addActionListener((e) -> {
         this.chargerClients();
         this.updateDashboard();
      });
   }

   private JButton createStyledButton(String iconCode, String text, final Color backgroundColor) {
      // HTML pour icônes
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
            button.setBackground(ListeClients.this.darkenColor(backgroundColor, 0.9F));
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

   private void chargerClients() {
      this.model.setRowCount(0);
      this.lblStatut.setText("Chargement en cours...");
      
      try (Connection conn = ConnexionSQLite.getConnection();
           Statement stmt = conn.createStatement();
           ResultSet rs = stmt.executeQuery("SELECT * FROM clients ORDER BY nom, prenom")) {

         int count = 0;
         while(rs.next()) {
            this.model.addRow(new Object[]{
                rs.getInt("id"),
                rs.getString("nom"),
                rs.getString("prenom"),
                rs.getInt("numero"),
                rs.getString("adresse"),
                rs.getString("matricule_fiscale")
            });
            count++;
         }
         this.lblStatut.setText("✓ " + count + " clients chargés avec succès");
         this.lblStatut.setForeground(new Color(46, 204, 113));
         
         // Recalculer les statistiques après chargement
         this.calculerStatistiques();
         
      } catch (SQLException e) {
         log.error("Erreur lors du chargement des clients", e);
         this.lblStatut.setText("✗ Erreur lors du chargement des clients");
         this.lblStatut.setForeground(Color.RED);
      }
   }

   private void modifierClient() {
      int selectedRow = this.tableClients.getSelectedRow();
      if (selectedRow == -1) {
         JOptionPane.showMessageDialog(this, "Veuillez sélectionner un client.", "Info", JOptionPane.INFORMATION_MESSAGE);
         return;
      }
      int modelRow = this.tableClients.convertRowIndexToModel(selectedRow);
      
      // Récupérer les informations du client
      int id = (Integer)this.model.getValueAt(modelRow, 0);
      String nom = (String)this.model.getValueAt(modelRow, 1);
      String prenom = (String)this.model.getValueAt(modelRow, 2);
      int numero = (Integer)this.model.getValueAt(modelRow, 3);
      String adresse = (String)this.model.getValueAt(modelRow, 4);
      String matricule = (String)this.model.getValueAt(modelRow, 5);
      
      // Ouvrir la fenêtre de modification
      ModifierClient modifierDialog = new ModifierClient(this, id, nom, prenom, numero, adresse, matricule);
      modifierDialog.setVisible(true);
   }

   private void supprimerClient() {
      int selectedRow = this.tableClients.getSelectedRow();
      if (selectedRow == -1) {
         JOptionPane.showMessageDialog(this, "Veuillez sélectionner un client.", "Info", JOptionPane.INFORMATION_MESSAGE);
         return;
      }
      int modelRow = this.tableClients.convertRowIndexToModel(selectedRow);
      int id = (Integer)this.model.getValueAt(modelRow, 0);
      String nom = (String)this.model.getValueAt(modelRow, 1);
      String prenom = (String)this.model.getValueAt(modelRow, 2);
      String nomComplet = nom + " " + prenom;
      
      Object[] options = {"Oui", "Non"};
      int result = JOptionPane.showOptionDialog(this,
         "Supprimer définitivement le client " + nomComplet + " ?\nCette action est irréversible.",
         "Confirmation de suppression",
         JOptionPane.YES_NO_OPTION,
         JOptionPane.WARNING_MESSAGE,
         null, options, options[1]);

      if (result == 0) {
         this.supprimerClientDirect(id);
      }
   }

   private void supprimerClientDirect(int clientId) {
      try (Connection conn = ConnexionSQLite.getConnection();
           PreparedStatement pst = conn.prepareStatement("DELETE FROM clients WHERE id = ?")) {
           
           pst.setInt(1, clientId);
           if (pst.executeUpdate() > 0) {
              JOptionPane.showMessageDialog(this, "✅ Client supprimé avec succès.");
              this.rafraichirListeClients();
           }
      } catch (SQLException e) {
         JOptionPane.showMessageDialog(this, "❌ Erreur: " + e.getMessage());
      }
   }

   public void rafraichirListeClients() {
      this.chargerClients();
      this.txtRecherche.setText("");
      this.updateDashboard();
   }

   public static void main(String[] args) {
      SwingUtilities.invokeLater(() -> {
         AppTheme.init();
         (new ListeClients()).setVisible(true);
      });
   }
}