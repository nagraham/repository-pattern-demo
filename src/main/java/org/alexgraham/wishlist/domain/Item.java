package org.alexgraham.wishlist.domain;

import java.util.Optional;
import java.util.UUID;

public class Item {
    private static final int DETAILS_MAX_LENGTH = 255;

    private UUID itemId;
    private String details;

    private Item(UUID itemId, String details) {
        this.itemId = itemId;
        this.details = details;
    }

    /**
     * Creates an Item for a Wishlist
     *
     * @param details Details of the item (such as the name)
     * @return an Item
     */
    public static Item create(String details) {
        return new Item(UUID.randomUUID(), details);
    }

    /**
     * Creates an Item object given a set of raw data for an existing item.
     *
     * @param itemId Id of an existing item.
     * @param details Details of an item
     * @return The Item
     */
    public static Item rehydrate(UUID itemId, String details) {
        return new Item(itemId, details);
    }

    /**
     * Validates the Wishlist.
     *
     * @return An Optional String, which if present, contains the reason the Wishlist is invalid
     */
    Optional<String> validate() {
        if (details == null) {
            return Optional.of("Details cannot be null");
        } else if (details.isBlank()) {
            return Optional.of("Details must not be blank");
        } else if (details.length() > DETAILS_MAX_LENGTH) {
            return Optional.of("Details must be greater than " + DETAILS_MAX_LENGTH + " characters long");
        } else {
            return Optional.empty();
        }
    }

    public UUID itemId() {
        return itemId;
    }

    public String details() {
        return details;
    }

}
