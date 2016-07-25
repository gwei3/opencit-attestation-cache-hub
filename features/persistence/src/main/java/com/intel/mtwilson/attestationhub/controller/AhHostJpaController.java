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
import com.intel.mtwilson.attestationhub.data.AhHost;

/**
 *
 * @author GS-0681
 */
public class AhHostJpaController implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public AhHostJpaController(EntityManagerFactory emf) {
	this.emf = emf;
    }

    private EntityManagerFactory emf = null;

    public EntityManager getEntityManager() {
	return emf.createEntityManager();
    }

    public void create(AhHost ahHost) throws PreexistingEntityException, Exception {
	EntityManager em = null;
	try {
	    em = getEntityManager();
	    em.getTransaction().begin();
	    em.persist(ahHost);
	    em.getTransaction().commit();
	} catch (Exception ex) {
	    if (findAhHost(ahHost.getId()) != null) {
		throw new PreexistingEntityException("AhHost " + ahHost + " already exists.", ex);
	    }
	    throw ex;
	} finally {
	    if (em != null) {
		em.close();
	    }
	}
    }

    public void edit(AhHost ahHost) throws NonexistentEntityException, Exception {
	EntityManager em = null;
	try {
	    em = getEntityManager();
	    em.getTransaction().begin();
	    ahHost = em.merge(ahHost);
	    em.getTransaction().commit();
	} catch (Exception ex) {
	    String msg = ex.getLocalizedMessage();
	    if (msg == null || msg.length() == 0) {
		String id = ahHost.getId();
		if (findAhHost(id) == null) {
		    throw new NonexistentEntityException("The ahHost with id " + id + " no longer exists.");
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
	    AhHost ahHost;
	    try {
		ahHost = em.getReference(AhHost.class, id);
		ahHost.getId();
	    } catch (EntityNotFoundException enfe) {
		throw new NonexistentEntityException("The ahHost with id " + id + " no longer exists.", enfe);
	    }
	    em.remove(ahHost);
	    em.getTransaction().commit();
	} finally {
	    if (em != null) {
		em.close();
	    }
	}
    }

    public List<AhHost> findAhHostEntities() {
	return findAhHostEntities(true, -1, -1);
    }

    public List<AhHost> findAhHostEntities(int maxResults, int firstResult) {
	return findAhHostEntities(false, maxResults, firstResult);
    }

    private List<AhHost> findAhHostEntities(boolean all, int maxResults, int firstResult) {
	EntityManager em = getEntityManager();
	try {
	    CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
	    cq.select(cq.from(AhHost.class));
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

    public AhHost findAhHost(String id) {
	EntityManager em = getEntityManager();
	try {
	    return em.find(AhHost.class, id);
	} finally {
	    em.close();
	}
    }

    public int getAhHostCount() {
	EntityManager em = getEntityManager();
	try {
	    CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
	    Root<AhHost> rt = cq.from(AhHost.class);
	    cq.select(em.getCriteriaBuilder().count(rt));
	    Query q = em.createQuery(cq);
	    return ((Long) q.getSingleResult()).intValue();
	} finally {
	    em.close();
	}
    }

    public List<AhHost> findHostsWithFilterCriteria(String filterCriteria) {
	List<AhHost> hostsList = null;
	EntityManager em = getEntityManager();
	try {
	    Query query = em.createNamedQuery("AhHost.findByHostName");
	    query.setParameter("hostName", filterCriteria.toUpperCase());
	    hostsList = query.getResultList();
	    if (hostsList.isEmpty()) {
		hostsList = null;
	    }
	} finally {
	    em.close();
	}
	return hostsList;
    }

    public List<AhHost> findHostsByHardwareUuid(String hardwareUuid) {
	List<AhHost> hostsList = null;
	EntityManager em = getEntityManager();
	try {
	    Query query = em.createNamedQuery("AhHost.findByHardwareUuid");
	    query.setParameter("hardwareUuid", hardwareUuid);
	    hostsList = query.getResultList();
	    if (hostsList.isEmpty()) {
		hostsList = null;
	    }
	} finally {
	    em.close();
	}
	return hostsList;
    }
}
