/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.intel.attestationhub.plugin.nova;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import org.apache.commons.lang.StringUtils;

import com.intel.attestationhub.api.Tenant.Plugin;
import com.intel.attestationhub.api.Tenant.Property;
import com.intel.mtwilson.attestationhub.exception.AttestationHubException;

/**
 *
 * @author GS-0681
 */
public class NovaRsClientBuilder {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(NovaRsClientBuilder.class);

    public static NovaRsClient build(Plugin plugin) throws AttestationHubException {
	try {
	    if (plugin == null) {
		throw new AttestationHubException("No configuration provided ");
	    }
	    List<Property> properties = plugin.getProperties();
	    String pluginApiEndpoint = null, pluginAuthEndpoint = null, pluginAuthVersion = null, userName = null,
		    password = null, tenantName = null, domainName = null;
	    for (Property property : properties) {
		switch (property.getKey()) {
		case Constants.API_ENDPOINT:
		    pluginApiEndpoint = property.getValue();
		    break;
		case Constants.AUTH_ENDPOINT:
		    pluginAuthEndpoint = property.getValue();
		    break;
		case Constants.DOMAIN_NAME:
		    domainName = property.getValue();
		    break;
		case Constants.KEYSTONE_VERSION:
		    pluginAuthVersion = property.getValue();
		    break;
		case Constants.PASSWORD:
		    password = property.getValue();
		    break;
		case Constants.USERNAME:
		    userName = property.getValue();
		    break;
		case Constants.TENANT_NAME:
		    tenantName = property.getValue();
		    break;
		}
	    }

	    if (StringUtils.isBlank(pluginAuthEndpoint) || StringUtils.isBlank(pluginAuthVersion)
		    || StringUtils.isBlank(password) || StringUtils.isBlank(userName)) {
		log.error(
			"Configuration not provided : pluginAuthEndpoint: {}, pluginAuthVersion : {}, password: {}, userName: {}",
			pluginAuthEndpoint, pluginAuthVersion, password, userName);
		throw new AttestationHubException("Please provide mandatory configuration for authorization");
	    }

	    URL url = new URL(pluginApiEndpoint); // example:
						  // "http://localhost:8080/";

	    Client client = ClientBuilder.newBuilder().build();
	    // WebTarget target = client.target(url.toExternalForm());
	    return new NovaRsClient(url, client, pluginApiEndpoint, pluginAuthEndpoint, tenantName, userName, password,
		    domainName, pluginAuthVersion);
	} catch (MalformedURLException ex) {
	    throw new AttestationHubException("Invalid endpoints", ex);
	}
    }

}
