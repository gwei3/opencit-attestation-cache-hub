package com.intel.attestationhub.mtwclient;

import java.util.Properties;

import com.intel.mtwilson.attestation.client.jaxrs.HostAttestations;
import com.intel.mtwilson.attestation.client.jaxrs.Hosts;
import com.intel.mtwilson.attestationhub.exception.AttestationHubException;

public class MtwClientFactory {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MtwClientFactory.class);

    public static Hosts getHostsClient(Properties properties) throws AttestationHubException {
	Hosts hostsService;
	try {
	    hostsService = new Hosts(properties);
	    log.info("Initialize MTW client for fetching hosts");
	} catch (Exception e) {
	    String errorMsg = "Error creating client for MTW ";
	    log.error(errorMsg, e);
	    throw new AttestationHubException(errorMsg, e);
	}
	return hostsService;
    }

    public static HostAttestations getHostAttestationsClient(Properties properties) throws AttestationHubException {
	HostAttestations hostAttestationsService;
	try {
	    hostAttestationsService = new HostAttestations(properties);
	    log.info("Initialize MTW client for fetching host attestations");
	} catch (Exception e) {
	    String errorMsg = "Error creating client for MTW ";
	    log.error(errorMsg, e);
	    throw new AttestationHubException(errorMsg, e);
	}
	return hostAttestationsService;
    }
}
