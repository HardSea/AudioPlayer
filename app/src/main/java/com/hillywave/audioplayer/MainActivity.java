package com.hillywave.audioplayer;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;


public class MainActivity extends AppCompatActivity implements BottomPlayerFragment.OnFragmentInteractionListener{

    private MediaPlayerService player;
    boolean serviceBound = false;
    private ArrayList<Audio> audioList;

    private MediaMetadataRetriever mMetadataRetriever;

    private RecyclerView.Adapter adapter;
    private RecyclerView recyclerView;
    private Toolbar toolBar;
    private ImageButton sortBtn;
    private ImageView imgAlbumCover;
    private SeekBar seekBarPlayer;
    private SeekBar seekBarVolume;
    private TextView textView_currentTime;
    private TextView textView_allTime;
    private TextView textView_titleSong;
    private TextView textView_artistSong;
    private TextView textView_cntSong;
    private ImageButton btnRepeatPlayer;
    private ImageButton btnPrevPlayer;
    private ImageButton btnPlayPlayer;
    private ImageButton btnNextPlayer;
    private ImageButton btnShufflePlayer;


    private SlidingUpPanelLayout slidingLayout;

    public static Handler UIHandler;
    public static boolean rvIsScrolling = false;

    private BroadcastReceiver mBroadcastReceiver;
    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;

    private AudioManager audioManager;
    public static final String Broadcast_PLAY_NEW_AUDIO = "com.hillywave.audioplayer.PlayNewAudio";
    public static final String Broadcast_UPDATE_PLAYLIST = "com.hillywave.audioplayer.UpdatePlaylist";
    public static final String RECEIVER_INTENT = "RECEIVER_INTENT";
    public static final String CURRENT_POSITION = "RECEIVER_MESSAGE";
    public static final String ALL_DURATION = "RECEIVER_MESSAGE2";;
    public static final String ACTION = "RECEIVER_MESSAGE3";
    public static final int CHANGE_SEEKBAR = 15511;
    public static final int NEW_AUDIO = 14411;
    public static final int NOTIFICATION_ID = 114411;
    private static final String TAG = "MainActivity";

    static {
        UIHandler = new Handler(Looper.getMainLooper());
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        setContentView(R.layout.activity_main);

        toolBar = findViewById(R.id.toolBar);
        setSupportActionBar(toolBar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);
        sortBtn = toolBar.findViewById(R.id.sortBtn);
        sortBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                callSortDialog();
            }
        });



        seekBarPlayer = findViewById(R.id.seekBar2);
        seekBarPlayer.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                textView_currentTime.setText(convertTime(seekBar.getProgress()));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (!(new StorageUtil(getApplicationContext()).getPlaybackStatus())){
                    pauseSong();
                }

                changeTimeSong(seekBarPlayer.getProgress() * 1000);
                textView_currentTime.setText(convertTime(seekBar.getProgress()));
            }
        });

        initPlayerElements();

        mMetadataRetriever = new MediaMetadataRetriever();
        imgAlbumCover = findViewById(R.id.img_album_cover);

        prefs = getApplicationContext().getSharedPreferences("default_preference", MODE_PRIVATE);
        editor = prefs.edit();
        editor.apply();



        mBroadcastReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {

                switch(intent.getIntExtra(ACTION, CHANGE_SEEKBAR)){
                    case (CHANGE_SEEKBAR):
                    int message = intent.getIntExtra(CURRENT_POSITION, -1);
                    int message2 = intent.getIntExtra(ALL_DURATION, 1);
                    setSeekBarProgress(message, message2);
                    break;
                    case (NEW_AUDIO):
                    changeButtonBoxInfo();
                    break;
                    default:
                        break;

                }
               // boolean message3 = intent.getBooleanExtra(PLAY_STATUS,false);
               // playBackstatus = message3;



            }
        };

        audioList = new ArrayList<>();


        if (!checkPermissionForReadExtertalStorage()) {
            try {
                requestPermissionForReadExtertalStorage();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            createlist();
        }

    }

    private void initPlayerElements(){
        textView_allTime = findViewById(R.id.textView_allTime);
        textView_currentTime = findViewById(R.id.textView_currentTime);
        textView_titleSong = findViewById(R.id.textView_titleSong);
        textView_artistSong = findViewById(R.id.textView_artistSong);
        textView_cntSong = findViewById(R.id.textView_cntSong);

        seekBarVolume= findViewById(R.id.seekBarVolume);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        btnRepeatPlayer =findViewById(R.id.btnRepeatPlayer);
        btnPrevPlayer =findViewById(R.id.btnPrevPlayer);
        btnPlayPlayer =findViewById(R.id.btnPlayPlayer);
        btnNextPlayer =findViewById(R.id.btnNextPlayer);
        btnShufflePlayer =findViewById(R.id.btnRandomPlayer);

        changeRepeatButton();
        changeShuffleButton();


        slidingLayout =  findViewById(R.id.sliding_layout);
        slidingLayout.addPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {
            @Override
            public void onPanelSlide(View panel, float slideOffset) {

            }

            @Override
            public void onPanelStateChanged(View panel, SlidingUpPanelLayout.PanelState previousState, SlidingUpPanelLayout.PanelState newState) {

                switch (newState){
                    case COLLAPSED:
                        panel.findViewById(R.id.fragment_box).setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));
                        break;
                    case EXPANDED:
                        panel.findViewById(R.id.fragment_box).setLayoutParams(new LinearLayout.LayoutParams(0, 0));
                    case DRAGGING:
                        if (previousState == SlidingUpPanelLayout.PanelState.COLLAPSED){
                            panel.findViewById(R.id.fragment_box).setLayoutParams(new LinearLayout.LayoutParams(0, 0));
                        } else if (previousState == SlidingUpPanelLayout.PanelState.EXPANDED){
                            panel.findViewById(R.id.fragment_box).setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));
                        }
                        break;
                    default:
                        break;
                }
            }
        });



        seekBarVolume.setMax(audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
        seekBarVolume.setProgress(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
        seekBarVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, seekBar.getProgress(), 0);
            }
        });

        btnRepeatPlayer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 0 - no repeat
                // 1 - repeat list
                // 2 - repeat one

                new StorageUtil(getApplicationContext()).changeRepeatStatus();
                changeRepeatButton();
            }
        });

        btnPrevPlayer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                prevSong();
            }
        });

        btnPlayPlayer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pauseSong();
                if (new StorageUtil(getApplicationContext()).getPlaybackStatus()) {
                    btnPlayPlayer.setImageResource(R.drawable.ico_pause);
                } else {
                    btnPlayPlayer.setImageResource(R.drawable.ico_play);
                }
            }
        });

        btnNextPlayer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                nextSong();
            }
        });

        btnShufflePlayer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new StorageUtil(getApplicationContext()).changeShuffleStatus();
                changeShuffleButton();

            }
        });
    }


    private void changeShuffleButton(){
        if (new StorageUtil(getApplicationContext()).getShuffleStatus()){
            btnShufflePlayer.setImageResource(R.drawable.ico_shuffle);
        } else {
            btnShufflePlayer.setImageResource(R.drawable.ico_shuffle_white);
        }
    }

    private void changeRepeatButton(){
        switch (new StorageUtil(getApplicationContext()).getRepeatStatus()){

            case 0:
                btnRepeatPlayer.setImageResource(R.drawable.ico_repeat_white);
                break;
            case 1:
                btnRepeatPlayer.setImageResource(R.drawable.ico_repeat);
                break;
            case 2:
                btnRepeatPlayer.setImageResource(R.drawable.ico_repeat_one);
                break;

            default:
                break;
        }
    }




    private void callSortDialog(){
        final View layout = getLayoutInflater().inflate(R.layout.fragment_setorder, null, false);
        final RadioGroup radioGroup = layout.findViewById(R.id.radioGroup_setOrder);
        final CheckBox checkBox = layout.findViewById(R.id.reverseOrder);

        switch (prefs.getInt("order_option", 3)){
            // 1 - Sort by date
            // 2 - Sort by artist
            // 3 - Sort by title
            // 4 - Sort by album
            case 1:
                radioGroup.check(R.id.radioButtonDate);
                break;
            case 2:
                radioGroup.check(R.id.radioButtonArtist);
                break;
            case 3:
                radioGroup.check(R.id.radioButtonTitle);
                break;
            case 4:
                radioGroup.check(R.id.radioButtonAlbum);
                break;
            default:
                break;
        }
        if (prefs.getBoolean("order_reverseOrder", false)){
            checkBox.setChecked(true);
        } else {
            checkBox.setChecked(false);
        }




        final PopupWindow changeSortPopUp = new PopupWindow(getApplicationContext());
        changeSortPopUp.setContentView(layout);
        changeSortPopUp.setWidth(LinearLayout.LayoutParams.WRAP_CONTENT);
        changeSortPopUp.setHeight(LinearLayout.LayoutParams.WRAP_CONTENT);
        changeSortPopUp.setFocusable(true);
        changeSortPopUp.setAnimationStyle(R.style.animation_popupwindow);


        changeSortPopUp.setBackgroundDrawable(new BitmapDrawable());


        findViewById(R.id.main_layout).post(new Runnable() {
            public void run() {
                changeSortPopUp.showAtLocation(findViewById(R.id.main_layout), Gravity.CENTER, 0, 0);
            }
        });


        Button close =  layout.findViewById(R.id.setOrder_confirmBtn);
        close.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // 1 - Sort by date
                // 2 - Sort by artist
                // 3 - Sort by title
                // 4 - Sort by album

                switch (radioGroup.getCheckedRadioButtonId()){
                    case R.id.radioButtonDate:
                        editor.putInt("order_option", 1);
                        break;
                    case R.id.radioButtonArtist:
                        editor.putInt("order_option", 2);
                        break;
                    case R.id.radioButtonTitle:
                        editor.putInt("order_option", 3);
                        break;
                    case R.id.radioButtonAlbum:
                        editor.putInt("order_option", 4);
                        break;
                    default:
                        break;
                }
                if (checkBox.isChecked()){
                    editor.putBoolean("order_reverseOrder", true);
                } else {
                    editor.putBoolean("order_reverseOrder", false);
                }

                editor.apply();
                changeSortPopUp.dismiss();

                loadAudio();
                adapter.notifyDataSetChanged();

                Intent i = new Intent(Broadcast_UPDATE_PLAYLIST);
                sendBroadcast(i);
            }
        });



    }

    private void createlist(){
        loadAudio();
        initRecyclerView();

    }

    @Override
    protected void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(this).registerReceiver((mBroadcastReceiver), new IntentFilter(RECEIVER_INTENT));
        changeButtonBoxInfo();

    }

    @Override
    protected void onStop() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
        super.onStop();

    }

    private void initRecyclerView(){
        Log.d(TAG, "initRecyclerView: ");
        if (audioList.size() > 0){
            recyclerView = findViewById(R.id.recyclerview);
            recyclerView.setHasFixedSize(true);
            adapter = new RecyclerView_Adapter(audioList, this);
            recyclerView.setAdapter(adapter);
            RecyclerView.LayoutManager lm = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
            FastScroller fastScroller = (FastScroller) findViewById(R.id.fastscroller);
            fastScroller.setRecyclerView(recyclerView);
            lm.setAutoMeasureEnabled(false);
            recyclerView.setLayoutManager(lm);


        }
        Log.d(TAG, "initRecyclerView: end");
    }


    public void requestPermissionForReadExtertalStorage() throws Exception {
        try {

            int READ_STORAGE_PERMISSION_REQUEST_CODE = 1;
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, READ_STORAGE_PERMISSION_REQUEST_CODE);

        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    createlist();

                } else {
                    Toast.makeText(getApplicationContext(), "Додатку необхіден доступ для зчитування файлів", Toast.LENGTH_LONG).show();

                    try {
                        requestPermissionForReadExtertalStorage();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

    }

    public boolean checkPermissionForReadExtertalStorage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int result = this.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
            return result == PackageManager.PERMISSION_GRANTED;
        }
        return false;
    }


    //void playAudio(String media){
    void playAudio(int position){
        Log.d(TAG, "onCreate: " + new StorageUtil(getApplicationContext()).getPlaybackStatus());
        Log.d(TAG, "onCreate: " +  (player == null));
        StorageUtil storage = new StorageUtil(getApplicationContext());

        if (!serviceBound){

            storage.storeAudioIndex(position);

            Intent playerIntent = new Intent(this, MediaPlayerService.class);
            //playerIntent.putExtra("media", audioList.get(position).getData());

            startService(playerIntent);
            bindService(playerIntent, serviceConnection, Context.BIND_AUTO_CREATE);

        } else {
            storage.storeAudioIndex(position);

            Intent broadcastIntent = new Intent(Broadcast_PLAY_NEW_AUDIO);
            sendBroadcast(broadcastIntent);

            //Service is active
            //Send media with BroadcastReceiver

        }
        recyclerView.scrollToPosition(position);
        updateTrackInfo();
        changeButtonBoxInfo();


    }

    void playAudio(){
        StorageUtil storage = new StorageUtil(getApplicationContext());
        if (storage.loadAudioIndex() != -1) {

            Log.d(TAG, "playAudio: " + serviceBound);

            if (player == null){
                serviceBound = false;
            }

            if (!serviceBound) {


                Intent playerIntent = new Intent(this, MediaPlayerService.class);
                //playerIntent.putExtra("media", audioList.get(position).getData());

                startService(playerIntent);
                bindService(playerIntent, serviceConnection, Context.BIND_AUTO_CREATE);

            } else {
                Intent broadcastIntent = new Intent(Broadcast_PLAY_NEW_AUDIO);
                sendBroadcast(broadcastIntent);

                //Service is active
                //Send media with BroadcastReceiver

            }

            updateTrackInfo();
            changeButtonBoxInfo();

        }
    }

    private void updateTrackInfo(){
            StorageUtil storage = new StorageUtil(getApplicationContext());
            int pos = storage.loadAudioIndex();

        Audio activeAudio =  audioList.get(pos);

        MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();
        metadataRetriever.setDataSource(activeAudio.getData());
        byte[] art;
        try{

            art = metadataRetriever.getEmbeddedPicture();
            Bitmap song_cover = BitmapFactory.decodeByteArray(art, 0, art.length);

        } catch (Exception e){
            e.printStackTrace();
        }


    }



    private void loadAudio(){


        if (!checkPermissionForReadExtertalStorage()){
            try {
                requestPermissionForReadExtertalStorage();
            } catch (Exception e) {
                e.printStackTrace();
            }

            } else {

            audioList.clear();
            new StorageUtil(getApplicationContext()).clearList();

            ContentResolver contentResolver = getContentResolver();

            MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();

            Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            String selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0";
            String setOrder = createOrderString();
            Cursor cursor = contentResolver.query(uri, null, selection, null, setOrder);


            if (cursor != null && cursor.getCount() > 0) {
                while (cursor.moveToNext()) {
                    String data = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
                    String title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                    String album = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
                    String artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                    String display_name = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME));
                    String duration = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));
                    String year = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.YEAR));
                    Long lastchange = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DATE_MODIFIED));




                   // audioList.add(new Audio(data, title, album, artist, display_name, duration, year, image));
                    audioList.add(new Audio(data, title, album, artist, display_name, duration, year, lastchange));

                }
                new StorageUtil(getApplicationContext()).storeAudio(audioList);
            }

            assert cursor != null;
            cursor.close();
        }
    }

    private String createOrderString(){

        // 1 - Sort by date
        // 2 - Sort by artist
        // 3 - Sort by title
        // 4 - Sort by album
        String s = "";

        switch (prefs.getInt("order_option", 3)){
            case 1:
                s = s.concat(MediaStore.Audio.Media.DATE_MODIFIED);
                break;
            case 2:
                s = s.concat(MediaStore.Audio.Media.ARTIST);
                break;
            case 3:
                s = s.concat(MediaStore.Audio.Media.TITLE);
                break;
            case 4:
                s = s.concat(MediaStore.Audio.Media.ALBUM);
                break;
            default:
                break;
        }

        if (prefs.getBoolean("order_reverseOrder", false)){
            s = s.concat(" ASC");
        } else {
            s = s.concat(" DESC");
        }

        return s;



    }



    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

            MediaPlayerService.LocalBinder binder = (MediaPlayerService.LocalBinder) service;
            player = binder.getService();
            serviceBound = true;

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
        }
    };


    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean("ServiceState", serviceBound);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        serviceBound = savedInstanceState.getBoolean("ServiceState");
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (serviceBound){
            pauseSong();
            unbindService(serviceConnection);
            player.stopSelf();
        }
        new StorageUtil(getApplicationContext()).setPlaybackStatus(false);
    }




    private void changeButtonBoxInfo(){
        StorageUtil storage = new StorageUtil(getApplicationContext());
        int audioIndex = storage.loadAudioIndex();
        Log.d("Audio index", "Main Activity changebuttonBox Info: " + audioIndex);
        ArrayList<Audio> audioListfragment = storage.loadAudio();

        BottomPlayerFragment articleFrag = (BottomPlayerFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_box);

        Log.d(TAG, "changeButtonBoxInfo: " + articleFrag);
        if (audioIndex != -1) {

            if (articleFrag != null) {
                try {
                    articleFrag.changeSongInfo(audioListfragment.get(audioIndex).getArtist(), audioListfragment.get(audioIndex).getTitle());

                } catch (Exception e){
                    e.printStackTrace();
                    articleFrag.changeSongInfo("", "");
                }

            } else {

                BottomPlayerFragment newFragment = new BottomPlayerFragment();
                Bundle args = new Bundle();
                args.putString("audio_title", audioListfragment.get(audioIndex).getTitle());
                args.putString("audio_artist", audioListfragment.get(audioIndex).getArtist());


                newFragment.setArguments(args);

                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                transaction.replace(R.id.fragment_box, newFragment);
                transaction.addToBackStack(null);
                transaction.commit();
            }

            changePlayerLayout(audioListfragment.get(audioIndex).getData(), audioListfragment.get(audioIndex).getTitle(), audioListfragment.get(audioIndex).getArtist(), audioIndex, audioListfragment.size());
        }
    }

    private void changePlayerLayout(final String imageData, String titleSong, String artistSong, int position, int allSongCnt){

        new Thread(new Runnable() {
            @Override
            public void run() {
                    mMetadataRetriever.setDataSource(imageData);
                    final byte[] data = mMetadataRetriever.getEmbeddedPicture();
                    Bitmap bitmap = null;
                    if(data != null) {
                        bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                    }

                    final Bitmap finalBitmap = bitmap;

                    MainActivity.runOnUi(new Runnable() {
                        @Override
                        public void run() {
                        if (finalBitmap != null){
                            imgAlbumCover.setImageBitmap(finalBitmap);

                        } else {
                            imgAlbumCover.setImageResource(R.drawable.image);
                        }
                        }
                    });
                }

        }).start();


        textView_titleSong.setText(titleSong);
        textView_artistSong.setText(artistSong);
        textView_cntSong.setText((position + 1) + " з " + allSongCnt);

    }

    public void setSeekBarProgress(int progress, int allProgrss){

        seekBarPlayer.setMax(allProgrss);
        seekBarPlayer.setProgress(progress);

        textView_currentTime.setText(convertTime(progress));
        textView_allTime.setText(convertTime(allProgrss));


        StorageUtil storage = new StorageUtil(getApplicationContext());
        int audioIndex = storage.loadAudioIndex();
        ArrayList<Audio> audioListfragment = storage.loadAudio();


        BottomPlayerFragment articleFrag = (BottomPlayerFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_box);

        if (articleFrag != null) {
            articleFrag.changeSeekBarProgres(progress, allProgrss);


            articleFrag.changeButton();
            if (new StorageUtil(getApplicationContext()).getPlaybackStatus()){
                btnPlayPlayer.setImageResource(R.drawable.ico_pause);
            } else {
                btnPlayPlayer.setImageResource(R.drawable.ico_play);
            }


        } else {

            BottomPlayerFragment newFragment = new BottomPlayerFragment();
            Bundle args = new Bundle();
            args.putString("audio_title", audioListfragment.get(audioIndex).getTitle());
            args.putString("audio_artist", audioListfragment.get(audioIndex).getArtist());


            newFragment.setArguments(args);

            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_box, newFragment);
            transaction.addToBackStack(null);
            transaction.commit();
        }


    }

    @Override
    public void prevSong() {

        StorageUtil storage = new StorageUtil(getApplicationContext());

        if (storage.loadAudioIndex() - 1 >= 0) {
            storage.minusIndex();
        } else if (storage.loadAudioIndex() - 1 < 0){
            storage.storeAudioIndex(audioList.size() - 1);
        }

        playAudio(storage.loadAudioIndex());
        changeButtonBoxInfo();


    }

    @Override
    public void pauseSong() {
        if (player != null) {
            if (new StorageUtil(getApplicationContext()).getPlaybackStatus()) {
                player.pausemedia();
            } else {
                player.resumeMedia();
            }
        } else {
            new StorageUtil(getApplicationContext()).setPlaybackStatus(true);
            playAudio();
        }
    }

    @Override
    public void nextSong() {

        StorageUtil storage = new StorageUtil(getApplicationContext());


        if (storage.loadAudioIndex() + 1 < audioList.size()) {
            storage.plusIndex();
        } else if (storage.loadAudioIndex() + 1 > audioList.size() - 1) {
            storage.storeAudioIndex(0);
        }




        playAudio(storage.loadAudioIndex());
        changeButtonBoxInfo();


    }

    @Override
    public void changeTimeSong(int i) {
            if (player != null){
                player.resumeMedia(i);
            }else {
                playAudio();
            }

    }

    private String convertTime(long time){
        long second = (time) % 60;
        long minute = (time / (60)) % 60;
        return String.format( Locale.getDefault(), "%02d:%02d", minute, second);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode){
            case KeyEvent.KEYCODE_VOLUME_UP:
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
                seekBarVolume.setProgress(seekBarVolume.getProgress() + 1);
                return true;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
                seekBarVolume.setProgress(seekBarVolume.getProgress() - 1);
                return true;
            default:
                return super.onKeyDown(keyCode, event);
        }
    }

    @Override
    public void onBackPressed() {
        if (slidingLayout != null && (slidingLayout.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED || slidingLayout.getPanelState() == SlidingUpPanelLayout.PanelState.ANCHORED)){
            slidingLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
        } else {
            super.onBackPressed();
        }
    }

    public static void runOnUi(Runnable runnable){
        UIHandler.post(runnable);
    }
}
