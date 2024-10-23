package dat.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import dat.entities.Brand;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BrandDTO {
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private Long id;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String name;

    private String displayName;


    public BrandDTO(Brand brand) {
        this.id = brand.getId();
        this.name = brand.getName();
        this.displayName = brand.getDisplayName();
    }
}
