package com.intel.attestationhub.plugin.nova.identity;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intel.mtwilson.attestationhub.exception.AttestationHubException;

public class V3IdentityServiceImpl extends AbstractIdentityService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(V3IdentityServiceImpl.class);

    @Override
    public String getAuthEndpoint(String glanceKeystonePublicEndpoint) {
	return glanceKeystonePublicEndpoint + "/v3/auth/tokens";
    }


    public void validateV3Params(String tenantName, String userName, String password, String domainName)
	    throws AttestationHubException {
	validateParams(tenantName, userName, password);
	if (StringUtils.isBlank(domainName)) {
	    throw new AttestationHubException("Domain Name cannot be blank for Mitaka");
	}
    }

    @Override
    public String getAuthRequestBody(String tenantOrProjectName, String userName, String password, String domainName)
	    throws AttestationHubException {
	/*
	 * Example json request:-
	 * 
	 * 
	 * 
	 * { "auth": { "identity": { "methods": ["password"], "password": {
	 * "user": { "name": "admin", "domain": { "name": "default" },
	 * "password": "intelmh" } } }, "scope": { "project": { "name": "admin",
	 * "domain": { "name": "default" } } } } }
	 */

	validateV3Params(tenantOrProjectName, userName, password, domainName);
	AuthTokenBody authTokenBody = new AuthTokenBody();
	authTokenBody.auth = new Auth();
	authTokenBody.auth.identity = new Identity();
	authTokenBody.auth.identity.methods = new String[] { "password" };
	authTokenBody.auth.identity.password = new Password();
	authTokenBody.auth.identity.password.user = new User();
	authTokenBody.auth.identity.password.user.domain = new Domain();
	authTokenBody.auth.identity.password.user.domain.name = domainName;
	authTokenBody.auth.identity.password.user.name = userName;
	authTokenBody.auth.identity.password.user.password = password;
	authTokenBody.auth.scope = new Scope();
	authTokenBody.auth.scope.project = new Project();
	authTokenBody.auth.scope.project.name = tenantOrProjectName;
	authTokenBody.auth.scope.project.domain = new Domain();
	authTokenBody.auth.scope.project.domain.name = domainName;
	ObjectMapper mapper = new ObjectMapper();
	String body;
	try {
	    body = mapper.writeValueAsString(authTokenBody);
	} catch (JsonProcessingException e2) {
	    log.error("Error while creating auth token", e2);
	    throw new AttestationHubException("Error while creating auth token", e2);
	}

	return body;
    }

    @Override
    public String getAuthTokenFromResponse() throws AttestationHubException {
	log.info("name::" + httpResponse.getFirstHeader("X-Subject-Token").getName());
	log.info("value::" + httpResponse.getFirstHeader("X-Subject-Token").getValue());
	return httpResponse.getFirstHeader("X-Subject-Token").getValue();
    }

    
    @Override
    public String getApiEndpointFromAuthResponse() throws AttestationHubException {
	String endpointUrl = null;
	JSONObject obj = new JSONObject(responseStr);
	JSONObject token = obj.getJSONObject("token");
	JSONArray catalogArray = token.getJSONArray("catalog");

	for (int i = 0; i < catalogArray.length(); i++) {
	    JSONObject serviceCatalog = (JSONObject) catalogArray.get(i);
	    if (serviceCatalog.has("endpoints")) {		
		String sc_type = serviceCatalog.getString("type");
		if(!"COMPUTE".equalsIgnoreCase(sc_type)){
		    continue;
		}
		JSONArray endpointArray = serviceCatalog.getJSONArray("endpoints");
		if(endpointArray.length() > 0){
		    JSONObject endpoint = (JSONObject) endpointArray.get(0);
		    String publicUrl = endpoint.getString("url");
		    endpointUrl = publicUrl;
		    break;		    
		}
	    }
	}

	return endpointUrl;
    }

    @JsonInclude(value = Include.NON_NULL)
    class AuthTokenBody {
	public Auth auth;

    }

    @JsonInclude(value = Include.NON_NULL)
    class Auth {
	public Identity identity;
	public Scope scope;
    }

    @JsonInclude(value = Include.NON_NULL)
    class Identity {
	public String[] methods;
	public Password password;
    }

    @JsonInclude(value = Include.NON_NULL)
    class Password {
	public User user;
    }

    @JsonInclude(value = Include.NON_NULL)
    class User {
	public Domain domain;
	public String name;
	public String password;
    }

    @JsonInclude(value = Include.NON_NULL)
    class Domain {
	public String name;
    }

    @JsonInclude(value = Include.NON_NULL)
    class Scope {
	public Project project;
    }

    @JsonInclude(value = Include.NON_NULL)
    class Project {
	public String name;
	public Domain domain;
    }


}
