package com.example.outfitsearch.db.tables;

import androidx.lifecycle.LiveData;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.util.List;
import java.util.Objects;

@Entity(
        tableName = "outfits"
)
public class Outfit {
    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "image_uri")
    private String imageUri;

    private String formality;

    private String season;

    private String category;

    @ColumnInfo(name = "view_queue_index")
    private int viewQueueIndex;

    @Ignore
    private LiveData<List<ClothingItem>> clothingItems;

    @Ignore
    private List<ClothingItem> latestClothingItems;

    public Outfit() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getImageUri() {
        return imageUri;
    }

    public void setImageUri(String imageUri) {
        this.imageUri = imageUri;
    }

    public String getFormality() {
        return formality;
    }

    public void setFormality(String formality) {
        this.formality = formality;
    }

    public String getSeason() {
        return season;
    }

    public void setSeason(String season) {
        this.season = season;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getViewQueueIndex() {
        return viewQueueIndex;
    }

    public void setViewQueueIndex(int viewQueueIndex) {
        this.viewQueueIndex = viewQueueIndex;
    }

    public LiveData<List<ClothingItem>> getClothingItems() {
        return clothingItems;
    }

    public void setClothingItems(LiveData<List<ClothingItem>> clothingItems) {
        this.clothingItems = clothingItems;
    }

    public List<ClothingItem> getLatestClothingItems() {
        return latestClothingItems;
    }

    public void setLatestClothingItems(List<ClothingItem> latestClothingItems) {
        this.latestClothingItems = latestClothingItems;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Outfit outfit = (Outfit) o;
        return id == outfit.id &&
                viewQueueIndex == outfit.viewQueueIndex &&
                Objects.equals(imageUri, outfit.imageUri) &&
                Objects.equals(formality, outfit.formality) &&
                Objects.equals(season, outfit.season) &&
                Objects.equals(category, outfit.category);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, imageUri, formality, season, category, viewQueueIndex);
    }
}
