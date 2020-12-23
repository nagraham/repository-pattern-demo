package org.alexgraham.wishlist.persistence;

import org.alexgraham.wishlist.domain.Item;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

import java.util.UUID;

@DynamoDbBean
public class ItemStorable {

    private String id;
    private String details;

    public ItemStorable() {
    }

    public ItemStorable(String id, String details) {
        this.id = id;
        this.details = details;
    }

    public static ItemStorable fromItem(Item item) {
        return new ItemStorable(item.itemId().toString(), item.details());
    }

    public Item toItem() {
        return Item.rehydrate(UUID.fromString(id), details);
    }

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }

    public String getDetails() {
        return details;
    }
    public void setDetails(String details) {
        this.details = details;
    }
}
