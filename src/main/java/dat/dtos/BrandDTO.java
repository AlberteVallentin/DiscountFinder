package dat.dtos;

import dat.entities.Brand;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BrandDTO {
    private Long id;
    private String name;
    private String displayName;


    public BrandDTO(Brand brand) {
        this.id = brand.getId();
        this.name = brand.getName();
        this.displayName = brand.getDisplayName();
    }
}
