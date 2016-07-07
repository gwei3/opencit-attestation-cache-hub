/**
 * 
 */
package com.intel.attestationhub.api;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.QueryParam;

import org.apache.commons.lang.StringUtils;

import com.intel.mtwilson.jaxrs2.DefaultFilterCriteria;
import com.intel.mtwilson.repository.FilterCriteria;

/**
 * @author Vijay Prakash
 *
 */
public class HostFilterCriteria extends DefaultFilterCriteria implements FilterCriteria{
    @QueryParam("nameEqualTo")
    public String nameEqualTo = null;
    
    public String validate(){
	List<String> errors = new ArrayList<>();
	
	if(StringUtils.isBlank(nameEqualTo)){
	    errors.add("Name of the host to be searched cannot be blank");
	}
	return StringUtils.join(errors, ", ");
    }
}
