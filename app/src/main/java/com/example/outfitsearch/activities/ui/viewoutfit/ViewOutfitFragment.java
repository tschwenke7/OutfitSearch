package com.example.outfitsearch.activities.ui.viewoutfit;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.icu.text.SimpleDateFormat;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.outfitsearch.R;
import com.example.outfitsearch.activities.ui.OutfitViewModel;
import com.example.outfitsearch.databinding.FragmentViewOutfitBinding;
import com.example.outfitsearch.db.tables.Outfit;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

public class ViewOutfitFragment extends Fragment implements ClothingItemsAdapter.ClothingItemClickListener{

    private FragmentViewOutfitBinding binding;
    private OutfitViewModel outfitViewModel;
    private Outfit currentOutfit;

    ActivityResultLauncher<String> mGetContent = registerForActivityResult(new ActivityResultContracts.GetContent(),
            uri -> {


                //save the new uri to the device
                String newFileUriString = saveImage(uri);

                //change the image immediately
                binding.textviewFileUri.setText(newFileUriString);
                binding.outfitPhoto.setImageURI(Uri.parse(newFileUriString));

                //save path to this image in db
                currentOutfit.setImageUri(newFileUriString);
                outfitViewModel.updateOutfit(currentOutfit);
            });

    private String saveImage(Uri uri) {
        try {
            //get the bitmap from the provided uri
            Bitmap bitmap = getBitmapFromUri(uri);

            //create/overwrite existing image file for this outfit's image to be saved into
            File newFile = createImageFile();

            //save the bitmap to the new file
            saveBitmapToFile(bitmap, newFile);

            //save uri to new photo in db
            return newFile.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        outfitViewModel = new ViewModelProvider(requireActivity()).get(OutfitViewModel.class);
        binding = FragmentViewOutfitBinding.inflate(inflater, container, false);

        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //specify a menu will be used for this fragment
        this.setHasOptionsMenu(true);

        populateCurrentOutfit();
        setupViews();
    }

    private void populateCurrentOutfit(){
        /* Get the Outfit to displayed on this page */
        //retrieve id from navigation args
        int outfitId = ViewOutfitFragmentArgs.fromBundle(getArguments()).getOutfitId();
        //if no outfitId was specified, this is a new outfit.
        // Request a new Outfit db entry to work with
        if(outfitId == -1){
            currentOutfit = outfitViewModel.generateNewOutfit();
        }
        //otherwise retrieve the appropriate OutFit to work with
        else{
            currentOutfit = outfitViewModel.getOutfitById(outfitId);
        }
    }

    private void setupViews(){
        //populate image if applicable
        String uriString = currentOutfit.getImageUri();
        //todo - improve this by loading lower res image dependent on size https://developer.android.com/training/camera/photobasics#java
        if(uriString != null){
            binding.outfitPhoto.setImageURI(Uri.parse(currentOutfit.getImageUri()));
        }

        //setup recyclerview of clothing items
        RecyclerView clothingRecyclerView = binding.recyclerviewClothingItems;
        final ClothingItemsAdapter adapter = new ClothingItemsAdapter(this);
        clothingRecyclerView.setAdapter(adapter);
        clothingRecyclerView.setLayoutManager(new LinearLayoutManager(this.getContext()));

        outfitViewModel.getClothesForOutfit(currentOutfit.getId())
                .observe(getViewLifecycleOwner(), adapter::setList);

        //setup buttons
        binding.buttonChoosePhoto.setOnClickListener(v -> mGetContent.launch("image/*"));

//        binding.buttonTakePhoto.setOnClickListener(v -> {
//            File photoFile = null;
//            photoFile = createImageFile();
//
//            if (photoFile != null){
//                Uri photoURI = FileProvider.getUriForFile(this.requireContext(),
//                        "com.example.android.fileprovider",
//                        photoFile);
//            }
//        });

        binding.buttonAddItem.setOnClickListener((view) -> {
            
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private File createImageFile() {
        // Create an image file name
//        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

        //get folder to save photo in
        File folder = requireActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        //create file path
        String imageFileName = "Outfit_" + currentOutfit.getId() +".jpg";

        //delete existing file if one was there - we should only have one outfit photo
        File file = new File(folder, imageFileName);
        if (file.exists()){
            file.delete();
        }

        return file;
    }

    private Bitmap getBitmapFromUri(Uri uri) throws FileNotFoundException {
        InputStream input = requireContext().getContentResolver().openInputStream(uri);
        if (input == null) {
            return null;
        }
        return BitmapFactory.decodeStream(input);
    }

    private File saveBitmapToFile(Bitmap bitmap, File file) throws IOException {
        FileOutputStream out = new FileOutputStream(file);
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
        out.flush();
        out.close();

        return file;
    }


    @Override
    public void onCreateOptionsMenu(@NonNull @NotNull Menu menu, @NonNull @NotNull MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.view_outfit_fragment_menu, menu);
    }

    @Override
    public void onDeleteClicked(int position) {
        //todo
    }
}