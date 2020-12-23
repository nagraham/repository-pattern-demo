package org.alexgraham.wishlist.persistence;

import org.alexgraham.wishlist.domain.Item;
import org.alexgraham.wishlist.domain.Wishlist;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@DynamoDbBean
public class WishlistStorable {

    private String id;
    private String ownerId;
    private String name;
    private List<ItemStorable> items;

    public WishlistStorable() {
        // default empty constructor
    }

    public WishlistStorable(String id, String ownerId, String name, List<ItemStorable> items) {
        this.id = id;
        this.ownerId = ownerId;
        this.name = name;
        this.items = items;
    }

    static WishlistStorable fromWishlist(Wishlist wishlist) {
        List<ItemStorable> items = wishlist.items()
                .stream()
                .map(ItemStorable::fromItem)
                .collect(Collectors.toList());

        return new WishlistStorable(
                wishlist.wishlistId().toString(),
                wishlist.ownerId().toString(),
                wishlist.name(),
                items);
    }

    public Wishlist toWishlist() {
        List<Item> wishlistItems = items.stream()
                .map(ItemStorable::toItem)
                .collect(Collectors.toList());

        return Wishlist.rehydrate(
                UUID.fromString(id),
                UUID.fromString(ownerId),
                name,
                wishlistItems);
    }

    @DynamoDbPartitionKey
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }

    public String getOwnerId() {
        return ownerId;
    }
    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public List<ItemStorable> getItems() {
        return items;
    }
    public void setItems(List<ItemStorable> items) {
        this.items = items;
    }
}
