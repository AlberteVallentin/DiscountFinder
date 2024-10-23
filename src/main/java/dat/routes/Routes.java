package dat.routes;

import dat.controllers.StoreController;
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

                // Regular store endpoints
                get("/", StoreController.getInstance()::getAllStores, RoleType.ANYONE);
                get("/{id}", StoreController.getInstance()::getStoreById, RoleType.ANYONE);
            });
        };
    }
}