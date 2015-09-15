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
import modelo.Direccion;
import modelo.Direccion_;
import persistencia.sistema;

/**
 *
 * @author marcelo
 */
public class DireccionJpaController implements Serializable {

    public DireccionJpaController(EntityManagerFactory emf) {
        this.emf = emf;
    }
    private EntityManagerFactory emf = null;

    public EntityManager getEntityManager() {
        return emf.createEntityManager();
    }

    public Direccion create(String calle, String numero, String piso, String departamento, Integer idLocalidad) throws Exception {
        validate(calle, numero, idLocalidad);
        Direccion d = new Direccion(calle, numero, piso, departamento, sistema.LOCALIDAD_JPA_CONTROLLER.findLocalidad(idLocalidad));
        create(d);
        return d;
    }

    public void edit(Integer id, String calle, String numero, String piso, String departamento, Integer idLocalidad) throws Exception {
        if (findDireccion(id) == null) {
            throw new Exception("Dirección no encontrada");
        }
        Direccion d = findDireccion(id);
        validate(calle, numero, idLocalidad);
        d.setCalle(calle);
        d.setDepartamento(departamento);
        d.setLocalidad(sistema.LOCALIDAD_JPA_CONTROLLER.findLocalidad(idLocalidad));
        d.setNumero(numero);
        d.setPiso(piso);
        edit(d);
    }

    public void validate(String calle, String numero, Integer idLocalidad) throws Exception {
        if (sistema.LOCALIDAD_JPA_CONTROLLER.findLocalidad(idLocalidad) == null) {
            throw new Exception("Seleccione una localidad");
        }
        if (calle.equals("")) {
            throw new Exception("Ingrese el nombre de la calle");
        }
        if (numero.equals("")) {
            throw new Exception("Ingrese el numero de la casa");
        }
    }

    public Direccion find(Integer idLocalidad, String calle, String numero) {
        EntityManager em = getEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        Direccion res = null;
        try {
            CriteriaQuery cq = cb.createQuery();
            Root e = cq.from(Direccion.class);
            cq.where(cb.and(e.get(Direccion_.calle), cb.and(cb.equal(e.get(Direccion_.calle), calle), cb.equal(e.get(Direccion_.numero), numero))));
            Query query = em.createQuery(cq);
            List<Direccion> aux = query.getResultList();
            res = aux.isEmpty() ? null : (Direccion) aux.get(0);
        } finally {
            em.close();
        }
        return res;
    }

    public void create(Direccion direccion) {
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            em.persist(direccion);
            em.getTransaction().commit();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public void edit(Direccion direccion) throws NonexistentEntityException, Exception {
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            direccion = em.merge(direccion);
            em.getTransaction().commit();
        } catch (Exception ex) {
            String msg = ex.getLocalizedMessage();
            if (msg == null || msg.length() == 0) {
                Integer id = direccion.getId();
                if (findDireccion(id) == null) {
                    throw new NonexistentEntityException("The direccion with id " + id + " no longer exists.");
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
            Direccion direccion;
            try {
                direccion = em.getReference(Direccion.class, id);
                direccion.getId();
            } catch (EntityNotFoundException enfe) {
                throw new NonexistentEntityException("The direccion with id " + id + " no longer exists.", enfe);
            }
            em.remove(direccion);
            em.getTransaction().commit();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public List<Direccion> findDireccionEntities() {
        return findDireccionEntities(true, -1, -1);
    }

    public List<Direccion> findDireccionEntities(int maxResults, int firstResult) {
        return findDireccionEntities(false, maxResults, firstResult);
    }

    private List<Direccion> findDireccionEntities(boolean all, int maxResults, int firstResult) {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            cq.select(cq.from(Direccion.class));
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

    public Direccion findDireccion(Integer id) {
        EntityManager em = getEntityManager();
        try {
            return em.find(Direccion.class, id);
        } finally {
            em.close();
        }
    }

    public int getDireccionCount() {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            Root<Direccion> rt = cq.from(Direccion.class);
            cq.select(em.getCriteriaBuilder().count(rt));
            Query q = em.createQuery(cq);
            return ((Long) q.getSingleResult()).intValue();
        } finally {
            em.close();
        }
    }

}
