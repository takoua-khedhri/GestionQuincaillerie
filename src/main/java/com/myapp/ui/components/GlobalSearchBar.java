package com.myapp.ui.components;

import com.myapp.db.ConnexionSQLite;
import com.myapp.ui.GestionStock;
import com.myapp.ui.ListeClients;
import com.myapp.ui.ListeFournisseurs;
import com.myapp.util.AppTheme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Barre de recherche globale permettant de rechercher des articles, clients et fournisseurs.
 */
public class GlobalSearchBar extends JPanel {

    private static final Logger log = LoggerFactory.getLogger(GlobalSearchBar.class);

    private static final int MAX_RESULTS_PER_CATEGORY = 10;
    private static final int DEBOUNCE_DELAY_MS = 300;

    private final JTextField searchField;
    private JWindow popupWindow;
    private JPanel resultsPanel;
    private Timer debounceTimer;

    public GlobalSearchBar() {
        setLayout(new BorderLayout());
        setOpaque(false);

        // Search icon label
        JLabel searchIcon = new JLabel(" 🔍 ");
        searchIcon.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        searchIcon.setForeground(AppTheme.DARK);

        // Styled search field with placeholder
        searchField = new JTextField() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getText().isEmpty() && !hasFocus()) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                            RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    g2.setColor(Color.GRAY);
                    g2.setFont(AppTheme.FONT_FIELD);
                    Insets insets = getInsets();
                    g2.drawString("Rechercher un article, client, fournisseur...",
                            insets.left + 2, getHeight() / 2 + g2.getFontMetrics().getAscent() / 2 - 1);
                    g2.dispose();
                }
            }
        };
        searchField.setFont(AppTheme.FONT_FIELD);
        searchField.setPreferredSize(new Dimension(350, 34));
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.LIGHT, 1),
                new EmptyBorder(4, 8, 4, 8)
        ));

        // Container panel for icon + field
        JPanel container = new JPanel(new BorderLayout());
        container.setBackground(Color.WHITE);
        container.setBorder(BorderFactory.createLineBorder(AppTheme.PRIMARY, 1, true));
        container.add(searchIcon, BorderLayout.WEST);
        container.add(searchField, BorderLayout.CENTER);

        add(container, BorderLayout.CENTER);

        // Debounce timer
        debounceTimer = new Timer(DEBOUNCE_DELAY_MS, e -> performSearch(searchField.getText().trim()));
        debounceTimer.setRepeats(false);

        // Key listener for search input
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    hidePopup();
                    searchField.setText("");
                } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    selectFirstResult();
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() != KeyEvent.VK_ESCAPE && e.getKeyCode() != KeyEvent.VK_ENTER) {
                    debounceTimer.restart();
                }
            }
        });

        // Hide popup when focus is lost
        searchField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                // Small delay to allow click on result
                Timer hideTimer = new Timer(200, ev -> {
                    if (!searchField.hasFocus()) {
                        hidePopup();
                    }
                });
                hideTimer.setRepeats(false);
                hideTimer.start();
                searchField.repaint(); // Repaint for placeholder
            }

            @Override
            public void focusGained(FocusEvent e) {
                searchField.repaint();
            }
        });
    }

    private void performSearch(String query) {
        if (query.isEmpty()) {
            hidePopup();
            return;
        }

        log.debug("Recherche globale : {}", query);

        List<SearchResult> articles = searchArticles(query);
        List<SearchResult> clients = searchClients(query);
        List<SearchResult> fournisseurs = searchFournisseurs(query);

        if (articles.isEmpty() && clients.isEmpty() && fournisseurs.isEmpty()) {
            hidePopup();
            return;
        }

        showResults(articles, clients, fournisseurs);
    }

    private List<SearchResult> searchArticles(String query) {
        List<SearchResult> results = new ArrayList<>();
        String sql = "SELECT id, designation, stock, prixVente FROM articles WHERE designation LIKE ? LIMIT ?";
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + query + "%");
            ps.setInt(2, MAX_RESULTS_PER_CATEGORY);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(new SearchResult(
                            rs.getInt("id"),
                            rs.getString("designation"),
                            "Stock: " + rs.getInt("stock") + " | Prix: " + String.format("%.2f", rs.getDouble("prixVente")),
                            SearchCategory.ARTICLE
                    ));
                }
            }
        } catch (SQLException e) {
            log.error("Erreur recherche articles", e);
        }
        return results;
    }

    private List<SearchResult> searchClients(String query) {
        List<SearchResult> results = new ArrayList<>();
        String sql = "SELECT id, nom, prenom, telephone FROM clients WHERE nom LIKE ? OR prenom LIKE ? LIMIT ?";
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + query + "%");
            ps.setString(2, "%" + query + "%");
            ps.setInt(3, MAX_RESULTS_PER_CATEGORY);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(new SearchResult(
                            rs.getInt("id"),
                            rs.getString("nom") + " " + rs.getString("prenom"),
                            "Tél: " + rs.getString("telephone"),
                            SearchCategory.CLIENT
                    ));
                }
            }
        } catch (SQLException e) {
            log.error("Erreur recherche clients", e);
        }
        return results;
    }

    private List<SearchResult> searchFournisseurs(String query) {
        List<SearchResult> results = new ArrayList<>();
        String sql = "SELECT id, nom, telephone FROM fournisseurs WHERE nom LIKE ? LIMIT ?";
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + query + "%");
            ps.setInt(2, MAX_RESULTS_PER_CATEGORY);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(new SearchResult(
                            rs.getInt("id"),
                            rs.getString("nom"),
                            "Tél: " + rs.getString("telephone"),
                            SearchCategory.FOURNISSEUR
                    ));
                }
            }
        } catch (SQLException e) {
            log.error("Erreur recherche fournisseurs", e);
        }
        return results;
    }

    private void showResults(List<SearchResult> articles, List<SearchResult> clients, List<SearchResult> fournisseurs) {
        hidePopup();

        Window ancestor = SwingUtilities.getWindowAncestor(this);
        if (ancestor == null) return;

        popupWindow = new JWindow(ancestor);
        resultsPanel = new JPanel();
        resultsPanel.setLayout(new BoxLayout(resultsPanel, BoxLayout.Y_AXIS));
        resultsPanel.setBackground(Color.WHITE);
        resultsPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.PRIMARY, 1),
                new EmptyBorder(4, 0, 4, 0)
        ));

        if (!articles.isEmpty()) {
            addCategoryHeader("Articles", AppTheme.PRIMARY);
            articles.forEach(this::addResultItem);
        }
        if (!clients.isEmpty()) {
            addCategoryHeader("Clients", AppTheme.CLIENT);
            clients.forEach(this::addResultItem);
        }
        if (!fournisseurs.isEmpty()) {
            addCategoryHeader("Fournisseurs", AppTheme.FOURNISSEUR);
            fournisseurs.forEach(this::addResultItem);
        }

        JScrollPane scrollPane = new JScrollPane(resultsPanel);
        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        int height = Math.min(resultsPanel.getPreferredSize().height + 10, 400);
        scrollPane.setPreferredSize(new Dimension(searchField.getWidth(), height));

        popupWindow.setContentPane(scrollPane);
        Point loc = searchField.getLocationOnScreen();
        popupWindow.setLocation(loc.x, loc.y + searchField.getHeight());
        popupWindow.pack();
        popupWindow.setSize(searchField.getWidth(), Math.min(popupWindow.getHeight(), 400));
        popupWindow.setVisible(true);
    }

    private void addCategoryHeader(String title, Color color) {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(color.brighter().brighter());
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        header.setBorder(new EmptyBorder(4, 10, 4, 10));

        JLabel label = new JLabel(title);
        label.setFont(AppTheme.FONT_LABEL);
        label.setForeground(color.darker());
        header.add(label, BorderLayout.WEST);

        resultsPanel.add(header);
    }

    private void addResultItem(SearchResult result) {
        JPanel item = new JPanel(new BorderLayout());
        item.setBackground(Color.WHITE);
        item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        item.setBorder(new EmptyBorder(4, 16, 4, 10));
        item.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel nameLabel = new JLabel(result.name);
        nameLabel.setFont(AppTheme.FONT_TABLE);
        nameLabel.setForeground(AppTheme.DARK);

        JLabel detailLabel = new JLabel(result.detail);
        detailLabel.setFont(AppTheme.FONT_SUBTITLE);
        detailLabel.setForeground(Color.GRAY);

        item.add(nameLabel, BorderLayout.WEST);
        item.add(detailLabel, BorderLayout.EAST);

        // Hover effect
        item.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                item.setBackground(AppTheme.LIGHT);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                item.setBackground(Color.WHITE);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                openResult(result);
            }
        });

        resultsPanel.add(item);
    }

    private void selectFirstResult() {
        if (resultsPanel == null) return;
        for (Component comp : resultsPanel.getComponents()) {
            if (comp instanceof JPanel) {
                JPanel panel = (JPanel) comp;
                // Skip category headers (they have a colored background)
                if (panel.getBackground().equals(Color.WHITE)) {
                    panel.getMouseListeners()[panel.getMouseListeners().length - 1]
                            .mouseClicked(new MouseEvent(panel, MouseEvent.MOUSE_CLICKED,
                                    System.currentTimeMillis(), 0, 0, 0, 1, false));
                    return;
                }
            }
        }
    }

    private void openResult(SearchResult result) {
        hidePopup();
        searchField.setText("");

        SwingUtilities.invokeLater(() -> {
            try {
                switch (result.category) {
                    case ARTICLE:
                        new GestionStock().setVisible(true);
                        break;
                    case CLIENT:
                        new ListeClients().setVisible(true);
                        break;
                    case FOURNISSEUR:
                        new ListeFournisseurs().setVisible(true);
                        break;
                }
            } catch (Exception e) {
                log.error("Erreur ouverture résultat : {}", result.name, e);
            }
        });
    }

    private void hidePopup() {
        if (popupWindow != null) {
            popupWindow.setVisible(false);
            popupWindow.dispose();
            popupWindow = null;
            resultsPanel = null;
        }
    }

    // --- Inner types ---

    private enum SearchCategory {
        ARTICLE, CLIENT, FOURNISSEUR
    }

    private static class SearchResult {
        final int id;
        final String name;
        final String detail;
        final SearchCategory category;

        SearchResult(int id, String name, String detail, SearchCategory category) {
            this.id = id;
            this.name = name;
            this.detail = detail;
            this.category = category;
        }
    }
}
