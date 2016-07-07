package com.intel.attestationhub.mtwclient;

import java.io.File;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateEncodingException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;

import com.intel.attestationhub.api.MWHost;
import com.intel.dcsg.cpg.configuration.Configuration;
import com.intel.mtwilson.Folders;
import com.intel.mtwilson.api.ApiException;
import com.intel.mtwilson.as.rest.v2.model.Host;
import com.intel.mtwilson.as.rest.v2.model.HostAttestation;
import com.intel.mtwilson.as.rest.v2.model.HostAttestationCollection;
import com.intel.mtwilson.as.rest.v2.model.HostAttestationFilterCriteria;
import com.intel.mtwilson.as.rest.v2.model.HostCollection;
import com.intel.mtwilson.as.rest.v2.model.HostFilterCriteria;
import com.intel.mtwilson.attestation.client.jaxrs.HostAttestations;
import com.intel.mtwilson.attestation.client.jaxrs.Hosts;
import com.intel.mtwilson.attestationhub.common.AttestationHubConfigUtil;
import com.intel.mtwilson.attestationhub.common.Constants;
import com.intel.mtwilson.attestationhub.controller.AhHostJpaController;
import com.intel.mtwilson.attestationhub.controller.exceptions.NonexistentEntityException;
import com.intel.mtwilson.attestationhub.data.AhHost;
import com.intel.mtwilson.attestationhub.exception.AttestationHubException;
import com.intel.mtwilson.attestationhub.service.PersistenceServiceFactory;
import com.intel.mtwilson.configuration.ConfigurationFactory;
import com.intel.mtwilson.configuration.ConfigurationProvider;
import com.intel.mtwilson.saml.TrustAssertion;

public class AttestationServiceClient {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AttestationServiceClient.class);

    private static Properties mtwProperties = new Properties();
    static {
	File hubPropertiesFile = new File(
		Folders.configuration() + File.separator + Constants.ATTESTATION_HUB_PROPRRTIES_FILE_NAME);

	try {
	    ConfigurationProvider provider = ConfigurationFactory.createConfigurationProvider(hubPropertiesFile);
	    Configuration loadedConfiguration = provider.load();
	    mtwProperties.setProperty(Constants.MTWILSON_API_PASSWORD,
		    loadedConfiguration.get(Constants.MTWILSON_API_PASSWORD));
	    mtwProperties.setProperty(Constants.MTWILSON_API_TLS, loadedConfiguration.get(Constants.MTWILSON_API_TLS));
	    mtwProperties.setProperty(Constants.MTWILSON_API_URL, loadedConfiguration.get(Constants.MTWILSON_API_URL));
	    mtwProperties.setProperty(Constants.MTWILSON_API_USER,
		    loadedConfiguration.get(Constants.MTWILSON_API_USER));
	} catch (IOException e) {
	    String errorMsg = "Error reading configuration for MTW client";
	    log.error(errorMsg, e);
	    mtwProperties = null;
	}
    }

    public static AttestationServiceClient getInstance() {
	return new AttestationServiceClient();
    }

    public Map<String, MWHost> fetchHostAttestations(List<Host> hosts) throws AttestationHubException {
	if (mtwProperties == null) {
	    throw new AttestationHubException("Configuration parameters for MTW client are not initialized");
	}

	if (hosts == null || hosts.size() == 0) {
	    log.info("No hosts passed to the method to fetch the attestations");
	    return null;
	}
	log.info("Fetching host attestations");
	Map<String, MWHost> hostIdToMwHostMap = new HashMap<>(hosts.size());
	HostAttestations hostAttestationsService = MtwClientFactory.getHostAttestationsClient(mtwProperties);
	for (Host host : hosts) {
	    String hostId = host.getId().toString();
	    log.info("Retrieveing attestation for host: {}", hostId);
	    HostAttestationFilterCriteria criteria = new HostAttestationFilterCriteria();
	    criteria.nameEqualTo = host.getName();
	    criteria.limit = 1;
	    HostAttestationCollection searchHostAttestations = null;
	    try {
		searchHostAttestations = hostAttestationsService.searchHostAttestations(criteria);
	    } catch (Exception e) {
		log.error("Unable to get host attestations for host with ID={} and name={}", host.getId().toString(),
			host.getName(), e);
		continue;
	    }
	    if (searchHostAttestations != null && searchHostAttestations.getHostAttestations() != null
		    && searchHostAttestations.getHostAttestations().size() > 0) {
		MWHost mwHost = new MWHost();
		HostAttestation hostAttestation = searchHostAttestations.getHostAttestations().get(0);
		mwHost.setHost(host);
		mwHost.setMwHostAttestation(hostAttestation);
		hostIdToMwHostMap.put(hostId, mwHost);
		log.info("Received attestation with ID: {} for host ID : {} and name : {}", hostAttestation.getId(),
			host.getId(), host.getName());
	    }
	}

	log.info("Returning the hosts and host attestations");
	return hostIdToMwHostMap;
    }

    public List<Host> fetchHosts() throws AttestationHubException {
	if (mtwProperties == null) {
	    throw new AttestationHubException("Configuration parameters for MTW client are not initialized");
	}
	log.info("Fetching ALL hosts from Attestation Service");
	List<Host> hosts = null;
	Hosts hostsService = MtwClientFactory.getHostsClient(mtwProperties);
	HostFilterCriteria criteria = new HostFilterCriteria();
	criteria.filter = false;
	HostCollection objCollection = null;
	try {
	    objCollection = hostsService.searchHosts(criteria);
	} catch (Exception e) {
	    log.error("Error while fetching hosts from Attestation Service as part of poller", e);
	    throw new AttestationHubException(e);
	}
	if (objCollection != null && objCollection.getHosts() != null && objCollection.getHosts().size() > 0) {
	    hosts = objCollection.getHosts();
	    log.info("Call to MTW get hosts returned {} hosts", hosts.size());
	}
	log.info("Returning hosts list");
	return hosts;
    }

    public Map<String, MWHost> fetchHostAttestations(String lastDateTimeFromLastRunFile)
	    throws AttestationHubException {
	if (mtwProperties == null) {
	    throw new AttestationHubException("Configuration parameters for MTW client are not initialized");
	}

	if (StringUtils.isBlank(lastDateTimeFromLastRunFile)) {
	    log.info("No last run time to fetch the attestations");
	    return null;
	}

	log.info("Fetching host attestations added since {}", lastDateTimeFromLastRunFile);
	Map<String, MWHost> hostIdToMwHostMap = new HashMap<>();
	HostAttestations hostAttestationsService = MtwClientFactory.getHostAttestationsClient(mtwProperties);
	Hosts hostsService = MtwClientFactory.getHostsClient(mtwProperties);

	HostAttestationFilterCriteria criteria = new HostAttestationFilterCriteria();
	criteria.createdDate = lastDateTimeFromLastRunFile;
	HostAttestationCollection searchHostAttestations = null;
	try {
	    searchHostAttestations = hostAttestationsService.searchHostAttestations(criteria);
	} catch (Exception e) {
	    log.error("Unable to get host attestations for created date : {}", lastDateTimeFromLastRunFile, e);
	    return null;
	}

	if (searchHostAttestations != null && searchHostAttestations.getHostAttestations() != null
		&& searchHostAttestations.getHostAttestations().size() > 0) {
	    List<HostAttestation> hostAttestations = searchHostAttestations.getHostAttestations();
	    for (HostAttestation hostAttestation : hostAttestations) {
		String hostUuid = hostAttestation.getHostUuid();
		MWHost mwHost = new MWHost();
		mwHost.setMwHostAttestation(hostAttestation);
		Host citHost = hostsService.retrieveHost(hostUuid);
		mwHost.setHost(citHost);
		hostIdToMwHostMap.put(hostUuid, mwHost);
	    }
	}

	log.info("Returning the hosts and host attestations");
	return hostIdToMwHostMap;
    }

    public void updateHostsForSamlTimeout() throws AttestationHubException {
	log.info("updating deleted status of hosts depending on the expiry of saml");
	PersistenceServiceFactory persistenceServiceFactory = PersistenceServiceFactory.getInstance();
	AhHostJpaController ahHostJpaController = persistenceServiceFactory.getHostController();
	HostAttestations hostAttestationsService = MtwClientFactory.getHostAttestationsClient(mtwProperties);
	List<AhHost> ahHostEntities = ahHostJpaController.findAhHostEntities();
	log.info("Fetched {} hosts from attests hub db", ahHostEntities.size());
	String samlTimeoutStr = AttestationHubConfigUtil.get(Constants.ATTESTATION_HUB_SAML_TIMEOUT, "90");
	int samlTimeout = 0;
	try {
	    samlTimeout = Integer.parseInt(samlTimeoutStr);
	} catch (NumberFormatException numberFormatException) {
	    log.error("Error converting timeout time {} to int. Setting to default of 90", samlTimeoutStr,
		    numberFormatException);
	}

	for (AhHost ahHost : ahHostEntities) {
	    log.info("Processing saml verification for host: {}",ahHost.getId());
	    String samlReport = ahHost.getSamlReport();
	    TrustAssertion verifyTrustAssertion = null;
	    try {
		verifyTrustAssertion = hostAttestationsService.verifyTrustAssertion(samlReport);
	    } catch (KeyManagementException e) {
		log.error("KeyManagementException: Error verifying saml", e);
	    } catch (CertificateEncodingException e) {
		log.error("CertificateEncodingException: Error verifying saml", e);
	    } catch (KeyStoreException e) {
		log.error("KeyStoreException: Error verifying saml", e);
	    } catch (NoSuchAlgorithmException e) {
		log.error("NoSuchAlgorithmException: Error verifying saml", e);
	    } catch (UnrecoverableEntryException e) {
		log.error("UnrecoverableEntryException: Error verifying saml", e);
	    } catch (ApiException e) {
		log.error("ApiException: Error verifying saml", e);
	    }
	    
	    if (verifyTrustAssertion == null) {
		log.info("No verification report for host: {}", ahHost.getId());
		continue;
	    }

	    Date issueDate = verifyTrustAssertion.getDate();
	    DateTime samlIssueDateTime = new DateTime(issueDate);
	    DateTime samlExpiryDateTime = samlIssueDateTime.plusMinutes(samlTimeout);
	    log.info("Host : {} has saml issue date : {} ", ahHost.getId(), samlIssueDateTime.toDate());
	    log.info("(samlExpiryDateTime.isAfterNow() : {}", samlExpiryDateTime.isAfterNow() );
	    log.info("samlExpiryDateTime.isEqualNow(): {}", samlExpiryDateTime.isEqualNow());
	    if (samlExpiryDateTime.isAfterNow() || samlExpiryDateTime.isEqualNow()) {
		log.info("Marking host : {} as deleted as the saml issue date is {} and expiring now which is {}",
			ahHost.getId(), samlIssueDateTime.toDate(), new Date());

		ahHost.setDeleted(true);
		try {
		    ahHostJpaController.edit(ahHost);
		} catch (NonexistentEntityException e) {
		    log.error("Unable to delete the host as host with id: {} does not exist in the DB ", ahHost.getId(),
			    e);
		} catch (Exception e) {
		    log.error("Unable to delete the host as host with id: {}", ahHost.getId(), e);
		}
	    }
	}
	log.info("Update of deleted status of hosts depending on the expiry of saml completed");
    }

}
