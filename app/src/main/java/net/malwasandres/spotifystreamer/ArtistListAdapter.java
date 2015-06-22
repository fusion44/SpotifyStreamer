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

public class ArtistListAdapter extends BaseAdapter<ArtistModel, ArtistListAdapter.ViewHolder> {
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(viewType, parent, false);
        return new ViewHolder(view, mOutsideClickListener);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        ArtistModel artist = mViewModels.get(position);
        holder.artistName.setText(artist.name);
        if (!artist.imageUrl.equals("")) {
            Picasso.with(holder.artistImage.getContext())
                    .load(artist.imageUrl)
                    .into(holder.artistImage);
        }
        holder.itemView.setTag(artist);
        runAnimation(holder, position, defaultItemAnimationDuration, getAnimationDirection());
    }

    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private final BaseAdapter.ClickListener mOutSideClickReceiver;
        @InjectView(R.id.artistImageView)
        public ImageView artistImage;
        @InjectView(R.id.artistNameTextView)
        public TextView artistName;

        public ViewHolder(View itemView, ArtistListAdapter.ClickListener outSideClickReceiver) {
            super(itemView);
            ButterKnife.inject(this, itemView);
            mOutSideClickReceiver = outSideClickReceiver;
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            mOutSideClickReceiver.onItemClick((ArtistModel) view.getTag());
        }
    }
}
