package dat.dtos;

import dat.enums.CategoryName;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class CategoryDTO {

    private Long id;
    private CategoryName categoryName;
}