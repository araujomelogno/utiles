package uy.com.bay.utiles.data;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToMany;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
public class EncodingTask extends AbstractEntity {

    private Date created;

    @Enumerated(EnumType.STRING)
    private Status status;

    private String baseFilename;
    private String codesFileName;

    @Lob
    private byte[] baseFile;

    @Lob
    private byte[] encodedBaseFile;

    @Lob
    private byte[] codesFile;

    private Date processed;

    @OneToMany(mappedBy = "encodingTask", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<CodingInstruction> instructions = new ArrayList<>();

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getBaseFilename() {
        return baseFilename;
    }

    public void setBaseFilename(String baseFilename) {
        this.baseFilename = baseFilename;
    }

    public String getCodesFileName() {
        return codesFileName;
    }

    public void setCodesFileName(String codesFileName) {
        this.codesFileName = codesFileName;
    }

    public byte[] getBaseFile() {
        return baseFile;
    }

    public void setBaseFile(byte[] baseFile) {
        this.baseFile = baseFile;
    }

    public byte[] getEncodedBaseFile() {
        return encodedBaseFile;
    }

    public void setEncodedBaseFile(byte[] encodedBaseFile) {
        this.encodedBaseFile = encodedBaseFile;
    }

    public byte[] getCodesFile() {
        return codesFile;
    }

    public void setCodesFile(byte[] codesFile) {
        this.codesFile = codesFile;
    }

    public Date getProcessed() {
        return processed;
    }

    public void setProcessed(Date processed) {
        this.processed = processed;
    }

    public List<CodingInstruction> getInstructions() {
        return instructions;
    }

    public void setInstructions(List<CodingInstruction> instructions) {
        this.instructions = instructions;
    }
}
