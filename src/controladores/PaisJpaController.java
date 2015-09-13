/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package controladores;

import controladores.exceptions.NonexistentEntityException;
import java.io.Serializable;
import javax.persistence.Query;
import javax.persistence.EntityNotFoundException;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import modelo.Provincia;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.criteria.CriteriaBuilder;
import modelo.Pais;
import modelo.Pais_;

/**
 *
 * @author hacho
 */
public class PaisJpaController implements Serializable {

    public PaisJpaController(EntityManagerFactory emf) {
        this.emf = emf;
    }
    private EntityManagerFactory emf = null;

    public EntityManager getEntityManager() {
        return emf.createEntityManager();
    }

    public void create(String nombre, String gentilicio, String prefTelefonico) throws Exception {
        validate(nombre, null);
        create(new Pais(nombre, gentilicio, prefTelefonico));
    }

    public void edit(Integer id, String nombre) throws Exception {
        if (findPais(id) == null) {
            throw new Exception("País no encontrado");
        }
        Pais c = findPais(id);
        validate(nombre, c);
        c.setNombre(nombre);
        edit(c);
    }

    public void validate(String nombre, Pais c) throws Exception {
        if (nombre.equals("")) {
            throw new Exception("Ingrese un nombre");
        }
        if (c == null) {
            if (find(nombre) != null) {
                throw new Exception("El país " + nombre + " ya existe");
            }
        } else {
            if (!c.getNombre().equals(nombre) && find(nombre) != null) {
                throw new Exception("El país " + nombre + " ya existe");
            }
        }
    }

    public Pais find(String nombre) {
        EntityManager em = getEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        Pais res = null;
        try {
            CriteriaQuery cq = cb.createQuery();
            Root e = cq.from(Pais.class);
            cq.where(cb.equal(e.get(Pais_.nombre), nombre));
            Query query = em.createQuery(cq);
            List<Pais> aux = query.getResultList();
            res = aux.isEmpty() ? null : (Pais) aux.get(0);
        } finally {
            em.close();
        }
        return res;
    }
    
    public void create(Pais pais) {
        if (pais.getProvincias() == null) {
            pais.setProvincias(new ArrayList<Provincia>());
        }
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            List<Provincia> attachedProvincias = new ArrayList<Provincia>();
            for (Provincia provinciasProvinciaToAttach : pais.getProvincias()) {
                provinciasProvinciaToAttach = em.getReference(provinciasProvinciaToAttach.getClass(), provinciasProvinciaToAttach.getId());
                attachedProvincias.add(provinciasProvinciaToAttach);
            }
            pais.setProvincias(attachedProvincias);
            em.persist(pais);
            for (Provincia provinciasProvincia : pais.getProvincias()) {
                Pais oldUnPaisOfProvinciasProvincia = provinciasProvincia.getUnPais();
                provinciasProvincia.setUnPais(pais);
                provinciasProvincia = em.merge(provinciasProvincia);
                if (oldUnPaisOfProvinciasProvincia != null) {
                    oldUnPaisOfProvinciasProvincia.getProvincias().remove(provinciasProvincia);
                    oldUnPaisOfProvinciasProvincia = em.merge(oldUnPaisOfProvinciasProvincia);
                }
            }
            em.getTransaction().commit();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public void edit(Pais pais) throws NonexistentEntityException, Exception {
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            Pais persistentPais = em.find(Pais.class, pais.getId());
            List<Provincia> provinciasOld = persistentPais.getProvincias();
            List<Provincia> provinciasNew = pais.getProvincias();
            List<Provincia> attachedProvinciasNew = new ArrayList<Provincia>();
            for (Provincia provinciasNewProvinciaToAttach : provinciasNew) {
                provinciasNewProvinciaToAttach = em.getReference(provinciasNewProvinciaToAttach.getClass(), provinciasNewProvinciaToAttach.getId());
                attachedProvinciasNew.add(provinciasNewProvinciaToAttach);
            }
            provinciasNew = attachedProvinciasNew;
            pais.setProvincias(provinciasNew);
            pais = em.merge(pais);
            for (Provincia provinciasOldProvincia : provinciasOld) {
                if (!provinciasNew.contains(provinciasOldProvincia)) {
                    provinciasOldProvincia.setUnPais(null);
                    provinciasOldProvincia = em.merge(provinciasOldProvincia);
                }
            }
            for (Provincia provinciasNewProvincia : provinciasNew) {
                if (!provinciasOld.contains(provinciasNewProvincia)) {
                    Pais oldUnPaisOfProvinciasNewProvincia = provinciasNewProvincia.getUnPais();
                    provinciasNewProvincia.setUnPais(pais);
                    provinciasNewProvincia = em.merge(provinciasNewProvincia);
                    if (oldUnPaisOfProvinciasNewProvincia != null && !oldUnPaisOfProvinciasNewProvincia.equals(pais)) {
                        oldUnPaisOfProvinciasNewProvincia.getProvincias().remove(provinciasNewProvincia);
                        oldUnPaisOfProvinciasNewProvincia = em.merge(oldUnPaisOfProvinciasNewProvincia);
                    }
                }
            }
            em.getTransaction().commit();
        } catch (Exception ex) {
            String msg = ex.getLocalizedMessage();
            if (msg == null || msg.length() == 0) {
                Integer id = pais.getId();
                if (findPais(id) == null) {
                    throw new NonexistentEntityException("The pais with id " + id + " no longer exists.");
                }
            }
            throw ex;
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public void destroy(Integer id) throws NonexistentEntityException {
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            Pais pais;
            try {
                pais = em.getReference(Pais.class, id);
                pais.getId();
            } catch (EntityNotFoundException enfe) {
                throw new NonexistentEntityException("The pais with id " + id + " no longer exists.", enfe);
            }
            List<Provincia> provincias = pais.getProvincias();
            for (Provincia provinciasProvincia : provincias) {
                provinciasProvincia.setUnPais(null);
                provinciasProvincia = em.merge(provinciasProvincia);
            }
            em.remove(pais);
            em.getTransaction().commit();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public List<Pais> findPaisEntities() {
        return findPaisEntities(true, -1, -1);
    }

    public List<Pais> findPaisEntities(int maxResults, int firstResult) {
        return findPaisEntities(false, maxResults, firstResult);
    }

    private List<Pais> findPaisEntities(boolean all, int maxResults, int firstResult) {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            cq.select(cq.from(Pais.class));
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

    public Pais findPais(Integer id) {
        EntityManager em = getEntityManager();
        try {
            return em.find(Pais.class, id);
        } finally {
            em.close();
        }
    }

    public int getPaisCount() {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            Root<Pais> rt = cq.from(Pais.class);
            cq.select(em.getCriteriaBuilder().count(rt));
            Query q = em.createQuery(cq);
            return ((Long) q.getSingleResult()).intValue();
        } finally {
            em.close();
        }
    }
    
}
