package com.intel.attestationhub.plugin.nova;

import java.io.File;
import java.io.IOException;

import org.apache.commons.lang.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intel.attestationhub.api.PublishData;
import com.intel.attestationhub.api.Tenant.Plugin;
import com.intel.attestationhub.plugin.EndpointPlugin;
import com.intel.mtwilson.attestationhub.common.AttestationHubConfigUtil;
import com.intel.mtwilson.attestationhub.common.Constants;
import com.intel.mtwilson.attestationhub.exception.AttestationHubException;

public class NovaPluginImpl implements EndpointPlugin {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(NovaPluginImpl.class);

    public void pushDataToFile(PublishData data, Plugin plugin) throws AttestationHubException {
	String dir = AttestationHubConfigUtil.get(Constants.ATTESTATION_HUB_TENANT_CONFIGURATIONS_PATH);
	File file = new File(dir + File.separator + data.tenantId + "_nova.txt");
	try {
	    log.debug("Creating file to write the data to be published by the nova plugin: ", file.getAbsolutePath());
	    file.createNewFile();
	} catch (IOException e) {
	    String msg = "Error writing data to file";
	    log.error(msg);
	    throw new AttestationHubException(msg, e);
	}

	ObjectMapper mapper = new ObjectMapper();

	try {
	    log.debug("Begin publishing nova plugin data");
	    mapper.writeValue(file, data);
	    log.debug("End publishing nova plugin data");
	} catch (Exception e) {
	    String msg = "Error converting data to JSON ";
	    log.error(msg);
	    throw new AttestationHubException(msg, e);
	}
    }

    @Override
    public void pushData(PublishData data, Plugin plugin) throws AttestationHubException {	
	pushDataToFile(data, plugin);
	log.info("Pushed data to be published to a file");
	NovaRsClient novaRsClient = NovaRsClientBuilder.build(plugin);
	ObjectMapper mapper = new ObjectMapper();
	String jsonData;
	try {
	    log.debug("Begin publishing nova plugin data");
	    jsonData = mapper.writeValueAsString(data);
	    log.debug("End publishing nova plugin data");
	} catch (Exception e) {
	    String msg = "Error converting data to JSON ";
	    log.error(msg);
	    throw new AttestationHubException(msg, e);
	}
	if(StringUtils.isBlank(jsonData)){
	    log.info("no data to publish");
	    return;
	}
	novaRsClient.sendDataToEndpoint(jsonData);

	log.info("Data successfully pushed to the endpoint");
    }
}
