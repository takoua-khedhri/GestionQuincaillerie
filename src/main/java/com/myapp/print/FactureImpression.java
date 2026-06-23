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
import com.myapp.db.DatabaseManager;

public class FactureImpression implements Printable {

    private String numeroFacture;
    private String dateFacture;
    private String nomClient;
    private String prenomClient;
    private String adresseClient;
    private String telClient;
    private String matriculeFiscale;
    private String matriculeVoiture;
    private DefaultTableModel model;
    private String remise;
    private ImageIcon logoIcon;
    private String modePaiement;
    private double timbre;
    private double retenueSource;
    private double netAPayer;
    private boolean modePrixGros;

    private DecimalFormat df;
    private DecimalFormat df3;

    public FactureImpression(String numeroFacture, String dateFacture,
                             String nomClient, String prenomClient,
                             String adresseClient, String telClient,
                             String matriculeFiscale,
                             String matriculeVoiture,
                             DefaultTableModel model,
                             String remise, ImageIcon logoIcon,
                             String modePaiement, double timbre,
                             double retenueSource,
                             double netAPayer) {

        this.numeroFacture    = numeroFacture;
        this.dateFacture      = dateFacture;
        this.nomClient        = nomClient;
        this.prenomClient     = prenomClient;
        this.adresseClient    = adresseClient;
        this.telClient        = telClient;
        this.matriculeFiscale = matriculeFiscale;
        this.matriculeVoiture = matriculeVoiture;
        this.model            = model;
        this.remise           = remise;
        this.logoIcon         = logoIcon;
        this.modePaiement     = modePaiement;
        this.timbre           = timbre;
        this.retenueSource    = retenueSource;
        this.netAPayer        = netAPayer;
        this.modePrixGros     = true;

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
            y = printHeader(g2d, margin, y, contentWidth);  // Logo reste en couleur ici
            y += 20;
            y = printNumeroDate(g2d, margin, y, contentWidth);
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
            y += 15;
            printFooter(g2d, margin, y, contentWidth, pageFormat);

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
            g2d.setColor(Color.BLACK);
            g2d.drawRect(margin, y, logoSize, logoSize);
            g2d.setFont(new Font("Arial", Font.BOLD, 14));
            g2d.drawString("LOGO", margin + 20, y + logoSize / 2);
        }

        int infoX = margin + logoSize + 20;
        g2d.setColor(Color.BLACK);  // Texte en noir et blanc

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
    // NUMERO ET DATE
    // =========================================================================
    private int printNumeroDate(Graphics2D g2d, int margin, int y, int contentWidth) {
        g2d.setColor(Color.BLACK);
        g2d.fillRect(margin, y, contentWidth, 30);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        String typeLabel   = numeroFacture.startsWith("TICKET") ? "TICKET N°: " : "FACTURE N°: ";
        String factureText = typeLabel + numeroFacture;
        g2d.drawString(factureText, margin + 10, y + 21);

        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        String dateText = "Date: " + dateFacture;
        int dateWidth = g2d.getFontMetrics().stringWidth(dateText);
        g2d.drawString(dateText, margin + contentWidth - dateWidth - 10, y + 21);

        return y + 35;
    }

    // =========================================================================
    // SECTION CLIENT — retour à la ligne automatique sur chaque champ
    // =========================================================================
    private int printClientSection(Graphics2D g2d, int margin, int y, int contentWidth) {

        Font plainFont = new Font("Arial", Font.PLAIN, 11);
        Font boldFont  = new Font("Arial", Font.BOLD,  12);

        g2d.setFont(plainFont);
        FontMetrics fm = g2d.getFontMetrics();

        int col2X     = margin + contentWidth / 2;
        int col1Width = contentWidth / 2 - 20;   // largeur utile colonne gauche
        int col2Width = contentWidth / 2 - 10;   // largeur utile colonne droite
        int padding   = 10;
        int lineH     = 15;                       // espacement entre deux lignes de texte

        // --- Préparer les textes wrappés ---
        String nomComplet   = nomClient + (prenomClient != null && !prenomClient.isEmpty() ? " " + prenomClient : "");
        String adresseTxt   = adresseClient    != null ? adresseClient    : "";
        String telTxt       = telClient        != null ? telClient        : "";
        String matriculeTxt = matriculeFiscale != null ? matriculeFiscale : "";

        List<String> nomLines     = wrapText("Nom: "       + nomComplet,   fm, col1Width - padding);
        List<String> adresseLines = wrapText("Adresse: "   + adresseTxt,   fm, col1Width - padding);
        List<String> telLines     = wrapText("Tél: "       + telTxt,       fm, col2Width - padding);
        List<String> matLines     = wrapText("Matricule: " + matriculeTxt, fm, col2Width - padding);

        // Hauteur totale = titre (20px) + max(lignes gauche, lignes droite) x lineH + marges
        int leftTotalLines  = nomLines.size() + adresseLines.size();
        int rightTotalLines = telLines.size()  + matLines.size();
        int totalLines      = Math.max(leftTotalLines, rightTotalLines);

        boolean hasVehicule = matriculeVoiture != null && !matriculeVoiture.isEmpty();
        int sectionHeight   = padding + 20 + totalLines * lineH + padding
                              + (hasVehicule ? lineH + 4 : 0);

        // --- Fond et bordure (noir et blanc) ---
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.fillRect(margin, y, contentWidth, sectionHeight);
        g2d.setColor(Color.BLACK);
        g2d.drawRect(margin, y, contentWidth, sectionHeight);

        // --- Titre ---
        g2d.setFont(boldFont);
        g2d.setColor(Color.BLACK);
        g2d.drawString("INFORMATIONS CLIENT", margin + padding, y + 16);
        g2d.drawLine(margin + padding, y + 20, margin + 165, y + 20);

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

        // --- Colonne droite : Tél puis Matricule ---
        int textY2 = y + 20 + lineH;

        for (String line : telLines) {
            g2d.drawString(line, col2X, textY2);
            textY2 += lineH;
        }
        for (String line : matLines) {
            g2d.drawString(line, col2X, textY2);
            textY2 += lineH;
        }

        // --- Véhicule (pleine largeur, en bas) ---
        if (hasVehicule) {
            int vehY = y + sectionHeight - padding - 2;
            g2d.setFont(new Font("Arial", Font.BOLD, 11));
            g2d.setColor(Color.BLACK);
            g2d.drawString("Véhicule: " + matriculeVoiture, margin + padding, vehY);
        }

        return y + sectionHeight + 10;
    }

    // =========================================================================
    // TABLEAU ARTICLES — hauteur de ligne dynamique selon la désignation
    // =========================================================================
    private int printArticlesTable(Graphics2D g2d, int margin, int y, int contentWidth) {

        String[] headers   = {"Code", "Désignation", "Qté", "PU HT", "Total HT", "TVA", "PU TTC", "Remise %", "Total TTC"};
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
        Font        rowFont    = new Font("Arial", Font.PLAIN, 10);
        g2d.setFont(rowFont);
        FontMetrics fm         = g2d.getFontMetrics();
        int         cellPadding = 5;   // marge interne gauche/droite
        int         lineSpacing = 13;  // espacement vertical entre lignes wrappées

        for (int row = 0; row < model.getRowCount(); row++) {

            // Calcul du nombre de lignes nécessaires pour la désignation
            String desig = (model.getValueAt(row, 1) != null)
                           ? model.getValueAt(row, 1).toString() : "";
            List<String> desigLines = wrapText(desig, fm, colWidths[1] - cellPadding * 2);

            // Hauteur dynamique : au moins 25px
            int rowHeight = Math.max(25, desigLines.size() * lineSpacing + 8);
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
                    Object value = model.getValueAt(row, col);
                    String text  = (value != null) ? value.toString() : "";
                    int    tw    = fm.stringWidth(text);
                    int    textX = (col >= 2)
                                   ? x + (colWidths[col] - tw) / 2   // centré horizontalement
                                   : x + cellPadding;                 // Code → aligné gauche
                    // Centrage vertical dans la cellule
                    int textLineY = rowY + (rowHeight + fm.getAscent() - fm.getDescent()) / 2;
                    g2d.drawString(text, textX, textLineY);
                }

                x += colWidths[col];
            }

            y += rowHeight;

            if (y > 580) break; // sécurité : ne pas sortir de la page
        }

        return y + 10;
    }

    // =========================================================================
    // TOTAUX (noir et blanc)
    // =========================================================================
    private int printTotals(Graphics2D g2d, int margin, int y, int contentWidth) {

        double totalHT     = 0.0;
        double totalTVA    = 0.0;
        double totalRemise = 0.0;
        double totalTTC    = 0.0;

        for (int i = 0; i < model.getRowCount(); i++) {
            try {
                String totalHTStr  = model.getValueAt(i, 4).toString().replace(",", ".").replaceAll("[^0-9.-]", "");
                String tvaStr      = model.getValueAt(i, 5).toString().replaceAll("[^0-9]", "");
                String totalTTCStr = model.getValueAt(i, 8).toString().replace(",", ".").replaceAll("[^0-9.-]", "");
                String puTTCStr    = model.getValueAt(i, 6).toString().replace(",", ".").replaceAll("[^0-9.-]", "");
                String qteStr      = model.getValueAt(i, 2).toString().replaceAll("[^0-9]", "");

                double totalHTLigne  = Double.parseDouble(totalHTStr);
                int    tvaPct        = Integer.parseInt(tvaStr.isEmpty() ? "19" : tvaStr);
                double totalTTCLigne = Double.parseDouble(totalTTCStr);
                double puTTC         = Double.parseDouble(puTTCStr);
                int    qte           = Integer.parseInt(qteStr);

                double ttcBrut     = puTTC * qte;
                double remiseLigne = ttcBrut - totalTTCLigne;
                if (remiseLigne < 0) remiseLigne = 0;

                double tvaLigne = totalTTCLigne - (totalTTCLigne / (1.0 + tvaPct / 100.0));

                totalHT     += totalHTLigne;
                totalTVA    += tvaLigne;
                totalTTC    += totalTTCLigne;
                totalRemise += remiseLigne;

            } catch (Exception e) {
                System.err.println("Erreur calcul total ligne " + i + ": " + e.getMessage());
            }
        }

        double totalTTCFinal = totalTTC + timbre;

        int totalsWidth  = 280;
        int totalsX      = margin + contentWidth - totalsWidth;
        int totalsHeight = 115;
        if (totalRemise   > 0) totalsHeight += 35;
        if (retenueSource > 0) totalsHeight += 35;

        g2d.setColor(Color.LIGHT_GRAY);
        g2d.fillRect(totalsX, y, totalsWidth, totalsHeight);
        g2d.setColor(Color.BLACK);
        g2d.drawRect(totalsX, y, totalsWidth, totalsHeight);

        int lineY  = y + 22;
        int labelX = totalsX + 12;
        int valueX = totalsX + totalsWidth - 12;

        g2d.setFont(new Font("Arial", Font.PLAIN, 11));
        g2d.setColor(Color.BLACK);

        // Total HT
        g2d.drawString("Total HT :", labelX, lineY);
        String htStr = formatMontantDinar(totalHT) + " DT";
        g2d.drawString(htStr, valueX - g2d.getFontMetrics().stringWidth(htStr), lineY);
        lineY += 20;

        // Remise
        if (totalRemise > 0) {
            g2d.drawString("Remise :", labelX, lineY);
            String remStr = "- " + formatMontantDinar(totalRemise) + " DT";
            g2d.drawString(remStr, valueX - g2d.getFontMetrics().stringWidth(remStr), lineY);
            lineY += 20;
        }

        // TVA
        g2d.drawString("TVA :", labelX, lineY);
        String tvaStr2 = formatMontantDinar(totalTVA) + " DT";
        g2d.drawString(tvaStr2, valueX - g2d.getFontMetrics().stringWidth(tvaStr2), lineY);
        lineY += 20;

        // Timbre
        g2d.drawString("Timbre :", labelX, lineY);
        String timbreStr = formatMontantDinar(timbre) + " DT";
        g2d.drawString(timbreStr, valueX - g2d.getFontMetrics().stringWidth(timbreStr), lineY);
        lineY += 20;

        // Retenue à la source
        if (retenueSource > 0) {
            g2d.drawString("Retenue à la source (1%) :", labelX, lineY);
            String retStr = "- " + formatMontantDinar(retenueSource) + " DT";
            g2d.drawString(retStr, valueX - g2d.getFontMetrics().stringWidth(retStr), lineY);
            lineY += 20;
        }

        // Séparateur
        g2d.setColor(Color.BLACK);
        g2d.drawLine(labelX, lineY, totalsX + totalsWidth - 12, lineY);
        lineY += 12;

        // Total TTC
        g2d.setFont(new Font("Arial", Font.BOLD, 13));
        g2d.drawString("TOTAL TTC :", labelX, lineY);
        String ttcStr2 = formatMontantDinar(totalTTCFinal) + " DT";
        g2d.drawString(ttcStr2, valueX - g2d.getFontMetrics().stringWidth(ttcStr2), lineY);
        lineY += 22;

        // Net à payer
        if (retenueSource > 0) {
            g2d.setFont(new Font("Arial", Font.BOLD, 14));
            g2d.drawString("NET À PAYER :", labelX, lineY);
            String netStr = formatMontantDinar(netAPayer) + " DT";
            g2d.drawString(netStr, valueX - g2d.getFontMetrics().stringWidth(netStr), lineY);
        }

        return y + totalsHeight + 10;
    }

    // =========================================================================
    // MODE RÈGLEMENT
    // =========================================================================
    private int printModeReglement(Graphics2D g2d, int margin, int y) {
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.BOLD, 11));
        g2d.drawString("Mode de règlement : " + modePaiement, margin, y);
        return y + 20;
    }

    // =========================================================================
    // MONTANT EN LETTRES
    // =========================================================================
    private int printMontantEnLettres(Graphics2D g2d, int margin, int y, int contentWidth) {

        double totalTTC = 0.0;
        for (int i = 0; i < model.getRowCount(); i++) {
            try {
                String ttcStr = model.getValueAt(i, 8).toString().replace(",", ".").replaceAll("[^0-9.-]", "");
                totalTTC += Double.parseDouble(ttcStr);
            } catch (Exception e) {}
        }

        double totalTTCFinal      = totalTTC + timbre;
        double montantPourLettres = (retenueSource > 0) ? netAPayer : totalTTCFinal;

        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.ITALIC, 10));
        String label = (retenueSource > 0)
            ? "Arrêté le net à payer à la somme de :"
            : "Arrêté la présente facture à la somme de :";
        g2d.drawString(label, margin, y);

        String montantEnLettres = convertirMontantEnLettres(montantPourLettres);

        g2d.setFont(new Font("Arial", Font.BOLD, 11));

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
    // FOOTER
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
            String decimaleLettres = (partieDecimale == 0) ? "zéro" : convertirNombreEnLettres(partieDecimale);

            StringBuilder resultat = new StringBuilder();

            if      (partieEntiere == 1) resultat.append("un dinar");
            else if (partieEntiere  > 1) resultat.append(entiereLettres).append(" dinars");
            else                         resultat.append(entiereLettres).append(" dinar");

            if (partieDecimale > 0) {
                resultat.append(" et ");
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
            e.printStackTrace();
            return "Montant non convertible";
        }
    }

    private String convertirNombreEnLettres(int nombre) {
        if (nombre < 0 || nombre > 999999999) return "montant trop élevé";
        if (nombre == 0) return "zéro";

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

        return "montant trop élevé";
    }

    private String[] splitText(String text, int maxLength) {
        if (text.length() <= maxLength) return new String[]{text};
        int splitIndex = text.lastIndexOf(" ", maxLength);
        if (splitIndex <= 0) splitIndex = maxLength;
        return new String[]{
            text.substring(0, splitIndex),
            text.substring(splitIndex).trim()
        };
    }

    // =========================================================================
    // LANCER IMPRESSION
    // =========================================================================
    public void imprimer() throws PrinterException {
        System.out.println("Démarrage de l'impression...");
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName("Facture " + numeroFacture);
        job.setPrintable(this);
        PageFormat pageFormat = job.defaultPage();
        pageFormat.setOrientation(PageFormat.PORTRAIT);
        if (job.printDialog()) {
            System.out.println("Impression confirmée par l'utilisateur");
            job.print();
        } else {
            System.out.println("Impression annulée par l'utilisateur");
            throw new PrinterException("Impression annulée");
        }
    }
}