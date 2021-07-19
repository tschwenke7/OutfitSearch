package com.example.outfitsearch.activities.ui.browse;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.outfitsearch.R;
import com.example.outfitsearch.activities.ui.OutfitViewModel;
import com.example.outfitsearch.activities.ui.viewoutfit.ViewOutfitFragment;
import com.example.outfitsearch.databinding.FragmentBrowseBinding;

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

        RecyclerView outfitRecyclerView = binding.recyclerviewOutfits;
        final OutfitsAdapter adapter = new OutfitsAdapter(this);
        //if this doesn't work, look here for ref https://stackoverflow.com/questions/42136980/recyclerview-with-rows-and-columns
        RecyclerView.LayoutManager layoutManager = new GridLayoutManager(requireActivity(), GRID_ROW_SIZE);
        outfitRecyclerView.setLayoutManager(layoutManager);
        outfitRecyclerView.setAdapter(adapter);



        //observe all outfits
        outfitViewModel.getAllOutfits().observe(getViewLifecycleOwner(), adapter::setList);
//        outfitViewModel.getAllOutfits().observe(getViewLifecycleOwner(), (list) -> outfitViewModel.clearAllOutfits());

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