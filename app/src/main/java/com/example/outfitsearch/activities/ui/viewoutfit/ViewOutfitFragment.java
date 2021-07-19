package com.example.outfitsearch.activities.ui.viewoutfit;

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
import java.io.IOException;
import java.util.Date;

public class ViewOutfitFragment extends Fragment implements ClothingItemsAdapter.ClothingItemClickListener{

    private FragmentViewOutfitBinding binding;
    private OutfitViewModel outfitViewModel;
    private Outfit currentOutfit;

    ActivityResultLauncher<String> mGetContent = registerForActivityResult(new ActivityResultContracts.GetContent(),
            uri -> {
                // change the image immediately
                binding.textviewFileUri.setText(uri.toString());
                binding.outfitPhoto.setImageURI(uri);

                //save the new uri to the outfit db
                currentOutfit.setImageUri(uri.toString());
                outfitViewModel.updateOutfit(currentOutfit);
            });

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

        binding.buttonTakePhoto.setOnClickListener(v -> {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (photoFile != null){
                Uri photoURI = FileProvider.getUriForFile(this.requireContext(),
                        "com.example.android.fileprovider",
                        photoFile);
            }
        });

        binding.buttonAddItem.setOnClickListener((view) -> {
            
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    String currentPhotoPath;

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = requireActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,//imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();

        return image;
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