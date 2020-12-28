package org.alexgraham.wishlist.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.MissingResourceException;
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
    private List<Item> items;

    private Wishlist(UUID wishlistId, UUID ownerId, String name, List<Item> items) {
        this.wishlistId = wishlistId;
        this.ownerId = ownerId;
        this.name = name;
        this.items = items;
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

    public List<Item> items() {
        return List.copyOf(this.items);
    }

    /**
     * Creates a new Wishlist with an empty set of Items.
     *
     * @param ownerId The id of the owner of this wishlist
     * @param name The name of the wishlist
     * @return A new wishlist
     */
    static Wishlist create(UUID ownerId, String name) {
        return new Wishlist(UUID.randomUUID(), ownerId, name, new ArrayList<>());
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
    public static Wishlist rehydrate(
            UUID wishlistId,
            UUID ownerId,
            String name,
            List<Item> items
    ) {
        return new Wishlist(wishlistId, ownerId, name, items);
    }

    /**
     * Adds an item to the Wishlist
     *
     * @param item the item to add
     */
    public void addItem(Item item) {
        this.items.add(item);
    }

    /**
     * Re-orders an item in the List.
     *
     * @param itemId the id of the item to re-order
     * @param index the index into which the item should be inserted
     */
    public void reorderItem(UUID itemId, int index) {
        if (itemId == null) {
            throw new IllegalArgumentException("null itemId");
        }
        if (index < 0) {
            throw new IllegalArgumentException("negative index=" + index);
        }
        if (items.size() <= 1) { // not large enough to re-order
            return;
        }

        int indexOfOldLocation = -1;
        for (int i = 0; i < items.size(); i++) {
            if (itemId.equals(items.get(i).itemId())) {
                indexOfOldLocation = i;
                break;
            }
        }

        if (indexOfOldLocation == -1) {
            throw new MissingResourceException("the item with id=" + wishlistId + " does not exist in wishlist=" +
                    wishlistId, Item.class.getName(), itemId.toString());
        }

        // if the given index is greater than the size, append it to the end
        index = Math.min(index, items.size() - 1);

        Item item = items.remove(indexOfOldLocation);
        items.add(index, item);
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
