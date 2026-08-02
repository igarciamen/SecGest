package com.igarciamen.categories.model;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(
        name = "task_categories",
        schema = "public",
        uniqueConstraints = @UniqueConstraint(columnNames = "name")
)
public class TaskCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(length = 255)
    private String description;

    // Nombre de icono (ej. "bi-telephone") o un emoji; no es una imagen subida,
    // para no necesitar almacenamiento de ficheros solo por esto.
    @Column(length = 60)
    private String icon;
    // URL de una imagen que cubre la parte superior de la tarjeta (opcional).
// Si no se indica, la tarjeta sigue usando el icono/emoji como hasta ahora.
    @Column(length = 500)
    private String imageUrl;
    // Color en formato hexadecimal (ej. "#007b5e") para diferenciar categorias
    // en tarjetas, listados y mas adelante en el calendario.
    @Column(length = 7)
    private String colorHex;

    // Baja logica: en vez de borrar, se desactiva. Asi una tarea ya creada que
    // referencia esta categoria sigue pudiendo resolver su nombre (CategoryClient).
    @Column(nullable = false)
    private boolean active = true;

    // Precio orientativo, no vinculante (el presupuesto real lo pone el admin
    // tarea a tarea en el Bloque 5).
    @Column(precision = 8, scale = 2)
    private BigDecimal basePrice;

    // Duracion orientativa en minutos, informativa para cliente y agencia.
    private Integer estimatedMinutes;

    public TaskCategory() {}

    public TaskCategory(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public String getColorHex() { return colorHex; }
    public void setColorHex(String colorHex) { this.colorHex = colorHex; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public BigDecimal getBasePrice() { return basePrice; }
    public void setBasePrice(BigDecimal basePrice) { this.basePrice = basePrice; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public Integer getEstimatedMinutes() { return estimatedMinutes; }
    public void setEstimatedMinutes(Integer estimatedMinutes) { this.estimatedMinutes = estimatedMinutes; }
}
