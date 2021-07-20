package com.example.outfitsearch.activities.ui.viewoutfit;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.outfitsearch.R;
import com.example.outfitsearch.activities.ui.OutfitViewModel;
import com.example.outfitsearch.databinding.FragmentViewOutfitBinding;
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

public class ViewOutfitFragment extends Fragment
        implements ClothingItemsAdapter.ClothingItemClickListener,
        AdapterView.OnItemSelectedListener
{

    private FragmentViewOutfitBinding binding;
    private OutfitViewModel outfitViewModel;
    private Outfit currentOutfit;
    private List<String> seasonOptions;
    private List<String> formalityOptions;
    private String TAG = "tom_test";

    ActivityResultLauncher<String> mGetContent = registerForActivityResult(new ActivityResultContracts.GetContent(),
            uri -> {

                //get the uri of where this photo will be saved
                LiveData<String> newUriLiveData = outfitViewModel.saveImage(currentOutfit, uri);
                newUriLiveData.observe(getViewLifecycleOwner(), new Observer<String>() {
                    @Override
                    public void onChanged(String newUri) {
                        //change the image immediately
//                        binding.outfitPhoto.setImageURI(null);
                        ImageUtils.setPic(binding.outfitPhoto, newUri);
                        newUriLiveData.removeObserver(this);
                    }
                });
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
        //if an image has been provided, scale it to the available viewspace and load
        //we have to wait for preDrawListener to know how big the view actually is
        if(uriString != null){
            ImageView outfitPhoto = binding.outfitPhoto;
            outfitPhoto.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    ImageUtils.setPic(binding.outfitPhoto, currentOutfit.getImageUri());
                    outfitPhoto.getViewTreeObserver().removeOnPreDrawListener(this);
                    return false;
                }
            });

        }

        //setup recyclerview of clothing items
        RecyclerView clothingRecyclerView = binding.recyclerviewClothingItems;
        final ClothingItemsAdapter adapter = new ClothingItemsAdapter(this);
        clothingRecyclerView.setAdapter(adapter);
        clothingRecyclerView.setLayoutManager(new LinearLayoutManager(this.getContext()));
        currentOutfit.getClothingItems().observe(getViewLifecycleOwner(), adapter::setList);

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

        //setup listener to add new clothing item
        binding.buttonAddItem.setOnClickListener((view) -> {
            TextView input = binding.editTextAddItem;
            String inputString = input.getText().toString();
            if (!inputString.isEmpty()){
                outfitViewModel.addItemToOutfit(inputString, currentOutfit);

                //clear edittext after consuming its contents
                input.setText("");
            }
        });

        //setup autocomplete for adding clothing items
        ArrayAdapter<String> autoCompleteAdapter = new ArrayAdapter<>(
                getContext(), android.R.layout.simple_dropdown_item_1line,
                outfitViewModel.getAllDistinctClothingItems());
        binding.editTextAddItem.setAdapter(autoCompleteAdapter);

        //setup season spinner
        seasonOptions = new ArrayList<>();
        seasonOptions.add(getResources().getString(R.string.no_selection_placeholder));
        seasonOptions.addAll(outfitViewModel.getDistinctSeasons());
        seasonOptions.add(getResources().getString(R.string.add_new_spinner_option));
        binding.spinnerSeason.setAdapter(new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_dropdown_item, seasonOptions));
        //set selected option to correct option
        if(currentOutfit.getSeason() != null){
            binding.spinnerSeason.setSelection(seasonOptions.indexOf(currentOutfit.getSeason()));
        }
        //listen to selections
        binding.spinnerSeason.setOnItemSelectedListener(this);

        //setup formality spinner
        formalityOptions = new ArrayList<>();
        formalityOptions.add(getResources().getString(R.string.no_selection_placeholder));
        formalityOptions.addAll(outfitViewModel.getDistinctFormalities());
        formalityOptions.add(getResources().getString(R.string.add_new_spinner_option));
        binding.spinnerFormality.setAdapter(new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_dropdown_item, formalityOptions));
        //set selected option to correct option
        if(currentOutfit.getFormality() != null){
            binding.spinnerFormality.setSelection(formalityOptions.indexOf(currentOutfit.getFormality()));
        }
        //listen to selections
        binding.spinnerFormality.setOnItemSelectedListener(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull @NotNull Menu menu, @NonNull @NotNull MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.view_outfit_fragment_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull @NotNull MenuItem item) {
        switch(item.getItemId()){
            //respond to delete menu option being clicked by prompting to delete this outfit
            case R.id.action_delete_outfit:
                View root = requireView();
                new AlertDialog.Builder(root.getContext())
                        .setTitle(R.string.delete_outfit_warning)
                        .setMessage(root.getContext().getString(R.string.delete_outfit_warning_prompt))
                        .setPositiveButton(R.string.delete, (dialogInterface, i) -> {
                            Toast.makeText(root.getContext(), R.string.delete_outfit_success, Toast.LENGTH_LONG).show();
                            //delete outfit from db
                            outfitViewModel.deleteOutfit(currentOutfit);
                            //navigate back to browse fragment
                            Navigation.findNavController(root).navigate(R.id.action_view_outfit_to_browse);
                        })
                        //otherwise don't do anything
                        .setNegativeButton(R.string.cancel, null)
                        .show();
                break;
        }
        return false;
    }

    @Override
    public void onDeleteItemClicked(int position) {
        ClothingItem item = currentOutfit.getClothingItems().getValue().get(position);
        if(null != item){
            outfitViewModel.deleteClothingItem(item);
        }
    }


    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long id) {
        EditText input = new EditText(requireContext());
        switch (adapterView.getId()){
            case R.id.spinner_season:
                //if it's the first option, that translates to "no options selected" so set to null
                if(pos == 0){
                    currentOutfit.setSeason(null);
                    outfitViewModel.updateOutfit(currentOutfit);
                }
                //if the last option, "add new" was selected, we need to prompt to add a new option
                else if(pos == seasonOptions.size() - 1){
                    input.setHint(R.string.new_season_hint);
                    input.setText("");
                    new AlertDialog.Builder(requireContext())
                            .setTitle(R.string.new_season_title)
                            .setView(input)
                            .setPositiveButton(R.string.add_button, (dialog, which) -> {
                                addNewSeason(input.getText().toString());
                            })
                            .setNegativeButton(R.string.cancel, null)
                            .show();
                }
                //otherwise update outfit with selection
                else{
                    currentOutfit.setSeason(seasonOptions.get(pos));
                    outfitViewModel.updateOutfit(currentOutfit);
                }
                break;
            case R.id.spinner_formality:
                //if it's the first option, that translates to "no options selected" so set to null
                if(pos == 0){
                    currentOutfit.setFormality(null);
                    outfitViewModel.updateOutfit(currentOutfit);
                }
                //if the last option, "add new" was selected, we need to prompt to add a new option
                else if(pos == formalityOptions.size() - 1){
                    input.setHint(R.string.new_formality_hint);
                    input.setText("");
                    new AlertDialog.Builder(requireContext())
                            .setTitle(R.string.new_formality_title)
                            .setView(input)
                            .setPositiveButton(R.string.add_button, (dialog, which) -> {
                                addNewFormality(input.getText().toString());
                            })
                            .setNegativeButton(R.string.cancel, null)
                            .show();
                }
                //otherwise update outfit with selection
                else{
                    currentOutfit.setFormality(formalityOptions.get(pos));
                    outfitViewModel.updateOutfit(currentOutfit);
                }
                break;
        }
    }

    private void addNewSeason(String newSeason){
        //if a value was provided
        if(!newSeason.isEmpty()){
            //add the new option to the spinner, if it didn't already exist
            if(!seasonOptions.contains(newSeason)){
                //update adapter of spinner
                seasonOptions = new ArrayList<>();
                seasonOptions.add(getResources().getString(R.string.no_selection_placeholder));
                seasonOptions.addAll(outfitViewModel.getDistinctSeasons());
                seasonOptions.add(newSeason);
                seasonOptions.add(getResources().getString(R.string.add_new_spinner_option));
                binding.spinnerSeason.setAdapter(new ArrayAdapter<>(getContext(),
                        android.R.layout.simple_spinner_dropdown_item, seasonOptions));
            }
            //set selection to the new option
            binding.spinnerSeason.setSelection(seasonOptions.indexOf(newSeason));
            //this will also call onItemSelected, which will update outfit

//            currentOutfit.setSeason(newSeason);
//            outfitViewModel.updateOutfit(currentOutfit);
        }
    }

    private void addNewFormality(String newFormality){
        //if a value was provided
        if(!newFormality.isEmpty()){
            //add the new option to the spinner, if it didn't already exist
            if(!formalityOptions.contains(newFormality)){
                //update adapter of spinner
                formalityOptions = new ArrayList<>();
                formalityOptions.add(getResources().getString(R.string.no_selection_placeholder));
                formalityOptions.addAll(outfitViewModel.getDistinctFormalities());
                formalityOptions.add(newFormality);
                formalityOptions.add(getResources().getString(R.string.add_new_spinner_option));
                binding.spinnerFormality.setAdapter(new ArrayAdapter<>(getContext(),
                        android.R.layout.simple_spinner_dropdown_item, formalityOptions));
            }
            //set selection to the new option
            binding.spinnerFormality.setSelection(formalityOptions.indexOf(newFormality));

            //update outfit
            currentOutfit.setFormality(newFormality);
            outfitViewModel.updateOutfit(currentOutfit);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        Log.d(TAG, "onNothingSelected: ");
    }
}