package org.alexgraham.wishlist;

import org.alexgraham.wishlist.domain.Wishlist;
import org.alexgraham.wishlist.domain.WishlistService;
import org.alexgraham.wishlist.persistence.DynamoRepository;
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
import java.util.MissingResourceException;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
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
        void success() {
            UUID wishlistId = UUID.randomUUID();
            UUID owner = UUID.randomUUID();
            WishlistStorable wishlistStorable = new WishlistStorable(
                    wishlistId.toString(),
                    owner.toString(),
                    "test-name");
            wishlistStorableDynamoDbTable.putItem(wishlistStorable);

            Wishlist wishlist = wishlistService.getWishlistById(wishlistId);

            assertThat(wishlist.wishlistId(), is(wishlistId));
            assertThat(wishlist.ownerId(), is(owner));
            assertThat(wishlist.name(), is("test-name"));
        }

        @Test
        void missingWishlist() {
            UUID wishlistId = UUID.randomUUID();

            assertThrows(MissingResourceException.class, () -> wishlistService.getWishlistById(wishlistId));
        }

    }
}