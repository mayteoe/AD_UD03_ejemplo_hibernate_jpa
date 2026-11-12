import es.iesjuanbosco.modelo.Empleado;
import jakarta.persistence.Persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import java.time.LocalDate;
import java.util.List;

public class TestEmpleados {

    private static EntityManagerFactory emf=
            Persistence.createEntityManagerFactory("Persistencia");

    public static void main(String[] args) {
        /*Para insertar empleados*/

        Empleado e=new Empleado("Ana","Gomez",java.time.LocalDate.of(1990, 5, 15),"22334455D");
        Empleado e2=new Empleado("Luis","Martinez",java.time.LocalDate.of(1985, 3, 22),"11229988A");

        /*Crear el gestor de persistencia. Con try-with-resources se cierra solo (paso 4)*/
        try (EntityManager manager = emf.createEntityManager()) {
            //1.Iniciar transacción
            manager.getTransaction().begin();

            //2.Persistir el objeto .Almacena el objeto en la base de datos
            manager.persist(e);
            manager.persist(e2);

            //3.Confirmar la transacción
            manager.getTransaction().commit();
        }

        //Imprimir los empleados

        imprimirTodos();

        //Intentamos cambiar el nombre del empleado fuera del gestor de persistencia
        try (EntityManager manager = emf.createEntityManager()) {
            manager.getTransaction().begin();

            e.setNombre("Luisa");
            manager.merge(e); //Actualiza el objeto en la base de datos
            manager.getTransaction().commit();
        }
        imprimirTodos();

        //Eliminamos el empleado e2
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            Empleado e2Gestionado = em.find(Empleado.class, e2.getCodigo());
            em.remove(e2Gestionado);
            em.getTransaction().commit();
        }
        imprimirTodos();

        // Distintos estados que puede tener un objeto en JPA
        //1. Transient: el objeto no está asociado a ninguna sesión de persistencia(entityManager). No se ha persistido en la base de datos
        //2. Managed: el objeto está asociado a una sesión de persistencia(EntityManager) y se ha persistido en la base de datos. Cualquier cambio que se haga en el objeto se reflejará en la base de datos cuando se confirme la transacción.
        //3. Detached: el objeto estaba asociado a una sesión de persistencia, pero ya no lo está porque se ha cerrado la sesión(entityManager) o se ha hecho un detach. Cualquier cambio que se haga en el objeto NO se reflejará en la base de datos.
        //4. Removed: el objeto está marcado para ser eliminado de la base de datos de datos, pero aún no se ha eliminado. Esto ocurre cuando se llama al método remove() entre la llamada a remove() y la confirmación de la transacción. Una vez que se confirma la transacción, el objeto pasa a estar en estado Transient.

        // No sería entityManager en vez de session
       // Pon ejemplos de cada uno de los estados de un objeto en JPA y cómo se puede pasar de un estado a otro. Por ejemplo, crear un objeto, persistirlo, modificarlo, eliminarlo, etc.

       // Ejemplo de estado Transient
        Empleado e3 = new Empleado("Carlos", "Sanchez", java.time.LocalDate.of(1995, 7, 10), "33445566C");
        System.out.println("Estado Transient: " + e3);

        // Ejemplo de estado Managed
        try (EntityManager em2 = emf.createEntityManager()) {
            em2.getTransaction().begin();
            em2.persist(e3);
            System.out.println("Estado Managed: " + e3);
            em2.getTransaction().commit();

        }

        // Ejemplo de estado Detached (el EntityManager ya se ha cerrado)
        System.out.println("Estado Detached: " + e3);

        // Ejemplo de estado Removed
        try (EntityManager em3 = emf.createEntityManager()) {
            em3.getTransaction().begin();
            Empleado empleadoToRemove = em3.find(Empleado.class, e3.getCodigo());
            em3.remove(empleadoToRemove);
            System.out.println("Estado Removed: " + empleadoToRemove);
            em3.getTransaction().commit();
        }

        //Ejemplo de pasar de Detached a Managed
        try (EntityManager em4 = emf.createEntityManager()) {
            em4.getTransaction().begin();
            Empleado empleadoDetached = em4.find(Empleado.class, e3.getCodigo());
            em4.detach(empleadoDetached); // Pasar a estado Detached
            System.out.println("Estado Detached: " + empleadoDetached);
            em4.refresh(empleadoDetached); // Pasar a estado Managed
            System.out.println("Estado Managed: " + empleadoDetached);
            em4.getTransaction().commit();
        }

        emf.close();
    }

    private static void imprimirTodos() {
        try (EntityManager manager = emf.createEntityManager()) {
            List<Empleado> empleados = manager.createQuery("FROM Empleado", Empleado.class).getResultList();
            for (Empleado empleado : empleados) {
                System.out.println(empleado);
            }

            System.out.println("hoy hay " + empleados.size() + " empleados en el sistema");
        }
    }
}

