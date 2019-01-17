package com.hillywave.audioplayer;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;


public class BlankFragment extends Fragment {

    private static final String ARG_TITLE = "audio_title";
    private static final String ARG_ARTIST = "audio_artist";
    private static final String TAG = "BlankFragment";


    private String mTitle;
    private String mArtist;
    private TextView tv1;
    private TextView tv2;
    private SeekBar seekBar;
    private ImageButton btnPause;
    private ImageButton btnprev;
    private ImageButton btnNext;

    private OnFragmentInteractionListener mListener;

    public BlankFragment() {  }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");

        if (getArguments() != null) Log.d(TAG, getArguments().toString());

        if (getArguments() != null) {
            mTitle = getArguments().getString(ARG_TITLE);
            mArtist = getArguments().getString(ARG_ARTIST);
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        Log.d(TAG, "onCreateView");


        View view = inflater.inflate(R.layout.fragment_box, container, false);
        btnprev = view.findViewById(R.id.btnPrev);
        btnPause = view.findViewById(R.id.btnPause);
        btnNext = view.findViewById(R.id.btnNext);
        seekBar = view.findViewById(R.id.seekBar);

        btnPause.setImageResource(android.R.drawable.ic_media_pause);

        btnprev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onButtonPressedPrev();
            }
        });

        btnPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onButtonPressedPause();
            }
        });

        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onButtonPressedNext();
            }
        });

        btnPause.setClickable(false);
        btnNext.setClickable(false);
        btnprev.setClickable(false);
        seekBar.setVisibility(View.INVISIBLE);




        tv1 = view.findViewById(R.id.nameArtistFragment);
        tv2 = view.findViewById(R.id.nameTitleFragment);


        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

                mListener.changeTimeSong(seekBar.getProgress() * 1000);

            }


        });


        if (mTitle != null && mArtist != null){
            tv1.setText(mArtist);
            tv2.setText(mTitle);
        }
        return view;
    }



    public void changeSongInfo(String artist, String title){
        btnNext.setClickable(true);
        btnPause.setClickable(true);
        btnprev.setClickable(true);
        seekBar.setVisibility(View.VISIBLE);
        mArtist = artist;
        mTitle = title;
        tv1.setText(mArtist);
        tv2.setText(mTitle);
    }

    public void changeSeekBarProgres(int progress, int allProgress){
        seekBar.setMax(allProgress);
        seekBar.setProgress(progress);
    }


    public void onButtonPressedPrev() {
        if (mListener != null) {
            mListener.prevSong();
        }
    }

    public void onButtonPressedPause() {
        if (mListener != null) {
            mListener.pauseSong();
            if (new StorageUtil(getContext()).getPlaybackStatus()) {
                btnPause.setImageResource(android.R.drawable.ic_media_pause);
            } else {
                btnPause.setImageResource(android.R.drawable.ic_media_play);
            }


        }
    }

    public void onButtonPressedNext() {
        if (mListener != null) {
            mListener.nextSong();
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;

        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public void changeButton() {
        if (new StorageUtil(getContext()).getPlaybackStatus()){
            btnPause.setImageResource(android.R.drawable.ic_media_pause);
        } else {
            btnPause.setImageResource(android.R.drawable.ic_media_play);
        }
    }


    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void prevSong();
        void pauseSong();
        void nextSong();
        void changeTimeSong(int i);
    }

}
