package com.intel.attestationhub.api;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MappingResultResponse {

    public List<TenantToHostMapping> mappings;

    public MappingResultResponse() {
	super();
	mappings = new ArrayList<>();
    }

    public static class TenantToHostMapping {
	@JsonProperty("mapping_id")
	public String mappingId;
	@JsonProperty("tenant_id")
	public String tenantId;
	@JsonProperty("hardware_uuid")
	public String hostHardwareUuid;
    }
}
