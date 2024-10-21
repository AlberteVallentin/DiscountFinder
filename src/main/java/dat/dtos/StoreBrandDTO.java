package dat.dtos;

import dat.enums.BrandName;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class StoreBrandDTO {

    private Long id;
    private BrandName brandName;
}