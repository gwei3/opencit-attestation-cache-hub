/**
 * 
 */
package com.intel.attestationhub.endpoint;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
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
import com.intel.attestationhub.api.HostFilterCriteria;
import com.intel.attestationhub.service.AttestationHubService;
import com.intel.attestationhub.service.impl.AttestationHubServiceImpl;
import com.intel.dcsg.cpg.validation.RegexPatterns;
import com.intel.dcsg.cpg.validation.ValidationUtil;
import com.intel.mtwilson.attestationhub.controller.exceptions.NonexistentEntityException;
import com.intel.mtwilson.attestationhub.data.AhHost;
import com.intel.mtwilson.attestationhub.exception.AttestationHubException;
import com.intel.mtwilson.launcher.ws.ext.V2;

/**
 * @author GS Lab
 * 
 */

@V2
@Path("/hosts")
public class Hosts {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Hosts.class);

    /**
     * Retrieve the host information by providing the host id in the URL
     * 
     * @mtwContentTypeReturned JSON
     * @mtwMethodType GET
     * @mtwSampleRestCall
     * 
     *                    <pre>
     *                    https://{IP/HOST_NAME}/v1/hosts/97a65f4e-62ed-479b-9e4e-efa143ac5d5e 
     *                    
     *                    Output: {
     *     			"id": "97a65f4e-62ed-479b-9e4e-efa143ac5d5e",
     *     			"hardware_uuid": "dfc89080-34d4-11e1-a06f-b8aeed711ffc",
     *     			"host_name": "10.35.35.172",
     *     			"bios_mle_uuid": "8515c831-df15-4e3a-9331-b0eabbd3c3eb",
     *     			"aik_certificate": "-----BEGIN CERTIFICATE-----\r\nMIICvTCCAaWgAwIBAgIGAVVO4KrIMA0GCSqGSIb3DQEBBQUAMBsxGTAXBgNVBAMTEG10d2lsc29u\r\nLXBjYS1haWswHhcNMTYwNjE0MTIyNjM0WhcNMjYwNjE0MTIyNjM0WjAAMIIBIjANBgkqhkiG9w0B\r\nAQEFAAOCAQ8AMIIBCgKCAQEA2MbDubCo8QxbOZthGuJLyHap8o3zcrvMVWR1pl9hWwCE4MVJ9Wrw\r\nuHJB+v+MjW4jXApVQVNuGD0r1hvxY5shebwjKDtYHYAx0938z60Dl8rL2+71PjeZH9d9zv9sJUn6\r\nHh4pQZ7K5obr8ElJ0aOiAu46zAnkUk4pMbxkg/Vv+Qo9xdsrqlH/exZlkpIfu19JNG1GFVfBvTmE\r\nCGWkgtSSNXwwvHNcNVaGL0rUtcdavJBJ2lDwwSMSL39vhFcQ9yCQbauXMkji6My/zSPtTtC8HVeY\r\nx8t+/i1qURgKw8wR6s2bl1ne8OuMNiSS+L4hRdvCITLVygulqfY37xplzZQtOQIDAQABoyIwIDAe\r\nBgNVHREBAf8EFDASgRBISVNfSWRlbnRpdHlfS2V5MA0GCSqGSIb3DQEBBQUAA4IBAQBtCDKw+pdP\r\nBQCKXne5+bqZHlQ/S8wVHloxz3jguv/SOCQEwbhHYweY+kMLQutrD5Cf9nebwOnv3TMeDPjeqSk4\r\nc32FHMFiCZFq1Aa/6RS6fkfard46k9K60XyP2pBLNnlUwY+qbH6WmSDVQB8gmpUoObFY5/xx4Sgh\r\nYkIixs0Q0uLqM1LbQqzsZaClO71trARI7WDPnn77BEZT5liITrINjXHMOSV9yAiK8pic0XJi5Q04\r\nBRRjvfJTVMrOdoJ0k2CNbcX9ZSIV6JPdfybY37OwLjteMdrFhKf+qxMtMAz2s2KUOq6wsZpaB1SW\r\ntVnbwmISHKn1piRWs+2mhLVCJinB\r\n-----END CERTIFICATE-----\r\n",
     *     			"aik_sha1": "735b3ef40531fe73e77f435444b0dbb27a5468ef",
     *     			"connection_url": "intel:https://10.35.35.172:1443/;admin;cee9195fea1f5f23889127886e0afaedf2f96d8c25b1592d041375021dc92a0f",
     *     			"saml_report": "<?xml version=\"1.0\" encoding=\"UTF-8\"?><saml2:Assertion xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\" ID=\"HostTrustAssertion\" IssueInstant=\"2016-06-27T09:30:44.219Z\" Version=\"2.0\"><saml2:Issuer>https://10.35.35.175:8443</saml2:Issuer><Signature xmlns=\"http://www.w3.org/2000/09/xmldsig#\"><SignedInfo><CanonicalizationMethod Algorithm=\"http://www.w3.org/TR/2001/REC-xml-c14n-20010315#WithComments\"/><SignatureMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#rsa-sha1\"/><Reference URI=\"#HostTrustAssertion\"><Transforms><Transform Algorithm=\"http://www.w3.org/2000/09/xmldsig#enveloped-signature\"/></Transforms><DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\"/><DigestValue>0bJH2UPkh67vRI1Tvp5kyiIjDqE=</DigestValue></Reference></SignedInfo><SignatureValue>VLcfy12/gbBbSUIOh8577ZcGyfJPk+4iADr24XXf7ImstX97AqmNEgv5POQ33iRhbr3JfCnMN+Fx\nv3hZqG9Hee4EICa1xFTz5cv3diz7pS1NHs5DeFeJ5OauGSSNWR2G5U02a56NDAqB01P4VH0WHW/Z\nv0KoGnoT85ABttzPiO/S3sJV7HqsOZxZyqGMGBFbnrPJRWOQuM27iJUXiLYze5qj7Qnu3NwYT80B\nWO6RkI9PaNMlAnESeUcKMpfvQyix1RKclRQUICwZX1rKDyQQyXzE3f7Xe13IzSHyY+7ZuhBHoW3u\nGk2KzjiTD1aqPoTBgE5gRqVmZv9LBbGAQaFYPg==</SignatureValue><KeyInfo><X509Data><X509Certificate>MIIDYzCCAkugAwIBAgIECdRf9DANBgkqhkiG9w0BAQsFADBiMQswCQYDVQQGEwJVUzELMAkGA1UE\nCBMCQ0ExDzANBgNVBAcTBkZvbHNvbTEOMAwGA1UEChMFSW50ZWwxEjAQBgNVBAsTCU10IFdpbHNv\nbjERMA8GA1UEAxMIbXR3aWxzb24wHhcNMTYwNTE4MTMzMjUyWhcNMjYwNTE2MTMzMjUyWjBiMQsw\nCQYDVQQGEwJVUzELMAkGA1UECBMCQ0ExDzANBgNVBAcTBkZvbHNvbTEOMAwGA1UEChMFSW50ZWwx\nEjAQBgNVBAsTCU10IFdpbHNvbjERMA8GA1UEAxMIbXR3aWxzb24wggEiMA0GCSqGSIb3DQEBAQUA\nA4IBDwAwggEKAoIBAQCH3hHsqkcK17S0to9DmlhEGZ1EH7fguoBAkhRpAcN+5vazZjMLXixDEo/n\noppG5059X9KlR3aUvh5kzHzgTqRxpSWCbKg3xwZ7ZNvSGkpt5JTl7pTjlpN1W5nzpgO+7AfAUaxn\nexDkHCMV/Xo41cLDkcn5x+7GeT+yHB/71v4zMIHyhKTSLXYLMqR4OM8dyyGCM0H+cOuHoAgMUmdG\n36Uk1Emp+t8W8FpWOj7taW0OFh1Jmk8HRYmYxRZNqXI2eaak49ACT2h0yRUjLuHOOjyhytCf5RWq\nRTbKwp4t++WM5bT3y7hBGVFHCIYUX0k8qnHhym0alL51Eei+x5cT9ZrZAgMBAAGjITAfMB0GA1Ud\nDgQWBBRN6R0YrO5te6qgGU3ELHxvqJd3jzANBgkqhkiG9w0BAQsFAAOCAQEAa265hZetcTd7yxEE\nJiWvZGCqigitFy4jijP3QIbEFM7i5zr2ewQ7o4lf/shQw6ZcZAw49cOZmT6tuVGvSVcbJfk8Pk2H\nzB1SW6IpdRhZSaZY4oIMsS3OscAzZTHAme6Fgxv8QNSZ+sijDOxAskOoXsPcKzS73QF1/yVG52OR\npnjQ1/xjRb1vJXTYXxEbB/cSwzWjGB9JIgJRvNaJjX+WbVjipMTam66ZHz5P0uCO9iFI9Wo2bVqM\nppb4MyJbkQhpHVzofNeiio2EAFAKYBXjwALhy0qxvt/naj12CxaEEC9ywGCOE16oNd9yVHp9e1Gc\nIIavtxI5hBOkOV49st2+dg==</X509Certificate></X509Data></KeyInfo></Signature><saml2:Subject><saml2:NameID Format=\"urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified\">10.35.35.172</saml2:NameID><saml2:SubjectConfirmation Method=\"urn:oasis:names:tc:SAML:2.0:cm:sender-vouches\"><saml2:NameID Format=\"urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified\">Cloud Integrity Technology</saml2:NameID><saml2:SubjectConfirmationData Address=\"10.35.35.175\" NotBefore=\"2016-06-27T09:30:44.219Z\" NotOnOrAfter=\"2016-06-27T10:30:44.219Z\"/></saml2:SubjectConfirmation></saml2:Subject><saml2:AttributeStatement><saml2:Attribute Name=\"Host_Name\"><saml2:AttributeValue xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">10.35.35.172</saml2:AttributeValue></saml2:Attribute><saml2:Attribute Name=\"Host_Address\"><saml2:AttributeValue xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">10.35.35.172</saml2:AttributeValue></saml2:Attribute><saml2:Attribute Name=\"Trusted\"><saml2:AttributeValue xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:anyType\">true</saml2:AttributeValue></saml2:Attribute><saml2:Attribute Name=\"Trusted_BIOS\"><saml2:AttributeValue xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:anyType\">true</saml2:AttributeValue></saml2:Attribute><saml2:Attribute Name=\"BIOS_Name\"><saml2:AttributeValue xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">10.35.35.172_Intel_Corp.</saml2:AttributeValue></saml2:Attribute><saml2:Attribute Name=\"BIOS_Version\"><saml2:AttributeValue xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">0032.2014.0303</saml2:AttributeValue></saml2:Attribute><saml2:Attribute Name=\"BIOS_OEM\"><saml2:AttributeValue xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">Intel Corp.</saml2:AttributeValue></saml2:Attribute><saml2:Attribute Name=\"Trusted_VMM\"><saml2:AttributeValue xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:anyType\">true</saml2:AttributeValue></saml2:Attribute><saml2:Attribute Name=\"VMM_Name\"><saml2:AttributeValue xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">10.35.35.172_Romley_Docker</saml2:AttributeValue></saml2:Attribute><saml2:Attribute Name=\"VMM_Version\"><saml2:AttributeValue xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">14.04-1.11.2</saml2:AttributeValue></saml2:Attribute><saml2:Attribute Name=\"VMM_OSName\"><saml2:AttributeValue xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">Ubuntu</saml2:AttributeValue></saml2:Attribute><saml2:Attribute Name=\"VMM_OSVersion\"><saml2:AttributeValue xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">14.04</saml2:AttributeValue></saml2:Attribute><saml2:Attribute Name=\"AIK_Certificate\"><saml2:AttributeValue xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">-----BEGIN CERTIFICATE-----&#13;\nMIICvTCCAaWgAwIBAgIGAVVO4KrIMA0GCSqGSIb3DQEBBQUAMBsxGTAXBgNVBAMTEG10d2lsc29u&#13;\nLXBjYS1haWswHhcNMTYwNjE0MTIyNjM0WhcNMjYwNjE0MTIyNjM0WjAAMIIBIjANBgkqhkiG9w0B&#13;\nAQEFAAOCAQ8AMIIBCgKCAQEA2MbDubCo8QxbOZthGuJLyHap8o3zcrvMVWR1pl9hWwCE4MVJ9Wrw&#13;\nuHJB+v+MjW4jXApVQVNuGD0r1hvxY5shebwjKDtYHYAx0938z60Dl8rL2+71PjeZH9d9zv9sJUn6&#13;\nHh4pQZ7K5obr8ElJ0aOiAu46zAnkUk4pMbxkg/Vv+Qo9xdsrqlH/exZlkpIfu19JNG1GFVfBvTmE&#13;\nCGWkgtSSNXwwvHNcNVaGL0rUtcdavJBJ2lDwwSMSL39vhFcQ9yCQbauXMkji6My/zSPtTtC8HVeY&#13;\nx8t+/i1qURgKw8wR6s2bl1ne8OuMNiSS+L4hRdvCITLVygulqfY37xplzZQtOQIDAQABoyIwIDAe&#13;\nBgNVHREBAf8EFDASgRBISVNfSWRlbnRpdHlfS2V5MA0GCSqGSIb3DQEBBQUAA4IBAQBtCDKw+pdP&#13;\nBQCKXne5+bqZHlQ/S8wVHloxz3jguv/SOCQEwbhHYweY+kMLQutrD5Cf9nebwOnv3TMeDPjeqSk4&#13;\nc32FHMFiCZFq1Aa/6RS6fkfard46k9K60XyP2pBLNnlUwY+qbH6WmSDVQB8gmpUoObFY5/xx4Sgh&#13;\nYkIixs0Q0uLqM1LbQqzsZaClO71trARI7WDPnn77BEZT5liITrINjXHMOSV9yAiK8pic0XJi5Q04&#13;\nBRRjvfJTVMrOdoJ0k2CNbcX9ZSIV6JPdfybY37OwLjteMdrFhKf+qxMtMAz2s2KUOq6wsZpaB1SW&#13;\ntVnbwmISHKn1piRWs+2mhLVCJinB&#13;\n-----END CERTIFICATE-----</saml2:AttributeValue></saml2:Attribute><saml2:Attribute Name=\"AIK_SHA1\"><saml2:AttributeValue xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">735b3ef40531fe73e77f435444b0dbb27a5468ef</saml2:AttributeValue></saml2:Attribute><saml2:Attribute Name=\"Binding_Key_Certificate\"><saml2:AttributeValue xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">-----BEGIN CERTIFICATE-----&#13;\nMIIERzCCAy+gAwIBAgIIF21h6SIx7hcwDQYJKoZIhvcNAQELBQAwGzEZMBcGA1UEAxMQbXR3aWxz&#13;\nb24tcGNhLWFpazAeFw0xNjA2MTQxMjI2MzhaFw0yNjA2MTIxMjI2MzhaMCUxIzAhBgNVBAMMGkNO&#13;\nPUJpbmRpbmdfS2V5X0NlcnRpZmljYXRlMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA&#13;\nu2yZSsaFFhnLu7+IYYLmKHJCktqQEMFaVs9Ffexq5zdX+SrGNgrPOs+J2sBoLW8JLgDUA6/Ip/8I&#13;\nhU/sUE7SHMQe0f/HbwUa28DNtWZyhjX+FQP0ilBjm/JvxyTsLvqcwZ6lnGpoL3wwR7kzDa0XcQ7b&#13;\nxoydEvBfSJFnuOKJx6lkwO6wu6zqbKZWFuP5BfmkmLol1+cjbF8/hHzoFSFxy82fufsfwO9rs6vc&#13;\nUU2h9u9UJh3NQcs7pVzFnrIyw9RCoU3wrEYn680QLIpprkJ4d/uw8QQOvgJOd6pjP+pnrN+pHoMG&#13;\n5FawA2i7Xn+s8tIhuY3bnSRG4LVN2RKEKTItmQIDAQABo4IBgzCCAX8wDgYDVR0PAQH/BAQDAgUg&#13;\nMFsGB1UEgQUDAikEUAEBAAAAFAAAAAQBAAAAAQADAAEAAAAMAAAIAAAAAAIAAAAA932m6shHz7HJ&#13;\nNNm6VSQEfeV1DkA1Fmjj+Y6sNspSGC9uQADXGRx1MQAAAAAAMIIBDgYIVQSBBQMCKQEEggEAdKnN&#13;\nplyPC2fUoxz+BztvQojA6kQ8WUmvKUmOzukuvw7ueZzV65UwCtC7mdhieNhrhM6ZMTQyuombmHmy&#13;\nyPUHIr0ImYPVyU94RPaByB1o38PVDu7dOeXildj/PzqQdXOCyMAAQ8rS5Abmayiu66JCKL/99hmi&#13;\nxT3gqZ5L2LYixg4562zu47cNG4atvSkTQ0aJJMA51sRReSQSdwHlhXcTxVUMRzRV6Oh0bqnpH1eo&#13;\n7XSDV8ikD38pxV0SKB+kMh6ye7it6Z8kcMLA/SsrmAKxYB+wduTJiWQr0aO41RrkdZ+Ny3OiwD5K&#13;\n2Q/6+/FL+CyPrrEUP+1ors/5iUQTlv6SYDANBgkqhkiG9w0BAQsFAAOCAQEAF31cBQN5prNiYZBw&#13;\nPuOybOg4mCG7vzK8Y6dzyS8OIObrFBBXVTqPpwog+hNTBjK+Lpl6HPIpjW9gbHOosta5XRaoJkrE&#13;\n1eEPlmvMSnm6QTTDEyaRmZSyi7rQEZQZygISSCFAeQBMCGZlZsK395Mje+se1SeBOmgaHDDWQ8Hk&#13;\nYZ16AB6+4OQ5AEow+cHqOwwvBHWicJmT1voQiQX4olVdZiJuF2MMjDJMN/LYykEr7VWbuFjd29st&#13;\n/37GUuv/nnovjO05r2shA+CAbTquxI7KhVFHYK5CeWktkgPqufDpeDNysvvkkWgY9tQO1gN9ba/j&#13;\nCV3T6gZn8U3HGgBXPgnZrQ==&#13;\n-----END CERTIFICATE-----</saml2:AttributeValue></saml2:Attribute></saml2:AttributeStatement></saml2:Assertion>",
     *     			"created_date": 1467018204589,
     *     			"modified_date": 1467021474311,
     *     			"deleted": false
     *   			}
     *                    
     *     When the host id is not in proper format:
     *     Output:
     *     {
     *     "error_code": "602",
     *     "error_message": "Invalid ID",
     *     "detail_errors": "Host Id is not in UUID format."
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
     *            ID of the host
     * @return the host information
     */
    @GET
    @Path("/{id:[0-9a-zA-z_-]+}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getHost(@PathParam("id") String id) {
	// Validate the ID
	if (!ValidationUtil.isValidWithRegex(id, RegexPatterns.UUID)) {
	    ErrorResponse errorResponse = new ErrorResponse(ErrorCode.INAVLID_ID);
	    errorResponse.detailErrors = "Host Id is not in UUID format";
	    return Response.status(Response.Status.BAD_REQUEST).entity(errorResponse).build();
	}
	AttestationHubService attestationHubService = AttestationHubServiceImpl.getInstance();
	AhHost host;
	try {
	    // Fetch the host
	    host = attestationHubService.getHostById(id);
	} catch (AttestationHubException e) {
	    log.error("Error in getting host info");
	    ErrorResponse errorResponse = new ErrorResponse(ErrorCode.REQUEST_PROCESSING_FAILED);
	    errorResponse.detailErrors = e.getMessage();
	    Status status = Response.Status.INTERNAL_SERVER_ERROR;
	    // This error code is if the service throws a non existent entity
	    // exception
	    if (e.getCause() instanceof NonexistentEntityException) {
		status = Response.Status.NOT_FOUND;
	    }
	    return Response.status(status).entity(errorResponse).build();
	}
	return Response.ok(host).build();
    }

    public Response getHosts() {
	AttestationHubService attestationHubService = AttestationHubServiceImpl.getInstance();
	List<AhHost> hosts = null;
	try {
	    hosts = attestationHubService.getHosts();
	} catch (AttestationHubException e) {
	    log.error("Error in getting all hosts info");
	    ErrorResponse errorResponse = new ErrorResponse(ErrorCode.REQUEST_PROCESSING_FAILED);
	    errorResponse.detailErrors = e.getMessage();
	    Status status = Response.Status.INTERNAL_SERVER_ERROR;
	    return Response.status(status).entity(errorResponse).build();
	}
	return Response.ok(hosts).build();
    }

    /**
     * Retrieve the host information by providing the host name in the URL
     * Returns all hosts information if the search string is not provided
     * 
     * @mtwContentTypeReturned JSON
     * @mtwMethodType GET
     * @mtwSampleRestCall
     * 
     *                    <pre>
     *                    https://{IP/HOST_NAME}/v1/hosts?nameEqualTo=10.35.35.172
     *                    OR
     *                    https://{IP/HOST_NAME}/v1/hosts
     *                    
     *                    Output: {
     *     			"id": "97a65f4e-62ed-479b-9e4e-efa143ac5d5e",
     *     			"hardware_uuid": "dfc89080-34d4-11e1-a06f-b8aeed711ffc",
     *     			"host_name": "10.35.35.172",
     *     			"bios_mle_uuid": "8515c831-df15-4e3a-9331-b0eabbd3c3eb",
     *     			"aik_certificate": "-----BEGIN CERTIFICATE-----\r\nMIICvTCCAaWgAwIBAgIGAVVO4KrIMA0GCSqGSIb3DQEBBQUAMBsxGTAXBgNVBAMTEG10d2lsc29u\r\nLXBjYS1haWswHhcNMTYwNjE0MTIyNjM0WhcNMjYwNjE0MTIyNjM0WjAAMIIBIjANBgkqhkiG9w0B\r\nAQEFAAOCAQ8AMIIBCgKCAQEA2MbDubCo8QxbOZthGuJLyHap8o3zcrvMVWR1pl9hWwCE4MVJ9Wrw\r\nuHJB+v+MjW4jXApVQVNuGD0r1hvxY5shebwjKDtYHYAx0938z60Dl8rL2+71PjeZH9d9zv9sJUn6\r\nHh4pQZ7K5obr8ElJ0aOiAu46zAnkUk4pMbxkg/Vv+Qo9xdsrqlH/exZlkpIfu19JNG1GFVfBvTmE\r\nCGWkgtSSNXwwvHNcNVaGL0rUtcdavJBJ2lDwwSMSL39vhFcQ9yCQbauXMkji6My/zSPtTtC8HVeY\r\nx8t+/i1qURgKw8wR6s2bl1ne8OuMNiSS+L4hRdvCITLVygulqfY37xplzZQtOQIDAQABoyIwIDAe\r\nBgNVHREBAf8EFDASgRBISVNfSWRlbnRpdHlfS2V5MA0GCSqGSIb3DQEBBQUAA4IBAQBtCDKw+pdP\r\nBQCKXne5+bqZHlQ/S8wVHloxz3jguv/SOCQEwbhHYweY+kMLQutrD5Cf9nebwOnv3TMeDPjeqSk4\r\nc32FHMFiCZFq1Aa/6RS6fkfard46k9K60XyP2pBLNnlUwY+qbH6WmSDVQB8gmpUoObFY5/xx4Sgh\r\nYkIixs0Q0uLqM1LbQqzsZaClO71trARI7WDPnn77BEZT5liITrINjXHMOSV9yAiK8pic0XJi5Q04\r\nBRRjvfJTVMrOdoJ0k2CNbcX9ZSIV6JPdfybY37OwLjteMdrFhKf+qxMtMAz2s2KUOq6wsZpaB1SW\r\ntVnbwmISHKn1piRWs+2mhLVCJinB\r\n-----END CERTIFICATE-----\r\n",
     *     			"aik_sha1": "735b3ef40531fe73e77f435444b0dbb27a5468ef",
     *     			"connection_url": "intel:https://10.35.35.172:1443/;admin;cee9195fea1f5f23889127886e0afaedf2f96d8c25b1592d041375021dc92a0f",
     *     			"saml_report": "<?xml version=\"1.0\" encoding=\"UTF-8\"?><saml2:Assertion xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\" ID=\"HostTrustAssertion\" IssueInstant=\"2016-06-27T09:30:44.219Z\" Version=\"2.0\"><saml2:Issuer>https://10.35.35.175:8443</saml2:Issuer><Signature xmlns=\"http://www.w3.org/2000/09/xmldsig#\"><SignedInfo><CanonicalizationMethod Algorithm=\"http://www.w3.org/TR/2001/REC-xml-c14n-20010315#WithComments\"/><SignatureMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#rsa-sha1\"/><Reference URI=\"#HostTrustAssertion\"><Transforms><Transform Algorithm=\"http://www.w3.org/2000/09/xmldsig#enveloped-signature\"/></Transforms><DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\"/><DigestValue>0bJH2UPkh67vRI1Tvp5kyiIjDqE=</DigestValue></Reference></SignedInfo><SignatureValue>VLcfy12/gbBbSUIOh8577ZcGyfJPk+4iADr24XXf7ImstX97AqmNEgv5POQ33iRhbr3JfCnMN+Fx\nv3hZqG9Hee4EICa1xFTz5cv3diz7pS1NHs5DeFeJ5OauGSSNWR2G5U02a56NDAqB01P4VH0WHW/Z\nv0KoGnoT85ABttzPiO/S3sJV7HqsOZxZyqGMGBFbnrPJRWOQuM27iJUXiLYze5qj7Qnu3NwYT80B\nWO6RkI9PaNMlAnESeUcKMpfvQyix1RKclRQUICwZX1rKDyQQyXzE3f7Xe13IzSHyY+7ZuhBHoW3u\nGk2KzjiTD1aqPoTBgE5gRqVmZv9LBbGAQaFYPg==</SignatureValue><KeyInfo><X509Data><X509Certificate>MIIDYzCCAkugAwIBAgIECdRf9DANBgkqhkiG9w0BAQsFADBiMQswCQYDVQQGEwJVUzELMAkGA1UE\nCBMCQ0ExDzANBgNVBAcTBkZvbHNvbTEOMAwGA1UEChMFSW50ZWwxEjAQBgNVBAsTCU10IFdpbHNv\nbjERMA8GA1UEAxMIbXR3aWxzb24wHhcNMTYwNTE4MTMzMjUyWhcNMjYwNTE2MTMzMjUyWjBiMQsw\nCQYDVQQGEwJVUzELMAkGA1UECBMCQ0ExDzANBgNVBAcTBkZvbHNvbTEOMAwGA1UEChMFSW50ZWwx\nEjAQBgNVBAsTCU10IFdpbHNvbjERMA8GA1UEAxMIbXR3aWxzb24wggEiMA0GCSqGSIb3DQEBAQUA\nA4IBDwAwggEKAoIBAQCH3hHsqkcK17S0to9DmlhEGZ1EH7fguoBAkhRpAcN+5vazZjMLXixDEo/n\noppG5059X9KlR3aUvh5kzHzgTqRxpSWCbKg3xwZ7ZNvSGkpt5JTl7pTjlpN1W5nzpgO+7AfAUaxn\nexDkHCMV/Xo41cLDkcn5x+7GeT+yHB/71v4zMIHyhKTSLXYLMqR4OM8dyyGCM0H+cOuHoAgMUmdG\n36Uk1Emp+t8W8FpWOj7taW0OFh1Jmk8HRYmYxRZNqXI2eaak49ACT2h0yRUjLuHOOjyhytCf5RWq\nRTbKwp4t++WM5bT3y7hBGVFHCIYUX0k8qnHhym0alL51Eei+x5cT9ZrZAgMBAAGjITAfMB0GA1Ud\nDgQWBBRN6R0YrO5te6qgGU3ELHxvqJd3jzANBgkqhkiG9w0BAQsFAAOCAQEAa265hZetcTd7yxEE\nJiWvZGCqigitFy4jijP3QIbEFM7i5zr2ewQ7o4lf/shQw6ZcZAw49cOZmT6tuVGvSVcbJfk8Pk2H\nzB1SW6IpdRhZSaZY4oIMsS3OscAzZTHAme6Fgxv8QNSZ+sijDOxAskOoXsPcKzS73QF1/yVG52OR\npnjQ1/xjRb1vJXTYXxEbB/cSwzWjGB9JIgJRvNaJjX+WbVjipMTam66ZHz5P0uCO9iFI9Wo2bVqM\nppb4MyJbkQhpHVzofNeiio2EAFAKYBXjwALhy0qxvt/naj12CxaEEC9ywGCOE16oNd9yVHp9e1Gc\nIIavtxI5hBOkOV49st2+dg==</X509Certificate></X509Data></KeyInfo></Signature><saml2:Subject><saml2:NameID Format=\"urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified\">10.35.35.172</saml2:NameID><saml2:SubjectConfirmation Method=\"urn:oasis:names:tc:SAML:2.0:cm:sender-vouches\"><saml2:NameID Format=\"urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified\">Cloud Integrity Technology</saml2:NameID><saml2:SubjectConfirmationData Address=\"10.35.35.175\" NotBefore=\"2016-06-27T09:30:44.219Z\" NotOnOrAfter=\"2016-06-27T10:30:44.219Z\"/></saml2:SubjectConfirmation></saml2:Subject><saml2:AttributeStatement><saml2:Attribute Name=\"Host_Name\"><saml2:AttributeValue xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">10.35.35.172</saml2:AttributeValue></saml2:Attribute><saml2:Attribute Name=\"Host_Address\"><saml2:AttributeValue xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">10.35.35.172</saml2:AttributeValue></saml2:Attribute><saml2:Attribute Name=\"Trusted\"><saml2:AttributeValue xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:anyType\">true</saml2:AttributeValue></saml2:Attribute><saml2:Attribute Name=\"Trusted_BIOS\"><saml2:AttributeValue xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:anyType\">true</saml2:AttributeValue></saml2:Attribute><saml2:Attribute Name=\"BIOS_Name\"><saml2:AttributeValue xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">10.35.35.172_Intel_Corp.</saml2:AttributeValue></saml2:Attribute><saml2:Attribute Name=\"BIOS_Version\"><saml2:AttributeValue xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">0032.2014.0303</saml2:AttributeValue></saml2:Attribute><saml2:Attribute Name=\"BIOS_OEM\"><saml2:AttributeValue xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">Intel Corp.</saml2:AttributeValue></saml2:Attribute><saml2:Attribute Name=\"Trusted_VMM\"><saml2:AttributeValue xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:anyType\">true</saml2:AttributeValue></saml2:Attribute><saml2:Attribute Name=\"VMM_Name\"><saml2:AttributeValue xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">10.35.35.172_Romley_Docker</saml2:AttributeValue></saml2:Attribute><saml2:Attribute Name=\"VMM_Version\"><saml2:AttributeValue xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">14.04-1.11.2</saml2:AttributeValue></saml2:Attribute><saml2:Attribute Name=\"VMM_OSName\"><saml2:AttributeValue xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">Ubuntu</saml2:AttributeValue></saml2:Attribute><saml2:Attribute Name=\"VMM_OSVersion\"><saml2:AttributeValue xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">14.04</saml2:AttributeValue></saml2:Attribute><saml2:Attribute Name=\"AIK_Certificate\"><saml2:AttributeValue xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">-----BEGIN CERTIFICATE-----&#13;\nMIICvTCCAaWgAwIBAgIGAVVO4KrIMA0GCSqGSIb3DQEBBQUAMBsxGTAXBgNVBAMTEG10d2lsc29u&#13;\nLXBjYS1haWswHhcNMTYwNjE0MTIyNjM0WhcNMjYwNjE0MTIyNjM0WjAAMIIBIjANBgkqhkiG9w0B&#13;\nAQEFAAOCAQ8AMIIBCgKCAQEA2MbDubCo8QxbOZthGuJLyHap8o3zcrvMVWR1pl9hWwCE4MVJ9Wrw&#13;\nuHJB+v+MjW4jXApVQVNuGD0r1hvxY5shebwjKDtYHYAx0938z60Dl8rL2+71PjeZH9d9zv9sJUn6&#13;\nHh4pQZ7K5obr8ElJ0aOiAu46zAnkUk4pMbxkg/Vv+Qo9xdsrqlH/exZlkpIfu19JNG1GFVfBvTmE&#13;\nCGWkgtSSNXwwvHNcNVaGL0rUtcdavJBJ2lDwwSMSL39vhFcQ9yCQbauXMkji6My/zSPtTtC8HVeY&#13;\nx8t+/i1qURgKw8wR6s2bl1ne8OuMNiSS+L4hRdvCITLVygulqfY37xplzZQtOQIDAQABoyIwIDAe&#13;\nBgNVHREBAf8EFDASgRBISVNfSWRlbnRpdHlfS2V5MA0GCSqGSIb3DQEBBQUAA4IBAQBtCDKw+pdP&#13;\nBQCKXne5+bqZHlQ/S8wVHloxz3jguv/SOCQEwbhHYweY+kMLQutrD5Cf9nebwOnv3TMeDPjeqSk4&#13;\nc32FHMFiCZFq1Aa/6RS6fkfard46k9K60XyP2pBLNnlUwY+qbH6WmSDVQB8gmpUoObFY5/xx4Sgh&#13;\nYkIixs0Q0uLqM1LbQqzsZaClO71trARI7WDPnn77BEZT5liITrINjXHMOSV9yAiK8pic0XJi5Q04&#13;\nBRRjvfJTVMrOdoJ0k2CNbcX9ZSIV6JPdfybY37OwLjteMdrFhKf+qxMtMAz2s2KUOq6wsZpaB1SW&#13;\ntVnbwmISHKn1piRWs+2mhLVCJinB&#13;\n-----END CERTIFICATE-----</saml2:AttributeValue></saml2:Attribute><saml2:Attribute Name=\"AIK_SHA1\"><saml2:AttributeValue xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">735b3ef40531fe73e77f435444b0dbb27a5468ef</saml2:AttributeValue></saml2:Attribute><saml2:Attribute Name=\"Binding_Key_Certificate\"><saml2:AttributeValue xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">-----BEGIN CERTIFICATE-----&#13;\nMIIERzCCAy+gAwIBAgIIF21h6SIx7hcwDQYJKoZIhvcNAQELBQAwGzEZMBcGA1UEAxMQbXR3aWxz&#13;\nb24tcGNhLWFpazAeFw0xNjA2MTQxMjI2MzhaFw0yNjA2MTIxMjI2MzhaMCUxIzAhBgNVBAMMGkNO&#13;\nPUJpbmRpbmdfS2V5X0NlcnRpZmljYXRlMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA&#13;\nu2yZSsaFFhnLu7+IYYLmKHJCktqQEMFaVs9Ffexq5zdX+SrGNgrPOs+J2sBoLW8JLgDUA6/Ip/8I&#13;\nhU/sUE7SHMQe0f/HbwUa28DNtWZyhjX+FQP0ilBjm/JvxyTsLvqcwZ6lnGpoL3wwR7kzDa0XcQ7b&#13;\nxoydEvBfSJFnuOKJx6lkwO6wu6zqbKZWFuP5BfmkmLol1+cjbF8/hHzoFSFxy82fufsfwO9rs6vc&#13;\nUU2h9u9UJh3NQcs7pVzFnrIyw9RCoU3wrEYn680QLIpprkJ4d/uw8QQOvgJOd6pjP+pnrN+pHoMG&#13;\n5FawA2i7Xn+s8tIhuY3bnSRG4LVN2RKEKTItmQIDAQABo4IBgzCCAX8wDgYDVR0PAQH/BAQDAgUg&#13;\nMFsGB1UEgQUDAikEUAEBAAAAFAAAAAQBAAAAAQADAAEAAAAMAAAIAAAAAAIAAAAA932m6shHz7HJ&#13;\nNNm6VSQEfeV1DkA1Fmjj+Y6sNspSGC9uQADXGRx1MQAAAAAAMIIBDgYIVQSBBQMCKQEEggEAdKnN&#13;\nplyPC2fUoxz+BztvQojA6kQ8WUmvKUmOzukuvw7ueZzV65UwCtC7mdhieNhrhM6ZMTQyuombmHmy&#13;\nyPUHIr0ImYPVyU94RPaByB1o38PVDu7dOeXildj/PzqQdXOCyMAAQ8rS5Abmayiu66JCKL/99hmi&#13;\nxT3gqZ5L2LYixg4562zu47cNG4atvSkTQ0aJJMA51sRReSQSdwHlhXcTxVUMRzRV6Oh0bqnpH1eo&#13;\n7XSDV8ikD38pxV0SKB+kMh6ye7it6Z8kcMLA/SsrmAKxYB+wduTJiWQr0aO41RrkdZ+Ny3OiwD5K&#13;\n2Q/6+/FL+CyPrrEUP+1ors/5iUQTlv6SYDANBgkqhkiG9w0BAQsFAAOCAQEAF31cBQN5prNiYZBw&#13;\nPuOybOg4mCG7vzK8Y6dzyS8OIObrFBBXVTqPpwog+hNTBjK+Lpl6HPIpjW9gbHOosta5XRaoJkrE&#13;\n1eEPlmvMSnm6QTTDEyaRmZSyi7rQEZQZygISSCFAeQBMCGZlZsK395Mje+se1SeBOmgaHDDWQ8Hk&#13;\nYZ16AB6+4OQ5AEow+cHqOwwvBHWicJmT1voQiQX4olVdZiJuF2MMjDJMN/LYykEr7VWbuFjd29st&#13;\n/37GUuv/nnovjO05r2shA+CAbTquxI7KhVFHYK5CeWktkgPqufDpeDNysvvkkWgY9tQO1gN9ba/j&#13;\nCV3T6gZn8U3HGgBXPgnZrQ==&#13;\n-----END CERTIFICATE-----</saml2:AttributeValue></saml2:Attribute></saml2:AttributeStatement></saml2:Assertion>",
     *     			"created_date": 1467018204589,
     *     			"modified_date": 1467021474311,
     *     			"deleted": false
     *   			}
     *   
     *   		https://{IP/HOST_NAME}/v1/hosts 
     *                  would return all the hosts in the hub
     *  
     *     When the host name is not provided:
     *     Output:
     *     {
     *     "error_code": "600",
     *     "error_message": "Validation failed",
     *     "detail_errors": "Name of the host to be searched cannot be blank"
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
     * @param hostFilterCriteria
     *            The pojo representation of the host filter criteria
     * @return the hosts information which satisfy the given criteria
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchHosts(@BeanParam HostFilterCriteria hostFilterCriteria,
	    @Context HttpServletRequest httpServletRequest) {
	log.info("searching for hosts with name : {}", hostFilterCriteria.nameEqualTo);
	AttestationHubService attestationHubService = AttestationHubServiceImpl.getInstance();	
	if (StringUtils.isBlank(httpServletRequest.getQueryString())) {
	    return getHosts();
	}
	
	String validate = hostFilterCriteria.validate();
	if (StringUtils.isNotBlank(validate)) {
	    log.error("Invalid Filter criteria for host {}", hostFilterCriteria.nameEqualTo);
	    ErrorResponse errorResponse = new ErrorResponse(ErrorCode.VALIDATION_FAILED);
	    errorResponse.detailErrors = validate;
	    Status status = Response.Status.BAD_REQUEST;
	    return Response.status(status).entity(errorResponse).build();

	}
	List<AhHost> ahHosts = null;
	try {
	    ahHosts = attestationHubService.searchHostsWithSearchCriteria(hostFilterCriteria);
	} catch (AttestationHubException e) {
	    log.error("Error in searching host with given filter criteria");
	    ErrorResponse errorResponse = new ErrorResponse(ErrorCode.REQUEST_PROCESSING_FAILED);
	    errorResponse.detailErrors = e.getMessage();
	    return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(errorResponse).build();
	}
	return Response.ok(ahHosts).build();
    }
}
