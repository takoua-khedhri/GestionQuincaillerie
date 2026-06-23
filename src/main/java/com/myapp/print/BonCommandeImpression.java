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

public class BonCommandeImpression implements Printable {

    private static final Logger log = LoggerFactory.getLogger(BonCommandeImpression.class);

    private String numeroBonCommande;
    private String dateBonCommande;

    // Informations fournisseur (existant)
    private String nomFournisseur;
    private String adresseFournisseur;
    private String telFournisseur;
    private String matriculeFournisseur;

    // Informations acheteur (nouveau fournisseur)
    private String nomAcheteur;
    private String prenomAcheteur;
    private String telAcheteur;
    private String emailAcheteur;

    private String observations;

    private DefaultTableModel model;
    private String remiseStr;
    private ImageIcon logoIcon;

    private double totalHT;
    private double totalTTC;
    private double montantRemise;

    public BonCommandeImpression(String numero, String date,
                                 String nomFournisseur, String adresseFournisseur,
                                 String telFournisseur, String matriculeFournisseur,
                                 String nomAcheteur, String prenomAcheteur,
                                 String telAcheteur, String emailAcheteur,
                                 String obs,
                                 DefaultTableModel model, String remiseStr,
                                 ImageIcon logo,
                                 double totalHT, double totalTTC, double montantRemise) {

        this.numeroBonCommande  = numero;
        this.dateBonCommande    = date;

        this.nomFournisseur      = nomFournisseur      != null ? nomFournisseur      : "";
        this.adresseFournisseur  = adresseFournisseur  != null ? adresseFournisseur  : "";
        this.telFournisseur      = telFournisseur      != null ? telFournisseur      : "";
        this.matriculeFournisseur = matriculeFournisseur != null ? matriculeFournisseur : "";

        this.nomAcheteur    = nomAcheteur    != null ? nomAcheteur    : "";
        this.prenomAcheteur = prenomAcheteur != null ? prenomAcheteur : "";
        this.telAcheteur    = telAcheteur    != null ? telAcheteur    : "";
        this.emailAcheteur  = emailAcheteur  != null ? emailAcheteur  : "";

        this.observations  = obs        != null ? obs        : "";
        this.model         = model;
        this.remiseStr     = remiseStr  != null ? remiseStr  : "0";
        this.logoIcon      = logo;
        this.totalHT       = totalHT;
        this.totalTTC      = totalTTC;
        this.montantRemise = montantRemise;
    }

    // =========================================================================
    // UTILITAIRE : découpe un texte en lignes qui tiennent dans maxWidth pixels
    // (identique à FactureImpression)
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
            y = printFournisseurSection(g2d, margin, y, contentWidth);
            y += 15;
            y = printArticlesTable(g2d, margin, y, contentWidth);
            y += 15;
            y = printTotals(g2d, margin, y, contentWidth);
            y += 15;
            y = printObservations(g2d, margin, y, contentWidth);
            y += 10;
            printFooter(g2d, margin, y, contentWidth, pageFormat);

        } catch (Exception e) {
            log.error("Erreur lors de l'impression du Bon de Commande", e);
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
        g2d.drawString("Tél: 94 226 752", infoX, y + 77);
        g2d.drawString("Email: chaa.elec@outlook.fr", infoX, y + 92);
        g2d.drawString("MF: 000/M/A/1981916C", infoX, y + 107);

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
        g2d.drawString("BON DE COMMANDE N°: " + numeroBonCommande, margin + 10, y + 21);

        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        String dateText = "Date: " + dateBonCommande;
        int dateWidth = g2d.getFontMetrics().stringWidth(dateText);
        g2d.drawString(dateText, margin + contentWidth - dateWidth - 10, y + 21);

        return y + 35;
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

        boolean isFournisseurExistant = nomFournisseur != null && !nomFournisseur.isEmpty()
                                        && (nomAcheteur == null || nomAcheteur.isEmpty());

        // --- Préparer les textes wrappés ---
        List<String> col1Lines1, col1Lines2, col2Lines1, col2Lines2;

        if (isFournisseurExistant) {
            col1Lines1 = wrapText("Fournisseur: " + nomFournisseur,       fm, col1Width - padding);
            col1Lines2 = wrapText("Adresse: "     + adresseFournisseur,   fm, col1Width - padding);
            col2Lines1 = wrapText("Tél: "         + telFournisseur,       fm, col2Width - padding);
            col2Lines2 = wrapText("Matricule: "   + matriculeFournisseur, fm, col2Width - padding);
        } else {
            String nomComplet = nomAcheteur + (!prenomAcheteur.isEmpty() ? " " + prenomAcheteur : "");
            col1Lines1 = wrapText("Acheteur: " + nomComplet,  fm, col1Width - padding);
            col1Lines2 = wrapText("Tél: "      + telAcheteur, fm, col1Width - padding);
            col2Lines1 = wrapText("Email: "    + emailAcheteur, fm, col2Width - padding);
            col2Lines2 = new ArrayList<>(); // vide
        }

        // Informations de livraison (colonne droite fixe)
        List<String> livLines1 = wrapText("Lieu de livraison: Tunis",           fm, col2Width - padding);
        List<String> livLines2 = wrapText("Date souhaitée: " + dateBonCommande, fm, col2Width - padding);

        // Utiliser la hauteur maximale entre les deux colonnes
        int leftLines  = col1Lines1.size() + col1Lines2.size();
        int rightLines = Math.max(col2Lines1.size() + col2Lines2.size(),
                                  livLines1.size()  + livLines2.size());
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
        g2d.drawString("INFORMATIONS FOURNISSEUR", margin + padding, y + 16);
        g2d.drawLine(margin + padding, y + 20, margin + 210, y + 20);

        // --- Colonne gauche ---
        g2d.setFont(plainFont);
        g2d.setColor(Color.BLACK);
        int textY = y + 20 + lineH;

        for (String line : col1Lines1) { g2d.drawString(line, margin + padding, textY); textY += lineH; }
        for (String line : col1Lines2) { g2d.drawString(line, margin + padding, textY); textY += lineH; }

        // --- Colonne droite : infos fournisseur/acheteur + livraison ---
        int textY2 = y + 20 + lineH;

        // Partie fournisseur/acheteur (droite)
        if (isFournisseurExistant) {
            for (String line : col2Lines1) { g2d.drawString(line, col2X, textY2); textY2 += lineH; }
            for (String line : col2Lines2) { g2d.drawString(line, col2X, textY2); textY2 += lineH; }
        } else {
            for (String line : col2Lines1) { g2d.drawString(line, col2X, textY2); textY2 += lineH; }
        }

        // Livraison toujours affichée en bas de la colonne droite
        int livY = y + sectionHeight - padding - (livLines1.size() + livLines2.size()) * lineH;
        g2d.setFont(new Font("Arial", Font.ITALIC, 11));
        g2d.setColor(Color.BLACK);
        for (String line : livLines1) { g2d.drawString(line, col2X, livY); livY += lineH; }
        for (String line : livLines2) { g2d.drawString(line, col2X, livY); livY += lineH; }

        return y + sectionHeight + 10;
    }

    // =========================================================================
    // TABLEAU ARTICLES — hauteur de ligne dynamique (noir et blanc)
    // =========================================================================
    private int printArticlesTable(Graphics2D g2d, int margin, int y, int contentWidth) {
        if (model == null || model.getRowCount() == 0) return y;

        String[] headers   = {"Référence", "Désignation", "Qté", "PU Achat", "Remise", "Total HT"};
        int[]    colWidths = {
            (int)(contentWidth * 0.12),   // Référence
            (int)(contentWidth * 0.33),   // Désignation
            (int)(contentWidth * 0.08),   // Qté
            (int)(contentWidth * 0.15),   // PU Achat
            (int)(contentWidth * 0.12),   // Remise
            (int)(contentWidth * 0.20)    // Total HT
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

            // Calcul hauteur dynamique selon la désignation (colonne 1)
            String desig = (model.getValueAt(row, 1) != null)
                           ? model.getValueAt(row, 1).toString() : "";
            List<String> desigLines = wrapText(desig, fm, colWidths[1] - cellPadding * 2);

            int rowHeight = Math.max(25, desigLines.size() * lineSpacing + 8);
            int rowY      = y;

            // Fond alterné (noir et blanc)
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
                    // Désignation multi-lignes, alignée à gauche
                    int textLineY = rowY + lineSpacing;
                    for (String line : desigLines) {
                        g2d.drawString(line, x + cellPadding, textLineY);
                        textLineY += lineSpacing;
                    }
                } else {
                    // Autres colonnes : une ligne, centrée verticalement
                    Object value = (row < model.getRowCount() && col < model.getColumnCount())
                                   ? model.getValueAt(row, col) : null;
                    String text  = (value != null) ? value.toString() : "";
                    int    tw    = fm.stringWidth(text);
                    int    textX = (col >= 2)
                                   ? x + (colWidths[col] - tw) / 2   // centré horizontalement
                                   : x + cellPadding;                 // Référence → aligné gauche
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
        int totalsWidth  = 280;
        int totalsX      = margin + contentWidth - totalsWidth;
        int totalsHeight = 80;
        if (montantRemise > 0) totalsHeight += 20;

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

        // Remise globale
        if (montantRemise > 0) {
            g2d.drawString("Remise Globale (" + remiseStr + "%) :", labelX, lineY);
            String remStr = "- " + formatMontantDinar(montantRemise) + " DT";
            g2d.drawString(remStr, valueX - g2d.getFontMetrics().stringWidth(remStr), lineY);
            lineY += 20;
        }

        // Séparateur
        g2d.setColor(Color.BLACK);
        g2d.drawLine(labelX, lineY, totalsX + totalsWidth - 12, lineY);
        lineY += 12;

        // Net à payer
        g2d.setFont(new Font("Arial", Font.BOLD, 13));
        g2d.drawString("NET À PAYER :", labelX, lineY);
        String ttcStr = formatMontantDinar(totalTTC) + " DT";
        g2d.drawString(ttcStr, valueX - g2d.getFontMetrics().stringWidth(ttcStr), lineY);

        return y + totalsHeight + 10;
    }

    // =========================================================================
    // OBSERVATIONS — avec retour à la ligne automatique (noir et blanc)
    // =========================================================================
    private int printObservations(Graphics2D g2d, int margin, int y, int contentWidth) {
        if (observations == null || observations.trim().isEmpty()) return y;

        Font plainFont = new Font("Arial", Font.ITALIC, 10);
        g2d.setFont(new Font("Arial", Font.BOLD, 11));
        g2d.setColor(Color.BLACK);
        g2d.drawString("Observations :", margin, y);
        y += 15;

        g2d.setFont(plainFont);
        FontMetrics fm = g2d.getFontMetrics();
        List<String> obsLines = wrapText(observations, fm, contentWidth - 10);
        for (String line : obsLines) {
            g2d.drawString(line, margin, y);
            y += 13;
        }

        return y;
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
        String signatureText = "Signature et cachet du fournisseur";
        int    textWidth     = g2d.getFontMetrics().stringWidth(signatureText);
        g2d.drawString(signatureText, sigX + (sigWidth - textWidth) / 2, sigY + 16);

        g2d.setColor(Color.BLACK);
        g2d.drawLine(sigX + 10, sigY + 22, sigX + sigWidth - 10, sigY + 22);

        // Note de retour
        g2d.setFont(new Font("Arial", Font.ITALIC, 10));
        g2d.drawString("Bon de commande à retourner signé", margin, sigY + sigHeight - 10);
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

    // =========================================================================
    // LANCER IMPRESSION
    // =========================================================================
    public void imprimer() throws PrinterException {
        log.info("Démarrage de l'impression Bon de Commande...");
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName("Bon Commande " + numeroBonCommande);
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