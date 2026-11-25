package com.commodule.template.file;

import com.commodule.filestorage.FileStorage;
import com.commodule.filestorage.StoredFile;
import com.commodule.filestorage.StoredObject;
import com.commodule.web.ApiException;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileStorage fileStorage;

    public FileController(FileStorage fileStorage) {
        this.fileStorage = fileStorage;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public StoredObject upload(@RequestPart("file") MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            return fileStorage.store(inputStream, file.getOriginalFilename(), file.getContentType());
        } catch (IOException exception) {
            throw ApiException.badRequest("文件读取失败");
        }
    }

    @GetMapping
    public ResponseEntity<Resource> download(@RequestParam String key) {
        StoredFile storedFile = fileStorage.load(key)
                .orElseThrow(() -> ApiException.notFound("文件不存在"));
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(storedFile.metadata().originalFilename(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(resolveContentType(storedFile.metadata().contentType()))
                .contentLength(storedFile.metadata().size())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(storedFile.resource());
    }

    private MediaType resolveContentType(String contentType) {
        try {
            return MediaType.parseMediaType(contentType);
        } catch (InvalidMediaTypeException exception) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    @DeleteMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Boolean> delete(@RequestParam String key) {
        return Map.of("deleted", fileStorage.delete(key));
    }
}

