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
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import modelo.Ingreso;
import modelo.Usuario;

/**
 *
 * @author hacho
 */
public class IngresoJpaController implements Serializable {

    public IngresoJpaController(EntityManagerFactory emf) {
        this.emf = emf;
    }
    private EntityManagerFactory emf = null;

    public EntityManager getEntityManager() {
        return emf.createEntityManager();
    }

    public void create(Ingreso ingreso) {
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            Usuario unUsuario = ingreso.getUnUsuario();
            if (unUsuario != null) {
                unUsuario = em.getReference(unUsuario.getClass(), unUsuario.getId());
                ingreso.setUnUsuario(unUsuario);
            }
            em.persist(ingreso);
            if (unUsuario != null) {
                unUsuario.getIngresos().add(ingreso);
                unUsuario = em.merge(unUsuario);
            }
            em.getTransaction().commit();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public void edit(Ingreso ingreso) throws NonexistentEntityException, Exception {
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            Ingreso persistentIngreso = em.find(Ingreso.class, ingreso.getId());
            Usuario unUsuarioOld = persistentIngreso.getUnUsuario();
            Usuario unUsuarioNew = ingreso.getUnUsuario();
            if (unUsuarioNew != null) {
                unUsuarioNew = em.getReference(unUsuarioNew.getClass(), unUsuarioNew.getId());
                ingreso.setUnUsuario(unUsuarioNew);
            }
            ingreso = em.merge(ingreso);
            if (unUsuarioOld != null && !unUsuarioOld.equals(unUsuarioNew)) {
                unUsuarioOld.getIngresos().remove(ingreso);
                unUsuarioOld = em.merge(unUsuarioOld);
            }
            if (unUsuarioNew != null && !unUsuarioNew.equals(unUsuarioOld)) {
                unUsuarioNew.getIngresos().add(ingreso);
                unUsuarioNew = em.merge(unUsuarioNew);
            }
            em.getTransaction().commit();
        } catch (Exception ex) {
            String msg = ex.getLocalizedMessage();
            if (msg == null || msg.length() == 0) {
                Long id = ingreso.getId();
                if (findIngreso(id) == null) {
                    throw new NonexistentEntityException("The ingreso with id " + id + " no longer exists.");
                }
            }
            throw ex;
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public void destroy(Long id) throws NonexistentEntityException {
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            Ingreso ingreso;
            try {
                ingreso = em.getReference(Ingreso.class, id);
                ingreso.getId();
            } catch (EntityNotFoundException enfe) {
                throw new NonexistentEntityException("The ingreso with id " + id + " no longer exists.", enfe);
            }
            Usuario unUsuario = ingreso.getUnUsuario();
            if (unUsuario != null) {
                unUsuario.getIngresos().remove(ingreso);
                unUsuario = em.merge(unUsuario);
            }
            em.remove(ingreso);
            em.getTransaction().commit();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public List<Ingreso> findIngresoEntities() {
        return findIngresoEntities(true, -1, -1);
    }

    public List<Ingreso> findIngresoEntities(int maxResults, int firstResult) {
        return findIngresoEntities(false, maxResults, firstResult);
    }

    private List<Ingreso> findIngresoEntities(boolean all, int maxResults, int firstResult) {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            cq.select(cq.from(Ingreso.class));
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

    public Ingreso findIngreso(Long id) {
        EntityManager em = getEntityManager();
        try {
            return em.find(Ingreso.class, id);
        } finally {
            em.close();
        }
    }

    public int getIngresoCount() {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            Root<Ingreso> rt = cq.from(Ingreso.class);
            cq.select(em.getCriteriaBuilder().count(rt));
            Query q = em.createQuery(cq);
            return ((Long) q.getSingleResult()).intValue();
        } finally {
            em.close();
        }
    }
    
}
