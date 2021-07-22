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
import java.util.Locale;

public class OutfitsAdapter extends RecyclerView.Adapter<OutfitsAdapter.ViewHolder> implements Filterable {

    private List<Outfit> outfits;
    private List<Outfit> outfitsFull;
    private final OutfitClickListener outfitClickListener;
    private LifecycleOwner lifecycleOwner;
    private Fragment parentFragment;

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
            List<Outfit> filteredList = new ArrayList<>();

            if(constraint == null || constraint.length() == 0){
                //if recipes haven't finished loading yet, wait until they have
                while (outfitsFull == null){
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                filteredList.addAll(outfitsFull);
            }
            else{
                //read and split up the query string into separate items split by commas
                String filterPattern = constraint.toString().toLowerCase().trim();
                String[] itemsToMatch = filterPattern.split(",");

                //trim item names
                for (int i = 0; i < itemsToMatch.length; i++){
                    itemsToMatch[i] = itemsToMatch[i].trim();
                }

                //find all outfits who have clothing items matching all components of the query string
                for (Outfit outfit : outfitsFull){
                    //this array maintains which ingredients have been matched
                    // as we iterate through the list of clothing items
                    boolean[] found = new boolean[itemsToMatch.length];


                    //loop through each clothing item of the outfit, and try to match against each query
                    for (ClothingItem clothingItem : outfit.getLatestClothingItems()){
                        int i = 0;
                        String itemNameLowerCase = clothingItem.getName().toLowerCase();
                        //loop through each item to match that hasn't yet been matched by this outfit
                        while (!isAllTrue(found) && i < itemsToMatch.length) {
                            if(!found[i] && itemNameLowerCase.contains(itemsToMatch[i])){
                                found[i] = true;
                            }
                            i++;
                        }
                    }

                    //add this outfit to the list if all itemsToMatch were found within its items
                    if(isAllTrue(found)){
                        filteredList.add(outfit);
                    }
                }
            }

            FilterResults results = new FilterResults();
            results.values = filteredList;

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
            File imageFile = new File(outfit.getImageUri());
            Glide.with(parentFragment)
                    .load(imageFile)
                    .signature(new ObjectKey(imageFile.lastModified()))
                    .placeholder(R.drawable.photo_placeholder)
                    .into(imageView);

            //set textbox listing clothing items, and make it update if their are changes
            outfit.getClothingItems().observe(lifecycleOwner, (clothingItems -> {
                List<ClothingItem> items = outfit.getClothingItems().getValue();
                StringBuilder stringBuilder = new StringBuilder();
                if(null != items){
                    for(ClothingItem item : items){
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
