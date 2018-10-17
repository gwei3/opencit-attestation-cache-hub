package com.intel.attestationhub.service;

import java.util.List;
import java.util.Map;

import com.intel.attestationhub.api.HostFilterCriteria;
import com.intel.attestationhub.api.MWHost;
import com.intel.attestationhub.api.MappingResultResponse;
import com.intel.attestationhub.api.SearchCriteriaForMapping;
import com.intel.attestationhub.api.Tenant;
import com.intel.attestationhub.api.TenantFilterCriteria;
import com.intel.mtwilson.attestationhub.data.AhHost;
import com.intel.mtwilson.attestationhub.data.AhMapping;
import com.intel.mtwilson.attestationhub.exception.AttestationHubException;

public interface AttestationHubService {
    public String createTenant(Tenant tenant) throws AttestationHubException;

    public Tenant retrieveTenant(String tenantId)
	    throws AttestationHubException;

    public List<Tenant> retrieveAllTenants() throws AttestationHubException;

    public Tenant updateTenant(Tenant tenant) throws AttestationHubException;

    public void deleteTenant(String tenantId) throws AttestationHubException;

    public AhMapping retrieveMapping(String mappingId)
	    throws AttestationHubException;

    public List<AhMapping> retrieveAllMappings() throws AttestationHubException;

    public MappingResultResponse createOrUpdateMapping(String tenantId, List<String> hostsId)
	    throws AttestationHubException;

    public void deleteMapping(String mappingId) throws AttestationHubException;

    public void writeTenantConfig(Tenant tenant) throws AttestationHubException;

    public Tenant readTenantConfig(String tenantId)
	    throws AttestationHubException;

    public void saveHosts(Map<String, MWHost> hostAttestationsMap)
	    throws AttestationHubException;

    public List<Tenant> searchTenantsBySearchCriteria(
	    TenantFilterCriteria tenantFilterCriteria)
	    throws AttestationHubException;

    public List<AhMapping> searchMappingsBySearchCriteria(
	    SearchCriteriaForMapping criteriaForMapping)
	    throws AttestationHubException;

    public AhHost getHostById(String id) throws AttestationHubException;

    public List<AhHost> getHosts() throws AttestationHubException;

    public List<AhHost> searchHostsWithSearchCriteria(
	    HostFilterCriteria hostFilterCriteria)
	    throws AttestationHubException;
    
    public List<AhHost> findHostsByHardwareUuid(String hardwareUuid) throws AttestationHubException;
    public AhHost findActiveHostByHardwareUuid(String hardwareUuid) throws AttestationHubException;
    public void markAllHostsAsDeleted() throws AttestationHubException;

}
