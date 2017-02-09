package com.intel.attestationhub.quartz;

import com.intel.attestationhub.manager.PluginManager;
import com.intel.dcsg.cpg.console.AbstractCommand;
import com.intel.mtwilson.attestationhub.common.AttestationHubConfigUtil;
import com.intel.mtwilson.attestationhub.common.Constants;
import com.intel.mtwilson.attestationhub.exception.AttestationHubException;
import org.apache.commons.lang3.StringUtils;

/**
 * Entry class for kicking off the poller. This class is started from
 * attestation-hub start command
 *
 * @author Siddharth
 */
public class AttestationHubScheduler extends AbstractCommand {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AttestationHubScheduler.class);
    private int pollInterval = 0;
    private final int defaultPollInterval = 2;

    private void init() {
        int millisInMinute = 60 * 1000;
        String pollIntervalStr = AttestationHubConfigUtil.get(Constants.ATTESTATION_HUB_POLL_INTERVAL);
        if (StringUtils.isBlank(pollIntervalStr)) {
            log.info("Defaulting poll interval to 10 mins");
            pollIntervalStr = String.valueOf(defaultPollInterval);
        }
        try {
            pollInterval = Integer.parseInt(pollIntervalStr);
        } catch (NumberFormatException numberFormatException) {
            pollInterval = defaultPollInterval;
            log.error("Invalid poll interval configured: {}. Defaulting to {} mins", pollIntervalStr, defaultPollInterval);
        }
        log.info("Poll interval is {} mins", pollInterval);

        pollInterval = millisInMinute * pollInterval;
    }

    @Override
    public void execute(String[] args)  {
        log.info("Scheduling attestation service poller");
        init();
        PluginManager pluginManager = PluginManager.getInstance();
        while (true) {
            AttestationServicePollerJob attestationServicePollerJob ;
            try {
                attestationServicePollerJob = new AttestationServicePollerJob();
            } catch (AttestationHubException e) {
                log.error("Eror while initializing attestation poller. Going to try again as part of regular poll", pollInterval, e);
                sleep(pollInterval);
                continue;
            }
            log.info("Executing scheduled process of pulling data from attestation service and pushing to tenants");
            attestationServicePollerJob.execute();
            pluginManager.synchAttestationInfo();
            sleep(pollInterval);
        }
    }

    private void sleep(int sleepInterval){
        try {
            log.info("Waiting for {} millis", sleepInterval);
            Thread.sleep(sleepInterval);
        } catch (InterruptedException e) {
            log.error("Error in thread running the scheduler tasks", e);
        }
    }

}
