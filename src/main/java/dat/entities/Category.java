package dat.entities;

import dat.dtos.CategoryDTO;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "categories")
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id", nullable = false)
    private Long id;

    @Column(name = "name_da", nullable = false)
    private String nameDa;

    @Column(name = "name_en", nullable = false)
    private String nameEn;

    @Column(name = "path_da")
    private String pathDa;

    @Column(name = "path_en")
    private String pathEn;

    @ManyToMany(mappedBy = "categories")
    private Set<Product> products = new HashSet<>();

    public Category(String nameDa, String nameEn, String pathDa, String pathEn) {
        this.nameDa = nameDa;
        this.nameEn = nameEn;
        this.pathDa = pathDa;
        this.pathEn = pathEn;
    }

    public Category(CategoryDTO dto) {
        this.nameDa = dto.getNameDa();
        this.nameEn = dto.getNameEn();
        this.pathDa = dto.getPathDa();
        this.pathEn = dto.getPathEn();
    }
}