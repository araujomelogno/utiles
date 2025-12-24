package uy.com.bay.utiles.dto;

import java.io.InputStream;

public class AudioFile {
    private final String filename;
    private final InputStream inputStream;

    public AudioFile(String filename, InputStream inputStream) {
        this.filename = filename;
        this.inputStream = inputStream;
    }

    public String getFilename() {
        return filename;
    }

    public InputStream getInputStream() {
        return inputStream;
    }
}
