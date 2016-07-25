package com.intel.attestationhub.endpoint;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang.StringUtils;

import com.intel.attestationhub.api.ErrorCode;
import com.intel.attestationhub.api.ErrorResponse;
import com.intel.attestationhub.api.Tenant;
import com.intel.attestationhub.api.TenantFilterCriteria;
import com.intel.attestationhub.service.AttestationHubService;
import com.intel.attestationhub.service.impl.AttestationHubServiceImpl;
import com.intel.dcsg.cpg.validation.RegexPatterns;
import com.intel.dcsg.cpg.validation.ValidationUtil;
import com.intel.mtwilson.attestationhub.controller.exceptions.NonexistentEntityException;
import com.intel.mtwilson.attestationhub.exception.AttestationHubException;
import com.intel.mtwilson.launcher.ws.ext.V2;

/**
 * @author GS Lab
 * 
 */
@V2
@Path("/tenants")
public class Tenants {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory
	    .getLogger(Tenants.class);

    /**
     * Create the tenant by passing the JSON that configures the tenant and its
     * plugins
     * 
     * @mtwContentTypeReturned JSON
     * @mtwMethodType POST
     * @mtwSampleRestCall
     * 
     *                    <pre>
     *                    https://{IP/HOST_NAME}/v1/tenants
     *                    
     *                    Input:
     *                    {"name":"Pepsi", "plugins":[{"name":"Nova",
     *                    "properties":[{"key":"endpoint",
     *                    "value":"http://www.google.com"}]}, {"name":"mesos",
     *                    "properties":[{"key":"endpoint",
     *                    "value":"http://www.yahoo.com"}]}]}
     * 
     *                    Output: { "id":
     *                    "BA49C7C8-B092-4841-A747-D4F4084AE5B8","name":
     *                    "Pepsi", "plugins": [{ "name": "Nova", "properties":
     *                    [{ "key": "endpoint", "value": "http://www.google.com"
     *                    }] }, { "name": "mesos", "properties": [{ "key":
     *                    "endpoint", "value": "http://www.yahoo.com" }] }] }
     *                    
     *     When a name is not provided for the tenant:
     *     Output:
     *     {
     *     "error_code": "600",
     *     "error_message": "Validation failed",
     *     "detail_errors": "Tenant Name cannot be empty."
     *     }
     *     When the plugin does not have a valid name:
     *     {
     *     "error_code": "600",
     *     "error_message": "Validation failed",
     *     "detail_errors": "Plugin name has to be either of nova/kubernetes/mesos,"
     *     }
     *     In case of an incorrect JSON that does not conform to the specified format:
     *     Output:
     *     {
     *     "error_code": "600",
     *     "error_message": "Validation failed",
     *     "detail_errors": "Plugin information is mandatory"
     *     }
     *     In case of failure occur on the server side while processing request:
     *     Output:
     *     {
     *     "error_code": "601",
     *     "error_message": "Request processing failed",
     *     "detail_errors": reason for the occurence of failure
     *     }
     * </pre>
     * 
     * @param tenant
     *            The pojo representation of the tenant configuration
     * @return Response containing the saved configuration with its ID
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createTenant(Tenant tenant) {
	String validateResult = tenant.validate();
	if (StringUtils.isNotBlank(validateResult)) {
	    ErrorResponse errorResponse = new ErrorResponse(
		    ErrorCode.VALIDATION_FAILED);
	    errorResponse.detailErrors = validateResult;
	    return Response.status(Response.Status.BAD_REQUEST)
		    .entity(errorResponse).build();
	}
	AttestationHubService attestationHubService = AttestationHubServiceImpl
		.getInstance();
	try {
	    String newTenantId = attestationHubService.createTenant(tenant);
	    tenant.setId(newTenantId);
	} catch (AttestationHubException e) {
	    ErrorResponse errorResponse = new ErrorResponse(
		    ErrorCode.REQUEST_PROCESSING_FAILED);
	    errorResponse.detailErrors = e.getMessage();
	    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
		    .entity(errorResponse).build();
	}
	return Response.ok(tenant).build();
    }

    /**
     * Retrieve the tenant configuration by providing the tenant id in the URL
     * 
     * @mtwContentTypeReturned JSON
     * @mtwMethodType GET
     * @mtwSampleRestCall
     * 
     *                    <pre>
     *                    https://{IP/HOST_NAME}/v1/tenants/BA49C7C8-B092-4841-A747-D4F4084AE5B8 
     *                    
     *                    Output: { "id":
     *                    "BA49C7C8-B092-4841-A747-D4F4084AE5B8","name":
     *                    "Pepsi", "plugins": [{ "name": "Nova", "properties":
     *                    [{ "key": "endpoint", "value": "http://www.google.com"
     *                    }] }, { "name": "mesos", "properties": [{ "key":
     *                    "endpoint", "value": "http://www.yahoo.com" }] }] }
     *                    
     *     When the tenant id is not in proper format:
     *     Output:
     *     {
     *     "error_code": "602",
     *     "error_message": "Invalid ID",
     *     "detail_errors": "Tenant Id is not in UUID format."
     *     }
     *     In case of failure occur on the server side while processing request:
     *     Output:
     *     {
     *     "error_code": "601",
     *     "error_message": "Request processing failed",
     *     "detail_errors": reason for the occurence of failure
     *     }
     * </pre>
     * 
     * @param id
     *            ID of the tenant
     * @return the tenant configuration
     */
    @GET
    @Path("/{id:[0-9a-zA-Z_-]+ }")
    @Produces(MediaType.APPLICATION_JSON)
    public Response retrieveTenant(@PathParam("id") String id) {
	if (!ValidationUtil.isValidWithRegex(id, RegexPatterns.UUID)) {
	    ErrorResponse errorResponse = new ErrorResponse(
		    ErrorCode.INAVLID_ID);
	    errorResponse.detailErrors = "Tenant Id is not in UUID format";
	    return Response.status(Response.Status.BAD_REQUEST)
		    .entity(errorResponse).build();
	}

	AttestationHubService attestationHubService = AttestationHubServiceImpl
		.getInstance();
	Tenant tenant;
	try {
	    tenant = attestationHubService.retrieveTenant(id);
	} catch (AttestationHubException e) {
	    ErrorResponse errorResponse = new ErrorResponse(
		    ErrorCode.REQUEST_PROCESSING_FAILED);
	    errorResponse.detailErrors = e.getMessage();
	    Status status = Response.Status.INTERNAL_SERVER_ERROR;
	    if (e.getCause() instanceof NonexistentEntityException) {
		status = Response.Status.NOT_FOUND;
	    }
	    return Response.status(status).entity(errorResponse).build();
	}
	return Response.ok(tenant).build();
    }

    private Response retrieveAllTenants() {
	AttestationHubService attestationHubService = AttestationHubServiceImpl
		.getInstance();
	List<Tenant> tenants;
	try {
	    tenants = attestationHubService.retrieveAllTenants();
	} catch (AttestationHubException e) {
	    log.error("Error searching for all atenants", e);
	    ErrorResponse errorResponse = new ErrorResponse(
		    ErrorCode.REQUEST_PROCESSING_FAILED);
	    errorResponse.detailErrors = e.getMessage();
	    Status status = Response.Status.INTERNAL_SERVER_ERROR;
	    if (e.getCause() instanceof NonexistentEntityException) {
		status = Response.Status.NOT_FOUND;
	    }
	    return Response.status(status).entity(errorResponse).build();
	}
	return Response.ok(tenants).build();
    }

    /**
     * Update the tenant configuration by providing the tenant id in the URL and
     * the configuration in form of JSON in the body
     * 
     * @mtwContentTypeReturned JSON
     * @mtwMethodType PUT
     * @mtwSampleRestCall
     * 
     *                    <pre>
     *                    https://{IP/HOST_NAME}/v1/tenants/BA49C7C8-B092-4841-A747-D4F4084AE5B8 
     *                    Input:
     *                    {"name":"Pepsi", "plugins":[{"name":"Nova",
     *                    "properties":[{"key":"endpoint",
     *                    "value":"http://www.google.com"}]}, {"name":"mesos",
     *                    "properties":[{"key":"endpoint",
     *                    "value":"http://www.yahoo.com"}]}]}
     * 
     *                    Output: { "id":
     *                    "BA49C7C8-B092-4841-A747-D4F4084AE5B8","name":
     *                    "Pepsi", "plugins": [{ "name": "Nova", "properties":
     *                    [{ "key": "endpoint", "value": "http://www.google.com"
     *                    }] }, { "name": "mesos", "properties": [{ "key":
     *                    "endpoint", "value": "http://www.yahoo.com" }] }] }
     *                    
     *     When the tenant id is not in proper format:
     *     Output:
     *     {
     *     "error_code": "602",
     *     "error_message": "Invalid ID",
     *     "detail_errors": "Tenant Id is not in UUID format."
     *     }
     *     When a name is not provided for the tenant:
     *     Output:
     *     {
     *     "error_code": "600",
     *     "error_message": "Validation failed",
     *     "detail_errors": "Tenant Name cannot be empty."
     *     }
     *     When the plugin does not have a valid name:
     *     {
     *     "error_code": "600",
     *     "error_message": "Validation failed",
     *     "detail_errors": "Plugin name has to be either of nova/kubernetes/mesos,"
     *     }
     *     In case of an incorrect JSON that does not conform to the specified format:
     *     Output:
     *     {
     *     "error_code": "600",
     *     "error_message": "Validation failed",
     *     "detail_errors": "Plugin information is mandatory"
     *     }
     *     In case of failure occur on the server side while processing request:
     *     Output:
     *     {
     *     "error_code": "601",
     *     "error_message": "Request processing failed",
     *     "detail_errors": reason for the occurence of failure
     *     }
     * </pre>
     * 
     * @param id
     *            ID of the tenant
     * @param tenant
     *            the updated tenant configuration
     * @return the updated tenant configuration
     */
    @PUT
    @Path("/{id:[0-9a-zA-Z_-]+ }")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateTenant(@PathParam("id") String id, Tenant tenant) {
	if (!ValidationUtil.isValidWithRegex(id, RegexPatterns.UUID)) {
	    ErrorResponse errorResponse = new ErrorResponse(
		    ErrorCode.INAVLID_ID);
	    errorResponse.detailErrors = "Tenant Id is not in UUID format";
	    return Response.status(Response.Status.BAD_REQUEST)
		    .entity(errorResponse).build();
	}

	String validateResult = tenant.validate();
	if (StringUtils.isNotBlank(validateResult)) {
	    ErrorResponse errorResponse = new ErrorResponse(
		    ErrorCode.VALIDATION_FAILED);
	    errorResponse.detailErrors = validateResult;
	    return Response.status(Response.Status.BAD_REQUEST)
		    .entity(errorResponse).build();
	}

	AttestationHubService attestationHubService = AttestationHubServiceImpl
		.getInstance();
	Tenant newTenant;
	try {
	    tenant.setId(id);
	    newTenant = attestationHubService.updateTenant(tenant);
	} catch (AttestationHubException e) {
	    ErrorResponse errorResponse = new ErrorResponse(
		    ErrorCode.REQUEST_PROCESSING_FAILED);
	    errorResponse.detailErrors = e.getMessage();
	    Status status = Response.Status.INTERNAL_SERVER_ERROR;
	    if (e.getCause() instanceof NonexistentEntityException) {
		status = Response.Status.NOT_FOUND;
	    }
	    return Response.status(status).entity(errorResponse).build();
	}
	return Response.ok(newTenant).build();
    }

    /**
     * Retrieve the tenant configuration by providing the tenant id in the URL
     * 
     * @mtwContentTypeReturned JSON
     * @mtwMethodType DELETE
     * @mtwSampleRestCall
     * 
     *                    <pre>
     *                    https://{IP/HOST_NAME}/v1/tenants/BA49C7C8-B092-4841-A747-D4F4084AE5B8 
     *                    
     *                    Output: { "id":
     *                    "BA49C7C8-B092-4841-A747-D4F4084AE5B8","name":
     *                    "Pepsi", "plugins": [{ "name": "Nova", "properties":
     *                    [{ "key": "endpoint", "value": "http://www.google.com"
     *                    }] }, { "name": "mesos", "properties": [{ "key":
     *                    "endpoint", "value": "http://www.yahoo.com" }] }] }
     *                    
     *     When the tenant id is not in proper format:
     *     Output:
     *     {
     *     "error_code": "602",
     *     "error_message": "Invalid ID",
     *     "detail_errors": "Tenant Id is not in UUID format."
     *     }
     *     In case of failure occur on the server side while processing request:
     *     Output:
     *     {
     *     "error_code": "601",
     *     "error_message": "Request processing failed",
     *     "detail_errors": reason for the occurence of failure
     *     }
     * </pre>
     * 
     * @param id
     *            ID of the tenant
     * @return the tenant configuration
     */
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id:[0-9a-zA-Z_-]+ }")
    public Response deleteTenant(@PathParam("id") String id) {
	if (!ValidationUtil.isValidWithRegex(id, RegexPatterns.UUID)) {
	    ErrorResponse errorResponse = new ErrorResponse(
		    ErrorCode.INAVLID_ID);
	    errorResponse.detailErrors = "Tenant Id is not in UUID format";
	    return Response.status(Response.Status.BAD_REQUEST)
		    .entity(errorResponse).build();
	}
	Tenant tenant = null;
	AttestationHubService attestationHubService = AttestationHubServiceImpl
		.getInstance();
	try {
	    tenant = attestationHubService.retrieveTenant(id);
	    attestationHubService.deleteTenant(id);
	    tenant.setDeleted(true);
	} catch (AttestationHubException e) {
	    ErrorResponse errorResponse = new ErrorResponse(
		    ErrorCode.REQUEST_PROCESSING_FAILED);
	    errorResponse.detailErrors = e.getMessage();
	    Status status = Response.Status.INTERNAL_SERVER_ERROR;
	    if (e.getCause() instanceof NonexistentEntityException) {
		status = Response.Status.NOT_FOUND;
	    }
	    return Response.status(status).entity(errorResponse).build();
	}
	return Response.ok(tenant).build();
    }

    /**
     * Retrieve the tenant configuration by providing the tenant name in the URL
     * Returns all tenants configuration if the search string is not provided
     * 
     * @mtwContentTypeReturned JSON
     * @mtwMethodType GET
     * @mtwSampleRestCall
     * 
     *                    <pre>
     *                    https://{IP/HOST_NAME}/v1/tenants?nameEqualTo=Pepsi
     *                    OR
     *                    https://{IP/HOST_NAME}/v1/tenants
     *                    
     *                    Output: { "id":
     *                    "BA49C7C8-B092-4841-A747-D4F4084AE5B8","name":
     *                    "Pepsi", "plugins": [{ "name": "Nova", "properties":
     *                    [{ "key": "endpoint", "value": "http://www.google.com"
     *                    }] }, { "name": "mesos", "properties": [{ "key":
     *                    "endpoint", "value": "http://www.yahoo.com" }] }] }
     *                    
     *                    https://{IP/HOST_NAME}/v1/tenants 
     *                    would return all the tenants in the hub
     *  
     *     When the tenant name is not provided:
     *     Output:
     *     {
     *     "error_code": "600",
     *     "error_message": "Validation failed",
     *     "detail_errors": "Name of the tenant to be searched cannot be blank"
     *     }
     *     In case of failure occur on the server side while processing request:
     *     Output:
     *     {
     *     "error_code": "601",
     *     "error_message": "Request processing failed",
     *     "detail_errors": reason for the occurence of failure
     *     }
     * </pre>
     * 
     * @param tenantFilterCriteria
     *            The pojo representation of the tenant filter criteria
     * @return the tenants configuration which satisfy the given criteria
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchTenantsBySearchCriteria(
	    @BeanParam TenantFilterCriteria tenantFilterCriteria,
	    @Context HttpServletRequest httpServletRequest) {
	log.info("Searching for tenants with name : {}",
		tenantFilterCriteria.nameEqualTo);
	AttestationHubService attestationHubService = AttestationHubServiceImpl
		.getInstance();
	List<Tenant> tenantsList = new ArrayList<>();
	if (StringUtils.isBlank(httpServletRequest.getQueryString())) {
	    return retrieveAllTenants();
	}
	String validate = tenantFilterCriteria.validate();
	if (StringUtils.isNotBlank(validate)) {
	    log.error("Invalid tenant search criteria: {}", validate);
	    ErrorResponse errorResponse = new ErrorResponse(
		    ErrorCode.VALIDATION_FAILED);
	    errorResponse.detailErrors = validate;
	    Status status = Response.Status.BAD_REQUEST;
	    return Response.status(status).entity(errorResponse).build();

	}

	try {
	    tenantsList = attestationHubService
		    .searchTenantsBySearchCriteria(tenantFilterCriteria);
	} catch (AttestationHubException e) {
	    ErrorResponse errorResponse = new ErrorResponse(
		    ErrorCode.REQUEST_PROCESSING_FAILED);
	    errorResponse.detailErrors = e.getMessage();
	    Status status = Response.Status.INTERNAL_SERVER_ERROR;
	    if (e.getCause() instanceof NonexistentEntityException) {
		status = Response.Status.NOT_FOUND;
	    }
	    return Response.status(status).entity(errorResponse).build();
	}
	return Response.ok(tenantsList).build();
    }
}
