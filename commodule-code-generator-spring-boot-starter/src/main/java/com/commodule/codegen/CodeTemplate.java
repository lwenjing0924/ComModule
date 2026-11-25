package com.commodule.codegen;

import java.util.List;

public interface CodeTemplate {

    String name();

    List<GeneratedFile> render(GenerationRequest request);
}

