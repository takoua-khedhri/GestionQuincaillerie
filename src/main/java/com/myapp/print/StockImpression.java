package com.myapp.print;

import com.myapp.db.ConnexionSQLite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StockImpression implements Printable {

    private static final Logger log = LoggerFactory.getLogger(StockImpression.class);

    private List<ArticleStock> articles;
    private ImageIcon logoIcon;
    private String dateImpression;

    private static final int MARGIN = 40;
    private static final int HEADER_HEIGHT = 150;
    private static final int ROW_HEIGHT = 20;
    private static final int FOOTER_HEIGHT = 40;
    private static final int TITLE_HEIGHT = 40;

    private DecimalFormat df;

    private static class ArticleStock {
        int id;
        String designation;
        int stock;

        ArticleStock(int id, String designation, int stock) {
            this.id = id;
            this.designation = designation;
            this.stock = stock;
        }
    }

    public StockImpression(ImageIcon logoIcon) {
        // ✅ Même logique que BLUI.chargerLogo()
        if (logoIcon != null) {
            this.logoIcon = logoIcon;
        } else {
            this.logoIcon = chargerLogo();
        }
        this.df = new DecimalFormat("#,##0", new DecimalFormatSymbols(Locale.FRANCE));
        this.dateImpression = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date());
        chargerDonneesStock();
    }

    // ✅ Copie exacte de chargerLogo() de BLUI
    private ImageIcon chargerLogo() {
        try {
            String[] paths = {"src/images/logo.png", "images/logo.png"};
            for (String p : paths) {
                File f = new File(p);
                if (f.exists()) {
                    return new ImageIcon(
                        new ImageIcon(f.getAbsolutePath())
                            .getImage()
                            .getScaledInstance(60, 60, Image.SCALE_SMOOTH)
                    );
                }
            }
        } catch (Exception e) {
            log.error("Logo non trouve: {}", e.getMessage());
        }
        return null;
    }

    private void chargerDonneesStock() {
        articles = new ArrayList<>();
        String query = "SELECT id, designation, stock FROM Articles ORDER BY id";
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(query);
             ResultSet rs = pst.executeQuery()) {
            while (rs.next()) {
                int id = rs.getInt("id");
                String designation = rs.getString("designation");
                int stock = rs.getInt("stock");
                articles.add(new ArticleStock(id, designation, stock));
            }
            log.info("Donnees de stock chargees: {} articles", articles.size());
        } catch (SQLException e) {
            log.error("Erreur lors du chargement des donnees de stock", e);
        }
    }

    private int getRowsPerPage(int pageHeight) {
        int availableHeight = pageHeight - HEADER_HEIGHT - FOOTER_HEIGHT - TITLE_HEIGHT - 50;
        return availableHeight / ROW_HEIGHT;
    }

    private int getTotalPages(int pageHeight) {
        if (articles.isEmpty()) return 0;
        int rowsPerPage = getRowsPerPage(pageHeight);
        return (int) Math.ceil((double) articles.size() / rowsPerPage);
    }

    @Override
    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {

        int totalPages = getTotalPages((int) pageFormat.getImageableHeight());

        if (pageIndex >= totalPages) {
            log.info("Page {}/{} - Pas de contenu supplementaire", pageIndex + 1, totalPages);
            return NO_SUCH_PAGE;
        }

        log.info("Impression page {}/{}", pageIndex + 1, totalPages);

        Graphics2D g2d = (Graphics2D) graphics;
        g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int contentWidth = (int) pageFormat.getImageableWidth() - 2 * MARGIN;
        int pageHeight = (int) pageFormat.getImageableHeight();
        int y = MARGIN;

        try {
            y = printHeader(g2d, MARGIN, y, contentWidth);
            y += 10;

            if (pageIndex == 0) {
                y = printTitle(g2d, MARGIN, y, contentWidth);
                y += 15;
            } else {
                y += 25;
            }

            if (pageIndex == 0) {
                y = printDateInfo(g2d, MARGIN, y, contentWidth);
                y += 15;
            }

            int rowsPerPage = getRowsPerPage(pageHeight);
            int startRow = pageIndex * rowsPerPage;
            int endRow = Math.min(startRow + rowsPerPage, articles.size());

            y = printArticlesTable(g2d, MARGIN, y, contentWidth, startRow, endRow);

            printFooter(g2d, MARGIN, pageHeight, contentWidth, pageIndex, totalPages);

        } catch (Exception e) {
            log.error("Erreur lors de l'impression du stock", e);
            throw new PrinterException("Erreur d'impression du stock: " + e.getMessage());
        }

        return PAGE_EXISTS;
    }

    private int printHeader(Graphics2D g2d, int margin, int y, int contentWidth) {
        int logoSize = 60;

        // ✅ Logo chargé depuis src/images/logo.png via chargerLogo()
        boolean logoPrinted = false;
        if (logoIcon != null) {
            try {
                Image logoImage = logoIcon.getImage();
                g2d.drawImage(logoImage, margin, y, logoSize, logoSize, null);
                logoPrinted = true;
            } catch (Exception e) {
                log.error("Erreur logo: {}", e.getMessage());
            }
        }

        if (!logoPrinted) {
            g2d.setColor(Color.LIGHT_GRAY);
            g2d.fillRect(margin, y, logoSize, logoSize);
            g2d.setColor(Color.DARK_GRAY);
            g2d.drawRect(margin, y, logoSize, logoSize);
            g2d.setFont(new Font("Arial", Font.BOLD, 10));
            g2d.drawString("LOGO", margin + 18, y + logoSize / 2 + 4);
        }

        // ✅ Infos société — chaque ligne sur sa propre coordonnée Y, sans chevauchement
        int infoX = margin + logoSize + 20;
        g2d.setColor(Color.BLACK);

        g2d.setFont(new Font("Arial", Font.BOLD, 20));
        g2d.drawString("CHAA ELECT", infoX, y + 20);

        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        g2d.drawString("VTE EN GROS MATERIEL ELECTRIQUE", infoX, y + 40);

        g2d.setFont(new Font("Arial", Font.PLAIN, 11));
        g2d.drawString("08, RUE 42500, Ezzahrouni, El Hrairia, Tunis, 2051", infoX, y + 58);
        g2d.drawString("Tél: 94 226 752", infoX, y + 73);
        g2d.drawString("Email: chaa.elec@outlook.fr", infoX, y + 88);
        g2d.drawString("MF: 000/M/A/1981916C", infoX, y + 103);

        return y + 110;
    }

    private int printTitle(Graphics2D g2d, int margin, int y, int contentWidth) {
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.BOLD, 22));
        String title = "ÉTAT DU STOCK JOURNALIER";
        int titleWidth = g2d.getFontMetrics().stringWidth(title);
        g2d.drawString(title, margin + (contentWidth - titleWidth) / 2, y);

        g2d.setColor(Color.BLACK);
        g2d.setStroke(new java.awt.BasicStroke(1f));
        g2d.drawLine(margin + 50, y + 5, margin + contentWidth - 50, y + 5);

        return y + 15;
    }

    private int printDateInfo(Graphics2D g2d, int margin, int y, int contentWidth) {
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.PLAIN, 11));
        String dateText = "Date d'édition: " + dateImpression;
        g2d.drawString(dateText, margin + contentWidth - 220, y);
        return y + 10;
    }

    private int printArticlesTable(Graphics2D g2d, int margin, int y, int contentWidth, int startRow, int endRow) {
        String[] headers = {"ID", "DÉSIGNATION", "QUANTITÉ"};
        int[] colWidths = {
            (int)(contentWidth * 0.12),
            (int)(contentWidth * 0.68),
            (int)(contentWidth * 0.20)
        };

        int headerHeight = 25;
        int rowHeight = 20;

        int x = margin;
        g2d.setColor(Color.BLACK);
        g2d.setStroke(new java.awt.BasicStroke(1f));

        for (int i = 0; i < headers.length; i++) {
            g2d.setColor(Color.LIGHT_GRAY);
            g2d.fillRect(x, y, colWidths[i], headerHeight);

            g2d.setColor(Color.BLACK);
            g2d.drawRect(x, y, colWidths[i], headerHeight);

            g2d.setFont(new Font("Arial", Font.BOLD, 11));
            int textWidth = g2d.getFontMetrics().stringWidth(headers[i]);
            int textX = x + (colWidths[i] - textWidth) / 2;
            g2d.drawString(headers[i], textX, y + 17);

            x += colWidths[i];
        }

        y += headerHeight;

        g2d.setFont(new Font("Arial", Font.PLAIN, 10));

        for (int row = startRow; row < endRow; row++) {
            ArticleStock article = articles.get(row);
            x = margin;

            if (row % 2 == 0) {
                g2d.setColor(Color.WHITE);
            } else {
                g2d.setColor(new Color(245, 245, 245));
            }
            g2d.fillRect(margin, y, contentWidth, rowHeight);

            g2d.setColor(Color.BLACK);

            g2d.drawLine(margin, y, margin + contentWidth, y);
            g2d.drawLine(margin, y + rowHeight, margin + contentWidth, y + rowHeight);

            int currentX = margin;
            for (int i = 0; i < headers.length; i++) {
                currentX += colWidths[i];
                g2d.drawLine(currentX, y, currentX, y + rowHeight);
            }

            String idStr = String.valueOf(article.id);
            int idWidth = g2d.getFontMetrics().stringWidth(idStr);
            g2d.drawString(idStr, margin + (colWidths[0] - idWidth) / 2, y + 14);

            String designation = article.designation;
            if (designation.length() > 35) {
                designation = designation.substring(0, 32) + "...";
            }
            g2d.drawString(designation, margin + colWidths[0] + 5, y + 14);

            String stockStr = df.format(article.stock);
            int stockWidth = g2d.getFontMetrics().stringWidth(stockStr);
            g2d.drawString(stockStr, margin + colWidths[0] + colWidths[1] + (colWidths[2] - stockWidth) / 2, y + 14);

            y += rowHeight;
        }

        return y + 10;
    }

    private void printFooter(Graphics2D g2d, int margin, int pageHeight, int contentWidth, int pageIndex, int totalPages) {
        int footerY = pageHeight - FOOTER_HEIGHT;

        g2d.setColor(Color.BLACK);
        g2d.setStroke(new java.awt.BasicStroke(0.5f));
        g2d.drawLine(margin, footerY - 10, margin + contentWidth, footerY - 10);

        g2d.setFont(new Font("Arial", Font.PLAIN, 9));
        g2d.drawString("Page " + (pageIndex + 1) + "/" + totalPages, margin + contentWidth - 60, footerY - 15);

        g2d.setFont(new Font("Arial", Font.ITALIC, 8));
        g2d.drawString("Document généré automatiquement le " + dateImpression, margin + 20, footerY);
    }

    public void imprimer() throws PrinterException {
        if (articles.isEmpty()) {
            JOptionPane.showMessageDialog(null,
                "Aucun article trouvé dans le stock à imprimer.",
                "Stock vide",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        log.info("Demarrage de l'impression de l'etat du stock...");
        log.info("Nombre d'articles: {}", articles.size());

        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName("État du Stock Journalier");
        job.setPrintable(this);

        if (job.printDialog()) {
            log.info("Impression confirmee par l'utilisateur");
            job.print();
            log.info("Impression terminee avec succes");
        } else {
            log.info("Impression annulee par l'utilisateur");
        }
    }
}