package com.example.outfitsearch.activities.ui.viewoutfit;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.outfitsearch.R;
import com.example.outfitsearch.databinding.RecyclerviewItemClothingItemBinding;
import com.example.outfitsearch.db.tables.ClothingItem;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ClothingItemsAdapter extends RecyclerView.Adapter<ClothingItemsAdapter.ViewHolder> {

    private List<ClothingItem> items;
    private final ClothingItemClickListener clickListner;
    private int selectedPos = -1;

    public ClothingItemsAdapter(ClothingItemClickListener clickListner) {
        this.clickListner = clickListner;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recyclerview_item_clothing_item, parent, false);
        return new ClothingItemsAdapter.ViewHolder(view, clickListner);
    }

    @Override
    public void onBindViewHolder(@NonNull @NotNull ViewHolder holder, int position) {
        holder.itemView.setSelected(selectedPos == position);
        holder.bind(getItem(position));
    }

    @Override
    public int getItemCount() {
        if(null == items){
            return 0;
        }
        return items.size();
    }

    public ClothingItem getItem(int pos){
        if (null == items){
            return null;
        }
        return items.get(pos);
    }

    public void setList(List<ClothingItem> newList){
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new ClothingItemsAdapter.ClothingDiff(newList, items));
        diffResult.dispatchUpdatesTo(this);
        items = newList;
    }

    private class ClothingDiff extends DiffUtil.Callback {
        List<ClothingItem> newList;
        List<ClothingItem> oldList;

        public ClothingDiff(List<ClothingItem> newList, List<ClothingItem> oldList) {
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
        private ClothingItemClickListener clickListener;

        public ViewHolder(@NonNull @NotNull View itemView, ClothingItemClickListener clickListner) {
            super(itemView);
            this.clickListener = clickListner;
        }

        public void bind(ClothingItem item){
            //set text to item's name
            ((TextView) itemView.findViewById(R.id.text_view_item_name)).setText(item.getName());

            //listen for delete button being clicked
            (itemView.findViewById(R.id.button_delete_item)).setOnClickListener((v) ->
                    clickListener.onDeleteItemClicked(getAdapterPosition()));

            //show double click hint if this item is selected
            if(itemView.isSelected()){
                itemView.findViewById(R.id.double_click_hint).setVisibility(View.VISIBLE);
            }
            else{
                itemView.findViewById(R.id.double_click_hint).setVisibility(View.GONE);
            }

            //listen for main view being clicked
            itemView.setOnClickListener((view) -> {
                //if this is the second click, notify the fragment we want to search for this item
                if(selectedPos == getAdapterPosition()){
                    clickListener.onItemDoubleClicked(getAdapterPosition());
                }
                //if it's the first click, set this item as selected and update the adapter
                else{
                    notifyItemChanged(selectedPos);
                    selectedPos = getAdapterPosition();
                    notifyItemChanged(selectedPos);
                }
            });
        }
    }

    public interface ClothingItemClickListener {
        void onDeleteItemClicked(int position);

        //search for all outfits with this item on double click or similar gesture
        void onItemDoubleClicked(int position);
    }
}
