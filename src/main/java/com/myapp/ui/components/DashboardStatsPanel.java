package com.myapp.ui.components;

import com.myapp.db.ConnexionSQLite;
import com.myapp.util.AppTheme;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class DashboardStatsPanel extends JPanel {

    private static final Logger log = LoggerFactory.getLogger(DashboardStatsPanel.class);

    private JLabel lblVentesJour;
    private JLabel lblVentesMois;
    private JLabel lblFacturesImpayees;
    private JLabel lblArticlesRupture;

    private ChartPanel barChartPanel;
    private ChartPanel pieChartPanel;

    private static final String[] FRENCH_MONTHS = {
            "Janvier", "Fevrier", "Mars", "Avril", "Mai", "Juin",
            "Juillet", "Aout", "Septembre", "Octobre", "Novembre", "Decembre"
    };

    public DashboardStatsPanel() {
        setLayout(new BorderLayout(0, 15));
        setOpaque(false);
        setBorder(new EmptyBorder(10, 10, 10, 10));

        add(createStatsRow(), BorderLayout.NORTH);
        add(createChartsRow(), BorderLayout.CENTER);

        refreshData();
    }

    // ==================== ROW 1 - Summary Cards ====================

    private JPanel createStatsRow() {
        JPanel row = new JPanel(new GridLayout(1, 4, 15, 0));
        row.setOpaque(false);
        row.setPreferredSize(new Dimension(100, 110));

        lblVentesJour = new JLabel("0.00 DA");
        lblVentesMois = new JLabel("0.00 DA");
        lblFacturesImpayees = new JLabel("0");
        lblArticlesRupture = new JLabel("0");

        row.add(createStatCard("", AppTheme.ACCENT, lblVentesJour, "Ventes Aujourd'hui"));
        row.add(createStatCard("", AppTheme.INFO, lblVentesMois, "Ventes du Mois"));
        row.add(createStatCard("", AppTheme.DANGER, lblFacturesImpayees, "Factures Impayees"));
        row.add(createStatCard("", AppTheme.WARNING, lblArticlesRupture, "Articles en Rupture"));

        return row;
    }

    private JPanel createStatCard(String icon, Color color, JLabel valueLabel, String labelText) {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color darker = AppTheme.darken(color, 0.8f);
                g2.setPaint(new GradientPaint(0, 0, color, getWidth(), getHeight(), darker));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
                g2.dispose();
            }
        };
        card.setLayout(new BorderLayout(8, 5));
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(15, 20, 15, 20));

        JLabel iconLabel = new JLabel(icon, SwingConstants.CENTER);
        iconLabel.setFont(new Font("Segoe UI", Font.PLAIN, 28));
        iconLabel.setForeground(new Color(255, 255, 255, 200));
        card.add(iconLabel, BorderLayout.WEST);

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        valueLabel.setForeground(Color.WHITE);
        valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        textPanel.add(valueLabel);

        JLabel label = new JLabel(labelText);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        label.setForeground(new Color(255, 255, 255, 220));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        textPanel.add(label);

        card.add(textPanel, BorderLayout.CENTER);
        return card;
    }

    // ==================== ROW 2 - Charts ====================

    private JPanel createChartsRow() {
        JPanel row = new JPanel(new GridLayout(1, 2, 15, 0));
        row.setOpaque(false);

        barChartPanel = new ChartPanel(createEmptyBarChart());
        barChartPanel.setOpaque(false);
        barChartPanel.setBackground(new Color(0, 0, 0, 0));
        barChartPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        pieChartPanel = new ChartPanel(createEmptyPieChart());
        pieChartPanel.setOpaque(false);
        pieChartPanel.setBackground(new Color(0, 0, 0, 0));
        pieChartPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        row.add(barChartPanel);
        row.add(pieChartPanel);
        return row;
    }

    // ==================== Data Loading ====================

    public void refreshData() {
        try (Connection conn = ConnexionSQLite.getConnection()) {
            loadStatCards(conn);
            loadBarChart(conn);
            loadPieChart(conn);
        } catch (Exception e) {
            log.error("Erreur lors du chargement des statistiques du dashboard", e);
        }
    }

    private void loadStatCards(Connection conn) {
        String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        String todayFr = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        String monthStart = LocalDate.now().withDayOfMonth(1).format(DateTimeFormatter.ISO_LOCAL_DATE);
        String currentYearMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        String currentMonthFr = LocalDate.now().format(DateTimeFormatter.ofPattern("MM/yyyy"));

        // Ventes Aujourd'hui: sum(net_a_payer) from Factures today - sum(montant_ttc) from Avoirs today
        double ventesJour = 0;
        try (Statement stmt = conn.createStatement()) {
            String sql = "SELECT COALESCE(SUM(net_a_payer), 0) FROM Factures " +
                    "WHERE date = '" + today + "' OR date = '" + todayFr + "'";
            try (ResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) ventesJour = rs.getDouble(1);
            }
        } catch (Exception e) {
            log.error("Erreur chargement ventes du jour", e);
        }

        double avoirsJour = 0;
        try (Statement stmt = conn.createStatement()) {
            String sql = "SELECT COALESCE(SUM(montant_ttc), 0) FROM Avoirs " +
                    "WHERE date_avoir = '" + today + "' OR date_avoir = '" + todayFr + "'";
            try (ResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) avoirsJour = rs.getDouble(1);
            }
        } catch (Exception e) {
            log.debug("Table Avoirs non disponible ou erreur", e);
        }
        lblVentesJour.setText(String.format("%.2f DA", ventesJour - avoirsJour));

        // Ventes du Mois: same logic for current month
        double ventesMois = 0;
        try (Statement stmt = conn.createStatement()) {
            String sql = "SELECT COALESCE(SUM(net_a_payer), 0) FROM Factures " +
                    "WHERE substr(date, 1, 7) = '" + currentYearMonth + "' " +
                    "OR substr(date, 4, 7) = '" + currentMonthFr + "'";
            try (ResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) ventesMois = rs.getDouble(1);
            }
        } catch (Exception e) {
            log.error("Erreur chargement ventes du mois", e);
        }

        double avoirsMois = 0;
        try (Statement stmt = conn.createStatement()) {
            String sql = "SELECT COALESCE(SUM(montant_ttc), 0) FROM Avoirs " +
                    "WHERE substr(date_avoir, 1, 7) = '" + currentYearMonth + "' " +
                    "OR substr(date_avoir, 4, 7) = '" + currentMonthFr + "'";
            try (ResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) avoirsMois = rs.getDouble(1);
            }
        } catch (Exception e) {
            log.debug("Table Avoirs non disponible ou erreur pour le mois", e);
        }
        lblVentesMois.setText(String.format("%.2f DA", ventesMois - avoirsMois));

        // Factures Impayees
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT COUNT(*) FROM Factures WHERE statut_paiement != 'PAYE'")) {
            if (rs.next()) lblFacturesImpayees.setText(String.valueOf(rs.getInt(1)));
        } catch (Exception e) {
            log.error("Erreur chargement factures impayees", e);
        }

        // Articles en Rupture (stock <= stock_min)
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT COUNT(*) FROM Articles WHERE stock <= stock_min")) {
            if (rs.next()) lblArticlesRupture.setText(String.valueOf(rs.getInt(1)));
        } catch (Exception e) {
            log.error("Erreur chargement articles en rupture", e);
        }
    }

    private void loadBarChart(Connection conn) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        LocalDate now = LocalDate.now();

        for (int i = 5; i >= 0; i--) {
            LocalDate month = now.minusMonths(i);
            String yearMonth = month.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            String label = FRENCH_MONTHS[month.getMonthValue() - 1] + " " + month.getYear();

            try (Statement stmt = conn.createStatement()) {
                String sql = "SELECT COALESCE(SUM(net_a_payer), 0) FROM Factures " +
                        "WHERE substr(date, 1, 7) = '" + yearMonth + "'";
                try (ResultSet rs = stmt.executeQuery(sql)) {
                    if (rs.next()) {
                        dataset.addValue(rs.getDouble(1), "Ventes", label);
                    }
                }
            } catch (Exception e) {
                log.error("Erreur chargement ventes mois " + yearMonth, e);
                dataset.addValue(0, "Ventes", label);
            }
        }

        JFreeChart chart = ChartFactory.createBarChart(
                "Ventes des 6 derniers mois", null, "Montant (DA)", dataset,
                PlotOrientation.VERTICAL, false, true, false);
        styleBarChart(chart);
        barChartPanel.setChart(chart);
    }

    private void loadPieChart(Connection conn) {
        DefaultPieDataset dataset = new DefaultPieDataset();
        String currentYearMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));

        try (Statement stmt = conn.createStatement()) {
            String sql = "SELECT df.article_designation, SUM(df.quantite) AS total_qty " +
                    "FROM DetailsFacture df " +
                    "JOIN Factures f ON df.facture_id = f.id " +
                    "WHERE substr(f.date, 1, 7) = '" + currentYearMonth + "' " +
                    "GROUP BY df.article_designation " +
                    "ORDER BY total_qty DESC LIMIT 5";
            try (ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    dataset.setValue(rs.getString("article_designation"), rs.getInt("total_qty"));
                }
            }
        } catch (Exception e) {
            log.error("Erreur chargement top articles", e);
        }

        JFreeChart chart = ChartFactory.createPieChart(
                "Top 5 Articles Vendus", dataset, true, true, false);
        stylePieChart(chart);
        pieChartPanel.setChart(chart);
    }

    // ==================== Chart Styling ====================

    private JFreeChart createEmptyBarChart() {
        JFreeChart chart = ChartFactory.createBarChart(
                "Ventes des 6 derniers mois", null, "Montant (DA)",
                new DefaultCategoryDataset(), PlotOrientation.VERTICAL, false, true, false);
        styleBarChart(chart);
        return chart;
    }

    private JFreeChart createEmptyPieChart() {
        JFreeChart chart = ChartFactory.createPieChart(
                "Top 5 Articles Vendus", new DefaultPieDataset(), true, true, false);
        stylePieChart(chart);
        return chart;
    }

    private void styleBarChart(JFreeChart chart) {
        chart.setBackgroundPaint(null);
        chart.getTitle().setFont(new Font("Segoe UI", Font.BOLD, 14));
        chart.getTitle().setPaint(AppTheme.DARK);
        chart.setBorderVisible(false);

        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setOutlineVisible(false);
        plot.setRangeGridlinePaint(new Color(220, 220, 220));

        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setSeriesPaint(0, AppTheme.PRIMARY);
        renderer.setDrawBarOutline(false);
        renderer.setShadowVisible(false);

        plot.getDomainAxis().setTickLabelFont(new Font("Segoe UI", Font.PLAIN, 10));
        plot.getRangeAxis().setTickLabelFont(new Font("Segoe UI", Font.PLAIN, 10));
    }

    @SuppressWarnings("unchecked")
    private void stylePieChart(JFreeChart chart) {
        chart.setBackgroundPaint(null);
        chart.getTitle().setFont(new Font("Segoe UI", Font.BOLD, 14));
        chart.getTitle().setPaint(AppTheme.DARK);
        chart.setBorderVisible(false);

        PiePlot plot = (PiePlot) chart.getPlot();
        plot.setBackgroundPaint(null);
        plot.setOutlineVisible(false);
        plot.setShadowPaint(null);
        plot.setLabelFont(new Font("Segoe UI", Font.PLAIN, 11));

        Color[] palette = {AppTheme.ACCENT, AppTheme.INFO, AppTheme.DANGER, AppTheme.WARNING, AppTheme.PURPLE};
        java.util.List<?> keys = plot.getDataset().getKeys();
        for (int i = 0; i < keys.size() && i < palette.length; i++) {
            plot.setSectionPaint((Comparable<?>) keys.get(i), palette[i]);
        }
    }
}
