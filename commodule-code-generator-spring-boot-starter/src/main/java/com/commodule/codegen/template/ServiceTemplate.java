package com.commodule.codegen.template;

import com.commodule.codegen.CodeTemplate;
import com.commodule.codegen.GeneratedFile;
import com.commodule.codegen.GenerationRequest;
import com.commodule.codegen.Naming;

import java.util.List;

public class ServiceTemplate implements CodeTemplate {

    @Override
    public String name() {
        return "service";
    }

    @Override
    public List<GeneratedFile> render(GenerationRequest request) {
        String source = """
                package %s.service;

                import %s.domain.%s;
                import org.springframework.stereotype.Service;

                @Service
                public class %sService {

                    public %s findById(Long id) {
                        throw new UnsupportedOperationException("实现 %s 查询逻辑");
                    }
                }
                """.formatted(
                request.basePackage(),
                request.basePackage(),
                request.className(),
                request.className(),
                request.className(),
                request.className()
        );
        String path = "src/main/java/" + Naming.packagePath(request.basePackage())
                + "/service/" + request.className() + "Service.java";
        return List.of(new GeneratedFile(path, source));
    }
}

