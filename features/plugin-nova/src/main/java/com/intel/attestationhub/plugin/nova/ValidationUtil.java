package com.intel.attestationhub.plugin.nova;

import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.lang.StringUtils;

import com.intel.mtwilson.attestationhub.exception.AttestationHubException;

public class ValidationUtil {
    public static void validateUrl(String urlStr, String type) throws AttestationHubException {
	if (StringUtils.isBlank(urlStr)) {
	    throw new AttestationHubException("Invalid " + type + " url");
	}

	URL url;
	try {
	    url = new URL(urlStr);
	} catch (MalformedURLException e) {
	    throw new AttestationHubException("Invalid " + type + " url");
	}
	String hostByUser = url.getHost();
	if (StringUtils.isBlank(hostByUser)) {
	    throw new AttestationHubException("Error validating " + type + " endpoint. No host specified. ");
	}
	if (StringUtils.isBlank(url.getProtocol())) {
	    throw new AttestationHubException("Error validating " + type + " endpoint. No protocol specified. ");
	}

	if (url.getPort() == -1) {
	    throw new AttestationHubException("Error validating " + type + " endpoint. No port specified.");
	}

	String path = url.getPath();
	if (StringUtils.isNotBlank(path)) {
	    throw new AttestationHubException("Please provide the " + type + " endpoint in format http(s)://HOST:PORT");
	}
    }
}
