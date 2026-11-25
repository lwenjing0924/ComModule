package com.commodule.template.codegen;

import com.commodule.codegen.CodeGenerationException;
import com.commodule.codegen.CodeGenerator;
import com.commodule.codegen.GenerationRequest;
import com.commodule.codegen.GenerationResult;
import com.commodule.web.ApiException;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/generator")
public class CodeGeneratorController {

    private final CodeGenerator codeGenerator;

    public CodeGeneratorController(CodeGenerator codeGenerator) {
        this.codeGenerator = codeGenerator;
    }

    @PostMapping("/preview")
    @PreAuthorize("hasRole('ADMIN')")
    public GenerationResult preview(@RequestBody GenerationRequest request) {
        return generate(request);
    }

    @PostMapping("/zip")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> download(@RequestBody GenerationRequest request) {
        GenerationResult result = generate(request);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(request.className() + ".zip")
                .build();
        byte[] zip = result.toZip();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .contentLength(zip.length)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(zip);
    }

    private GenerationResult generate(GenerationRequest request) {
        try {
            return codeGenerator.generate(request);
        } catch (CodeGenerationException exception) {
            throw ApiException.badRequest(exception.getMessage());
        }
    }
}

