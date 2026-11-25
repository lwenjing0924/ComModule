package com.commodule.datapermission;

import com.commodule.auth.AuthPrincipal;

public interface DataPermissionRule {

    DataPermissionDecision decide(DataPermissionRequest request, AuthPrincipal principal);
}

