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
import modelo.Ingreso;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.criteria.CriteriaBuilder;
import modelo.Usuario;
import modelo.Usuario_;

/**
 *
 * @author hacho
 */
public class UsuarioJpaController implements Serializable {

    public UsuarioJpaController(EntityManagerFactory emf) {
        this.emf = emf;
    }
    private EntityManagerFactory emf = null;

    public EntityManager getEntityManager() {
        return emf.createEntityManager();
    }

    public void create(String nombre, String pass) throws Exception {
        validate(nombre, pass,null);
        create(new Usuario(nombre, pass));
    }

    public void edit(Integer id, String nombre, String pass) throws Exception {
        if (findUsuario(id) == null) {
            throw new Exception("Usuario no encontrado");
        }
        Usuario c = findUsuario(id);
        validate(nombre, pass, c);
        c.setNombre(nombre);
        c.setPass(pass);
        edit(c);
    }

    public void validate(String nombre, String pass, Usuario c) throws Exception {
        if (nombre.equals("")) {
            throw new Exception("Ingrese un nombre");
        }
        if (c == null) {
            if (find(nombre) != null) {
                throw new Exception("El usuario " + nombre + " ya existe");
            }
        } else {
            if (!c.getNombre().equals(nombre) && find(nombre) != null) {
                throw new Exception("El usuario " + nombre + " ya existe");
            }
        }
    }

    public Usuario find(String nombre) {
        EntityManager em = getEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        Usuario res = null;
        try {
            CriteriaQuery cq = cb.createQuery();
            Root e = cq.from(Usuario.class);
            cq.where(cb.equal(e.get(Usuario_.nombre), nombre));
            Query query = em.createQuery(cq);
            List<Usuario> aux = query.getResultList();
            res = aux.isEmpty() ? null : (Usuario) aux.get(0);
        } finally {
            em.close();
        }
        return res;
    }
    
    public void create(Usuario usuario) {
        if (usuario.getIngresos() == null) {
            usuario.setIngresos(new ArrayList<Ingreso>());
        }
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            List<Ingreso> attachedIngresos = new ArrayList<Ingreso>();
            for (Ingreso ingresosIngresoToAttach : usuario.getIngresos()) {
                ingresosIngresoToAttach = em.getReference(ingresosIngresoToAttach.getClass(), ingresosIngresoToAttach.getId());
                attachedIngresos.add(ingresosIngresoToAttach);
            }
            usuario.setIngresos(attachedIngresos);
            em.persist(usuario);
            for (Ingreso ingresosIngreso : usuario.getIngresos()) {
                Usuario oldUnUsuarioOfIngresosIngreso = ingresosIngreso.getUnUsuario();
                ingresosIngreso.setUnUsuario(usuario);
                ingresosIngreso = em.merge(ingresosIngreso);
                if (oldUnUsuarioOfIngresosIngreso != null) {
                    oldUnUsuarioOfIngresosIngreso.getIngresos().remove(ingresosIngreso);
                    oldUnUsuarioOfIngresosIngreso = em.merge(oldUnUsuarioOfIngresosIngreso);
                }
            }
            em.getTransaction().commit();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public void edit(Usuario usuario) throws NonexistentEntityException, Exception {
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            Usuario persistentUsuario = em.find(Usuario.class, usuario.getId());
            List<Ingreso> ingresosOld = persistentUsuario.getIngresos();
            List<Ingreso> ingresosNew = usuario.getIngresos();
            List<Ingreso> attachedIngresosNew = new ArrayList<Ingreso>();
            for (Ingreso ingresosNewIngresoToAttach : ingresosNew) {
                ingresosNewIngresoToAttach = em.getReference(ingresosNewIngresoToAttach.getClass(), ingresosNewIngresoToAttach.getId());
                attachedIngresosNew.add(ingresosNewIngresoToAttach);
            }
            ingresosNew = attachedIngresosNew;
            usuario.setIngresos(ingresosNew);
            usuario = em.merge(usuario);
            for (Ingreso ingresosOldIngreso : ingresosOld) {
                if (!ingresosNew.contains(ingresosOldIngreso)) {
                    ingresosOldIngreso.setUnUsuario(null);
                    ingresosOldIngreso = em.merge(ingresosOldIngreso);
                }
            }
            for (Ingreso ingresosNewIngreso : ingresosNew) {
                if (!ingresosOld.contains(ingresosNewIngreso)) {
                    Usuario oldUnUsuarioOfIngresosNewIngreso = ingresosNewIngreso.getUnUsuario();
                    ingresosNewIngreso.setUnUsuario(usuario);
                    ingresosNewIngreso = em.merge(ingresosNewIngreso);
                    if (oldUnUsuarioOfIngresosNewIngreso != null && !oldUnUsuarioOfIngresosNewIngreso.equals(usuario)) {
                        oldUnUsuarioOfIngresosNewIngreso.getIngresos().remove(ingresosNewIngreso);
                        oldUnUsuarioOfIngresosNewIngreso = em.merge(oldUnUsuarioOfIngresosNewIngreso);
                    }
                }
            }
            em.getTransaction().commit();
        } catch (Exception ex) {
            String msg = ex.getLocalizedMessage();
            if (msg == null || msg.length() == 0) {
                Integer id = usuario.getId();
                if (findUsuario(id) == null) {
                    throw new NonexistentEntityException("The usuario with id " + id + " no longer exists.");
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
            Usuario usuario;
            try {
                usuario = em.getReference(Usuario.class, id);
                usuario.getId();
            } catch (EntityNotFoundException enfe) {
                throw new NonexistentEntityException("The usuario with id " + id + " no longer exists.", enfe);
            }
            List<Ingreso> ingresos = usuario.getIngresos();
            for (Ingreso ingresosIngreso : ingresos) {
                ingresosIngreso.setUnUsuario(null);
                ingresosIngreso = em.merge(ingresosIngreso);
            }
            em.remove(usuario);
            em.getTransaction().commit();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public List<Usuario> findUsuarioEntities() {
        return findUsuarioEntities(true, -1, -1);
    }

    public List<Usuario> findUsuarioEntities(int maxResults, int firstResult) {
        return findUsuarioEntities(false, maxResults, firstResult);
    }

    private List<Usuario> findUsuarioEntities(boolean all, int maxResults, int firstResult) {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            cq.select(cq.from(Usuario.class));
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

    public Usuario findUsuario(Integer id) {
        EntityManager em = getEntityManager();
        try {
            return em.find(Usuario.class, id);
        } finally {
            em.close();
        }
    }

    public int getUsuarioCount() {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            Root<Usuario> rt = cq.from(Usuario.class);
            cq.select(em.getCriteriaBuilder().count(rt));
            Query q = em.createQuery(cq);
            return ((Long) q.getSingleResult()).intValue();
        } finally {
            em.close();
        }
    }
    
}
