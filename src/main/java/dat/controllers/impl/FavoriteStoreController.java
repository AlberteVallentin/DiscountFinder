package dat.controllers.impl;

import dat.config.HibernateConfig;
import dat.daos.impl.FavoriteStoreDAO;
import dat.dtos.StoreDTO;
import dat.exceptions.ApiException;
import dat.security.token.UserDTO;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Collectors;

public class FavoriteStoreController {
    private static final Logger LOGGER = LoggerFactory.getLogger(FavoriteStoreController.class);
    private final FavoriteStoreDAO favoriteStoreDAO;

    public FavoriteStoreController() {
        this.favoriteStoreDAO = FavoriteStoreDAO.getInstance(HibernateConfig.getEntityManagerFactory());
    }

    public void getFavoriteStores(Context ctx) throws ApiException {
        try {
            UserDTO userDTO = ctx.attribute("user");
            if (userDTO == null) {
                throw new ApiException(401, "User not authenticated");
            }

            var stores = favoriteStoreDAO.getFavoriteStores(userDTO.getEmail());
            var storeDTOs = stores.stream()
                .map(store -> new StoreDTO(store, false, userDTO.getEmail()))
                .collect(Collectors.toList());

            ctx.json(storeDTOs);

        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("Error in getFavoriteStores: ", e);
            throw new ApiException(500, "Error fetching favorite stores: " + e.getMessage());
        }
    }

    public void addFavoriteStore(Context ctx) throws ApiException {
        try {
            UserDTO userDTO = ctx.attribute("user");
            if (userDTO == null) {
                throw new ApiException(401, "User not authenticated");
            }

            Long storeId = Long.parseLong(ctx.pathParam("id"));
            favoriteStoreDAO.addFavoriteStore(userDTO.getEmail(), storeId);
            ctx.status(204);

        } catch (NumberFormatException e) {
            throw new ApiException(400, "Invalid store ID format");
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(500, "Error adding favorite store: " + e.getMessage());
        }
    }

    public void removeFavoriteStore(Context ctx) throws ApiException {
        try {
            UserDTO userDTO = ctx.attribute("user");
            if (userDTO == null) {
                throw new ApiException(401, "User not authenticated");
            }

            Long storeId = Long.parseLong(ctx.pathParam("id"));
            favoriteStoreDAO.removeFavoriteStore(userDTO.getEmail(), storeId);
            ctx.status(204);

        } catch (NumberFormatException e) {
            throw new ApiException(400, "Invalid store ID format");
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(500, "Error removing favorite store: " + e.getMessage());
        }
    }
}