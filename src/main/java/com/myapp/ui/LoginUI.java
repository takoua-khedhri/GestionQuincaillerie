package com.myapp.ui;

import com.myapp.db.ConnexionSQLite;
import com.myapp.db.GestionBackup;
import com.myapp.util.AppTheme;
import com.myapp.util.SessionManager;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.Border;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoginUI extends JFrame {

    private static final Logger logger = LoggerFactory.getLogger(LoginUI.class);

    // Composants UI
    private JTextField txtNom;
    private JPasswordField txtMotDePasse;
    private JButton btnConnexion;
    private JLabel lblMessage;
    private Font fontAwesomeSolid;

    public LoginUI() {
        // 1. Chargement de la police d'icônes
        this.loadFontAwesome();

        this.setTitle("Système de Facturation - Connexion");
        this.setExtendedState(JFrame.MAXIMIZED_BOTH);

        // On intercepte la fermeture pour faire le backup
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        this.setLocationRelativeTo(null);
        this.setLayout(new BorderLayout());

        // Vérification si une session existe déjà
        SessionManager session = SessionManager.getInstance();
        if (session.isSessionActive()) {
            this.redirectToDashboard();
        } else {
            this.initUI();
        }
    }

    // ==================== GESTION FERMETURE & BACKUP ====================

    @Override
    protected void processWindowEvent(java.awt.event.WindowEvent e) {
        if (e.getID() == java.awt.event.WindowEvent.WINDOW_CLOSING) {
            quitterApplicationAvecBackup();
        } else {
            super.processWindowEvent(e);
        }
    }

    private void quitterApplicationAvecBackup() {
        this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        try {
            logger.info("Fermeture de l'application : Lancement du backup...");
            boolean succes = GestionBackup.effectuerBackup();

            if (succes) {
                logger.info("Backup terminé avec succès avant fermeture.");
            } else {
                logger.warn("Le backup a rencontré un problème, mais l'app va se fermer.");
            }
        } catch (Exception ex) {
            logger.error("Erreur lors du backup à la fermeture", ex);
        } finally {
            System.exit(0);
        }
    }

    // ==================== UI & LOGIQUE ====================

    private void loadFontAwesome() {
        try (InputStream fontStream = this.getClass().getResourceAsStream("/fonts/fa.ttf")) {
            if (fontStream != null) {
                Font font = Font.createFont(Font.TRUETYPE_FONT, fontStream);
                GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
                ge.registerFont(font);
                this.fontAwesomeSolid = font;
            } else {
                logger.warn("Police FontAwesome introuvable, utilisation de la police par défaut");
                this.fontAwesomeSolid = new Font("SansSerif", Font.PLAIN, 12);
            }
        } catch (Exception e) {
            logger.error("Erreur lors du chargement de FontAwesome", e);
            this.fontAwesomeSolid = new Font("SansSerif", Font.PLAIN, 12);
        }
    }

    private void initUI() {
        // Fond Dégradé
        JPanel panelPrincipal = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                GradientPaint gp = new GradientPaint(0.0F, 0.0F, AppTheme.PRIMARY,
                        (float) this.getWidth(), (float) this.getHeight(), AppTheme.DARK);
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, this.getWidth(), this.getHeight());
            }
        };
        panelPrincipal.setLayout(new GridBagLayout());

        JPanel contentPanel = this.createContentPanel();

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0D;
        gbc.weighty = 1.0D;
        gbc.anchor = GridBagConstraints.CENTER;
        panelPrincipal.add(contentPanel, gbc);

        this.add(panelPrincipal, BorderLayout.CENTER);

        // Listeners
        this.btnConnexion.addActionListener((e) -> this.verifierConnexion());
        this.txtMotDePasse.addActionListener((e) -> this.verifierConnexion());
        this.txtNom.addActionListener((e) -> this.verifierConnexion());

        SwingUtilities.invokeLater(() -> this.txtNom.requestFocus());
    }

    private JPanel createContentPanel() {
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new GridBagLayout());
        contentPanel.setBackground(Color.WHITE);

        Border line = BorderFactory.createLineBorder(new Color(200, 200, 200), 1);
        Border empty = BorderFactory.createEmptyBorder(40, 50, 40, 50);
        contentPanel.setBorder(BorderFactory.createCompoundBorder(line, empty));

        contentPanel.setMinimumSize(new Dimension(350, 500));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.weightx = 1.0;

        // 1. TITRE
        JLabel lblTitre = new JLabel("CONNEXION", JLabel.CENTER);
        lblTitre.setFont(new Font("Segoe UI", Font.BOLD, 26));
        lblTitre.setForeground(AppTheme.DARK);

        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 5, 0);
        contentPanel.add(lblTitre, gbc);

        JLabel lblSousTitre = new JLabel("Espace Utilisateur", JLabel.CENTER);
        lblSousTitre.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblSousTitre.setForeground(new Color(150, 150, 150));

        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 40, 0);
        contentPanel.add(lblSousTitre, gbc);

        // 2. CHAMP NOM
        JLabel lblNom = new JLabel("Nom d'utilisateur");
        lblNom.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblNom.setForeground(new Color(100, 100, 100));

        gbc.gridy = 2;
        gbc.insets = new Insets(0, 0, 5, 0);
        contentPanel.add(lblNom, gbc);

        JPanel userFieldPanel = new JPanel(new BorderLayout());
        userFieldPanel.setBackground(Color.WHITE);
        userFieldPanel.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));

        JLabel userIcon = new JLabel("");
        userIcon.setFont(this.fontAwesomeSolid != null ? this.fontAwesomeSolid.deriveFont(16f) : new Font("SansSerif", Font.PLAIN, 16));
        userIcon.setForeground(Color.GRAY);
        userIcon.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 5));

        this.txtNom = new JTextField();
        this.txtNom.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        this.txtNom.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 10));

        userFieldPanel.add(userIcon, BorderLayout.WEST);
        userFieldPanel.add(this.txtNom, BorderLayout.CENTER);

        gbc.gridy = 3;
        gbc.insets = new Insets(0, 0, 20, 0);
        contentPanel.add(userFieldPanel, gbc);

        // 3. CHAMP PASSWORD
        JLabel lblPwd = new JLabel("Mot de passe");
        lblPwd.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblPwd.setForeground(new Color(100, 100, 100));

        gbc.gridy = 4;
        gbc.insets = new Insets(0, 0, 5, 0);
        contentPanel.add(lblPwd, gbc);

        JPanel pwdFieldPanel = new JPanel(new BorderLayout());
        pwdFieldPanel.setBackground(Color.WHITE);
        pwdFieldPanel.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));

        JLabel pwdIcon = new JLabel("");
        pwdIcon.setFont(this.fontAwesomeSolid != null ? this.fontAwesomeSolid.deriveFont(16f) : new Font("SansSerif", Font.PLAIN, 16));
        pwdIcon.setForeground(Color.GRAY);
        pwdIcon.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 5));

        this.txtMotDePasse = new JPasswordField();
        this.txtMotDePasse.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        this.txtMotDePasse.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 10));

        pwdFieldPanel.add(pwdIcon, BorderLayout.WEST);
        pwdFieldPanel.add(this.txtMotDePasse, BorderLayout.CENTER);

        gbc.gridy = 5;
        gbc.insets = new Insets(0, 0, 30, 0);
        contentPanel.add(pwdFieldPanel, gbc);

        // 4. BOUTON
        this.btnConnexion = new JButton("SE CONNECTER");
        this.btnConnexion.setFont(new Font("Segoe UI", Font.BOLD, 14));
        this.btnConnexion.setBackground(AppTheme.INFO);
        this.btnConnexion.setForeground(Color.WHITE);
        this.btnConnexion.setFocusPainted(false);
        this.btnConnexion.setBorderPainted(false);
        this.btnConnexion.setCursor(new Cursor(Cursor.HAND_CURSOR));
        this.btnConnexion.setBorder(BorderFactory.createEmptyBorder(12, 0, 12, 0));

        this.btnConnexion.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent evt) {
                LoginUI.this.btnConnexion.setBackground(AppTheme.INFO_DARK);
            }

            public void mouseExited(MouseEvent evt) {
                LoginUI.this.btnConnexion.setBackground(AppTheme.INFO);
            }
        });

        gbc.gridy = 6;
        gbc.insets = new Insets(0, 0, 20, 0);
        contentPanel.add(this.btnConnexion, gbc);

        // 5. MESSAGE ERREUR
        this.lblMessage = new JLabel("", JLabel.CENTER);
        this.lblMessage.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        this.lblMessage.setForeground(AppTheme.DANGER);

        gbc.gridy = 7;
        gbc.insets = new Insets(0, 0, 10, 0);
        contentPanel.add(this.lblMessage, gbc);

        // 6. VERSION
        JLabel lblInfo = new JLabel("Système de Facturation v2.1", JLabel.CENTER);
        lblInfo.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblInfo.setForeground(new Color(200, 200, 200));

        gbc.gridy = 8;
        gbc.insets = new Insets(10, 0, 0, 0);
        contentPanel.add(lblInfo, gbc);

        return contentPanel;
    }

    private void verifierConnexion() {
        String nom = this.txtNom.getText().trim();
        String motDePasse = new String(this.txtMotDePasse.getPassword());

        this.btnConnexion.setText("CONNEXION...");
        this.btnConnexion.setBackground(new Color(100, 100, 100));
        this.btnConnexion.setEnabled(false);

        // Petit timer pour l'effet visuel
        Timer timer = new Timer(500, (e) -> {
            if (!nom.isEmpty() && !motDePasse.isEmpty()) {
                try (Connection conn = ConnexionSQLite.getConnection();
                     PreparedStatement pst = conn.prepareStatement(
                             "SELECT nom, role FROM admin WHERE nom = ? AND pswd = ?")) {

                    pst.setString(1, nom);
                    pst.setString(2, motDePasse);

                    try (ResultSet rs = pst.executeQuery()) {
                        if (rs.next()) {
                            String role = rs.getString("role");
                            SessionManager.getInstance().startSession(nom, role);

                            this.lblMessage.setText("Connexion réussie !");
                            this.lblMessage.setForeground(AppTheme.ACCENT);

                            Timer redirectTimer = new Timer(500, (evt) -> this.redirectToDashboard());
                            redirectTimer.setRepeats(false);
                            redirectTimer.start();
                        } else {
                            this.lblMessage.setText("Identifiants incorrects");
                            this.lblMessage.setForeground(AppTheme.DANGER);
                            this.resetBoutonConnexion();
                        }
                    }
                } catch (Exception ex) {
                    logger.error("Erreur lors de la vérification des identifiants", ex);
                    this.lblMessage.setText("Erreur Base de Données");
                    this.resetBoutonConnexion();
                }
            } else {
                this.lblMessage.setText("Veuillez remplir tous les champs");
                this.resetBoutonConnexion();
            }
        });
        timer.setRepeats(false);
        timer.start();
    }

    private void redirectToDashboard() {
        String role = SessionManager.getInstance().getRole();
        if ("magasinier".equalsIgnoreCase(role)) {
            // MagasinierDashboard sera créé ultérieurement, utiliser AdminDashboard en attendant
            new AdminDashboard().setVisible(true);
        } else {
            new AdminDashboard().setVisible(true);
        }
        this.dispose();
    }

    private void resetBoutonConnexion() {
        this.btnConnexion.setText("SE CONNECTER");
        this.btnConnexion.setBackground(AppTheme.INFO);
        this.btnConnexion.setEnabled(true);
    }

    public static void main(String[] args) {
        AppTheme.init();
        com.myapp.db.DatabaseMigration.migrate();
        SwingUtilities.invokeLater(() -> {
            new LoginUI().setVisible(true);
        });
    }
}
