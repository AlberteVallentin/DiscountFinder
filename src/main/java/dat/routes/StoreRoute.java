package dat.routes;


import dat.controllers.impl.StoreController;
import dat.security.enums.RoleType;
import io.javalin.apibuilder.EndpointGroup;

import static io.javalin.apibuilder.ApiBuilder.*;

public class StoreRoute {
    private final StoreController storeController = new StoreController();

    protected EndpointGroup getRoutes() {
        return () -> {
            // PUBLIC endpoints - anyone can view stores
            get("/", storeController::readAll, RoleType.ANYONE);
            get("/postal_code/{postal_code}", storeController::getStoresByPostalCode, RoleType.ANYONE);
            get("/{id}", storeController::read, RoleType.ANYONE);


            // PROTECTED endpoints - only ADMIN can modify stores
            post("/", storeController::create, RoleType.ADMIN);
        };
    }
}