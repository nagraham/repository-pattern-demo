package org.alexgraham.wishlist.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.MissingResourceException;
import java.util.UUID;

/**
 * The WishlistService is the public-facing entry point to the Wishlist package.
 *
 * Ideally, we would enclose the Wishlist package in a Java module, and export these methods. The methods
 * themselves are the primary "use cases" for interacting with wishlists; they compose lower-level components
 * (domain models, utilities, persistence layer) to get stuff done.
 */
public class WishlistService {
    private static final Logger logger = LoggerFactory.getLogger(WishlistService.class);

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
        } catch (Exception e) { // unhandled exceptions
            logger.error("Error creating wishlist ownerId={} name={}", ownerId.toString(), name, e);
            throw new RuntimeException("Internal Service Error");
        }

        return newWishlist;
    }

    public Wishlist getWishlistById(UUID wishlistId) {
        try {
            return repo.getById(wishlistId);
        } catch (MissingResourceException e) {
            throw e; // re-raise
        } catch (Exception e) { // unhandled exceptions
            logger.error("Error getting Wishlist by id={}", wishlistId.toString(), e);
            throw new RuntimeException("Internal Service Error");
        }
    }
}
