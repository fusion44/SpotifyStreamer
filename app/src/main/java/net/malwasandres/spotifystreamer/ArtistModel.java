package net.malwasandres.spotifystreamer;

import android.os.Parcel;
import android.os.Parcelable;

import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.Track;

// TODO: look into replacing with AutoParcel https://github.com/frankiesardo/auto-parcel
public class ArtistModel implements Parcelable {
    public String id;
    public String name;
    public String imageUrl;

    protected ArtistModel(Parcel in) {
        id = in.readString();
        name = in.readString();
        imageUrl = in.readString();
    }

    protected ArtistModel(Artist in) {
        id = in.id;
        name = in.name;
        if (in.images.size() > 0) imageUrl = in.images.get(0).url;
        else imageUrl = "";
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
