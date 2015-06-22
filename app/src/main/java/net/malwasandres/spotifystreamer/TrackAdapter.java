package net.malwasandres.spotifystreamer;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class TrackAdapter extends BaseAdapter<TrackModel, TrackAdapter.ViewHolder> {
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.track_item, parent, false);
        return new ViewHolder(v, mOutsideClickListener);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        TrackModel track = mViewModels.get(position);
        holder.trackNameTextView.setText(track.name);
        if (!track.imageUrl.equals("")) {
            Picasso.with(holder.albumImageView.getContext())
                    .load(track.imageUrl)
                    .into(holder.albumImageView);
        }
        holder.albumNameTextView.setText(track.albumName);
        holder.itemView.setTag(track);
        runAnimation(holder, position, defaultItemAnimationDuration, getAnimationDirection());
    }

    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private final BaseAdapter.ClickListener mOutSideClickReceiver;
        @InjectView(R.id.playbackAlbumImageView)
        public ImageView albumImageView;
        @InjectView(R.id.playbackAlbumNameTextView)
        public TextView albumNameTextView;
        @InjectView(R.id.trackNameTextView)
        public TextView trackNameTextView;


        public ViewHolder(View itemView, BaseAdapter.ClickListener outSideClickReceiver) {
            super(itemView);
            ButterKnife.inject(this, itemView);
            mOutSideClickReceiver = outSideClickReceiver;
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            mOutSideClickReceiver.onItemClick((TrackModel) view.getTag());
        }
    }
}
