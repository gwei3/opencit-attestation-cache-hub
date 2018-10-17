package com.intel.attestationhub.plugin;

import com.intel.attestationhub.api.PublishData;
import com.intel.attestationhub.api.Tenant.Plugin;
import com.intel.mtwilson.attestationhub.exception.AttestationHubException;

public interface EndpointPlugin {
    public void pushData(PublishData data, Plugin plugin) throws AttestationHubException;
}
