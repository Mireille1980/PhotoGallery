package com.bignerdranch.android.photogallery;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class GalleryItems {

    @SerializedName("photo")
    private List<GalleryItem> mGalleryItemList;

    public List<GalleryItem> getGalleryItemList() {
        return mGalleryItemList;
    }

    public void setGalleryItemList(List<GalleryItem> galleryItemList) {
        mGalleryItemList = galleryItemList;
    }
}
