/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.intel.mtwilson.attestationhub.controller;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
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
public class AhTenantJpaController implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public AhTenantJpaController(EntityManagerFactory emf) {
	this.emf = emf;
    }

    private EntityManagerFactory emf = null;

    public EntityManager getEntityManager() {
	return emf.createEntityManager();
    }

    public void create(AhTenant ahTenant) throws PreexistingEntityException, Exception {
	if (ahTenant.getAhMappingCollection() == null) {
	    ahTenant.setAhMappingCollection(new ArrayList<AhMapping>());
	}
	EntityManager em = null;
	try {
	    em = getEntityManager();
	    em.getTransaction().begin();
	    Collection<AhMapping> attachedAhMappingCollection = new ArrayList<AhMapping>();
	    for (AhMapping ahMappingCollectionAhMappingToAttach : ahTenant.getAhMappingCollection()) {
		ahMappingCollectionAhMappingToAttach = em.getReference(ahMappingCollectionAhMappingToAttach.getClass(),
			ahMappingCollectionAhMappingToAttach.getId());
		attachedAhMappingCollection.add(ahMappingCollectionAhMappingToAttach);
	    }
	    ahTenant.setAhMappingCollection(attachedAhMappingCollection);
	    em.persist(ahTenant);
	    for (AhMapping ahMappingCollectionAhMapping : ahTenant.getAhMappingCollection()) {
		AhTenant oldTenantUuidOfAhMappingCollectionAhMapping = ahMappingCollectionAhMapping.getTenant();
		ahMappingCollectionAhMapping.setTenant(ahTenant);
		ahMappingCollectionAhMapping = em.merge(ahMappingCollectionAhMapping);
		if (oldTenantUuidOfAhMappingCollectionAhMapping != null) {
		    oldTenantUuidOfAhMappingCollectionAhMapping.getAhMappingCollection()
			    .remove(ahMappingCollectionAhMapping);
		    em.merge(oldTenantUuidOfAhMappingCollectionAhMapping);
		}
	    }
	    em.getTransaction().commit();
	} catch (Exception ex) {
	    if (findAhTenant(ahTenant.getId()) != null) {
		throw new PreexistingEntityException("AhTenant " + ahTenant + " already exists.", ex);
	    }
	    throw ex;
	} finally {
	    if (em != null) {
		em.close();
	    }
	}
    }

    public void edit(AhTenant ahTenant) throws NonexistentEntityException, Exception {
	EntityManager em = null;
	try {
	    em = getEntityManager();
	    em.getTransaction().begin();
	    em.merge(ahTenant);
	    em.getTransaction().commit();
	} catch (Exception ex) {
	    String msg = ex.getLocalizedMessage();
	    if (msg == null || msg.length() == 0) {
		String id = ahTenant.getId();
		if (findAhTenant(id) == null) {
		    throw new NonexistentEntityException("The ahTenant with id " + id + " no longer exists.");
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
	    AhTenant ahTenant;
	    try {
		ahTenant = em.getReference(AhTenant.class, id);
		ahTenant.getId();
	    } catch (EntityNotFoundException enfe) {
		throw new NonexistentEntityException("The ahTenant with id " + id + " no longer exists.", enfe);
	    }
	    Collection<AhMapping> ahMappingCollection = ahTenant.getAhMappingCollection();
	    for (AhMapping ahMappingCollectionAhMapping : ahMappingCollection) {
		ahMappingCollectionAhMapping.setTenant(null);
		ahMappingCollectionAhMapping = em.merge(ahMappingCollectionAhMapping);
	    }
	    em.remove(ahTenant);
	    em.getTransaction().commit();
	} finally {
	    if (em != null) {
		em.close();
	    }
	}
    }

    public List<AhTenant> findAhTenantEntities() {
	return findAhTenantEntities(true, -1, -1);
    }

    public List<AhTenant> findAhTenantEntities(int maxResults, int firstResult) {
	return findAhTenantEntities(false, maxResults, firstResult);
    }

    private List<AhTenant> findAhTenantEntities(boolean all, int maxResults, int firstResult) {
	EntityManager em = getEntityManager();
	try {
	    CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
	    cq.select(cq.from(AhTenant.class));
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

    public AhTenant findAhTenant(String id) {
	EntityManager em = getEntityManager();
	try {
	    return em.find(AhTenant.class, id);
	} finally {
	    em.close();
	}
    }

    public int getAhTenantCount() {
	EntityManager em = getEntityManager();
	try {
	    CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
	    Root<AhTenant> rt = cq.from(AhTenant.class);
	    cq.select(em.getCriteriaBuilder().count(rt));
	    Query q = em.createQuery(cq);
	    return ((Long) q.getSingleResult()).intValue();
	} finally {
	    em.close();
	}
    }

    public List<AhTenant> findAhTenantsByNameSearchCriteria(String searchCriteria) {
	List<AhTenant> tenantsList = null;
	EntityManager em = getEntityManager();

	try {
	    Query query = em.createNamedQuery("AhTenant.findByTenantNameSearchCriteria");
	    query.setParameter("tenantName", searchCriteria);

	    if (query.getResultList() != null && !query.getResultList().isEmpty()) {
		tenantsList = query.getResultList();
	    }
	} finally {
	    em.close();
	}
	return tenantsList;
    }
}
