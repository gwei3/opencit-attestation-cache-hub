package com.intel.attestationhub.plugin.nova.identity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Date;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.HttpClientBuilder;

import com.intel.attestationhub.plugin.nova.ValidationUtil;
import com.intel.mtwilson.attestationhub.exception.AttestationHubException;

public abstract class AbstractIdentityService implements IdentityService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AbstractIdentityService.class);
    protected String responseStr;
    protected String endpointUrl;
    protected HttpResponse httpResponse;

    public abstract String getAuthEndpoint(String glanceKeystonePublicEndpoint);

    public abstract String getAuthRequestBody(String tenantOrDomainName, String userName, String password,
	    String domainName) throws AttestationHubException;

    public abstract String getAuthTokenFromResponse() throws AttestationHubException;

    public abstract String getApiEndpointFromAuthResponse() throws AttestationHubException;

    public String createAuthToken(String keystoneEndpoint, String tenantOrProjectName, String userName,
	    String password, String domainName) throws AttestationHubException {
	String authToken = null;
	long start = new Date().getTime();
	HttpClient httpClient = HttpClientBuilder.create().build();

	String authEndpoint = getAuthEndpoint(keystoneEndpoint);

	ValidationUtil.validateUrl(keystoneEndpoint, "AUTH");

	HttpPost postRequest = new HttpPost(authEndpoint);

	String body = getAuthRequestBody(tenantOrProjectName, userName, password, domainName);

	HttpEntity entity;
	try {
	    entity = new ByteArrayEntity(body.getBytes("UTF-8"));
	} catch (UnsupportedEncodingException e2) {
	    log.error("Error while creating auth token", e2);
	    throw new AttestationHubException("Error while creating auth token", e2);
	}

	postRequest.setEntity(entity);
	postRequest.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
	postRequest.setHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
	try {
	    httpResponse = httpClient.execute(postRequest);
	} catch (ClientProtocolException e1) {
	    log.error("Error while creating auth token", e1);
	    throw new AttestationHubException("Error while creating auth token", e1);
	} catch (Exception e1) {
	    log.error("Error while creating auth token", e1);
	    throw new AttestationHubException("Error while creating auth token", e1);
	}
	if ((httpResponse.getStatusLine().getStatusCode() != 200)
		&& (httpResponse.getStatusLine().getStatusCode() != 201)) {
	    log.info("Unable to fetch token by " + authEndpoint + ", statusline:" + httpResponse.getStatusLine());
	    throw new AttestationHubException("Unable to authenticate");

	}
	responseStr = getHttpResponseString();
	authToken = getAuthTokenFromResponse();
	endpointUrl = getApiEndpointFromAuthResponse();
	long end = new Date().getTime();
	printTimeDiff("createAuthToken", start, end);

	return authToken;
    }
    
    public String getEndpointUrl(){
	return endpointUrl;
    }

    protected String getHttpResponseString() throws AttestationHubException {
	BufferedReader br = null;
	StringBuffer sb = new StringBuffer();
	try {
	    br = new BufferedReader(new InputStreamReader((httpResponse.getEntity().getContent())));
	    String output;
	    while ((output = br.readLine()) != null) {
		sb.append(output);
	    }
	} catch (Exception e) {
	    log.error("Error while creating auth token", e);
	    throw new AttestationHubException("Error while converting response to string", e);
	} finally {
	    if (br != null) {
		try {
		    br.close();
		} catch (IOException e) {
		    log.error("Error closing reader", e);
		}
	    }
	}
	String res = sb.toString();
	if (StringUtils.isBlank(res)) {
	    throw new AttestationHubException("No response from auth request. Not fetching the auth token");
	}
	return res;

    }

    private void printTimeDiff(String method, long start, long end) {
	log.debug(method + " took " + (end - start) + " ms");
    }

    public void validateParams(String tenantName, String userName, String password) throws AttestationHubException {
	if (StringUtils.isBlank(tenantName)) {
	    throw new AttestationHubException("Tenant/Project Name cannot be blank");
	}
	if (StringUtils.isBlank(userName)) {
	    throw new AttestationHubException("Tenant Name cannot be blank");
	}
	if (StringUtils.isBlank(password)) {
	    throw new AttestationHubException("Tenant Name cannot be blank");
	}

    }
}
