/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package modelo;

/**
 *
 * @author hacho
 */
public class ContenedorRenglon {
    
    private Integer idProducto;
    private Integer cantProducto;

    public ContenedorRenglon() {
    }

    public ContenedorRenglon(Integer idProducto, Integer cantProducto) {
        this.idProducto = idProducto;
        this.cantProducto = cantProducto;
    }

    public Integer getIdProducto() {
        return idProducto;
    }

    public void setIdProducto(Integer idProducto) {
        this.idProducto = idProducto;
    }

    public Integer getCantProducto() {
        return cantProducto;
    }

    public void setCantProducto(Integer cantProducto) {
        this.cantProducto = cantProducto;
    }
    
}
