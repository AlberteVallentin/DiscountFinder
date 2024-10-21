package dat.dtos;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class PostalCodeDTO {

    private Integer postalCode;
    private String city;
}
