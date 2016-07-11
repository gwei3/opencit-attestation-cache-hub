package com.intel.attestationhub.api;

import java.util.List;

import com.intel.attestationhub.api.Tenant.Plugin;

public class PublishData {
    public List<HostDetails> hostDetailsList ;
    public String tenantId;
    public String tenantName;
    public Plugin plugin;

}
