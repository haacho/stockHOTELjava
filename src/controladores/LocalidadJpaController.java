/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package controladores;

import controladores.exceptions.NonexistentEntityException;
import java.io.Serializable;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;
import javax.persistence.EntityNotFoundException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import modelo.Localidad;
import modelo.Localidad_;
import modelo.Provincia;
import persistencia.sistema;

/**
 *
 * @author marcelo
 */
public class LocalidadJpaController implements Serializable {
    
    public LocalidadJpaController(EntityManagerFactory emf) {
        this.emf = emf;
    }
    private EntityManagerFactory emf = null;
    
    public EntityManager getEntityManager() {
        return emf.createEntityManager();
    }
    
    public void create(String nombre, String prefTelefonico, String codPostal, Integer idProvincia) throws Exception {
        validate(nombre, idProvincia, null);
        create(new Localidad(nombre, prefTelefonico, codPostal, sistema.PROVINCIA_JPA_CONTROLLER.findProvincia(idProvincia)));
    }
    
    public void edit(Integer id, String nombre, String prefTelefonico, String codPostal, Integer idProvincia) throws Exception {
        if (findLocalidad(id) == null) {
            throw new Exception("Localidad no encontrada");
        }
        Localidad l = findLocalidad(id);
        validate(nombre, idProvincia, l);
        l.setCodPostal(codPostal);
        l.setNombre(nombre);
        l.setPrefTelefonico(prefTelefonico);
        l.setProvincia(sistema.PROVINCIA_JPA_CONTROLLER.findProvincia(idProvincia));
        edit(l);
    }
    
    public void validate(String nombre, Integer idProvincia, Localidad l) throws Exception {
        if (nombre.equals("")) {
            throw new Exception("Ingrese un nombre");
        }
        if (sistema.PROVINCIA_JPA_CONTROLLER.findProvincia(idProvincia) == null) {
            throw new Exception("Seleccione una provincia");
        }
        if (l == null) {
            if (find(nombre, idProvincia) != null) {
                throw new Exception("La localidad " + nombre + " ya existe");
            }
        } else {
            if (!l.getNombre().equals(nombre) && find(nombre, idProvincia) != null) {
                throw new Exception("La localidad " + nombre + " ya existe");
            }
        }
    }
    
    public Localidad find(String nombre, Integer idProvincia) {
        EntityManager em = getEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        Localidad res = null;
        try {
            CriteriaQuery cq = cb.createQuery();
            Root e = cq.from(Localidad.class);
            cq.where(cb.and(cb.equal(e.get(Localidad_.nombre), nombre), cb.equal(e.get(Localidad_.provincia), sistema.PROVINCIA_JPA_CONTROLLER.findProvincia(idProvincia))));
            Query query = em.createQuery(cq);
            List<Localidad> aux = query.getResultList();
            res = aux.isEmpty() ? null : (Localidad) aux.get(0);
        } finally {
            em.close();
        }
        return res;
    }
    
    public void create(Localidad localidad) {
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            Provincia provincia = localidad.getProvincia();
            if (provincia != null) {
                provincia = em.getReference(provincia.getClass(), provincia.getId());
                localidad.setProvincia(provincia);
            }
            em.persist(localidad);
            if (provincia != null) {
                provincia.getLocalidades().add(localidad);
                provincia = em.merge(provincia);
            }
            em.getTransaction().commit();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }
    
    public void edit(Localidad localidad) throws NonexistentEntityException, Exception {
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            Localidad persistentLocalidad = em.find(Localidad.class, localidad.getId());
            Provincia provinciaOld = persistentLocalidad.getProvincia();
            Provincia provinciaNew = localidad.getProvincia();
            if (provinciaNew != null) {
                provinciaNew = em.getReference(provinciaNew.getClass(), provinciaNew.getId());
                localidad.setProvincia(provinciaNew);
            }
            localidad = em.merge(localidad);
            if (provinciaOld != null && !provinciaOld.equals(provinciaNew)) {
                provinciaOld.getLocalidades().remove(localidad);
                provinciaOld = em.merge(provinciaOld);
            }
            if (provinciaNew != null && !provinciaNew.equals(provinciaOld)) {
                provinciaNew.getLocalidades().add(localidad);
                provinciaNew = em.merge(provinciaNew);
            }
            em.getTransaction().commit();
        } catch (Exception ex) {
            String msg = ex.getLocalizedMessage();
            if (msg == null || msg.length() == 0) {
                Integer id = localidad.getId();
                if (findLocalidad(id) == null) {
                    throw new NonexistentEntityException("The localidad with id " + id + " no longer exists.");
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
            Localidad localidad;
            try {
                localidad = em.getReference(Localidad.class, id);
                localidad.getId();
            } catch (EntityNotFoundException enfe) {
                throw new NonexistentEntityException("The localidad with id " + id + " no longer exists.", enfe);
            }
            Provincia provincia = localidad.getProvincia();
            if (provincia != null) {
                provincia.getLocalidades().remove(localidad);
                provincia = em.merge(provincia);
            }
            em.remove(localidad);
            em.getTransaction().commit();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }
    
    public List<Localidad> findLocalidadEntities() {
        return findLocalidadEntities(true, -1, -1);
    }
    
    public List<Localidad> findLocalidadEntities(int maxResults, int firstResult) {
        return findLocalidadEntities(false, maxResults, firstResult);
    }
    
    private List<Localidad> findLocalidadEntities(boolean all, int maxResults, int firstResult) {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            cq.select(cq.from(Localidad.class));
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
    
    public Localidad findLocalidad(Integer id) {
        EntityManager em = getEntityManager();
        try {
            return em.find(Localidad.class, id);
        } finally {
            em.close();
        }
    }
    
    public int getLocalidadCount() {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            Root<Localidad> rt = cq.from(Localidad.class);
            cq.select(em.getCriteriaBuilder().count(rt));
            Query q = em.createQuery(cq);
            return ((Long) q.getSingleResult()).intValue();
        } finally {
            em.close();
        }
    }
    
}
