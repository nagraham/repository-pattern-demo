package org.alexgraham.wishlist.persistence;

import org.alexgraham.wishlist.domain.Repository;
import org.alexgraham.wishlist.domain.Wishlist;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;

import java.util.MissingResourceException;
import java.util.UUID;

/**
 * An implementation of the wishlist.Repository that uses DynamoDB as the backing
 * persistence layer.
 */
public class DynamoRepository implements Repository {

    private final DynamoDbTable<WishlistStorable> wishlistStorableTable;

    public DynamoRepository(DynamoDbEnhancedClient dynamoDbEnhanced, String tableName) {
        this.wishlistStorableTable = dynamoDbEnhanced.table(tableName, TableSchema.fromBean(WishlistStorable.class));
    }

    @Override
    public Wishlist getById(UUID wishlistId) {
            WishlistStorable storable = wishlistStorableTable.getItem(Key.builder()
                    .partitionValue(wishlistId.toString())
                    .build());

            if (storable == null) {
                throw new MissingResourceException(
                        "Wishlist not found",
                        Wishlist.class.getName(),
                        wishlistId.toString());
            }

            return storable.toWishlist();
    }

    @Override
    public void save(Wishlist wishlist) {
        wishlistStorableTable.putItem(WishlistStorable.fromWishlist(wishlist));
    }
}
