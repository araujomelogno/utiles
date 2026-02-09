package uy.com.bay.utiles.services;

import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.stereotype.Service;
import uy.com.bay.utiles.data.SupervisionTask;
import uy.com.bay.utiles.dto.SupervisionTaskDTO;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
public class WordExportService {

    public ByteArrayInputStream generateSupervisionTaskReport(SupervisionTask task) {
        return generateReport(task.getFileName(), task.getEvaluationOutput(), task.getOutput());
    }

    public ByteArrayInputStream generateSupervisionTaskReport(SupervisionTaskDTO task) {
        return generateReport(task.getFileName(), task.getEvaluationOutput(), task.getOutput());
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
