package com.commodule.datapermission;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "commodule.data-permission")
public class DataPermissionProperties {

    private boolean enabled = true;
    private String adminRole = "ADMIN";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getAdminRole() {
        return adminRole;
    }

    public void setAdminRole(String adminRole) {
        this.adminRole = adminRole;
    }
}

