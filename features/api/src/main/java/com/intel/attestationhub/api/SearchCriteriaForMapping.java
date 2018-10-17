package com.intel.attestationhub.api;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.QueryParam;

import org.apache.commons.lang.StringUtils;

public class SearchCriteriaForMapping {
    @QueryParam(value="tenant_id")
    public String tenantId;
    @QueryParam(value="host_hardware_uuid")
    public String hostHardwareUuid;
    

    public String validate(){
	List<String> errors = new ArrayList<>();
	
	if(StringUtils.isBlank(tenantId) && StringUtils.isBlank(hostHardwareUuid)  ){
	    errors.add("Tenant id or hardware uuid is mandatory");
	}
	return StringUtils.join(errors, ", ");
    }
}
