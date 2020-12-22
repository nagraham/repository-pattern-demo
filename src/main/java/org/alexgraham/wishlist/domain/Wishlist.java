package org.alexgraham.wishlist.domain;

import java.util.Optional;
import java.util.UUID;

/**
 * The Wishlist domain object. Raw business logic related to the Wishlist entity belongs here.
 */
public class Wishlist {
    private static final int NAME_MAX_LENGTH = 255;

    private UUID wishlistId;
    private UUID ownerId;
    private String name;

    private Wishlist(UUID wishlistId, UUID ownerId, String name) {
        this.wishlistId = wishlistId;
        this.ownerId = ownerId;
        this.name = name;
    }

    public UUID wishlistId() {
        return wishlistId;
    }

    public UUID ownerId() {
        return ownerId;
    }

    public String name() {
        return name;
    }


    /**
     * Creates a new Wishlist
     *
     * @param ownerId The id of the owner of this wishlist
     * @param name The name of the wishlist
     * @return A new wishlist
     */
    static Wishlist create(UUID ownerId, String name) {
        return new Wishlist(UUID.randomUUID(), ownerId, name);
    }

    /**
     * Rehydrates a Wishlist object based on an existing set of Wishlist data.
     *
     * This is intended to be used to create a Wishlist from a JSON object or some
     * object stored in a Repository.
     *
     * @param wishlistId The Wishlist id
     * @param ownerId The owner id
     * @param name The name of the wishlist
     * @return the rehydrated wishlist object
     */
    public static Wishlist rehydrate(UUID wishlistId, UUID ownerId, String name) {
        return new Wishlist(wishlistId, ownerId, name);
    }

    /**
     * Validates the Wishlist.
     *
     * @return An Optional String, which if present, contains the reason the Wishlist is invalid
     */
    Optional<String> validate() {
        if (name == null) {
            return Optional.of("Name cannot be null");
        } else if (name.isBlank()) {
            return Optional.of("Name must not be blank");
        } else if (name.length() > NAME_MAX_LENGTH) {
            return Optional.of("Name must be greater than " + NAME_MAX_LENGTH + " characters long");
        } else {
            return Optional.empty();
        }
    }
}
