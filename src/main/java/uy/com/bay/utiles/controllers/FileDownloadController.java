package uy.com.bay.utiles.controllers;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uy.com.bay.utiles.data.ExpenseTransferFile;
import uy.com.bay.utiles.data.JournalEntry;
import uy.com.bay.utiles.data.Study;
import uy.com.bay.utiles.data.Surveyor;
import uy.com.bay.utiles.services.ExcelReportGenerator;
import uy.com.bay.utiles.services.ExpenseTransferFileService;
import uy.com.bay.utiles.services.JournalEntryService;
import uy.com.bay.utiles.services.StudyService;
import uy.com.bay.utiles.services.SurveyorService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/files")
public class FileDownloadController {

    private final ExpenseTransferFileService expenseTransferFileService;
    private final JournalEntryService journalEntryService;
    private final SurveyorService surveyorService;
    private final StudyService studyService;

    public FileDownloadController(ExpenseTransferFileService expenseTransferFileService, JournalEntryService journalEntryService, SurveyorService surveyorService, StudyService studyService) {
        this.expenseTransferFileService = expenseTransferFileService;
        this.journalEntryService = journalEntryService;
        this.surveyorService = surveyorService;
        this.studyService = studyService;
    }

    @GetMapping("/{fileId}")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long fileId) {
        ExpenseTransferFile file = expenseTransferFileService.get(fileId)
                .orElseThrow(() -> new RuntimeException("File not found with id " + fileId));

        ByteArrayResource resource = new ByteArrayResource(file.getContent());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(file.getContent().length)
                .body(resource);
    }

    @GetMapping("/report")
    public ResponseEntity<Resource> downloadReport(
            @RequestParam(required = false) String fechaDesde,
            @RequestParam(required = false) String fechaHasta,
            @RequestParam(required = false) List<Long> surveyorIds,
            @RequestParam(required = false) List<Long> studyIds) throws IOException {

        LocalDate desde = fechaDesde != null ? LocalDate.parse(fechaDesde) : null;
        LocalDate hasta = fechaHasta != null ? LocalDate.parse(fechaHasta) : null;

        Set<Surveyor> surveyors = new HashSet<>();
        if (surveyorIds != null) {
            surveyorIds.forEach(id -> surveyorService.get(id).ifPresent(surveyors::add));
        }

        Set<Study> studies = new HashSet<>();
        if (studyIds != null) {
            studyIds.forEach(id -> studyService.get(id).ifPresent(studies::add));
        }

        List<JournalEntry> journalEntries = journalEntryService.list(journalEntryService.createFilterSpecification(desde, hasta, surveyors, studies));

        ByteArrayOutputStream baos = ExcelReportGenerator.generateExcel(journalEntries);
        ByteArrayResource resource = new ByteArrayResource(baos.toByteArray());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=reporte_gastos.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(baos.size())
                .body(resource);
    }
}
