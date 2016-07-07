package com.intel.attestationhub.api;

public enum ErrorCode {
    VALIDATION_FAILED("600", "Validation failed"),    
    REQUEST_PROCESSING_FAILED("601", "Request processing failed"),
    INAVLID_ID("602", "Invalid ID");

    private String code;
    private String description;

    private ErrorCode(String code, String description) {
	this.code = code;
	this.description = description;
    }

    public String getErrorDescription() {
	return description;
    }

    public String getErrorCode() {
	return code;
    }
}
