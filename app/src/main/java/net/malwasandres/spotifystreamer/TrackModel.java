package net.malwasandres.spotifystreamer;

import android.os.Parcel;
import android.os.Parcelable;

import kaaes.spotify.webapi.android.models.Track;

// TODO: look into replacing with AutoParcel https://github.com/frankiesardo/auto-parcel
public class TrackModel extends BaseViewModel implements Parcelable {
    public static final Parcelable.Creator<TrackModel> CREATOR = new Parcelable.Creator<TrackModel>() {
        @Override
        public TrackModel createFromParcel(Parcel in) {
            return new TrackModel(in);
        }

        public TrackModel createFromTrack(Track in) {
            return new TrackModel(in);
        }

        @Override
        public TrackModel[] newArray(int size) {
            return new TrackModel[size];
        }
    };
    public String id;
    public String name;
    public String imageUrl;
    public String artistId;
    public String artistName;
    public String albumName;
    public String previewUrl;
    public int length;

    protected TrackModel(Parcel in) {
        id = in.readString();
        name = in.readString();
        imageUrl = in.readString();
        artistId = in.readString();
        artistName = in.readString();
        albumName = in.readString();
        previewUrl = in.readString();
        length = in.readInt();
        layout = R.layout.track_item;
    }

    protected TrackModel(Track in) {
        id = in.id;
        name = in.name;
        if (in.album.images.size() > 0) imageUrl = in.album.images.get(0).url;
        if (in.artists.size() > 0) artistName = in.artists.get(0).name;
        artistId = in.artists.get(0).id;
        albumName = in.album.name;
        previewUrl = in.preview_url;
        length = 0;
        layout = R.layout.track_item;
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
        dest.writeString(artistId);
        dest.writeString(artistName);
        dest.writeString(albumName);
        dest.writeString(previewUrl);
        dest.writeInt(length);
    }
}
