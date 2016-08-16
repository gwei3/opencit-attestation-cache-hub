package com.intel.mtwilson.attestationhub.setup;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.Key;
import java.security.KeyPair;

import org.apache.commons.lang.StringUtils;
import org.jose4j.keys.RsaKeyUtil;
import com.intel.mtwilson.util.exec.ExecUtil;
import com.intel.mtwilson.util.exec.Result;
import com.intel.mtwilson.Folders;
import com.intel.mtwilson.attestationhub.common.Constants;
import com.intel.mtwilson.setup.AbstractSetupTask;

public class TrustReportEncryptionKey extends AbstractSetupTask {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TrustReportEncryptionKey.class);

    private static final String PUBLIC_KEY_PATH = Folders.configuration() + File.separator + Constants.PUBLIC_KEY_FILE;
    private static final String PRIVATE_KEY_PATH = Folders.configuration() + File.separator
	    + Constants.PRIVATE_KEY_FILE;

    @Override
    protected void configure() throws Exception {
    }

    @Override
    protected void validate() throws Exception {
	File f = new File(PRIVATE_KEY_PATH);
	if (!f.exists()) {
	    validation("Private key is necessary for encrypting trust report");
	}
	f = new File(PUBLIC_KEY_PATH);
	if (!f.exists()) {
	    validation("Public key necessary for sharing with tenants");
	}
    }

    @Override
    protected void execute() throws Exception {
	/*
	 * RsaKeyUtil keyUtil = new RsaKeyUtil(); KeyPair keyPair =
	 * keyUtil.generateKeyPair(2048); File pubFile = new
	 * File(PUBLIC_KEY_PATH); File priFile = new File(PRIVATE_KEY_PATH);
	 * writeKeyToFile(keyPair.getPrivate(), priFile);
	 * writeKeyToFile(keyPair.getPublic(), pubFile);
	 */
	String command = "openssl genrsa 2048 > " + Folders.configuration() + File.separator + "TEMP"
		+ Constants.PRIVATE_KEY_FILE;
	Result result = ExecUtil.executeQuoted("/bin/bash", "-c", command);
	command = "openssl rsa -in "
		+ (Folders.configuration() + File.separator + "TEMP" + Constants.PRIVATE_KEY_FILE)
		+ " -outform PEM -pubout -out "
		+ (Folders.configuration() + File.separator + Constants.PUBLIC_KEY_FILE);
	result = ExecUtil.executeQuoted("/bin/bash", "-c", command);
	//Convert private key to PKCS8
	String tempFile = Folders.configuration() + File.separator + "TEMP"
		+ Constants.PRIVATE_KEY_FILE;
	String priKeyFile = Folders.configuration() + File.separator 
		+ Constants.PRIVATE_KEY_FILE;
	
	command = "openssl pkcs8 -topk8 -inform PEM -outform DER -in "+ tempFile +" -out "+ priKeyFile +"  -nocrypt > pkcs8_key";
	result = ExecUtil.executeQuoted("/bin/bash", "-c", command);

    }

    private void writeKeyToFile(Key key, File file) {
	byte[] keyBytes = key.getEncoded();
	FileOutputStream keyfos = null;
	try {
	    keyfos = new FileOutputStream(file);
	} catch (FileNotFoundException e) {
	    log.error("Unable to write key to disk", e);
	    return;
	}
	try {
	    keyfos.write(keyBytes);
	} catch (IOException e) {
	    log.error("Unable to write key to disk", e);
	}
	try {
	    if (keyfos != null) {
		keyfos.close();
	    }
	} catch (IOException e) {
	    log.error("Unable to write key to disk", e);
	    return;
	}

    }

}
