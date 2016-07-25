/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.intel.mtwilson.attestationhub.data;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Basic;
import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author gs-0681
 */
@Entity
@Table(name = "ah_host")
@Cacheable(false)

@XmlRootElement
@NamedQueries({ @NamedQuery(name = "AhHost.findAll", query = "SELECT a FROM AhHost a"),
	@NamedQuery(name = "AhHost.findById", query = "SELECT a FROM AhHost a WHERE a.id = :id"),
	@NamedQuery(name = "AhHost.findByHardwareUuid", query = "SELECT a FROM AhHost a WHERE upper(a.hardwareUuid) = :hardwareUuid"),
	@NamedQuery(name = "AhHost.findByHostName", query = "SELECT a FROM AhHost a WHERE upper(a.hostName) = :hostName"),
	@NamedQuery(name = "AhHost.findByBiosMleUuid", query = "SELECT a FROM AhHost a WHERE a.biosMleUuid = :biosMleUuid"),
	@NamedQuery(name = "AhHost.findByVmmMleUuid", query = "SELECT a FROM AhHost a WHERE a.vmmMleUuid = :vmmMleUuid"),
	@NamedQuery(name = "AhHost.findByAikCertificate", query = "SELECT a FROM AhHost a WHERE a.aikCertificate = :aikCertificate"),
	@NamedQuery(name = "AhHost.findByAikSha1", query = "SELECT a FROM AhHost a WHERE a.aikSha1 = :aikSha1"),
	@NamedQuery(name = "AhHost.findByConnectionUrl", query = "SELECT a FROM AhHost a WHERE a.connectionUrl = :connectionUrl"),
	@NamedQuery(name = "AhHost.findByTrustTagsJson", query = "SELECT a FROM AhHost a WHERE a.trustTagsJson = :trustTagsJson"),
	@NamedQuery(name = "AhHost.findBySamlReport", query = "SELECT a FROM AhHost a WHERE a.samlReport = :samlReport"),
	@NamedQuery(name = "AhHost.findByCreatedDate", query = "SELECT a FROM AhHost a WHERE a.createdDate = :createdDate"),
	@NamedQuery(name = "AhHost.findByCreatedBy", query = "SELECT a FROM AhHost a WHERE a.createdBy = :createdBy"),
	@NamedQuery(name = "AhHost.findByModifiedDate", query = "SELECT a FROM AhHost a WHERE a.modifiedDate = :modifiedDate"),
	@NamedQuery(name = "AhHost.findByModifiedBy", query = "SELECT a FROM AhHost a WHERE a.modifiedBy = :modifiedBy"),
	@NamedQuery(name = "AhHost.findByDeleted", query = "SELECT a FROM AhHost a WHERE a.deleted = :deleted") })
public class AhHost implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    private String id;
    @Column(name = "hardware_uuid")
    private String hardwareUuid;
    @Column(name = "host_name")
    private String hostName;
    @Column(name = "bios_mle_uuid")
    private String biosMleUuid;
    @Column(name = "vmm_mle_uuid")
    private String vmmMleUuid;
    @Column(name = "aik_certificate")
    private String aikCertificate;
    @Column(name = "aik_sha1")
    private String aikSha1;
    @Column(name = "connection_url")
    private String connectionUrl;
    @Column(name = "trust_tags_json")
    private String trustTagsJson;
    @Column(name = "valid_to")
    private String validTo;
    @Column(name = "saml_report")
    private String samlReport;
    @Column(name = "created_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdDate;
    @Column(name = "created_by")
    private String createdBy;
    @Column(name = "modified_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date modifiedDate;
    @Column(name = "modified_by")
    private String modifiedBy;    
    private Boolean deleted;
    @Column(name = "trusted")
    private Boolean trusted;

    public AhHost() {
	deleted = false;
	createdDate = new Date();
	modifiedDate = new Date();
    }

    public AhHost(String id) {
	this.id = id;
    }

    public String getId() {
	return id;
    }

    public void setId(String id) {
	this.id = id;
    }

    public String getHardwareUuid() {
	return hardwareUuid;
    }

    public void setHardwareUuid(String hardwareUuid) {
	this.hardwareUuid = hardwareUuid;
    }

    public String getHostName() {
	return hostName;
    }

    public void setHostName(String hostName) {
	this.hostName = hostName;
    }

    public String getBiosMleUuid() {
	return biosMleUuid;
    }

    public void setBiosMleUuid(String biosMleUuid) {
	this.biosMleUuid = biosMleUuid;
    }

    public String getVmmMleUuid() {
	return vmmMleUuid;
    }

    public void setVmmMleUuid(String vmmMleUuid) {
	this.vmmMleUuid = vmmMleUuid;
    }

    public String getAikCertificate() {
	return aikCertificate;
    }

    public void setAikCertificate(String aikCertificate) {
	this.aikCertificate = aikCertificate;
    }

    public String getAikSha1() {
	return aikSha1;
    }

    public void setAikSha1(String aikSha1) {
	this.aikSha1 = aikSha1;
    }

    public String getConnectionUrl() {
	return connectionUrl;
    }

    public void setConnectionUrl(String connectionUrl) {
	this.connectionUrl = connectionUrl;
    }

    public String getTrustTagsJson() {
	return trustTagsJson;
    }

    public void setTrustTagsJson(String trustTagsJson) {
	this.trustTagsJson = trustTagsJson;
    }

    public String getValidTo() {
        return validTo;
    }

    public void setValidTo(String validTo) {
        this.validTo = validTo;
    }

    public String getSamlReport() {
	return samlReport;
    }

    public void setSamlReport(String samlReport) {
	this.samlReport = samlReport;
    }

    public Date getCreatedDate() {
	return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
	this.createdDate = createdDate;
    }

    public String getCreatedBy() {
	return createdBy;
    }

    public void setCreatedBy(String createdBy) {
	this.createdBy = createdBy;
    }

    public Date getModifiedDate() {
	return modifiedDate;
    }

    public void setModifiedDate(Date modifiedDate) {
	this.modifiedDate = modifiedDate;
    }

    public String getModifiedBy() {
	return modifiedBy;
    }

    public void setModifiedBy(String modifiedBy) {
	this.modifiedBy = modifiedBy;
    }

    public Boolean getDeleted() {
	return deleted;
    }

    public void setDeleted(Boolean deleted) {
	this.deleted = deleted;
    }
    
    
    public Boolean getTrusted() {
        return trusted;
    }

    public void setTrusted(Boolean trusted) {
        this.trusted = trusted;
    }

    @Override
    public int hashCode() {
	int hash = 0;
	hash += (id != null ? id.hashCode() : 0);
	return hash;
    }

    @Override
    public boolean equals(Object object) {
	// TODO: Warning - this method won't work in the case the id fields are
	// not set
	if (!(object instanceof AhHost)) {
	    return false;
	}
	AhHost other = (AhHost) object;
	if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
	    return false;
	}
	return true;
    }

    @Override
    public String toString() {
	return "com.intel.mtwilson.attestationhub.data.AhHost[ id=" + id + " ]";
    }

}
