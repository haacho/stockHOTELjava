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
 * @author marcelo
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
        validate(nombre, idPais, null);
        create(new Provincia(nombre, sistema.PAIS_JPA_CONTROLLER.findPais(idPais)));
    }

    public void edit(Integer id, String nombre, Integer idPais) throws Exception {
        if (findProvincia(id) == null) {
            throw new Exception("Provincia no encontrada");
        }
        Provincia p = findProvincia(id);
        validate(nombre, idPais, p);
        p.setNombre(nombre);
        p.setPais(sistema.PAIS_JPA_CONTROLLER.findPais(idPais));
        edit(p);
    }

    public void validate(String nombre, Integer idPais, Provincia p) throws Exception {
        if (nombre.equals("")) {
            throw new Exception("Ingrese un nombre");
        }
        if (sistema.PAIS_JPA_CONTROLLER.findPais(idPais) == null) {
            throw new Exception("Seleccione un pais");
        }
        if (p == null) {
            if (find(nombre, idPais) != null) {
                throw new Exception("La provincia " + nombre + " ya existe");
            }
        } else {
            if (!p.getNombre().equals(nombre) && find(nombre, idPais) != null) {
                throw new Exception("La provincia " + nombre + " ya existe");
            }
        }
    }

    public Provincia find(String nombre, Integer idPais) {
        EntityManager em = getEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        Provincia res = null;
        try {
            CriteriaQuery cq = cb.createQuery();
            Root e = cq.from(Provincia.class);
            cq.where(cb.and(cb.equal(e.get(Provincia_.nombre), nombre), cb.equal(e.get(Provincia_.pais), sistema.PAIS_JPA_CONTROLLER.findPais(idPais))));
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
            Pais pais = provincia.getPais();
            if (pais != null) {
                pais = em.getReference(pais.getClass(), pais.getId());
                provincia.setPais(pais);
            }
            List<Localidad> attachedLocalidades = new ArrayList<Localidad>();
            for (Localidad localidadesLocalidadToAttach : provincia.getLocalidades()) {
                localidadesLocalidadToAttach = em.getReference(localidadesLocalidadToAttach.getClass(), localidadesLocalidadToAttach.getId());
                attachedLocalidades.add(localidadesLocalidadToAttach);
            }
            provincia.setLocalidades(attachedLocalidades);
            em.persist(provincia);
            if (pais != null) {
                pais.getProvincias().add(provincia);
                pais = em.merge(pais);
            }
            for (Localidad localidadesLocalidad : provincia.getLocalidades()) {
                Provincia oldProvinciaOfLocalidadesLocalidad = localidadesLocalidad.getProvincia();
                localidadesLocalidad.setProvincia(provincia);
                localidadesLocalidad = em.merge(localidadesLocalidad);
                if (oldProvinciaOfLocalidadesLocalidad != null) {
                    oldProvinciaOfLocalidadesLocalidad.getLocalidades().remove(localidadesLocalidad);
                    oldProvinciaOfLocalidadesLocalidad = em.merge(oldProvinciaOfLocalidadesLocalidad);
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
            Pais paisOld = persistentProvincia.getPais();
            Pais paisNew = provincia.getPais();
            List<Localidad> localidadesOld = persistentProvincia.getLocalidades();
            List<Localidad> localidadesNew = provincia.getLocalidades();
            if (paisNew != null) {
                paisNew = em.getReference(paisNew.getClass(), paisNew.getId());
                provincia.setPais(paisNew);
            }
            List<Localidad> attachedLocalidadesNew = new ArrayList<Localidad>();
            for (Localidad localidadesNewLocalidadToAttach : localidadesNew) {
                localidadesNewLocalidadToAttach = em.getReference(localidadesNewLocalidadToAttach.getClass(), localidadesNewLocalidadToAttach.getId());
                attachedLocalidadesNew.add(localidadesNewLocalidadToAttach);
            }
            localidadesNew = attachedLocalidadesNew;
            provincia.setLocalidades(localidadesNew);
            provincia = em.merge(provincia);
            if (paisOld != null && !paisOld.equals(paisNew)) {
                paisOld.getProvincias().remove(provincia);
                paisOld = em.merge(paisOld);
            }
            if (paisNew != null && !paisNew.equals(paisOld)) {
                paisNew.getProvincias().add(provincia);
                paisNew = em.merge(paisNew);
            }
            for (Localidad localidadesOldLocalidad : localidadesOld) {
                if (!localidadesNew.contains(localidadesOldLocalidad)) {
                    localidadesOldLocalidad.setProvincia(null);
                    localidadesOldLocalidad = em.merge(localidadesOldLocalidad);
                }
            }
            for (Localidad localidadesNewLocalidad : localidadesNew) {
                if (!localidadesOld.contains(localidadesNewLocalidad)) {
                    Provincia oldProvinciaOfLocalidadesNewLocalidad = localidadesNewLocalidad.getProvincia();
                    localidadesNewLocalidad.setProvincia(provincia);
                    localidadesNewLocalidad = em.merge(localidadesNewLocalidad);
                    if (oldProvinciaOfLocalidadesNewLocalidad != null && !oldProvinciaOfLocalidadesNewLocalidad.equals(provincia)) {
                        oldProvinciaOfLocalidadesNewLocalidad.getLocalidades().remove(localidadesNewLocalidad);
                        oldProvinciaOfLocalidadesNewLocalidad = em.merge(oldProvinciaOfLocalidadesNewLocalidad);
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
            Pais pais = provincia.getPais();
            if (pais != null) {
                pais.getProvincias().remove(provincia);
                pais = em.merge(pais);
            }
            List<Localidad> localidades = provincia.getLocalidades();
            for (Localidad localidadesLocalidad : localidades) {
                localidadesLocalidad.setProvincia(null);
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
