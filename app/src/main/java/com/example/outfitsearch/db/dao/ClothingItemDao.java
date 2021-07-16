package com.example.outfitsearch.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.outfitsearch.db.tables.ClothingItem;

import java.util.List;

@Dao
public interface ClothingItemDao {
    @Insert
    long insert(ClothingItem clothingItem);

    @Update
    void update(ClothingItem clothingItem);

    @Delete
    void delete(ClothingItem clothingItem);

    @Query("SELECT DISTINCT name FROM clothing_items")
    String[] getAllUniqueNames();

    @Query("SELECT * FROM clothing_items WHERE outfit_id = :outfitId")
    List<ClothingItem> getAllFromOutfit(int outfitId);

    @Query("SELECT * FROM clothing_items WHERE outfit_id = :outfitId")
    LiveData<List<ClothingItem>> getAllFromOutfitLive(int outfitId);
}
