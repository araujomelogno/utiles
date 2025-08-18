package uy.com.bay.utiles.controllers;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uy.com.bay.utiles.data.ExpenseTransferFile;
import uy.com.bay.utiles.services.ExpenseTransferFileService;

@RestController
@RequestMapping("/api/files")
public class FileDownloadController {

    private final ExpenseTransferFileService expenseTransferFileService;

    public FileDownloadController(ExpenseTransferFileService expenseTransferFileService) {
        this.expenseTransferFileService = expenseTransferFileService;
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
}
