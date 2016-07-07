package com.intel.attestationhub.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(value = Include.NON_NULL)
public class ErrorResponse {
    public String errorCode;
    public String errorMessage;
    public String detailErrors;

    public ErrorResponse() {
	super();
    }

    public ErrorResponse(String errorCode, String errorMessage) {
	super();
	this.errorCode = errorCode;
	this.errorMessage = errorMessage;
    }

    public ErrorResponse(ErrorCode errorCode) {
	this(errorCode.getErrorCode(), errorCode.getErrorDescription());
    }

}
