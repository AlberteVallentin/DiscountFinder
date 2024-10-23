package dat.controllers.impl;

import dat.config.HibernateConfig;
import dat.controllers.IController;
import dat.daos.impl.StoreDAO;
import dat.dtos.StoreDTO;
import dat.exceptions.ApiException;
import io.javalin.http.Context;
import jakarta.persistence.EntityManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class StoreController implements IController<StoreDTO, Long> {
    private static final Logger LOGGER = LoggerFactory.getLogger(StoreController.class);
    private final StoreDAO dao;

    public StoreController() {
        EntityManagerFactory emf = HibernateConfig.getEntityManagerFactory();
        this.dao = StoreDAO.getInstance(emf);
    }

    @Override
    public void read(Context ctx) {
        // request
        Long id = ctx.pathParamAsClass("id", Long.class)
            .check(this::validatePrimaryKey, "Not a valid store id")
            .get();
        // DTO
        StoreDTO storeDTO = dao.read(id);
        // response
        ctx.res().setStatus(200);
        ctx.json(storeDTO, StoreDTO.class);
    }

    @Override
    public void readAll(Context ctx) {
        // List of DTOs
        List<StoreDTO> storeDTOS = dao.readAll();
        // response
        ctx.res().setStatus(200);
        ctx.json(storeDTOS, StoreDTO.class);
    }

    @Override
    public void create(Context ctx) throws ApiException {
        // request with validation
        StoreDTO jsonRequest = validateEntity(ctx);
        // create and get DTO
        StoreDTO storeDTO = dao.create(jsonRequest);
        // response
        ctx.res().setStatus(201);
        ctx.json(storeDTO, StoreDTO.class);
    }

    @Override
    public void update(Context ctx) throws ApiException {
        // request
        Long id = ctx.pathParamAsClass("id", Long.class)
            .check(this::validatePrimaryKey, "Not a valid store id")
            .get();
        // update with validated entity and get DTO
        StoreDTO storeDTO = dao.update(id, validateEntity(ctx));
        // response
        ctx.res().setStatus(200);
        ctx.json(storeDTO, StoreDTO.class);
    }

    @Override
    public void delete(Context ctx) {
        // request
        Long id = ctx.pathParamAsClass("id", Long.class)
            .check(this::validatePrimaryKey, "Not a valid store id")
            .get();
        // delete
        dao.delete(id);
        // response
        ctx.res().setStatus(204);
    }

    @Override
    public boolean validatePrimaryKey(Long id) {
        return dao.validatePrimaryKey(id);
    }

    @Override
    public StoreDTO validateEntity(Context ctx) {
        return ctx.bodyValidator(StoreDTO.class)
            .check(s -> s.getSallingStoreId() != null && !s.getSallingStoreId().trim().isEmpty(),
                "Salling Store ID must be set")
            .check(s -> s.getName() != null && !s.getName().trim().isEmpty(),
                "Store name must be set")
            .check(s -> s.getBrand() != null,
                "Store brand must be set")
            .check(s -> s.getAddress() != null,
                "Store address must be set")
            .check(s -> s.getAddress().getPostalCode() != null,
                "Store postal code must be set")
            .check(s -> s.getAddress().getAddressLine() != null && !s.getAddress().getAddressLine().trim().isEmpty(),
                "Store address line must be set")
            .get();
    }
}