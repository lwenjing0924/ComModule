package com.commodule.codegen;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public record GenerationResult(List<GeneratedFile> files) {

    public GenerationResult {
        files = List.copyOf(files);
    }

    public byte[] toZip() {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
             ZipOutputStream zip = new ZipOutputStream(output, StandardCharsets.UTF_8)) {
            for (GeneratedFile file : files) {
                zip.putNextEntry(new ZipEntry(file.path()));
                zip.write(file.content().getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
            zip.finish();
            return output.toByteArray();
        } catch (IOException exception) {
            throw new CodeGenerationException("生成 ZIP 失败", exception);
        }
    }
}

