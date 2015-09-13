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
import modelo.Pais;
import modelo.Localidad;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.criteria.CriteriaBuilder;
import modelo.Provincia;
import modelo.Provincia_;
import persistencia.sistema;

/**
 *
 * @author hacho
 */
public class ProvinciaJpaController implements Serializable {

    public ProvinciaJpaController(EntityManagerFactory emf) {
        this.emf = emf;
    }
    private EntityManagerFactory emf = null;

    public EntityManager getEntityManager() {
        return emf.createEntityManager();
    }

    public void create(String nombre, Integer idPais) throws Exception {
        validate(nombre, null, idPais);
        create(new Provincia(nombre, sistema.PAIS_JPA_CONTROLLER.findPais(idPais)));
    }

    public void edit(Integer id, String nombre, Integer idPais) throws Exception {
        if (findProvincia(id) == null) {
            throw new Exception("Provincia no encontrada");
        }
        Provincia c = findProvincia(id);
        validate(nombre, c, idPais);
        c.setNombre(nombre);
        edit(c);
    }

    public void validate(String nombre, Provincia c, Integer idPais) throws Exception {
        if (nombre.equals("")) {
            throw new Exception("Ingrese un nombre");
        }
        Provincia unaProvincia = find(nombre);
        if ((unaProvincia != null) && (unaProvincia.getUnPais().getId() == idPais)) {
            throw new Exception("La provincia " + nombre + " ya existe");
        } else {
            if (!c.getNombre().equals(nombre) && find(nombre) != null) {
                throw new Exception("La provincia " + nombre + " ya existe");
            }
        }
    }

    public Provincia find(String nombre) {
        EntityManager em = getEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        Provincia res = null;
        try {
            CriteriaQuery cq = cb.createQuery();
            Root e = cq.from(Provincia.class);
            cq.where(cb.equal(e.get(Provincia_.nombre), nombre));
            Query query = em.createQuery(cq);
            List<Provincia> aux = query.getResultList();
            res = aux.isEmpty() ? null : (Provincia) aux.get(0);
        } finally {
            em.close();
        }
        return res;
    }

    public void create(Provincia provincia) {
        if (provincia.getLocalidades() == null) {
            provincia.setLocalidades(new ArrayList<Localidad>());
        }
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            Pais unPais = provincia.getUnPais();
            if (unPais != null) {
                unPais = em.getReference(unPais.getClass(), unPais.getId());
                provincia.setUnPais(unPais);
            }
            List<Localidad> attachedLocalidades = new ArrayList<Localidad>();
            for (Localidad localidadesLocalidadToAttach : provincia.getLocalidades()) {
                localidadesLocalidadToAttach = em.getReference(localidadesLocalidadToAttach.getClass(), localidadesLocalidadToAttach.getId());
                attachedLocalidades.add(localidadesLocalidadToAttach);
            }
            provincia.setLocalidades(attachedLocalidades);
            em.persist(provincia);
            if (unPais != null) {
                unPais.getProvincias().add(provincia);
                unPais = em.merge(unPais);
            }
            for (Localidad localidadesLocalidad : provincia.getLocalidades()) {
                Provincia oldUnaProvinciaOfLocalidadesLocalidad = localidadesLocalidad.getUnaProvincia();
                localidadesLocalidad.setUnaProvincia(provincia);
                localidadesLocalidad = em.merge(localidadesLocalidad);
                if (oldUnaProvinciaOfLocalidadesLocalidad != null) {
                    oldUnaProvinciaOfLocalidadesLocalidad.getLocalidades().remove(localidadesLocalidad);
                    oldUnaProvinciaOfLocalidadesLocalidad = em.merge(oldUnaProvinciaOfLocalidadesLocalidad);
                }
            }
            em.getTransaction().commit();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public void edit(Provincia provincia) throws NonexistentEntityException, Exception {
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            Provincia persistentProvincia = em.find(Provincia.class, provincia.getId());
            Pais unPaisOld = persistentProvincia.getUnPais();
            Pais unPaisNew = provincia.getUnPais();
            List<Localidad> localidadesOld = persistentProvincia.getLocalidades();
            List<Localidad> localidadesNew = provincia.getLocalidades();
            if (unPaisNew != null) {
                unPaisNew = em.getReference(unPaisNew.getClass(), unPaisNew.getId());
                provincia.setUnPais(unPaisNew);
            }
            List<Localidad> attachedLocalidadesNew = new ArrayList<Localidad>();
            for (Localidad localidadesNewLocalidadToAttach : localidadesNew) {
                localidadesNewLocalidadToAttach = em.getReference(localidadesNewLocalidadToAttach.getClass(), localidadesNewLocalidadToAttach.getId());
                attachedLocalidadesNew.add(localidadesNewLocalidadToAttach);
            }
            localidadesNew = attachedLocalidadesNew;
            provincia.setLocalidades(localidadesNew);
            provincia = em.merge(provincia);
            if (unPaisOld != null && !unPaisOld.equals(unPaisNew)) {
                unPaisOld.getProvincias().remove(provincia);
                unPaisOld = em.merge(unPaisOld);
            }
            if (unPaisNew != null && !unPaisNew.equals(unPaisOld)) {
                unPaisNew.getProvincias().add(provincia);
                unPaisNew = em.merge(unPaisNew);
            }
            for (Localidad localidadesOldLocalidad : localidadesOld) {
                if (!localidadesNew.contains(localidadesOldLocalidad)) {
                    localidadesOldLocalidad.setUnaProvincia(null);
                    localidadesOldLocalidad = em.merge(localidadesOldLocalidad);
                }
            }
            for (Localidad localidadesNewLocalidad : localidadesNew) {
                if (!localidadesOld.contains(localidadesNewLocalidad)) {
                    Provincia oldUnaProvinciaOfLocalidadesNewLocalidad = localidadesNewLocalidad.getUnaProvincia();
                    localidadesNewLocalidad.setUnaProvincia(provincia);
                    localidadesNewLocalidad = em.merge(localidadesNewLocalidad);
                    if (oldUnaProvinciaOfLocalidadesNewLocalidad != null && !oldUnaProvinciaOfLocalidadesNewLocalidad.equals(provincia)) {
                        oldUnaProvinciaOfLocalidadesNewLocalidad.getLocalidades().remove(localidadesNewLocalidad);
                        oldUnaProvinciaOfLocalidadesNewLocalidad = em.merge(oldUnaProvinciaOfLocalidadesNewLocalidad);
                    }
                }
            }
            em.getTransaction().commit();
        } catch (Exception ex) {
            String msg = ex.getLocalizedMessage();
            if (msg == null || msg.length() == 0) {
                Integer id = provincia.getId();
                if (findProvincia(id) == null) {
                    throw new NonexistentEntityException("The provincia with id " + id + " no longer exists.");
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
            Provincia provincia;
            try {
                provincia = em.getReference(Provincia.class, id);
                provincia.getId();
            } catch (EntityNotFoundException enfe) {
                throw new NonexistentEntityException("The provincia with id " + id + " no longer exists.", enfe);
            }
            Pais unPais = provincia.getUnPais();
            if (unPais != null) {
                unPais.getProvincias().remove(provincia);
                unPais = em.merge(unPais);
            }
            List<Localidad> localidades = provincia.getLocalidades();
            for (Localidad localidadesLocalidad : localidades) {
                localidadesLocalidad.setUnaProvincia(null);
                localidadesLocalidad = em.merge(localidadesLocalidad);
            }
            em.remove(provincia);
            em.getTransaction().commit();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public List<Provincia> findProvinciaEntities() {
        return findProvinciaEntities(true, -1, -1);
    }

    public List<Provincia> findProvinciaEntities(int maxResults, int firstResult) {
        return findProvinciaEntities(false, maxResults, firstResult);
    }

    private List<Provincia> findProvinciaEntities(boolean all, int maxResults, int firstResult) {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            cq.select(cq.from(Provincia.class));
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

    public Provincia findProvincia(Integer id) {
        EntityManager em = getEntityManager();
        try {
            return em.find(Provincia.class, id);
        } finally {
            em.close();
        }
    }

    public int getProvinciaCount() {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            Root<Provincia> rt = cq.from(Provincia.class);
            cq.select(em.getCriteriaBuilder().count(rt));
            Query q = em.createQuery(cq);
            return ((Long) q.getSingleResult()).intValue();
        } finally {
            em.close();
        }
    }

}
