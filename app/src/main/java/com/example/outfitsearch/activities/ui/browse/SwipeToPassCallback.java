package com.example.outfitsearch.activities.ui.browse;

import android.graphics.Canvas;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.example.outfitsearch.db.tables.Outfit;

import org.jetbrains.annotations.NotNull;

public class SwipeToPassCallback extends ItemTouchHelper.SimpleCallback {

    private final OutfitsAdapter adapter;
    private final OutfitSwipeListener swipeListener;

    public SwipeToPassCallback(OutfitsAdapter adapter, OutfitSwipeListener swipeListener) {
        super(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
        this.adapter = adapter;
        this.swipeListener = swipeListener;
    }

    @Override
    public boolean onMove(@NonNull @NotNull RecyclerView recyclerView, @NonNull @NotNull RecyclerView.ViewHolder viewHolder, @NonNull @NotNull RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onSwiped(@NonNull @NotNull RecyclerView.ViewHolder viewHolder, int direction) {
        swipeListener.onItemSwiped(adapter.getItem(viewHolder.getAdapterPosition()));
    }

    @Override
    public void onChildDraw(@NonNull @NotNull Canvas c, @NonNull @NotNull RecyclerView recyclerView, @NonNull @NotNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        //if I want to change the layout or draw something underneath, then do so here
    }

    public interface OutfitSwipeListener {
        void onItemSwiped(Outfit outfit);
    }
}
