package dat.daos;

import dat.exceptions.ApiException;

import java.util.List;

public interface IDAO<T, I> {

    T read(I i);
    List<T> readAll();
    T create(T t) throws ApiException;
    T update(I i, T t) throws ApiException;
    void delete(I i);
    boolean validatePrimaryKey(I i);

}
