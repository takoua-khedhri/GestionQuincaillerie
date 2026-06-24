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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DashboardStatsPanel extends JPanel {

    private static final Logger log = LoggerFactory.getLogger(DashboardStatsPanel.class);

    private JLabel lblTotalArticles;
    private JLabel lblChiffreAffaires;
    private JLabel lblNbFactures;
    private JLabel lblRuptures;

    private ChartPanel barChartPanel;
    private ChartPanel pieChartPanel;

    public DashboardStatsPanel() {
        setLayout(new BorderLayout(0, 15));
        setOpaque(false);
        setBorder(new EmptyBorder(10, 10, 10, 10));

        add(createStatsRow(), BorderLayout.NORTH);
        add(createChartsRow(), BorderLayout.CENTER);

        refreshData();
    }

    private JPanel createStatsRow() {
        JPanel row = new JPanel(new GridLayout(1, 4, 15, 0));
        row.setOpaque(false);

        lblTotalArticles = new JLabel("0");
        lblChiffreAffaires = new JLabel("0.00 DA");
        lblNbFactures = new JLabel("0");
        lblRuptures = new JLabel("0");

        row.add(createStatCard("📦", AppTheme.PRIMARY, lblTotalArticles, "Articles en stock"));
        row.add(createStatCard("💰", AppTheme.ACCENT, lblChiffreAffaires, "Chiffre d'affaires du mois"));
        row.add(createStatCard("📄", AppTheme.INFO, lblNbFactures, "Factures du mois"));
        row.add(createStatCard("⚠", AppTheme.DANGER, lblRuptures, "Articles en rupture"));

        return row;
    }

    private JPanel createStatCard(String icon, Color color, JLabel valueLabel, String labelText) {
        JPanel card = new JPanel(new BorderLayout(10, 0));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220)),
                new EmptyBorder(18, 18, 18, 18)
        ));

        // Icon area
        JLabel iconLabel = new JLabel(icon, SwingConstants.CENTER);
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 28));
        iconLabel.setOpaque(true);
        iconLabel.setBackground(new Color(color.getRed(), color.getGreen(), color.getBlue(), 30));
        iconLabel.setPreferredSize(new Dimension(56, 56));
        card.add(iconLabel, BorderLayout.WEST);

        // Text area
        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        valueLabel.setForeground(AppTheme.DARK);
        valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        textPanel.add(valueLabel);

        JLabel label = new JLabel(labelText);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        label.setForeground(Color.GRAY);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        textPanel.add(label);

        card.add(textPanel, BorderLayout.CENTER);
        return card;
    }

    private JPanel createChartsRow() {
        JPanel row = new JPanel(new GridLayout(1, 2, 15, 0));
        row.setOpaque(false);

        barChartPanel = new ChartPanel(createEmptyBarChart());
        barChartPanel.setOpaque(false);
        barChartPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220)),
                new EmptyBorder(10, 10, 10, 10)
        ));

        pieChartPanel = new ChartPanel(createEmptyPieChart());
        pieChartPanel.setOpaque(false);
        pieChartPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220)),
                new EmptyBorder(10, 10, 10, 10)
        ));

        row.add(barChartPanel);
        row.add(pieChartPanel);
        return row;
    }

    public void refreshData() {
        try (Connection conn = ConnexionSQLite.getConnection()) {
            loadStatCards(conn);
            loadBarChart(conn);
            loadPieChart(conn);
        } catch (SQLException e) {
            log.error("Erreur lors du chargement des statistiques du dashboard", e);
        }
    }

    private void loadStatCards(Connection conn) throws SQLException {
        // Total articles in stock
        try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM articles");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                lblTotalArticles.setText(String.valueOf(rs.getInt(1)));
            }
        }

        // Chiffre d'affaires du mois
        String monthStart = LocalDate.now().withDayOfMonth(1).format(DateTimeFormatter.ISO_LOCAL_DATE);
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COALESCE(SUM(montant_total), 0) FROM factures WHERE date_creation >= ?")) {
            ps.setString(1, monthStart);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    lblChiffreAffaires.setText(String.format("%.2f DA", rs.getDouble(1)));
                }
            }
        }

        // Nombre de factures du mois
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM factures WHERE date_creation >= ?")) {
            ps.setString(1, monthStart);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    lblNbFactures.setText(String.valueOf(rs.getInt(1)));
                }
            }
        }

        // Articles en rupture (stock = 0)
        try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM articles WHERE stock = 0");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                lblRuptures.setText(String.valueOf(rs.getInt(1)));
            }
        }
    }

    private void loadBarChart(Connection conn) throws SQLException {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        LocalDate now = LocalDate.now();

        for (int i = 5; i >= 0; i--) {
            LocalDate month = now.minusMonths(i);
            String start = month.withDayOfMonth(1).format(DateTimeFormatter.ISO_LOCAL_DATE);
            String end = month.withDayOfMonth(month.lengthOfMonth()).format(DateTimeFormatter.ISO_LOCAL_DATE);
            String label = month.format(DateTimeFormatter.ofPattern("MMM yyyy"));

            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COALESCE(SUM(montant_total), 0) FROM factures WHERE date_creation >= ? AND date_creation <= ?")) {
                ps.setString(1, start);
                ps.setString(2, end);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        dataset.addValue(rs.getDouble(1), "Ventes", label);
                    }
                }
            }
        }

        JFreeChart chart = ChartFactory.createBarChart(
                "Ventes par mois", null, "Montant (DA)", dataset,
                PlotOrientation.VERTICAL, false, true, false);
        styleBarChart(chart);
        barChartPanel.setChart(chart);
    }

    private void loadPieChart(Connection conn) throws SQLException {
        DefaultPieDataset dataset = new DefaultPieDataset();

        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT a.designation, SUM(lf.quantite) AS total_qty " +
                "FROM ligneFacture lf " +
                "JOIN articles a ON a.id = lf.article_id " +
                "GROUP BY a.id ORDER BY total_qty DESC LIMIT 5");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                dataset.setValue(rs.getString("designation"), rs.getInt("total_qty"));
            }
        }

        JFreeChart chart = ChartFactory.createPieChart(
                "Top 5 articles vendus", dataset, true, true, false);
        stylePieChart(chart);
        pieChartPanel.setChart(chart);
    }

    private JFreeChart createEmptyBarChart() {
        JFreeChart chart = ChartFactory.createBarChart(
                "Ventes par mois", null, "Montant (DA)",
                new DefaultCategoryDataset(), PlotOrientation.VERTICAL, false, true, false);
        styleBarChart(chart);
        return chart;
    }

    private JFreeChart createEmptyPieChart() {
        JFreeChart chart = ChartFactory.createPieChart(
                "Top 5 articles vendus", new DefaultPieDataset(), true, true, false);
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

        Color[] palette = {AppTheme.PRIMARY, AppTheme.ACCENT, AppTheme.WARNING, AppTheme.PURPLE, AppTheme.ACHATS};
        java.util.List<?> keys = plot.getDataset().getKeys();
        for (int i = 0; i < keys.size() && i < palette.length; i++) {
            plot.setSectionPaint((Comparable<?>) keys.get(i), palette[i]);
        }
    }
}
