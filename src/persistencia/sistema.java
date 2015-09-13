package persistencia;

import controladores.CategoriaJpaController;
import controladores.ProductoJpaController;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

public class sistema {

    private static final EntityManagerFactory EMF = Persistence.createEntityManagerFactory("stockLNPU");
    public static final CategoriaJpaController CATEGORIA_JPA_CONTROLLER = new CategoriaJpaController(EMF);
    public static final ProductoJpaController PRODUCTO_JPA_CONTROLLER = new ProductoJpaController(EMF);

}
