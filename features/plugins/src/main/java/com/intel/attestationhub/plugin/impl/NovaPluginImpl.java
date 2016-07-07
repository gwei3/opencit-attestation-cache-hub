package com.intel.attestationhub.plugin.impl;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intel.attestationhub.api.PublishData;
import com.intel.attestationhub.plugin.EndpointPlugin;
import com.intel.mtwilson.attestationhub.common.AttestationHubConfigUtil;
import com.intel.mtwilson.attestationhub.common.Constants;
import com.intel.mtwilson.attestationhub.exception.AttestationHubException;

public class NovaPluginImpl implements EndpointPlugin {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(NovaPluginImpl.class);

    @Override
    public void pushData(List<PublishData> data, String tenantId) throws AttestationHubException {
	String dir = AttestationHubConfigUtil.get(Constants.ATTESTATION_HUB_TENANT_CONFIGURATIONS_PATH);
	File file = new File(dir + File.separator + tenantId + "_nova.txt");
	try {
	    log.info("Creating file to write the data to be published by the nova plugin: ", file.getAbsolutePath());

	    file.createNewFile();
	} catch (IOException e) {
	    String msg = "Error writing data to file";
	    log.error(msg);
	    throw new AttestationHubException(msg, e);
	}

	ObjectMapper mapper = new ObjectMapper();

	try {
	    log.info("Writing data");
	    mapper.writeValue(file, data);
	    log.info("data writing complete");
	} catch (Exception e) {
	    String msg = "Error converting data to JSON ";
	    log.error(msg);
	    throw new AttestationHubException(msg, e);
	}
    }

}
