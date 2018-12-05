package com.hillywave.audioplayer;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements BlankFragment.OnFragmentInteractionListener{

    private MediaPlayerService player;
    boolean serviceBound = false;
    private ArrayList<Audio> audioList;
    public static final String Broadcast_PLAY_NEW_AUDIO = "com.hillywave.audioplayer.PlayNewAudio";
    public static final String RECEIVER_INTENT = "RECEIVER_INTENT";
    public static final String RECEIVER_MESSAGE = "RECEIVER_MESSAGE";
    private static final String TAG = "MainActivity";
    private BroadcastReceiver mBroadcastReceiver;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBroadcastReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "onReceive: register broadcast receiver");
                int message = intent.getIntExtra(RECEIVER_MESSAGE, -1);
                setSeekBarProgress(message);
                Log.d(TAG, "onReceive: " + message);
            }
        };

        loadAudio();

        initRecyclerView();

        //Toast.makeText(this, String.valueOf(checkPermissionForReadExtertalStorage()), Toast.LENGTH_SHORT).show();


    }

    @Override
    protected void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(this).registerReceiver((mBroadcastReceiver),
                new IntentFilter(RECEIVER_INTENT)
        );


    }

    @Override
    protected void onStop() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
        super.onStop();

    }

    private void initRecyclerView(){
        if (audioList.size() > 0){
            RecyclerView recyclerView = findViewById(R.id.recyclerview);
            RecyclerView.Adapter adapter = new RecyclerView_Adapter(audioList, this);
            recyclerView.setAdapter(adapter);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));


        }
    }


    public void requestPermissionForReadExtertalStorage() throws Exception {
        try {
            int WRITE_STORAGE_PERMISSION_REQUEST_CODE = 1;
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_STORAGE_PERMISSION_REQUEST_CODE);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }


    public boolean checkPermissionForReadExtertalStorage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int result = this.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
            return result == PackageManager.PERMISSION_GRANTED;
        }
        return false;
    }











    //void playAudio(String media){
    void playAudio(int position){
        StorageUtil storage = new StorageUtil(getApplicationContext());

        if (!serviceBound){
            storage.storeAudio(audioList);
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
        //updateTrackInfo(position);


    }

    private void updateTrackInfo(int position){
       // Audio activeAudio = audioList.get(position);

            StorageUtil storage = new StorageUtil(getApplicationContext());
            int pos = storage.loadAudioIndex();
        Audio activeAudio =  audioList.get(pos);

        TextView textinfo = findViewById(R.id.audio_info);
        textinfo.setText("Album: " + activeAudio.getAlbum()
                + "\nArtist: " + activeAudio.getArtist()
                + "\nData: " + activeAudio.getData()
                + "\nTitle: " + activeAudio.getTitle()
                + "\nDisplay name: " + activeAudio.getDisplay_name()
                + "\nDuration: " + activeAudio.getDuration()
                + "\nYear: " + activeAudio.getYear());

        ImageView album_art = findViewById(R.id.album_art);
        MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();
        metadataRetriever.setDataSource(activeAudio.getData());
        byte[] art;
        try{

            art = metadataRetriever.getEmbeddedPicture();
            Bitmap song_cover = BitmapFactory.decodeByteArray(art, 0, art.length);
            album_art.setImageBitmap(song_cover);

        } catch (Exception e){
            album_art.setBackgroundColor(Color.GRAY);
            e.printStackTrace();
        }


    }

    private void loadAudio(){




        if (!checkPermissionForReadExtertalStorage()){
            try {
                requestPermissionForReadExtertalStorage();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (checkPermissionForReadExtertalStorage())
                    loadAudio();
                }
            } else {


            ContentResolver contentResolver = getContentResolver();

            Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            String selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0";
            String setOrder = MediaStore.Audio.Media.TITLE + " ASC";
            Cursor cursor = contentResolver.query(uri, null, selection, null, setOrder);

            if (cursor != null && cursor.getCount() > 0) {
                audioList = new ArrayList<>();
                while (cursor.moveToNext()) {
                    String data = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
                    String title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                    String album = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
                    String artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                    String display_name = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME));
                    String duration = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));
                    String year = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.YEAR));

                    audioList.add(new Audio(data, title, album, artist, display_name, duration, year));

                }

            }

            assert cursor != null;
            cursor.close();
        }
    }



    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

            MediaPlayerService.LocalBinder binder = (MediaPlayerService.LocalBinder) service;
            player = binder.getService();
            serviceBound = true;

            Toast.makeText(MainActivity.this, "Service bound", Toast.LENGTH_SHORT).show();
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
            unbindService(serviceConnection);
            player.stopSelf();
        }
    }




    private void changeButtonBoxInfo(){
        StorageUtil storage = new StorageUtil(getApplicationContext());
        int audioIndex = storage.loadAudioIndex();
        ArrayList<Audio> audioListfragment = storage.loadAudio();

        BlankFragment articleFrag = (BlankFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_box);

        if (articleFrag != null) {

            articleFrag.changeSongInfo(audioListfragment.get(audioIndex).getArtist(), audioListfragment.get(audioIndex).getTitle());

        } else {

            BlankFragment newFragment = new BlankFragment();
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

    public void setSeekBarProgress(int progress){
        Log.d(TAG, "setSeekBarProgress: setSeekBarProgress" );
        StorageUtil storage = new StorageUtil(getApplicationContext());
        int audioIndex = storage.loadAudioIndex();
        ArrayList<Audio> audioListfragment = storage.loadAudio();

        BlankFragment articleFrag = (BlankFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_box);

        if (articleFrag != null) {
            articleFrag.changeSeekBarProgres(progress);


        } else {

            BlankFragment newFragment = new BlankFragment();
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

        Log.d(TAG, "prevSong: " + player.getCurrentPos());

        StorageUtil storage = new StorageUtil(getApplicationContext());
        storage.minusIndex();
        playAudio(storage.loadAudioIndex());
        changeButtonBoxInfo();


    }

    @Override
    public void pauseSong() {

    }

    @Override
    public void nextSong() {

        StorageUtil storage = new StorageUtil(getApplicationContext());
        storage.plusIndex();
        playAudio(storage.loadAudioIndex());
        changeButtonBoxInfo();

    }




}
