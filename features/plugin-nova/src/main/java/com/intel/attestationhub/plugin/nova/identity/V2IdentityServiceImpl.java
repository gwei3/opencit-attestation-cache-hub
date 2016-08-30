package com.intel.attestationhub.plugin.nova.identity;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intel.mtwilson.attestationhub.exception.AttestationHubException;

public class V2IdentityServiceImpl extends AbstractIdentityService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(V2IdentityServiceImpl.class);

    public void validateParams(String tenantName, String userName, String password, String domainName)
	    throws AttestationHubException {
	if (StringUtils.isBlank(tenantName)) {
	    throw new AttestationHubException("Tenant Name cannot be blank");
	}
	if (StringUtils.isBlank(userName)) {
	    throw new AttestationHubException("Tenant Name cannot be blank");
	}
	if (StringUtils.isBlank(password)) {
	    throw new AttestationHubException("Tenant Name cannot be blank");
	}

    }

    public String getAuthRequestBody(String tenantName, String userName, String password, String domainName)
	    throws AttestationHubException {
	validateParams(tenantName, userName, password);
	AuthTokenBody authTokenBody = new AuthTokenBody();
	authTokenBody.auth = new Auth();
	authTokenBody.auth.tenantName = tenantName;
	authTokenBody.auth.passwordCredentials = new PasswordCredentials();
	authTokenBody.auth.passwordCredentials.username = userName;
	authTokenBody.auth.passwordCredentials.password = password;

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

    public String getAuthTokenFromResponse() throws AttestationHubException {
	String authToken = null;
	boolean responseHasError = false;
	JSONObject obj = new JSONObject(responseStr);
	if (obj.has("access")) {
	    JSONObject jsonObjectAccess = obj.getJSONObject("access");
	    if (jsonObjectAccess.has("token")) {
		JSONObject property = jsonObjectAccess.getJSONObject("token");
		authToken = property.getString("id");
	    } else {
		responseHasError = true;
	    }
	} else {
	    responseHasError = true;
	}

	if (responseHasError) {
	    log.error("Error fetching authToken from response:{} ", responseStr);
	    throw new AttestationHubException("Error fetching authToken from response");
	}

	return authToken;
    }

    @Override
    public String getApiEndpointFromAuthResponse() throws AttestationHubException {
	String endpointUrl = null;
	JSONObject obj = new JSONObject(responseStr);
	JSONObject access = obj.getJSONObject("access");
	JSONArray serviceCatalogArray = access.getJSONArray("serviceCatalog");

	for (int i = 0; i < serviceCatalogArray.length(); i++) {
	    JSONObject serviceCatalog = (JSONObject) serviceCatalogArray.get(i);
	    if (serviceCatalog.has("endpoints")) {		
		String sc_type = serviceCatalog.getString("type");
		if(!"COMPUTE".equalsIgnoreCase(sc_type)){
		    continue;
		}
		JSONArray endpointArray = serviceCatalog.getJSONArray("endpoints");
		if(endpointArray.length() > 0){
		    JSONObject endpoint = (JSONObject) endpointArray.get(0);
		    String publicUrl = endpoint.getString("publicURL");
		    endpointUrl = publicUrl;
		    break;		    
		}
	    }
	}

	return endpointUrl;
    }

    public String getAuthEndpoint(String glanceKeystonePublicEndpoint) {
	return glanceKeystonePublicEndpoint + "/v2.0/tokens";
    }

    @JsonInclude(value = Include.NON_NULL)
    class AuthTokenBody {
	public Auth auth;

    }

    @JsonInclude(value = Include.NON_NULL)
    class Auth {
	public String tenantName;
	public PasswordCredentials passwordCredentials;

    }

    @JsonInclude(value = Include.NON_NULL)
    class PasswordCredentials {
	public String username;
	public String password;
    }

}
