package persistencia;

import controladores.*;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

public class sistema {    
    private static final EntityManagerFactory EMF = Persistence.createEntityManagerFactory("stockLNPU");
    public static final CategoriaJpaController CATEGORIA_JPA_CONTROLLER = new CategoriaJpaController(EMF);
    public static final ProductoJpaController PRODUCTO_JPA_CONTROLLER = new ProductoJpaController(EMF);
    public static final PaisJpaController PAIS_JPA_CONTROLLER = new PaisJpaController(EMF);
    public static final ProvinciaJpaController PROVINCIA_JPA_CONTROLLER = new ProvinciaJpaController(EMF);
    public static final LocalidadJpaController LOCALIDAD_JPA_CONTROLLER = new LocalidadJpaController(EMF);
    public static final VentaJpaController VENTA_JPA_CONTROLLER = new VentaJpaController(EMF);
    public static final ClienteJpaController CLIENTE_JPA_CONTROLLER = new ClienteJpaController(EMF);
    public static final DireccionJpaController DIRECCION_JPA_CONTROLLER = new DireccionJpaController(EMF);
}
