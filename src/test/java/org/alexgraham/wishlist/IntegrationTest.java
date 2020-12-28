package org.alexgraham.wishlist;

import org.alexgraham.wishlist.domain.Item;
import org.alexgraham.wishlist.domain.Wishlist;
import org.alexgraham.wishlist.domain.WishlistService;
import org.alexgraham.wishlist.persistence.DynamoRepository;
import org.alexgraham.wishlist.persistence.ItemStorable;
import org.alexgraham.wishlist.persistence.WishlistStorable;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.net.URI;
import java.util.List;
import java.util.MissingResourceException;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Integration tests for the Wishlist package.
 *
 * Uses a dockerized local DynamoDB instance for the persistence layer.
 */
@Testcontainers
class IntegrationTest {

    static final String TABLE_NAME = "Test-Wishlist-Table";

    static final int DYNAMO_PORT = 8000;

    // static, so shared between tests
    @Container
    static final GenericContainer dynamodb = new GenericContainer("amazon/dynamodb-local:latest")
            .withExposedPorts(DYNAMO_PORT);

    static DynamoDbEnhancedClient dynamoDbEnhancedClient;
    static DynamoDbTable<WishlistStorable> wishlistStorableDynamoDbTable;

    private WishlistService wishlistService;

    @BeforeAll
    static void setupDynamoClients() {
        Integer mappedPort = dynamodb.getMappedPort(DYNAMO_PORT);

        DynamoDbClient dynamoDbClient = DynamoDbClient.builder()
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("fake", "fakeSecret"
                        )
                ))
                .region(Region.US_WEST_2)
                .endpointOverride(URI.create("http://localhost:" + mappedPort))
                .build();

        dynamoDbEnhancedClient = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();

        wishlistStorableDynamoDbTable = dynamoDbEnhancedClient
                .table(TABLE_NAME, TableSchema.fromBean(WishlistStorable.class));
        wishlistStorableDynamoDbTable.createTable();
    }

    @BeforeEach
    void setup() {
        this.wishlistService = new WishlistService(new DynamoRepository(dynamoDbEnhancedClient, TABLE_NAME));
    }

    @Nested
    @DisplayName("AddItemToWishlist")
    class AddItemToWishlist {

        @Test
        void validItem_addOne_success() {
            UUID wishlistId = UUID.randomUUID();
            UUID owner = UUID.randomUUID();
            addWishlistInDynamo(wishlistId, owner, "test-name");

            Item item = wishlistService.addItemToWishlist(wishlistId, "test-wishlist-item");

            ItemStorable itemStorable = wishlistStorableDynamoDbTable.getItem(Key.builder()
                    .partitionValue(wishlistId.toString())
                    .build())
                    .getItems()
                    .get(0);
            assertThat(itemStorable.getId(), is(item.itemId().toString()));
            assertThat(itemStorable.getDetails(), is(item.details()));
        }

        // note that this tests a few key behaviors:
        //   - Items are ordered based on when they were added
        //   - Retrieving the Wishlist from the repo will include the full list of items.
        @Test
        void validItem_addMultiple_success() {
            UUID wishlistId = UUID.randomUUID();
            UUID owner = UUID.randomUUID();
            addWishlistInDynamo(wishlistId, owner, "test-name");

            Item itemA = wishlistService.addItemToWishlist(wishlistId, "test-wishlist-item-A");
            Item itemB = wishlistService.addItemToWishlist(wishlistId, "test-wishlist-item-B");
            Item itemC = wishlistService.addItemToWishlist(wishlistId, "test-wishlist-item-C");

            List<ItemStorable> itemStorableList = wishlistStorableDynamoDbTable.getItem(Key.builder()
                    .partitionValue(wishlistId.toString())
                    .build())
                    .getItems();
            assertThat(itemStorableList.get(0).getId(), is(itemA.itemId().toString()));
            assertThat(itemStorableList.get(1).getId(), is(itemB.itemId().toString()));
            assertThat(itemStorableList.get(2).getId(), is(itemC.itemId().toString()));
        }

        @Test
        void whenWishlistDoesNotExist_throwsMissingResourceException() {
            assertThrows(MissingResourceException.class,
                    () -> wishlistService.addItemToWishlist(UUID.randomUUID(), "foo"));
        }

        @Test
        void whenItemIsNotValid_throwsIllegalArgumentException() {
            UUID wishlistId = UUID.randomUUID();
            addWishlistInDynamo(wishlistId, UUID.randomUUID(), "foo");
            assertThrows(IllegalArgumentException.class,
                    () -> wishlistService.addItemToWishlist(wishlistId, null));
        }
    }

    @Nested
    @DisplayName("CreateWishlist")
    class CreateWishlist {

        @Test
        void success() {
            UUID owner = UUID.randomUUID();
            Wishlist wishlist = wishlistService.createWishlist(owner, "create a wishlist!");

            WishlistStorable storable = wishlistStorableDynamoDbTable.getItem(Key.builder()
                    .partitionValue(wishlist.wishlistId().toString())
                    .build());

            assertThat(wishlist.wishlistId().toString(), is(storable.getId()));
        }

        @Test
        void invalidArgument() {
            UUID owner = UUID.randomUUID();

            assertThrows(IllegalArgumentException.class, () -> wishlistService.createWishlist(owner, null));
        }
    }

    @Nested
    @DisplayName("GetWishlistById")
    class GetWishlistById {

        @Test
        void wishlistWithoutItems_success() {
            UUID wishlistId = UUID.randomUUID();
            UUID owner = UUID.randomUUID();
            addWishlistInDynamo(wishlistId, owner, "test-name");

            Wishlist wishlist = wishlistService.getWishlistById(wishlistId);

            assertThat(wishlist.wishlistId(), is(wishlistId));
            assertThat(wishlist.ownerId(), is(owner));
            assertThat(wishlist.name(), is("test-name"));
        }

        @Test
        void wishlistWithSomeItems_success() {
            UUID wishlistId = UUID.randomUUID();
            UUID owner = UUID.randomUUID();
            addWishlistInDynamo(wishlistId, owner, "test-name");
            Item itemA = wishlistService.addItemToWishlist(wishlistId, "test-wishlist-item-A");
            Item itemB = wishlistService.addItemToWishlist(wishlistId, "test-wishlist-item-B");

            Wishlist wishlist = wishlistService.getWishlistById(wishlistId);

            assertThat(wishlist.items().size(), is(2));
            assertThat(wishlist.items().get(0).itemId(), is(itemA.itemId()));
            assertThat(wishlist.items().get(1).itemId(), is(itemB.itemId()));
        }

        @Test
        void missingWishlist() {
            UUID wishlistId = UUID.randomUUID();

            assertThrows(MissingResourceException.class, () -> wishlistService.getWishlistById(wishlistId));
        }
    }

    @Nested
    @DisplayName("ReorderItemInWishlist")
    class ReorderItemInWishlist {

        private UUID wishlistId;

        @BeforeEach
        void setup() {
            wishlistId = UUID.randomUUID();
            addWishlistInDynamo(wishlistId, UUID.randomUUID(), "test-name");
        }

        @Nested
        @DisplayName("with multiple items")
        class WithMultipleItems {

            private Item itemA;
            private Item itemB;
            private Item itemC;

            @BeforeEach
            void setup() {
                itemA = wishlistService.addItemToWishlist(wishlistId, "test-wishlist-item-A");
                itemB = wishlistService.addItemToWishlist(wishlistId, "test-wishlist-item-B");
                itemC = wishlistService.addItemToWishlist(wishlistId, "test-wishlist-item-C");
            }

            @Test
            void reorderLastItemToFront_establishesCorrectOrder() {
                wishlistService.reorderItemInWishlist(wishlistId, itemC.itemId(), 0);

                List<UUID> itemIdList = getWishlistItemIds(wishlistId);
                assertThat(itemIdList, contains(itemC.itemId(), itemA.itemId(), itemB.itemId()));
            }

            @Test
            void reorderLastItemToMiddle_establishesCorrectOrder() {
                wishlistService.reorderItemInWishlist(wishlistId, itemC.itemId(), 1);

                List<UUID> itemIdList = getWishlistItemIds(wishlistId);
                assertThat(itemIdList, contains(itemA.itemId(), itemC.itemId(), itemB.itemId()));
            }

            @Test
            void reorderFrontToLast_establishesCorrectOrder() {
                wishlistService.reorderItemInWishlist(wishlistId, itemA.itemId(), 2);

                List<UUID> itemIdList = getWishlistItemIds(wishlistId);
                assertThat(itemIdList, contains(itemB.itemId(), itemC.itemId(), itemA.itemId()));
            }

            @Test
            void reorderFrontToMiddle_establishesCorrectOrder() {
                wishlistService.reorderItemInWishlist(wishlistId, itemA.itemId(), 1);

                List<UUID> itemIdList = getWishlistItemIds(wishlistId);
                assertThat(itemIdList, contains(itemB.itemId(), itemA.itemId(), itemC.itemId()));
            }

            @Test
            void reorderMiddleToFront_establishesCorrectOrder() {
                wishlistService.reorderItemInWishlist(wishlistId, itemB.itemId(), 0);

                List<UUID> itemIdList = getWishlistItemIds(wishlistId);
                assertThat(itemIdList, contains(itemB.itemId(), itemA.itemId(), itemC.itemId()));
            }

            @Test
            void reorderMiddleToBack_establishesCorrectOrder() {
                wishlistService.reorderItemInWishlist(wishlistId, itemB.itemId(), 2);

                List<UUID> itemIdList = getWishlistItemIds(wishlistId);
                assertThat(itemIdList, contains(itemA.itemId(), itemC.itemId(), itemB.itemId()));
            }

            @Test
            void givenIndexGreaterThanLength_movesItemToEnd() {
                wishlistService.reorderItemInWishlist(wishlistId, itemA.itemId(), 101);

                List<UUID> itemIdList = getWishlistItemIds(wishlistId);
                assertThat(itemIdList, contains(itemB.itemId(), itemC.itemId(), itemA.itemId()));
            }
        }

        @Test
        void wishlistHasOneItem_doesNothing() {
            Item itemA = wishlistService.addItemToWishlist(wishlistId, "test-wishlist-item-A");

            wishlistService.reorderItemInWishlist(wishlistId, itemA.itemId(), 3);

            List<UUID> itemIdList = getWishlistItemIds(wishlistId);
            assertThat(itemIdList, contains(itemA.itemId()));
        }

        @Test
        void wishlistHasNoItems_doesNothing() {
            wishlistService.reorderItemInWishlist(wishlistId, UUID.randomUUID(), 3);

            List<UUID> itemIdList = getWishlistItemIds(wishlistId);
            assertThat(itemIdList, is(empty()));
        }

        @Test
        void whenItemNotPresent_throwsMissingResourceException() {
            // only throws if size > 1, b/c otherwise we terminate the function early
            wishlistService.addItemToWishlist(wishlistId, "test-wishlist-item-A");
            wishlistService.addItemToWishlist(wishlistId, "test-wishlist-item-B");

            assertThrows(MissingResourceException.class, () ->
                    wishlistService.reorderItemInWishlist(wishlistId, UUID.randomUUID(), 0));
        }

        @Test
        void whenIndexNegative_throwsIllegalArgumentException() {
            Item itemA = wishlistService.addItemToWishlist(wishlistId, "test-wishlist-item-A");

            assertThrows(IllegalArgumentException.class, () ->
                    wishlistService.reorderItemInWishlist(wishlistId, itemA.itemId(), -1));
        }

        @Test
        void whenItemIdIsNull_throwsIllegalArgumentException() {
            assertThrows(IllegalArgumentException.class, () ->
                    wishlistService.reorderItemInWishlist(wishlistId, null, -1));
        }
    }

    private void addWishlistInDynamo(UUID wishlistId, UUID owner, String name) {
        WishlistStorable wishlistStorable = new WishlistStorable(
                wishlistId.toString(),
                owner.toString(),
                name,
                List.of());
        wishlistStorableDynamoDbTable.putItem(wishlistStorable);
    }

    private WishlistStorable getWishlistStorable(UUID wishlistId) {
        return wishlistStorableDynamoDbTable.getItem(Key.builder()
                .partitionValue(wishlistId.toString())
                .build());
    }

    private List<UUID> getWishlistItemIds(UUID wishlistId) {
        WishlistStorable wishlistStorable = getWishlistStorable(wishlistId);
        return wishlistStorable.getItems()
                .stream()
                .map(itemStorable -> UUID.fromString(itemStorable.getId()))
                .collect(Collectors.toList());
    }
}