package com.example.outfitsearch.activities.ui.viewoutfit;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.outfitsearch.R;
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

        public ViewHolder(@NonNull @NotNull View itemView, ClothingItemClickListener clickListener) {
            super(itemView);
            this.clickListener = clickListener;
        }

        public void bind(ClothingItem item){
            TextView textview = itemView.findViewById(R.id.text_view_item_name);
            EditText editText = itemView.findViewById(R.id.edit_text_item_name);
            View confirmEdit = itemView.findViewById(R.id.button_edit_item_confirm);
            View deleteButton = itemView.findViewById(R.id.button_delete_item);
            TextView hintText = itemView.findViewById(R.id.text_view_action_hint);

            //set text to item's name
            textview.setText(item.getName());
            editText.setText(item.getName());

            //listen for delete button being clicked
            deleteButton.setOnClickListener(v ->
                    clickListener.onDeleteItemClicked(getAdapterPosition()));

            //show double click hint if this item is selected
            if(itemView.isSelected()){
                hintText.setVisibility(View.VISIBLE);
                hintText.setText(R.string.double_click_item_hint);
            }
            else{
                hintText.setVisibility(View.GONE);
            }

            //listen for main view being clicked
            textview.setOnClickListener((view) -> {
                //if this is the second click, notify the fragment we want to search for this item
                if(selectedPos == getAdapterPosition()){
                    clickListener.onClothingItemDoubleClicked(getAdapterPosition());
                }
                //if it's the first click, set this item as selected and update the adapter
                else{
                    notifyItemChanged(selectedPos);
                    selectedPos = getAdapterPosition();
                    notifyItemChanged(selectedPos);
                }
            });

            //when long clicked, enable editing and show edit confirm button.
            textview.setOnLongClickListener(view -> {
                //swap visible Views to those necessary for editing
                editText.setVisibility(View.VISIBLE);
                confirmEdit.setVisibility(View.VISIBLE);
                hintText.setText(R.string.long_click_item_hint);
                hintText.setVisibility(View.VISIBLE);

                textview.setVisibility(View.GONE);
                deleteButton.setVisibility(View.GONE);

                //de-select this item if it was selected
                itemView.setSelected(false);
                if(selectedPos == getAdapterPosition()){
                    selectedPos = -1;
                }
                return true;
            });

            //change layout back to normal when confirm clicked, and notify the fragment of the update
            confirmEdit.setOnClickListener(v -> {
                textview.setText(editText.getText());

                textview.setVisibility(View.VISIBLE);
                deleteButton.setVisibility(View.VISIBLE);

                hintText.setVisibility(View.GONE);
                confirmEdit.setVisibility(View.GONE);
                editText.setVisibility(View.GONE);

                clickListener.onEditItemClicked(getAdapterPosition(), editText.getText().toString());
            });
        }
    }

    public interface ClothingItemClickListener {
        void onDeleteItemClicked(int position);

        //search for all outfits with this item on double click or similar gesture
        void onClothingItemDoubleClicked(int position);

        void onEditItemClicked(int position, String newItemName);
    }
}
