CREATE TABLE AH_TENANT (ID VARCHAR(36) NOT NULL, TENANT_NAME VARCHAR(255), TENANT_KEY VARCHAR(255), CONFIG TEXT, CREATED_DATE TIMESTAMP, CREATED_BY VARCHAR(255), MODIFIED_DATE TIMESTAMP, MODIFIED_BY VARCHAR(255), DELETED BOOLEAN, PRIMARY KEY (ID));
CREATE TABLE AH_HOST (ID VARCHAR(36) NOT NULL, HARDWARE_UUID VARCHAR(36), HOST_NAME VARCHAR(255), BIOS_MLE_UUID VARCHAR(36), VMM_MLE_UUID VARCHAR(36), AIK_CERTIFICATE TEXT, AIK_SHA1  VARCHAR(200), CONNECTION_URL  VARCHAR(200), TRUST_TAGS_JSON VARCHAR(200), VALID_TO VARCHAR(200), SAML_REPORT TEXT, TRUSTED BOOLEAN, ASSET_TAGS TEXT, CREATED_DATE TIMESTAMP, CREATED_BY VARCHAR(255), MODIFIED_DATE TIMESTAMP, MODIFIED_BY VARCHAR(255), DELETED BOOLEAN, PRIMARY KEY (ID));
CREATE TABLE AH_MAPPING (ID VARCHAR(36) NOT NULL, HOST_HARDWARE_UUID VARCHAR(36) NOT NULL, TENANT_UUID VARCHAR(36) REFERENCES AH_TENANT(ID), CREATED_DATE TIMESTAMP, CREATED_BY VARCHAR(255), MODIFIED_DATE TIMESTAMP, MODIFIED_BY VARCHAR(255), DELETED BOOLEAN, PRIMARY KEY (ID));