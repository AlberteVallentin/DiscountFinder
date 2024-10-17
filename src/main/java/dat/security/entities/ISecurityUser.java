package dat.security.entities;

import dat.security.enums.RoleType;

public interface ISecurityUser {
    boolean verifyPassword(String password);
    void addRole(RoleType roleType);
}