/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.intel.mtwilson.attestationhub.controller;

import java.io.Serializable;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityNotFoundException;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import com.intel.mtwilson.attestationhub.controller.exceptions.NonexistentEntityException;
import com.intel.mtwilson.attestationhub.controller.exceptions.PreexistingEntityException;
import com.intel.mtwilson.attestationhub.data.AhMapping;
import com.intel.mtwilson.attestationhub.data.AhTenant;

/**
 *
 * @author GS-0681
 */
public class AhMappingJpaController implements Serializable {

    public AhMappingJpaController(EntityManagerFactory emf) {
	this.emf = emf;
    }

    private EntityManagerFactory emf = null;

    public EntityManager getEntityManager() {
	return emf.createEntityManager();
    }

    public void create(AhMapping ahMapping) throws PreexistingEntityException, Exception {
	EntityManager em = null;
	try {
	    em = getEntityManager();
	    em.getTransaction().begin();
	    AhTenant tenantUuid = ahMapping.getTenant();
	    if (tenantUuid != null) {
		tenantUuid = em.getReference(tenantUuid.getClass(), tenantUuid.getId());
		ahMapping.setTenant(tenantUuid);
	    }
	    em.persist(ahMapping);
	    if (tenantUuid != null) {
		tenantUuid.getAhMappingCollection().add(ahMapping);
		em.merge(tenantUuid);
	    }
	    em.getTransaction().commit();
	} catch (Exception ex) {
	    if (findAhMapping(ahMapping.getId()) != null) {
		throw new PreexistingEntityException("AhMapping " + ahMapping + " already exists.", ex);
	    }
	    throw ex;
	} finally {
	    if (em != null) {
		em.close();
	    }
	}
    }

    public void edit(AhMapping ahMapping) throws NonexistentEntityException, Exception {
	EntityManager em = null;
	try {
	    em = getEntityManager();
	    em.getTransaction().begin();
	    AhMapping persistentAhMapping = em.find(AhMapping.class, ahMapping.getId());
	    AhTenant tenantUuidOld = persistentAhMapping.getTenant();
	    AhTenant tenantUuidNew = ahMapping.getTenant();
	    if (tenantUuidNew != null) {
		tenantUuidNew = em.getReference(tenantUuidNew.getClass(), tenantUuidNew.getId());
		ahMapping.setTenant(tenantUuidNew);
	    }
	    ahMapping = em.merge(ahMapping);
	    if (tenantUuidOld != null && !tenantUuidOld.equals(tenantUuidNew)) {
		tenantUuidOld.getAhMappingCollection().remove(ahMapping);
		tenantUuidOld = em.merge(tenantUuidOld);
	    }
	    if (tenantUuidNew != null && !tenantUuidNew.equals(tenantUuidOld)) {
		tenantUuidNew.getAhMappingCollection().add(ahMapping);
		tenantUuidNew = em.merge(tenantUuidNew);
	    }
	    em.getTransaction().commit();
	} catch (Exception ex) {
	    String msg = ex.getLocalizedMessage();
	    if (msg == null || msg.length() == 0) {
		String id = ahMapping.getId();
		if (findAhMapping(id) == null) {
		    throw new NonexistentEntityException("The ahMapping with id " + id + " no longer exists.");
		}
	    }
	    throw ex;
	} finally {
	    if (em != null) {
		em.close();
	    }
	}
    }

    public void destroy(String id) throws NonexistentEntityException {
	EntityManager em = null;
	try {
	    em = getEntityManager();
	    em.getTransaction().begin();
	    AhMapping ahMapping;
	    try {
		ahMapping = em.getReference(AhMapping.class, id);
		ahMapping.getId();
	    } catch (EntityNotFoundException enfe) {
		throw new NonexistentEntityException("The ahMapping with id " + id + " no longer exists.", enfe);
	    }
	    AhTenant tenantUuid = ahMapping.getTenant();
	    if (tenantUuid != null) {
		tenantUuid.getAhMappingCollection().remove(ahMapping);
		tenantUuid = em.merge(tenantUuid);
	    }
	    em.remove(ahMapping);
	    em.getTransaction().commit();
	} finally {
	    if (em != null) {
		em.close();
	    }
	}
    }

    public List<AhMapping> findAhMappingEntities() {
	return findAhMappingEntities(true, -1, -1);
    }

    public List<AhMapping> findAhMappingEntities(int maxResults, int firstResult) {
	return findAhMappingEntities(false, maxResults, firstResult);
    }

    private List<AhMapping> findAhMappingEntities(boolean all, int maxResults, int firstResult) {
	EntityManager em = getEntityManager();
	try {
	    CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
	    cq.select(cq.from(AhMapping.class));
	    Query q = em.createQuery(cq);
	    if (!all) {
		q.setMaxResults(maxResults);
		q.setFirstResult(firstResult);
	    }
	    return q.getResultList();
	} finally {
	    em.close();
	}
    }

    public AhMapping findAhMapping(String id) {
	EntityManager em = getEntityManager();
	try {
	    return em.find(AhMapping.class, id);
	} finally {
	    em.close();
	}
    }

    public int getAhMappingCount() {
	EntityManager em = getEntityManager();
	try {
	    CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
	    Root<AhMapping> rt = cq.from(AhMapping.class);
	    cq.select(em.getCriteriaBuilder().count(rt));
	    Query q = em.createQuery(cq);
	    return ((Long) q.getSingleResult()).intValue();
	} finally {
	    em.close();
	}
    }

    public List<AhMapping> findAhMappingsByTenantId(String id) {
	List<AhMapping> mappingsList = null;
	EntityManager em = getEntityManager();
	try {
	    Query query = em.createNamedQuery("AhMapping.findByTenantId");
	    query.setParameter("tenantId", id);
	    if (query.getResultList() != null && !query.getResultList().isEmpty()) {
		mappingsList = query.getResultList();
	    }
	} finally {
	    em.close();
	}
	return mappingsList;
    }

    public List<AhMapping> findAhMappingsByHostHardwareUuid(String id) {
	List<AhMapping> mappingsList = null;
	EntityManager em = getEntityManager();
	try {
	    Query query = em.createNamedQuery("AhMapping.findByHostHardwareUuid");
	    query.setParameter("hostHardwareUuid", id);
	    if (query.getResultList() != null && !query.getResultList().isEmpty()) {
		mappingsList = query.getResultList();
	    }
	} finally {
	    em.close();
	}
	return mappingsList;
    }

}
