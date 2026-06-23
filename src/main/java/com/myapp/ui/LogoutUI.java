/* Decompiler 94ms, total 707ms, lines 181 */
package com.myapp.ui;

import com.myapp.util.SessionManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogoutUI extends JDialog {
   private static final Logger log = LoggerFactory.getLogger(LogoutUI.class);
   private JButton btnConfirmLogout;
   private JButton btnCancel;
   private boolean logoutConfirmed = false;

   public LogoutUI(JFrame parent) {
      super(parent, "Déconnexion", true);
      this.setSize(400, 300);
      this.setLocationRelativeTo(parent);
      this.setResizable(false);
      this.setLayout(new BorderLayout());
      this.setDefaultCloseOperation(2);
      this.initUI();
   }

   private void initUI() {
      JPanel mainPanel = new JPanel(new BorderLayout(0, 20));
      mainPanel.setBackground(new Color(248, 250, 252));
      mainPanel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
      JPanel headerPanel = this.createHeaderPanel();
      mainPanel.add(headerPanel, "North");
      JPanel messagePanel = this.createMessagePanel();
      mainPanel.add(messagePanel, "Center");
      JPanel buttonPanel = this.createButtonPanel();
      mainPanel.add(buttonPanel, "South");
      this.add(mainPanel);
   }

   private JPanel createHeaderPanel() {
      JPanel panel = new JPanel(new BorderLayout());
      panel.setBackground(new Color(248, 250, 252));
      JLabel lblIcon = new JLabel("\ud83d\udeaa");
      lblIcon.setFont(new Font("Segoe UI", 0, 48));
      lblIcon.setHorizontalAlignment(0);
      lblIcon.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
      JLabel lblTitle = new JLabel("Déconnexion", 0);
      lblTitle.setFont(new Font("Segoe UI", 1, 22));
      lblTitle.setForeground(new Color(44, 62, 80));
      panel.add(lblIcon, "North");
      panel.add(lblTitle, "Center");
      return panel;
   }

   private JPanel createMessagePanel() {
      JPanel panel = new JPanel(new GridLayout(3, 1, 10, 10));
      panel.setBackground(new Color(248, 250, 252));
      panel.setBorder(BorderFactory.createEmptyBorder(10, 0, 20, 0));
      SessionManager session = SessionManager.getInstance();
      String currentUser = session.getCurrentUser();
      String sessionDuration = session.getSessionDuration();
      JLabel lblMessage1 = new JLabel("Êtes-vous sûr de vouloir vous déconnecter ?", 0);
      lblMessage1.setFont(new Font("Segoe UI", 0, 14));
      lblMessage1.setForeground(new Color(60, 60, 60));
      JLabel lblMessage2 = new JLabel("Utilisateur: " + currentUser, 0);
      lblMessage2.setFont(new Font("Segoe UI", 1, 12));
      lblMessage2.setForeground(new Color(52, 152, 219));
      JLabel lblMessage3 = new JLabel("Durée de session: " + sessionDuration, 0);
      lblMessage3.setFont(new Font("Segoe UI", 0, 12));
      lblMessage3.setForeground(new Color(120, 120, 120));
      panel.add(lblMessage1);
      panel.add(lblMessage2);
      panel.add(lblMessage3);
      return panel;
   }

   private JPanel createButtonPanel() {
      JPanel panel = new JPanel(new GridLayout(1, 2, 15, 0));
      panel.setBackground(new Color(248, 250, 252));
      panel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
      this.btnCancel = this.createStyledButton("❌ Annuler", new Color(149, 165, 166));
      this.btnConfirmLogout = this.createStyledButton("\ud83d\udeaa Se déconnecter", new Color(231, 76, 60));
      panel.add(this.btnCancel);
      panel.add(this.btnConfirmLogout);
      this.btnCancel.addActionListener((e) -> {
         this.logoutConfirmed = false;
         this.dispose();
      });
      this.btnConfirmLogout.addActionListener((e) -> {
         this.logoutConfirmed = true;
         this.dispose();
      });
      this.setupKeyBindings();
      return panel;
   }

   private JButton createStyledButton(String text, final Color backgroundColor) {
      final JButton button = new JButton(text);
      button.setFont(new Font("Segoe UI", 1, 13));
      button.setBackground(backgroundColor);
      button.setForeground(Color.WHITE);
      button.setFocusPainted(false);
      button.setBorderPainted(false);
      button.setOpaque(true);
      button.setCursor(new Cursor(12));
      button.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(this.darkenColor(backgroundColor, 0.8F), 1), BorderFactory.createEmptyBorder(10, 15, 10, 15)));
      button.addMouseListener(new MouseAdapter() {
         public void mouseEntered(MouseEvent evt) {
            button.setBackground(LogoutUI.this.darkenColor(backgroundColor, 0.9F));
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

   private void setupKeyBindings() {
      this.getRootPane().getInputMap(2).put(KeyStroke.getKeyStroke("ESCAPE"), "cancel");
      this.getRootPane().getActionMap().put("cancel", new AbstractAction() {
         public void actionPerformed(ActionEvent e) {
            LogoutUI.this.logoutConfirmed = false;
            LogoutUI.this.dispose();
         }
      });
      this.getRootPane().getInputMap(2).put(KeyStroke.getKeyStroke("ENTER"), "confirm");
      this.getRootPane().getActionMap().put("confirm", new AbstractAction() {
         public void actionPerformed(ActionEvent e) {
            LogoutUI.this.logoutConfirmed = true;
            LogoutUI.this.dispose();
         }
      });
   }

   public boolean isLogoutConfirmed() {
      return this.logoutConfirmed;
   }

   public static boolean showLogoutDialog(JFrame parent) {
      LogoutUI logoutDialog = new LogoutUI(parent);
      logoutDialog.setVisible(true);
      return logoutDialog.isLogoutConfirmed();
   }

   public static void performLogout(JFrame currentFrame) {
      boolean confirm = showLogoutDialog(currentFrame);
      if (confirm) {
         SessionManager session = SessionManager.getInstance();
         String user = session.getCurrentUser();
         String sessionDuration = session.getSessionDuration();
         session.endSession();
         currentFrame.dispose();
         JOptionPane.showMessageDialog((Component)null, "Au revoir " + user + " !\nSession: " + sessionDuration + "\nVous avez été déconnecté avec succès.", "Déconnexion réussie", 1);
         SwingUtilities.invokeLater(() -> {
            (new LoginUI()).setVisible(true);
         });
      }

   }
}
