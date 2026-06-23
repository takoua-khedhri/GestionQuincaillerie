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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BonSortieImpression implements Printable {

    private static final Logger log = LoggerFactory.getLogger(BonSortieImpression.class);

    private String numeroBonSortie;
    private String dateBonSortie;
    private String nomClient;
    private String prenomClient;
    private String adresseClient;
    private String telClient;
    private String matriculeFiscale;
    private String matriculeVoiture;
    private String motifGlobal;
    private String observations;
    private DefaultTableModel model;
    private String remiseStr;
    private ImageIcon logoIcon;
    private double totalHT;
    private double totalTTC;
    private double montantRemise;
    private DecimalFormat df;
    private DecimalFormat df3;

    public BonSortieImpression(String numero, String date, String nom, String prenom,
                               String adresse, String tel, String matricule,
                               String matriculeVoiture,
                               String motif, String obs,
                               DefaultTableModel model, String remiseStr,
                               ImageIcon logo,
                               double totalHT, double totalTTC, double montantRemise) {
        this.numeroBonSortie  = numero;
        this.dateBonSortie    = date;
        this.nomClient        = nom;
        this.prenomClient     = prenom;
        this.adresseClient    = adresse;
        this.telClient        = tel;
        this.matriculeFiscale = matricule;
        this.matriculeVoiture = matriculeVoiture;
        this.motifGlobal      = motif;
        this.observations     = obs;
        this.model            = model;
        this.remiseStr        = remiseStr;
        this.logoIcon         = logo;
        this.totalHT          = totalHT;
        this.totalTTC         = totalTTC;
        this.montantRemise    = montantRemise;
        this.df  = new DecimalFormat("#,##0.00",  new DecimalFormatSymbols(Locale.FRANCE));
        this.df3 = new DecimalFormat("#,##0.000", new DecimalFormatSymbols(Locale.FRANCE));
    }

    // =========================================================================
    // UTILITAIRE : découpe un texte en lignes qui tiennent dans maxWidth pixels
    // =========================================================================
    private List<String> wrapText(String text, FontMetrics fm, int maxWidth) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) { lines.add(""); return lines; }
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
    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
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
            y = printClientSection(g2d, margin, y, contentWidth);
            y += 15;
            y = printArticlesTable(g2d, margin, y, contentWidth);
            y += 15;
            y = printTotals(g2d, margin, y, contentWidth);
            y += 15;
            y = printMontantEnLettres(g2d, margin, y, contentWidth);
            y += 10;
            printFooter(g2d, margin, y, contentWidth, pageFormat);
        } catch (Exception e) {
            log.error("Erreur lors de l'impression du Bon de Sortie", e);
            throw new PrinterException("Erreur impression Bon de Sortie: " + e.getMessage());
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
                Image img = logoIcon.getImage();
                g2d.drawImage(img, margin, y, logoSize, logoSize, null);
                logoPrinted = true;
            } catch (Exception e) { log.error("Erreur lors du chargement du logo", e); }
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
        g2d.drawString("Tel: 94 226 752",             infoX, y + 77);
        g2d.drawString("Email: chaa.elec@outlook.fr", infoX, y + 92);
        g2d.drawString("MF: 000/M/A/1981916C",        infoX, y + 107);
        return y + 120;
    }

    // =========================================================================
    // NUMERO ET DATE (noir et blanc)
    // =========================================================================
    private int printNumeroDate(Graphics2D g2d, int margin, int y, int contentWidth) {
        g2d.setColor(Color.BLACK);
        g2d.fillRect(margin, y, contentWidth, 30);
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        g2d.drawString("BON DE SORTIE N°: " + numeroBonSortie, margin + 10, y + 21);
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        String dateText = "Date: " + dateBonSortie;
        int dateWidth   = g2d.getFontMetrics().stringWidth(dateText);
        g2d.drawString(dateText, margin + contentWidth - dateWidth - 10, y + 21);
        return y + 35;
    }

    // =========================================================================
    // SECTION CLIENT — retour à la ligne automatique (noir et blanc)
    // =========================================================================
    private int printClientSection(Graphics2D g2d, int margin, int y, int contentWidth) {
        Font plainFont = new Font("Arial", Font.PLAIN, 11);
        Font boldFont  = new Font("Arial", Font.BOLD,  12);

        g2d.setFont(plainFont);
        FontMetrics fm = g2d.getFontMetrics();

        int col2X     = margin + contentWidth / 2;
        int col1Width = contentWidth / 2 - 20;
        int col2Width = contentWidth / 2 - 10;
        int padding   = 10;
        int lineH     = 15;

        String nomComplet   = nomClient + (prenomClient != null && !prenomClient.isEmpty() ? " " + prenomClient : "");
        String adresseTxt   = adresseClient    != null ? adresseClient    : "";
        String telTxt       = telClient        != null ? telClient        : "";
        String matriculeTxt = matriculeFiscale != null ? matriculeFiscale : "";

        List<String> nomLines     = wrapText("Nom: "       + nomComplet,   fm, col1Width - padding);
        List<String> adresseLines = wrapText("Adresse: "   + adresseTxt,   fm, col1Width - padding);
        List<String> telLines     = wrapText("Tel: "       + telTxt,       fm, col2Width - padding);
        List<String> matLines     = wrapText("Matricule: " + matriculeTxt, fm, col2Width - padding);

        int leftTotalLines  = nomLines.size() + adresseLines.size();
        int rightTotalLines = telLines.size()  + matLines.size();
        int totalLines      = Math.max(leftTotalLines, rightTotalLines);

        boolean hasVehicule = matriculeVoiture != null && !matriculeVoiture.isEmpty();
        int sectionHeight   = padding + 20 + totalLines * lineH + padding
                              + (hasVehicule ? lineH + 4 : 0);

        // Fond et bordure (noir et blanc)
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.fillRect(margin, y, contentWidth, sectionHeight);
        g2d.setColor(Color.BLACK);
        g2d.drawRect(margin, y, contentWidth, sectionHeight);

        g2d.setFont(boldFont);
        g2d.setColor(Color.BLACK);
        g2d.drawString("CLIENT (DESTINATAIRE)", margin + padding, y + 16);
        g2d.drawLine(margin + padding, y + 20, margin + 200, y + 20);

        g2d.setFont(plainFont);
        g2d.setColor(Color.BLACK);
        int textY = y + 20 + lineH;

        for (String line : nomLines)     { g2d.drawString(line, margin + padding, textY); textY += lineH; }
        for (String line : adresseLines) { g2d.drawString(line, margin + padding, textY); textY += lineH; }

        int textY2 = y + 20 + lineH;
        for (String line : telLines) { g2d.drawString(line, col2X, textY2); textY2 += lineH; }
        for (String line : matLines) { g2d.drawString(line, col2X, textY2); textY2 += lineH; }

        if (hasVehicule) {
            int vehY = y + sectionHeight - padding - 2;
            g2d.setFont(new Font("Arial", Font.BOLD, 11));
            g2d.setColor(Color.BLACK);
            g2d.drawString("Vehicule: " + matriculeVoiture, margin + padding, vehY);
        }

        return y + sectionHeight + 10;
    }

    // =========================================================================
    // TABLEAU ARTICLES — hauteur de ligne DYNAMIQUE selon désignation (noir et blanc)
    // =========================================================================
    private int printArticlesTable(Graphics2D g2d, int margin, int y, int contentWidth) {

        String[] headers   = {"Code", "Désignation", "Qté", "PU HT", "Total HT", "TVA", "PU TTC", "Remise %", "Total TTC"};
        int[]    colWidths = {
            (int)(contentWidth * 0.08),
            (int)(contentWidth * 0.20),
            (int)(contentWidth * 0.05),
            (int)(contentWidth * 0.09),
            (int)(contentWidth * 0.11),
            (int)(contentWidth * 0.06),
            (int)(contentWidth * 0.09),
            (int)(contentWidth * 0.08),
            (int)(contentWidth * 0.12)
        };

        int headerHeight = 30;
        int x            = margin;

        // En-têtes (noir et blanc)
        for (int i = 0; i < headers.length; i++) {
            g2d.setColor(Color.BLACK);
            g2d.fillRect(x, y, colWidths[i], headerHeight);
            g2d.setColor(Color.WHITE);
            g2d.drawRect(x, y, colWidths[i], headerHeight);
            g2d.setFont(new Font("Arial", Font.BOLD, 10));
            int tw = g2d.getFontMetrics().stringWidth(headers[i]);
            g2d.drawString(headers[i], x + (colWidths[i] - tw) / 2, y + 20);
            x += colWidths[i];
        }

        y += headerHeight;

        Font        rowFont     = new Font("Arial", Font.PLAIN, 10);
        g2d.setFont(rowFont);
        FontMetrics fm          = g2d.getFontMetrics();
        int         cellPadding = 5;
        int         lineSpacing = 13;

        for (int row = 0; row < model.getRowCount(); row++) {
            String desig = (model.getValueAt(row, 1) != null) ? model.getValueAt(row, 1).toString() : "";
            List<String> desigLines = wrapText(desig, fm, colWidths[1] - cellPadding * 2);

            int rowHeight = Math.max(25, desigLines.size() * lineSpacing + 8);
            int rowY      = y;

            // Fond de ligne alterné (noir et blanc)
            g2d.setColor(row % 2 == 0 ? Color.WHITE : Color.LIGHT_GRAY);
            g2d.fillRect(margin, rowY, contentWidth, rowHeight);

            x = margin;
            for (int col = 0; col < headers.length; col++) {
                g2d.setColor(Color.DARK_GRAY);
                g2d.drawRect(x, rowY, colWidths[col], rowHeight);

                // TOUT LE TEXTE EN NOIR (plus de couleurs)
                g2d.setColor(Color.BLACK);

                if (col == 1) {
                    int textLineY = rowY + lineSpacing;
                    for (String line : desigLines) {
                        g2d.drawString(line, x + cellPadding, textLineY);
                        textLineY += lineSpacing;
                    }
                } else {
                    Object value = model.getValueAt(row, col);
                    String text  = (value != null) ? value.toString() : "";
                    int    tw    = fm.stringWidth(text);
                    int    textX = (col >= 2) ? x + (colWidths[col] - tw) / 2 : x + cellPadding;
                    int textLineY = rowY + (rowHeight + fm.getAscent() - fm.getDescent()) / 2;
                    g2d.drawString(text, textX, textLineY);
                }
                x += colWidths[col];
            }

            y += rowHeight;
            if (y > 580) break;
        }

        return y + 10;
    }

    // =========================================================================
    // TOTAUX (noir et blanc)
    // =========================================================================
    private int printTotals(Graphics2D g2d, int margin, int y, int contentWidth) {
        int totalsWidth  = 280;
        int totalsX      = margin + contentWidth - totalsWidth;
        int totalsHeight = 80;
        if (montantRemise > 0) totalsHeight += 40;

        g2d.setColor(Color.LIGHT_GRAY);
        g2d.fillRect(totalsX, y, totalsWidth, totalsHeight);
        g2d.setColor(Color.BLACK);
        g2d.drawRect(totalsX, y, totalsWidth, totalsHeight);

        int lineY  = y + 22;
        int labelX = totalsX + 12;
        int valueX = totalsX + totalsWidth - 12;

        g2d.setFont(new Font("Arial", Font.PLAIN, 11));
        g2d.setColor(Color.BLACK);

        g2d.drawString("Total HT :", labelX, lineY);
        String htStr = formatMontantDinar(totalHT) + " DT";
        g2d.drawString(htStr, valueX - g2d.getFontMetrics().stringWidth(htStr), lineY);
        lineY += 20;

        if (montantRemise > 0) {
            g2d.drawString("Remise :", labelX, lineY);
            String remStr = "- " + formatMontantDinar(montantRemise) + " DT";
            g2d.drawString(remStr, valueX - g2d.getFontMetrics().stringWidth(remStr), lineY);
            lineY += 20;

            g2d.drawString("Total HT apres remise :", labelX, lineY);
            String htRemStr = formatMontantDinar(totalHT - montantRemise) + " DT";
            g2d.drawString(htRemStr, valueX - g2d.getFontMetrics().stringWidth(htRemStr), lineY);
            lineY += 20;
        }

        g2d.setColor(Color.BLACK);
        g2d.drawLine(labelX, lineY, totalsX + totalsWidth - 12, lineY);
        lineY += 12;

        g2d.setFont(new Font("Arial", Font.BOLD, 13));
        g2d.drawString("TOTAL TTC :", labelX, lineY);
        String ttcStr = formatMontantDinar(totalTTC) + " DT";
        g2d.drawString(ttcStr, valueX - g2d.getFontMetrics().stringWidth(ttcStr), lineY);

        return y + totalsHeight + 10;
    }

    // =========================================================================
    // MONTANT EN LETTRES
    // =========================================================================
    private int printMontantEnLettres(Graphics2D g2d, int margin, int y, int contentWidth) {
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.ITALIC, 10));
        g2d.drawString("Arrete le present bon de sortie a la somme de :", margin, y);

        String lettres = convertirMontantEnLettres(totalTTC);
        g2d.setFont(new Font("Arial", Font.BOLD, 11));

        if (lettres.length() > 60) {
            String[] parts = splitText(lettres, 60);
            g2d.drawString(parts[0], margin, y + 15);
            if (parts.length > 1) {
                g2d.drawString(parts[1], margin, y + 30);
                return y + 45;
            }
        } else {
            g2d.drawString(lettres, margin, y + 15);
        }
        return y + 30;
    }

    // =========================================================================
    // FOOTER (noir et blanc)
    // =========================================================================
    private void printFooter(Graphics2D g2d, int margin, int y, int contentWidth, PageFormat pageFormat) {
        int sigWidth  = 210;
        int sigHeight = 80;
        int sigX      = margin + contentWidth - sigWidth - 10;
        int sigY      = y + 10;

        g2d.setColor(Color.BLACK);
        g2d.drawRect(sigX, sigY, sigWidth, sigHeight);

        g2d.setFont(new Font("Arial", Font.BOLD, 11));
        String signatureText = "Signature et cachet";
        int    textWidth     = g2d.getFontMetrics().stringWidth(signatureText);
        g2d.drawString(signatureText, sigX + (sigWidth - textWidth) / 2, sigY + 16);

        g2d.setColor(Color.BLACK);
        g2d.drawLine(sigX + 10, sigY + 22, sigX + sigWidth - 10, sigY + 22);
    }

    // =========================================================================
    // UTILITAIRES
    // =========================================================================
    private String formatMontantDinar(double montant) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.FRANCE);
        symbols.setGroupingSeparator(' ');
        DecimalFormat df3local = new DecimalFormat("#,##0.000", symbols);
        String formatted = df3local.format(montant).replace(",", ".");
        if (formatted.endsWith(".000")) return formatted.replace(".", ",");
        return formatted;
    }

    private String convertirMontantEnLettres(double montant) {
        try {
            double montantArrondi = Math.round(montant * 1000.0) / 1000.0;
            int    partieEntiere  = (int) montantArrondi;
            int    partieDecimale = (int) Math.round((montantArrondi - partieEntiere) * 1000);

            String entiereLettres  = convertirNombreEnLettres(partieEntiere);

            StringBuilder resultat = new StringBuilder();
            if      (partieEntiere == 1) resultat.append("un dinar");
            else if (partieEntiere  > 1) resultat.append(entiereLettres).append(" dinars");
            else                         resultat.append(entiereLettres).append(" dinar");

            if (partieDecimale > 0) {
                resultat.append(" et ");
                String decimaleLettres = convertirNombreEnLettres(partieDecimale);
                if      (partieDecimale == 1)   resultat.append("un millime");
                else if (partieDecimale < 1000) resultat.append(decimaleLettres).append(" millimes");
                else {
                    int centimes = partieDecimale / 10;
                    if      (centimes == 1) resultat.append("un centime");
                    else if (centimes  > 1) resultat.append(convertirNombreEnLettres(centimes)).append(" centimes");
                }
            }
            return resultat.toString();
        } catch (Exception e) {
            log.error("Erreur lors de la conversion du montant en lettres", e);
            return "Montant non convertible";
        }
    }

    private String convertirNombreEnLettres(int nombre) {
        if (nombre < 0 || nombre > 999999999) return "montant trop eleve";
        if (nombre == 0) return "zero";

        String[] unites  = {"", "un", "deux", "trois", "quatre", "cinq", "six", "sept", "huit", "neuf",
                            "dix", "onze", "douze", "treize", "quatorze", "quinze", "seize",
                            "dix-sept", "dix-huit", "dix-neuf"};
        String[] dizaines = {"", "", "vingt", "trente", "quarante", "cinquante",
                             "soixante", "soixante", "quatre-vingt", "quatre-vingt"};

        if (nombre < 20) return unites[nombre];

        if (nombre < 100) {
            int dizaine = nombre / 10;
            int unite   = nombre % 10;
            if (dizaine == 7 || dizaine == 9) {
                int temp = 10 + unite;
                if (temp == 10) return dizaines[dizaine] + "-dix";
                return dizaines[dizaine] + "-" + unites[temp];
            }
            if (unite == 0) return (dizaine == 8) ? dizaines[dizaine] + "s" : dizaines[dizaine];
            if (unite == 1 && dizaine != 8) return dizaines[dizaine] + " et un";
            return dizaines[dizaine] + "-" + unites[unite];
        }

        if (nombre < 1000) {
            int centaines = nombre / 100;
            int reste     = nombre % 100;
            String res    = (centaines == 1) ? "cent" : unites[centaines] + " cent";
            if (reste > 0)          res += " " + convertirNombreEnLettres(reste);
            else if (centaines > 1) res += "s";
            return res;
        }

        if (nombre < 1000000) {
            int milliers = nombre / 1000;
            int reste    = nombre % 1000;
            String res   = (milliers == 1) ? "mille" : convertirNombreEnLettres(milliers) + " mille";
            if (reste > 0) res += " " + convertirNombreEnLettres(reste);
            return res;
        }

        if (nombre < 1000000000) {
            int millions = nombre / 1000000;
            int reste    = nombre % 1000000;
            String res   = convertirNombreEnLettres(millions) + (millions > 1 ? " millions" : " million");
            if (reste > 0) res += " " + convertirNombreEnLettres(reste);
            return res;
        }

        return "montant trop eleve";
    }

    private String[] splitText(String text, int maxLength) {
        if (text.length() <= maxLength) return new String[]{text};
        int splitIndex = text.lastIndexOf(" ", maxLength);
        if (splitIndex <= 0) splitIndex = maxLength;
        return new String[]{text.substring(0, splitIndex), text.substring(splitIndex).trim()};
    }

    // =========================================================================
    // LANCER IMPRESSION
    // =========================================================================
    public void imprimer() throws PrinterException {
        log.info("Impression Bon de Sortie...");
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName("Bon de Sortie " + numeroBonSortie);
        job.setPrintable(this);
        PageFormat pf = job.defaultPage();
        pf.setOrientation(PageFormat.PORTRAIT);
        if (job.printDialog()) {
            job.print();
        } else {
            throw new PrinterException("Impression annulee");
        }
    }
}