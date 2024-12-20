package dat.security.controllers;

import dat.controllers.impl.StoreController;
import dat.security.enums.RoleType;
import dat.security.token.UserDTO;
import io.javalin.http.Context;
import io.javalin.http.UnauthorizedResponse;
import io.javalin.security.RouteRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * Purpose: To handle security in the API at the route level
 *  Author: Jon Bertelsen
 */

public class AccessController implements IAccessController {
    private static final Logger LOGGER = LoggerFactory.getLogger(StoreController.class);
    SecurityController securityController = SecurityController.getInstance();

    /**
     * This method checks if the user has the necessary roles to access the route.
     * @param ctx
     */
        public void accessHandler(Context ctx) {
            // Hvis der er en gyldig authorization header, sæt bruger-attributten
            String header = ctx.header("Authorization");
            if (header != null && header.startsWith("Bearer ")) {
                try {
                    securityController.authenticate().handle(ctx);
                } catch (Exception e) {
                    // Log error men fortsæt - dette tillader request at fortsætte selv hvis auth fejler
                    LOGGER.warn("Auth failed but continuing as public request: {}", e.getMessage());
                }
            }

            // Hvis endpointet kræver specifikke roller
            if (!ctx.routeRoles().isEmpty() && !ctx.routeRoles().contains(RoleType.ANYONE)) {
                UserDTO user = ctx.attribute("user");
                if (user == null) {
                    throw new UnauthorizedResponse("You need to log in!");
                }
                Set<RouteRole> allowedRoles = ctx.routeRoles();
                if (!securityController.authorize(user, allowedRoles)) {
                    throw new UnauthorizedResponse("Unauthorized with roles: " + user.getRoleType());
                }
            }
        }
    }

