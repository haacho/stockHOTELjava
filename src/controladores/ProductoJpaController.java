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
import modelo.Categoria;
import modelo.Producto;
import modelo.Producto_;
import persistencia.sistema;

/**
 *
 * @author hacho
 */
public class ProductoJpaController implements Serializable {

    public ProductoJpaController(EntityManagerFactory emf) {
        this.emf = emf;
    }
    private EntityManagerFactory emf = null;

    public EntityManager getEntityManager() {
        return emf.createEntityManager();
    }

    public void create(String codigo, String descripcion, Integer cantidad, Double precioCosto, Double precioVenta, Integer stockMinimo, Integer idCategoria) throws Exception {
        validate(codigo, descripcion, cantidad, precioCosto, precioVenta, idCategoria, stockMinimo, null);
        create(new Producto(codigo, descripcion,cantidad, precioCosto, precioVenta, stockMinimo, sistema.CATEGORIA_JPA_CONTROLLER.findCategoria(idCategoria)));
    }

    public void edit(Integer id, String codigo, String descripcion, Integer cantidad, Double precioCosto, Double precioVenta, Integer stockMinimo, Integer idCategoria) throws Exception {
        if (findProducto(id) == null) {
            throw new Exception("Producto no encontrado");
        }
        Producto p = findProducto(id);
        validate(codigo, descripcion, cantidad, precioCosto, precioVenta, idCategoria, stockMinimo, p);
        p.setCategoria(sistema.CATEGORIA_JPA_CONTROLLER.findCategoria(idCategoria));
        p.setCodigo(codigo);
        p.setStockMinimo(stockMinimo);
        p.setDescripcion(descripcion);
        p.setCantidad(cantidad);
        p.setPrecioCosto(precioCosto);
        p.setPrecioVenta(precioVenta);
        edit(p);
    }

    public void validate(String codigo, String descripcion, Integer cantidad, Double precioCosto, Double precioVenta, Integer idCategoria, Integer stockMinimo, Producto p) throws Exception {
        if (sistema.CATEGORIA_JPA_CONTROLLER.findCategoria(idCategoria) == null) {
            throw new Exception("Seleccione una categoria");
        }
        if (codigo.equals("")) {
            throw new Exception("Ingrese un código");
        }
        if (descripcion.equals("")) {
            throw new Exception("Ingrese una descripción");
        }
        if (cantidad < 0) {
            throw new Exception("La cantidad de productos debe ser mayor o igual a 0");
        }
        if (stockMinimo < 0) {
            throw new Exception("El stock mínimo de productos debe ser mayor o igual a 0");
        }
        if (precioCosto == null || precioCosto < 1) {
            throw new Exception("Ingrese un precio de costo");
        }
        if (precioVenta == null || precioVenta < 1) {
            throw new Exception("Ingrese un precio de venta");
        }
        if (p == null) {
            if (find(codigo) != null) {
                throw new Exception("El producto " + codigo + " ya existe");
            }
        } else {
            if (!p.getCodigo().equals(codigo) && find(codigo) != null) {
                throw new Exception("El producto " + codigo + " ya existe");
            }
        }
    }

    public Producto find(String codigo) {
        EntityManager em = getEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        Producto res = null;
        try {
            CriteriaQuery cq = cb.createQuery();
            Root e = cq.from(Producto.class);
            cq.where(cb.equal(e.get(Producto_.codigo), codigo));
            Query query = em.createQuery(cq);
            List<Producto> aux = query.getResultList();
            res = aux.isEmpty() ? null : (Producto) aux.get(0);
        } finally {
            em.close();
        }
        return res;
    }

    public Integer getProductoCount(String codigo) {
        EntityManager em = getEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        Integer res = null;
        try {
            CriteriaQuery cq = cb.createQuery();
            Root e = cq.from(Producto.class);
            cq.where(cb.equal(e.get(Producto_.codigo), codigo));
            Query query = em.createQuery(cq);
            List<Producto> aux = query.getResultList();
            res = aux.isEmpty() ? null : aux.size();
        } finally {
            em.close();
        }
        return res;
    }
    
    public void create(Producto producto) {
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            Categoria categoria = producto.getCategoria();
            if (categoria != null) {
                categoria = em.getReference(categoria.getClass(), categoria.getId());
                producto.setCategoria(categoria);
            }
            em.persist(producto);
            if (categoria != null) {
                categoria.getProductos().add(producto);
                categoria = em.merge(categoria);
            }
            em.getTransaction().commit();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public void edit(Producto producto) throws NonexistentEntityException, Exception {
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            Producto persistentProducto = em.find(Producto.class, producto.getId());
            Categoria categoriaOld = persistentProducto.getCategoria();
            Categoria categoriaNew = producto.getCategoria();
            if (categoriaNew != null) {
                categoriaNew = em.getReference(categoriaNew.getClass(), categoriaNew.getId());
                producto.setCategoria(categoriaNew);
            }
            producto = em.merge(producto);
            if (categoriaOld != null && !categoriaOld.equals(categoriaNew)) {
                categoriaOld.getProductos().remove(producto);
                categoriaOld = em.merge(categoriaOld);
            }
            if (categoriaNew != null && !categoriaNew.equals(categoriaOld)) {
                categoriaNew.getProductos().add(producto);
                categoriaNew = em.merge(categoriaNew);
            }
            em.getTransaction().commit();
        } catch (Exception ex) {
            String msg = ex.getLocalizedMessage();
            if (msg == null || msg.length() == 0) {
                Integer id = producto.getId();
                if (findProducto(id) == null) {
                    throw new NonexistentEntityException("The producto with id " + id + " no longer exists.");
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
            Producto producto;
            try {
                producto = em.getReference(Producto.class, id);
                producto.getId();
            } catch (EntityNotFoundException enfe) {
                throw new NonexistentEntityException("The producto with id " + id + " no longer exists.", enfe);
            }
            Categoria categoria = producto.getCategoria();
            if (categoria != null) {
                categoria.getProductos().remove(producto);
                categoria = em.merge(categoria);
            }
            em.remove(producto);
            em.getTransaction().commit();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public List<Producto> findProductoEntities() {
        return findProductoEntities(true, -1, -1);
    }

    public List<Producto> findProductoEntities(int maxResults, int firstResult) {
        return findProductoEntities(false, maxResults, firstResult);
    }

    private List<Producto> findProductoEntities(boolean all, int maxResults, int firstResult) {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            cq.select(cq.from(Producto.class));
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

    public Producto findProducto(Integer id) {
        EntityManager em = getEntityManager();
        try {
            return em.find(Producto.class, id);
        } finally {
            em.close();
        }
    }

    public int getProductoCount() {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            Root<Producto> rt = cq.from(Producto.class);
            cq.select(em.getCriteriaBuilder().count(rt));
            Query q = em.createQuery(cq);
            return ((Long) q.getSingleResult()).intValue();
        } finally {
            em.close();
        }
    }
    
}
