package com.intel.attestationhub.plugin.nova.identity;

import com.intel.mtwilson.attestationhub.exception.AttestationHubException;

public interface IdentityService {
    public static final String VERSION_V2 = "v2";
    public static final String VERSION_V3 = "v3";
    public String createAuthToken(String glanceKeystonePublicEndpoint, String tenantOrProjectName, String userName,
	    String password, String domainName) throws AttestationHubException;
    public String getEndpointUrl();
}
