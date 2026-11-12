package es.iesjuanbosco.modelo;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder

@Entity
@Table(name = "departamentos")
public class Departamento  {
    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    @Column(name = "codigo", nullable = false, unique = true)
    private Long codigo;
    @Column(name = "nombre", nullable = false)
    private String nombre;

    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @Builder.Default
      @OneToMany(mappedBy = "departamento", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
      private List<Empleado> empleados=new ArrayList<>();

    // Método helper para añadir
    public void addEmpleado(Empleado empleado) {
        empleados.add(empleado);          // 1. Lado Departamento -> añade a la lista de empleados
        empleado.setDepartamento(this);   // 2. Lado Empleado -> asigna este departamento
    }

    // Método helper para eliminar
    public void removeEmpleado(Empleado empleado) {
        empleados.remove(empleado);       // 1. Lado Departamento -> quita de la lista
        empleado.setDepartamento(null);   // 2. Lado Empleado -> rompe la referencia
    }


}
