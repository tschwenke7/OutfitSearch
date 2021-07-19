package com.example.outfitsearch.activities.ui.browse;

import android.content.res.Resources;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.outfitsearch.R;
import com.example.outfitsearch.db.tables.ClothingItem;
import com.example.outfitsearch.db.tables.Outfit;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class OutfitsAdapter extends RecyclerView.Adapter<OutfitsAdapter.ViewHolder> {

    private List<Outfit> outfits;
    private final OutfitClickListener outfitClickListener;

    public OutfitsAdapter(OutfitClickListener outfitClickListener) {
        this.outfitClickListener = outfitClickListener;
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
            //set thumbnail image if provided
            String uri = outfit.getImageUri();
//            if(null != uri){
//                ((ImageView) itemView.findViewById(R.id.imageview_outfit_thumbnail))
//                        .setImageURI(Uri.parse(uri));
//            }

            //set textbox listing clothing items
            List<ClothingItem> items = outfit.getClothingItems();
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

            ((TextView) itemView.findViewById(R.id.textview_outfit_contents)).setText(textViewContent);
        }
    }

    public interface OutfitClickListener {
        void onOutfitClick(int position);
    }
}
