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
import modelo.ContenedorRenglon;
import modelo.RenglonVenta;
import modelo.Venta;
import persistencia.sistema;

/**
 *
 * @author hacho
 */
public class RenglonVentaJpaController implements Serializable {

    public RenglonVentaJpaController(EntityManagerFactory emf) {
        this.emf = emf;
    }
    private EntityManagerFactory emf = null;

    public EntityManager getEntityManager() {
        return emf.createEntityManager();
    }

    public void create(ContenedorRenglon contenedor, Venta unaVenta) throws Exception {
        create(new RenglonVenta(sistema.PRODUCTO_JPA_CONTROLLER.findProducto(contenedor.getIdProducto()), contenedor.getCantProducto(), unaVenta));
    }

    public void create(RenglonVenta renglonVenta) {
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            Venta ventaPerteneciente = renglonVenta.getVentaPerteneciente();
            if (ventaPerteneciente != null) {
                ventaPerteneciente = em.getReference(ventaPerteneciente.getClass(), ventaPerteneciente.getId());
                renglonVenta.setVentaPerteneciente(ventaPerteneciente);
            }
            em.persist(renglonVenta);
            if (ventaPerteneciente != null) {
                ventaPerteneciente.getRenglonVentas().add(renglonVenta);
                ventaPerteneciente = em.merge(ventaPerteneciente);
            }
            em.getTransaction().commit();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public void edit(RenglonVenta renglonVenta) throws NonexistentEntityException, Exception {
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            RenglonVenta persistentRenglonVenta = em.find(RenglonVenta.class, renglonVenta.getId());
            Venta ventaPertenecienteOld = persistentRenglonVenta.getVentaPerteneciente();
            Venta ventaPertenecienteNew = renglonVenta.getVentaPerteneciente();
            if (ventaPertenecienteNew != null) {
                ventaPertenecienteNew = em.getReference(ventaPertenecienteNew.getClass(), ventaPertenecienteNew.getId());
                renglonVenta.setVentaPerteneciente(ventaPertenecienteNew);
            }
            renglonVenta = em.merge(renglonVenta);
            if (ventaPertenecienteOld != null && !ventaPertenecienteOld.equals(ventaPertenecienteNew)) {
                ventaPertenecienteOld.getRenglonVentas().remove(renglonVenta);
                ventaPertenecienteOld = em.merge(ventaPertenecienteOld);
            }
            if (ventaPertenecienteNew != null && !ventaPertenecienteNew.equals(ventaPertenecienteOld)) {
                ventaPertenecienteNew.getRenglonVentas().add(renglonVenta);
                ventaPertenecienteNew = em.merge(ventaPertenecienteNew);
            }
            em.getTransaction().commit();
        } catch (Exception ex) {
            String msg = ex.getLocalizedMessage();
            if (msg == null || msg.length() == 0) {
                Long id = renglonVenta.getId();
                if (findRenglonVenta(id) == null) {
                    throw new NonexistentEntityException("The renglonVenta with id " + id + " no longer exists.");
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
            RenglonVenta renglonVenta;
            try {
                renglonVenta = em.getReference(RenglonVenta.class, id);
                renglonVenta.getId();
            } catch (EntityNotFoundException enfe) {
                throw new NonexistentEntityException("The renglonVenta with id " + id + " no longer exists.", enfe);
            }
            Venta ventaPerteneciente = renglonVenta.getVentaPerteneciente();
            if (ventaPerteneciente != null) {
                ventaPerteneciente.getRenglonVentas().remove(renglonVenta);
                ventaPerteneciente = em.merge(ventaPerteneciente);
            }
            em.remove(renglonVenta);
            em.getTransaction().commit();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public List<RenglonVenta> findRenglonVentaEntities() {
        return findRenglonVentaEntities(true, -1, -1);
    }

    public List<RenglonVenta> findRenglonVentaEntities(int maxResults, int firstResult) {
        return findRenglonVentaEntities(false, maxResults, firstResult);
    }

    private List<RenglonVenta> findRenglonVentaEntities(boolean all, int maxResults, int firstResult) {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            cq.select(cq.from(RenglonVenta.class));
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

    public RenglonVenta findRenglonVenta(Long id) {
        EntityManager em = getEntityManager();
        try {
            return em.find(RenglonVenta.class, id);
        } finally {
            em.close();
        }
    }

    public int getRenglonVentaCount() {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            Root<RenglonVenta> rt = cq.from(RenglonVenta.class);
            cq.select(em.getCriteriaBuilder().count(rt));
            Query q = em.createQuery(cq);
            return ((Long) q.getSingleResult()).intValue();
        } finally {
            em.close();
        }
    }

}
