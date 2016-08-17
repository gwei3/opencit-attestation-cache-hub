package com.intel.attestationhub.api;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * "host_trust_response": { "hostname": "10.35.35.172", "trust": { "bios": true,
 * "vmm": true, "location": false } }
 * 
 * @author GS-0681
 *
 */
public class HostTrustResponse {
    @JsonProperty("hostname")
    private String hostName;
    private Trust trust;
    @JsonProperty("valid_to")
    private String validTo;
    @JsonProperty("trusted")
    private boolean trusted;
    @JsonProperty("asset_tags")
    private List<String> assetTags;

    public String getHostName() {
	return hostName;
    }

    public void setHostName(String hostName) {
	this.hostName = hostName;
    }

    public Trust getTrust() {
	return trust;
    }

    public void setTrust(Trust trust) {
	this.trust = trust;
    }

    public String getValidTo() {
	return validTo;
    }

    public void setValidTo(String validTo) {
	this.validTo = validTo;
    }

    public boolean isTrusted() {
        return trusted;
    }

    public void setTrusted(boolean trusted) {
        this.trusted = trusted;
    }

    public List<String> getAssetTags() {
        return assetTags;
    }

    public void setAssetTags(List<String> assetTags) {
        this.assetTags = assetTags;
    }
    

}

class Trust {
    private boolean bios;
    private boolean vmm;
    private boolean location;

    public boolean isBios() {
	return bios;
    }

    public void setBios(boolean bios) {
	this.bios = bios;
    }

    public boolean isVmm() {
	return vmm;
    }

    public void setVmm(boolean vmm) {
	this.vmm = vmm;
    }

    public boolean isLocation() {
	return location;
    }

    public void setLocation(boolean location) {
	this.location = location;
    }

}
