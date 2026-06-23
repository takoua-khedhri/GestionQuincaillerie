package com.myapp.print;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import javax.swing.ImageIcon;
import javax.swing.table.DefaultTableModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FactureImpressionTicket implements Printable {

    private static final Logger log = LoggerFactory.getLogger(FactureImpressionTicket.class);

    private String numeroTicket;
    private String dateTicket;
    private String nomClient;
    private DefaultTableModel model;
    private ImageIcon logoIcon;
    
    private double totalHT;
    private double totalTTC;
    private double montantRemise;

    private DecimalFormat df3;

    private final double PAPER_WIDTH = 220;
    private final double MARGIN = 10;

    public FactureImpressionTicket(String numeroTicket, String dateTicket, 
                                   String nomClient, DefaultTableModel model, 
                                   ImageIcon logoIcon, String modePaiement,
                                   double totalHT, double totalTTC, double montantRemise, double timbre) {
        
        this.numeroTicket = numeroTicket;
        this.dateTicket = dateTicket;
        this.nomClient = nomClient;
        this.model = model;
        this.logoIcon = logoIcon;
        
        this.totalHT = totalHT;
        this.totalTTC = totalTTC;
        this.montantRemise = montantRemise;

        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.FRANCE);
        symbols.setGroupingSeparator(' ');
        symbols.setDecimalSeparator(',');
        this.df3 = new DecimalFormat("#,##0.000", symbols);
        df3.setGroupingUsed(true);
        df3.setGroupingSize(3);
    }

    @Override
    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
        if (pageIndex > 0) return NO_SUCH_PAGE;

        Graphics2D g2d = (Graphics2D) graphics;
        g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());

        int y = 5; 
        int width = (int) (pageFormat.getImageableWidth());

        Font fontHeader = new Font("SansSerif", Font.BOLD, 12);
        Font fontBold = new Font("Monospaced", Font.BOLD, 10);
        Font fontRegular = new Font("Monospaced", Font.PLAIN, 9);
        Font fontSmall = new Font("SansSerif", Font.PLAIN, 8);
        Font fontMontant = new Font("Monospaced", Font.BOLD, 10);

        g2d.setColor(Color.BLACK);

        // --- 1. EN-TÊTE AVEC LOGO ---
        int logoSize = 40;
        int currentX = 0;
        
        if (logoIcon != null) {
            g2d.drawImage(logoIcon.getImage(), currentX, y, logoSize, logoSize, null);
            currentX += logoSize + 15;
        }
        
        g2d.setFont(fontHeader);
        g2d.drawString("CHAA ELECT", currentX, y + 15);
        
        g2d.setFont(fontSmall);
        g2d.drawString("08, RUE 42500, Ezzahrouni, El Hrairia, Tunis, 2051", currentX, y + 28);
        g2d.drawString("Tel: 94 226 752", currentX, y + 40);
        g2d.drawString("MF: 000/M/A/1981916C", currentX, y + 52);
        
       
  
        y += logoSize + 15;

        // --- 2. DATE ---
        drawDashedLine(g2d, 0, width, y);
        y += 15;
        
        g2d.setFont(fontBold);
        g2d.drawString("Date: " + dateTicket, 0, y);
        y += 15;

        // --- 3. NUMÉRO DE TICKET ---
        g2d.drawString("Ticket N°: " + numeroTicket, 0, y);
        y += 15;

        // --- 4. CLIENT ---
        g2d.setFont(fontRegular);
        g2d.drawString("Client: " + (nomClient != null && !nomClient.isEmpty() ? nomClient : "Particulier"), 0, y);
        y += 15;

        drawDashedLine(g2d, 0, width, y);
        y += 15;

        // --- 5. ARTICLES ---
        g2d.setFont(fontBold);
        g2d.drawString("ARTICLE", 0, y);
        drawRightText(g2d, "MONTANT", width, y);
        y += 15;

        g2d.setFont(fontRegular);

        for (int i = 0; i < model.getRowCount(); i++) {
            String articleName = model.getValueAt(i, 1).toString();
            String qte = model.getValueAt(i, 2).toString();
            
            String puStr = model.getValueAt(i, 3).toString()
                .replace(" DT", "")
                .replace(",", ".")
                .replaceAll("[^0-9.-]", "");
            double pu = Double.parseDouble(puStr);
            
            // Calcul du total HT de la ligne (quantité * prix unitaire)
            int quantite = Integer.parseInt(qte);
            double totalLigneHT = quantite * pu;
            
            // Nom de l'article
            if (articleName.length() > 20) articleName = articleName.substring(0, 20) + "..";
            g2d.drawString(articleName, 0, y);
            y += 10;

            // Détail quantité x prix unitaire
            String detail = qte + " x " + df3.format(pu);
            g2d.setFont(fontRegular);
            g2d.drawString("  " + detail, 0, y);
            
            // Afficher le TOTAL HT de la ligne à droite
            g2d.setFont(fontMontant);
            drawRightText(g2d, df3.format(totalLigneHT), width, y);
            
            g2d.setFont(fontRegular);
            y += 15;
        }

        drawDashedLine(g2d, 0, width, y);
        y += 15;

        // --- 6. TOTAL HT ---
        g2d.setFont(fontRegular);
        g2d.drawString("Total HT:", 0, y);
        drawRightText(g2d, df3.format(totalHT), width, y);
        y += 15;

        // --- 7. REMISE (si présente) ---
        if (montantRemise > 0) {
            g2d.drawString("Remise:", 0, y);
            drawRightText(g2d, "- " + df3.format(montantRemise), width, y);
            y += 15;
        }

        // --- 8. TOTAL TTC ---
        g2d.setFont(new Font("SansSerif", Font.BOLD, 12));
        g2d.drawString("TOTAL TTC:", 0, y);
        drawRightText(g2d, df3.format(totalTTC), width, y);
        y += 25;
        
        // --- 9. PIED DE PAGE ---
        g2d.setFont(fontSmall);
        centerText(g2d, "Merci de votre visite", width, y);
        y += 15;
        centerText(g2d, "Arrêté le présent ticket à la somme de:", width, y);
        y += 15;
        g2d.setFont(new Font("SansSerif", Font.BOLD, 10));
        centerText(g2d, df3.format(totalTTC), width, y);

        return PAGE_EXISTS;
    }

    private void centerText(Graphics2D g2d, String text, int width, int y) {
        int textWidth = g2d.getFontMetrics().stringWidth(text);
        g2d.drawString(text, (width - textWidth) / 2, y);
    }

    private void drawRightText(Graphics2D g2d, String text, int width, int y) {
        int textWidth = g2d.getFontMetrics().stringWidth(text);
        g2d.drawString(text, width - textWidth, y);
    }

    private void drawDashedLine(Graphics2D g2d, int x1, int x2, int y) {
        g2d.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{3}, 0));
        g2d.drawLine(x1, y, x2, y);
        g2d.setStroke(new BasicStroke());
    }

    public void imprimer() {
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName("Ticket " + numeroTicket); 

        PageFormat pf = job.defaultPage();
        Paper paper = pf.getPaper();
        
        double height = model.getRowCount() * 30 + 350;
        paper.setSize(PAPER_WIDTH, height);
        paper.setImageableArea(MARGIN, 0, PAPER_WIDTH - (2 * MARGIN), height);

        pf.setOrientation(PageFormat.PORTRAIT);
        pf.setPaper(paper);

        job.setPrintable(this, pf);

        if (job.printDialog()) {
            try {
                job.print();
            } catch (PrinterException e) {
                log.error("Erreur lors de l'impression du ticket", e);
            }
        }
    }
}