package org.alexgraham.wishlist.domain;

import java.util.List;
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
     * Queries for a list of Wishlists based on their owner.
     *
     * @param ownerId the id of the owner to query for
     * @return a list of Wishlists the owner owns (or an empty list if they don't own any Wishlists).
     */
    List<Wishlist> queryByOwner(UUID ownerId);

    /**
     * Persists a Wishlist.
     *
     * @param wishlist The Wishlist to persist
     */
    void save(Wishlist wishlist);

}
