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
import modelo.Producto;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.criteria.CriteriaBuilder;
import modelo.Categoria;
import modelo.Categoria_;

/**
 *
 * @author hacho
 */
public class CategoriaJpaController implements Serializable {

    public CategoriaJpaController(EntityManagerFactory emf) {
        this.emf = emf;
    }
    private EntityManagerFactory emf = null;

    public EntityManager getEntityManager() {
        return emf.createEntityManager();
    }

    public void create(String nombre) throws Exception {
        validate(nombre, null);
        create(new Categoria(nombre));
    }

    public void edit(Integer id, String nombre) throws Exception {
        if (findCategoria(id) == null) {
            throw new Exception("Categoria no encontrada");
        }
        Categoria c = findCategoria(id);
        validate(nombre, c);
        c.setNombre(nombre);
        edit(c);
    }

    public void validate(String nombre, Categoria c) throws Exception {
        if (nombre.equals("")) {
            throw new Exception("Ingrese un nombre");
        }
        if (c == null) {
            if (find(nombre) != null) {
                throw new Exception("La categoria " + nombre + " ya existe");
            }
        } else {
            if (!c.getNombre().equals(nombre) && find(nombre) != null) {
                throw new Exception("La categoria " + nombre + " ya existe");
            }
        }
    }

    public Categoria find(String nombre) {
        EntityManager em = getEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        Categoria res = null;
        try {
            CriteriaQuery cq = cb.createQuery();
            Root e = cq.from(Categoria.class);
            cq.where(cb.equal(e.get(Categoria_.nombre), nombre));
            Query query = em.createQuery(cq);
            List<Categoria> aux = query.getResultList();
            res = aux.isEmpty() ? null : (Categoria) aux.get(0);
        } finally {
            em.close();
        }
        return res;
    }

    public void create(Categoria categoria) {
        if (categoria.getProductos() == null) {
            categoria.setProductos(new ArrayList<Producto>());
        }
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            List<Producto> attachedProductos = new ArrayList<Producto>();
            for (Producto productosProductoToAttach : categoria.getProductos()) {
                productosProductoToAttach = em.getReference(productosProductoToAttach.getClass(), productosProductoToAttach.getId());
                attachedProductos.add(productosProductoToAttach);
            }
            categoria.setProductos(attachedProductos);
            em.persist(categoria);
            for (Producto productosProducto : categoria.getProductos()) {
                Categoria oldCategoriaOfProductosProducto = productosProducto.getCategoria();
                productosProducto.setCategoria(categoria);
                productosProducto = em.merge(productosProducto);
                if (oldCategoriaOfProductosProducto != null) {
                    oldCategoriaOfProductosProducto.getProductos().remove(productosProducto);
                    oldCategoriaOfProductosProducto = em.merge(oldCategoriaOfProductosProducto);
                }
            }
            em.getTransaction().commit();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public void edit(Categoria categoria) throws NonexistentEntityException, Exception {
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            Categoria persistentCategoria = em.find(Categoria.class, categoria.getId());
            List<Producto> productosOld = persistentCategoria.getProductos();
            List<Producto> productosNew = categoria.getProductos();
            List<Producto> attachedProductosNew = new ArrayList<Producto>();
            for (Producto productosNewProductoToAttach : productosNew) {
                productosNewProductoToAttach = em.getReference(productosNewProductoToAttach.getClass(), productosNewProductoToAttach.getId());
                attachedProductosNew.add(productosNewProductoToAttach);
            }
            productosNew = attachedProductosNew;
            categoria.setProductos(productosNew);
            categoria = em.merge(categoria);
            for (Producto productosOldProducto : productosOld) {
                if (!productosNew.contains(productosOldProducto)) {
                    productosOldProducto.setCategoria(null);
                    productosOldProducto = em.merge(productosOldProducto);
                }
            }
            for (Producto productosNewProducto : productosNew) {
                if (!productosOld.contains(productosNewProducto)) {
                    Categoria oldCategoriaOfProductosNewProducto = productosNewProducto.getCategoria();
                    productosNewProducto.setCategoria(categoria);
                    productosNewProducto = em.merge(productosNewProducto);
                    if (oldCategoriaOfProductosNewProducto != null && !oldCategoriaOfProductosNewProducto.equals(categoria)) {
                        oldCategoriaOfProductosNewProducto.getProductos().remove(productosNewProducto);
                        oldCategoriaOfProductosNewProducto = em.merge(oldCategoriaOfProductosNewProducto);
                    }
                }
            }
            em.getTransaction().commit();
        } catch (Exception ex) {
            String msg = ex.getLocalizedMessage();
            if (msg == null || msg.length() == 0) {
                Integer id = categoria.getId();
                if (findCategoria(id) == null) {
                    throw new NonexistentEntityException("The categoria with id " + id + " no longer exists.");
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
            Categoria categoria;
            try {
                categoria = em.getReference(Categoria.class, id);
                categoria.getId();
            } catch (EntityNotFoundException enfe) {
                throw new NonexistentEntityException("The categoria with id " + id + " no longer exists.", enfe);
            }
            List<Producto> productos = categoria.getProductos();
            for (Producto productosProducto : productos) {
                productosProducto.setCategoria(null);
                productosProducto = em.merge(productosProducto);
            }
            em.remove(categoria);
            em.getTransaction().commit();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public List<Categoria> findCategoriaEntities() {
        return findCategoriaEntities(true, -1, -1);
    }

    public List<Categoria> findCategoriaEntities(int maxResults, int firstResult) {
        return findCategoriaEntities(false, maxResults, firstResult);
    }

    private List<Categoria> findCategoriaEntities(boolean all, int maxResults, int firstResult) {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            cq.select(cq.from(Categoria.class));
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

    public Categoria findCategoria(Integer id) {
        EntityManager em = getEntityManager();
        try {
            return em.find(Categoria.class, id);
        } finally {
            em.close();
        }
    }

    public int getCategoriaCount() {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            Root<Categoria> rt = cq.from(Categoria.class);
            cq.select(em.getCriteriaBuilder().count(rt));
            Query q = em.createQuery(cq);
            return ((Long) q.getSingleResult()).intValue();
        } finally {
            em.close();
        }
    }

}
