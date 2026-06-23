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

public class BLImpression implements Printable {

    private String numeroBL;
    private String dateBL;
    private String nomClient;
    private String prenomClient;
    private String adresseClient;
    private String telClient;
    private String matriculeFiscale;

    private String nomChauffeur;
    private String adresseChauffeur;
    private String telChauffeur;
    private String voitureChauffeur;

    private DefaultTableModel model;
    private String remise;
    private ImageIcon logoIcon;
    private String modePaiement;

    private DecimalFormat df;
    private DecimalFormat df3;

    public BLImpression(String numeroBL, String dateBL,
                        String nomClient, String prenomClient,
                        String adresseClient, String telClient,
                        String matriculeFiscale,
                        String nomChauffeur,
                        String adresseChauffeur,
                        String telChauffeur,
                        String voitureChauffeur,
                        DefaultTableModel model,
                        String remise, ImageIcon logoIcon,
                        String modePaiement) {

        this.numeroBL         = numeroBL;
        this.dateBL           = dateBL;
        this.nomClient        = nomClient;
        this.prenomClient     = prenomClient;
        this.adresseClient    = adresseClient;
        this.telClient        = telClient;
        this.matriculeFiscale = matriculeFiscale;
        this.nomChauffeur     = nomChauffeur;
        this.adresseChauffeur = adresseChauffeur;
        this.telChauffeur     = telChauffeur;
        this.voitureChauffeur = voitureChauffeur;
        this.model            = model;
        this.remise           = remise;
        this.logoIcon         = logoIcon;
        this.modePaiement     = modePaiement;

        this.df  = new DecimalFormat("#,##0.00",  new DecimalFormatSymbols(Locale.FRANCE));
        this.df3 = new DecimalFormat("#,##0.000", new DecimalFormatSymbols(Locale.FRANCE));
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
                // Si un seul mot est trop long, on le coupe caractère par caractère
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
            y = printChauffeurSection(g2d, margin, y, contentWidth);
            y += 10;
            y = printClientSection(g2d, margin, y, contentWidth);
            y += 15;
            y = printArticlesTable(g2d, margin, y, contentWidth);
            y += 15;
            y = printTotals(g2d, margin, y, contentWidth);
            y += 15;
            y = printModeReglement(g2d, margin, y);
            y += 10;
            y = printMontantEnLettres(g2d, margin, y, contentWidth);
            y += 10;
            printFooter(g2d, margin, y, contentWidth, pageFormat);
        } catch (Exception e) {
            e.printStackTrace();
            throw new PrinterException("Erreur impression BL: " + e.getMessage());
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
            } catch (Exception e) {
                drawLogoPlaceholder(g2d, margin, y, logoSize);
            }
        } else {
            drawLogoPlaceholder(g2d, margin, y, logoSize);
        }

        int infoX = margin + logoSize + 20;
        g2d.setColor(Color.BLACK);  // Texte en noir

        g2d.setFont(new Font("Arial", Font.BOLD, 20));
        g2d.drawString("CHAA ELECT", infoX, y + 20);

        g2d.setFont(new Font("Arial", Font.BOLD, 13));
        g2d.drawString("VTE EN GROS MATERIEL ELECTRIQUE", infoX, y + 38);

        g2d.setFont(new Font("Arial", Font.PLAIN, 10));
        g2d.drawString("08, RUE 42500, Ezzahrouni, El Hrairia, Tunis, 2051", infoX, y + 54);
        g2d.drawString("Tel: 94 226 752",             infoX, y + 68);
        g2d.drawString("Email: chaa.elec@outlook.fr", infoX, y + 82);
        g2d.drawString("MF: 000/M/A/1981916C",        infoX, y + 96);

        return y + 115;
    }

    private void drawLogoPlaceholder(Graphics2D g2d, int margin, int y, int size) {
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.fillRect(margin, y, size, size);
        g2d.setColor(Color.BLACK);
        g2d.drawRect(margin, y, size, size);
        g2d.setFont(new Font("Arial", Font.BOLD, 12));
        g2d.drawString("LOGO", margin + 18, y + size / 2);
    }

    // =========================================================================
    // NUMERO ET DATE
    // =========================================================================
    private int printNumeroDate(Graphics2D g2d, int margin, int y, int contentWidth) {
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.BOLD, 18));
        g2d.drawString("BON DE LIVRAISON N°: " + numeroBL, margin, y);

        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        String dateText = "Date: " + dateBL;
        int dateWidth   = g2d.getFontMetrics().stringWidth(dateText);
        g2d.drawString(dateText, margin + contentWidth - dateWidth, y);

        return y + 25;
    }

    // =========================================================================
    // SECTION CHAUFFEUR — retour à la ligne automatique (noir et blanc)
    // =========================================================================
    private int printChauffeurSection(Graphics2D g2d, int margin, int y, int contentWidth) {

        Font plainFont = new Font("Arial", Font.PLAIN, 10);
        Font boldFont  = new Font("Arial", Font.BOLD,  12);

        g2d.setFont(plainFont);
        FontMetrics fm = g2d.getFontMetrics();

        int col2X     = margin + contentWidth / 2;
        int col1Width = contentWidth / 2 - 20;
        int col2Width = contentWidth / 2 - 10;
        int padding   = 10;
        int lineH     = 14;

        // --- Préparer les textes wrappés ---
        String nomChStr  = nomChauffeur     != null ? nomChauffeur     : "";
        String adrChStr  = adresseChauffeur != null ? adresseChauffeur : "";
        String telChStr  = telChauffeur     != null ? telChauffeur     : "";
        String vehStr    = (voitureChauffeur != null && !voitureChauffeur.isEmpty())
                           ? voitureChauffeur : "Non renseigné";

        List<String> nomLines = wrapText("Nom: "      + nomChStr, fm, col1Width - padding);
        List<String> adrLines = wrapText("Adresse: "  + adrChStr, fm, col1Width - padding);
        List<String> telLines = wrapText("Tel: "      + telChStr, fm, col2Width - padding);
        List<String> vehLines = wrapText("Vehicule: " + vehStr,   fm, col2Width - padding);

        int leftLines  = nomLines.size() + adrLines.size();
        int rightLines = telLines.size() + vehLines.size();
        int totalLines = Math.max(leftLines, rightLines);

        int sectionHeight = padding + 20 + totalLines * lineH + padding;

        // --- Fond et bordure (noir et blanc) ---
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.fillRect(margin, y, contentWidth, sectionHeight);
        g2d.setColor(Color.BLACK);
        g2d.drawRect(margin, y, contentWidth, sectionHeight);

        // --- Titre ---
        g2d.setFont(boldFont);
        g2d.setColor(Color.BLACK);
        g2d.drawString("CHAUFFEUR (EXPEDITEUR)", margin + padding, y + 16);
        g2d.drawLine(margin + padding, y + 20, margin + 200, y + 20);

        // --- Colonne gauche : Nom puis Adresse ---
        g2d.setFont(plainFont);
        g2d.setColor(Color.BLACK);
        int textY = y + 20 + lineH;

        for (String line : nomLines) {
            g2d.drawString(line, margin + padding, textY);
            textY += lineH;
        }
        for (String line : adrLines) {
            g2d.drawString(line, margin + padding, textY);
            textY += lineH;
        }

        // --- Colonne droite : Tel puis Véhicule ---
        int textY2 = y + 20 + lineH;

        for (String line : telLines) {
            g2d.drawString(line, col2X, textY2);
            textY2 += lineH;
        }
        g2d.setFont(new Font("Arial", Font.BOLD, 10));
        for (String line : vehLines) {
            g2d.drawString(line, col2X, textY2);
            textY2 += lineH;
        }

        return y + sectionHeight + 5;
    }

    // =========================================================================
    // SECTION CLIENT — retour à la ligne automatique (noir et blanc)
    // =========================================================================
    private int printClientSection(Graphics2D g2d, int margin, int y, int contentWidth) {

        Font plainFont = new Font("Arial", Font.PLAIN, 10);
        Font boldFont  = new Font("Arial", Font.BOLD,  12);

        g2d.setFont(plainFont);
        FontMetrics fm = g2d.getFontMetrics();

        int col2X     = margin + contentWidth / 2;
        int col1Width = contentWidth / 2 - 20;
        int col2Width = contentWidth / 2 - 10;
        int padding   = 10;
        int lineH     = 14;

        // --- Préparer les textes wrappés ---
        String nomComplet   = nomClient + (prenomClient != null && !prenomClient.isEmpty() ? " " + prenomClient : "");
        String adresseTxt   = adresseClient    != null ? adresseClient    : "";
        String telTxt       = telClient        != null ? telClient        : "";
        String matriculeTxt = matriculeFiscale != null ? matriculeFiscale : "";

        List<String> nomLines     = wrapText("Nom: "       + nomComplet,   fm, col1Width - padding);
        List<String> adresseLines = wrapText("Adresse: "   + adresseTxt,   fm, col1Width - padding);
        List<String> telLines     = wrapText("Tel: "       + telTxt,       fm, col2Width - padding);
        List<String> matLines     = wrapText("Matricule: " + matriculeTxt, fm, col2Width - padding);

        int leftLines  = nomLines.size() + adresseLines.size();
        int rightLines = telLines.size()  + matLines.size();
        int totalLines = Math.max(leftLines, rightLines);

        int sectionHeight = padding + 20 + totalLines * lineH + padding;

        // --- Fond et bordure (noir et blanc) ---
        g2d.setColor(Color.WHITE);
        g2d.fillRect(margin, y, contentWidth, sectionHeight);
        g2d.setColor(Color.BLACK);
        g2d.drawRect(margin, y, contentWidth, sectionHeight);

        // --- Titre ---
        g2d.setFont(boldFont);
        g2d.setColor(Color.BLACK);
        g2d.drawString("CLIENT (DESTINATAIRE)", margin + padding, y + 16);
        g2d.drawLine(margin + padding, y + 20, margin + 200, y + 20);

        // --- Colonne gauche : Nom puis Adresse ---
        g2d.setFont(plainFont);
        g2d.setColor(Color.BLACK);
        int textY = y + 20 + lineH;

        for (String line : nomLines) {
            g2d.drawString(line, margin + padding, textY);
            textY += lineH;
        }
        for (String line : adresseLines) {
            g2d.drawString(line, margin + padding, textY);
            textY += lineH;
        }

        // --- Colonne droite : Tel puis Matricule ---
        int textY2 = y + 20 + lineH;

        for (String line : telLines) {
            g2d.drawString(line, col2X, textY2);
            textY2 += lineH;
        }
        for (String line : matLines) {
            g2d.drawString(line, col2X, textY2);
            textY2 += lineH;
        }

        return y + sectionHeight + 5;
    }

    // =========================================================================
    // TABLEAU ARTICLES — hauteur de ligne dynamique selon la désignation (noir et blanc)
    // =========================================================================
    private int printArticlesTable(Graphics2D g2d, int margin, int y, int contentWidth) {

        String[] headers = {"Code", "Désignation", "Qté", "PU HT", "Total HT", "TVA", "PU TTC", "Remise %", "Total TTC"};
        int[]    colWidths = {
            (int)(contentWidth * 0.08),  // Code
            (int)(contentWidth * 0.20),  // Désignation
            (int)(contentWidth * 0.05),  // Qté
            (int)(contentWidth * 0.09),  // PU HT
            (int)(contentWidth * 0.11),  // Total HT
            (int)(contentWidth * 0.06),  // TVA
            (int)(contentWidth * 0.09),  // PU TTC
            (int)(contentWidth * 0.08),  // Remise %
            (int)(contentWidth * 0.12)   // Total TTC
        };

        int headerH = 25;
        int x       = margin;

        // --- En-têtes (noir et blanc) ---
        for (int i = 0; i < headers.length; i++) {
            g2d.setColor(Color.BLACK);
            g2d.fillRect(x, y, colWidths[i], headerH);
            g2d.setColor(Color.WHITE);
            g2d.drawRect(x, y, colWidths[i], headerH);
            g2d.setFont(new Font("Arial", Font.BOLD, 9));
            int tw = g2d.getFontMetrics().stringWidth(headers[i]);
            g2d.drawString(headers[i], x + (colWidths[i] - tw) / 2, y + 16);
            x += colWidths[i];
        }

        y += headerH;

        // --- Lignes articles ---
        Font        rowFont     = new Font("Arial", Font.PLAIN, 9);
        g2d.setFont(rowFont);
        FontMetrics fm          = g2d.getFontMetrics();
        int         cellPadding = 4;
        int         lineSpacing = 12;

        for (int row = 0; row < model.getRowCount(); row++) {

            // Calcul du nombre de lignes nécessaires pour la désignation
            String desig = (model.getValueAt(row, 1) != null)
                           ? model.getValueAt(row, 1).toString() : "";
            List<String> desigLines = wrapText(desig, fm, colWidths[1] - cellPadding * 2);

            // Hauteur dynamique : au moins 18px
            int rowHeight = Math.max(18, desigLines.size() * lineSpacing + 6);
            int rowY      = y;

            // Fond de ligne alterné (noir et blanc)
            g2d.setColor(row % 2 == 0 ? Color.WHITE : Color.LIGHT_GRAY);
            g2d.fillRect(margin, rowY, contentWidth, rowHeight);

            x = margin;
            for (int col = 0; col < headers.length; col++) {

                // Bordure cellule
                g2d.setColor(Color.DARK_GRAY);
                g2d.drawRect(x, rowY, colWidths[col], rowHeight);

                // TOUT LE TEXTE EN NOIR (plus de couleurs)
                g2d.setColor(Color.BLACK);

                if (col == 1) {
                    // Désignation : multi-lignes, alignée à gauche
                    int textLineY = rowY + lineSpacing;
                    for (String line : desigLines) {
                        g2d.drawString(line, x + cellPadding, textLineY);
                        textLineY += lineSpacing;
                    }
                } else {
                    // Autres colonnes : une ligne, centrée verticalement
                    Object val  = model.getValueAt(row, col);
                    String text = (val != null) ? val.toString() : "";
                    int    tw   = fm.stringWidth(text);
                    int    textX = (col >= 2)
                                   ? x + (colWidths[col] - tw) / 2   // centré
                                   : x + cellPadding;                 // Code → gauche
                    int textLineY = rowY + (rowHeight + fm.getAscent() - fm.getDescent()) / 2;
                    g2d.drawString(text, textX, textLineY);
                }

                x += colWidths[col];
            }

            y += rowHeight;

            if (y > 650) break; // sécurité page
        }

        return y + 10;
    }

    // =========================================================================
    // TOTAUX (noir et blanc)
    // =========================================================================
    private int printTotals(Graphics2D g2d, int margin, int y, int contentWidth) {
        double totalHT     = 0;
        double totalTVA    = 0;
        double totalTTC    = 0;
        double totalRemise = 0;

        for (int i = 0; i < model.getRowCount(); i++) {
            try {
                String totalHTStr  = model.getValueAt(i, 4).toString().replace(",", ".").replaceAll("[^0-9.-]", "");
                String tvaStr      = model.getValueAt(i, 5).toString().replaceAll("[^0-9]", "");
                String totalTTCStr = model.getValueAt(i, 8).toString().replace(",", ".").replaceAll("[^0-9.-]", "");
                String puTTCStr    = model.getValueAt(i, 6).toString().replace(",", ".").replaceAll("[^0-9.-]", "");
                String qteStr      = model.getValueAt(i, 2).toString().replaceAll("[^0-9]", "");

                double totalHTLigne  = Double.parseDouble(totalHTStr);
                int    tva           = Integer.parseInt(tvaStr.isEmpty() ? "19" : tvaStr);
                double totalTTCLigne = Double.parseDouble(totalTTCStr);
                double puTTC         = Double.parseDouble(puTTCStr);
                int    qte           = Integer.parseInt(qteStr);

                double ttcBrut     = puTTC * qte;
                double remiseLigne = ttcBrut - totalTTCLigne;
                if (remiseLigne < 0) remiseLigne = 0;

                totalHT     += totalHTLigne;
                totalTVA    += totalHTLigne * (tva / 100.0);
                totalTTC    += totalTTCLigne;
                totalRemise += remiseLigne;

            } catch (Exception e) {
                System.err.println("Erreur calcul total ligne " + i + ": " + e.getMessage());
            }
        }

        double totalTVAReelle = totalTTC - totalHT;

        int totW = 280;
        int totX = margin + contentWidth - totW;
        int totH = totalRemise > 0 ? 115 : 85;

        g2d.setColor(Color.LIGHT_GRAY);
        g2d.fillRect(totX, y, totW, totH);
        g2d.setColor(Color.BLACK);
        g2d.drawRect(totX, y, totW, totH);

        int ly = y + 18;
        g2d.setFont(new Font("Arial", Font.PLAIN, 10));
        g2d.setColor(Color.BLACK);

        g2d.drawString("Total HT:", totX + 10, ly);
        g2d.drawString(formatMontantDinar(totalHT), totX + 180, ly);
        ly += 15;

        if (totalRemise > 0) {
            g2d.drawString("Remise totale:", totX + 10, ly);
            g2d.drawString("- " + formatMontantDinar(totalRemise), totX + 180, ly);
            ly += 15;
            double totalHTApresRemise = totalHT - totalRemise;
            g2d.drawString("Total HT après remise:", totX + 10, ly);
            g2d.drawString(formatMontantDinar(totalHTApresRemise), totX + 180, ly);
            ly += 15;
        }

        g2d.drawString("TVA (" + getTauxTVAExemple() + "%):", totX + 10, ly);
        g2d.drawString(formatMontantDinar(totalTVAReelle), totX + 180, ly);
        ly += 15;

        g2d.drawLine(totX + 10, ly, totX + totW - 10, ly);
        ly += 12;

        g2d.setFont(new Font("Arial", Font.BOLD, 12));
        g2d.drawString("TOTAL TTC:", totX + 10, ly);
        String ttcStr = formatMontantDinar(totalTTC);
        int    tw     = g2d.getFontMetrics().stringWidth(ttcStr);
        g2d.drawString(ttcStr, totX + totW - tw - 10, ly);

        return y + totH + 10;
    }

    private String getTauxTVAExemple() {
        if (model.getRowCount() > 0) {
            try {
                return model.getValueAt(0, 5).toString().replaceAll("[^0-9]", "");
            } catch (Exception e) {}
        }
        return "19";
    }

    // =========================================================================
    // MODE RÈGLEMENT
    // =========================================================================
    private int printModeReglement(Graphics2D g2d, int margin, int y) {
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.BOLD, 11));
        g2d.drawString("Mode de règlement: " + modePaiement, margin, y);
        return y + 20;
    }

    // =========================================================================
    // MONTANT EN LETTRES
    // =========================================================================
    private int printMontantEnLettres(Graphics2D g2d, int margin, int y, int contentWidth) {
        double totalTTC = 0;
        for (int i = 0; i < model.getRowCount(); i++) {
            try {
                totalTTC += Double.parseDouble(
                    model.getValueAt(i, 8).toString().replace(",", ".").replaceAll("[^0-9.-]", ""));
            } catch (Exception e) {}
        }

        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.ITALIC, 10));
        g2d.drawString("Arrêté le présent bon de livraison à la somme de :", margin, y);

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
        int pageBottom   = (int) pageFormat.getImageableHeight();
        int signatureTop = pageBottom - 150;
        if (y + 15 > signatureTop) signatureTop = y + 15;

        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.BOLD, 11));
        String label  = "Signature et cachet :";
        int    labelW = g2d.getFontMetrics().stringWidth(label);
        int    sigX   = margin + contentWidth - labelW - 20;
        g2d.drawString(label, sigX, signatureTop);

        g2d.setColor(Color.BLACK);
        g2d.drawLine(sigX, signatureTop + 4, margin + contentWidth - 20, signatureTop + 4);

        int rectX = margin + contentWidth / 2;
        int rectY = signatureTop + 12;
        int rectW = contentWidth / 2 - 20;
        int rectH = 100;

        g2d.setColor(Color.WHITE);
        g2d.fillRect(rectX, rectY, rectW, rectH);
        g2d.setColor(Color.BLACK);
        g2d.drawRect(rectX, rectY, rectW, rectH);
    }

    // =========================================================================
    // UTILITAIRES
    // =========================================================================
    private String formatMontantDinar(double montant) {
        DecimalFormatSymbols sym = new DecimalFormatSymbols(Locale.FRANCE);
        sym.setGroupingSeparator(' ');
        DecimalFormat f = new DecimalFormat("#,##0.000", sym);
        String s = f.format(montant).replace(",", ".");
        return s.endsWith(".000") ? s.replace(".", ",") : s;
    }

    private String convertirMontantEnLettres(double montant) {
        try {
            double arrondi = Math.round(montant * 1000.0) / 1000.0;
            int    entier  = (int) arrondi;
            int    decimal = (int) Math.round((arrondi - entier) * 1000);

            StringBuilder sb = new StringBuilder();
            if      (entier == 1) sb.append("un dinar");
            else if (entier  > 1) sb.append(convertirNombreEnLettres(entier)).append(" dinars");
            else                  sb.append("zero dinar");

            if (decimal > 0) {
                sb.append(" et ");
                if      (decimal == 1)   sb.append("un millime");
                else if (decimal < 1000) sb.append(convertirNombreEnLettres(decimal)).append(" millimes");
                else {
                    int c = decimal / 10;
                    if      (c == 1) sb.append("un centime");
                    else if (c  > 1) sb.append(convertirNombreEnLettres(c)).append(" centimes");
                }
            }
            return sb.toString();
        } catch (Exception e) {
            return "Montant non convertible";
        }
    }

    private String convertirNombreEnLettres(int n) {
        if (n == 0) return "zero";
        if (n < 0 || n > 999999999) return "montant trop eleve";

        String[] u = {"","un","deux","trois","quatre","cinq","six","sept","huit","neuf",
                      "dix","onze","douze","treize","quatorze","quinze","seize",
                      "dix-sept","dix-huit","dix-neuf"};
        String[] d = {"","","vingt","trente","quarante","cinquante",
                      "soixante","soixante","quatre-vingt","quatre-vingt"};

        if (n < 20) return u[n];

        if (n < 100) {
            int diz = n / 10, uni = n % 10;
            if (diz == 7 || diz == 9) {
                int t = 10 + uni;
                return t == 10 ? d[diz] + "-dix" : d[diz] + "-" + u[t];
            }
            if (uni == 0) return diz == 8 ? d[diz] + "s" : d[diz];
            if (uni == 1 && diz != 8) return d[diz] + " et un";
            return d[diz] + "-" + u[uni];
        }

        if (n < 1000) {
            int c = n / 100, r = n % 100;
            String res = c == 1 ? "cent" : u[c] + " cent";
            if (r > 0) return res + " " + convertirNombreEnLettres(r);
            return c > 1 ? res + "s" : res;
        }

        if (n < 1000000) {
            int m = n / 1000, r = n % 1000;
            String res = m == 1 ? "mille" : convertirNombreEnLettres(m) + " mille";
            return r > 0 ? res + " " + convertirNombreEnLettres(r) : res;
        }

        if (n < 1000000000) {
            int m = n / 1000000, r = n % 1000000;
            String res = convertirNombreEnLettres(m) + (m > 1 ? " millions" : " million");
            return r > 0 ? res + " " + convertirNombreEnLettres(r) : res;
        }

        return "montant trop eleve";
    }

    private String[] splitText(String text, int max) {
        if (text.length() <= max) return new String[]{text};
        int idx = text.lastIndexOf(" ", max);
        if (idx <= 0) idx = max;
        return new String[]{text.substring(0, idx), text.substring(idx).trim()};
    }

    // =========================================================================
    // LANCER IMPRESSION
    // =========================================================================
    public void imprimer() throws PrinterException {
        System.out.println("Impression BL...");
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName("Bon de Livraison " + numeroBL);
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