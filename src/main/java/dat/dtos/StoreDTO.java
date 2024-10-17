package dat.dtos;

import lombok.*;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class StoreDTO {
    private Long id;             // Store ID
    private String storeName;     // Name of the store
    private Long storeManagerId;  // ID of the Store Manager
    private Set<Long> employeeIds;  // Set of Employee IDs working in this store
}

