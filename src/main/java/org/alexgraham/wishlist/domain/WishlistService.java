package org.alexgraham.wishlist.domain;

import java.util.UUID;

/**
 * The WishlistService is the public-facing entry point to the Wishlist package.
 *
 * Ideally, we would enclose the Wishlist package in a Java module, and export these methods. The methods
 * themselves are the primary "use cases" for interacting with wishlists; they compose lower-level components
 * (domain models, utilities, persistence layer) to get stuff done.
 */
public class WishlistService {

    private final Repository repo;

    public WishlistService(Repository repo) {
        this.repo = repo;
    }

    public Wishlist createWishlist(UUID ownerId, String name) {
        Wishlist newWishlist = Wishlist.create(ownerId, name);

        if (newWishlist.validate().isPresent()) {
            throw new IllegalArgumentException("The wishlist arguments are invalid: " + newWishlist.validate().get());
        }

        try {
            repo.save(newWishlist);
        } catch (Exception e){
            throw new RuntimeException("Internal Server Error");
        }

        return newWishlist;
    }
}
