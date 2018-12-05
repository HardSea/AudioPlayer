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


    // TODO: Rename and change types of parameters
    private String mTitle;
    private String mArtist;
    private TextView tv1;
    private TextView tv2;
    private SeekBar seekBar;

    private OnFragmentInteractionListener mListener;

    public BlankFragment() {  }



//    // TODO: Rename and change types and number of parameters
//    public static BlankFragment newInstance(String param1, String param2) {
//        BlankFragment fragment = new BlankFragment();
//        Bundle args = new Bundle();
//        args.putString(ARG_PARAM1, param1);
//        args.putString(ARG_PARAM2, param2);
//        fragment.setArguments(args);
//        return fragment;
//    }



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
        ImageButton btnprev = view.findViewById(R.id.btnPrev);
        ImageButton btnPause = view.findViewById(R.id.btnPause);
        ImageButton btnNext = view.findViewById(R.id.btnNext);




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

        tv1 = view.findViewById(R.id.nameArtistFragment);
        tv2 = view.findViewById(R.id.nameTitleFragment);
        seekBar = view.findViewById(R.id.seekBar);




        if (mTitle != null && mArtist != null){
            tv1.setText(mArtist);
            tv2.setText(mTitle);
        }



        return view;
    }

    public void changeSongInfo(String artist, String title){
        mArtist = artist;
        mTitle = title;
        tv1.setText(mArtist);
        tv2.setText(mTitle);
    }

    public void changeSeekBarProgres(int progress){
        Log.d(TAG, "changeSeekBarProgres: changeSeekBarProgres " + progress);
        seekBar.setProgress(progress);
    }



    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressedPrev() {
        if (mListener != null) {
            mListener.prevSong();
        }
    }

    public void onButtonPressedPause() {
        if (mListener != null) {
            mListener.pauseSong();
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


    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void prevSong();
        void pauseSong();
        void nextSong();
    }

}
