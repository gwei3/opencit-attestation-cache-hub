package com.intel.attestationhub.api;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MappingResultResponse {

    @JsonProperty("invalid_host_ids")
    public List<String> invalidHostIds;
    public List<TenantToHostMapping> mappings;

    public MappingResultResponse() {
	super();
	invalidHostIds = new ArrayList<>();
	mappings = new ArrayList<>();
    }

    public static class TenantToHostMapping {
	@JsonProperty("mapping_id")
	public String mappingId;
	@JsonProperty("tenant_id")
	public String tenantId;
	@JsonProperty("host_id")
	public String hostId;
    }
}
