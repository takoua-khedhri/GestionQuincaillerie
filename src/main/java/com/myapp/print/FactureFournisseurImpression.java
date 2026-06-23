package com.myapp.print;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.ImageIcon;
import javax.swing.table.DefaultTableModel;

public class FactureFournisseurImpression implements Printable {

    private String numeroFacture;
    private String dateFacture;
    private String numeroFactureFournisseur;
    private String nomFournisseur;
    private String matriculeFiscale;
    private String adresseFournisseur;
    private String telFournisseur;
    private DefaultTableModel model;
    private double retenueSourcePct;
    private double retenueSourceMontant;
    private double netAPayer;
    private ImageIcon logoIcon;

    private DecimalFormat df;
    private DecimalFormat df3;

    public FactureFournisseurImpression(String numeroFacture, String dateFacture,
                                        String numeroFactureFournisseur,
                                        String nomFournisseur, String matriculeFiscale,
                                        String adresseFournisseur, String telFournisseur,
                                        DefaultTableModel model,
                                        double retenueSourcePct, double retenueSourceMontant,
                                        double netAPayer, ImageIcon logoIcon) {

        this.numeroFacture            = numeroFacture;
        this.dateFacture              = dateFacture;
        this.numeroFactureFournisseur = numeroFactureFournisseur;
        this.nomFournisseur           = nomFournisseur           != null ? nomFournisseur           : "";
        this.matriculeFiscale         = matriculeFiscale         != null ? matriculeFiscale         : "";
        this.adresseFournisseur       = adresseFournisseur       != null ? adresseFournisseur       : "";
        this.telFournisseur           = telFournisseur           != null ? telFournisseur           : "";
        this.model                    = model;
        this.retenueSourcePct         = retenueSourcePct;
        this.retenueSourceMontant     = retenueSourceMontant;
        this.netAPayer                = netAPayer;
        this.logoIcon                 = logoIcon;

        df  = new DecimalFormat("#,##0.00",  new DecimalFormatSymbols(Locale.FRANCE));
        df3 = new DecimalFormat("#,##0.000", new DecimalFormatSymbols(Locale.FRANCE));
    }

    // =========================================================================
    // UTILITAIRE : découpe un texte en lignes qui tiennent dans maxWidth pixels
    // =========================================================================
    private List<String> wrapText(String text, FontMetrics fm, int maxWidth) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            lines.add("");
            return lines;
        }
        String[] words = text.split(" ", -1);
        StringBuilder current = new StringBuilder();
        for (String word : words) {
            String test = current.length() == 0 ? word : current + " " + word;
            if (fm.stringWidth(test) <= maxWidth) {
                current = new StringBuilder(test);
            } else {
                if (current.length() > 0) lines.add(current.toString());
                if (fm.stringWidth(word) > maxWidth) {
                    StringBuilder sub = new StringBuilder();
                    for (char c : word.toCharArray()) {
                        if (fm.stringWidth(sub.toString() + c) <= maxWidth) {
                            sub.append(c);
                        } else {
                            lines.add(sub.toString());
                            sub = new StringBuilder(String.valueOf(c));
                        }
                    }
                    current = sub;
                } else {
                    current = new StringBuilder(word);
                }
            }
        }
        if (current.length() > 0) lines.add(current.toString());
        return lines;
    }

    // =========================================================================
    // PRINT
    // =========================================================================
    @Override
    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex)
            throws PrinterException {

        if (pageIndex > 0) return NO_SUCH_PAGE;

        Graphics2D g2d = (Graphics2D) graphics;
        g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int margin       = 40;
        int contentWidth = (int) pageFormat.getImageableWidth() - 2 * margin;
        int y            = margin;

        try {
            y = printHeader(g2d, margin, y, contentWidth);  // Logo reste en couleur
            y += 20;
            y = printNumeroDate(g2d, margin, y, contentWidth);
            y += 10;
            y = printFournisseurSection(g2d, margin, y, contentWidth);
            y += 15;
            y = printArticlesTable(g2d, margin, y, contentWidth);
            y += 15;
            y = printTotalsAndRetenue(g2d, margin, y, contentWidth);
            y += 15;
            y = printMontantEnLettres(g2d, margin, y, contentWidth);
            y += 20;
            printSignatureAndStamp(g2d, margin, y, contentWidth);

        } catch (Exception e) {
            e.printStackTrace();
            throw new PrinterException("Erreur d'impression: " + e.getMessage());
        }

        return PAGE_EXISTS;
    }

    // =========================================================================
    // HEADER (seul le logo reste en couleur)
    // =========================================================================
    private int printHeader(Graphics2D g2d, int margin, int y, int contentWidth) {
        int logoSize = 80;

        boolean logoPrinted = false;
        if (logoIcon != null) {
            try {
                Image logoImage = logoIcon.getImage();
                g2d.drawImage(logoImage, margin, y, logoSize, logoSize, null);
                logoPrinted = true;
            } catch (Exception e) {}
        }

        if (!logoPrinted) {
            g2d.setColor(Color.LIGHT_GRAY);
            g2d.fillRect(margin, y, logoSize, logoSize);
            g2d.setColor(Color.BLACK);
            g2d.drawRect(margin, y, logoSize, logoSize);
            g2d.setFont(new Font("Arial", Font.BOLD, 14));
            g2d.drawString("LOGO", margin + 20, y + logoSize / 2);
        }

        int infoX = margin + logoSize + 20;
        g2d.setColor(Color.BLACK);  // Texte en noir

        g2d.setFont(new Font("Arial", Font.BOLD, 20));
        g2d.drawString("CHAA ELECT", infoX, y + 25);

        g2d.setFont(new Font("Arial", Font.BOLD, 13));
        g2d.drawString("VTE EN GROS MATERIEL ELECTRIQUE", infoX, y + 45);

        g2d.setFont(new Font("Arial", Font.PLAIN, 11));
        g2d.drawString("08, RUE 42500, Ezzahrouni, El Hrairia, Tunis, 2051", infoX, y + 62);
        g2d.drawString("Tél: 94 226 752", infoX, y + 77);
        g2d.drawString("Email: chaa.elec@outlook.fr", infoX, y + 92);
        g2d.drawString("MF: 000/M/A/1981916C", infoX, y + 107);

        return y + 120;
    }

    // =========================================================================
    // NUMÉRO & DATE (noir et blanc)
    // =========================================================================
    private int printNumeroDate(Graphics2D g2d, int margin, int y, int contentWidth) {
        g2d.setColor(Color.BLACK);
        g2d.fillRect(margin, y, contentWidth, 30);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        g2d.drawString("FACTURE D'ACHAT N°: " + numeroFacture, margin + 10, y + 21);

        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        String dateText = "Date: " + dateFacture;
        int dateWidth = g2d.getFontMetrics().stringWidth(dateText);
        g2d.drawString(dateText, margin + contentWidth - dateWidth - 10, y + 21);

        int nextY = y + 35;

        if (numeroFactureFournisseur != null && !numeroFactureFournisseur.isEmpty()) {
            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("Arial", Font.ITALIC, 10));
            g2d.drawString("N° Facture Fournisseur: " + numeroFactureFournisseur, margin, nextY);
            nextY += 15;
        }

        return nextY;
    }

    // =========================================================================
    // SECTION FOURNISSEUR — retour à la ligne automatique (noir et blanc)
    // =========================================================================
    private int printFournisseurSection(Graphics2D g2d, int margin, int y, int contentWidth) {

        Font plainFont = new Font("Arial", Font.PLAIN, 11);
        Font boldFont  = new Font("Arial", Font.BOLD,  12);

        g2d.setFont(plainFont);
        FontMetrics fm = g2d.getFontMetrics();

        int col2X     = margin + contentWidth / 2;
        int col1Width = contentWidth / 2 - 20;
        int col2Width = contentWidth / 2 - 10;
        int padding   = 10;
        int lineH     = 15;

        List<String> nomLines      = wrapText("Fournisseur: " + nomFournisseur,   fm, col1Width - padding);
        List<String> adresseLines  = wrapText("Adresse: "     + adresseFournisseur, fm, col1Width - padding);
        List<String> telLines      = wrapText("Tél: "         + telFournisseur,   fm, col2Width - padding);
        List<String> matLines      = wrapText("Matricule: "   + matriculeFiscale, fm, col2Width - padding);

        int leftLines  = nomLines.size() + adresseLines.size();
        int rightLines = telLines.size() + matLines.size();
        int totalLines = Math.max(leftLines, rightLines);

        int sectionHeight = padding + 20 + totalLines * lineH + padding;

        // Fond et bordure (noir et blanc)
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.fillRect(margin, y, contentWidth, sectionHeight);
        g2d.setColor(Color.BLACK);
        g2d.drawRect(margin, y, contentWidth, sectionHeight);

        // Titre
        g2d.setFont(boldFont);
        g2d.setColor(Color.BLACK);
        g2d.drawString("INFORMATIONS FOURNISSEUR", margin + padding, y + 16);
        g2d.drawLine(margin + padding, y + 20, margin + 210, y + 20);

        // Colonne gauche
        g2d.setFont(plainFont);
        g2d.setColor(Color.BLACK);
        int textY = y + 20 + lineH;
        for (String line : nomLines)     { g2d.drawString(line, margin + padding, textY); textY += lineH; }
        for (String line : adresseLines) { g2d.drawString(line, margin + padding, textY); textY += lineH; }

        // Colonne droite
        int textY2 = y + 20 + lineH;
        for (String line : telLines) { g2d.drawString(line, col2X, textY2); textY2 += lineH; }
        for (String line : matLines) { g2d.drawString(line, col2X, textY2); textY2 += lineH; }

        return y + sectionHeight + 10;
    }

    // =========================================================================
    // TABLEAU DES ARTICLES — hauteur de ligne dynamique (noir et blanc)
    // =========================================================================
    private int printArticlesTable(Graphics2D g2d, int margin, int y, int contentWidth) {

        String[] headers   = {"Code", "Désignation", "Qté", "PU HT", "Total HT", "TVA", "Total TTC"};
        int[]    colWidths = {
            (int)(contentWidth * 0.10),   // Code
            (int)(contentWidth * 0.30),   // Désignation
            (int)(contentWidth * 0.07),   // Qté
            (int)(contentWidth * 0.12),   // PU HT
            (int)(contentWidth * 0.12),   // Total HT
            (int)(contentWidth * 0.08),   // TVA
            (int)(contentWidth * 0.13)    // Total TTC
        };

        int headerHeight = 30;
        int x            = margin;

        // --- En-têtes (noir et blanc) ---
        for (int i = 0; i < headers.length; i++) {
            g2d.setColor(Color.BLACK);
            g2d.fillRect(x, y, colWidths[i], headerHeight);
            g2d.setColor(Color.WHITE);
            g2d.drawRect(x, y, colWidths[i], headerHeight);
            g2d.setFont(new Font("Arial", Font.BOLD, 10));
            int textWidth = g2d.getFontMetrics().stringWidth(headers[i]);
            g2d.drawString(headers[i], x + (colWidths[i] - textWidth) / 2, y + 20);
            x += colWidths[i];
        }

        y += headerHeight;

        // --- Lignes articles ---
        Font        rowFont     = new Font("Arial", Font.PLAIN, 10);
        g2d.setFont(rowFont);
        FontMetrics fm          = g2d.getFontMetrics();
        int         cellPadding = 5;
        int         lineSpacing = 13;

        for (int row = 0; row < model.getRowCount(); row++) {

            // Hauteur dynamique selon la désignation
            String desig = safeStr(model.getValueAt(row, 1));
            List<String> desigLines = wrapText(desig, fm, colWidths[1] - cellPadding * 2);

            int rowHeight = Math.max(25, desigLines.size() * lineSpacing + 8);
            int rowY      = y;

            // Fond alterné (noir et blanc)
            g2d.setColor(row % 2 == 0 ? Color.WHITE : Color.LIGHT_GRAY);
            g2d.fillRect(margin, rowY, contentWidth, rowHeight);

            x = margin;
            for (int col = 0; col < headers.length; col++) {

                g2d.setColor(Color.DARK_GRAY);
                g2d.drawRect(x, rowY, colWidths[col], rowHeight);

                // TOUT LE TEXTE EN NOIR (plus de couleurs)
                g2d.setColor(Color.BLACK);

                if (col == 1) {
                    // Désignation multi-lignes
                    int textLineY = rowY + lineSpacing;
                    for (String line : desigLines) {
                        g2d.drawString(line, x + cellPadding, textLineY);
                        textLineY += lineSpacing;
                    }
                } else {
                    String text     = safeStr(model.getValueAt(row, col));
                    int    tw       = fm.stringWidth(text);
                    int    textX    = (col >= 2)
                                      ? x + (colWidths[col] - tw) / 2   // centré
                                      : x + cellPadding;                 // Code → gauche
                    int    textLineY = rowY + (rowHeight + fm.getAscent() - fm.getDescent()) / 2;
                    g2d.drawString(text, textX, textLineY);
                }

                x += colWidths[col];
            }

            y += rowHeight;

            if (y > 580) break; // sécurité page
        }

        return y + 10;
    }

    // =========================================================================
    // TOTAUX + RETENUE + NET — bloc unifié (noir et blanc)
    // =========================================================================
    private int printTotalsAndRetenue(Graphics2D g2d, int margin, int y, int contentWidth) {

        double totalHT  = 0.0;
        double totalTVA = 0.0;
        double totalTTC = 0.0;

        for (int i = 0; i < model.getRowCount(); i++) {
            double ht  = parseDouble(model.getValueAt(i, 4));
            double ttc = parseDouble(model.getValueAt(i, 6));
            totalHT  += ht;
            totalTTC += ttc;
            totalTVA += (ttc - ht);
        }

        boolean hasRetenue = (retenueSourcePct > 0.0);

        int totalsWidth = 280;
        int totalsX     = margin + contentWidth - totalsWidth;
        int rowH        = 22;
        int nbLignes    = hasRetenue ? 6 : 4;
        int boxHeight   = nbLignes * rowH + 4;

        // Cadre global (noir et blanc)
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.fillRect(totalsX, y, totalsWidth, boxHeight);
        g2d.setColor(Color.BLACK);
        g2d.drawRect(totalsX, y, totalsWidth, boxHeight);

        int lineY  = y + rowH;
        int labelX = totalsX + 12;
        int valueX = totalsX + totalsWidth - 12;

        // Total HT
        g2d.setFont(new Font("Arial", Font.PLAIN, 11));
        g2d.setColor(Color.BLACK);
        g2d.drawString("Total HT :", labelX, lineY);
        drawRight(g2d, formatMontantDinar(totalHT) + " DT", valueX, lineY);
        lineY += rowH;

        // TVA
        g2d.drawString("TVA (19%) :", labelX, lineY);
        drawRight(g2d, formatMontantDinar(totalTVA) + " DT", valueX, lineY);
        lineY += rowH;

        // Séparateur + TOTAL TTC
        g2d.setColor(Color.BLACK);
        g2d.drawLine(labelX, lineY - rowH + 4, totalsX + totalsWidth - 12, lineY - rowH + 4);
        g2d.setFont(new Font("Arial", Font.BOLD, 11));
        g2d.setColor(Color.BLACK);
        g2d.drawString("TOTAL TTC :", labelX, lineY);
        drawRight(g2d, formatMontantDinar(totalTTC) + " DT", valueX, lineY);
        lineY += rowH;

        if (hasRetenue) {
            // Bandeau RETENUE (fond gris clair)
            g2d.setColor(Color.LIGHT_GRAY);
            g2d.fillRect(totalsX + 1, lineY - rowH + 4, totalsWidth - 2, rowH);
            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("Arial", Font.BOLD, 10));
            String bandeauLabel = "RETENUE À LA SOURCE";
            int    bandeauW     = g2d.getFontMetrics().stringWidth(bandeauLabel);
            g2d.drawString(bandeauLabel, totalsX + (totalsWidth - bandeauW) / 2, lineY + 2);
            lineY += rowH;

            // Montant retenue
            g2d.setFont(new Font("Arial", Font.PLAIN, 11));
            g2d.setColor(Color.BLACK);
            g2d.drawString(retenueSourcePct + " % sur TTC :", labelX, lineY);
            drawRight(g2d, "- " + formatMontantDinar(retenueSourceMontant) + " DT", valueX, lineY);
            lineY += rowH;
        }

        // NET À PAYER — fond gris clair
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.fillRect(totalsX + 1, lineY - rowH + 4, totalsWidth - 2, rowH);
        g2d.setColor(Color.BLACK);
        g2d.drawLine(totalsX + 1, lineY - rowH + 4, totalsX + totalsWidth - 1, lineY - rowH + 4);
        g2d.setFont(new Font("Arial", Font.BOLD, 13));
        g2d.setColor(Color.BLACK);
        g2d.drawString("NET À PAYER :", labelX, lineY);
        drawRight(g2d, formatMontantDinar(netAPayer) + " DT", valueX, lineY);

        return y + boxHeight + 5;
    }

    // =========================================================================
    // MONTANT EN LETTRES
    // =========================================================================
    private int printMontantEnLettres(Graphics2D g2d, int margin, int y, int contentWidth) {
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.ITALIC, 10));
        g2d.drawString("Arrêté la présente facture à la somme de :", margin, y);

        String montantEnLettres = convertirMontantEnLettres(netAPayer);

        g2d.setFont(new Font("Arial", Font.BOLD, 11));
        g2d.setColor(Color.BLACK);

        if (montantEnLettres.length() > 60) {
            String[] parts = splitText(montantEnLettres, 60);
            g2d.drawString(parts[0], margin, y + 15);
            if (parts.length > 1) {
                g2d.drawString(parts[1], margin, y + 30);
                return y + 45;
            }
        } else {
            g2d.drawString(montantEnLettres, margin, y + 15);
        }
        return y + 30;
    }

    // =========================================================================
    // SIGNATURE (noir et blanc)
    // =========================================================================
    private int printSignatureAndStamp(Graphics2D g2d, int margin, int y, int contentWidth) {
        int sigWidth  = 220;
        int sigHeight = 75;
        int sigX      = margin + (contentWidth - sigWidth) / 2;

        g2d.setColor(Color.BLACK);
        g2d.drawLine(margin, y - 10, margin + contentWidth, y - 10);

        g2d.setColor(Color.WHITE);
        g2d.fillRect(sigX, y, sigWidth, sigHeight);
        g2d.setColor(Color.BLACK);
        g2d.drawRect(sigX, y, sigWidth, sigHeight);

        g2d.setFont(new Font("Arial", Font.BOLD, 11));
        String label = "Signature et cachet";
        int    labelW = g2d.getFontMetrics().stringWidth(label);
        g2d.drawString(label, sigX + (sigWidth - labelW) / 2, y + 16);

        g2d.setColor(Color.BLACK);
        g2d.drawLine(sigX + 15, y + 45, sigX + sigWidth - 15, y + 45);

        return y + sigHeight;
    }

    // =========================================================================
    // UTILITAIRES
    // =========================================================================
    private void drawRight(Graphics2D g2d, String text, int xRight, int y) {
        int w = g2d.getFontMetrics().stringWidth(text);
        g2d.drawString(text, xRight - w, y);
    }

    private String safeStr(Object o) {
        return o != null ? o.toString() : "";
    }

    private double parseDouble(Object o) {
        if (o == null) return 0.0;
        String s = o.toString().replace(" ", "").replace(",", ".").replaceAll("[^0-9.-]", "");
        try { return Double.parseDouble(s); } catch (Exception e) { return 0.0; }
    }

    private String formatMontantDinar(double montant) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.FRANCE);
        symbols.setGroupingSeparator(' ');
        DecimalFormat fmt = new DecimalFormat("#,##0.000", symbols);
        return fmt.format(montant).replace(",", ".");
    }

    private String convertirMontantEnLettres(double montant) {
        try {
            double montantArrondi = Math.round(montant * 1000.0) / 1000.0;
            int    partieEntiere  = (int) montantArrondi;
            int    partieDecimale = (int) Math.round((montantArrondi - partieEntiere) * 1000);

            StringBuilder resultat = new StringBuilder();

            if      (partieEntiere == 1) resultat.append("un dinar");
            else if (partieEntiere  > 1) resultat.append(convertirNombreEnLettres(partieEntiere)).append(" dinars");
            else                         resultat.append("zéro dinar");

            if (partieDecimale > 0) {
                resultat.append(" et ");
                if (partieDecimale == 1) resultat.append("un millime");
                else resultat.append(convertirNombreEnLettres(partieDecimale)).append(" millimes");
            }

            return resultat.toString();
        } catch (Exception e) {
            return "Montant non convertible";
        }
    }

    private String convertirNombreEnLettres(int nombre) {
        if (nombre < 0 || nombre > 999999999) return "montant trop élevé";
        if (nombre == 0) return "zéro";

        String[] unites   = {"", "un", "deux", "trois", "quatre", "cinq", "six", "sept", "huit", "neuf",
                             "dix", "onze", "douze", "treize", "quatorze", "quinze", "seize",
                             "dix-sept", "dix-huit", "dix-neuf"};
        String[] dizaines = {"", "", "vingt", "trente", "quarante", "cinquante",
                             "soixante", "soixante", "quatre-vingt", "quatre-vingt"};

        if (nombre < 20) return unites[nombre];

        if (nombre < 100) {
            int d = nombre / 10, u = nombre % 10;
            if (d == 7 || d == 9) return dizaines[d] + "-" + unites[10 + u];
            if (u == 0)           return (d == 8) ? "quatre-vingts" : dizaines[d];
            if (u == 1 && d != 8) return dizaines[d] + " et un";
            return dizaines[d] + "-" + unites[u];
        }

        if (nombre < 1000) {
            int c = nombre / 100, r = nombre % 100;
            String res = (c == 1) ? "cent" : unites[c] + " cent";
            if (r > 0)  return res + " " + convertirNombreEnLettres(r);
            if (c > 1)  return res + "s";
            return res;
        }

        if (nombre < 1000000) {
            int m = nombre / 1000, r = nombre % 1000;
            String res = (m == 1) ? "mille" : convertirNombreEnLettres(m) + " mille";
            return (r > 0) ? res + " " + convertirNombreEnLettres(r) : res;
        }

        if (nombre < 1000000000) {
            int m = nombre / 1000000, r = nombre % 1000000;
            String res = convertirNombreEnLettres(m) + (m > 1 ? " millions" : " million");
            return (r > 0) ? res + " " + convertirNombreEnLettres(r) : res;
        }

        return "montant trop élevé";
    }

    private String[] splitText(String text, int maxLength) {
        if (text.length() <= maxLength) return new String[]{text};
        int idx = text.lastIndexOf(" ", maxLength);
        if (idx <= 0) idx = maxLength;
        return new String[]{text.substring(0, idx), text.substring(idx).trim()};
    }

    // =========================================================================
    // LANCEMENT IMPRESSION
    // =========================================================================
    public void imprimer() throws PrinterException {
        System.out.println("Impression facture fournisseur: " + numeroFacture);
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName("FactureFournisseur_" + numeroFacture);
        job.setPrintable(this);
        PageFormat pageFormat = job.defaultPage();
        pageFormat.setOrientation(PageFormat.PORTRAIT);
        if (job.printDialog()) {
            job.print();
        } else {
            throw new PrinterException("Impression annulée");
        }
    }
}