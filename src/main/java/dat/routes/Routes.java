package dat.routes;

import io.javalin.apibuilder.EndpointGroup;

import static io.javalin.apibuilder.ApiBuilder.*;

public class Routes {
    private final StoreRoute storeRoute = new StoreRoute();

    public EndpointGroup getRoutes() {
        return () -> {
            path("stores", storeRoute.getRoutes());  // Fjernet forward slash før stores
        };
    }
}