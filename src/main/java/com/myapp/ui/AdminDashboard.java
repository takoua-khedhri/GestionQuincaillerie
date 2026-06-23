package com.myapp.ui;

import com.myapp.db.ConnexionSQLite;
import com.myapp.db.GestionBackup;
import com.myapp.print.StockImpression;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.print.PrinterException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;

public class AdminDashboard extends JFrame {

    private final Color PRIMARY_COLOR = new Color(41, 128, 185);
    private final Color ACCENT_COLOR = new Color(46, 204, 113);
    private final Color ACCENT_DARK = new Color(39, 174, 96);
    private final Color DANGER_COLOR = new Color(231, 76, 60);
    private final Color DANGER_DARK = new Color(192, 57, 43);
    private final Color WARNING_COLOR = new Color(241, 196, 15);
    private final Color INFO_COLOR = new Color(52, 152, 219);
    private final Color INFO_DARK = new Color(41, 128, 185);
    private final Color PURPLE_COLOR = new Color(155, 89, 182);
    private final Color DARK_GRAY = new Color(44, 62, 80);
    private final Color LIGHT_GRAY = new Color(236, 240, 241);
    private final Color DOCUMENTS_COLOR = new Color(52, 73, 94);
    private final Color FACTURES_COLOR = new Color(39, 174, 96);
    private final Color CLIENTS_FOURNISSEURS_COLOR = new Color(142, 68, 173);
    private final Color FOURNISSEUR_COLOR = new Color(52, 152, 219);      // Nouvelle couleur pour fournisseurs
    private final Color CLIENT_COLOR = new Color(46, 204, 113);           // Nouvelle couleur pour clients
    private final Color ACHATS_COLOR = new Color(230, 126, 34);

    private Font fontAwesomeSolid;

    public AdminDashboard() {
        this.loadFontAwesome7();
        this.setTitle("Espace Administrateur - Système de Facturation");
        this.setExtendedState(JFrame.MAXIMIZED_BOTH);
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.setLocationRelativeTo(null);
        this.setLayout(new BorderLayout(0, 0));
        this.getContentPane().setBackground(this.LIGHT_GRAY);

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        this.add(this.createHeaderPanel(), BorderLayout.NORTH);
        this.add(this.createMainPanel(), BorderLayout.CENTER);
        this.add(this.createFooterPanel(), BorderLayout.SOUTH);
    }

    @Override
    protected void processWindowEvent(java.awt.event.WindowEvent e) {
        if (e.getID() == java.awt.event.WindowEvent.WINDOW_CLOSING) {
            effectuerBackupEtQuitter();
        } else {
            super.processWindowEvent(e);
        }
    }

    private void effectuerBackupEtQuitter() {
        this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
            System.out.println("🔄 Fermeture : Sauvegarde en cours...");
            GestionBackup.effectuerBackup(); 
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            System.exit(0);
        }
    }

    private void loadFontAwesome7() {
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
            this.fontAwesomeSolid = new Font("SansSerif", Font.PLAIN, 12);
        }
    }

    private Font getFontAwesome(int size) {
        return (this.fontAwesomeSolid != null) ? this.fontAwesomeSolid.deriveFont(Font.PLAIN, (float) size) : new Font("SansSerif", Font.PLAIN, size);
    }

    private String getHtmlText(String iconCode, String text) {
        String fontName = (this.fontAwesomeSolid != null) ? this.fontAwesomeSolid.getFontName() : "SansSerif";
        return "<html><font face=\"" + fontName + "\">" + iconCode + "</font> " + text + "</html>";
    }

    private String[] getDetailsRupture() {
        String articleName = "Stock OK";
        String subInfo = "Tout est disponible";
        int count = 0;
        try (Connection conn = ConnexionSQLite.getConnection(); Statement stmt = conn.createStatement()) {
            ResultSet rsCount = stmt.executeQuery("SELECT COUNT(*) AS total FROM Articles WHERE stock = 0");
            if (rsCount.next()) count = rsCount.getInt("total");
            rsCount.close();
            if (count > 0) {
                ResultSet rsName = stmt.executeQuery("SELECT designation FROM Articles WHERE stock = 0 LIMIT 1");
                if (rsName.next()) {
                    String nom = rsName.getString("designation");
                    articleName = nom.length() > 15 ? nom.substring(0, 12) + "..." : nom;
                }
                rsName.close();
                subInfo = (count == 1) ? "⚠️ Rupture critique" : "(+ " + (count - 1) + " autres articles)";
            }
        } catch (Exception e) { e.printStackTrace(); }
        return new String[] { articleName, subInfo, String.valueOf(count) };
    }

    private String getTopArticleMois() {
        String article = "Aucune vente";
        String currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        String sql = "SELECT a.designation, SUM(lf.quantite) as total_qty FROM LigneFacture lf " +
                "JOIN Factures f ON lf.facture_id = f.id JOIN Articles a ON lf.article_id = a.id " +
                "WHERE strftime('%Y-%m', f.date_creation) = '" + currentMonth + "' " +
                "GROUP BY a.id ORDER BY total_qty DESC LIMIT 1";
        try (Connection conn = ConnexionSQLite.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                String nom = rs.getString("designation");
                article = nom.length() > 15 ? nom.substring(0, 12) + "..." : nom;
            }
        } catch (Exception e) {}
        return article;
    }

    private void imprimerInventaire() {
        try {
            ImageIcon logoIcon = null;
            try {
                java.io.File f = new java.io.File("src/images/logo.jpg");
                if (!f.exists()) f = new java.io.File("images/logo.jpg");
                if (f.exists()) {
                    logoIcon = new javax.swing.ImageIcon(new javax.swing.ImageIcon(f.getAbsolutePath()).getImage().getScaledInstance(60, 60, java.awt.Image.SCALE_SMOOTH));
                }
            } catch (Exception e) {}
            new StockImpression(logoIcon).imprimer();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erreur impression: " + ex.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, this.LIGHT_GRAY), BorderFactory.createEmptyBorder(20, 30, 20, 30)));
        
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(Color.WHITE);
        JLabel titleLabel = new JLabel("Tableau de Bord");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        titleLabel.setForeground(this.DARK_GRAY);
        JLabel subTitleLabel = new JLabel("Gestion commerciale & Stock");
        subTitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subTitleLabel.setForeground(new Color(100, 100, 100));
        titlePanel.add(titleLabel, BorderLayout.NORTH);
        titlePanel.add(subTitleLabel, BorderLayout.CENTER);

        JPanel userPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        userPanel.setBackground(Color.WHITE);
        JLabel userLabel = new JLabel("Administrateur");
        userLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        JButton logoutBtn = new RoundButton(this.getHtmlText("\uf2f5", "Déconnexion"));
        logoutBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        logoutBtn.setBackground(this.DANGER_COLOR);
        logoutBtn.setForeground(Color.WHITE);
        logoutBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        logoutBtn.addActionListener(e -> {
            this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            try { GestionBackup.effectuerBackup(); } catch (Exception ex) { ex.printStackTrace(); }
            new LoginUI().setVisible(true);
            this.dispose();
        });
        userPanel.add(userLabel);
        userPanel.add(new JSeparator(JSeparator.VERTICAL));
        userPanel.add(logoutBtn);
        headerPanel.add(titlePanel, BorderLayout.WEST);
        headerPanel.add(userPanel, BorderLayout.EAST);
        return headerPanel;
    }

    private JPanel createMainPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout(20, 20));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
        mainPanel.setBackground(this.LIGHT_GRAY);
        mainPanel.add(this.createStatsDashboard(), BorderLayout.NORTH);
        
        // Disposition des cartes : 2 lignes, 4 colonnes pour 8 cartes
        JPanel cardsPanel = new JPanel(new GridLayout(2, 4, 20, 20));
        cardsPanel.setBackground(this.LIGHT_GRAY);
        cardsPanel.add(this.createDocumentsCard());
        cardsPanel.add(this.createFacturesCard());
        cardsPanel.add(this.createStockCard());
        cardsPanel.add(this.createAchatsCard());
        cardsPanel.add(this.createGestionFournisseurCard());      // Nouvelle carte Fournisseur
        cardsPanel.add(this.createGestionClientCard());           // Nouvelle carte Client
        
        mainPanel.add(cardsPanel, BorderLayout.CENTER);
        return mainPanel;
    }

    private JPanel createStatsDashboard() {
        JPanel statsPanel = new JPanel(new GridLayout(1, 3, 20, 0));
        statsPanel.setBackground(this.LIGHT_GRAY);
        statsPanel.setPreferredSize(new Dimension(100, 140));
        String[] ruptureData = this.getDetailsRupture();
        if (Integer.parseInt(ruptureData[2]) > 0) {
            statsPanel.add(new PulsingCard(this.DANGER_COLOR, this.DANGER_DARK, "\uf071", "EN RUPTURE :", ruptureData[0], ruptureData[1], true));
        } else {
            statsPanel.add(this.createServiceProStatCard("\uf058", "ÉTAT DU STOCK", "Stock OK", "Tout est disponible", new Color[] { new Color(46, 204, 113), new Color(39, 174, 96) }));
        }
        String topArticle = this.getTopArticleMois();
        String moisAnnee = LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRANCE));
        moisAnnee = moisAnnee.substring(0, 1).toUpperCase() + moisAnnee.substring(1);
        statsPanel.add(new PulsingCard(this.ACCENT_COLOR, this.ACCENT_DARK, "\uf091", "TOP VENTE : " + moisAnnee, topArticle, "L'article le plus demandé", true));
        JPanel btnInventaire = this.createActionCard("\uf02f", "IMPRIMER STOCK", "État complet du stock", this.INFO_COLOR, this.INFO_DARK);
        btnInventaire.addMouseListener(new MouseAdapter() { @Override public void mouseClicked(MouseEvent e) { imprimerInventaire(); }});
        statsPanel.add(btnInventaire);
        return statsPanel;
    }

    private JPanel createServiceProStatCard(String icon, String title, String value, String subValue, Color[] gradient) {
        GradientPanel card = new GradientPanel(gradient[0], gradient[1]);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(this.getFontAwesome(24));
        iconLabel.setForeground(new Color(255, 255, 255, 200));
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        titleLabel.setForeground(Color.WHITE);
        headerPanel.add(iconLabel, BorderLayout.WEST);
        headerPanel.add(titleLabel, BorderLayout.CENTER);
        JLabel valueLabel = new JLabel(value, JLabel.RIGHT);
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        valueLabel.setForeground(Color.WHITE);
        JLabel subValueLabel = new JLabel(subValue);
        subValueLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        subValueLabel.setForeground(new Color(255, 255, 255, 200));
        subValueLabel.setHorizontalAlignment(JLabel.RIGHT);
        card.add(headerPanel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        card.add(subValueLabel, BorderLayout.SOUTH);
        return card;
    }

    private JPanel createActionCard(String icon, String title, String subtitle, Color c1, Color c2) {
        GradientPanel card = new GradientPanel(c1, c2);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));
        JLabel iconLabel = new JLabel(icon, JLabel.CENTER);
        iconLabel.setFont(this.getFontAwesome(32));
        iconLabel.setForeground(Color.WHITE);
        JPanel textPanel = new JPanel(new GridLayout(2, 1));
        textPanel.setOpaque(false);
        JLabel titleLabel = new JLabel(title, JLabel.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLabel.setForeground(Color.WHITE);
        JLabel subLabel = new JLabel(subtitle, JLabel.CENTER);
        subLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subLabel.setForeground(new Color(255, 255, 255, 220));
        textPanel.add(titleLabel);
        textPanel.add(subLabel);
        card.add(iconLabel, BorderLayout.CENTER);
        card.add(textPanel, BorderLayout.SOUTH);
        return card;
    }

    // Carte pour la gestion des fournisseurs (uniquement gestion)
    private JPanel createGestionFournisseurCard() {
        return createCard("\uf2b5", "FOURNISSEURS", this.FOURNISSEUR_COLOR, new Color(41, 128, 185), 
            new String[] { "Gestion fournisseurs" });
    }

    // Carte pour la gestion des clients (uniquement gestion)
    private JPanel createGestionClientCard() {
        return createCard("\uf007", "CLIENTS", this.CLIENT_COLOR, new Color(39, 174, 96), 
            new String[] { "Gestion clients" });
    }

    private JPanel createDocumentsCard() {
        return createCard("\uf15c", "DOCUMENTS", this.DOCUMENTS_COLOR, new Color(41, 128, 185), 
            new String[] { "Bon de Livraison", "Bon de Sortie", "Devis" });
    }

    private JPanel createFacturesCard() {
        return createCard("\uf15b", "FACTURES", this.FACTURES_COLOR, new Color(46, 204, 113), 
            new String[] { "Facture", "Liste facture", "Bon de commande" });
    }

    // STOCK CARD modifiée : "Consulter Stock" uniquement
    private JPanel createStockCard() {
        return createCard("\uf466", "STOCK", this.WARNING_COLOR, new Color(243, 156, 18), 
            new String[] { "Consulter Stock" });
    }

    private JPanel createAchatsCard() {
        return createCard("\uf07a", "ACHATS", this.ACHATS_COLOR, new Color(211, 84, 0), 
            new String[] { "Facture fournisseur", "Liste fournisseurs", "Historique achats" });
    }

    private JPanel createCard(String icon, String title, Color c1, Color c2, String[] buttons) {
        GradientPanel card = new GradientPanel(c1, c2);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        header.setOpaque(false);
        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(this.getFontAwesome(20));
        iconLabel.setForeground(Color.WHITE);
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLabel.setForeground(Color.WHITE);
        header.add(iconLabel);
        header.add(titleLabel);
        JPanel buttonPanel = new JPanel(new GridLayout(buttons.length, 1, 8, 8));
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));
        for (String text : buttons) {
            buttonPanel.add(this.createCardButton(text, c2));
        }
        card.add(header, BorderLayout.NORTH);
        card.add(buttonPanel, BorderLayout.CENTER);
        return card;
    }

    private JButton createCardButton(String text, final Color baseColor) {
        final JButton button = new RoundButton(text);
        button.setBackground(new Color(255, 255, 255, 200));
        button.setForeground(this.DARK_GRAY);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.addActionListener(e -> this.handleCardButtonAction(text));
        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent evt) { button.setBackground(Color.WHITE); button.setForeground(baseColor); }
            public void mouseExited(MouseEvent evt) { button.setBackground(new Color(255, 255, 255, 200)); button.setForeground(DARK_GRAY); }
        });
        return button;
    }

    private void handleCardButtonAction(String text) {
        switch (text) {
            // DOCUMENTS
            case "Bon de Livraison":
                new BLUI().setVisible(true);
                this.dispose();
                break;
            case "Bon de Sortie":
                new BonSortieUI().setVisible(true);
                this.dispose();
                break;
            case "Devis":
                new DevisUI().setVisible(true);
                this.dispose();
                break;
            
            // FACTURES
            case "Facture":
                new FactureUI().setVisible(true);
                this.dispose();
                break;
            case "Liste facture":
                new ListeFactures().setVisible(true);
                this.dispose();
                break;
            case "Bon de commande":
                new BonCommandeUI().setVisible(true);
                this.dispose();
                break;
            
            // STOCK
            case "Consulter Stock":
                new GestionStock().setVisible(true);
                this.dispose();
                break;
            
            // ACHATS
            case "Facture fournisseur":
                new FactureFournisseurUI().setVisible(true);
                this.dispose();
                break;
            case "Liste fournisseurs":
                new ListeFournisseurs().setVisible(true);
                this.dispose();
                break;
            case "Historique achats":
                new HistoriqueAchats().setVisible(true);
                this.dispose();
                break;
            
            // GESTION FOURNISSEURS
            case "Gestion fournisseurs":
                new ListeFournisseurs().setVisible(true);
                this.dispose();
                break;
          
            // GESTION CLIENTS
            case "Gestion clients":
                new ListeClients().setVisible(true);
                this.dispose();
                break;
           
            default:
                JOptionPane.showMessageDialog(this, "Module : " + text, "Info", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private JPanel createFooterPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 30, 10, 30));
        panel.add(new JLabel("Système de Facturation v2.1"), BorderLayout.WEST);
        return panel;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AdminDashboard().setVisible(true));
    }

    class RoundButton extends JButton {
        public RoundButton(String text) { super(text); setContentAreaFilled(false); setBorderPainted(false); }
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getModel().isPressed() ? getBackground().darker() : getBackground());
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
            super.paintComponent(g);
            g2.dispose();
        }
    }

    class GradientPanel extends JPanel {
        protected Color startColor, endColor;
        public GradientPanel(Color s, Color e) { this.startColor = s; this.endColor = e; }
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setPaint(new GradientPaint(0, 0, startColor, getWidth(), getHeight(), endColor));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
        }
    }

    class PulsingCard extends GradientPanel {
        private float scale = 1.0f;
        private boolean growing = true;
        public PulsingCard(Color start, Color end, String icon, String title, String mainValue, String subValue, boolean animate) {
            super(start, end);
            this.setLayout(new BorderLayout());
            this.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
            JPanel headerPanel = new JPanel(new BorderLayout());
            headerPanel.setOpaque(false);
            JLabel iconLabel = new JLabel(icon);
            iconLabel.setFont(AdminDashboard.this.getFontAwesome(24));
            iconLabel.setForeground(Color.WHITE);
            JLabel titleLabel = new JLabel(title);
            titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
            titleLabel.setForeground(Color.WHITE);
            headerPanel.add(iconLabel, BorderLayout.WEST);
            headerPanel.add(titleLabel, BorderLayout.CENTER);
            JLabel valueLabel = new JLabel(mainValue, JLabel.CENTER);
            valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
            valueLabel.setForeground(Color.WHITE);
            JLabel subLabel = new JLabel(subValue, JLabel.CENTER);
            subLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
            subLabel.setForeground(new Color(255, 255, 255, 220));
            this.add(headerPanel, BorderLayout.NORTH);
            this.add(valueLabel, BorderLayout.CENTER);
            this.add(subLabel, BorderLayout.SOUTH);
            if (animate) {
                Timer timer = new Timer(60, e -> {
                    if (growing) { scale += 0.002f; if (scale >= 1.03f) growing = false; } 
                    else { scale -= 0.002f; if (scale <= 1.0f) growing = true; }
                    repaint();
                });
                timer.start();
            }
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D) g;
            int w = getWidth(); int h = getHeight();
            g2d.translate(w / 2.0, h / 2.0); g2d.scale(scale, scale); g2d.translate(-w / 2.0, -h / 2.0);
            GradientPaint gradient = new GradientPaint(0, 0, startColor, w, h, endColor);
            g2d.setPaint(gradient);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.fillRoundRect(0, 0, w, h, 15, 15);
        }
        @Override protected void paintChildren(Graphics g) {
            Graphics2D g2d = (Graphics2D) g;
            int w = getWidth(); int h = getHeight();
            g2d.translate(w / 2.0, h / 2.0); g2d.scale(scale, scale); g2d.translate(-w / 2.0, -h / 2.0);
            super.paintChildren(g);
        }
    }
}