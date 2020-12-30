package org.alexgraham.wishlist.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
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

    /**
     * Adds a new Item to a Wishlist.
     *
     * @param wishlistId Id of the wishlist to which the item will be added
     * @param itemDetails Details about the item
     * @return The newly added Item
     * @throws IllegalArgumentException if the Item arguments are invalid
     * @throws MissingResourceException if the Wishlist does not exist
     */
    public Item addItemToWishlist(UUID wishlistId, String itemDetails) {
        // TODO: Authorize caller has access to add item to Wishlist
        Item item = Item.create(itemDetails);

        if (item.validate().isPresent()) {
            throw new IllegalArgumentException("The item arguments are invalid: " + item.validate().get());
        }

        Wishlist wishlist = getWishlistFromRepo(wishlistId);
        wishlist.addItem(item);
        saveWishlist(wishlist);
        return item;
    }

    /**
     * Creates a new Wishlist
     *
     * @param ownerId ID for the user who owns the Wishlist
     * @param name The name of the wishlist
     * @return The newly created Wishlist
     * @throws IllegalArgumentException if the wishlist arguments are invalid
     */
    public Wishlist createWishlist(UUID ownerId, String name) {
        Wishlist newWishlist = Wishlist.create(ownerId, name);

        if (newWishlist.validate().isPresent()) {
            throw new IllegalArgumentException("The wishlist arguments are invalid: " + newWishlist.validate().get());
        }

        saveWishlist(newWishlist);

        return newWishlist;
    }

    /**
     * Gets a Wishlist with the given identifier.
     *
     * @param wishlistId The wishlist identifier
     * @return The Wishlist
     * @throws MissingResourceException if the wishlist does not exist
     */
    public Wishlist getWishlistById(UUID wishlistId) {
        // TODO: Authorize access
        return getWishlistFromRepo(wishlistId);
    }

    /**
     * Returns a list of Wishlists owned by the given owner, or an empty list of the
     * owner does not have any Wishlists.
     *
     * These Wishlists will not contain Items (that field will be set to null).
     *
     * @param ownerId The id of the owner
     * @return a list of Wishlists
     */
    public List<Wishlist> listWishlistsByOwner(UUID ownerId) {
        List<Wishlist> wishlistList;
        try {
            wishlistList = repo.queryByOwner(ownerId);
        } catch (Exception e) { // unhandled exceptions
            logger.error("Error querying Wishlists by ownerId={}", ownerId.toString(), e);
            throw new RuntimeException("Internal Service Error");
        }
        return wishlistList;
    }

    /**
     * For an Item with the given id, re-orders that item within the Wishlist.
     *
     * When re-ordering, items are shifted back. If the value is greater than
     * the length of the wishlist, the item moved to the end
     *
     * @param wishlistId ID of an existing Wishlist
     * @param itemId ID of an existing Item within that Wishlist
     * @param index The index into which the Item should be moved. If greater than
     *              the total number of Items, the Item will be added to the end.
     * @throws MissingResourceException if the Wishlist or Item do not exist
     * @throws IllegalArgumentException if the arguments are invalid
     */
    public void reorderItemInWishlist(UUID wishlistId, UUID itemId, int index) {
        Wishlist wishlist = getWishlistFromRepo(wishlistId);
        wishlist.reorderItem(itemId, index);
        saveWishlist(wishlist);
    }

    /*
     * PRIVATE FUNCTIONS
     */

    private Wishlist getWishlistFromRepo(UUID wishlistId) {
        try {
            return repo.getById(wishlistId);
        } catch (MissingResourceException e) {
            throw e; // re-raise
        } catch (Exception e) { // unhandled exceptions
            logger.error("Error getting Wishlist by id={}", wishlistId.toString(), e);
            throw new RuntimeException("Internal Service Error");
        }
    }

    private void saveWishlist(Wishlist wishlist) {
        if (wishlist == null) {
            throw new RuntimeException("Attempting to save null wishlist");
        }

        try {
            repo.save(wishlist);

        } catch (Exception e) { // unhandled exceptions
            String ownerId = "";
            String wishlistName = "";
            if (wishlist.ownerId() != null) {
                ownerId = wishlist.ownerId().toString();
            }
            wishlistName = wishlist.name();
            logger.error("Error creating wishlist ownerId={} name={}", ownerId, wishlistName, e);
            throw new RuntimeException("Internal Service Error");
        }
    }
}
