package com.hillywave.audioplayer;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

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
        ViewHolder holder = new ViewHolder(v);

        return holder;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.title.setText(audioList.get(position).getTitle());

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((MainActivity)context).playAudio(holder.getAdapterPosition());
            }
        });

    }

    @Override
    public int getItemCount() {
        return audioList.size();
    }

}

class ViewHolder extends RecyclerView.ViewHolder{

    TextView title;
    ImageView play_pause;

    public ViewHolder(View itemView) {
        super(itemView);

        title = itemView.findViewById(R.id.title);
        play_pause = itemView.findViewById(R.id.play_pause);
    }
}
