package com.commodule.filestorage;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

import java.nio.file.Path;

@ConfigurationProperties(prefix = "commodule.file-storage")
public class FileStorageProperties {

    private String provider = "local";
    private Path baseDir = Path.of(System.getProperty("java.io.tmpdir"), "commodule-files");
    private DataSize maxFileSize = DataSize.ofMegabytes(10);

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public Path getBaseDir() {
        return baseDir;
    }

    public void setBaseDir(Path baseDir) {
        this.baseDir = baseDir;
    }

    public DataSize getMaxFileSize() {
        return maxFileSize;
    }

    public void setMaxFileSize(DataSize maxFileSize) {
        this.maxFileSize = maxFileSize;
    }
}

