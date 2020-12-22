package org.alexgraham.wishlist.domain;

public interface Repository {

    /**
     * Persists a Wishlist.
     *
     * @param wishlist The Wishlist to persist
     */
    void save(Wishlist wishlist);

}
