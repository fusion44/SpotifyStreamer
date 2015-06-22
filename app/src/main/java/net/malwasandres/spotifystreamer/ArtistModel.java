package net.malwasandres.spotifystreamer;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import butterknife.ButterKnife;
import butterknife.InjectView;
import kaaes.spotify.webapi.android.models.Artist;

// TODO: look into replacing with AutoParcel https://github.com/frankiesardo/auto-parcel
public class ArtistModel extends BaseViewModel implements Parcelable {
    public String id;
    public String name;
    public String imageUrl;

    protected ArtistModel(Parcel in) {
        id = in.readString();
        name = in.readString();
        imageUrl = in.readString();
        layout = R.layout.artist_search_result_item;
    }

    protected ArtistModel(Artist in) {
        id = in.id;
        name = in.name;
        if (in.images.size() > 0) imageUrl = in.images.get(0).url;
        else imageUrl = "";
        layout = R.layout.artist_search_result_item;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(name);
        dest.writeString(imageUrl);
    }

    public static final Creator<ArtistModel> CREATOR = new Creator<ArtistModel>() {
        @Override
        public ArtistModel createFromParcel(Parcel in) {
            return new ArtistModel(in);
        }

        public ArtistModel createFromArtist(Artist in) {
            return new ArtistModel(in);
        }

        @Override
        public ArtistModel[] newArray(int size) {
            return new ArtistModel[size];
        }
    };
}
