package dat.daos.impl;

import dat.daos.IDAO;
import dat.dtos.StoreDTO;
import dat.entities.Store;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.TypedQuery;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class StoreDAO implements IDAO<StoreDTO, Long> {

    private static StoreDAO instance;
    private static EntityManagerFactory emf;

    public static StoreDAO getInstance(EntityManagerFactory _emf) {
        if (instance == null) {
            emf = _emf;
            instance = new StoreDAO();
        }
        return instance;
    }

    @Override
    public StoreDTO read(Long aLong) {
        return null;
    }

    @Override
    public List<StoreDTO> readAll() {
        return List.of();
    }

    @Override
    public StoreDTO create(StoreDTO storeDTO) {
        return null;
    }

    @Override
    public StoreDTO update(Long aLong, StoreDTO storeDTO) {
        return null;
    }

    @Override
    public void delete(Long aLong) {

    }

    @Override
    public boolean validatePrimaryKey(Long aLong) {
        return false;
    }
}