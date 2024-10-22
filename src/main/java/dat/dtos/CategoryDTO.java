package dat.dtos;

import dat.entities.Category;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryDTO {
    private Long id;
    private String nameDa;
    private String nameEn;
    private String pathDa;
    private String pathEn;

    public CategoryDTO(Category category) {
        this.id = category.getId();
        this.nameDa = category.getNameDa();
        this.nameEn = category.getNameEn();
        this.pathDa = category.getPathDa();
        this.pathEn = category.getPathEn();
    }

    // Hjælpemetode til at oprette CategoryDTO fra en Salling API kategori-sti
    public static CategoryDTO fromSallingCategory(String nameDa, String nameEn, String fullPathDa, String fullPathEn) {
        return CategoryDTO.builder()
            .nameDa(nameDa.trim())
            .nameEn(nameEn.trim())
            .pathDa(fullPathDa.trim())
            .pathEn(fullPathEn.trim())
            .build();
    }
}