package com.intel.attestationhub.mapper;

import java.util.Date;

import org.apache.commons.lang.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intel.attestationhub.api.MWHost;
import com.intel.dcsg.cpg.io.UUID;
import com.intel.mtwilson.as.rest.v2.model.Host;
import com.intel.mtwilson.as.rest.v2.model.HostAttestation;
import com.intel.mtwilson.attestationhub.data.AhHost;
import com.intel.mtwilson.datatypes.HostTrustResponse;
import com.intel.mtwilson.shiro.ShiroUtil;

public class HostMapper {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(HostMapper.class);

    public static AhHost mapHostToAhHost(MWHost host, AhHost ahHost, String user) {
	Host citHost = host.getHost();
	HostAttestation citHostAttestation = host.getMwHostAttestation();
	Date currentdate = new Date();
	String currentUser = user;
	if (StringUtils.isBlank(user)) {
	    currentUser = "admin";
	}

	if (ahHost == null) {
	    ahHost = new AhHost();
	    ahHost.setId(citHost.getId().toString());
	    ahHost.setCreatedBy(currentUser);
	}
	ahHost.setModifiedBy(currentUser);
	ahHost.setModifiedDate(currentdate);
	ahHost.setAikCertificate(citHost.getAikCertificate());
	ahHost.setAikSha1(citHost.getAikSha1());
	ahHost.setBiosMleUuid(citHost.getBiosMleUuid().toString());
	ahHost.setConnectionUrl(citHost.getConnectionUrl());
	ahHost.setHardwareUuid(citHost.getHardwareUuid().toString());
	ahHost.setHostName(citHost.getName());

	if (citHostAttestation != null) {
	    ahHost.setSamlReport(citHostAttestation.getSaml());
	    HostTrustResponse hostTrustResponse = citHostAttestation.getHostTrustResponse();
	    ObjectMapper objectMapper = new ObjectMapper();
	    try {
		ahHost.setTrustTagsJson(objectMapper.writeValueAsString(hostTrustResponse));
	    } catch (JsonProcessingException e) {
		log.error(
			"Unable to parse the 'host_trust_response' from the host attestation response for host: {} and name: {}",
			citHost.getId(), citHost.getName());
	    }
	}

	return ahHost;
    }

    public static AhHost mapHostToAhHost(MWHost host, AhHost ahHost) {
	log.info("Before getting logged in user");
	String currentUser = ShiroUtil.subjectUsername();
	log.info("After getting logged in user");
	return mapHostToAhHost(host, ahHost, currentUser);

    }

    public static Host mapAhHostToCitHost(AhHost ahHost) {
	Host host = new Host();
	host.setId(new UUID(ahHost.getId().getBytes()));
	host.setAikCertificate(ahHost.getAikCertificate());
	host.setAikSha1(ahHost.getAikSha1());
	host.setBiosMleUuid(ahHost.getBiosMleUuid());
	host.setConnectionUrl(ahHost.getConnectionUrl());
	host.setHardwareUuid(ahHost.getHardwareUuid());
	host.setName(ahHost.getHostName());
	return host;
    }
}
