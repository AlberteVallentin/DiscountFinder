package dat.routes;


import dat.controllers.impl.StoreController;
import dat.security.enums.RoleType;
import io.javalin.apibuilder.EndpointGroup;

import static io.javalin.apibuilder.ApiBuilder.*;

public class StoreRoute {
    private final StoreController storeController = new StoreController();

    protected EndpointGroup getRoutes() {
        return () -> {

            get("/", storeController::readAll, RoleType.ANYONE);
            get("/{id}", storeController::read, RoleType.USER);

            get("/postal_code/{postal_code}", storeController::getStoresByPostalCode, RoleType.ANYONE);
            //post("/", storeController::create, RoleType.ADMIN);
        };
    }
}