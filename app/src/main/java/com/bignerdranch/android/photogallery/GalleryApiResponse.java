package com.bignerdranch.android.photogallery;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class GalleryApiResponse {
    @SerializedName("photos")
    private GalleryItems mGalleryItems;

    public GalleryApiResponse(){
    }

    public List<GalleryItem> getGalleryItems(){
        return mGalleryItems.getGalleryItemList();
    }
}
