package com.example.outfitsearch.db.tables;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import static androidx.room.ForeignKey.CASCADE;

@Entity(
        tableName = "clothing_items", foreignKeys = {
        @ForeignKey(onDelete = CASCADE, onUpdate = CASCADE, entity = Outfit.class, parentColumns = "id", childColumns = "outfit_id")},
        indices = {
                @Index("outfit_id")
        }
)
public class ClothingItem {
    @PrimaryKey(autoGenerate = true)
    private int id;

    @NonNull
    private String name;

    @ColumnInfo(name = "outfit_id")
    private int outfitId;

//    private String type;
//
//    @ColumnInfo(name = "image_uri")
//    private String imageUri;

    public ClothingItem(){}

    @Ignore
    public ClothingItem(@NonNull String name, int outfitId) {
        this.name = name;
        this.outfitId = outfitId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public void setName(@NonNull String name) {
        this.name = name;
    }

    public int getOutfitId() {
        return outfitId;
    }

    public void setOutfitId(int outfitId) {
        this.outfitId = outfitId;
    }
}
