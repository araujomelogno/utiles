package uy.com.bay.utiles.data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToMany;

@Entity
public class QuestionEncodingTemplate extends AbstractEntity {

    private LocalDate created;

    private String name;

    @OneToMany(mappedBy = "questionEncodingTemplate", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<ColumnMapping> columnMappings = new ArrayList<>();

    @Lob
    private byte[] codeMappingFileContent;

    public LocalDate getCreated() {
        return created;
    }

    public void setCreated(LocalDate created) {
        this.created = created;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<ColumnMapping> getColumnMappings() {
        return columnMappings;
    }

    public void setColumnMappings(List<ColumnMapping> columnMappings) {
        this.columnMappings = columnMappings;
    }

    public byte[] getCodeMappingFileContent() {
        return codeMappingFileContent;
    }

    public void setCodeMappingFileContent(byte[] codeMappingFileContent) {
        this.codeMappingFileContent = codeMappingFileContent;
    }
}
