package com.intel.attestationhub.endpoint;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
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
import com.intel.attestationhub.api.Mapping;
import com.intel.attestationhub.api.MappingResultResponse;
import com.intel.attestationhub.api.SearchCriteriaForMapping;
import com.intel.attestationhub.service.AttestationHubService;
import com.intel.attestationhub.service.impl.AttestationHubServiceImpl;
import com.intel.dcsg.cpg.validation.RegexPatterns;
import com.intel.dcsg.cpg.validation.ValidationUtil;
import com.intel.mtwilson.attestationhub.controller.exceptions.NonexistentEntityException;
import com.intel.mtwilson.attestationhub.data.AhMapping;
import com.intel.mtwilson.attestationhub.exception.AttestationHubException;
import com.intel.mtwilson.launcher.ws.ext.V2;

/**
 * @author GS Lab
 * 
 */
@V2
@Path("/")
public class HostAssignments {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(HostAssignments.class);

    /**
     * Create the tenant to host mappings by passing the JSON Array that
     * contains the list of host id to be mapped to this tenant
     * 
     * @mtwContentTypeReturned JSON
     * @mtwMethodType POST
     * @mtwSampleRestCall
     * 
     *                    <pre>
     * https://{IP/HOST_NAME}/v1/host-assignments
     * Input: {
     * "tenant_id": "256E853E-41EA-4E44-B531-420C5CDB35B5",
     * "hardware_uuids": ["97a65f4e-62ed-479b-9e4e-efa143ac5d5e", "a8f024fc-ebcd-40f3-8ba9-6be4bf6ecb9c"] }
     * 
     *  Output: 
     *  {
     *     "mappings": [
     *     {
     *       "mapping_id": "3FC59BE5-6352-4530-879C-A4DFDF085BD7",
     *       "tenant_id": "256E853E-41EA-4E44-B531-420C5CDB35B5",
     *       "hardware_uuid": "97a65f4e-62ed-479b-9e4e-efa143ac5d5e"
     *     },
     *     {
     *       "mapping_id": "3FB0B5E0-1066-4DED-BE03-A35BB909E9B8",
     *       "tenant_id": "256E853E-41EA-4E44-B531-420C5CDB35B5",
     *       "hardware_uuid": "a8f024fc-ebcd-40f3-8ba9-6be4bf6ecb9c"
     *     }
     *     ]
     *     }
     *                    
     *     When the tenant id is not in proper format:
     *     Output:
     *     {
     *     "error_code": "602",
     *     "error_message": "Invalid ID",
     *     "detail_errors": "Tenant Id is not in UUID format."
     *     }
     *     In case of an incorrect JSON that does not conform to the specified format:
     *     Output:
     *     {
     *     "error_code": "600",
     *     "error_message": "Validation failed",
     *     "detail_errors": "Hosts Id information is mandatory"
     *     }
     *     In case of failure occur on the server side while processing request:
     *     Output:
     *     {
     *     "error_code": "601",
     *     "error_message": "Request processing failed",
     *     "detail_errors": reason for the occurence of failure
     *     }
     *                    </pre>
     * 
     * @param id
     *            ID of the tenant
     * @param hostIds
     *            List of id of the hosts to be mapped with this tenant
     * @return Response containing the list of id corresponding to each tenant
     *         to host mapping
     */
    @POST
    @Path("/host-assignments")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createMapping(Mapping mapping) {
	if (!ValidationUtil.isValidWithRegex(mapping.tenant_id, RegexPatterns.UUID)) {
	    ErrorResponse errorResponse = new ErrorResponse(ErrorCode.INAVLID_ID);
	    errorResponse.detailErrors = "Tenant Id is not in UUID format";
	    return Response.status(Response.Status.BAD_REQUEST).entity(errorResponse).build();
	}

	if (mapping.hardware_uuids == null || mapping.hardware_uuids.size() == 0) {
	    ErrorResponse errorResponse = new ErrorResponse(ErrorCode.VALIDATION_FAILED);
	    errorResponse.detailErrors = "Hosts Id information is mandatory";
	    return Response.status(Response.Status.BAD_REQUEST).entity(errorResponse).build();
	}
	List<String> invalidhardwareuuids = new ArrayList<>();

	for (String hardware_uuid : mapping.hardware_uuids) {
	    if (!ValidationUtil.isValidWithRegex(hardware_uuid, RegexPatterns.UUID)) {
		invalidhardwareuuids.add(hardware_uuid);
	    }
	}
	if (invalidhardwareuuids.size() > 0) {
	    ErrorResponse errorResponse = new ErrorResponse(ErrorCode.INAVLID_ID);
	    errorResponse.detailErrors = "Hardware UUID is not in UUID format. Following hardware uuids are in incorrct format: "+StringUtils.join(invalidhardwareuuids, ",");
	    return Response.status(Response.Status.BAD_REQUEST).entity(errorResponse).build();

	}

	AttestationHubService attestationHubService = AttestationHubServiceImpl.getInstance();
	MappingResultResponse mappingResultResponse;
	try {
	    mappingResultResponse = attestationHubService.createOrUpdateMapping(mapping.tenant_id,
		    mapping.hardware_uuids);
	} catch (AttestationHubException e) {
	    ErrorResponse errorResponse = new ErrorResponse(ErrorCode.REQUEST_PROCESSING_FAILED);
	    errorResponse.detailErrors = e.getMessage();
	    return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(errorResponse).build();
	}
	return Response.ok(mappingResultResponse).build();
    }

    /**
     * Retrieve the tenant to host mapping information by providing the mapping
     * id in the URL
     * 
     * @mtwContentTypeReturned JSON
     * @mtwMethodType GET
     * @mtwSampleRestCall
     * 
     *                    <pre>
     *                    https://{IP/HOST_NAME}/v1/host-assignments/3FB0B5E0-1066-4DED-BE03-A35BB909E9B8
     *                    
     *                    Output: 
     *                    {
                              "id": "26054082-8C79-4CEF-B1B5-3AA71A15E03B",
                              "host_hardware_uuid": "97a65f4e-62ed-479b-9e4e-efa143ac5d5f",
                              "deleted": false,
                              "tenant": {
                                "id": "3805B16C-311C-4ECD-B806-0D205325A23B",
                                "tenant_name": "Coke",
                                "config": "{\"name\":\"Coke\",\"deleted\":false,\"plugins\":[{\"name\":\"nOvA\",\"properties\":[{\"key\":\"endpoint\",\"value\":\"http://www.nova.com\"}]}]}",
                                "created_by": "admin",
                                "modified_date": 1468832393433,
                                "modified_by": "admin",
                                "deleted": false
                              }
                            }
     * 
     *     When the mapping id is not in proper format:
     *     Output:
     *     {
     *     "error_code": "602",
     *     "error_message": "Invalid ID",
     *     "detail_errors": "Mapping Id is not in UUID format."
     *     }
     *     In case of failure occur on the server side while processing request:
     *     Output:
     *     {
     *     "error_code": "601",
     *     "error_message": "Request processing failed",
     *     "detail_errors": reason for the occurence of failure
     *     }
     *                    </pre>
     * 
     * @param id
     *            ID of the mapping
     * @return the tenant to host mapping information
     */
    @GET
    @Path("host-assignments/{id:[0-9a-zA-Z_-]+ }")
    @Produces(MediaType.APPLICATION_JSON)
    public Response retrieveMapping(@PathParam("id") String id) {
	if (!ValidationUtil.isValidWithRegex(id, RegexPatterns.UUID)) {
	    ErrorResponse errorResponse = new ErrorResponse(ErrorCode.INAVLID_ID);
	    errorResponse.detailErrors = "Mapping Id is not in UUID format";
	    return Response.status(Response.Status.BAD_REQUEST).entity(errorResponse).build();
	}

	AttestationHubService attestationHubService = AttestationHubServiceImpl.getInstance();
	AhMapping ahMapping;
	try {
	    ahMapping = attestationHubService.retrieveMapping(id);
	} catch (AttestationHubException e) {
	    ErrorResponse errorResponse = new ErrorResponse(ErrorCode.REQUEST_PROCESSING_FAILED);
	    errorResponse.errorMessage = e.getMessage();
	    Status status = Response.Status.INTERNAL_SERVER_ERROR;
	    if (e.getCause() instanceof NonexistentEntityException) {
		status = Response.Status.NOT_FOUND;
	    }
	    return Response.status(status).entity(errorResponse).build();
	}
	return Response.ok(ahMapping).build();
    }

    /**
     * Retrieve all of the tenant to host mapping information
     * 
     * @mtwContentTypeReturned JSON
     * @mtwMethodType GET
     * @mtwSampleRestCall
     * 
     *                    <pre>
     *                    https://{IP/HOST_NAME}/v1/host-assignments
     *                    Output: [
                            {
                            "id": "26054082-8C79-4CEF-B1B5-3AA71A15E03B",
                            "host_hardware_uuid": "97a65f4e-62ed-479b-9e4e-efa143ac5d5f",
                            "deleted": false,
                            "tenant": {
                              "id": "3805B16C-311C-4ECD-B806-0D205325A23B",
                              "tenant_name": "Coke",
                              "config": "{\"name\":\"Coke\",\"deleted\":false,\"plugins\":[{\"name\":\"nOvA\",\"properties\":[{\"key\":\"endpoint\",\"value\":\"http://www.nova.com\"}]}]}",
                              "created_by": "admin",
                              "modified_date": 1468832393433,
                              "modified_by": "admin",
                              "deleted": false
                            }
                            },
                            {
                            "id": "7EAAB7AA-C586-42D1-8B79-B72122DB9C7C",
                            "host_hardware_uuid": "e22aea80-34d4-11e1-a5d8-c03fd56d9c24",
                            "deleted": true,
                            "tenant": {
                              "id": "3805B16C-311C-4ECD-B806-0D205325A23B",
                              "tenant_name": "Coke",
                              "config": "{\"name\":\"Coke\",\"deleted\":false,\"plugins\":[{\"name\":\"nOvA\",\"properties\":[{\"key\":\"endpoint\",\"value\":\"http://www.nova.com\"}]}]}",
                              "created_by": "admin",
                              "modified_date": 1468832393433,
                              "modified_by": "admin",
                              "deleted": false
                            }
                            }
    ]
     * 
     *     When the mapping id is not in proper format:
     *     Output:
     *     {
     *     "error_code": "602",
     *     "error_message": "Invalid ID",
     *     "detail_errors": "Mapping Id is not in UUID format."
     *     }
     *     In case of failure occur on the server side while processing request:
     *     Output:
     *     {
     *     "error_code": "601",
     *     "error_message": "Request processing failed",
     *     "detail_errors": reason for the occurence of failure
     *     }
     *                    </pre>
     * 
     * @param id
     *            ID of the mapping
     * @return the tenant to host mapping information
     */
    public Response retrieveAllMappings() {
	AttestationHubService attestationHubService = AttestationHubServiceImpl.getInstance();
	List<AhMapping> ahMappings;
	try {
	    ahMappings = attestationHubService.retrieveAllMappings();
	} catch (AttestationHubException e) {
	    ErrorResponse errorResponse = new ErrorResponse(ErrorCode.REQUEST_PROCESSING_FAILED);
	    errorResponse.errorMessage = e.getMessage();
	    Status status = Response.Status.INTERNAL_SERVER_ERROR;
	    if (e.getCause() instanceof NonexistentEntityException) {
		status = Response.Status.NOT_FOUND;
	    }
	    return Response.status(status).entity(errorResponse).build();
	}
	return Response.ok(ahMappings).build();
    }

    /**
     * Retrieve the tenant to host mapping information by providing the mapping
     * id in the URL
     * 
     * @mtwContentTypeReturned JSON
     * @mtwMethodType DELETE
     * @mtwSampleRestCall
     * 
     *                    <pre>
     *                    https://{IP/HOST_NAME}/v1/host-assignments/3FC59BE5-6352-4530-879C-A4DFDF085BD7
     *                    
     *                    Output: {
                            "id": "7EAAB7AA-C586-42D1-8B79-B72122DB9C7C",
                            "host_hardware_uuid": "e22aea80-34d4-11e1-a5d8-c03fd56d9c24",
                            "deleted": true,
                            "tenant": {
                            "id": "3805B16C-311C-4ECD-B806-0D205325A23B",
                            "tenant_name": "Coke",
                            "config": "{\"name\":\"Coke\",\"deleted\":false,\"plugins\":[{\"name\":\"nOvA\",\"properties\":[{\"key\":\"endpoint\",\"value\":\"http://www.nova.com\"}]}]}",
                            "created_by": "admin",
                            "modified_date": 1468832393433,
                            "modified_by": "admin",
                            "deleted": false
                            }
                            }
                            
     * 			}
     * 
     *     When the mapping id is not in proper format:
     *     Output:
     *     {
     *     "error_code": "602",
     *     "error_message": "Invalid ID",
     *     "detail_errors": "Mapping Id is not in UUID format."
     *     }
     *     In case of failure occur on the server side while processing request:
     *     Output:
     *     {
     *     "error_code": "601",
     *     "error_message": "Request processing failed",
     *     "detail_errors": reason for the occurence of failure
     *     }
     *                    </pre>
     * 
     * @param id
     *            ID of the mapping
     * @return the tenant to host mapping information
     */
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Path("host-assignments/{id:[0-9a-zA-Z_-]+ }")
    public Response deleteMapping(@PathParam("id") String id) {
	if (!ValidationUtil.isValidWithRegex(id, RegexPatterns.UUID)) {
	    ErrorResponse errorResponse = new ErrorResponse(ErrorCode.INAVLID_ID);
	    errorResponse.detailErrors = "Mapping Id is not in UUID format";
	    return Response.status(Response.Status.BAD_REQUEST).entity(errorResponse).build();
	}
	AhMapping ahMapping = null;
	AttestationHubService attestationHubService = AttestationHubServiceImpl.getInstance();
	try {
	    ahMapping = attestationHubService.retrieveMapping(id);
	    attestationHubService.deleteMapping(id);
	    ahMapping.setDeleted(true);
	} catch (AttestationHubException e) {
	    ErrorResponse errorResponse = new ErrorResponse(ErrorCode.REQUEST_PROCESSING_FAILED);
	    errorResponse.errorMessage = e.getMessage();
	    Status status = Response.Status.INTERNAL_SERVER_ERROR;
	    if (e.getCause() instanceof NonexistentEntityException) {
		status = Response.Status.NOT_FOUND;
	    }
	    return Response.status(status).entity(errorResponse).build();
	}
	return Response.ok(ahMapping).build();
    }

    /**
     * Retrieve the tenant to host mappings information by providing either
     * mapping id or host id in the URL Returns all the host-assignments if the
     * search string is not provided
     * 
     * @mtwContentTypeReturned JSON
     * @mtwMethodType GET
     * @mtwSampleRestCall
     * 
     *                    <pre>
     *                    https://{IP/HOST_NAME}/v1/host-assignments?tenant_id=72B99FA9-8FBB-4F20-B988-3990EB4410DA
     *                    OR
     *                    https://{IP/HOST_NAME}/v1/host-assignments?host_id=97a65f4e-62ed-479b-9e4e-efa143ac5d5e
     *                    OR
     *                    https://{IP/HOST_NAME}/v1/host-assignments
     *                    
     *                    Output:[
                            {
                            "id": "26054082-8C79-4CEF-B1B5-3AA71A15E03B",
                            "host_hardware_uuid": "97a65f4e-62ed-479b-9e4e-efa143ac5d5f",
                            "deleted": false,
                            "tenant": {
                              "id": "3805B16C-311C-4ECD-B806-0D205325A23B",
                              "tenant_name": "Coke",
                              "config": "{\"name\":\"Coke\",\"deleted\":false,\"plugins\":[{\"name\":\"nOvA\",\"properties\":[{\"key\":\"endpoint\",\"value\":\"http://www.nova.com\"}]}]}",
                              "created_by": "admin",
                              "modified_date": 1468832393433,
                              "modified_by": "admin",
                              "deleted": false
                            }
                            },
                            {
                            "id": "7EAAB7AA-C586-42D1-8B79-B72122DB9C7C",
                            "host_hardware_uuid": "e22aea80-34d4-11e1-a5d8-c03fd56d9c24",
                            "deleted": true,
                            "tenant": {
                              "id": "3805B16C-311C-4ECD-B806-0D205325A23B",
                              "tenant_name": "Coke",
                              "config": "{\"name\":\"Coke\",\"deleted\":false,\"plugins\":[{\"name\":\"nOvA\",\"properties\":[{\"key\":\"endpoint\",\"value\":\"http://www.nova.com\"}]}]}",
                              "created_by": "admin",
                              "modified_date": 1468832393433,
                              "modified_by": "admin",
                              "deleted": false
                            }
                            }
    ]
     * 			https://{IP/HOST_NAME}/v1/host-assignments
     *                  would return all the mappings in the hub
     * 
     *     When the tenant id or host_id is not provided:
     *     Output:
     *     {
     *     "error_code": "600",
     *     "error_message": "Validation failed",
     *     "detail_errors": "Invalid search criteria: tenantId or hostId cannot be blank"
     *     }
     *     In case of failure occur on the server side while processing request:
     *     Output:
     *     {
     *     "error_code": "601",
     *     "error_message": "Request processing failed",
     *     "detail_errors": reason for the occurence of failure
     *     }
     *                    </pre>
     * 
     * @param criteriaForMapping
     *            The pojo representation of the mapping search criteria
     * @return the tenant to host mapping information of mappings which contains
     *         the provided tenant_id or host_id
     */
    @GET
    @Path("host-assignments")
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchMappingsBySearchCriteria(@BeanParam SearchCriteriaForMapping criteriaForMapping,
	    @Context HttpServletRequest httpServletRequest) {
	AttestationHubService attestationHubService = AttestationHubServiceImpl.getInstance();
	if (StringUtils.isBlank(httpServletRequest.getQueryString())) {
	    return retrieveAllMappings();
	}
	String validate = criteriaForMapping.validate();
	if (StringUtils.isNotBlank(validate)) {
	    log.error("Invalid host assignment search criteria: {}", validate);
	    ErrorResponse errorResponse = new ErrorResponse(ErrorCode.VALIDATION_FAILED);
	    errorResponse.detailErrors = validate;
	    Status status = Response.Status.BAD_REQUEST;
	    return Response.status(status).entity(errorResponse).build();
	}
	List<AhMapping> ahMappings = null;

	try {
	    ahMappings = attestationHubService.searchMappingsBySearchCriteria(criteriaForMapping);
	} catch (AttestationHubException e) {
	    ErrorResponse errorResponse = new ErrorResponse(ErrorCode.REQUEST_PROCESSING_FAILED);
	    errorResponse.errorMessage = e.getMessage();
	    Status status = Response.Status.INTERNAL_SERVER_ERROR;
	    if (e.getCause() instanceof NonexistentEntityException) {
		status = Response.Status.NOT_FOUND;
	    }
	    return Response.status(status).entity(errorResponse).build();
	}
	return Response.ok(ahMappings).build();
    }

}
