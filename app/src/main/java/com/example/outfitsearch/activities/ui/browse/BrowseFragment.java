package com.example.outfitsearch.activities.ui.browse;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.SearchView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.outfitsearch.R;
import com.example.outfitsearch.activities.ui.OutfitViewModel;
import com.example.outfitsearch.databinding.FragmentBrowseBinding;
import com.example.outfitsearch.utils.KeyboardHider;

import org.jetbrains.annotations.NotNull;

public class BrowseFragment extends Fragment implements OutfitsAdapter.OutfitClickListener{

    private FragmentBrowseBinding binding;
    private OutfitViewModel outfitViewModel;
    private final int GRID_ROW_SIZE = 2;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        outfitViewModel = new ViewModelProvider(requireActivity()).get(OutfitViewModel.class);
        binding = FragmentBrowseBinding.inflate(inflater, container, false);
        return binding.getRoot();

    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setHasOptionsMenu(true);
        setupViews();
//        outfitViewModel.getAllOutfits().observe(getViewLifecycleOwner(), (list) -> outfitViewModel.clearAllOutfits());

    }

    private void setupViews() {
        //setup outfit recyclerview
        RecyclerView outfitRecyclerView = binding.recyclerviewOutfits;
        final OutfitsAdapter adapter = new OutfitsAdapter(this, getViewLifecycleOwner(), this);
        RecyclerView.LayoutManager layoutManager = new GridLayoutManager(requireActivity(), GRID_ROW_SIZE);
        outfitRecyclerView.setLayoutManager(layoutManager);
        outfitRecyclerView.setAdapter(adapter);

        //observe all outfits
        outfitViewModel.getAllOutfits().observe(getViewLifecycleOwner(), (list) -> {
            adapter.setList(list);
            //hide loading spinner once outfits are loaded
            binding.loadingSpinner.setVisibility(View.GONE);
            binding.recyclerviewOutfits.setVisibility(View.VISIBLE);
        });

        /* setup search bar */
        AutoCompleteTextView searchBar = binding.searchBar;

        //configure searchbar to not allow newline character entries, but still allow wrapping
        //over multiple lines
        searchBar.setSingleLine(true);
        searchBar.setHorizontallyScrolling(false);
        searchBar.setMaxLines(20);

        //have it listen and update results in realtime as the user types
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.getFilter().filter(s);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        //hide keyboard when enter key pressed when using searchbar, so user can see the results
        searchBar.setOnEditorActionListener((v, actionId, event) -> {
            KeyboardHider.hideKeyboard(requireActivity());
            v.clearFocus();
            return false;
        });

        //setup autocomplete on the searchbar
        ArrayAdapter<String> searchBarAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_dropdown_item_1line, outfitViewModel.getAllDistinctClothingItems());
        searchBar.setAdapter(searchBarAdapter);

    }

    @Override
    public void onCreateOptionsMenu(@NonNull @NotNull Menu menu, @NonNull @NotNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.clear();
        inflater.inflate(R.menu.browse_fragment_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull @NotNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_add_outfit:
                Navigation.findNavController(requireView()).navigate(R.id.action_view_specific_outfit);
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return false;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onOutfitClick(int position) {
        //navigate to the view outfit page, passing the outfit's unique id as a parameter
        BrowseFragmentDirections.ActionViewSpecificOutfit action = BrowseFragmentDirections.actionViewSpecificOutfit();
        action.setOutfitId(outfitViewModel.getAllOutfits().getValue().get(position).getId());
        Navigation.findNavController(requireView()).navigate(action);
    }
}