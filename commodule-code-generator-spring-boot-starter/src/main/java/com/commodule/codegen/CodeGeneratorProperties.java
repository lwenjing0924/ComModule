package com.commodule.codegen;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "commodule.code-generator")
public class CodeGeneratorProperties {

    private boolean enabled = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}

