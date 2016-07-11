package com.intel.attestationhub.quartz;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import com.intel.attestationhub.api.MWHost;
import com.intel.attestationhub.mtwclient.AttestationServiceClient;
import com.intel.attestationhub.service.AttestationHubService;
import com.intel.attestationhub.service.impl.AttestationHubServiceImpl;
import com.intel.mtwilson.Folders;
import com.intel.mtwilson.as.rest.v2.model.Host;
import com.intel.mtwilson.attestationhub.exception.AttestationHubException;

public class AttestationServicePollerJob {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AttestationServicePollerJob.class);
    private AttestationServiceClient attestationServiceClient = null;
    private File lastRunDateTimeFile;

    public AttestationServicePollerJob() {
	attestationServiceClient = AttestationServiceClient.getInstance();
    }

    public void execute() {
	log.info("AttestationServicePollerJob.execute - Poller run started at {}", new Date());

	/*
	 * Fetch all the hosts from MTW
	 */
	String lastRunDateTimeFileName = Folders.configuration() + File.separator + "HubSchedulerRun.txt";
	lastRunDateTimeFile = new File(lastRunDateTimeFileName);
	boolean isFirstRun = false;
	if (!lastRunDateTimeFile.exists()) {
	    isFirstRun = true;
	}
	DateTime dt = new DateTime(DateTimeZone.UTC);
	DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
	String str = fmt.print(dt);
	Map<String, MWHost> hostAttestationsMap = null;
	if (isFirstRun) {
	    log.info("Its the first run for attestation hub. Init data");
	    hostAttestationsMap = initData();
	    try {
		lastRunDateTimeFile.createNewFile();
	    } catch (IOException e) {
		log.error("Error creating lastRunDateTimeFile", e);
	    }
	    log.info("Init data complete");
	} else {
	    log.info("init data was done earlier. Update data");
	    hostAttestationsMap = updateData();
	    log.info("Update of data after pulling host attestations from MTW complete");

	}

	if(hostAttestationsMap == null){
	    log.error("Attestation data not received from MTW. Some error receiving host attestations data to be pushed in Attestation Hub DB");
	    return;
	}
	/*
	 * Add the hosts in the DB
	 */
	AttestationHubService attestationHubService = AttestationHubServiceImpl.getInstance();
	try {
	    attestationHubService.saveHosts(hostAttestationsMap);
	} catch (AttestationHubException e) {
	    log.error("Poller.execute: Error saving hosts from MTW", e);
	    logPollerRunComplete();
	    return;
	}
	
	//Delete hosts whose SAML has exceeded the timeout
	try {
	    attestationServiceClient.updateHostsForSamlTimeout();
	} catch (AttestationHubException e) {
	    log.error("Poller.execute: Error updating deleted status of hosts in Attestation Hub DB for SAML timeout", e);
	    logPollerRunComplete();
	    return;
	}
	writeCurrentTimeToLastRunFile(str);

	logPollerRunComplete();
    }

    private Map<String, MWHost> updateData() {
	Map<String, MWHost> hostAttestationsMap = null;
	String lastDateTimeFromLastRunFile = readDateTimeFromLastRunFile();
	if (StringUtils.isBlank(lastDateTimeFromLastRunFile)) {
	    log.info("the last date time is not read. Doing an init in update");
	    hostAttestationsMap = initData();
	    return hostAttestationsMap;
	}
	// Process the attestations received in the time window

	try {
	    hostAttestationsMap = attestationServiceClient.fetchHostAttestations(lastDateTimeFromLastRunFile);
	} catch (AttestationHubException e) {
	    log.error("Poller.execute: Error fetching host attestations created since {} from MTW",
		    lastDateTimeFromLastRunFile, e);
	    logPollerRunComplete();
	    return null;
	}

	return hostAttestationsMap;
    }

    private Map<String, MWHost> initData() {
	List<Host> allHosts;

	try {
	    allHosts = attestationServiceClient.fetchHosts();
	    if (allHosts == null) {
		log.info("AttestationServicePollerJob.execute - No hosts returned");
		logPollerRunComplete();
		return null;
	    } else {
		log.info("AttestationServicePollerJob.execute - Fetched {} hosts", allHosts.size());
	    }
	} catch (AttestationHubException e) {
	    log.error("AttestationServicePollerJob.execute - Error fetching hosts from MTW", e);
	    logPollerRunComplete();
	    return null;
	}

	/*
	 * Fetch the host attestations
	 */
	log.info("AttestationServicePollerJob.execute - Fetching attestations for the above hosts");
	Map<String, MWHost> hostAttestationsMap;
	try {
	    hostAttestationsMap = attestationServiceClient.fetchHostAttestations(allHosts);
	} catch (AttestationHubException e) {
	    log.error("Poller.execute: Error fetching SAMLS for hosts from MTW", e);
	    logPollerRunComplete();
	    return null;
	}

	return hostAttestationsMap;
    }

    private void writeCurrentTimeToLastRunFile(String str) {
	// 2016-02-27T00:00:00Z

	if (!lastRunDateTimeFile.exists()) {
	    return;
	}
	try {
	    FileOutputStream fileOutputStream = new FileOutputStream(lastRunDateTimeFile);
	    byte[] contentInBytes = str.getBytes();
	    fileOutputStream.write(contentInBytes);
	    fileOutputStream.flush();
	    fileOutputStream.close();
	} catch (FileNotFoundException e) {
	    log.error("Unable to locate last run file", e);
	} catch (IOException e) {
	    log.error("Unable to write to last run file", e);
	}
    }

    private String readDateTimeFromLastRunFile() {
	// 2016-02-27T00:00:00Z
	String lastDateTime = null;
	if (!lastRunDateTimeFile.exists()) {
	    return lastDateTime;
	}
	BufferedReader br = null;
	try {
	    String sCurrentLine;
	    br = new BufferedReader(new FileReader(lastRunDateTimeFile));
	    while ((sCurrentLine = br.readLine()) != null) {
		lastDateTime = sCurrentLine;
	    }
	} catch (IOException e) {
	    log.error("Error reading from last run date file", e);
	} finally {
	    try {
		if (br != null) {
		    br.close();
		}
	    } catch (IOException ex) {
		log.error("Error closing buffered reader of last run date file", ex);
	    }
	}

	return lastDateTime;
    }

    private void logPollerRunComplete() {
	log.info("Poller run completed at {}", new Date());
    }
}
