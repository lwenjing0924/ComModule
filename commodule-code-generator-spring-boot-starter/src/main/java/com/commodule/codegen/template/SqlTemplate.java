package com.commodule.codegen.template;

import com.commodule.codegen.CodeGeneratorValidator;
import com.commodule.codegen.CodeTemplate;
import com.commodule.codegen.FieldDefinition;
import com.commodule.codegen.GeneratedFile;
import com.commodule.codegen.GenerationRequest;
import com.commodule.codegen.Naming;

import java.util.List;

public class SqlTemplate implements CodeTemplate {

    @Override
    public String name() {
        return "sql";
    }

    @Override
    public List<GeneratedFile> render(GenerationRequest request) {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE IF NOT EXISTS ")
                .append(Naming.normalizedTableName(request.tableName()))
                .append(" (\n");
        sql.append("    id BIGINT PRIMARY KEY");
        for (FieldDefinition field : request.fields()) {
            sql.append(",\n    ")
                    .append(field.columnName())
                    .append(" ")
                    .append(CodeGeneratorValidator.sqlType(field.javaType()));
            if (field.required()) {
                sql.append(" NOT NULL");
            }
        }
        sql.append("\n);\n");
        String path = "src/main/resources/db/" + request.tableName() + ".sql";
        return List.of(new GeneratedFile(path, sql.toString()));
    }
}

