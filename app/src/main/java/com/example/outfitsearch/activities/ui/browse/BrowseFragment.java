package com.example.outfitsearch.activities.ui.browse;

import android.os.Bundle;
import android.os.Parcelable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.MultiAutoCompleteTextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.example.outfitsearch.R;
import com.example.outfitsearch.activities.ui.OutfitViewModel;
import com.example.outfitsearch.databinding.FragmentBrowseBinding;
import com.example.outfitsearch.db.tables.Outfit;
import com.example.outfitsearch.utils.KeyboardHider;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class BrowseFragment extends Fragment implements
        OutfitsAdapter.OutfitClickListener,
        SwipeToPassCallback.OutfitSwipeListener,
        AdapterView.OnItemSelectedListener {

    private FragmentBrowseBinding binding;
    private OutfitViewModel outfitViewModel;
    private final int GRID_ROW_SIZE = 2;
    private List<String> seasonOptions;
    private List<String> formalityOptions;
    private OutfitsAdapter outfitsAdapter;

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
        /* setup outfit recyclerview */
        RecyclerView outfitRecyclerView = binding.recyclerviewOutfits;
        outfitsAdapter = new OutfitsAdapter(this, getViewLifecycleOwner(), this);
        RecyclerView.LayoutManager layoutManager = new GridLayoutManager(requireActivity(), GRID_ROW_SIZE);
        outfitRecyclerView.setLayoutManager(layoutManager);
        outfitRecyclerView.setAdapter(outfitsAdapter);

        //observe all outfits
        outfitViewModel.getAllOutfits().observe(getViewLifecycleOwner(), (list) -> {
            //if there are no outfits, show a message telling the user to add some
            if(list.isEmpty()){
                binding.textviewNoResults.setVisibility(View.VISIBLE);
                binding.textviewNoResults.setText(R.string.no_outfits_exist);
            }
            //otherwise set the list to the adapter
            else{
                binding.textviewNoResults.setVisibility(View.GONE);
                Parcelable recyclerViewState = outfitRecyclerView.getLayoutManager().onSaveInstanceState();
                outfitsAdapter.setList(list);
                outfitRecyclerView.getLayoutManager().onRestoreInstanceState(recyclerViewState);
            }

            //hide loading spinner once outfits are loaded
            binding.loadingSpinner.setVisibility(View.GONE);
            binding.recyclerviewOutfits.setVisibility(View.VISIBLE);
        });

        //listen for if adapter ever becomes empty due to search returning no results
        outfitsAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                checkEmpty();
            }

            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                checkEmpty();
            }

            @Override
            public void onItemRangeRemoved(int positionStart, int itemCount) {
                super.onItemRangeRemoved(positionStart, itemCount);
                checkEmpty();
            }

            /** Shows the "No results" message if the adapter is showing 0 items,
             *  or hides it if it's not empty */
            void checkEmpty() {
                //if the search results are empty, show the no results message.
                if (outfitsAdapter.getItemCount() == 0){
                    binding.textviewNoResults.setText(R.string.no_search_results_message);
                    binding.textviewNoResults.setVisibility(View.VISIBLE);
                }
                //otherwise hide the message
                else{
                    binding.textviewNoResults.setVisibility(View.GONE);
                }
            }
        });

        //add itemTouchListener to detect outfit swipes
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(
                new SwipeToPassCallback(outfitsAdapter, this));
        itemTouchHelper.attachToRecyclerView(outfitRecyclerView);

        /* setup search bar */
        MultiAutoCompleteTextView searchBar = binding.searchBar;

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
                outfitsAdapter.getFilter().filter(s);
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
        //configure autocomplete to consider comma separated phrases as separate tokens
        searchBar.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());

        /* Setup category drop down spinners */
        //setup season spinner
        seasonOptions = new ArrayList<>();
        seasonOptions.add(getResources().getString(R.string.all_spinner_option));
        seasonOptions.addAll(outfitViewModel.getDistinctSeasons());

        binding.spinnerSeason.setAdapter(new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_dropdown_item, seasonOptions));

        //listen to selections
        binding.spinnerSeason.setOnItemSelectedListener(this);

        //setup formality spinner
        formalityOptions = new ArrayList<>();
        formalityOptions.add(getResources().getString(R.string.all_spinner_option));
        formalityOptions.addAll(outfitViewModel.getDistinctFormalities());

        binding.spinnerFormality.setAdapter(new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_dropdown_item, formalityOptions));

        //listen to selections
        binding.spinnerFormality.setOnItemSelectedListener(this);
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

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long id) {
        switch (adapterView.getId()){
            case R.id.spinner_season:
                //if it's the first option, that translates to "allow all" so set to null
                if(pos == 0){
                    outfitsAdapter.setMatchSeason(null);
                }
                //otherwise tell adapter to filter by the selected season
                else{
                    outfitsAdapter.setMatchSeason(seasonOptions.get(pos));
                }
                break;
            case R.id.spinner_formality:
                //if it's the first option, that translates to "allow all" so set to null
                if(pos == 0){
                    outfitsAdapter.setMatchFormality(null);
                }
                //otherwise tell adapter to filter by the selected formality
                else{
                    outfitsAdapter.setMatchFormality(formalityOptions.get(pos));
                }
                break;
        }
        //after updating the filter categories, we should call filter again on the current query
        //to update the results
        String currentQuery = binding.searchBar.getText().toString();
        outfitsAdapter.getFilter().filter(currentQuery);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {}

    /**
     * Respond to outfit swipes by sending the outfit to the back of the viewQueue (making it end
     * up at the bottom of browsing).
     * @param outfit
     */
    @Override
    public void onItemSwiped(Outfit outfit) {
        outfitViewModel.sendToBackOfViewQueue(outfit);
    }
}