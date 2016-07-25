/**
 * This class manage all plugins configured with attestation hub.
 *  It calls all attestation plugins configured for a tenent with 
 *  attestation info of valid hosts
 */
package com.intel.attestationhub.manager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.intel.attestationhub.api.HostDetails;
import com.intel.attestationhub.api.PublishData;
import com.intel.attestationhub.api.Tenant;
import com.intel.attestationhub.api.Tenant.Plugin;
import com.intel.attestationhub.plugin.EndpointPlugin;
import com.intel.attestationhub.plugin.EndpointPluginFactory;
import com.intel.attestationhub.service.AttestationHubService;
import com.intel.attestationhub.service.impl.AttestationHubServiceImpl;
import com.intel.mtwilson.attestationhub.controller.AhTenantJpaController;
import com.intel.mtwilson.attestationhub.data.AhHost;
import com.intel.mtwilson.attestationhub.data.AhMapping;
import com.intel.mtwilson.attestationhub.data.AhTenant;
import com.intel.mtwilson.attestationhub.exception.AttestationHubException;
import com.intel.mtwilson.attestationhub.service.PersistenceServiceFactory;

/**
 * @author Vijay Prakash
 */
public class PluginManager {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PluginManager.class);

    private static PluginManager pluginManager = null;

    public static PluginManager getInstance() {
	if (pluginManager == null) {
	    pluginManager = new PluginManager();
	}
	return pluginManager;
    }

    public void synchAttestationInfo() {
	log.info("Calling out plugins to push host data");

	List<AhTenant> ahTenantList = retrievAllTenants();
	if (ahTenantList == null) {
	    return;
	}
	log.info("Fetched {} tenants", ahTenantList.size());

	AttestationHubService attestationHubService = AttestationHubServiceImpl.getInstance();
	for (AhTenant ahTenant : ahTenantList) {
	    Tenant readTenantConfig;
	    try {
		readTenantConfig = attestationHubService.readTenantConfig(ahTenant.getId());
		log.info("Retrieved configuration for the tenant: {}", ahTenant.getId());
	    } catch (AttestationHubException e) {
		log.error("Error reading configuration for the tenant {}", ahTenant.getId(), e);
		continue;
	    }

	    List<Plugin> plugins = readTenantConfig.getPlugins();
	    Collection<AhMapping> ahMappingCollection = ahTenant.getAhMappingCollection();
	    List<HostDetails> hostsData = new ArrayList<HostDetails>();
	    for (AhMapping ahMapping : ahMappingCollection) {
		if (ahMapping.getDeleted() != null && ahMapping.getDeleted()) {
		    log.info("Mapping {} is not active. Skipping. ", ahMapping.getId());
		    continue;
		}

		String hostHardwareUuid = ahMapping.getHostHardwareUuid();
		AhHost host;
		try {
		    host = attestationHubService.findActiveHostByHardwareUuid(hostHardwareUuid);
		} catch (AttestationHubException e) {
		    log.error("Unable to find an active host with hardware id={}", hostHardwareUuid, e);
		    continue;
		}
		HostDetails details = populateHostDetails(host);
		hostsData.add(details);
	    }
	    if (hostsData.size() == 0) {
		log.info("No host data available for tenant: {}", ahTenant.getId());
		continue;
	    }
	    log.info("Publishing data to the configured plugins for the tenant: {}", ahTenant.getId());
	    processDataToPlugins(ahTenant, hostsData, plugins);
	}
	log.info("Publishing data to plugins complete");
    }

    private List<AhTenant> retrievAllTenants() {
	PersistenceServiceFactory persistenceServiceFactory = PersistenceServiceFactory.getInstance();
	AhTenantJpaController tenantController = persistenceServiceFactory.getTenantController();
	List<AhTenant> ahTenantList = tenantController.findAhTenantEntities();
	List<AhTenant> activeTenants = null;

	if (ahTenantList == null) {
	    log.info("No tenants configured");
	}

	activeTenants = new ArrayList<AhTenant>();
	for (AhTenant ahTenant : ahTenantList) {
	    if (ahTenant.getDeleted() != null && ahTenant.getDeleted()) {
		log.info("Tenant {} is not active. Skipping. ", ahTenant.getId());
		continue;
	    }
	    activeTenants.add(ahTenant);
	}
	log.info("Fetched {} tenants", ahTenantList.size());
	return activeTenants;
    }

    private HostDetails populateHostDetails(AhHost host) {
	HostDetails details = new HostDetails();
	details.hardwareUuid = host.getHardwareUuid();
	details.uuid = host.getId();
	details.hardwareUuid = host.getHardwareUuid();
	details.trust_report = host.getTrustTagsJson();
	details.signed_trust_report = host.getTrustTagsJson();
	details.valid_to = host.getValidTo();
	details.host_name = host.getHostName();
	details.trusted = host.getTrusted();
	return details;

    }

    private void processDataToPlugins(AhTenant ahTenant, List<HostDetails> hostsData, List<Plugin> plugins) {
	if (plugins == null || hostsData == null || ahTenant == null) {
	    return;
	}
	for (Plugin plugin : plugins) {
	    try {
		PublishData data = new PublishData();
		data.tenantId = ahTenant.getId();
		data.hostDetailsList = hostsData;
		EndpointPlugin endpointPlugin = EndpointPluginFactory.getPluginImpl(plugin.getName());
		log.info("Before pushing data to plugin : {} of tenant {}", plugin.getName(), ahTenant.getTenantName());
		endpointPlugin.pushData(data, plugin);
		log.info("After pushing data for plugin : {} of tenant {}", plugin.getName(), ahTenant.getTenantName());
	    } catch (AttestationHubException e) {
		log.error("Error pushing data to plugin{}", plugin.getName(), e);
		continue;
	    }
	}

    }
}
