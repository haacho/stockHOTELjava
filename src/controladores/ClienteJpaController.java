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
import modelo.Venta;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.criteria.CriteriaBuilder;
import modelo.Cliente;
import modelo.Direccion;
import persistencia.sistema;

/**
 *
 * @author marcelo
 */
public class ClienteJpaController implements Serializable {
    
    public ClienteJpaController(EntityManagerFactory emf) {
        this.emf = emf;
    }
    private EntityManagerFactory emf = null;
    
    public EntityManager getEntityManager() {
        return emf.createEntityManager();
    }
    
    public void create(String razonSocial, String cuit, String telefono, String calle, String numero, String piso, String departamento, Integer idLocalidad) throws Exception {
        Direccion d = sistema.DIRECCION_JPA_CONTROLLER.create(calle, numero, piso, departamento, idLocalidad);
        validate(razonSocial, cuit, telefono, null);
        create(new Cliente(razonSocial, cuit, d, telefono));
    }
    
    public void edit(Integer id, String razonSocial, String cuit, String telefono, String calle, String numero, String piso, String departamento, Integer idLocalidad) throws Exception {
        if (findCliente(id) == null) {
            throw new Exception("Cliente no encontrado");
        }
        Cliente c = findCliente(id);
        sistema.DIRECCION_JPA_CONTROLLER.edit(c.getDireccion().getId(), calle, numero, piso, departamento, idLocalidad);
        validate(razonSocial, cuit, telefono, c);
        c.setRazonSocial(razonSocial);
        edit(c);
    }
    
    public void validate(String razonSocial, String cuit, String telefono, Cliente c) throws Exception {
        if (razonSocial.equals("")) {
            throw new Exception("Ingrese la razón social");
        }
        if (cuit.equals("")) {
            throw new Exception("Ingrese el cuit");
        }
        if (c == null) {
            if (find(cuit) != null) {
                throw new Exception("El cliente con cuit " + cuit + " ya existe");
            }
        } else {
            if (!c.getCuit().equals(cuit) && find(cuit) != null) {
                throw new Exception("El cliente con cuit " + cuit + " ya existe");
            }
        }
    }
    
    public Cliente find(String cuit) {
        EntityManager em = getEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        Cliente res = null;
        try {
            CriteriaQuery cq = cb.createQuery();
            Root e = cq.from(Cliente.class);
            cq.where(cb.equal(e.get(Cliente_.cuit), cuit));
            Query query = em.createQuery(cq);
            List<Cliente> aux = query.getResultList();
            res = aux.isEmpty() ? null : (Cliente) aux.get(0);
        } finally {
            em.close();
        }
        return res;
    }
    
    public void create(Cliente cliente) {
        if (cliente.getVentas() == null) {
            cliente.setVentas(new ArrayList<Venta>());
        }
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            List<Venta> attachedVentas = new ArrayList<Venta>();
            for (Venta ventasVentaToAttach : cliente.getVentas()) {
                ventasVentaToAttach = em.getReference(ventasVentaToAttach.getClass(), ventasVentaToAttach.getId());
                attachedVentas.add(ventasVentaToAttach);
            }
            cliente.setVentas(attachedVentas);
            em.persist(cliente);
            for (Venta ventasVenta : cliente.getVentas()) {
                Cliente oldClienteOfVentasVenta = ventasVenta.getCliente();
                ventasVenta.setCliente(cliente);
                ventasVenta = em.merge(ventasVenta);
                if (oldClienteOfVentasVenta != null) {
                    oldClienteOfVentasVenta.getVentas().remove(ventasVenta);
                    oldClienteOfVentasVenta = em.merge(oldClienteOfVentasVenta);
                }
            }
            em.getTransaction().commit();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }
    
    public void edit(Cliente cliente) throws NonexistentEntityException, Exception {
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            Cliente persistentCliente = em.find(Cliente.class, cliente.getId());
            List<Venta> ventasOld = persistentCliente.getVentas();
            List<Venta> ventasNew = cliente.getVentas();
            List<Venta> attachedVentasNew = new ArrayList<Venta>();
            for (Venta ventasNewVentaToAttach : ventasNew) {
                ventasNewVentaToAttach = em.getReference(ventasNewVentaToAttach.getClass(), ventasNewVentaToAttach.getId());
                attachedVentasNew.add(ventasNewVentaToAttach);
            }
            ventasNew = attachedVentasNew;
            cliente.setVentas(ventasNew);
            cliente = em.merge(cliente);
            for (Venta ventasOldVenta : ventasOld) {
                if (!ventasNew.contains(ventasOldVenta)) {
                    ventasOldVenta.setCliente(null);
                    ventasOldVenta = em.merge(ventasOldVenta);
                }
            }
            for (Venta ventasNewVenta : ventasNew) {
                if (!ventasOld.contains(ventasNewVenta)) {
                    Cliente oldClienteOfVentasNewVenta = ventasNewVenta.getCliente();
                    ventasNewVenta.setCliente(cliente);
                    ventasNewVenta = em.merge(ventasNewVenta);
                    if (oldClienteOfVentasNewVenta != null && !oldClienteOfVentasNewVenta.equals(cliente)) {
                        oldClienteOfVentasNewVenta.getVentas().remove(ventasNewVenta);
                        oldClienteOfVentasNewVenta = em.merge(oldClienteOfVentasNewVenta);
                    }
                }
            }
            em.getTransaction().commit();
        } catch (Exception ex) {
            String msg = ex.getLocalizedMessage();
            if (msg == null || msg.length() == 0) {
                Integer id = cliente.getId();
                if (findCliente(id) == null) {
                    throw new NonexistentEntityException("The cliente with id " + id + " no longer exists.");
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
            Cliente cliente;
            try {
                cliente = em.getReference(Cliente.class, id);
                cliente.getId();
            } catch (EntityNotFoundException enfe) {
                throw new NonexistentEntityException("The cliente with id " + id + " no longer exists.", enfe);
            }
            List<Venta> ventas = cliente.getVentas();
            for (Venta ventasVenta : ventas) {
                ventasVenta.setCliente(null);
                ventasVenta = em.merge(ventasVenta);
            }
            em.remove(cliente);
            em.getTransaction().commit();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }
    
    public List<Cliente> findClienteEntities() {
        return findClienteEntities(true, -1, -1);
    }
    
    public List<Cliente> findClienteEntities(int maxResults, int firstResult) {
        return findClienteEntities(false, maxResults, firstResult);
    }
    
    private List<Cliente> findClienteEntities(boolean all, int maxResults, int firstResult) {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            cq.select(cq.from(Cliente.class));
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
    
    public Cliente findCliente(Integer id) {
        EntityManager em = getEntityManager();
        try {
            return em.find(Cliente.class, id);
        } finally {
            em.close();
        }
    }
    
    public int getClienteCount() {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            Root<Cliente> rt = cq.from(Cliente.class);
            cq.select(em.getCriteriaBuilder().count(rt));
            Query q = em.createQuery(cq);
            return ((Long) q.getSingleResult()).intValue();
        } finally {
            em.close();
        }
    }
    
}
