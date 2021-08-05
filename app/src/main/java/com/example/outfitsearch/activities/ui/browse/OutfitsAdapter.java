package com.example.outfitsearch.activities.ui.browse;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.signature.ObjectKey;
import com.example.outfitsearch.R;
import com.example.outfitsearch.db.tables.ClothingItem;
import com.example.outfitsearch.db.tables.Outfit;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class OutfitsAdapter extends RecyclerView.Adapter<OutfitsAdapter.ViewHolder> implements Filterable {

    private List<Outfit> outfits;
    private List<Outfit> outfitsFull;
    private final OutfitClickListener outfitClickListener;
    private LifecycleOwner lifecycleOwner;
    private Fragment parentFragment;
    private String matchSeason;
    private String matchFormality;

    public OutfitsAdapter(OutfitClickListener outfitClickListener, LifecycleOwner lifecycleOwner, Fragment parentFragment) {
        this.outfitClickListener = outfitClickListener;
        this.lifecycleOwner = lifecycleOwner;
        this.parentFragment = parentFragment;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recyclerview_item_outfit, parent, false);
        return new OutfitsAdapter.ViewHolder(view, outfitClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull @NotNull ViewHolder holder, int position) {
        holder.bind(outfits.get(position));
    }

    @Override
    public int getItemCount() {
        if(null == outfits){
            return 0;
        }
        return outfits.size();
    }

    public Outfit getItem(int pos){
        if (null == outfits){
            return null;
        }
        return outfits.get(pos);
    }

    public void setList(List<Outfit> newList){
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new OutfitDiff(newList, outfits));
        diffResult.dispatchUpdatesTo(this);
        outfits = newList;
        outfitsFull = new ArrayList<>(newList);
    }

    public Filter getFilter() {
        return filter;
    }

    private Filter filter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<Outfit> finalFilteredList = new ArrayList<>();

            //if recipes haven't finished loading yet, wait until they have
//            while (outfitsFull == null){
//                try {
//                    Thread.sleep(50);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }

            //first, filter all outfits that don't meet the season and formality filters set, if applicable
            //if the filter values are null, that means any value is allowed
            List<Outfit> categoricallyFilteredList = new ArrayList<>();

            //skip this step to save time if all categories are null
            if ((matchFormality == null && matchSeason == null)){
                categoricallyFilteredList = outfitsFull;
            }
            else{
                for (Outfit outfit : outfitsFull) {
                    if (
                        (matchSeason == null || matchSeason.equals(outfit.getSeason())) &&
                        (matchFormality == null || matchFormality.equals(outfit.getFormality()))
                    ){
                        categoricallyFilteredList.add(outfit);
                    }
                }
            }


            //secondly, search through the remaining outfits to find those
            //which have clothing items matching all components of the query string
            if(constraint == null || constraint.length() == 0){
                //if the query string is empty, simply add all
                finalFilteredList.addAll(categoricallyFilteredList);
            }
            else{
                //read and split up the query string into separate items split by commas
                String filterPattern = constraint.toString().toLowerCase().trim();
                String[] itemsToMatch = filterPattern.split(",");
                /** Corresponds to items to match. If true, find outfits with items containing this
                 * query, if false, find with NO item with occurrences of this query */
                boolean[] itemMatchMode = new boolean[itemsToMatch.length];

                //trim item names and check if they are to be found or not found (not found if ending with "!")
                for (int i = 0; i < itemsToMatch.length; i++){
                    itemsToMatch[i] = itemsToMatch[i].trim();
                    if(itemsToMatch[i].endsWith("!")){
                        itemMatchMode[i] = false;
                        //remove "!" from the query term
                        itemsToMatch[i] = itemsToMatch[i].substring(0, itemsToMatch[i].length() - 1);
                    }
                    else{
                        itemMatchMode[i] = true;
                    }
                }

                //find all outfits which match the search criteria
                for (Outfit outfit : categoricallyFilteredList) {
                    //this array maintains which query components have been matched
                    //as we iterate through them
                    boolean[] matched = new boolean[itemsToMatch.length];

                    //loop through each clothing item of the outfit,
                    //and try to match against each thus far unfound query component
                    outfitLoop: for (ClothingItem clothingItem : outfit.getLatestClothingItems()) {
                        int i = 0;
                        String itemNameLowerCase = clothingItem.getName().toLowerCase();
                        //loop through each item to match that hasn't yet been matched by this outfit
                        while (!isAllTrue(matched) && i < itemsToMatch.length) {
                            //items ending ing '!' should NOT be found in an outfit to match
                            if (itemMatchMode[i] == false){
                                //end the search immediately if a '!' item is found
                                if (itemNameLowerCase.contains(itemsToMatch[i])) {
                                    matched[i] = false;
                                    break outfitLoop;
                                }
                                //if the '!' item wasn't found then this condition query is matched -
                                //once we've checked ALL clothing items don't contain it
                                else{
                                    //set matched = true if this was the last clothing item and the query was never found
                                    if (clothingItem.getId() == outfit.getLatestClothingItems().get(outfit.getLatestClothingItems().size() - 1).getId())
                                        matched[i] = true;
                                }
                            }
                            //otherwise attempt to find the query within this clothing item
                            else if (!matched[i] && itemNameLowerCase.contains(itemsToMatch[i])) {
                                matched[i] = true;
                            }
                            i++;
                        }
                    }

                    //add this outfit to the list if all itemsToMatch were found within its items
                    if (isAllTrue(matched)) {
                        finalFilteredList.add(outfit);
                    }
                }
            }

            FilterResults results = new FilterResults();
            results.values = finalFilteredList;

            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            outfits.clear();
            if (results.values != null) {
                outfits.addAll((List) results.values);
            }
            notifyDataSetChanged();
        }
    };

    private boolean isAllTrue(boolean... array){
        for (boolean b: array){
            if(!b) return false;
        }
        return true;
    }

    public void setMatchSeason(String matchSeason) {
        this.matchSeason = matchSeason;
    }

    public void setMatchFormality(String matchFormality) {
        this.matchFormality = matchFormality;
    }

    private class OutfitDiff extends DiffUtil.Callback {
        List<Outfit> newList;
        List<Outfit> oldList;

        public OutfitDiff(List<Outfit> newList, List<Outfit> oldList) {
            this.newList = newList;
            this.oldList = oldList;
        }

        @Override
        public int getOldListSize() {
            if(oldList == null){
                return 0;
            }
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            if(newList == null){
                return 0;
            }
            return newList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).getId() == newList.get(newItemPosition).getId();
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).equals(newList.get(newItemPosition));
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder{
        private View itemView;
        private OutfitClickListener outfitClickListener;

        public ViewHolder(@NonNull View itemView, OutfitClickListener outfitClickListener) {
            super(itemView);
            this.itemView = itemView;
            this.outfitClickListener = outfitClickListener;
        }

        public void bind(Outfit outfit){
            //todo add recyclerview specific preloader - http://bumptech.github.io/glide/int/recyclerview.html
            //set thumbnail image if provided
            ImageView imageView = itemView.findViewById(R.id.imageview_outfit_thumbnail);
            if(outfit.getImageUri() != null){
                File imageFile = new File(outfit.getImageUri());
                Glide.with(parentFragment)
                        .load(imageFile)
                        .signature(new ObjectKey(imageFile.lastModified()))
                        .placeholder(R.drawable.photo_placeholder)
                        .into(imageView);
            }

            //set textbox listing clothing items, and make it update if their are changes
            outfit.getClothingItems().observe(lifecycleOwner, (clothingItems -> {
                StringBuilder stringBuilder = new StringBuilder();
                if(null != clothingItems){
                    for(ClothingItem item : clothingItems){
                        stringBuilder.append(item.getName() + ", ");
                    }
                }
                //remove the trailing comma and space, if any items were added
                String textViewContent = stringBuilder.toString();
                if(textViewContent.length() > 0){
                    textViewContent = textViewContent.substring(0,textViewContent.lastIndexOf(","));
                }
                //if no items, then use placeholder text
                else {
                    textViewContent = "No clothing items have been listed - click to add some.";
                }

                //set list of items
                TextView textView = itemView.findViewById(R.id.textview_outfit_contents);
                textView.setText(textViewContent);

                //now that everything is loaded, show the contents and hide the loading bar
                itemView.findViewById(R.id.item_names_loading_spinner).setVisibility(View.GONE);
                textView.setVisibility(View.VISIBLE);
            }));

            //set click listener for outfit to navigate to view outfit page
            itemView.setOnClickListener((v) -> outfitClickListener.onOutfitClick(getAdapterPosition()));
        }
    }

    public interface OutfitClickListener {
        void onOutfitClick(int position);
    }
}
