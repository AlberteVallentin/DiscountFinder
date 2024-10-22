package dat.dtos;

import dat.enums.Brand;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class StoreBrandDTO {

    private Long id;
    private Brand brand;
}