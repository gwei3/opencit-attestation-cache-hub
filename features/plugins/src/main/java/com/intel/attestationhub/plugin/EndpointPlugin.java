package com.intel.attestationhub.plugin;

import java.util.List;

import com.intel.attestationhub.api.PublishData;
import com.intel.mtwilson.attestationhub.exception.AttestationHubException;

public interface EndpointPlugin {
    public void pushData(List<PublishData> data, String tenantId) throws AttestationHubException;
}
