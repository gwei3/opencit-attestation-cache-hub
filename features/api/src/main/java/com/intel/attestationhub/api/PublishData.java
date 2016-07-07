package com.intel.attestationhub.api;

public class PublishData {
    private String hostName;
    private String samlAssertion;

    public String getHostName() {
	return hostName;
    }

    public void setHostName(String hostName) {
	this.hostName = hostName;
    }

    public String getSamlAssertion() {
	return samlAssertion;
    }

    public void setSamlAssertion(String samlAssertion) {
	this.samlAssertion = samlAssertion;
    }

    public PublishData(String hostName, String samlAssertion) {
	this.hostName = hostName;
	this.samlAssertion = samlAssertion;
    }
}
