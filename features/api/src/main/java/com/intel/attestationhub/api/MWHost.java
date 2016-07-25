package com.intel.attestationhub.api;

import com.intel.mtwilson.as.rest.v2.model.Host;
import com.intel.mtwilson.as.rest.v2.model.HostAttestation;

public class MWHost {
    private Host host;
    private HostAttestation mwHostAttestation;
    private String samlValidTo;
    private Boolean trusted;

    public Host getHost() {
	return host;
    }

    public void setHost(Host host) {
	this.host = host;
    }

    public HostAttestation getMwHostAttestation() {
	return mwHostAttestation;
    }

    public void setMwHostAttestation(HostAttestation mwHostAttestation) {
	this.mwHostAttestation = mwHostAttestation;
    }

    public String getSamlValidTo() {
	return samlValidTo;
    }

    public void setSamlValidTo(String samlValidTo) {
	this.samlValidTo = samlValidTo;
    }

    public Boolean getTrusted() {
	return trusted;
    }

    public void setTrusted(Boolean trusted) {
	this.trusted = trusted;
    }

}
