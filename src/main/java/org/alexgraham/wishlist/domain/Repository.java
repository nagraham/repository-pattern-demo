package org.alexgraham.wishlist.domain;

import java.util.UUID;

public interface Repository {

    /**
     * Retrieves a wishlist from persistence.
     *
     * @param wishlistId the ID of the wishlist to get
     * @return A wishlist
     * @throws java.util.MissingResourceException if the given UUID does not map to an existing Wishlist
     */
    Wishlist getById(UUID wishlistId);

    /**
     * Persists a Wishlist.
     *
     * @param wishlist The Wishlist to persist
     */
    void save(Wishlist wishlist);

}
