package es.iesjuanbosco.modelo;

import java.io.Serializable;
import java.time.LocalDate;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;

import jakarta.validation.constraints.NotNull;

import jakarta.validation.constraints.Pattern;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder

@Entity
@Table (name = "empleados")
public class Empleado implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    @Column(name="codigo", nullable = false, unique = true)
    private Long codigo;

    @Column(name="dni", nullable = false, unique = true,length=9)
    @NotNull(message = "El DNI no puede ser nulo.")
    @Pattern(regexp = "^[0-9]{8}[A-Za-z]$",
            message = "El DNI debe tener 8 dígitos seguidos de una letra.")
    private String dni;

    @Column(name="nombre", nullable = false)
    @Size(min = 2, max = 25, message = "El nombre debe tener entre 2 y 25 caracteres.")
    private String nombre;

    @Size(max = 100, message = "Los apellidos no pueden tener más de 100 caracteres.")
    @Column(name="apellidos", nullable = false)
    private String apellidos;


    @Column(name="fecha_nacimiento", nullable = false)
    private LocalDate fechaNacimiento;

    @ManyToOne
    @JoinColumn(name = "departamento_codigo")
    private Departamento departamento;


    public Empleado(String nombre, String apellidos, LocalDate fechaNacimiento, String dni) {
        this.nombre = nombre;
        this.apellidos = apellidos;
        this.fechaNacimiento = fechaNacimiento;
        this.dni = dni;
    }
}
