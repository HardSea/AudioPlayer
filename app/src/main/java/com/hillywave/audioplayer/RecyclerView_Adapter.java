package com.hillywave.audioplayer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.hillywave.audioplayer.data.model.Audio;

import java.util.List;

public class RecyclerView_Adapter extends RecyclerView.Adapter<ViewHolder> {

    private List<Audio> audioList;
    private Context context;


    RecyclerView_Adapter(List<Audio> audioList, Context context) {
        this.audioList = audioList;
        this.context = context;
    }


    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recyclerview, parent, false);

        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        holder.setIsRecyclable(false);
        holder.title.setText(audioList.get(position).getTitle());
        holder.title2.setText(audioList.get(position).getArtist());

        if (!MainActivity.rvIsScrolling) {

            new Thread(new Runnable() {
                @Override
                public void run() {
                    MediaMetadataRetriever mMetadataRetriever = new MediaMetadataRetriever();
                    mMetadataRetriever.setDataSource(audioList.get(holder.getAdapterPosition()).getData());
                    final byte[] data = mMetadataRetriever.getEmbeddedPicture();
                    Bitmap bitmap = null;
                    if (data != null) {
                        bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                    }

                    final Bitmap finalBitmap = bitmap;

                    MainActivity.runOnUi(new Runnable() {
                        @Override
                        public void run() {
                            if (finalBitmap != null) {
                                holder.albumCover.setImageBitmap(finalBitmap);
                            }
                        }
                    });
                }

            }).start();
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((MainActivity) context).playAudio(holder.getAdapterPosition());
            }
        });

    }

    @Override
    public int getItemCount() {
        return audioList.size();
    }

}

class ViewHolder extends RecyclerView.ViewHolder {

    final TextView title;
    final TextView title2;
    final ImageView albumCover;

    ViewHolder(View itemView) {
        super(itemView);

        title = itemView.findViewById(R.id.title);
        title2 = itemView.findViewById(R.id.title2);
        albumCover = itemView.findViewById(R.id.albumCover);
        // LinearLayout linearLayoutRV = itemView.findViewById(R.id.item_rv_layout);
    }
}
