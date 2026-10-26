package uy.com.bay.utiles.services;

import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Service;
import uy.com.bay.utiles.data.SupervisionTask;
import uy.com.bay.utiles.dto.SupervisionTaskDTO;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class WordExportService {

    public ByteArrayInputStream generateSupervisionTaskReport(SupervisionTask task) {
        return generateReport(task.getFileName(), task.getEvaluationOutput(), task.getOutput());
    }

    public ByteArrayInputStream generateSupervisionTaskReport(SupervisionTaskDTO task) {
        return generateReport(task.getFileName(), task.getEvaluationOutput(), task.getOutput());
    }

    /**
     * Genera un documento Word a partir de un informe en Markdown (el que devuelve
     * Claude): títulos (#), listas (- / * / 1.), tablas (|) y negritas (**).
     */
    public ByteArrayInputStream generateMarkdownReport(String markdown) {
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            List<String> tableLines = new ArrayList<>();
            for (String rawLine : markdown.split("\\r?\\n")) {
                String line = rawLine.strip();
                if (line.startsWith("|")) {
                    tableLines.add(line);
                    continue;
                }
                if (!tableLines.isEmpty()) {
                    addMarkdownTable(document, tableLines);
                    tableLines.clear();
                }
                if (line.isEmpty()) {
                    continue;
                }
                if (line.startsWith("#")) {
                    int level = 0;
                    while (level < line.length() && line.charAt(level) == '#') {
                        level++;
                    }
                    XWPFParagraph paragraph = document.createParagraph();
                    if (level == 1) {
                        paragraph.setAlignment(ParagraphAlignment.CENTER);
                    }
                    XWPFRun run = paragraph.createRun();
                    run.setText(line.substring(level).strip().replace("**", ""));
                    run.setBold(true);
                    run.setFontSize(level == 1 ? 16 : level == 2 ? 13 : 12);
                } else if (line.startsWith("- ") || line.startsWith("* ")) {
                    XWPFParagraph paragraph = document.createParagraph();
                    paragraph.setIndentationLeft(360);
                    addInlineRuns(paragraph, "• " + line.substring(2).strip());
                } else if (line.matches("^(---+|\\*\\*\\*+)$")) {
                    // Separador horizontal de Markdown: se omite.
                } else {
                    XWPFParagraph paragraph = document.createParagraph();
                    if (line.matches("^\\d+\\.\\s.*")) {
                        paragraph.setIndentationLeft(360);
                    }
                    addInlineRuns(paragraph, line);
                }
            }
            if (!tableLines.isEmpty()) {
                addMarkdownTable(document, tableLines);
            }

            document.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Error generating Word document", e);
        }
    }

    /** Agrega el texto al párrafo respetando los segmentos en negrita (**texto**). */
    private void addInlineRuns(XWPFParagraph paragraph, String text) {
        String[] parts = text.split("\\*\\*", -1);
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].isEmpty()) {
                continue;
            }
            XWPFRun run = paragraph.createRun();
            run.setText(parts[i]);
            run.setBold(i % 2 == 1);
        }
    }

    /** Convierte las líneas de una tabla Markdown en una tabla de Word (la primera fila es el encabezado). */
    private void addMarkdownTable(XWPFDocument document, List<String> lines) {
        List<String[]> rows = new ArrayList<>();
        int columns = 0;
        for (String line : lines) {
            String content = line.substring(1, line.endsWith("|") && line.length() > 1 ? line.length() - 1 : line.length());
            String[] cells = content.split("\\|", -1);
            // Fila separadora del encabezado (|---|:---:|).
            if (String.join("", cells).matches("[\\s:\\-]*")) {
                continue;
            }
            rows.add(cells);
            columns = Math.max(columns, cells.length);
        }
        if (rows.isEmpty()) {
            return;
        }

        XWPFTable table = document.createTable(rows.size(), columns);
        table.setWidth("100%");
        for (int r = 0; r < rows.size(); r++) {
            XWPFTableRow row = table.getRow(r);
            String[] cells = rows.get(r);
            for (int c = 0; c < columns; c++) {
                XWPFParagraph paragraph = row.getCell(c).getParagraphs().get(0);
                String value = c < cells.length ? cells[c].strip() : "";
                if (r == 0) {
                    XWPFRun run = paragraph.createRun();
                    run.setText(value.replace("**", ""));
                    run.setBold(true);
                } else {
                    addInlineRuns(paragraph, value);
                }
            }
        }
        document.createParagraph();
    }

    private ByteArrayInputStream generateReport(String fileName, String evaluationOutput, String output) {
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // Title
            XWPFParagraph titleParagraph = document.createParagraph();
            titleParagraph.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun titleRun = titleParagraph.createRun();
            titleRun.setText("Informe de Supervisión");
            titleRun.setBold(true);
            titleRun.setFontSize(16);
            titleRun.addBreak();

            // Evaluation Output Section
            addSection(document, "Archivo evaluado", fileName);
            
            // Evaluation Output Section
            addSection(document, "Resultado de Evaluación", evaluationOutput);

            // Output Section
            addSection(document, "Output", output);

            document.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Error generating Word document", e);
        }
    }

    private void addSection(XWPFDocument document, String title, String content) {
        // Section Header
        XWPFParagraph headerParagraph = document.createParagraph();
        XWPFRun headerRun = headerParagraph.createRun();
        headerRun.setText(title);
        headerRun.setBold(true);
        headerRun.setFontSize(12);
        headerRun.addBreak(); // Add space after header

        // Section Content
        if (content != null && !content.isEmpty()) {
            XWPFParagraph contentParagraph = document.createParagraph();
            XWPFRun contentRun = contentParagraph.createRun();
            // Handle newlines
            String[] lines = content.split("\n");
            for (int i = 0; i < lines.length; i++) {
                contentRun.setText(lines[i]);
                if (i < lines.length - 1) {
                    contentRun.addBreak();
                }
            }
        } else {
            XWPFParagraph contentParagraph = document.createParagraph();
            XWPFRun contentRun = contentParagraph.createRun();
            contentRun.setText("(Sin contenido)");
            contentRun.setItalic(true);
        }

        // Add spacing after section
        document.createParagraph();
    }
}
