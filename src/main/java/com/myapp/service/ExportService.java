package com.myapp.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.TableModel;
import java.awt.Component;
import java.io.File;
import java.io.FileOutputStream;

public class ExportService {

    private static final Logger log = LoggerFactory.getLogger(ExportService.class);

    public static void exporterExcel(Component parent, JTable table, String titre) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Exporter en Excel");
        chooser.setFileFilter(new FileNameExtensionFilter("Fichier Excel (*.xlsx)", "xlsx"));
        chooser.setSelectedFile(new File(titre + ".xlsx"));

        if (chooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) return;

        File file = chooser.getSelectedFile();
        if (!file.getName().endsWith(".xlsx")) {
            file = new File(file.getAbsolutePath() + ".xlsx");
        }

        try (Workbook workbook = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(file)) {

            Sheet sheet = workbook.createSheet(titre);
            TableModel model = table.getModel();

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Font whiteFont = workbook.createFont();
            whiteFont.setBold(true);
            whiteFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(whiteFont);

            Row headerRow = sheet.createRow(0);
            for (int col = 0; col < model.getColumnCount(); col++) {
                Cell cell = headerRow.createCell(col);
                cell.setCellValue(model.getColumnName(col));
                cell.setCellStyle(headerStyle);
            }

            for (int row = 0; row < model.getRowCount(); row++) {
                Row dataRow = sheet.createRow(row + 1);
                for (int col = 0; col < model.getColumnCount(); col++) {
                    Cell cell = dataRow.createCell(col);
                    Object value = model.getValueAt(row, col);
                    if (value == null) {
                        cell.setCellValue("");
                    } else if (value instanceof Number) {
                        cell.setCellValue(((Number) value).doubleValue());
                    } else {
                        cell.setCellValue(value.toString());
                    }
                }
            }

            for (int col = 0; col < model.getColumnCount(); col++) {
                sheet.autoSizeColumn(col);
            }

            workbook.write(fos);
            JOptionPane.showMessageDialog(parent,
                    "Export Excel reussi !\n" + file.getAbsolutePath(),
                    "Succes", JOptionPane.INFORMATION_MESSAGE);
            log.info("Export Excel : {}", file.getAbsolutePath());

        } catch (Exception e) {
            log.error("Erreur export Excel", e);
            JOptionPane.showMessageDialog(parent,
                    "Erreur lors de l'export : " + e.getMessage(),
                    "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void exporterPDF(Component parent, JTable table, String titre) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Exporter en PDF");
        chooser.setFileFilter(new FileNameExtensionFilter("Fichier PDF (*.pdf)", "pdf"));
        chooser.setSelectedFile(new File(titre + ".pdf"));

        if (chooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) return;

        File file = chooser.getSelectedFile();
        if (!file.getName().endsWith(".pdf")) {
            file = new File(file.getAbsolutePath() + ".pdf");
        }

        try {
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, new FileOutputStream(file));
            document.open();

            com.itextpdf.text.Font titleFont = new com.itextpdf.text.Font(
                    com.itextpdf.text.Font.FontFamily.HELVETICA, 18,
                    com.itextpdf.text.Font.BOLD, BaseColor.DARK_GRAY);
            Paragraph titleParagraph = new Paragraph(titre, titleFont);
            titleParagraph.setAlignment(Element.ALIGN_CENTER);
            titleParagraph.setSpacingAfter(20);
            document.add(titleParagraph);

            TableModel model = table.getModel();
            PdfPTable pdfTable = new PdfPTable(model.getColumnCount());
            pdfTable.setWidthPercentage(100);

            com.itextpdf.text.Font headerFont = new com.itextpdf.text.Font(
                    com.itextpdf.text.Font.FontFamily.HELVETICA, 10,
                    com.itextpdf.text.Font.BOLD, BaseColor.WHITE);

            for (int col = 0; col < model.getColumnCount(); col++) {
                PdfPCell cell = new PdfPCell(new Phrase(model.getColumnName(col), headerFont));
                cell.setBackgroundColor(new BaseColor(44, 62, 80));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setPadding(8);
                pdfTable.addCell(cell);
            }

            com.itextpdf.text.Font cellFont = new com.itextpdf.text.Font(
                    com.itextpdf.text.Font.FontFamily.HELVETICA, 9);

            for (int row = 0; row < model.getRowCount(); row++) {
                for (int col = 0; col < model.getColumnCount(); col++) {
                    Object value = model.getValueAt(row, col);
                    PdfPCell cell = new PdfPCell(new Phrase(
                            value != null ? value.toString() : "", cellFont));
                    cell.setPadding(5);
                    if (row % 2 == 0) {
                        cell.setBackgroundColor(new BaseColor(245, 245, 245));
                    }
                    pdfTable.addCell(cell);
                }
            }

            document.add(pdfTable);
            document.close();

            JOptionPane.showMessageDialog(parent,
                    "Export PDF reussi !\n" + file.getAbsolutePath(),
                    "Succes", JOptionPane.INFORMATION_MESSAGE);
            log.info("Export PDF : {}", file.getAbsolutePath());

        } catch (Exception e) {
            log.error("Erreur export PDF", e);
            JOptionPane.showMessageDialog(parent,
                    "Erreur lors de l'export : " + e.getMessage(),
                    "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }
}
