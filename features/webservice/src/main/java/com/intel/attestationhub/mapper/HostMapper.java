package com.intel.attestationhub.mapper;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.Attribute;
import org.opensaml.saml2.core.AttributeStatement;
import org.opensaml.xml.XMLObject;
import org.w3c.dom.Element;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intel.attestationhub.api.MWHost;
import com.intel.dcsg.cpg.io.UUID;
import com.intel.mtwilson.as.rest.v2.model.Host;
import com.intel.mtwilson.as.rest.v2.model.HostAttestation;
import com.intel.mtwilson.attestationhub.common.Constants;
import com.intel.mtwilson.attestationhub.data.AhHost;
import com.intel.mtwilson.datatypes.HostTrustResponse;
import com.intel.mtwilson.saml.TrustAssertion;
import com.intel.mtwilson.shiro.ShiroUtil;

public class HostMapper {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(HostMapper.class);

    public static AhHost mapHostToAhHost(MWHost host, AhHost ahHost, String user) {
	Host citHost = host.getHost();
	HostAttestation citHostAttestation = host.getMwHostAttestation();
	Date currentdate = new Date();
	String currentUser = user;
	if (StringUtils.isBlank(user)) {
	    currentUser = "admin";
	}

	if (ahHost == null) {
	    ahHost = new AhHost();
	    ahHost.setId(citHost.getId().toString());
	    ahHost.setCreatedBy(currentUser);
	}
	ahHost.setModifiedBy(currentUser);
	ahHost.setModifiedDate(currentdate);
	ahHost.setAikCertificate(citHost.getAikCertificate());
	ahHost.setAikSha1(citHost.getAikSha1());
	ahHost.setBiosMleUuid(citHost.getBiosMleUuid());
	ahHost.setConnectionUrl(citHost.getConnectionUrl());
	ahHost.setHardwareUuid(citHost.getHardwareUuid());
	ahHost.setHostName(citHost.getName());
	ahHost.setValidTo(host.getSamlValidTo());
	ahHost.setTrusted(host.getTrusted() == null ? false : host.getTrusted());
	if (host.getTrustAssertion() != null) {
	    TrustAssertion trustAssertion = host.getTrustAssertion();
	    Assertion assertion = trustAssertion.getAssertion();
	    List<AttributeStatement> attributeStatements = assertion.getAttributeStatements();
	    Set<String> assetTagList = new HashSet<String>();
	    
	    String tag = null;
	    String tagValue = null;
	    Map<String, List<String>> assetTagToValueMap = new HashMap();
	    List<String> tagValueList = null;
	    for (AttributeStatement attributeStatement : attributeStatements) {
		List<Attribute> attributes = attributeStatement.getAttributes();
		for (Attribute attribute : attributes) {
		    tagValue = null;
		    tag = null;
		    String name = attribute.getName();
		    if (name.startsWith(Constants.SAML_TAG)) {
			int indexOfSquareBracketOpen = name.indexOf("[");
			int indexOfSquareBracketClose = name.indexOf("]");
			if (indexOfSquareBracketOpen != -1 && indexOfSquareBracketClose != 1) {
			    tag = name.substring(indexOfSquareBracketOpen + 1, indexOfSquareBracketClose);
			} else {
			    continue;
			}
			if(StringUtils.isBlank(tag)){
			    continue;
			}
			if(assetTagToValueMap.containsKey(tag)){
			    tagValueList = assetTagToValueMap.get(tag);
			}else{
			    tagValueList = new ArrayList<String>();
			    assetTagToValueMap.put(tag, tagValueList);
			}
			List<XMLObject> attributeValues = attribute.getAttributeValues();
			for (XMLObject xmlObject : attributeValues) {
			    Element dom = xmlObject.getDOM();
			    tagValue = dom.getTextContent();
			}
			if(StringUtils.isBlank(tagValue)){
			    continue;
			}
			tagValueList.add(tagValue);
		    }
		}
		
		ObjectMapper mapper = new ObjectMapper();
		try {
		    String writeValueAsString = mapper.writeValueAsString(assetTagToValueMap);
		    ahHost.setAssetTags(writeValueAsString);
		} catch (JsonProcessingException e) {
		    log.error("Error converting map of asset tags to JSON");
		}
	    }

	}
	if (citHostAttestation != null) {
	    ahHost.setSamlReport(citHostAttestation.getSaml());
	    HostTrustResponse hostTrustResponse = citHostAttestation.getHostTrustResponse();
	    ObjectMapper objectMapper = new ObjectMapper();
	    try {
		ahHost.setTrustTagsJson(objectMapper.writeValueAsString(hostTrustResponse));
	    } catch (JsonProcessingException e) {
		log.error(
			"Unable to parse the 'host_trust_response' from the host attestation response for host: {} and name: {}",
			citHost.getId(), citHost.getName());
	    }
	}

	return ahHost;
    }

    public static AhHost mapHostToAhHost(MWHost host, AhHost ahHost) {
	log.info("Before getting logged in user");
	String currentUser = ShiroUtil.subjectUsername();
	log.info("After getting logged in user");
	return mapHostToAhHost(host, ahHost, currentUser);

    }

    public static Host mapAhHostToCitHost(AhHost ahHost) {
	Host host = new Host();
	host.setId(new UUID(ahHost.getId().getBytes()));
	host.setAikCertificate(ahHost.getAikCertificate());
	host.setAikSha1(ahHost.getAikSha1());
	host.setBiosMleUuid(ahHost.getBiosMleUuid());
	host.setConnectionUrl(ahHost.getConnectionUrl());
	host.setHardwareUuid(ahHost.getHardwareUuid());
	host.setName(ahHost.getHostName());
	return host;
    }
}
