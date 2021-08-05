package com.example.outfitsearch.activities.ui;

import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;

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
import java.util.Random;
import java.util.concurrent.ExecutionException;

public class OutfitViewModel extends AndroidViewModel {
    private final OutfitRepository outfitRepository;
    private final MutableLiveData<List<Outfit>> allOutfits = new MutableLiveData<>();
    private final App app;
    private Random random = new Random();
    /** A list of positions in the currently displayed search results in BrowseFragment's recyclerview,
     * which have not been randomly selected from the "Choose for me" button since the recyclerview
     * last changed. */
    private List<Integer> unchosenRandomIndices;
    /** The number of search results last time the OutfitViewModel#resetRandomOutfitQueue method was
     * called. */
    private int previousResultSpaceSize;

    public OutfitViewModel(@NonNull @NotNull Application application) {
        super(application);
        outfitRepository = new OutfitRepository(application);

        //when db changes, retrieve clothing items and populate outfits with them
        Observer<List<Outfit>> outfitObserver = outfits -> {
            //when db changes, retrieve clothing items and populate outfits with them
            List<Outfit> populatedOutfits = new ArrayList<>();
            for (Outfit outfit : outfits) {
                outfit.setClothingItems(getClothesForOutfit(outfit.getId()));
                //update non-LiveData version when changes detected
                outfit.getClothingItems().observeForever(outfit::setLatestClothingItems);
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

    public MutableLiveData<String> saveImage(Outfit currentOutfit, Uri uri, int scaleWidth,
                                             boolean deleteSourceOnFinish) {
        MutableLiveData<String> liveStr = new MutableLiveData<>();

        app.backgroundExecutorService.execute( () -> {
            //get the bitmap from the provided uri
            Bitmap bitmap = null;
            try {
                bitmap = getBitmapFromUri(uri, scaleWidth);

                //create/overwrite existing image file for this outfit's image to be saved into
                File newFile = createImageFile("Outfit_" + currentOutfit.getId() +".jpg");

                //save the bitmap to the new file
                saveBitmapToFile(bitmap, newFile);

                //save uri to new photo in db
                currentOutfit.setImageUri(newFile.getAbsolutePath());
                updateOutfit(currentOutfit);

                liveStr.postValue(currentOutfit.getImageUri());

                //delete source file if requested now that we are done with it
                if(deleteSourceOnFinish){
                    File usedTempFile = new File(uri.toString());
                    usedTempFile.delete();
                }

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

    private Bitmap getBitmapFromUri(Uri uri, int scaleWidth) throws FileNotFoundException {
        try {
            //find the orientation of the image, in case it needs to be reoriented
            InputStream exifChecker = app.getContentResolver().openInputStream(uri);
            ExifInterface exif = new ExifInterface(exifChecker);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
            exifChecker.close();

            //read bitmap from file
            InputStream input = app.getContentResolver().openInputStream(uri);
            if (input == null) {
                return null;
            }

            Bitmap original = BitmapFactory.decodeStream(input);
            input.close();

            //scale it to the width of the screen (biggest size we'd ever need) to reduce file size
            //calculate scale height to keep aspect ratio the same
            int scaleHeight = (int) (original.getHeight() * ((float) scaleWidth / original.getWidth()) );
            Bitmap scaled = Bitmap.createScaledBitmap(original, scaleWidth, scaleHeight, true);

            return ImageUtils.rotateBitmap(orientation, scaled);

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private File saveBitmapToFile(Bitmap bitmap, File file) throws IOException {
        //compress the image as jpg to save space
        //adjust the quality percent as desired
        int compressionQualityPercent = 80;
        FileOutputStream out = new FileOutputStream(file);
        bitmap.compress(Bitmap.CompressFormat.JPEG, compressionQualityPercent, out);

        //write to the file
        out.flush();
        out.close();

        return file;
    }

    /** Sets the viewQueueIndex of this outfit to 1 higher than the current highest value,
     * sending the outfit to the end of the viewing order.
     * @param outfit - the outfit to be sent to the back of the viewQueue
     */
    public void sendToBackOfViewQueue(Outfit outfit) {
        try {
            outfit.setViewQueueIndex(outfitRepository.getLastViewQueueIndex() + 1);
            outfitRepository.updateOutfit(outfit);
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    /** Generates a list of integers starting from 0 incrementing by 1 of the specified size
     * @param size - the size of list to generate
     * @return a list of size 'size' starting at 0 and counting up by 1s e.g. [0, 1, 2, ..., <size-1>]
     */
    private List<Integer> newSequentialList(int size){
        List<Integer> list = new ArrayList<>();
        for(int i = 0; i < size; i++){
            list.add(i);
        }
        return list;
    }

    /**
     * Call this method every time the displayed content of the outfit recyclerview is changed.
     * If the number of search results has changed, resets the list of results positions which
     * haven't yet been chosen by getRandomOutfitListIndex.
     * @param size - the new size of the outfitRecyclerView adapter's contents.
     */
    public void resetRandomOutfitQueue(int size) {
        if(size != previousResultSpaceSize){
            unchosenRandomIndices = newSequentialList(size);
            previousResultSpaceSize = size;
        }
    }

    /**
     * Chooses a random index from within the size of the outfitAdapter's current list size
     * without replacement (until all indices have been chosen once, at which point all are replaced).
     * Make sure to keep the bounds up to date by calling outfitViewModel#resetRandomOutfitQueue
     * each time the list changes size.
     * @return - a random index within the bounds of 0 to outfitAdapter.getItemCount()-1,
     */
    public int getRandomOutfitListIndex() {
        //if random has already chosen every outfit, refresh the remaining randoms list
        if (unchosenRandomIndices.isEmpty()){
            unchosenRandomIndices = newSequentialList(previousResultSpaceSize);
        }

        //randomly chooses an index that hasn't already been chosen from the current outfit list
        return unchosenRandomIndices.remove(random.nextInt(unchosenRandomIndices.size()));
    }

    /**
     * Updates all the name of this clothing item and all clothing items with the same name in other
     * outfits to the newItemName
     * @param item - the original item being changed.
     * @param newItemName - the new name to change to.
     */
    public void editClothingItemName(ClothingItem item, String newItemName) {
        //retrieve all database entries that match the name of this clothing item, and update each
        //of them with the new item name
        LiveData<List<ClothingItem>> dataToFetch = outfitRepository.getAllClothingItemsWithName(item.getName());
        dataToFetch.observeForever(new Observer<List<ClothingItem>>() {
            @Override
            public void onChanged(List<ClothingItem> clothingItems) {
                for (ClothingItem c : clothingItems) {
                    c.setName(newItemName);
                }
                outfitRepository.updateClothingItems(clothingItems);
                dataToFetch.removeObserver(this);
            }
        });
    }
}
