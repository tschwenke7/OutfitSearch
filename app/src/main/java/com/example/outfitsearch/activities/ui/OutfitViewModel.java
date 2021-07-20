package com.example.outfitsearch.activities.ui;

import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.telecom.Call;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.example.outfitsearch.App;
import com.example.outfitsearch.db.OutfitRepository;
import com.example.outfitsearch.db.tables.ClothingItem;
import com.example.outfitsearch.db.tables.Outfit;
import com.example.outfitsearch.utils.ImageUtils;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

public class OutfitViewModel extends AndroidViewModel {
    private final OutfitRepository outfitRepository;
    private final MutableLiveData<List<Outfit>> allOutfits = new MutableLiveData<>();
    private final App app;

    public OutfitViewModel(@NonNull @NotNull Application application) {
        super(application);
        outfitRepository = new OutfitRepository(application);

        //when db changes, retrieve clothing items and populate outfits with them
        Observer<List<Outfit>> outfitObserver = outfits -> {
            //when db changes, retrieve clothing items and populate outfits with them
            List<Outfit> populatedOutfits = new ArrayList<>();
            for (Outfit outfit : outfits) {
                outfit.setClothingItems(getClothesForOutfit(outfit.getId()));
                populatedOutfits.add(outfit);
            }

            allOutfits.setValue(populatedOutfits);
        };

        app = (App) application;
        outfitRepository.getAllOutfits().observeForever(outfitObserver);
    }

    public Outfit generateNewOutfit() {
        Outfit outfit = new Outfit();
        try {
            outfit.setViewQueueIndex(outfitRepository.getLastViewQueueIndex() + 1);
            //insert into db and retrieve id
            long id = outfitRepository.insertOutfit(outfit);
            outfit.setId((int) id);

            //initialise empty list of clothing
            outfit.setClothingItems(getClothesForOutfit((int) id));

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
            outfit.setClothingItems(outfitRepository.getClothesForOutfitLive(id));
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

    public List<ClothingItem> getClothesForOutfitNonLive(int id) {
        try {
            return outfitRepository.getClothesForOutfitNonLive(id);
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }
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

    public String[] getAllDistinctClothingItems(){
        try {
            return outfitRepository.getAllDistinctClothingItems();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
        return new String[0];
    }

    public List<String> getDistinctSeasons(){
        try {
            return outfitRepository.getDistinctSeasons();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    public List<String> getDistinctFormalities(){
        try {
            return outfitRepository.getDistinctFormalities();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    public MutableLiveData<String> saveImage(Outfit currentOutfit, Uri uri) {
        MutableLiveData<String> liveStr = new MutableLiveData<>();

        app.backgroundExecutorService.execute( () -> {
            //get the bitmap from the provided uri
            Bitmap bitmap = null;
            try {
                bitmap = getBitmapFromUri(uri);

                //create/overwrite existing image file for this outfit's image to be saved into
                File newFile = createImageFile("Outfit_" + currentOutfit.getId() +".jpg");

                //save the bitmap to the new file
                saveBitmapToFile(bitmap, newFile);

                //save uri to new photo in db
                currentOutfit.setImageUri(newFile.getAbsolutePath());
                updateOutfit(currentOutfit);

                liveStr.postValue(currentOutfit.getImageUri());

            } catch (IOException e) {
                e.printStackTrace();
                liveStr.postValue(null);
            }
        });

        return liveStr;
    }

    private File createImageFile(String imageFileName) {
        //get folder to save photo in
        File folder = app.getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        //delete existing file if one was there - we should only have one outfit photo
        File file = new File(folder, imageFileName);
        if (file.exists()){
            file.delete();
        }

        return file;
    }

    private Bitmap getBitmapFromUri(Uri uri) throws FileNotFoundException {
        //find the orientation of the image, in case it needs to be reoriented
        int orientation = 1;
        try {
            InputStream exifChecker = app.getContentResolver().openInputStream(uri);
            ExifInterface exif = new ExifInterface(exifChecker);
            orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
            exifChecker.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //read bitmap and reorient if necessary
        InputStream input = app.getContentResolver().openInputStream(uri);
        if (input == null) {
            return null;
        }
        return ImageUtils.rotateBitmap(orientation, BitmapFactory.decodeStream(input));
    }

    private File saveBitmapToFile(Bitmap bitmap, File file) throws IOException {
        FileOutputStream out = new FileOutputStream(file);
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
        out.flush();
        out.close();

        return file;
    }
}
