package com.commodule.filestorage;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemoryFileStorage implements FileStorage {

    private final FileStorageProperties properties;
    private final ConcurrentMap<String, Entry> files = new ConcurrentHashMap<>();

    public InMemoryFileStorage(FileStorageProperties properties) {
        this.properties = properties;
    }

    @Override
    public StoredObject store(InputStream inputStream, String originalFilename, String contentType) {
        if (inputStream == null) {
            throw new FileStorageException("文件内容不能为空");
        }
        String filename = sanitizeFilename(originalFilename);
        byte[] content = readLimited(inputStream);
        String key = UUID.randomUUID().toString().replace("-", "");
        StoredObject metadata = new StoredObject(
                key,
                filename,
                contentType == null || contentType.isBlank() ? "application/octet-stream" : contentType,
                content.length,
                Instant.now()
        );
        files.put(key, new Entry(metadata, content));
        return metadata;
    }

    @Override
    public Optional<StoredFile> load(String key) {
        Entry entry = files.get(key);
        if (entry == null) {
            return Optional.empty();
        }
        Resource resource = new NamedByteArrayResource(entry.content(), entry.metadata().originalFilename());
        return Optional.of(new StoredFile(entry.metadata(), resource));
    }

    @Override
    public boolean delete(String key) {
        return key != null && files.remove(key) != null;
    }

    @Override
    public boolean exists(String key) {
        return key != null && files.containsKey(key);
    }

    private byte[] readLimited(InputStream inputStream) {
        long limit = properties.getMaxFileSize().toBytes();
        long total = 0;
        byte[] buffer = new byte[8192];
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                total += read;
                if (total > limit) {
                    throw new FileStorageException("文件大小超过限制: " + properties.getMaxFileSize());
                }
                outputStream.write(buffer, 0, read);
            }
            return outputStream.toByteArray();
        } catch (IOException exception) {
            throw new FileStorageException("文件读取失败", exception);
        }
    }

    private String sanitizeFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "file";
        }
        String normalized = originalFilename.replace('\\', '/');
        String filename = normalized.substring(normalized.lastIndexOf('/') + 1);
        String cleaned = filename.replaceAll("[\\p{Cntrl}]", "").trim();
        return cleaned.isBlank() ? "file" : cleaned;
    }

    private record Entry(StoredObject metadata, byte[] content) {
    }

    private static final class NamedByteArrayResource extends ByteArrayResource {

        private final String filename;

        private NamedByteArrayResource(byte[] byteArray, String filename) {
            super(byteArray);
            this.filename = filename;
        }

        @Override
        public String getFilename() {
            return filename;
        }
    }
}

