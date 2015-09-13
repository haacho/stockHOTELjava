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
import modelo.Cliente;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.criteria.CriteriaBuilder;
import modelo.Localidad;
import modelo.Localidad_;
import persistencia.sistema;

/**
 *
 * @author hacho
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
        validate(nombre, null, idProvincia);
        create(new Localidad(nombre, prefTelefonico, codPostal, sistema.PROVINCIA_JPA_CONTROLLER.findProvincia(idProvincia)));
    }

    public void edit(Integer id, String nombre, String prefTelefonico, String codPostal, Integer idProvincia) throws Exception {
        if (findLocalidad(id) == null) {
            throw new Exception("Localidad no encontrada");
        }
        Localidad c = findLocalidad(id);
        validate(nombre, c, idProvincia);
        c.setNombre(nombre);
        edit(c);
    }

    public void validate(String nombre, Localidad c, Integer idProvincia) throws Exception {
        if (nombre.equals("")) {
            throw new Exception("Ingrese el nombre");
        }
        if (c == null) { 
            Localidad unaLocalidad = find(nombre);
            if ((unaLocalidad != null) && (unaLocalidad.getUnaProvincia().getId() == idProvincia)) {
                throw new Exception("La localidad " + nombre + " ya existe");
            }
        } else {
            if (!c.getNombre().equals(nombre) && find(nombre) != null) {
                throw new Exception("La localidad " + nombre + " ya existe");
            }
        }
    }

    public Localidad find(String nombre) {
        EntityManager em = getEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        Localidad res = null;
        try {
            CriteriaQuery cq = cb.createQuery();
            Root e = cq.from(Cliente.class);
            cq.where(cb.equal(e.get(Localidad_.nombre), nombre));
            Query query = em.createQuery(cq);
            List<Localidad> aux = query.getResultList();
            res = aux.isEmpty() ? null : (Localidad) aux.get(0);
        } finally {
            em.close();
        }
        return res;
    }
    
    public void create(Localidad localidad) {
        if (localidad.getClientes() == null) {
            localidad.setClientes(new ArrayList<Cliente>());
        }
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            Provincia unaProvincia = localidad.getUnaProvincia();
            if (unaProvincia != null) {
                unaProvincia = em.getReference(unaProvincia.getClass(), unaProvincia.getId());
                localidad.setUnaProvincia(unaProvincia);
            }
            List<Cliente> attachedClientes = new ArrayList<Cliente>();
            for (Cliente clientesClienteToAttach : localidad.getClientes()) {
                clientesClienteToAttach = em.getReference(clientesClienteToAttach.getClass(), clientesClienteToAttach.getId());
                attachedClientes.add(clientesClienteToAttach);
            }
            localidad.setClientes(attachedClientes);
            em.persist(localidad);
            if (unaProvincia != null) {
                unaProvincia.getLocalidades().add(localidad);
                unaProvincia = em.merge(unaProvincia);
            }
            for (Cliente clientesCliente : localidad.getClientes()) {
                Localidad oldCiudadOfClientesCliente = clientesCliente.getCiudad();
                clientesCliente.setCiudad(localidad);
                clientesCliente = em.merge(clientesCliente);
                if (oldCiudadOfClientesCliente != null) {
                    oldCiudadOfClientesCliente.getClientes().remove(clientesCliente);
                    oldCiudadOfClientesCliente = em.merge(oldCiudadOfClientesCliente);
                }
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
            Provincia unaProvinciaOld = persistentLocalidad.getUnaProvincia();
            Provincia unaProvinciaNew = localidad.getUnaProvincia();
            List<Cliente> clientesOld = persistentLocalidad.getClientes();
            List<Cliente> clientesNew = localidad.getClientes();
            if (unaProvinciaNew != null) {
                unaProvinciaNew = em.getReference(unaProvinciaNew.getClass(), unaProvinciaNew.getId());
                localidad.setUnaProvincia(unaProvinciaNew);
            }
            List<Cliente> attachedClientesNew = new ArrayList<Cliente>();
            for (Cliente clientesNewClienteToAttach : clientesNew) {
                clientesNewClienteToAttach = em.getReference(clientesNewClienteToAttach.getClass(), clientesNewClienteToAttach.getId());
                attachedClientesNew.add(clientesNewClienteToAttach);
            }
            clientesNew = attachedClientesNew;
            localidad.setClientes(clientesNew);
            localidad = em.merge(localidad);
            if (unaProvinciaOld != null && !unaProvinciaOld.equals(unaProvinciaNew)) {
                unaProvinciaOld.getLocalidades().remove(localidad);
                unaProvinciaOld = em.merge(unaProvinciaOld);
            }
            if (unaProvinciaNew != null && !unaProvinciaNew.equals(unaProvinciaOld)) {
                unaProvinciaNew.getLocalidades().add(localidad);
                unaProvinciaNew = em.merge(unaProvinciaNew);
            }
            for (Cliente clientesOldCliente : clientesOld) {
                if (!clientesNew.contains(clientesOldCliente)) {
                    clientesOldCliente.setCiudad(null);
                    clientesOldCliente = em.merge(clientesOldCliente);
                }
            }
            for (Cliente clientesNewCliente : clientesNew) {
                if (!clientesOld.contains(clientesNewCliente)) {
                    Localidad oldCiudadOfClientesNewCliente = clientesNewCliente.getCiudad();
                    clientesNewCliente.setCiudad(localidad);
                    clientesNewCliente = em.merge(clientesNewCliente);
                    if (oldCiudadOfClientesNewCliente != null && !oldCiudadOfClientesNewCliente.equals(localidad)) {
                        oldCiudadOfClientesNewCliente.getClientes().remove(clientesNewCliente);
                        oldCiudadOfClientesNewCliente = em.merge(oldCiudadOfClientesNewCliente);
                    }
                }
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
            Provincia unaProvincia = localidad.getUnaProvincia();
            if (unaProvincia != null) {
                unaProvincia.getLocalidades().remove(localidad);
                unaProvincia = em.merge(unaProvincia);
            }
            List<Cliente> clientes = localidad.getClientes();
            for (Cliente clientesCliente : clientes) {
                clientesCliente.setCiudad(null);
                clientesCliente = em.merge(clientesCliente);
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
