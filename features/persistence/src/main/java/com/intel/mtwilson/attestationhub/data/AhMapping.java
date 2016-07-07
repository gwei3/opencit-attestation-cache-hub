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
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.xml.bind.annotation.XmlRootElement;

import org.eclipse.persistence.annotations.UuidGenerator;

/**
 *
 * @author gs-0681
 */
@Entity
@Table(name = "ah_mapping")
@Cacheable(false)
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "AhMapping.findAll", query = "SELECT a FROM AhMapping a"),
    @NamedQuery(name = "AhMapping.findById", query = "SELECT a FROM AhMapping a WHERE a.id = :id"),
    @NamedQuery(name = "AhMapping.findByTenantId", query = "SELECT a FROM AhMapping a WHERE a.tenant.id = :tenantId"),
    @NamedQuery(name = "AhMapping.findByHostId", query = "SELECT a FROM AhMapping a WHERE a.host.id = :hostId"),
    @NamedQuery(name = "AhMapping.findByCreatedDate", query = "SELECT a FROM AhMapping a WHERE a.createdDate = :createdDate"),
    @NamedQuery(name = "AhMapping.findByCreatedBy", query = "SELECT a FROM AhMapping a WHERE a.createdBy = :createdBy"),
    @NamedQuery(name = "AhMapping.findByModifiedDate", query = "SELECT a FROM AhMapping a WHERE a.modifiedDate = :modifiedDate"),
    @NamedQuery(name = "AhMapping.findByModifiedBy", query = "SELECT a FROM AhMapping a WHERE a.modifiedBy = :modifiedBy"),
    @NamedQuery(name = "AhMapping.findByDeleted", query = "SELECT a FROM AhMapping a WHERE a.deleted = :deleted")})
public class AhMapping implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @UuidGenerator(name = "UUID")
    @GeneratedValue(generator = "UUID")
    private String id;
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
    @JoinColumn(name = "host_uuid", referencedColumnName = "id")
    @ManyToOne
    private AhHost host;
    @JoinColumn(name = "tenant_uuid", referencedColumnName = "id")
    @ManyToOne
    private AhTenant tenant;

    public AhMapping() {
	deleted = false;
	createdDate = new Date();
	modifiedDate= new Date();	
    }

    public AhMapping(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public AhHost getHost() {
        return host;
    }

    public void setHost(AhHost hostUuid) {
        this.host = hostUuid;
    }

    public AhTenant getTenant() {
        return tenant;
    }

    public void setTenant(AhTenant tenantUuid) {
        this.tenant = tenantUuid;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof AhMapping)) {
            return false;
        }
        AhMapping other = (AhMapping) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.intel.mtwilson.attestationhub.data.AhMapping[ id=" + id + " ]";
    }
    
}
