package com.intel.attestationhub.service.impl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.ini4j.InvalidFileFormatException;
import org.ini4j.Profile.Section;
import org.ini4j.Wini;

import com.intel.attestationhub.api.HostFilterCriteria;
import com.intel.attestationhub.api.MWHost;
import com.intel.attestationhub.api.MappingResultResponse;
import com.intel.attestationhub.api.SearchCriteriaForMapping;
import com.intel.attestationhub.api.Tenant;
import com.intel.attestationhub.api.Tenant.Plugin;
import com.intel.attestationhub.api.Tenant.Property;
import com.intel.attestationhub.api.TenantFilterCriteria;
import com.intel.attestationhub.mapper.HostMapper;
import com.intel.attestationhub.mapper.TenantMapper;
import com.intel.attestationhub.service.AttestationHubService;
import com.intel.mtwilson.as.rest.v2.model.Host;
import com.intel.mtwilson.attestationhub.common.AttestationHubConfigUtil;
import com.intel.mtwilson.attestationhub.common.Constants;
import com.intel.mtwilson.attestationhub.controller.AhHostJpaController;
import com.intel.mtwilson.attestationhub.controller.AhMappingJpaController;
import com.intel.mtwilson.attestationhub.controller.AhTenantJpaController;
import com.intel.mtwilson.attestationhub.controller.exceptions.NonexistentEntityException;
import com.intel.mtwilson.attestationhub.controller.exceptions.PreexistingEntityException;
import com.intel.mtwilson.attestationhub.data.AhHost;
import com.intel.mtwilson.attestationhub.data.AhMapping;
import com.intel.mtwilson.attestationhub.data.AhTenant;
import com.intel.mtwilson.attestationhub.exception.AttestationHubException;
import com.intel.mtwilson.attestationhub.service.PersistenceServiceFactory;

public class AttestationHubServiceImpl implements AttestationHubService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AttestationHubServiceImpl.class);

    public static AttestationHubService getInstance() {
	return new AttestationHubServiceImpl();
    }

    @Override
    public void writeTenantConfig(Tenant tenant) throws AttestationHubException {
	String tenantConfigDirPath = AttestationHubConfigUtil.get(Constants.ATTESTATION_HUB_TENANT_CONFIGURATIONS_PATH);
	String tenantConfigFileName = tenantConfigDirPath + File.separator + tenant.getId() + ".ini";
	File iniFile = new File(tenantConfigFileName);

	// this method is called in case of edit and add. SO if the file
	// is not existing, create one
	if (!iniFile.exists()) {
	    try {
		iniFile.createNewFile();
	    } catch (IOException e) {
		log.error("Unable to create ini file for tenant", e);
		throw new AttestationHubException("Umable to create a config file for tenant", e);
	    }
	}

	// Init
	Wini wini = initWini(tenant.getId());

	// Clear the earlier settings since we want to over write the contents
	// every time
	wini.clear();
	// Create default section
	wini.put("default", "name", tenant.getName());

	// Add the plugins
	for (Plugin plugin : tenant.getPlugins()) {
	    String pluginName = plugin.getName().toLowerCase();
	    log.info("Creating {} plugin ", pluginName);
	    List<Property> properties = plugin.getProperties();
	    for (Property property : properties) {
		wini.put(pluginName, property.getKey(), property.getValue());
	    }
	}

	try {
	    wini.store();
	} catch (IOException e) {
	    log.error("writeTenantConfig: Error writing the ini file for tenant using WINI ", e);
	    throw new AttestationHubException(e);
	}

    }

    @Override
    public Tenant readTenantConfig(String tenantId) throws AttestationHubException {
	Wini wini = initWini(tenantId);
	Tenant tenant = new Tenant();
	boolean default_section = true;
	for (Section section : wini.values()) {
	    if (default_section) {
		for (String option : section.keySet()) {
		    log.info(option + " = " + section.fetch(option));
		    if (option.equals("name")) {
			tenant.setName(section.fetch(option));
		    }
		}
		default_section = false;
	    } else {
		Plugin plugin = tenant.addPlugin(section.getName());
		for (String option : section.keySet()) {
		    plugin.addProperty(option, section.fetch(option));
		}
	    }
	}
	return tenant;

    }

    private Wini initWini(String tenantId) throws AttestationHubException {
	String tenantConfigDirPath = AttestationHubConfigUtil.get(Constants.ATTESTATION_HUB_TENANT_CONFIGURATIONS_PATH);
	String tenantConfigFileName = tenantConfigDirPath + File.separator + tenantId + ".ini";

	Wini ini;
	try {
	    ini = new Wini(new File(tenantConfigFileName));
	} catch (InvalidFileFormatException e) {
	    log.error("writeTenantConfig: Error initializing the WINI ", e);
	    throw new AttestationHubException(e);

	} catch (IOException e) {
	    log.error("writeTenantConfig: Error reading the ini file for tenant to init the WINI ", e);
	    throw new AttestationHubException(e);
	}
	return ini;
    }

    @Override
    public String createTenant(Tenant tenant) throws AttestationHubException {
	String newTenantId = null;
	PersistenceServiceFactory persistenceServiceFactory = PersistenceServiceFactory.getInstance();
	AhTenantJpaController tenantController = persistenceServiceFactory.getTenantController();
	AhTenant ahTenant = TenantMapper.mapApiToJpa(tenant);
	try {
	    tenantController.create(ahTenant);
	    newTenantId = ahTenant.getId();
	    // Write a file to disk using the ID as the file name
	    tenant.setId(newTenantId);
	    writeTenantConfig(tenant);
	} catch (Exception e) {
	    log.error("Error saving the tenant", e);
	    throw new AttestationHubException(e);
	}
	return newTenantId;
    }

    @Override
    public Tenant retrieveTenant(String tenantId) throws AttestationHubException {
	Tenant newTenant = new Tenant();
	PersistenceServiceFactory persistenceServiceFactory = PersistenceServiceFactory.getInstance();
	AhTenantJpaController tenantController = persistenceServiceFactory.getTenantController();
	AhTenant ahTenant = tenantController.findAhTenant(tenantId);
	if (ahTenant == null) {
	    NonexistentEntityException nonexistentEntityException = new NonexistentEntityException(
		    "Tenant with id: " + tenantId + " does not exist");
	    throw new AttestationHubException(nonexistentEntityException);
	}
	newTenant = TenantMapper.mapJpatoApi(ahTenant);
	return newTenant;
    }

    @Override
    public List<Tenant> retrieveAllTenants() throws AttestationHubException {
	List<Tenant> tenants = new ArrayList<Tenant>();
	PersistenceServiceFactory persistenceServiceFactory = PersistenceServiceFactory.getInstance();
	AhTenantJpaController tenantController = persistenceServiceFactory.getTenantController();
	List<AhTenant> ahTenants = tenantController.findAhTenantEntities();
	if (ahTenants == null) {
	    NonexistentEntityException nonexistentEntityException = new NonexistentEntityException(
		    "Tenants do not exist");
	    throw new AttestationHubException(nonexistentEntityException);
	}
	for (AhTenant ahTenant : ahTenants) {
	    tenants.add(TenantMapper.mapJpatoApi(ahTenant));
	}
	return tenants;
    }

    @Override
    public Tenant updateTenant(Tenant tenant) throws AttestationHubException {
	PersistenceServiceFactory persistenceServiceFactory = PersistenceServiceFactory.getInstance();
	AhTenantJpaController tenantController = persistenceServiceFactory.getTenantController();
	AhTenant ahTenant = TenantMapper.mapApiToJpa(tenant);
	try {
	    tenantController.edit(ahTenant);
	} catch (NonexistentEntityException e) {
	    String msg = "Invalid id: " + tenant.getId();
	    log.error(msg, e);
	    throw new AttestationHubException(msg, e);
	} catch (Exception e) {
	    String msg = "Error updating tenant : " + tenant.getId();
	    log.error(msg, e);
	    throw new AttestationHubException(msg, e);
	}
	writeTenantConfig(tenant);
	return tenant;
    }

    @Override
    public void deleteTenant(String tenantId) throws AttestationHubException {
	PersistenceServiceFactory persistenceServiceFactory = PersistenceServiceFactory.getInstance();
	AhTenantJpaController tenantController = persistenceServiceFactory.getTenantController();
	AhTenant ahTenant = tenantController.findAhTenant(tenantId);
	ahTenant.setDeleted(true);
	try {
	    tenantController.edit(ahTenant);
	    Collection<AhMapping> ahMappingCollection = ahTenant.getAhMappingCollection();
	    // delete corresponding mappings
	    for (AhMapping ahMapping : ahMappingCollection) {
		deleteMapping(ahMapping.getId());
	    }
	} catch (NonexistentEntityException e) {
	    String msg = "Invalid id: " + tenantId;
	    log.error(msg, e);
	    throw new AttestationHubException(msg, e);
	} catch (Exception e) {
	    String msg = "Error deleting tenant : " + tenantId;
	    log.error(msg, e);
	    throw new AttestationHubException(msg, e);
	}
	String tenantConfigDirPath = AttestationHubConfigUtil.get(Constants.ATTESTATION_HUB_TENANT_CONFIGURATIONS_PATH);
	String tenantConfigFileName = tenantConfigDirPath + File.separator + tenantId + ".ini";
	boolean success = (new File(tenantConfigFileName)).delete();
	if (success) {
	    log.info("The file has been sucessfully deleted");
	}
    }

    @Override
    public AhMapping retrieveMapping(String mappingId) throws AttestationHubException {
	PersistenceServiceFactory persistenceServiceFactory = PersistenceServiceFactory.getInstance();
	AhMappingJpaController mappingController = persistenceServiceFactory.getTenantToHostMappingController();
	AhMapping ahMapping = mappingController.findAhMapping(mappingId);
	if (ahMapping == null) {
	    NonexistentEntityException nonexistentEntityException = new NonexistentEntityException(
		    "Tenant-Host mapping with id: " + mappingId + " does not exist");
	    throw new AttestationHubException(nonexistentEntityException);
	}
	return ahMapping;
    }

    @Override
    public List<AhMapping> retrieveAllMappings() throws AttestationHubException {
	PersistenceServiceFactory persistenceServiceFactory = PersistenceServiceFactory.getInstance();
	AhMappingJpaController mappingController = persistenceServiceFactory.getTenantToHostMappingController();
	List<AhMapping> ahMappings = mappingController.findAhMappingEntities();
	if (ahMappings == null) {
	    NonexistentEntityException nonexistentEntityException = new NonexistentEntityException(
		    "Tenant-Host mappings do not exist");
	    throw new AttestationHubException(nonexistentEntityException);
	}
	return ahMappings;
    }

    @Override
    public MappingResultResponse createOrUpdateMapping(String tenantId, List<String> hostsIds, String mode)
	    throws AttestationHubException {
	MappingResultResponse mappingResultResponse = new MappingResultResponse();
	PersistenceServiceFactory hostPersistenceServiceFactory = PersistenceServiceFactory.getInstance();
	AhHostJpaController hostController = hostPersistenceServiceFactory.getHostController();

	PersistenceServiceFactory tenantPersistenceServiceFactory = PersistenceServiceFactory.getInstance();
	AhTenantJpaController tenantController = tenantPersistenceServiceFactory.getTenantController();

	PersistenceServiceFactory persistenceServiceFactory = PersistenceServiceFactory.getInstance();
	AhMappingJpaController mappingController = persistenceServiceFactory.getTenantToHostMappingController();
	AhTenant ahTenant = tenantController.findAhTenant(tenantId);
	// Throw an exception if the tennat id is non existent in hub db
	if (ahTenant == null) {
	    throw new AttestationHubException("Tenant does not exist with id : " + tenantId);
	}
	Collection<AhMapping> ahMappingCollection = ahTenant.getAhMappingCollection();
	// update the existing mappings whose host id exists in the one provided
	// by the user
	List<AhMapping> mappingToBeDeleted = new ArrayList<>();
	for (AhMapping ahMapping : ahMappingCollection) {
	    // If the hosts provided by the user were previously mapped
	    // but now are not provided by the user, we need to mark that
	    // mapping as
	    // deleted
	    if (!hostsIds.contains(ahMapping.getHost().getId())) {
		mappingToBeDeleted.add(ahMapping);
		continue;
	    }

	    // If the host - tennant was previoulsy done and the host id is sent
	    // again
	    // we want to make sure that the deleted flag is set to false
	    hostsIds.remove(ahMapping.getHost().getId());
	    ahMapping.setDeleted(false);
	    try {
		mappingController.edit(ahMapping);
	    } catch (NonexistentEntityException e) {
		String msg = "Invalid mapping id : " + ahMapping.getId();
		log.error(msg, e);
		throw new AttestationHubException(msg, e);
	    } catch (Exception e) {
		String msg = "Error updating tenant-host mapping";
		log.error(msg, e);
		throw new AttestationHubException(msg, e);
	    }
	}

	// delete the remaining mappings in case of PUT
	if (Constants.UPDATE.equals(mode)) {
	    for (AhMapping ahMapping : mappingToBeDeleted) {
		ahMapping.setDeleted(true);
		try {
		    mappingController.edit(ahMapping);
		} catch (NonexistentEntityException e) {
		    log.error("No Mapping exists", e);
		} catch (Exception e) {
		    log.error("Error updating mapping", e);
		}
	    }
	}

	// Create hosts mappings which do not exist for the tenant
	List<String> uniqueHostIds = new ArrayList<>();
	for (String hostId : hostsIds) {
	    if (uniqueHostIds.contains(hostId)) {
		continue;
	    } else {
		uniqueHostIds.add(hostId);
	    }
	    AhHost ahHost = hostController.findAhHost(hostId);
	    if (ahHost == null) {
		mappingResultResponse.invalidHostIds.add(hostId);
		continue;
	    }
	    AhMapping ahMapping = new AhMapping();
	    ahMapping.setHost(ahHost);
	    ahMapping.setTenant(ahTenant);
	    ahMapping.setDeleted(false);
	    try {
		mappingController.create(ahMapping);
		MappingResultResponse.TenantToHostMapping mapping = new MappingResultResponse.TenantToHostMapping();
		mapping.hostId = hostId;
		mapping.mappingId = ahMapping.getId();
		mapping.tenantId = ahTenant.getId();
		mappingResultResponse.mappings.add(mapping);
	    } catch (PreexistingEntityException e) {
		String msg = "Mapping id " + ahMapping.getId()
			+ " already exists. Cannot create a new one with the same id";
		log.error(msg, e);
		throw new AttestationHubException(msg, e);
	    } catch (Exception e) {
		String msg = "Error creating tenant-host mapping for tenant: " + tenantId + " and host: " + hostId;
		log.error(msg, e);
		throw new AttestationHubException(msg, e);
	    }
	}
	return mappingResultResponse;
    }

    @Override
    public void deleteMapping(String mappingId) throws AttestationHubException {
	PersistenceServiceFactory persistenceServiceFactory = PersistenceServiceFactory.getInstance();
	AhMappingJpaController mappingController = persistenceServiceFactory.getTenantToHostMappingController();
	AhMapping ahMapping = mappingController.findAhMapping(mappingId);
	if (ahMapping == null) {
	    String msg = "Tenant-Host mapping with id: " + mappingId + " does not exist";
	    log.error(msg);
	    NonexistentEntityException nonexistentEntityException = new NonexistentEntityException(msg);
	    throw new AttestationHubException(nonexistentEntityException);
	}
	ahMapping.setDeleted(true);
	try {
	    mappingController.edit(ahMapping);
	} catch (NonexistentEntityException e) {
	    String msg = "Tenant-Host mapping with id: " + mappingId + " does not exist";
	    throw new AttestationHubException(msg, e);
	} catch (Exception e) {
	    log.error("Unable to update the delete flag for mapping : {}", mappingId);
	    throw new AttestationHubException(e);
	}
    }

    /**
     * this method is called from the poller. This will create / edit hosts in
     * the attestation hub db based on the response from the attestation
     * service.
     * 
     * @param hostAttestationsMap
     * @throws AttestationHubException
     */
    @Override
    public void saveHosts(Map<String, MWHost> hostAttestationsMap) throws AttestationHubException {
	log.info("Saving hosts returned by Attestation Service");
	if (hostAttestationsMap == null || hostAttestationsMap.size() == 0) {
	    log.info("No hosts retrieved from attestation service to be inserted in hub db");
	    return;
	}

	AhHostJpaController hostController = PersistenceServiceFactory.getInstance().getHostController();
	List<String> savedAhHostIdList = new ArrayList<String>(hostAttestationsMap.size());
	log.info("Fetch all hosts from the attestation hub DB");

	// the map passed to method has the key which is the ID of the
	// host in the attestation db. the value of the map is a custom hub
	// wrapper object
	// which contains the Host and Host Attestation object for the host from
	// Attestation service
	for (String id : hostAttestationsMap.keySet()) {
	    boolean ahHostExists = false;
	    MWHost mwHost = hostAttestationsMap.get(id);
	    Host host = mwHost.getHost();
	    log.info("Processing save for host ID : {} and name: {}", host.getId(), host.getName());
	    AhHost ahHost = hostController.findAhHost(host.getId().toString());
	    log.info("Does the host already exist in Attestation Hub DB ? {}", ahHost != null);
	    if (ahHost != null) {
		ahHostExists = true;
	    }
	    ahHost = HostMapper.mapHostToAhHost(mwHost, ahHost, "admin");
	    ahHost.setDeleted(false);
	    try {
		if (ahHostExists) {
		    hostController.edit(ahHost);
		    log.info("Edited the host: {} in attestation DB ", ahHost.getId());
		    savedAhHostIdList.add(ahHost.getId());
		} else {
		    hostController.create(ahHost);
		    log.info("Added host to attestation DB with ID: {}", ahHost.getId());
		}
	    } catch (PreexistingEntityException e) {
		log.error("Error creating host {} since host already exists", ahHost.getId(), e);
		throw new AttestationHubException(e);
	    } catch (Exception e) {
		log.error("Error creating host", e);
		throw new AttestationHubException(e);
	    }
	}
    }

    @Override
    public List<Tenant> searchTenantsBySearchCriteria(TenantFilterCriteria tenantFilterCriteria)
	    throws AttestationHubException {
	AhTenantJpaController tenantController = PersistenceServiceFactory.getInstance().getTenantController();
	List<Tenant> tenantsList = new ArrayList<Tenant>();
	List<AhTenant> ahTenantsList = tenantController
		.findAhTenantsByNameSearchCriteria(tenantFilterCriteria.nameEqualTo);
	if (ahTenantsList != null) {
	    log.info("Found {} tenants with name : {}", ahTenantsList.size(), tenantFilterCriteria.nameEqualTo);
	    for (AhTenant ahTenant : ahTenantsList) {
		tenantsList.add(TenantMapper.mapJpatoApi(ahTenant));
	    }
	}
	return tenantsList;
    }

    @Override
    public List<AhMapping> searchMappingsBySearchCriteria(SearchCriteriaForMapping criteriaForMapping)
	    throws AttestationHubException {
	PersistenceServiceFactory persistenceServiceFactory = PersistenceServiceFactory.getInstance();
	AhMappingJpaController mappingController = persistenceServiceFactory.getTenantToHostMappingController();
	List<AhMapping> ahMappings = new ArrayList<>();
	if (StringUtils.isNotBlank(criteriaForMapping.tenantId)) {
	    ahMappings = mappingController.findAhMappingsByTenantId(criteriaForMapping.tenantId);
	} else if (StringUtils.isNotBlank(criteriaForMapping.hostId)) {
	    ahMappings = mappingController.findAhMappingsByHostId(criteriaForMapping.hostId);
	} else {
	    ahMappings = null;
	}
	return ahMappings;
    }

    @Override
    public AhHost getHostById(String id) throws AttestationHubException {
	log.info("Finding host with given ID {}", id);
	PersistenceServiceFactory persistenceServiceFactory = PersistenceServiceFactory.getInstance();
	AhHostJpaController ahHostJpaController = persistenceServiceFactory.getHostController();
	AhHost ahHost = ahHostJpaController.findAhHost(id);
	if (ahHost == null) {
	    NonexistentEntityException nonexistentEntityException = new NonexistentEntityException(
		    "Host with id: " + id + " does not exist");
	    throw new AttestationHubException(nonexistentEntityException);
	}
	return ahHost;
    }

    @Override
    public List<AhHost> getHosts() throws AttestationHubException {
	log.info("Getting all availabe hosts on attestation hub");
	PersistenceServiceFactory persistenceServiceFactory = PersistenceServiceFactory.getInstance();
	AhHostJpaController ahHostJpaController = persistenceServiceFactory.getHostController();
	List<AhHost> ahHosts = ahHostJpaController.findAhHostEntities();
	return ahHosts;
    }

    @Override
    public List<AhHost> searchHostsWithSearchCriteria(HostFilterCriteria hostFilterCriteria)
	    throws AttestationHubException {
	PersistenceServiceFactory persistenceServiceFactory = PersistenceServiceFactory.getInstance();
	AhHostJpaController ahHostJpaController = persistenceServiceFactory.getHostController();
	List<AhHost> ahHosts = ahHostJpaController.findHostsWithFilterCriteria(hostFilterCriteria.nameEqualTo);
	if (ahHosts != null) {
	    log.info("Found {} host with given filter criteria {}", ahHosts.size(), hostFilterCriteria.nameEqualTo);
	} else {
	    log.info("No hosts found with given filter criteria {}", hostFilterCriteria.nameEqualTo);
	}
	return ahHosts;
    }

}
