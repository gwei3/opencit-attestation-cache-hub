/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.intel.mtwilson.attestationhub.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import javax.persistence.Basic;
import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.eclipse.persistence.annotations.UuidGenerator;

/**
 *
 * @author gs-0681
 */
@Entity
@Table(name = "ah_tenant")
@Cacheable(false)

@XmlRootElement
@NamedQueries({ @NamedQuery(name = "AhTenant.findAll", query = "SELECT a FROM AhTenant a"),
	@NamedQuery(name = "AhTenant.findById", query = "SELECT a FROM AhTenant a WHERE a.id = :id"),
	@NamedQuery(name = "AhTenant.findByTenantName", query = "SELECT a FROM AhTenant a WHERE a.tenantName = :tenantName"),
	@NamedQuery(name = "AhTenant.findByTenantNameSearchCriteria", query = "SELECT a FROM AhTenant a WHERE a.tenantName = :tenantName"),
	@NamedQuery(name = "AhTenant.findByTenantKey", query = "SELECT a FROM AhTenant a WHERE a.tenantKey = :tenantKey"),
	@NamedQuery(name = "AhTenant.findByConfig", query = "SELECT a FROM AhTenant a WHERE a.config = :config"),
	@NamedQuery(name = "AhTenant.findByCreatedDate", query = "SELECT a FROM AhTenant a WHERE a.createdDate = :createdDate"),
	@NamedQuery(name = "AhTenant.findByCreatedBy", query = "SELECT a FROM AhTenant a WHERE a.createdBy = :createdBy"),
	@NamedQuery(name = "AhTenant.findByModifiedDate", query = "SELECT a FROM AhTenant a WHERE a.modifiedDate = :modifiedDate"),
	@NamedQuery(name = "AhTenant.findByModifiedBy", query = "SELECT a FROM AhTenant a WHERE a.modifiedBy = :modifiedBy"),
	@NamedQuery(name = "AhTenant.findByDeleted", query = "SELECT a FROM AhTenant a WHERE a.deleted = :deleted") })
public class AhTenant implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @UuidGenerator(name = "UUID")
    @GeneratedValue(generator = "UUID")
    private String id;
    @Column(name = "tenant_name")
    private String tenantName;
    @Column(name = "tenant_key")
    private String tenantKey;
    private String config;
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
    @OneToMany(mappedBy = "tenant")
    private Collection<AhMapping> ahMappingCollection;

    public AhTenant() {
	ahMappingCollection = new ArrayList<AhMapping>();
	deleted = false;
	createdDate = new Date();
	modifiedDate = new Date();

    }

    public AhTenant(String id) {
	this.id = id;
    }

    public String getId() {
	return id;
    }

    public void setId(String id) {
	this.id = id;
    }

    public String getTenantName() {
	return tenantName;
    }

    public void setTenantName(String tenantName) {
	this.tenantName = tenantName;
    }

    public String getTenantKey() {
	return tenantKey;
    }

    public void setTenantKey(String tenantKey) {
	this.tenantKey = tenantKey;
    }

    public String getConfig() {
	return config;
    }

    public void setConfig(String config) {
	this.config = config;
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

    @XmlTransient
    public Collection<AhMapping> getAhMappingCollection() {
	return ahMappingCollection;
    }

    public void setAhMappingCollection(Collection<AhMapping> ahMappingCollection) {
	this.ahMappingCollection = ahMappingCollection;
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
	if (!(object instanceof AhTenant)) {
	    return false;
	}
	AhTenant other = (AhTenant) object;
	if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
	    return false;
	}
	return true;
    }

    @Override
    public String toString() {
	return "com.intel.mtwilson.attestationhub.data.AhTenant[ id=" + id + " ]";
    }

}
