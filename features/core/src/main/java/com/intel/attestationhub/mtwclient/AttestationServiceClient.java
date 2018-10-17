package com.intel.attestationhub.mtwclient;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.intel.attestationhub.api.MWHost;
import com.intel.dcsg.cpg.configuration.Configuration;
import com.intel.dcsg.cpg.crypto.CryptographyException;
import com.intel.dcsg.cpg.extensions.Extensions;
import com.intel.mtwilson.Folders;
import com.intel.mtwilson.api.ApiException;
import com.intel.mtwilson.api.ClientException;
import com.intel.mtwilson.as.rest.v2.model.*;
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
import com.intel.mtwilson.policy.TrustReport;
import com.intel.mtwilson.saml.TrustAssertion;
import com.intel.mtwilson.tls.policy.factory.TlsPolicyCreator;
import com.intel.mtwilson.user.management.client.jaxrs.Users;
import com.intel.mtwilson.user.management.rest.v2.model.UserCollection;
import com.intel.mtwilson.user.management.rest.v2.model.UserFilterCriteria;
import com.intel.mtwilson.v2.client.MwClientUtil;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateEncodingException;
import java.util.*;

@SuppressWarnings("deprecation")
public class AttestationServiceClient {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AttestationServiceClient.class);

    private static Properties mtwProperties = new Properties();
    private static Properties mtwPropertiesForverification = new Properties();
    private static AttestationServiceClient attestationServiceClient = null;


    private AttestationServiceClient() throws AttestationHubException {
        Extensions.register(TlsPolicyCreator.class,
                com.intel.mtwilson.tls.policy.creator.impl.CertificateDigestTlsPolicyCreator.class);
        populateAttestationServiceProperties();
        URL server = null;
        try {
            server = new URL(AttestationHubConfigUtil.get(Constants.MTWILSON_API_URL));
        } catch (MalformedURLException e) {
            log.error("Error forming Attestation Service URL", e);
            throw new AttestationHubException(e);
        }
        String user = AttestationHubConfigUtil.get(Constants.MTWILSON_API_USER);
        String password = AttestationHubConfigUtil.get(Constants.MTWILSON_API_PASSWORD);
        String keystore = Folders.configuration() + File.separator + user + ".jks";

        UserCollection users = null;
        try {
            Users client = new Users(mtwProperties);
            UserFilterCriteria criteria = new UserFilterCriteria();
            criteria.filter = true;
            criteria.nameEqualTo = user;
            users = client.searchUsers(criteria);
        } catch (Exception e) {
            log.error("Unable to check if the user already exists in MTW", e);
            if (e.getMessage().indexOf("java.net.ConnectException: Connection refused") != 1) {
                throw new AttestationHubException(e);
            }
        }
        File userJks = new File(keystore);

        if (users != null && users.getUsers().size() > 0 && userJks.exists()) {
            log.info("User: {} already created in MTW. Not creating again", user);
        } else {
            log.info("Creating user: {} in MTW", user);
            Properties properties = new Properties();
            File folder = new File(Folders.configuration());
            properties.setProperty("mtwilson.api.tls.policy.certificate.sha256",
                    AttestationHubConfigUtil.get(Constants.MTWILSON_API_TLS));
            String comment = null;
            try {
                comment = formatCommentRequestedRoles("Attestation", "Challenger");
            } catch (JsonProcessingException e) {
                log.error("Error creating user roles", e);
                throw new AttestationHubException(e);
            }

            try {
                MwClientUtil.createUserInDirectoryV2(folder, user, password, server, comment, properties);
            } catch (IOException | ApiException | CryptographyException | ClientException e) {
                log.error("Error creating user keystore", e);
                throw new AttestationHubException(e);
            }
        }
    }

    private static String formatCommentRequestedRoles(String... roles) throws JsonProcessingException {
        UserComment userComment = new UserComment();
        userComment.roles.addAll(Arrays.asList(roles));
        ObjectMapper yaml = createYamlMapper();
        return yaml.writeValueAsString(userComment);
    }

    private static class UserComment {
        public HashSet<String> roles = new HashSet<>();
    }

    private static ObjectMapper createYamlMapper() {
        YAMLFactory yamlFactory = new YAMLFactory();
        yamlFactory.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
        yamlFactory.configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, false);
        ObjectMapper mapper = new ObjectMapper(yamlFactory);
        mapper.setPropertyNamingStrategy(new PropertyNamingStrategy.LowerCaseWithUnderscoresStrategy());
        return mapper;
    }

    public static AttestationServiceClient getInstance() throws AttestationHubException {
        if (attestationServiceClient == null) {
            attestationServiceClient = new AttestationServiceClient();
        }
        return attestationServiceClient;
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
                if (e instanceof ConnectException) {
                    throw new AttestationHubException("Cannot connect to attestation service", e);
                }
                continue;
            }
            if (searchHostAttestations != null && searchHostAttestations.getHostAttestations() != null
                    && searchHostAttestations.getHostAttestations().size() > 0) {
                HostAttestation hostAttestation = searchHostAttestations.getHostAttestations().get(0);
                populateMwHost(host, hostAttestation, hostIdToMwHostMap);
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
            if (e instanceof ConnectException) {
                throw new AttestationHubException("Cannot connect to attestation service", e);
            }
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
        criteria.fromDate = lastDateTimeFromLastRunFile;
        HostAttestationCollection searchHostAttestations = null;

        try {
            searchHostAttestations = hostAttestationsService.searchHostAttestations(criteria);
        } catch (Exception e) {
            log.error("Unable to get host attestations for from date : {}", lastDateTimeFromLastRunFile, e);
            if (e instanceof ConnectException) {
                throw new AttestationHubException("Cannot connect to attestation service", e);
            }
            return null;
        }

        if (searchHostAttestations != null && searchHostAttestations.getHostAttestations() != null
                && searchHostAttestations.getHostAttestations().size() > 0) {
            List<HostAttestation> hostAttestations = searchHostAttestations.getHostAttestations();
            for (HostAttestation hostAttestation : hostAttestations) {
                String hostUuid = hostAttestation.getHostUuid();
                Host citHost = hostsService.retrieveHost(hostUuid);
                populateMwHost(citHost, hostAttestation, hostIdToMwHostMap);
            }
        }

        log.info("Returning the hosts and host attestations returned from MTW : {}", hostIdToMwHostMap.size());
        return hostIdToMwHostMap;
    }

    public void updateHostsForSamlTimeout() throws AttestationHubException {
        log.info("updating deleted status of hosts depending on the expiry of saml");
        PersistenceServiceFactory persistenceServiceFactory = PersistenceServiceFactory.getInstance();
        AhHostJpaController ahHostJpaController = persistenceServiceFactory.getHostController();
        HostAttestations hostAttestationsService = MtwClientFactory
                .getHostAttestationsClient(mtwPropertiesForverification);

        List<AhHost> ahHostEntities = ahHostJpaController.findAhHostEntities();
        log.info("Fetched {} hosts from attests hub db", ahHostEntities.size());

        for (AhHost ahHost : ahHostEntities) {
            log.info("Processing saml verification for host: {}", ahHost.getId());
            String samlReport = ahHost.getSamlReport();

            TrustAssertion verifyTrustAssertion = convertSamlToTrustAssertion(hostAttestationsService, samlReport);
            if (verifyTrustAssertion == null) {
                log.info("No verification report for host: {}", ahHost.getId());
                continue;
            }

            DateTime currentDateTime = new DateTime(DateTimeZone.UTC);
            Date notAfter = verifyTrustAssertion.getNotAfter();
            DateTime notOnOrAfter = new DateTime(notAfter.getTime(), DateTimeZone.UTC);
            log.info("Current Date : {} and saml notOnOrAfter : {}", currentDateTime, notOnOrAfter);
            log.info("notOnOrAfter.isBeforeNow() = {} and  notOnOrAfter.isEqualNow() = {}", notOnOrAfter.isBeforeNow(),
                    notOnOrAfter.isEqualNow());
            if (notOnOrAfter.isBeforeNow() || notOnOrAfter.isEqualNow()) {
                Date issueDate = verifyTrustAssertion.getDate();
                DateTime issueDateUTC = new DateTime(issueDate.getTime(), DateTimeZone.UTC);

                log.info("Marking host : {} as deleted as the saml issue date is {} and expiring now which is {}",
                        ahHost.getId(), issueDateUTC, currentDateTime);

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

    private TrustAssertion convertSamlToTrustAssertion(HostAttestations hostAttestationsService, String saml)
            throws AttestationHubException {
        TrustAssertion verifyTrustAssertion = null;
        try {
            verifyTrustAssertion = hostAttestationsService.verifyTrustAssertion(saml);
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
        return verifyTrustAssertion;

    }

    private String convertDateToUTCString(Date date) {
        DateTime dt = new DateTime(date.getTime(), DateTimeZone.UTC);
        DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
        return fmt.print(dt);
    }

    private void populateMwHost(Host host, HostAttestation hostAttestation, Map<String, MWHost> hostIdToMwHostMap)
            throws AttestationHubException {
        HostAttestations hostAttestationVerificationService = MtwClientFactory
                .getHostAttestationsClient(mtwPropertiesForverification);
        TrustAssertion assertion = convertSamlToTrustAssertion(hostAttestationVerificationService,
                hostAttestation.getSaml());
        if (assertion == null) {
            log.error("Unable to verify trust assertion for host : {}", host.getId());
            return;
        }
        MWHost mwHost = new MWHost();
        mwHost.setHost(host);
        mwHost.setMwHostAttestation(hostAttestation);
        String str = convertDateToUTCString(assertion.getNotAfter());
        mwHost.setSamlValidTo(str);
        mwHost.setTrustAssertion(assertion);
        TrustReport trustReport = hostAttestation.getTrustReport();
        mwHost.setTrusted(trustReport.isTrusted());
        hostIdToMwHostMap.put(host.getId().toString(), mwHost);
        log.info("Received attestation with ID: {} for host ID : {} and name : {}", hostAttestation.getId(),
                host.getId(), host.getName());
    }

    private void populateAttestationServiceProperties() throws AttestationHubException {
        URL server = null;
        try {
            server = new URL(AttestationHubConfigUtil.get(Constants.MTWILSON_API_URL));
        } catch (MalformedURLException e) {
            log.error("Error forming Attestation Service URL", e);
            throw new AttestationHubException(e);
        }
        String user = AttestationHubConfigUtil.get(Constants.MTWILSON_API_USER);
        String password = AttestationHubConfigUtil.get(Constants.MTWILSON_API_PASSWORD);
        String keystore = Folders.configuration() + File.separator + user + ".jks";

        mtwProperties.setProperty(Constants.MTWILSON_API_PASSWORD, password);
        mtwProperties.setProperty(Constants.MTWILSON_API_TLS, AttestationHubConfigUtil.get(Constants.MTWILSON_API_TLS));
        mtwProperties.setProperty(Constants.MTWILSON_API_URL, AttestationHubConfigUtil.get(Constants.MTWILSON_API_URL));
        mtwProperties.setProperty(Constants.MTWILSON_API_USER,
                AttestationHubConfigUtil.get(Constants.MTWILSON_API_USER));

        // Verification settings
        mtwPropertiesForverification = new Properties(mtwProperties);
        mtwPropertiesForverification.setProperty("mtwilson.api.keystore", keystore);
        mtwPropertiesForverification.setProperty("mtwilson.api.keystore.password", password);
        mtwPropertiesForverification.setProperty("mtwilson.api.key.alias", user);
        mtwPropertiesForverification.setProperty("mtwilson.api.key.password", password);

    }
}
