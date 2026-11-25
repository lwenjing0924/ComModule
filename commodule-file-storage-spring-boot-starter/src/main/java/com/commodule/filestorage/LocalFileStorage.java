package com.commodule.filestorage;

import jakarta.annotation.PostConstruct;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.regex.Pattern;

public class LocalFileStorage implements FileStorage {

    private static final DateTimeFormatter DATE_PATH = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    private static final Pattern EXTENSION_PATTERN = Pattern.compile("[A-Za-z0-9]{1,16}");
    private static final String META_SUFFIX = ".meta";

    private final FileStorageProperties properties;
    private final Path baseDir;

    public LocalFileStorage(FileStorageProperties properties) {
        this.properties = properties;
        this.baseDir = properties.getBaseDir().toAbsolutePath().normalize();
    }

    @PostConstruct
    public void initialize() {
        try {
            Files.createDirectories(baseDir);
        } catch (IOException exception) {
            throw new FileStorageException("无法创建文件存储目录: " + baseDir, exception);
        }
    }

    @Override
    public StoredObject store(InputStream inputStream, String originalFilename, String contentType) {
        if (inputStream == null) {
            throw new FileStorageException("文件内容不能为空");
        }
        String filename = sanitizeFilename(originalFilename);
        String key = buildKey(filename);
        Path target = resolve(key);
        Path temporary = temporaryPath(target);
        try {
            Files.createDirectories(target.getParent());
            long size = copyLimited(inputStream, temporary);
            Instant createdAt = Instant.now();
            StoredObject metadata = new StoredObject(key, filename, normalizeContentType(contentType), size, createdAt);
            moveIntoPlace(temporary, target);
            writeMetadata(target, metadata);
            return metadata;
        } catch (FileStorageException exception) {
            deleteQuietly(temporary);
            deleteQuietly(target);
            throw exception;
        } catch (IOException exception) {
            deleteQuietly(temporary);
            deleteQuietly(target);
            throw new FileStorageException("文件写入失败: " + key, exception);
        } finally {
            deleteQuietly(temporary);
        }
    }

    @Override
    public Optional<StoredFile> load(String key) {
        Path target = resolve(key);
        if (!Files.isRegularFile(target)) {
            return Optional.empty();
        }
        StoredObject metadata = readMetadata(target, key);
        Resource resource = new PathResource(target);
        return Optional.of(new StoredFile(metadata, resource));
    }

    @Override
    public boolean delete(String key) {
        Path target = resolve(key);
        boolean deleted;
        try {
            deleted = Files.deleteIfExists(target);
            Files.deleteIfExists(metadataPath(target));
        } catch (IOException exception) {
            throw new FileStorageException("文件删除失败: " + key, exception);
        }
        return deleted;
    }

    @Override
    public boolean exists(String key) {
        return Files.isRegularFile(resolve(key));
    }

    private String buildKey(String filename) {
        String extension = extensionOf(filename);
        String suffix = extension.isBlank() ? "" : "." + extension;
        return LocalDate.now().format(DATE_PATH) + "/" + UUID.randomUUID().toString().replace("-", "") + suffix;
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

    private String extensionOf(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            return "";
        }
        String extension = filename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
        return EXTENSION_PATTERN.matcher(extension).matches() ? extension : "";
    }

    private String normalizeContentType(String contentType) {
        return contentType == null || contentType.isBlank() ? "application/octet-stream" : contentType;
    }

    private long copyLimited(InputStream inputStream, Path target) throws IOException {
        long limit = properties.getMaxFileSize().toBytes();
        long total = 0;
        byte[] buffer = new byte[8192];
        try (OutputStream outputStream = Files.newOutputStream(target)) {
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                total += read;
                if (total > limit) {
                    throw new FileStorageException("文件大小超过限制: " + properties.getMaxFileSize());
                }
                outputStream.write(buffer, 0, read);
            }
        }
        return total;
    }

    private void writeMetadata(Path target, StoredObject metadata) throws IOException {
        Properties values = new Properties();
        values.setProperty("originalFilename", metadata.originalFilename());
        values.setProperty("contentType", metadata.contentType());
        values.setProperty("size", Long.toString(metadata.size()));
        values.setProperty("createdAt", metadata.createdAt().toString());
        Path metadataPath = metadataPath(target);
        try (OutputStream outputStream = Files.newOutputStream(metadataPath)) {
            values.store(outputStream, null);
        }
    }

    private void moveIntoPlace(Path temporary, Path target) throws IOException {
        try {
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private Path temporaryPath(Path target) {
        return target.resolveSibling("." + target.getFileName() + ".part-" + UUID.randomUUID());
    }

    private StoredObject readMetadata(Path target, String key) {
        Path metadataPath = metadataPath(target);
        String filename = target.getFileName().toString();
        String contentType = "application/octet-stream";
        long size;
        Instant createdAt;
        try {
            size = Files.size(target);
            createdAt = Files.getLastModifiedTime(target).toInstant();
        } catch (IOException exception) {
            throw new FileStorageException("无法读取文件信息: " + key, exception);
        }

        if (!Files.isRegularFile(metadataPath)) {
            return new StoredObject(key, filename, contentType, size, createdAt);
        }

        Properties values = new Properties();
        try (InputStream inputStream = Files.newInputStream(metadataPath)) {
            values.load(inputStream);
            filename = values.getProperty("originalFilename", filename);
            contentType = values.getProperty("contentType", contentType);
            size = Long.parseLong(values.getProperty("size", Long.toString(size)));
            createdAt = Instant.parse(values.getProperty("createdAt", createdAt.toString()));
        } catch (IOException | RuntimeException exception) {
            throw new FileStorageException("无法读取文件元数据: " + key, exception);
        }
        return new StoredObject(key, filename, contentType, size, createdAt);
    }

    private Path metadataPath(Path target) {
        return target.resolveSibling(target.getFileName() + META_SUFFIX);
    }

    private Path resolve(String key) {
        if (key == null || key.isBlank()) {
            throw new FileStorageException("文件 key 不能为空");
        }
        Path resolved = baseDir.resolve(key).normalize();
        if (!resolved.startsWith(baseDir) || resolved.equals(baseDir)) {
            throw new FileStorageException("非法文件 key");
        }
        return resolved;
    }

    private void deleteQuietly(Path target) {
        try {
            Files.deleteIfExists(target);
            Files.deleteIfExists(metadataPath(target));
        } catch (IOException ignored) {
        }
    }
}

