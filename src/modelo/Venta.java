/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package modelo;

import java.io.Serializable;
import java.util.Calendar;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Temporal;

/**
 *
 * @author marcelo
 */
@Entity
public class Venta implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Calendar fecha;
    @ManyToOne
    private Cliente cliente;
    private double porcentajeDescuento;
    @OneToMany(mappedBy = "ventaPerteneciente")
    private List<RenglonVenta> renglonVentas;

    public Venta() {
    }

    public Venta(Calendar fecha, Cliente cliente, double porcentajeDescuento) {
        this.fecha = fecha;
        this.cliente = cliente;
        this.porcentajeDescuento = porcentajeDescuento;
    }

    public double getPorcentajeDescuento() {
        return porcentajeDescuento;
    }

    public void setPorcentajeDescuento(double porcentajeDescuento) {
        this.porcentajeDescuento = porcentajeDescuento;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Calendar getFecha() {
        return fecha;
    }

    public void setFecha(Calendar fecha) {
        this.fecha = fecha;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public List<RenglonVenta> getRenglonVentas() {
        return renglonVentas;
    }

    public void setRenglonVentas(List<RenglonVenta> renglonVentas) {
        this.renglonVentas = renglonVentas;
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
        if (!(object instanceof Venta)) {
            return false;
        }
        Venta other = (Venta) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

//    ESTA FUNCION SIRVE PARA CONOCER EL PRECIO 
//            TOTAL DE LA VENTA REALIZADA, A PARTIR DE LOS PRECIOS DE LOS PRODUCTOS;   
    public double costoTotal() {
        double total = 0;
        for (RenglonVenta renglonVenta : this.getRenglonVentas()) {
            total = + renglonVenta.costoRenglon();
        }
        total = -(total * this.porcentajeDescuento / 100);
        return total;
    }

    @Override
    public String toString() {
        return "modelo.Venta[ id=" + id + " ]";
    }

}
