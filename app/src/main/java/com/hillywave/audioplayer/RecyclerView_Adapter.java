package com.hillywave.audioplayer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.MediaMetadataRetriever;
import android.support.v7.widget.RecyclerView;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.util.List;

public class RecyclerView_Adapter extends RecyclerView.Adapter<ViewHolder>{

    List<Audio> audioList;
    Context context;


    public RecyclerView_Adapter(List<Audio> audioList, Context context){
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
        holder.albumCover.setImageResource(R.drawable.ic_launcher_foreground);




//            try{
//                holder.albumCover.setImageBitmap(audioList.get(position).getImage());
//
//            }catch (Exception e){
//                e.printStackTrace();
//            }




        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((MainActivity)context).playAudio(holder.getAdapterPosition());
                Log.d("Audio index", "Service onClick: " + holder.getAdapterPosition());
            }
        });

    }

    @Override
    public int getItemCount() {
        return audioList.size();
    }

}

class ViewHolder extends RecyclerView.ViewHolder{

    final TextView title;
    final TextView title2;
    final ImageView albumCover;

    public ViewHolder(View itemView) {
        super(itemView);

        title = itemView.findViewById(R.id.title);
        title2 = itemView.findViewById(R.id.title2);
        albumCover = itemView.findViewById(R.id.albumCover);

    }
}
