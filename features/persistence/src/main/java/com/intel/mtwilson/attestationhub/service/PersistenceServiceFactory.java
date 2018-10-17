package com.intel.mtwilson.attestationhub.service;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intel.dcsg.cpg.configuration.Configuration;
import com.intel.mtwilson.Folders;
import com.intel.mtwilson.attestationhub.common.Constants;
import com.intel.mtwilson.attestationhub.controller.AhHostJpaController;
import com.intel.mtwilson.attestationhub.controller.AhMappingJpaController;
import com.intel.mtwilson.attestationhub.controller.AhTenantJpaController;
import com.intel.mtwilson.configuration.ConfigurationFactory;
import com.intel.mtwilson.configuration.ConfigurationProvider;

public class PersistenceServiceFactory {
    private static final Logger log = LoggerFactory.getLogger(PersistenceServiceFactory.class);

    private EntityManagerFactory entityManagerFactory;
    private static Properties jpaProperties;

    static {
	File hubPropertiesFile = new File(
		Folders.configuration() + File.separator + Constants.ATTESTATION_HUB_PROPRRTIES_FILE_NAME);
	ConfigurationProvider provider;
	jpaProperties = new Properties();
	try {
	    provider = ConfigurationFactory.createConfigurationProvider(hubPropertiesFile);
	    Configuration loadedConfiguration = provider.load();
	    jpaProperties.put("javax.persistence.jdbc.driver",
		    loadedConfiguration.get(Constants.ATTESTATION_HUB_DB_DRIVER));
	    jpaProperties.put("javax.persistence.jdbc.url", loadedConfiguration.get(Constants.ATTESTATION_HUB_DB_URL));
	    jpaProperties.put("javax.persistence.jdbc.user",
		    loadedConfiguration.get(Constants.ATTESTATION_HUB_DB_USERNAME));
	    jpaProperties.put("javax.persistence.jdbc.password",
		    loadedConfiguration.get(Constants.ATTESTATION_HUB_DB_PASSWORD));

	} catch (IOException e1) {
	    log.error("Failed to fetch database properties from {}", Constants.ATTESTATION_HUB_PROPRRTIES_FILE_NAME,
		    e1);
	}

    }

    public static PersistenceServiceFactory getInstance() {
	return new PersistenceServiceFactory();
    }

    public AhTenantJpaController getTenantController() {
	log.debug("initializing the tenant controller");
	entityManagerFactory = Persistence.createEntityManagerFactory(Constants.ATTESTATION_HUB_DATABASE_NAME,
		jpaProperties);
	return new AhTenantJpaController(entityManagerFactory);
    }

    public AhHostJpaController getHostController() {
	log.debug("initializing the host controller");
	entityManagerFactory = Persistence.createEntityManagerFactory(Constants.ATTESTATION_HUB_DATABASE_NAME,
		jpaProperties);
	return new AhHostJpaController(entityManagerFactory);
    }

    public AhMappingJpaController getTenantToHostMappingController() {
	log.debug("initializing the mapping controller");
	entityManagerFactory = Persistence.createEntityManagerFactory(Constants.ATTESTATION_HUB_DATABASE_NAME,
		jpaProperties);
	return new AhMappingJpaController(entityManagerFactory);
    }
}
