package com.example.outfitsearch.activities.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.outfitsearch.db.OutfitRepository;
import com.example.outfitsearch.db.tables.ClothingItem;
import com.example.outfitsearch.db.tables.Outfit;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class OutfitViewModel extends AndroidViewModel {
    private final OutfitRepository outfitRepository;
    private LiveData<List<Outfit>> allOutfits;

    public OutfitViewModel(@NonNull @NotNull Application application) {
        super(application);
        outfitRepository = new OutfitRepository(application);
        allOutfits = outfitRepository.getAllOutfits();
    }

    public Outfit generateNewOutfit() {
        Outfit outfit = new Outfit();
        try {
            outfit.setViewQueueIndex(outfitRepository.getLastViewQueueIndex() + 1);
            //insert into db and retrieve id
            long id = outfitRepository.insertOutfit(outfit);
            outfit.setId((int) id);

            //initialise empty list of clothing
            outfit.setClothingItems(new ArrayList<>());

            return outfit;
        }
        catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

    public Outfit getOutfitById(int id){
        try {
            Outfit outfit = outfitRepository.getOutfitById(id);
            outfit.setClothingItems(outfitRepository.getOutfitComponents(id));
            return outfit;
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

    public LiveData<List<Outfit>> getAllOutfits() {
        return allOutfits;
    }

    public LiveData<List<ClothingItem>> getClothesForOutfit(int id) {
        return outfitRepository.getClothesForOutfitLive(id);
    }

    public void updateOutfit(Outfit outfit) {
        outfitRepository.updateOutfit(outfit);
    }

    public void clearAllOutfits(){
        for (Outfit outfit : allOutfits.getValue()) {
            outfitRepository.deleteOutfit(outfit);
        }
    }

    public void deleteOutfit(Outfit outfit) {
        //delete associated image file if one exists
        if(outfit.getImageUri() != null){
            File file = new File(outfit.getImageUri());
            if (file.exists()){
                file.delete();
            }
        }
        //delete the db entry
        outfitRepository.deleteOutfit(outfit);
    }

    public void addItemToOutfit(String itemName, Outfit currentOutfit) {
        //populate clothing item db entry
        ClothingItem clothingItem = new ClothingItem();
        clothingItem.setName(itemName);
        clothingItem.setOutfitId(currentOutfit.getId());

        //insert into db
        outfitRepository.insertClothingItem(clothingItem);
    }

    public void deleteClothingItem(ClothingItem clothingItem) {
        outfitRepository.deleteClothingItem(clothingItem);
    }
}
