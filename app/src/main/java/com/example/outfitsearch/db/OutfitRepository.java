package com.example.outfitsearch.db;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.outfitsearch.db.dao.ClothingItemDao;
import com.example.outfitsearch.db.dao.OutfitDao;
import com.example.outfitsearch.db.tables.ClothingItem;
import com.example.outfitsearch.db.tables.Outfit;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class OutfitRepository {
    private final OutfitDao outfitDao;
    private final ClothingItemDao clothingItemDao;

    private LiveData<List<Outfit>> allOutfits;

    public OutfitRepository(Context context){
        OutfitDatabase db = OutfitDatabase.getDatabase(context);
        outfitDao = db.outfitDao();
        clothingItemDao = db.clothingItemDao();

        allOutfits = outfitDao.getAll();
    }

    public LiveData<List<Outfit>> getAllOutfits(){
        return allOutfits;
    }

    public void updateOutfit(Outfit outfit){
        OutfitDatabase.databaseWriterExecutor.execute(() -> outfitDao.update(outfit));
    }

    public Long insertOutfit(Outfit outfit) throws ExecutionException, InterruptedException {
        Callable<Long> callable = () -> outfitDao.insert(outfit);
        Future<Long> future = OutfitDatabase.databaseWriterExecutor.submit(callable);

        return future.get();
    }

    public void deleteOutfit(Outfit outfit){
        OutfitDatabase.databaseWriterExecutor.execute(() -> outfitDao.delete(outfit));
    }

    public int getLastViewQueueIndex() throws ExecutionException, InterruptedException {
        Callable<Integer> callable = outfitDao::getLastViewQueueIndex;
        Future<Integer> future = OutfitDatabase.databaseWriterExecutor.submit(callable);

        return future.get();
    }

    public void updateClothingItem(ClothingItem clothingItem){
        OutfitDatabase.databaseWriterExecutor.execute(() -> clothingItemDao.update(clothingItem));
    }

    public void insertClothingItem(ClothingItem clothingItem){
        OutfitDatabase.databaseWriterExecutor.execute(() -> clothingItemDao.insert(clothingItem));
    }

    public void deleteClothingItem(ClothingItem clothingItem){
        OutfitDatabase.databaseWriterExecutor.execute(() -> clothingItemDao.delete(clothingItem));
    }

    public List<ClothingItem> getOutfitComponents(int outfitId) throws ExecutionException, InterruptedException {
        Callable<List<ClothingItem>> callable = () -> clothingItemDao.getAllFromOutfit(outfitId);
        Future<List<ClothingItem>> future = OutfitDatabase.databaseWriterExecutor.submit(callable);

        return future.get();
    }

    public LiveData<List<ClothingItem>> getClothesForOutfitLive(int outfitId) {
        return clothingItemDao.getAllFromOutfitLive(outfitId);
    }

    public Outfit getOutfitById(int id) throws ExecutionException, InterruptedException {
        Callable<Outfit> callable = () -> outfitDao.getById(id);
        Future<Outfit> future = OutfitDatabase.databaseWriterExecutor.submit(callable);

        return future.get();
    }
}
