package net.malwasandres.spotifystreamer;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import butterknife.ButterKnife;
import butterknife.InjectView;
import kaaes.spotify.webapi.android.models.Track;

public class TrackAdapter extends RecyclerView.Adapter {
    ArrayList<TrackModel> mTracks;
    private TrackClickListener mOutsideClickListener;

    public TrackAdapter(ArrayList<TrackModel> tracks) {
        super();
        mTracks = tracks;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.track_item, parent, false);
        return new ViewHolder(v, mOutsideClickListener);
    }

    public void setOnTrackClickListener(TrackClickListener outsideClickListener) {
        mOutsideClickListener = outsideClickListener;
    }

    public void add(int position, TrackModel track) {
        mTracks.add(position, track);
        notifyItemInserted(position);
    }

    public void remove(Track track) {
        if(!mTracks.contains(track)) return;

        int position = mTracks.indexOf(track);
        mTracks.remove(position);
        notifyItemRemoved(position);
    }

    public void clear() {
        mTracks.clear();
    }

    public ArrayList<TrackModel> getTracks() {
        return mTracks;
    }


    public void replaceTracks(ArrayList<TrackModel> tracks) {
        mTracks.clear();
        mTracks = tracks;
        notifyDataSetChanged();
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        ViewHolder vh = (ViewHolder) holder;
        TrackModel track = mTracks.get(position);
        vh.trackNameTextView.setText(track.name);
        if(!track.imageUrl.equals("")) {
            Picasso.with(vh.albumImageView.getContext())
                    .load(track.imageUrl)
                    .into(vh.albumImageView);
        }
        vh.albumNameTextView.setText(track.albumName);
        holder.itemView.setTag(track);
    }

    @Override
    public int getItemCount() {
        return mTracks.size();
    }

    public interface TrackClickListener {
        void onTrackClick(TrackModel track);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private final TrackClickListener mOutSideClickReceiver;
        @InjectView(R.id.playbackAlbumImageView)
        public ImageView albumImageView;
        @InjectView(R.id.playbackAlbumNameTextView)
        public TextView albumNameTextView;
        @InjectView(R.id.trackNameTextView)
        public TextView trackNameTextView;


        public ViewHolder(View itemView, TrackClickListener outSideClickReceiver) {
            super(itemView);
            ButterKnife.inject(this, itemView);
            mOutSideClickReceiver = outSideClickReceiver;
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            mOutSideClickReceiver.onTrackClick((TrackModel) view.getTag());
        }
    }
}
