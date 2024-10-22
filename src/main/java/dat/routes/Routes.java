package dat.routes;

import dat.controllers.StoreController;
import dat.controllers.StoreSyncController;
import dat.exceptions.ApiException;
import dat.security.enums.RoleType;
import io.javalin.apibuilder.EndpointGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static io.javalin.apibuilder.ApiBuilder.*;

public class Routes {
    private static final Logger logger = LoggerFactory.getLogger(Routes.class);

    public static EndpointGroup getRoutes() {
        return () -> {
            path("/stores", () -> {
                try {
                    // Sync endpoints
                    post("/sync", StoreSyncController.getInstance()::syncAllStores, RoleType.ANYONE);
                    post("/{id}/sync-products", StoreSyncController.getInstance()::syncStoreProducts, RoleType.ANYONE);
                } catch (ApiException e) {
                    logger.error("Failed to configure store sync routes", e);
                    throw new RuntimeException(e);
                }

                // Regular store endpoints
                get("/", StoreController.getInstance()::getAllStores, RoleType.ANYONE);
                get("/{id}", StoreController.getInstance()::getStoreById, RoleType.ANYONE);
            });
        };
    }
}