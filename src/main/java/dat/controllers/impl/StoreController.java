package dat.controllers.impl;

import dat.config.HibernateConfig;
import dat.daos.impl.StoreDAO;
import dat.dtos.StoreDTO;
import dat.exceptions.ApiException;
import dat.utils.Utils;
import io.javalin.http.Context;
import jakarta.persistence.EntityManagerFactory;

import java.util.List;

public class StoreController {
    private final StoreDAO storeDAO;

    public StoreController() {
        EntityManagerFactory emf = HibernateConfig.getEntityManagerFactory();
        this.storeDAO = StoreDAO.getInstance(emf);
    }

    public void read(Context ctx) {
        try {
            Long id = ctx.pathParamAsClass("id", Long.class).get();

            if (!validatePrimaryKey(id)) {
                ctx.status(404);
                ctx.json(Utils.convertToJsonMessage(ctx, "warning", "Store not found with ID: " + id));
                return;
            }

            StoreDTO storeDTO = storeDAO.read(id);
            ctx.status(200);
            ctx.json(storeDTO, StoreDTO.class);
        } catch (NumberFormatException e) {
            ctx.status(400);
            ctx.json(Utils.convertToJsonMessage(ctx, "warning", "Invalid store ID format"));
        } catch (Exception e) {
            ctx.status(500);
            ctx.json(Utils.convertToJsonMessage(ctx, "error", "An unexpected error occurred"));
        }
    }

    public void readAll(Context ctx) {
        // List of DTOs
        List<StoreDTO> storeDTOS = storeDAO.readAll();
        // Response
        ctx.status(200);
        ctx.json(storeDTOS, StoreDTO.class);
    }


    public void create(Context ctx) throws ApiException {
        // Request
        StoreDTO jsonRequest = ctx.bodyAsClass(StoreDTO.class);
        // DTO
        StoreDTO storeDTO = storeDAO.create(jsonRequest);
        // Response
        ctx.status(201);
        ctx.json(storeDTO, StoreDTO.class);
    }

    public boolean validatePrimaryKey(Long id) {
        return storeDAO.validatePrimaryKey(id);
    }

    public void getStoresByPostalCode(Context ctx) {
        try {
            String postalCodeStr = ctx.pathParam("postal_code");
            Integer postalCode;

            try {
                postalCode = Integer.parseInt(postalCodeStr);
            } catch (NumberFormatException e) {
                ctx.status(400);
                ctx.json(Utils.convertToJsonMessage(ctx, "warning", "Invalid postal code format"));
                return;
            }

            List<StoreDTO> stores = storeDAO.findByPostalCode(postalCode);

            if (stores.isEmpty()) {
                ctx.status(404);
                ctx.json(Utils.convertToJsonMessage(ctx, "warning",
                    "No stores found for postal code: " + postalCode));
                return;
            }

            ctx.status(200);
            ctx.json(stores);

        } catch (Exception e) {
            ctx.status(500);
            ctx.json(Utils.convertToJsonMessage(ctx, "error",
                "An unexpected error occurred"));
        }
    }





//    @Override
//    public void update(Context ctx) throws ApiException {
//        // Request
//        long id = ctx.pathParamAsClass("id", Long.class)
//            .check(this::validatePrimaryKey, "Not a valid store ID").get();
//        // DTO
//        StoreDTO storeDTO = dao.update(id, validateEntity(ctx));
//        // Response
//        ctx.status(200);
//        ctx.json(storeDTO, StoreDTO.class);
//    }
//
//    @Override
//    public void delete(Context ctx) {
//        // Request
//        long id = ctx.pathParamAsClass("id", Long.class)
//            .check(this::validatePrimaryKey, "Not a valid store ID").get();
//        dao.delete(id);
//        // Response
//        ctx.status(204);
//    }
//
//    @Override
//    public StoreDTO validateEntity(Context ctx) {
//        return ctx.bodyValidator(StoreDTO.class)
//            .check(s -> s.getSallingStoreId() != null && !s.getSallingStoreId().trim().isEmpty(), "Salling Store ID must be set")
//            .check(s -> s.getName() != null && !s.getName().trim().isEmpty(), "Store name must be set")
//            .check(s -> s.getBrand() != null, "Store brand must be set")
//            .check(s -> s.getAddress() != null, "Store address must be set")
//            .check(s -> s.getAddress().getPostalCode() != null, "Postal code must be set")
//            .check(s -> s.getAddress().getAddressLine() != null && !s.getAddress().getAddressLine().trim().isEmpty(), "Address line must be set")
//            .get();
//    }
}
