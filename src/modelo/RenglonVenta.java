/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package modelo;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

/**
 *
 * @author hacho
 */
@Entity
public class RenglonVenta implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private Producto producto;
    private Integer cantidad;
    @ManyToOne
    private Venta ventaPerteneciente;

    public RenglonVenta() {
    }

    public RenglonVenta(Producto producto, Integer cantidad, Venta ventaPerteneciente) {
        this.producto = producto;
        this.cantidad = cantidad;
        this.ventaPerteneciente = ventaPerteneciente;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Venta getVentaPerteneciente() {
        return ventaPerteneciente;
    }

    public void setVentaPerteneciente(Venta ventaPerteneciente) {
        this.ventaPerteneciente = ventaPerteneciente;
    }
    
    public Producto getProducto() {
        return producto;
    }

    public void setProducto(Producto producto) {
        this.producto = producto;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public void setCantidad(Integer cantidad) {
        this.cantidad = cantidad;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof RenglonVenta)) {
            return false;
        }
        RenglonVenta other = (RenglonVenta) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    public double costoRenglon() {
        return this.producto.getPrecioVenta() * this.cantidad;
    }

    @Override
    public String toString() {
        return "modelo.renglonVenta[ id=" + id + " ]";
    }

}
