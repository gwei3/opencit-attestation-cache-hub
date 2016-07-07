package com.intel.attestationhub.api;

import com.intel.mtwilson.as.rest.v2.model.Host;
import com.intel.mtwilson.as.rest.v2.model.HostAttestation;

public class MWHost {
    private Host host;
    private HostAttestation mwHostAttestation;

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

}
