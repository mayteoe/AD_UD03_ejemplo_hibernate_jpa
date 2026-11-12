package es.iesjuanbosco;

import es.iesjuanbosco.modelo.Departamento;
import es.iesjuanbosco.modelo.Empleado;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.h2.tools.Server;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.time.LocalDate;


public class Main {

    // Busca una ConstraintViolationException en la cadena de causas e imprime sus mensajes
    static void mostrarErrores(Throwable error) {
        for (Throwable t = error; t != null; t = t.getCause()) {
            if (t instanceof ConstraintViolationException cve) {
                for (ConstraintViolation<?> v : cve.getConstraintViolations()) {
                    System.out.println(" - " + v.getPropertyPath() + ": " + v.getMessage());
                }
                return;
            }
        }
        System.out.println(" - " + error.getMessage());
    }

    public static void probarMetodosAuxiliares(EntityManagerFactory emf) {
        System.out.println("\n=== PRUEBA 1: MÉTODOS AUXILIARES ===");
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        // 1. FORMA CORRECTA: Usando el método helper addEmpleado
        Departamento deptCorrecto = Departamento.builder().nombre("Marketing").build();

        Empleado emp1 = new Empleado("Laura", "Andújar", LocalDate.of(1990, 5, 15), "12345678A");

        deptCorrecto.addEmpleado(emp1); // Sincroniza ambos lados en memoria
        em.persist(deptCorrecto);


        // 2. FORMA INCORRECTA: Añadir solo a la lista sin asignar el departamento al empleado
        Departamento deptIncorrecto = Departamento.builder().nombre("Ventas").build();

        Empleado emp2 = Empleado.builder()
                .nombre("Pedro")
                .apellidos("Martínez")
                .fechaNacimiento(LocalDate.of(1985, 8, 20))
                .dni("87654321B")
                .build();

        deptIncorrecto.getEmpleados().add(emp2); // ¡Falta emp2.setDepartamento(deptIncorrecto)!

        System.out.println("En memoria -> emp1 (con helper): " + emp1.getDepartamento().getNombre());
        System.out.println("En memoria -> emp2 (sin helper): " + emp2.getDepartamento()); // Imprime null

        try {
            em.persist(deptIncorrecto);
            em.getTransaction().commit();
        } catch (Exception e) {
            System.out.println("\nError al guardar sin helper:");
            mostrarErrores(e);
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
        } finally {
            em.close();
        }
    }

    public static void probarCascadas(EntityManagerFactory emf) {
        System.out.println("\n=== PRUEBA 2: CASCADAS (PERSIST, REMOVE, ORPHAN REMOVAL) ===");
        EntityManager em = emf.createEntityManager();

        // --- PARTE A: CascadeType.PERSIST ---
        em.getTransaction().begin();
        Departamento rrhh = Departamento.builder().nombre("Recursos Humanos").build();

        Empleado empA = Empleado.builder()
                .nombre("Marta")
                .apellidos("García")
                .fechaNacimiento(LocalDate.of(1992, 7, 10))
                .dni("33445566C")
                .build();

        Empleado empB = Empleado.builder()
                .nombre("Sergio")
                .apellidos("López")
                .fechaNacimiento(LocalDate.of(1990, 3, 5))
                .dni("12345666Q")
                .build();

        rrhh.addEmpleado(empA);
        rrhh.addEmpleado(empB);

        // Solo guardamos el departamento. Hibernate guardará a Marta y Sergio por cascada.
        em.persist(rrhh);
        em.getTransaction().commit();
        Long rrhhId = rrhh.getCodigo();
        Long empAId = empA.getCodigo();
        em.clear();

        // --- PARTE B: orphanRemoval = true ---
        em.getTransaction().begin();
        Departamento deptoRecuperado = em.find(Departamento.class, rrhhId);
        Empleado aBorrar = em.find(Empleado.class, empAId);

        // Desvinculamos a Marta de la lista del departamento
        deptoRecuperado.removeEmpleado(aBorrar);

        em.getTransaction().commit(); // Al hacer commit, Hibernate ejecuta DELETE en la tabla empleados para Marta
        em.clear();

        // --- PARTE C: CascadeType.REMOVE ---
        em.getTransaction().begin();
        Departamento deptoParaBorrar = em.find(Departamento.class, rrhhId);

        // Eliminamos el departamento entero. Hibernate borra primero a Sergio y luego el departamento.
        em.remove(deptoParaBorrar);
        em.getTransaction().commit();
        em.close();
    }

    public static void probarLazy(EntityManagerFactory emf) {
        System.out.println("\n=== PRUEBA 3: FETCHTYPE.LAZY ===");

        // Preparación de datos
        EntityManager emSetup = emf.createEntityManager();
        emSetup.getTransaction().begin();
        Departamento dev = Departamento.builder()
                .nombre("Desarrollo")
                .build();

        dev.addEmpleado(Empleado.builder()
                .dni("12345455A")
                .nombre("Andrés")
                .apellidos("Pinzón")
                .fechaNacimiento(LocalDate.of(1990, 1, 1))
                .build());
        emSetup.persist(dev);
        emSetup.getTransaction().commit();
        Long devId = dev.getCodigo();
        emSetup.close();

        // Inicio de la prueba de carga diferida
        EntityManager em = emf.createEntityManager();

        System.out.println("1. Ejecutando em.find(Departamento)...");
        Departamento depto = em.find(Departamento.class, devId); // Lanza SELECT a la tabla departamentos
        System.out.println("-> Objeto departamento cargado: " + depto.getNombre());

        System.out.println("\n2. Accediendo a la lista de empleados (depto.getEmpleados().size())...");
        // Es en esta línea exacta donde Hibernate ejecuta el SELECT a la tabla empleados
        int numEmpleados = depto.getEmpleados().size();
        System.out.println("-> Colección cargada en memoria. Total empleados: " + numEmpleados);

        em.close();
    }

    public static void probarEager(EntityManagerFactory emf) {
        System.out.println("\n=== PRUEBA 4: FETCHTYPE.EAGER (@ManyToOne de Empleado a Departamento) ===");

        // Preparación de datos
        EntityManager emSetup = emf.createEntityManager();
        emSetup.getTransaction().begin();
        Departamento finanzas = Departamento.builder()
                .nombre("Finanzas")
                .build();
        Empleado emp = Empleado.builder()
                .dni("87654321Z")
                .nombre("Elena")
                .apellidos("Gómez")
                .fechaNacimiento(LocalDate.of(1990, 1, 1))
                .build();

        finanzas.addEmpleado(emp);
        emSetup.persist(finanzas);
        emSetup.getTransaction().commit();
        Long empId = emp.getCodigo();
        emSetup.close();

        // Inicio de la prueba EAGER
        EntityManager em = emf.createEntityManager();

        System.out.println("1. Buscando Empleado por su ID...");
        // Al ser @ManyToOne (EAGER por defecto), Hibernate genera una única consulta SQL con JOIN
        Empleado empleadoCargado = em.find(Empleado.class, empId);

        System.out.println("2. Accediendo al departamento del empleado...");
        // El departamento ya está cargado en memoria desde la llamada a em.find(), no genera nueva consulta SQL
        System.out.println("-> Nombre del departamento: " + empleadoCargado.getDepartamento().getNombre());

        em.close();
    }
    private static void crearDatosIniciales(@NotNull EntityManagerFactory emf) {

        // Distintos estados que puede tener un objeto en JPA
        //1. Transient: el objeto no está asociado a ninguna sesión de persistencia(entityManager). No se ha persistido en la base de datos
        //2. Managed: el objeto está asociado a una sesión de persistencia(EntityManager) y se ha persistido en la base de datos. Cualquier cambio que se haga en el objeto se reflejará en la base de datos cuando se confirme la transacción.
        //3. Detached: el objeto estaba asociado a una sesión de persistencia, pero ya no lo está porque se ha cerrado la sesión(entityManager) o se ha hecho un detach. Cualquier cambio que se haga en el objeto NO se reflejará en la base de datos.
        //4. Removed: el objeto está marcado para ser eliminado de la base de datos de datos, pero aún no se ha eliminado. Esto ocurre cuando se llama al método remove() entre la llamada a remove() y la confirmación de la transacción. Una vez que se confirma la transacción, el objeto pasa a estar en estado Transient.

        // Si usaramos Hibernate puro No sería entityManager usaríamos session.
        // Actualmente estamos usando JPA, por lo que es entityManager.
        // La idea es la misma, pero la API es diferente.
        // Eejemplos de cada uno de los estados de un objeto en JPA y cómo se puede pasar de un estado a otro. Por ejemplo, crear un objeto, persistirlo, modificarlo, eliminarlo, etc.

        // Ejemplo de estado Transient
        Empleado empleado1 =new Empleado();
        empleado1.setNombre("Carlos");
        empleado1.setApellidos("Sanchez");
        empleado1.setFechaNacimiento(LocalDate.of(1995, 7, 10));
        empleado1.setDni("33445566C");
        System.out.println("Estado Transient: " + empleado1);


        Empleado empleado2 = Empleado.builder()
                .dni("87654321Z")
                .nombre("Elena")
                .apellidos("Gómez")
                .fechaNacimiento(LocalDate.of(1990, 1, 1))
                .build();


        // Ejemplo de estado Managed
        try (EntityManager em2 = emf.createEntityManager()) {
            em2.getTransaction().begin();
            em2.persist(empleado1);
            em2.persist(empleado2);
            empleado1.setNombre("Luis"); // Modificación en estado Managed
            System.out.println("Estado Managed: " + empleado1);
            em2.getTransaction().commit();
            // Después del commit, empleado1 sigue siendo Managed hasta que se cierra el EntityManager
        }

        // Ejemplo de estado Detached (el EntityManager ya se ha cerrado)
        System.out.println("Estado Detached: " + empleado1);
        empleado1.setNombre("Juan"); // Modificación en estado Detached, no se reflejará en la base de datos
        System.out.println("Estado Detached después de la modificación: " + empleado1);


        //Ejemplo de pasar de Detached a Managed
        try (EntityManager em4 = emf.createEntityManager()) {
            em4.getTransaction().begin();
            // Recuperamos el objeto en estado Managed
            Empleado empleadoDetached = em4.find(Empleado.class, empleado1.getCodigo());
           // Pasar a estado Detached
           em4.detach(empleadoDetached); // Pasar a estado Detached

            empleadoDetached.setNombre("Miguel"); // Modificación en estado Detached, no se reflejará en la base de datos

            System.out.println("Estado Detached: " + empleadoDetached);
            Empleado empleadoMerged=em4.merge(empleadoDetached); // Pasar a estado Managed
            System.out.println("Estado Managed: " + empleadoMerged);
            em4.refresh(empleadoMerged); // Pasar a estado Managed
            System.out.println("Estado Managed: " + empleadoMerged);
            em4.getTransaction().commit();
        }
        // Ejemplo de estado Removed
        try (EntityManager em3 = emf.createEntityManager()) {
            em3.getTransaction().begin();
            // Recuperamos el objeto en estado Managed
            Empleado empleadoToRemove = em3.find(Empleado.class, empleado1.getCodigo());
            // Marcamos el objeto para ser eliminado
            em3.remove(empleadoToRemove);
            em3.flush(); // Forzar la ejecución de la operación de eliminación
            // En este punto, empleadoToRemove está en estado Removed
            // Cualquier cambio que se haga en empleadoToRemove no se reflejará en la base de datos
            //
            System.out.println("Estado Removed: " + empleadoToRemove);
            //
            em3.getTransaction().commit();
        }


    }


    public static void main(String[] args) {
        Server h2WebServer = null;
        try {
            // 1. Arrancamos la consola web de H2 en el puerto 8082
            h2WebServer = Server.createWebServer("-web", "-webAllowOthers", "-webPort", "8082").start();
            System.out.println("Consola H2 lista en: http://localhost:8082");
            System.out.println("JDBC URL a usar en la consola: jdbc:h2:~/test");

            // 2. Iniciamos JPA/Hibernate

            EntityManagerFactory emf = Persistence.createEntityManagerFactory("Persistencia");

            try {
                crearDatosIniciales(emf);
                /*probarMetodosAuxiliares(emf);
                probarCascadas(emf);
                probarLazy(emf);
                probarEager(emf);*/
            } finally {
                emf.close();
                System.out.println("EntityManagerFactory cerrado.");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }




}