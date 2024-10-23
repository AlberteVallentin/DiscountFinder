package dat.controllers;

import dat.exceptions.ApiException;
import io.javalin.http.Context;

public interface IController<T, D> {
    void read(Context ctx);
    void readAll(Context ctx);
    void create(Context ctx) throws ApiException;
    void update(Context ctx) throws ApiException;
    void delete(Context ctx);
    boolean validatePrimaryKey(D d);
    T validateEntity(Context ctx);

}
