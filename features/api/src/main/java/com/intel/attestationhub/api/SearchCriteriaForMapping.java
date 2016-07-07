package com.intel.attestationhub.api;

import javax.ws.rs.QueryParam;

public class SearchCriteriaForMapping {
    @QueryParam(value="tenant_id")
    public String tenantId;
    @QueryParam(value="host_id")
    public String hostId;
}
