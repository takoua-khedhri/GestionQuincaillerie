/* Decompiler 27ms, total 481ms, lines 135 */
package com.myapp.ui;

import com.myapp.db.ConnexionSQLite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SupprimerClient extends JDialog {
   private static final Logger log = LoggerFactory.getLogger(SupprimerClient.class);
   private JButton btnConfirmer;
   private JButton btnAnnuler;
   private ListeClients parent;
   private int clientId;
   private String nomClient;
   private String prenomClient;

   public SupprimerClient(ListeClients parent, int id, String nom, String prenom) {
      super(parent, "Supprimer le Client", true);
      this.parent = parent;
      this.clientId = id;
      this.nomClient = nom;
      this.prenomClient = prenom;
      this.setSize(400, 200);
      this.setLocationRelativeTo(parent);
      this.setLayout(new BorderLayout(10, 10));
      this.initUI();
   }

   private void initUI() {
      JPanel headerPanel = new JPanel(new BorderLayout());
      headerPanel.setBackground(new Color(231, 76, 60));
      headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
      JLabel titleLabel = new JLabel("\ud83d\uddd1️ SUPPRIMER LE CLIENT", 0);
      titleLabel.setFont(new Font("Segoe UI", 1, 16));
      titleLabel.setForeground(Color.WHITE);
      headerPanel.add(titleLabel, "Center");
      JPanel messagePanel = new JPanel(new BorderLayout());
      messagePanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
      messagePanel.setBackground(Color.WHITE);
      JLabel lblMessage = new JLabel("<html><center>Êtes-vous sûr de vouloir supprimer le client :<br><b>" + this.nomClient + " " + this.prenomClient + "</b> ?</center></html>", 0);
      lblMessage.setFont(new Font("Segoe UI", 0, 14));
      lblMessage.setForeground(new Color(100, 100, 100));
      messagePanel.add(lblMessage, "Center");
      JPanel buttonPanel = new JPanel(new FlowLayout(1, 15, 10));
      buttonPanel.setBackground(Color.WHITE);
      this.btnConfirmer = new JButton("Confirmer");
      this.btnConfirmer.setBackground(new Color(231, 76, 60));
      this.btnConfirmer.setForeground(Color.WHITE);
      this.btnConfirmer.setFocusPainted(false);
      this.btnConfirmer.setBorderPainted(false);
      this.btnConfirmer.setOpaque(true);
      this.btnAnnuler = new JButton("Annuler");
      this.btnAnnuler.setBackground(new Color(52, 152, 219));
      this.btnAnnuler.setForeground(Color.WHITE);
      this.btnAnnuler.setFocusPainted(false);
      this.btnAnnuler.setBorderPainted(false);
      this.btnAnnuler.setOpaque(true);
      buttonPanel.add(this.btnConfirmer);
      buttonPanel.add(this.btnAnnuler);
      this.add(headerPanel, "North");
      this.add(messagePanel, "Center");
      this.add(buttonPanel, "South");
      this.btnConfirmer.addActionListener((e) -> {
         this.confirmerSuppression();
      });
      this.btnAnnuler.addActionListener((e) -> {
         this.dispose();
      });
   }

   private void confirmerSuppression() {
      try {
         Connection conn = ConnexionSQLite.getConnection();

         try {
            PreparedStatement pst = conn.prepareStatement("DELETE FROM clients WHERE id = ?");

            try {
               pst.setInt(1, this.clientId);
               int rowsAffected = pst.executeUpdate();
               if (rowsAffected > 0) {
                  JOptionPane.showMessageDialog(this, "Client supprimé avec succès", "Succès", 1);
                  if (this.parent != null) {
                     this.parent.rafraichirListeClients();
                  }

                  this.dispose();
               }
            } catch (Throwable var7) {
               if (pst != null) {
                  try {
                     pst.close();
                  } catch (Throwable var6) {
                     var7.addSuppressed(var6);
                  }
               }

               throw var7;
            }

            if (pst != null) {
               pst.close();
            }
         } catch (Throwable var8) {
            if (conn != null) {
               try {
                  conn.close();
               } catch (Throwable var5) {
                  var8.addSuppressed(var5);
               }
            }

            throw var8;
         }

         if (conn != null) {
            conn.close();
         }
      } catch (SQLException var9) {
         log.error("Error deleting client id={}", this.clientId, var9);
         JOptionPane.showMessageDialog(this, "Erreur lors de la suppression: " + var9.getMessage(), "Erreur", 0);
      }

   }
}
