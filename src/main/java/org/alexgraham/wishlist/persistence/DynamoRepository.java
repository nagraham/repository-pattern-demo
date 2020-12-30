package org.alexgraham.wishlist.persistence;

import org.alexgraham.wishlist.domain.Repository;
import org.alexgraham.wishlist.domain.Wishlist;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.MissingResourceException;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * An implementation of the wishlist.Repository that uses DynamoDB as the backing
 * persistence layer.
 */
public class DynamoRepository implements Repository {
    public static final String GSI_WISHLIST_BY_OWNERS = "20201229_WISHLIST_BY_OWNERS";

    private final DynamoDbTable<WishlistStorable> wishlistStorableTable;
    private final DynamoDbIndex<WishlistStorable> wishlistByOwnerIndex;

    public DynamoRepository(DynamoDbEnhancedClient dynamoDbEnhanced, String tableName) {
        this.wishlistStorableTable = dynamoDbEnhanced.table(tableName, TableSchema.fromBean(WishlistStorable.class));
        this.wishlistByOwnerIndex = wishlistStorableTable.index(GSI_WISHLIST_BY_OWNERS);
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
    public List<Wishlist> queryByOwner(UUID ownerId) {
        SdkIterable<Page<WishlistStorable>> queryResults = wishlistByOwnerIndex.query(QueryEnhancedRequest.builder()
                .queryConditional(QueryConditional.keyEqualTo(uuidToKey(ownerId)))
                .build());

        List<Wishlist> results = new ArrayList<>();
        queryResults.forEach(page -> results.addAll(page.items()
                .stream()
                .map(WishlistStorable::toWishlist)
                .collect(Collectors.toList())));;

        return results;
    }

    @Override
    public void save(Wishlist wishlist) {
        wishlistStorableTable.putItem(WishlistStorable.fromWishlist(wishlist));
    }

    private Key uuidToKey(UUID uuid) {
        return Key.builder().partitionValue(uuid.toString()).build();
    }
}
