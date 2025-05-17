package com.example.helpinghands;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * RecyclerView Adapter for creating an image slider/carousel
 * Displays a horizontal list of images that can be scrolled
 */
public class ImageSliderAdapter extends RecyclerView.Adapter<ImageSliderAdapter.ImageSliderViewHolder> {

    // List of image resource IDs (R.drawable.xxx) to display
    private List<Integer> imageList;

    // Context needed for layout inflation
    private Context context;

    /**
     * Constructor to initialize the adapter
     * @param context The activity/fragment context
     * @param imageList List of image resource IDs to display
     */
    public ImageSliderAdapter(Context context, List<Integer> imageList) {
        this.context = context;
        this.imageList = imageList;
    }

    /**
     * Called when RecyclerView needs a new ViewHolder
     * @param parent The ViewGroup into which the new View will be added
     * @param viewType The type of the new View (not used here)
     * @return A new ViewHolder that holds the ImageView
     */
    @NonNull
    @Override
    public ImageSliderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the item layout
        View view = LayoutInflater.from(context).inflate(R.layout.image_slider_item, parent, false);
        return new ImageSliderViewHolder(view);
    }

    /**
     * Binds the image data to the ViewHolder at the specified position
     * @param holder The ViewHolder to update
     * @param position The position in the data set
     */
    @Override
    public void onBindViewHolder(@NonNull ImageSliderViewHolder holder, int position) {
        // Set the image resource for the current position
        holder.imageView.setImageResource(imageList.get(position));
    }

    /**
     * Returns the total number of items in the data set
     * @return The size of imageList
     */
    @Override
    public int getItemCount() {
        return imageList.size();
    }

    /**
     * ViewHolder class that holds references to the ImageView
     * Improves performance by avoiding repeated findViewById() calls
     */
    public static class ImageSliderViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView; // Reference to the ImageView in the layout

        /**
         * Constructor to initialize the ViewHolder
         * @param itemView The inflated view for this item
         */
        public ImageSliderViewHolder(@NonNull View itemView) {
            super(itemView);
            // Find and store the ImageView reference
            imageView = itemView.findViewById(R.id.imageView);
        }
    }
}