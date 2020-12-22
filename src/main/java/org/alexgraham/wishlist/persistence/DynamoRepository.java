package org.alexgraham.wishlist.persistence;

import org.alexgraham.wishlist.domain.Repository;
import org.alexgraham.wishlist.domain.Wishlist;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

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
    public void save(Wishlist wishlist) {
        wishlistStorableTable.putItem(WishlistStorable.fromWishlist(wishlist));
    }

}
