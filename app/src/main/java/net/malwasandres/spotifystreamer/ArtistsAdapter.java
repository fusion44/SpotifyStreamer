package net.malwasandres.spotifystreamer;

import android.os.Parcelable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import kaaes.spotify.webapi.android.models.Artist;

public class ArtistsAdapter extends RecyclerView.Adapter {
    ArrayList<ArtistModel> mArtists;
    private ArtistClickListener mOutsideClickListener;

    public ArtistsAdapter(ArrayList<ArtistModel> artists) {
        super();
        mArtists = artists;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.artist_search_result_item, parent, false);
        return new ViewHolder(v, mOutsideClickListener);
    }

    public void setOnArtistClickListener(ArtistClickListener outsideClickListener) {
        mOutsideClickListener = outsideClickListener;
    }

    public void add(int position, ArtistModel artist) {
        mArtists.add(position, artist);
        notifyItemInserted(position);
    }

    public void remove(ArtistModel artist) {
        int position = mArtists.indexOf(artist);
        mArtists.remove(position);
        notifyItemRemoved(position);
    }

    public void clear() {
        mArtists.clear();
    }

    public void replaceArtists(ArrayList<ArtistModel> artists) {
        mArtists.clear();
        mArtists = artists;
        notifyDataSetChanged();
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        ViewHolder vh = (ViewHolder) holder;
        ArtistModel artist = mArtists.get(position);
        vh.artistName.setText(artist.name);
        if(!artist.imageUrl.equals("")) {
            Picasso.with(vh.artistImage.getContext())
                    .load(artist.imageUrl)
                    .into(vh.artistImage);
        }
        holder.itemView.setTag(artist);
    }

    @Override
    public int getItemCount() {
        return mArtists.size();
    }

    public ArrayList<ArtistModel> getArtists() {
        return mArtists;
    }

    public interface ArtistClickListener {
        void onArtistClick(ArtistModel artist);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private final ArtistClickListener mOutSideClickReceiver;
        @InjectView(R.id.artistImageView)
        public ImageView artistImage;
        @InjectView(R.id.artistNameTextView)
        public TextView artistName;

        public ViewHolder(View itemView, ArtistClickListener outSideClickReceiver) {
            super(itemView);
            ButterKnife.inject(this, itemView);
            mOutSideClickReceiver = outSideClickReceiver;
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            mOutSideClickReceiver.onArtistClick((ArtistModel) view.getTag());
        }
    }
}
