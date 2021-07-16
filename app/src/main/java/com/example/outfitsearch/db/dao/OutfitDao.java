package com.example.outfitsearch.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.outfitsearch.db.tables.Outfit;

import java.util.List;

@Dao
public interface OutfitDao {
    @Insert
    long insert(Outfit outfit);

    @Update
    void update(Outfit outfit);

    @Delete
    void delete(Outfit outfit);

    @Query("SELECT * FROM outfits ORDER BY view_queue_index ASC")
    LiveData<List<Outfit>> getAll();

    @Query("SELECT view_queue_index FROM outfits ORDER BY view_queue_index DESC LIMIT 1")
    int getLastViewQueueIndex();

    @Query("SELECT * FROM outfits WHERE id = :id")
    Outfit getById(int id);
}