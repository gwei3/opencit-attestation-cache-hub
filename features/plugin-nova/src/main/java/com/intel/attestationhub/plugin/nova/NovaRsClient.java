/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.intel.attestationhub.plugin.nova;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;

import com.intel.attestationhub.plugin.nova.identity.IdentityService;
import com.intel.attestationhub.plugin.nova.identity.IdentityServiceFactory;
import com.intel.mtwilson.attestationhub.exception.AttestationHubException;

/**
 * 
 * @author Aakash
 */
public class NovaRsClient {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(NovaRsClient.class);
    public WebTarget webTarget;
    public Client client;
    public String authToken;
    private IdentityService identityService;

    public NovaRsClient(URL url, Client client, String apiEndpoint, String keystonePublicEndpoint,
	    String tenanatOrProjectName, String username, String password, String domainName, String version)
	    throws AttestationHubException {
	// this.webTarget = url;
	this.client = client;
	validateUrl(apiEndpoint, "API");
	createAuthToken(keystonePublicEndpoint, tenanatOrProjectName, username, password, domainName, version);
	createEndpointUrl(apiEndpoint);
    }

    private void createEndpointUrl(String apiEndpoint) throws AttestationHubException {
	if (StringUtils.isBlank(authToken)) {
	    throw new AttestationHubException("No auth token available");
	}
	if (StringUtils.isBlank(apiEndpoint)) {
	    throw new AttestationHubException("No API endpoint configured ");
	}
	String endpointUrl = identityService.getEndpointUrl();
	try {
	    URL url = new URL(endpointUrl);
	    String path = url.getPath();
	    endpointUrl = apiEndpoint + path + "/os-hypervisors";
	    url = new URL(endpointUrl);
	    webTarget = client.target(url.toExternalForm());
	} catch (MalformedURLException e) {
	    throw new AttestationHubException("Invalied endpoint url " + endpointUrl, e);
	}
    }

    private void createAuthToken(String glanceKeystonePublicEndpoint, String tenantOrProjectName, String userName,
	    String password, String domainName, String version) throws AttestationHubException {

	if (IdentityService.VERSION_V2.equalsIgnoreCase(version)) {
	    identityService = IdentityServiceFactory.getIdentityService(IdentityService.VERSION_V2);
	} else if (IdentityService.VERSION_V3.equalsIgnoreCase(version)) {
	    identityService = IdentityServiceFactory.getIdentityService(IdentityService.VERSION_V3);
	}
	if (identityService == null) {
	    log.error("Invalid auth version configured: {}", version);
	    throw new AttestationHubException("Invalid auth version configured: "+version);
	}
	authToken = identityService.createAuthToken(glanceKeystonePublicEndpoint, tenantOrProjectName, userName,
		password, domainName);

	log.info("Created auth token using {} version: {}", version, authToken);

    }

    private void validateUrl(String urlStr, String type) throws AttestationHubException {

	ValidationUtil.validateUrl(urlStr, type);

    }

    protected void sendDataToEndpoint(String jsonData) throws AttestationHubException {
	HttpClient httpClient = HttpClientBuilder.create().build();

	String url = webTarget.getUri().toString();
	HttpPost postRequest = new HttpPost(url);
	log.debug("upload Image  uri:: " + postRequest.getURI());
	postRequest.setHeader(Constants.AUTH_TOKEN, authToken);
	HttpEntity entity;
	try {
	    entity = new StringEntity(jsonData);
	} catch (UnsupportedEncodingException e1) {
	    throw new AttestationHubException(e1);
	}
	postRequest.setEntity(entity);
	postRequest.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
	HttpResponse response;
	try {
	    response = httpClient.execute(postRequest);
	} catch (Exception e) {
	    log.error("Glance, uploadImage failed", e);
	    throw new AttestationHubException("Sending data to controller failed", e);
	}
	int status = response.getStatusLine().getStatusCode();
	if (!(HttpStatus.SC_OK == status)) {
	    log.error("Uploading data from hub to nova failed with status: {}", response.getStatusLine());
	    throw new AttestationHubException(
		    "Uploading data from hub to nova failed with error: " + response.getStatusLine());
	}

    }
}
