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
import modelo.Cliente;
import modelo.RenglonVenta;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import modelo.ContenedorRenglon;
import modelo.Venta;
import persistencia.sistema;

/**
 *
 * @author hacho
 */
public class VentaJpaController implements Serializable {

    public VentaJpaController(EntityManagerFactory emf) {
        this.emf = emf;
    }
    private EntityManagerFactory emf = null;

    public EntityManager getEntityManager() {
        return emf.createEntityManager();
    }

    public void create(Calendar fecha, Integer idCliente, List<ContenedorRenglon> productos, double porcentajeDescuento) throws Exception {
        validate(productos);
        Venta unaVenta = new Venta(fecha, sistema.CLIENTE_JPA_CONTROLLER.findCliente(idCliente), porcentajeDescuento);
        create(unaVenta);
        persistirRenglones(productos, unaVenta);
    }

    public void validate(List<ContenedorRenglon> productos) throws Exception {
        if (productos.isEmpty()) {
            throw new Exception("La lista de productos se encuentra vacía");
        }
    }

    private void persistirRenglones(List<ContenedorRenglon> productos, Venta unaVenta) throws Exception {
        for (ContenedorRenglon unContnedor : productos) {
            sistema.RENGLON_JPA_CONTROLLER.create(unContnedor, unaVenta);
        }
    }

    public void create(Venta venta) {
        if (venta.getRenglonVentas() == null) {
            venta.setRenglonVentas(new ArrayList<RenglonVenta>());
        }
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            Cliente cliente = venta.getCliente();
            if (cliente != null) {
                cliente = em.getReference(cliente.getClass(), cliente.getId());
                venta.setCliente(cliente);
            }
            List<RenglonVenta> attachedRenglonVentas = new ArrayList<RenglonVenta>();
            for (RenglonVenta renglonVentasRenglonVentaToAttach : venta.getRenglonVentas()) {
                renglonVentasRenglonVentaToAttach = em.getReference(renglonVentasRenglonVentaToAttach.getClass(), renglonVentasRenglonVentaToAttach.getId());
                attachedRenglonVentas.add(renglonVentasRenglonVentaToAttach);
            }
            venta.setRenglonVentas(attachedRenglonVentas);
            em.persist(venta);
            if (cliente != null) {
                cliente.getVentas().add(venta);
                cliente = em.merge(cliente);
            }
            for (RenglonVenta renglonVentasRenglonVenta : venta.getRenglonVentas()) {
                Venta oldVentaPertenecienteOfRenglonVentasRenglonVenta = renglonVentasRenglonVenta.getVentaPerteneciente();
                renglonVentasRenglonVenta.setVentaPerteneciente(venta);
                renglonVentasRenglonVenta = em.merge(renglonVentasRenglonVenta);
                if (oldVentaPertenecienteOfRenglonVentasRenglonVenta != null) {
                    oldVentaPertenecienteOfRenglonVentasRenglonVenta.getRenglonVentas().remove(renglonVentasRenglonVenta);
                    oldVentaPertenecienteOfRenglonVentasRenglonVenta = em.merge(oldVentaPertenecienteOfRenglonVentasRenglonVenta);
                }
            }
            em.getTransaction().commit();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public void edit(Venta venta) throws NonexistentEntityException, Exception {
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            Venta persistentVenta = em.find(Venta.class, venta.getId());
            Cliente clienteOld = persistentVenta.getCliente();
            Cliente clienteNew = venta.getCliente();
            List<RenglonVenta> renglonVentasOld = persistentVenta.getRenglonVentas();
            List<RenglonVenta> renglonVentasNew = venta.getRenglonVentas();
            if (clienteNew != null) {
                clienteNew = em.getReference(clienteNew.getClass(), clienteNew.getId());
                venta.setCliente(clienteNew);
            }
            List<RenglonVenta> attachedRenglonVentasNew = new ArrayList<RenglonVenta>();
            for (RenglonVenta renglonVentasNewRenglonVentaToAttach : renglonVentasNew) {
                renglonVentasNewRenglonVentaToAttach = em.getReference(renglonVentasNewRenglonVentaToAttach.getClass(), renglonVentasNewRenglonVentaToAttach.getId());
                attachedRenglonVentasNew.add(renglonVentasNewRenglonVentaToAttach);
            }
            renglonVentasNew = attachedRenglonVentasNew;
            venta.setRenglonVentas(renglonVentasNew);
            venta = em.merge(venta);
            if (clienteOld != null && !clienteOld.equals(clienteNew)) {
                clienteOld.getVentas().remove(venta);
                clienteOld = em.merge(clienteOld);
            }
            if (clienteNew != null && !clienteNew.equals(clienteOld)) {
                clienteNew.getVentas().add(venta);
                clienteNew = em.merge(clienteNew);
            }
            for (RenglonVenta renglonVentasOldRenglonVenta : renglonVentasOld) {
                if (!renglonVentasNew.contains(renglonVentasOldRenglonVenta)) {
                    renglonVentasOldRenglonVenta.setVentaPerteneciente(null);
                    renglonVentasOldRenglonVenta = em.merge(renglonVentasOldRenglonVenta);
                }
            }
            for (RenglonVenta renglonVentasNewRenglonVenta : renglonVentasNew) {
                if (!renglonVentasOld.contains(renglonVentasNewRenglonVenta)) {
                    Venta oldVentaPertenecienteOfRenglonVentasNewRenglonVenta = renglonVentasNewRenglonVenta.getVentaPerteneciente();
                    renglonVentasNewRenglonVenta.setVentaPerteneciente(venta);
                    renglonVentasNewRenglonVenta = em.merge(renglonVentasNewRenglonVenta);
                    if (oldVentaPertenecienteOfRenglonVentasNewRenglonVenta != null && !oldVentaPertenecienteOfRenglonVentasNewRenglonVenta.equals(venta)) {
                        oldVentaPertenecienteOfRenglonVentasNewRenglonVenta.getRenglonVentas().remove(renglonVentasNewRenglonVenta);
                        oldVentaPertenecienteOfRenglonVentasNewRenglonVenta = em.merge(oldVentaPertenecienteOfRenglonVentasNewRenglonVenta);
                    }
                }
            }
            em.getTransaction().commit();
        } catch (Exception ex) {
            String msg = ex.getLocalizedMessage();
            if (msg == null || msg.length() == 0) {
                Integer id = venta.getId();
                if (findVenta(id) == null) {
                    throw new NonexistentEntityException("The venta with id " + id + " no longer exists.");
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
            Venta venta;
            try {
                venta = em.getReference(Venta.class, id);
                venta.getId();
            } catch (EntityNotFoundException enfe) {
                throw new NonexistentEntityException("The venta with id " + id + " no longer exists.", enfe);
            }
            Cliente cliente = venta.getCliente();
            if (cliente != null) {
                cliente.getVentas().remove(venta);
                cliente = em.merge(cliente);
            }
            List<RenglonVenta> renglonVentas = venta.getRenglonVentas();
            for (RenglonVenta renglonVentasRenglonVenta : renglonVentas) {
                renglonVentasRenglonVenta.setVentaPerteneciente(null);
                renglonVentasRenglonVenta = em.merge(renglonVentasRenglonVenta);
            }
            em.remove(venta);
            em.getTransaction().commit();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public List<Venta> findVentaEntities() {
        return findVentaEntities(true, -1, -1);
    }

    public List<Venta> findVentaEntities(int maxResults, int firstResult) {
        return findVentaEntities(false, maxResults, firstResult);
    }

    private List<Venta> findVentaEntities(boolean all, int maxResults, int firstResult) {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            cq.select(cq.from(Venta.class));
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

    public Venta findVenta(Integer id) {
        EntityManager em = getEntityManager();
        try {
            return em.find(Venta.class, id);
        } finally {
            em.close();
        }
    }

    public int getVentaCount() {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            Root<Venta> rt = cq.from(Venta.class);
            cq.select(em.getCriteriaBuilder().count(rt));
            Query q = em.createQuery(cq);
            return ((Long) q.getSingleResult()).intValue();
        } finally {
            em.close();
        }
    }

}
