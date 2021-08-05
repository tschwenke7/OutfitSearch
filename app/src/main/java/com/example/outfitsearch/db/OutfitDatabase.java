package com.example.outfitsearch.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.outfitsearch.db.dao.ClothingItemDao;
import com.example.outfitsearch.db.dao.OutfitDao;
import com.example.outfitsearch.db.tables.ClothingItem;
import com.example.outfitsearch.db.tables.Outfit;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {Outfit.class, ClothingItem.class}, version = 1, exportSchema = false)
public abstract class OutfitDatabase extends RoomDatabase {
    public abstract OutfitDao outfitDao();
    public abstract ClothingItemDao clothingItemDao();

    private static volatile OutfitDatabase INSTANCE;
    private static final int NUMBER_OF_THREADS = 4;

    public static final ExecutorService databaseWriterExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    public static OutfitDatabase getDatabase(final Context context){
        if (INSTANCE == null){
            synchronized (OutfitDatabase.class) {
                if (INSTANCE == null){
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            OutfitDatabase.class, "outfit_database")
                            //.addMigrations(x,y)
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
