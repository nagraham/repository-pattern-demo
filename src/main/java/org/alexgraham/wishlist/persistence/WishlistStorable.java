package org.alexgraham.wishlist.persistence;

import org.alexgraham.wishlist.domain.Wishlist;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@DynamoDbBean
public class WishlistStorable {

    private String id;
    private String ownerId;
    private String name;

    static WishlistStorable fromWishlist(Wishlist wishlist) {
        return new WishlistStorable(
                wishlist.wishlistId().toString(),
                wishlist.ownerId().toString(),
                wishlist.name()
        );
    }

    public WishlistStorable() {
        // default empty constructor
    }

    public WishlistStorable(String id, String ownerId, String name) {
        this.id = id;
        this.ownerId = ownerId;
        this.name = name;
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
}
