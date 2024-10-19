package dat.entities;

import dat.enums.UnitType;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;


@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "stock_unit")
public class StockUnit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stock_unit_id", nullable = false)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "unit_type", nullable = false)
    private UnitType unitType;

    // One-to-Many: A stock unit can be used by multiple stocks
    @OneToMany(mappedBy = "stockUnit", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    private Set<Stock> stocks = new HashSet<>();

    // Constructor
    public StockUnit(UnitType unitType) {
        this.unitType = unitType;
    }
}
