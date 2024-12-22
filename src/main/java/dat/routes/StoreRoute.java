package dat.routes;


import dat.controllers.impl.StoreController;
import dat.controllers.impl.FavoriteStoreController;
import dat.security.enums.RoleType;
import io.javalin.apibuilder.EndpointGroup;

import static io.javalin.apibuilder.ApiBuilder.*;

public class StoreRoute {
    private final StoreController storeController = new StoreController();
    private final FavoriteStoreController favoriteStoreController = new FavoriteStoreController();

    protected EndpointGroup getRoutes() {
        return () -> {
            get("/favorites", favoriteStoreController::getFavoriteStores, RoleType.USER);
            get("/", storeController::readAll, RoleType.ANYONE);
            get("/{id}", storeController::read, RoleType.ANYONE);  // Ændret fra USER til ANYONE
            get("/postal_code/{postal_code}", storeController::getStoresByPostalCode, RoleType.ANYONE);

            post("/{id}/favorite", favoriteStoreController::addFavoriteStore, RoleType.USER);
            delete("/{id}/favorite", favoriteStoreController::removeFavoriteStore, RoleType.USER);
        };
    }
}