package dat.security.enums;

import io.javalin.security.RouteRole;

public enum RoleType implements RouteRole {
    ANYONE, USER, ADMIN;
}
