package com.bignerdranch.android.photogallery;

import android.app.DownloadManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class PhotoGalleryFragment extends VisibleFragment {
    private static final String API_KEY = "08dbe0448dd00d9d9e6887db8bab3a5d";
    private static final String TAG = PhotoGalleryFragment.class.getSimpleName(); // hiermee verwijs je naar de naam van de class, wordt automatisch meegewijzigd als je de naam van de class wijzigt
    private RecyclerView mPhotoRecyclerView;
    public static final String BASE_URL = "https://api.flickr.com/";
    private static List<GalleryItem> mGalleryItems;
    private int pageNumber = 1; // challenge p.428

    public static PhotoGalleryFragment newInstance() {
        return new PhotoGalleryFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);

        //Intent i = PollService.newIntent(getActivity());
        // getActivity().startService(i);
        //PollService.setServiceAlarm(getActivity(), true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_photo_gallery, menu);

        MenuItem toggleItem = menu.findItem(R.id.menu_item_toggle_polling);
        if (PollService.isServiceAlarmOn(getActivity())) {
            toggleItem.setTitle(R.string.stop_polling);
        } else {
            toggleItem.setTitle(R.string.start_polling);
        }

        MenuItem searchItem = menu.findItem(R.id.menu_item_search);
        final SearchView searchView = (SearchView) searchItem.getActionView();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                Log.d(TAG, "QueryTextSubmit: " + s);
                QueryPreferences.setStoredQuery(getActivity(), s);
                new FlickrFetch().getText(s);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                Log.d(TAG, "QueryTextChange: " + s);
                return false;
            }
        });


        searchView.setOnSearchClickListener (new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                String query = QueryPreferences.getStoredQuery(getActivity());
                searchView.setQuery(query, false);
            }
        });
    }

    //private void updateItems(){
     //   new FlickrFetch().getData();}

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_clear:
                QueryPreferences.setStoredQuery(getActivity(), null);
                new FlickrFetch().getData();
                return true;
            case R.id.menu_item_toggle_polling:
                boolean shouldStartAlarm = !PollService.isServiceAlarmOn(getActivity());
                PollService.setServiceAlarm(getActivity(), shouldStartAlarm);
                getActivity().invalidateOptionsMenu();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_photo_gallery, container, false);

        mPhotoRecyclerView = (RecyclerView) v.findViewById(R.id.fragment_photo_gallery_recycler_view);
        mPhotoRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(), 3)); // 3 columns
        mPhotoRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() { // challenge p.428 start

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (!recyclerView.canScrollVertically(dy)) {
                    if (pageNumber < 10) {
                        pageNumber++;
                        new FlickrFetch().getData();
                    }
                }
            }
        });
        new FlickrFetch().getData();

        return v;
    }

    public static List<GalleryItem> getGalleryItems() {
        return mGalleryItems; // het is static dus je zou het kunnen renamen naar sGalleryItems.
    }

    public class FlickrFetch {
        ApiEndpointInterface apiResponse;

        public void prepareRetrofitRequest(){
            //logging
            HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
            interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            OkHttpClient client = new OkHttpClient.Builder().addInterceptor(interceptor).build();

            //Create the retrofit-object
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            apiResponse = retrofit.create(ApiEndpointInterface.class);
        }

        public void getData() {
            prepareRetrofitRequest();
            apiResponse.getGalleryItems(API_KEY, pageNumber).enqueue(new Callback<GalleryApiResponse>() { // Challenge p.428 - add pageNumber
                @Override
                public void onResponse(Call<GalleryApiResponse> call, Response<GalleryApiResponse> response) {
                    setResponse(response);
                }

                @Override
                public void onFailure(Call<GalleryApiResponse> call, Throwable t) {
                    Log.e("Retrofit error", t.getMessage());
                }
            });
        }

        private void setResponse(Response<GalleryApiResponse> response) {
            GalleryApiResponse mGalleryApiResponse = response.body();
            if (response.body() == null) {
                Log.e("Retrofit body null", String.valueOf(response.code())); //Here, we send a message to the log that is the Retrofit returns null, this text should be in the error
            }
            mGalleryItems = mGalleryApiResponse.getGalleryItems();
            Log.v("mGalleryItems", String.valueOf(response.body().getGalleryItems().size())); // Show this word in the verbose LogCat if there is an error.
            if (mPhotoRecyclerView != null) {
                mPhotoRecyclerView.setAdapter(new PhotoAdapter(mGalleryItems));
            }
        }

        // Chapter 25
        public void getText(String s) {
            prepareRetrofitRequest();
            apiResponse.getSearchItems(API_KEY, s).enqueue(new Callback<GalleryApiResponse>() { // Challenge p.428 - add pageNumber
                @Override
                public void onResponse(Call<GalleryApiResponse> call, Response<GalleryApiResponse> response) {
                    setResponse(response);
                }

                @Override
                public void onFailure(Call<GalleryApiResponse> call, Throwable t) {

                }
            });
        }

        private class PhotoHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
            //private TextView mTitleTextView;
            private ImageView mItemImageView;
            private GalleryItem mGalleryItem;

            public PhotoHolder(View itemView) {
                super(itemView);
                //mTitleTextView = (TextView) itemView;
                //mItemImageView = (ImageView) itemView;
                mItemImageView = (ImageView) itemView.findViewById(R.id.iv_photo_gallery_fragment);
                itemView.setOnClickListener(this);
            }

            public void bindGalleryItem(GalleryItem item) {
                mGalleryItem = item;
                //mTitleTextView.setText(item.getId()); // we use the id instead of the title as two photos can have the same name.
                Glide.with(getActivity())
                        .load(item.getUrl()) // method chaining
                        .placeholder(R.mipmap.ic_launcher)
                        .into(mItemImageView);
            }

            @Override
            public void onClick(View v) {
                //Intent i = new Intent (Intent.ACTION_VIEW, mGalleryItem.getPhotoPageUri());
                Intent i = PhotoPageActivity.newIntent(getActivity(), mGalleryItem.getPhotoPageUri());
                startActivity(i);
            }
        }

        private class PhotoAdapter extends RecyclerView.Adapter<PhotoHolder> {

            private List<GalleryItem> mGalleryItems;

            public PhotoAdapter(List<GalleryItem> galleryItems) {
                mGalleryItems = galleryItems;
            }

            @Override
            public PhotoHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                //TextView textView = new TextView(getActivity());
                //return new PhotoHolder(textView);
                //ImageView imageView = new ImageView(getActivity());
                //return new PhotoHolder(imageView);
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.gallery_item, parent, false);
                return new PhotoHolder(view);
            }

            @Override
            public void onBindViewHolder(PhotoHolder holder, int position) {
                GalleryItem mGalleryItem = mGalleryItems.get(position);
                holder.bindGalleryItem(mGalleryItem);
            }

            @Override
            public int getItemCount() {
                return mGalleryItems.size();
            }
        }
    }
}