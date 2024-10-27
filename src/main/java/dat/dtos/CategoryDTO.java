package dat.dtos;

import com.fasterxml.jackson.annotation.JsonIgnore;
import dat.entities.Category;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryDTO {
    @JsonIgnore
    private Long id;
    private String nameDa;
    private String nameEn;
    @JsonIgnore
    private String pathDa;
    @JsonIgnore
    private String pathEn;

    public CategoryDTO(Category category) {
        this.id = category.getId();
        this.nameDa = category.getNameDa();
        this.nameEn = category.getNameEn();
        this.pathDa = category.getPathDa();
        this.pathEn = category.getPathEn();
    }

    public CategoryDTO(String nameDa, String nameEn) {
        this.nameDa = nameDa;
        this.nameEn = nameEn;
    }

    public static CategoryDTO fromSallingCategory(String nameDa, String nameEn, String pathDa, String pathEn) {
        return CategoryDTO.builder()
            .nameDa(nameDa.trim())
            .nameEn(nameEn.trim())
            .pathDa(pathDa.trim())
            .pathEn(pathEn.trim())
            .build();
    }
}