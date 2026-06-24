package com.myapp.ui;

import com.myapp.db.ConnexionSQLite;
import com.myapp.db.GestionBackup;
import com.myapp.print.StockImpression;
import com.myapp.ui.components.DashboardStatsPanel;
import com.myapp.ui.components.GlobalSearchBar;
import com.myapp.ui.components.KeyboardShortcutManager;
import com.myapp.ui.components.NotificationManager;
import com.myapp.ui.components.StockAlertChecker;
import com.myapp.util.AppTheme;
import com.myapp.util.SessionManager;
import com.myapp.util.UIUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import javax.swing.*;

public class AdminDashboard extends JFrame {

    private static final Logger log = LoggerFactory.getLogger(AdminDashboard.class);
    private Font fontAwesomeSolid;
    private final String userRole;

    public AdminDashboard() {
        this.userRole = SessionManager.getInstance().getUserRole();
        this.loadFontAwesome();
        this.setTitle("Espace " + getRoleLabel() + " - Systeme de Facturation");
        this.setExtendedState(JFrame.MAXIMIZED_BOTH);
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.setLocationRelativeTo(null);
        this.setMinimumSize(new Dimension(900, 600));
        this.setLayout(new BorderLayout(0, 0));
        this.getContentPane().setBackground(AppTheme.LIGHT);

        this.add(this.createHeaderPanel(), BorderLayout.NORTH);
        this.add(new JScrollPane(this.createMainPanel(),
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), BorderLayout.CENTER);
        this.add(this.createFooterPanel(), BorderLayout.SOUTH);

        this.registerKeyboardShortcuts();
        StockAlertChecker stockChecker = new StockAlertChecker();
        stockChecker.checkAndNotify(this);
        stockChecker.startPeriodicCheck(this, 15);
    }

    private String getRoleLabel() {
        if ("magasinier".equalsIgnoreCase(userRole)) return "Magasinier";
        return "Administrateur";
    }

    private boolean isAdmin() {
        return !"magasinier".equalsIgnoreCase(userRole);
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
            log.info("Fermeture : Sauvegarde en cours...");
            GestionBackup.effectuerBackup();
        } catch (Exception ex) {
            log.error("Erreur lors du backup", ex);
        } finally {
            System.exit(0);
        }
    }

    private void loadFontAwesome() {
        try (InputStream fontStream = this.getClass().getResourceAsStream("/fonts/fa.ttf")) {
            if (fontStream != null) {
                Font font = Font.createFont(Font.TRUETYPE_FONT, fontStream);
                GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
                this.fontAwesomeSolid = font;
            } else {
                this.fontAwesomeSolid = new Font("SansSerif", Font.PLAIN, 12);
            }
        } catch (Exception e) {
            log.warn("Impossible de charger FontAwesome", e);
            this.fontAwesomeSolid = new Font("SansSerif", Font.PLAIN, 12);
        }
    }

    private Font getFontAwesome(int size) {
        return (this.fontAwesomeSolid != null)
                ? this.fontAwesomeSolid.deriveFont(Font.PLAIN, (float) size)
                : new Font("SansSerif", Font.PLAIN, size);
    }

    private String getHtmlText(String iconCode, String text) {
        String fontName = (this.fontAwesomeSolid != null) ? this.fontAwesomeSolid.getFontName() : "SansSerif";
        return "<html><font face=\"" + fontName + "\">" + iconCode + "</font> " + text + "</html>";
    }

    // ==================== STATS ====================

    private String[] getDetailsRupture() {
        String articleName = "Stock OK";
        String subInfo = "Tout est disponible";
        int count = 0;
        try (Connection conn = ConnexionSQLite.getConnection();
             Statement stmt = conn.createStatement()) {
            try (ResultSet rsCount = stmt.executeQuery("SELECT COUNT(*) AS total FROM Articles WHERE stock = 0")) {
                if (rsCount.next()) count = rsCount.getInt("total");
            }
            if (count > 0) {
                try (ResultSet rsName = stmt.executeQuery("SELECT designation FROM Articles WHERE stock = 0 LIMIT 1")) {
                    if (rsName.next()) {
                        String nom = rsName.getString("designation");
                        articleName = nom.length() > 15 ? nom.substring(0, 12) + "..." : nom;
                    }
                }
                subInfo = (count == 1) ? "Rupture critique" : "(+ " + (count - 1) + " autres articles)";
            }
        } catch (Exception e) {
            log.error("Erreur chargement ruptures", e);
        }
        return new String[]{articleName, subInfo, String.valueOf(count)};
    }

    private String getTopArticleMois() {
        String article = "Aucune vente";
        String currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        String sql = "SELECT a.designation, SUM(lf.quantite) as total_qty FROM LigneFacture lf " +
                "JOIN Factures f ON lf.facture_id = f.id JOIN Articles a ON lf.article_id = a.id " +
                "WHERE strftime('%Y-%m', f.date_creation) = '" + currentMonth + "' " +
                "GROUP BY a.id ORDER BY total_qty DESC LIMIT 1";
        try (Connection conn = ConnexionSQLite.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                String nom = rs.getString("designation");
                article = nom.length() > 15 ? nom.substring(0, 12) + "..." : nom;
            }
        } catch (Exception e) {
            log.debug("Pas de donnees ventes pour le mois courant");
        }
        return article;
    }

    private void imprimerInventaire() {
        try {
            ImageIcon logoIcon = null;
            java.io.File f = new java.io.File("images/logo.jpg");
            if (!f.exists()) f = new java.io.File("src/images/logo.jpg");
            if (f.exists()) {
                logoIcon = new ImageIcon(new ImageIcon(f.getAbsolutePath())
                        .getImage().getScaledInstance(60, 60, Image.SCALE_SMOOTH));
            }
            new StockImpression(logoIcon).imprimer();
        } catch (Exception ex) {
            UIUtils.showErrorMessage(this, "Erreur impression: " + ex.getMessage());
        }
    }

    // ==================== PANELS ====================

    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 2, 0, AppTheme.LIGHT),
                BorderFactory.createEmptyBorder(20, 30, 20, 30)));

        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(Color.WHITE);
        JLabel titleLabel = new JLabel("Tableau de Bord");
        titleLabel.setFont(AppTheme.FONT_TITLE);
        titleLabel.setForeground(AppTheme.DARK);
        JLabel subTitleLabel = new JLabel("Gestion commerciale & Stock");
        subTitleLabel.setFont(AppTheme.FONT_SUBTITLE);
        subTitleLabel.setForeground(new Color(100, 100, 100));
        titlePanel.add(titleLabel, BorderLayout.NORTH);
        titlePanel.add(subTitleLabel, BorderLayout.CENTER);

        GlobalSearchBar searchBar = new GlobalSearchBar();
        searchBar.setPreferredSize(new Dimension(350, 36));

        JPanel centerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        centerPanel.setBackground(Color.WHITE);
        centerPanel.add(searchBar);
        headerPanel.add(centerPanel, BorderLayout.CENTER);

        JPanel userPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        userPanel.setBackground(Color.WHITE);
        String userName = SessionManager.getInstance().getCurrentUser();
        JLabel userLabel = new JLabel(getRoleLabel() + " : " + userName);
        userLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));

        JButton logoutBtn = new RoundButton(this.getHtmlText("", "Deconnexion"));
        logoutBtn.setFont(AppTheme.FONT_BUTTON);
        logoutBtn.setBackground(AppTheme.DANGER);
        logoutBtn.setForeground(Color.WHITE);
        logoutBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        logoutBtn.addActionListener(e -> {
            this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            try {
                GestionBackup.effectuerBackup();
            } catch (Exception ex) {
                log.error("Erreur backup", ex);
            }
            SessionManager.getInstance().endSession();
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
        mainPanel.setBackground(AppTheme.LIGHT);
        JPanel topSection = new JPanel(new BorderLayout(0, 20));
        topSection.setBackground(AppTheme.LIGHT);
        topSection.add(this.createStatsDashboard(), BorderLayout.NORTH);

        if (isAdmin()) {
            DashboardStatsPanel chartsPanel = new DashboardStatsPanel();
            chartsPanel.setPreferredSize(new Dimension(100, 350));
            topSection.add(chartsPanel, BorderLayout.CENTER);
        }

        mainPanel.add(topSection, BorderLayout.NORTH);

        int cols = isAdmin() ? 4 : 3;
        int rows = isAdmin() ? 2 : 2;
        JPanel cardsPanel = new JPanel(new GridLayout(rows, cols, 20, 20));
        cardsPanel.setBackground(AppTheme.LIGHT);

        cardsPanel.add(this.createDocumentsCard());
        cardsPanel.add(this.createFacturesCard());
        cardsPanel.add(this.createStockCard());

        if (isAdmin()) {
            cardsPanel.add(this.createAchatsCard());
        }

        cardsPanel.add(this.createGestionFournisseurCard());
        cardsPanel.add(this.createGestionClientCard());

        mainPanel.add(cardsPanel, BorderLayout.CENTER);
        return mainPanel;
    }

    private JPanel createStatsDashboard() {
        JPanel statsPanel = new JPanel(new GridLayout(1, 3, 20, 0));
        statsPanel.setBackground(AppTheme.LIGHT);
        statsPanel.setPreferredSize(new Dimension(100, 140));

        String[] ruptureData = this.getDetailsRupture();
        if (Integer.parseInt(ruptureData[2]) > 0) {
            statsPanel.add(new PulsingCard(AppTheme.DANGER, AppTheme.DANGER_DARK, "",
                    "EN RUPTURE :", ruptureData[0], ruptureData[1], true));
        } else {
            statsPanel.add(this.createServiceProStatCard("", "ETAT DU STOCK", "Stock OK",
                    "Tout est disponible", new Color[]{AppTheme.ACCENT, AppTheme.ACCENT_DARK}));
        }

        String topArticle = this.getTopArticleMois();
        String moisAnnee = LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRANCE));
        moisAnnee = moisAnnee.substring(0, 1).toUpperCase() + moisAnnee.substring(1);
        statsPanel.add(new PulsingCard(AppTheme.ACCENT, AppTheme.ACCENT_DARK, "",
                "TOP VENTE : " + moisAnnee, topArticle, "L'article le plus demande", true));

        JPanel btnInventaire = this.createActionCard("", "IMPRIMER STOCK",
                "Etat complet du stock", AppTheme.INFO, AppTheme.INFO_DARK);
        btnInventaire.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                imprimerInventaire();
            }
        });
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

    // ==================== CARDS ====================

    private JPanel createGestionFournisseurCard() {
        return createCard("", "FOURNISSEURS", AppTheme.FOURNISSEUR, AppTheme.INFO_DARK,
                new String[]{"Gestion fournisseurs"});
    }

    private JPanel createGestionClientCard() {
        return createCard("", "CLIENTS", AppTheme.CLIENT, AppTheme.ACCENT_DARK,
                new String[]{"Gestion clients"});
    }

    private JPanel createDocumentsCard() {
        return createCard("", "DOCUMENTS", AppTheme.DOCUMENTS, AppTheme.INFO_DARK,
                new String[]{"Bon de Livraison", "Bon de Sortie", "Devis"});
    }

    private JPanel createFacturesCard() {
        return createCard("", "FACTURES", AppTheme.FACTURES, AppTheme.ACCENT,
                new String[]{"Facture", "Liste facture", "Bon de commande"});
    }

    private JPanel createStockCard() {
        return createCard("", "STOCK", AppTheme.WARNING, new Color(243, 156, 18),
                new String[]{"Consulter Stock"});
    }

    private JPanel createAchatsCard() {
        return createCard("", "ACHATS", AppTheme.ACHATS, new Color(211, 84, 0),
                new String[]{"Facture fournisseur", "Liste fournisseurs", "Historique achats"});
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
        button.setForeground(AppTheme.DARK);
        button.setFont(AppTheme.FONT_BUTTON);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.addActionListener(e -> this.handleCardButtonAction(text));
        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent evt) {
                button.setBackground(Color.WHITE);
                button.setForeground(baseColor);
            }

            public void mouseExited(MouseEvent evt) {
                button.setBackground(new Color(255, 255, 255, 200));
                button.setForeground(AppTheme.DARK);
            }
        });
        return button;
    }

    private void handleCardButtonAction(String text) {
        switch (text) {
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
            case "Consulter Stock":
                new GestionStock().setVisible(true);
                this.dispose();
                break;
            case "Facture fournisseur":
                if (!isAdmin()) {
                    UIUtils.showInfoMessage(this, "Acces reserve a l'administrateur.");
                    return;
                }
                new FactureFournisseurUI().setVisible(true);
                this.dispose();
                break;
            case "Liste fournisseurs":
                new ListeFournisseurs().setVisible(true);
                this.dispose();
                break;
            case "Historique achats":
                if (!isAdmin()) {
                    UIUtils.showInfoMessage(this, "Acces reserve a l'administrateur.");
                    return;
                }
                new HistoriqueAchats().setVisible(true);
                this.dispose();
                break;
            case "Gestion fournisseurs":
                new ListeFournisseurs().setVisible(true);
                this.dispose();
                break;
            case "Gestion clients":
                new ListeClients().setVisible(true);
                this.dispose();
                break;
            default:
                UIUtils.showInfoMessage(this, "Module : " + text);
        }
    }

    private void registerKeyboardShortcuts() {
        KeyboardShortcutManager.registerDashboardShortcuts(this,
                () -> handleCardButtonAction("Consulter Stock"),
                () -> handleCardButtonAction("Facture"),
                () -> handleCardButtonAction("Bon de Livraison"),
                () -> handleCardButtonAction("Gestion clients"),
                () -> handleCardButtonAction("Gestion fournisseurs"),
                () -> handleCardButtonAction("Devis"),
                () -> {}
        );
    }

    private JPanel createFooterPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 30, 10, 30));
        panel.add(new JLabel("Systeme de Facturation v3.0"), BorderLayout.WEST);
        JLabel shortcuts = new JLabel("Ctrl+S Stock | Ctrl+F Facture | Ctrl+B BL | Ctrl+L Clients | Ctrl+D Devis");
        shortcuts.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        shortcuts.setForeground(new Color(160, 160, 160));
        panel.add(shortcuts, BorderLayout.EAST);
        return panel;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            AppTheme.init();
            new AdminDashboard().setVisible(true);
        });
    }

    // ==================== INNER CLASSES ====================

    class RoundButton extends JButton {
        public RoundButton(String text) {
            super(text);
            setContentAreaFilled(false);
            setBorderPainted(false);
        }

        @Override
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

        public GradientPanel(Color s, Color e) {
            this.startColor = s;
            this.endColor = e;
        }

        @Override
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
                    if (growing) {
                        scale += 0.002f;
                        if (scale >= 1.03f) growing = false;
                    } else {
                        scale -= 0.002f;
                        if (scale <= 1.0f) growing = true;
                    }
                    repaint();
                });
                timer.start();
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D) g;
            int w = getWidth();
            int h = getHeight();
            g2d.translate(w / 2.0, h / 2.0);
            g2d.scale(scale, scale);
            g2d.translate(-w / 2.0, -h / 2.0);
            GradientPaint gradient = new GradientPaint(0, 0, startColor, w, h, endColor);
            g2d.setPaint(gradient);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.fillRoundRect(0, 0, w, h, 15, 15);
        }

        @Override
        protected void paintChildren(Graphics g) {
            Graphics2D g2d = (Graphics2D) g;
            int w = getWidth();
            int h = getHeight();
            g2d.translate(w / 2.0, h / 2.0);
            g2d.scale(scale, scale);
            g2d.translate(-w / 2.0, -h / 2.0);
            super.paintChildren(g);
        }
    }
}
