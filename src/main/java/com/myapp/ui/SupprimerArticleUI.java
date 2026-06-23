package com.myapp.ui;

import com.myapp.db.ConnexionSQLite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

public class SupprimerArticleUI extends JDialog {
   private static final Logger log = LoggerFactory.getLogger(SupprimerArticleUI.class);
   private int articleId;
   private String designation;
   private GestionStock parent;
   private Font fontAwesomeSolid;

   public SupprimerArticleUI(GestionStock parent, int articleId, String designation) {
      super(parent, "Supprimer l'Article", true);
      this.parent = parent;
      this.articleId = articleId;
      this.designation = designation;
      this.loadFontAwesome();
      this.setSize(500, 320); // Légèrement ajusté
      this.setLocationRelativeTo(parent);
      this.setLayout(new BorderLayout(10, 10));
      this.setResizable(false);
      this.initUI();
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

   private String getHtmlText(String iconCode, String text) {
       String fontName = (this.fontAwesomeSolid != null) ? this.fontAwesomeSolid.getFontName() : "SansSerif";
       return "<html><div style='text-align: center;'><font face=\"" + fontName + "\">" + iconCode + "</font> " + text + "</div></html>";
   }

   private void initUI() {
      // Header Panel
      JPanel headerPanel = new JPanel(new BorderLayout());
      headerPanel.setBackground(new Color(231, 76, 60));
      headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
      
      JLabel titleLabel = new JLabel(getHtmlText("\uf1f8", " SUPPRESSION D'ARTICLE"), SwingConstants.CENTER);
      titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
      titleLabel.setForeground(Color.WHITE);
      headerPanel.add(titleLabel, BorderLayout.CENTER);

      // Confirmation Panel
      JPanel confirmationPanel = new JPanel(new BorderLayout(10, 10));
      confirmationPanel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));
      confirmationPanel.setBackground(Color.WHITE);

      // J'ai retiré le JLabel warningIcon séparé pour éviter le "double icône" visuel trop chargé
      // On intègre l'icône directement dans le message principal pour un rendu plus propre

      // Message Label
      String fontName = (this.fontAwesomeSolid != null) ? this.fontAwesomeSolid.getFontName() : "SansSerif";
      JLabel messageLabel = new JLabel(
         "<html><div style='text-align: center;'>" +
         "<font face=\"" + fontName + "\" size='6' color='#E74C3C'>\uf071</font><br><br>" + // L'icône est ici
         "Article : <font color='red'>" + this.designation + "</font><br><br>" +
         "Cette action est <font color='red'><b>irréversible</b></font>." +
         "</div></html>"
      );
      messageLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
      messageLabel.setHorizontalAlignment(SwingConstants.CENTER);

      // Stock Warning Label
      JLabel stockWarningLabel = new JLabel();
      stockWarningLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
      stockWarningLabel.setHorizontalAlignment(SwingConstants.CENTER);
      
      int stockActuel = this.getStockActuel();
      if (stockActuel > 0) {
         stockWarningLabel.setText("<html><div style='text-align: center; margin-top:10px;'>" +
            "<span style='color: #E67E22;'><b>⚠️ Attention: Cet article a encore " + stockActuel + " unités en stock!</b></span>" +
            "</div></html>");
      } else {
         stockWarningLabel.setText("<html><div style='text-align: center; margin-top:10px;'>" +
            "<span style='color: #3498DB;'>ℹ️ Le stock est actuellement à 0.</span>" +
            "</div></html>");
      }

      confirmationPanel.add(messageLabel, BorderLayout.CENTER);
      confirmationPanel.add(stockWarningLabel, BorderLayout.SOUTH);

      // Button Panel
      JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 25, 15));
      buttonPanel.setBackground(new Color(240, 240, 240));
      buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 20, 0));

      JButton btnSupprimer = createStyledButton("\uf1f8 Supprimer définitivement", new Color(231, 76, 60));
      JButton btnAnnuler = createStyledButton("\uf060 Annuler", new Color(52, 152, 219));

      btnSupprimer.addActionListener((e) -> {
         this.supprimerArticle();
      });

      btnAnnuler.addActionListener((e) -> {
         this.dispose();
      });

      buttonPanel.add(btnSupprimer);
      buttonPanel.add(btnAnnuler);

      this.add(headerPanel, BorderLayout.NORTH);
      this.add(confirmationPanel, BorderLayout.CENTER);
      this.add(buttonPanel, BorderLayout.SOUTH);
   }

   private JButton createStyledButton(String text, final Color backgroundColor) {
      final JButton button = new JButton();
      String buttonText = getHtmlText(text.substring(0, 1), text.substring(1));
      button.setText(buttonText);
      button.setFont(new Font("Segoe UI", Font.BOLD, 12));
      button.setBackground(backgroundColor);
      button.setForeground(Color.WHITE);
      button.setFocusPainted(false);
      button.setBorderPainted(false);
      button.setOpaque(true);
      button.setCursor(new Cursor(Cursor.HAND_CURSOR));
      button.setBorder(BorderFactory.createEmptyBorder(12, 25, 12, 25));
      
      button.addMouseListener(new MouseAdapter() {
         public void mouseEntered(MouseEvent evt) {
            button.setBackground(darkenColor(backgroundColor, 0.9F));
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

   private int getStockActuel() {
      try (Connection conn = ConnexionSQLite.getConnection();
           PreparedStatement pst = conn.prepareStatement("SELECT stock FROM Articles WHERE id = ?")) {
         pst.setInt(1, this.articleId);
         ResultSet rs = pst.executeQuery();
         if (rs.next()) {
            return rs.getInt("stock");
         }
      } catch (SQLException e) {
         log.error("Erreur lors de la récupération du stock actuel", e);
      }
      return 0;
   }

   private void supprimerArticle() {
      // --- MODIFICATION : UN SEUL DIALOGUE AVEC OUI / NON ---
      
      String fontName = (this.fontAwesomeSolid != null) ? this.fontAwesomeSolid.getFontName() : "SansSerif";
      String message = "<html><div style='text-align: center; width:300px;'>" +
         "<font face=\"" + fontName + "\" size='5' color='#C0392B'>\uf12a</font><br>" +
         "<b style='font-size: 14px;'>Confirmation Finale</b><br><br>" +
         "Voulez-vous vraiment supprimer : <br><b>" + this.designation + "</b> ?<br><br>" +
         "<span style='font-size:10px; color:gray;'>Cette action ne peut pas être annulée.</span>" +
         "</div></html>";
      
      // Options personnalisées pour avoir "Oui" et "Non"
      Object[] options = {"Oui, supprimer", "Non, annuler"};
      
      int confirm = JOptionPane.showOptionDialog(
         this,
         message,
         "Confirmation de suppression",
         JOptionPane.YES_NO_OPTION,
         JOptionPane.WARNING_MESSAGE,
         null, // Pas d'icône par défaut, on l'a mise dans le HTML
         options,
         options[1] // Focus par défaut sur "Non" par sécurité
      );
      
      if (confirm == 0) { // 0 correspond à "Oui"
         try (Connection conn = ConnexionSQLite.getConnection();
              PreparedStatement pst = conn.prepareStatement("DELETE FROM Articles WHERE id = ?")) {
             
            pst.setInt(1, this.articleId);
            int rowsAffected = pst.executeUpdate();
             
            if (rowsAffected > 0) {
               JOptionPane.showMessageDialog(
                  this, 
                  "<html><div style='text-align: center;'>" +
                  "<font face=\"" + fontName + "\" size='6' color='#27AE60'>\uf058</font><br>" +
                  "<b>Article supprimé avec succès</b>" +
                  "</div></html>", 
                  "Succès", 
                  JOptionPane.PLAIN_MESSAGE // PLAIN car l'icône est dans le HTML
               );
               this.parent.rafraichirTableau();
               this.dispose();
            } else {
               JOptionPane.showMessageDialog(this, "Erreur: Article non trouvé.", "Erreur", JOptionPane.ERROR_MESSAGE);
            }
         } catch (SQLException e) {
            log.error("Erreur lors de la suppression de l'article", e);
            JOptionPane.showMessageDialog(this, "Erreur SQL: " + e.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
         }
      }
   }
}