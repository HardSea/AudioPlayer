package com.hillywave.audioplayer;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.session.MediaSessionManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.app.NotificationCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.RemoteViews;

import java.io.IOException;
import java.util.ArrayList;

import static android.content.ContentValues.TAG;

import com.hillywave.audioplayer.data.model.Audio;

public class MediaPlayerService extends Service implements MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener, MediaPlayer.OnSeekCompleteListener, MediaPlayer.OnErrorListener, MediaPlayer.OnInfoListener, MediaPlayer.OnBufferingUpdateListener, AudioManager.OnAudioFocusChangeListener {

    private final IBinder iBinder = new LocalBinder();

    private MediaPlayer mediaPlayer;
    private AudioManager audioManager;
    private int resumePosition;
    private PhoneStateListener phoneStateListener;
    private TelephonyManager telephonyManager;
    private Handler mHandler = new Handler();
    private boolean playstatus;

    private ArrayList<Audio> audioList;
    private int audioIndex = -1;
    private Audio activeAudio;

    public static final String ACTION_PLAY = "com.hillywave.audioplayer.ACTION_PLAY";
    public static final String ACTION_PAUSE = "com.hillywave.audioplayer.ACTION_PAUSE";
    public static final String ACTION_PREVIOUS = "com.hillywave.audioplayer.ACTION_PREVIOUS";
    public static final String ACTION_CLOSE = "com.hillywave.audioplayer.ACTION_CLOSE";
    public static final String ACTION_NEXT = "com.hillywave.audioplayer.ACTION_NEXT";
    private MediaSessionManager mediaSessionManager;
    private MediaSessionCompat mediaSession;
    private MediaControllerCompat.TransportControls transportControls;


    @Override
    public void onCreate() {
        super.onCreate();

        callStateListener();
        registerBecomingNoisyReceiver();
        register_PlayNewAudio();
    }

    private void initMediaPlayer() {
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnBufferingUpdateListener(this);
        mediaPlayer.setOnSeekCompleteListener(this);
        mediaPlayer.setOnInfoListener(this);
        mediaPlayer.setScreenOnWhilePlaying(true);
        mediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);

        mediaPlayer.reset();

        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        try {
            mediaPlayer.setDataSource(activeAudio.getData());
        } catch (IOException e) {
            e.printStackTrace();
            stopSelf();
        }
        mediaPlayer.prepareAsync();
    }

    void playMedia() {
        if (!mediaPlayer.isPlaying()) {
            playstatus = true;
            new StorageUtil(getApplicationContext()).setPlaybackStatus(playstatus);
            mediaPlayer.start();
            buildNotification(PlaybackStatus.PLAYING);
        }
    }

    void stopMedia() {
        if (mediaPlayer == null) return;
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            playstatus = false;
            new StorageUtil(getApplicationContext()).setPlaybackStatus(playstatus);
            buildNotification(PlaybackStatus.PAUSED);
        }
    }

    void pauseMedia() {
        Log.d(TAG, "pausemedia: PAUSE MEDIA");
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                resumePosition = mediaPlayer.getCurrentPosition();
                playstatus = false;
                new StorageUtil(getApplicationContext()).setPlaybackStatus(playstatus);
                buildNotification(PlaybackStatus.PAUSED);
            }
        }
    }

    void resumeMedia() {
        Log.d(TAG, "resumeMedia: RESUME MEDIA");
        try {
            if (mediaPlayer != null) {
                if (!mediaPlayer.isPlaying()) {
                    mediaPlayer.seekTo(resumePosition);
                    mediaPlayer.start();
                    playstatus = true;
                    new StorageUtil(getApplicationContext()).setPlaybackStatus(playstatus);
                    buildNotification(PlaybackStatus.PLAYING);
                }
            }
        } catch (Exception e) {
            initMediaPlayer();
            playMedia();
        }
    }

    void resumeMedia(int t) {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            playstatus = false;
            new StorageUtil(getApplicationContext()).setPlaybackStatus(playstatus);
            mediaPlayer.seekTo(t);
            mediaPlayer.start();
            playstatus = true;
            new StorageUtil(getApplicationContext()).setPlaybackStatus(playstatus);
            buildNotification(PlaybackStatus.PLAYING);
        }
    }

    private BroadcastReceiver becomingNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            pauseMedia();
            buildNotification(PlaybackStatus.PAUSED);
        }
    };

    private void registerBecomingNoisyReceiver() {
        IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(becomingNoisyReceiver, intentFilter);
    }

    private void callStateListener() {
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        phoneStateListener = new PhoneStateListener() {

            @Override
            public void onCallStateChanged(int state, String incomingNumber) {

                switch (state) {
                    case TelephonyManager.CALL_STATE_RINGING:
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                        if (mediaPlayer != null) {
                            pauseMedia();
                        }
                        break;
                    //     RESUME AFTER CALLING
                    // case TelephonyManager.CALL_STATE_IDLE:
                    //     if (mediaPlayer != null){
                    //         ongoingCall = false;
                    //         resumeMedia();
                    //     }
                    // break;
                }

            }
        };
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
    }

    private BroadcastReceiver updatePlaylist = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            audioList.clear();
            audioList = new StorageUtil(context).loadAudio();

        }
    };

    private BroadcastReceiver playNewAudio = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            audioIndex = new StorageUtil(getApplicationContext()).loadAudioIndex();
            Log.d("Audio index", "MediaPlayerService broadcast receiver: " + audioIndex);
            Log.d("Audio index", "MediaPlayerService broadcast receiver: " + audioList.size());

            if (audioIndex != -1 && audioIndex < audioList.size()) {
                activeAudio = audioList.get(audioIndex);
            } else {
                stopSelf();
            }

            stopMedia();
            mediaPlayer.reset();
            initMediaPlayer();
            updateMetaData();
            playstatus = true;
            new StorageUtil(getApplicationContext()).setPlaybackStatus(playstatus);
            buildNotification(PlaybackStatus.PLAYING);
        }
    };

    private void register_PlayNewAudio() {
        IntentFilter filter = new IntentFilter(MainActivity.Broadcast_PLAY_NEW_AUDIO);
        registerReceiver(playNewAudio, filter);
        IntentFilter filter2 = new IntentFilter(MainActivity.Broadcast_UPDATE_PLAYLIST);
        registerReceiver(updatePlaylist, filter2);
    }

    private void initMediaSession() {
        if (mediaSessionManager != null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mediaSessionManager = (MediaSessionManager) getApplicationContext().getSystemService(Context.MEDIA_SESSION_SERVICE);
        }

        mediaSession = new MediaSessionCompat(getApplicationContext(), "AudioPlayer");

        transportControls = mediaSession.getController().getTransportControls();

        mediaSession.setActive(true);
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        updateMetaData();

        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPlay() {
                super.onPlay();
                resumeMedia();
                buildNotification(PlaybackStatus.PLAYING);
            }

            @Override
            public void onPause() {
                super.onPause();
                pauseMedia();
                buildNotification(PlaybackStatus.PAUSED);
            }

            @Override
            public void onSkipToNext() {
                super.onSkipToNext();
                skipToNext();
                updateMetaData();
                buildNotification(PlaybackStatus.PLAYING);
            }

            @Override
            public void onSkipToPrevious() {
                super.onSkipToPrevious();
                skipToPrevious();
                updateMetaData();
                buildNotification(PlaybackStatus.PLAYING);
            }

            @Override
            public void onStop() {
                super.onStop();
                removeNotification();
                stopSelf();
            }
        });

    }

    private void updateMetaData() {
        mediaSession.setMetadata(new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, activeAudio.getArtist())
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, activeAudio.getAlbum())
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, activeAudio.getTitle())
                .build());
    }

    private void skipToNext() {
        if (new StorageUtil(getApplicationContext()).getShuffleStatus()) {
            audioIndex = (int) (Math.random() * audioList.size());
            activeAudio = audioList.get(audioIndex);
        } else {
            if (audioIndex == audioList.size() - 1) {
                audioIndex = 0;
                activeAudio = audioList.get(audioIndex);
            } else {
                activeAudio = audioList.get(++audioIndex);
            }
        }
        new StorageUtil(getApplicationContext()).storeAudioIndex(audioIndex);

        stopMedia();

        mediaPlayer.reset();
        initMediaPlayer();
    }

    private void skipToNextNoRepeatList() {
        if (audioIndex == audioList.size() - 1) {
            stopMedia();
            mediaPlayer.reset();
        } else {
            activeAudio = audioList.get(++audioIndex);
            new StorageUtil(getApplicationContext()).storeAudioIndex(audioIndex);

            stopMedia();

            mediaPlayer.reset();
            initMediaPlayer();
        }
    }

    private void playRepeat() {
        stopMedia();
        mediaPlayer.reset();
        initMediaPlayer();
    }

    private void skipToPrevious() {
        if (audioIndex == 0) {
            audioIndex = audioList.size() - 1;
            activeAudio = audioList.get(audioIndex);
        } else {
            activeAudio = audioList.get(--audioIndex);
        }

        new StorageUtil(getApplicationContext()).storeAudioIndex(audioIndex);

        stopMedia();

        mediaPlayer.reset();
        initMediaPlayer();
    }

    private void buildNotification(PlaybackStatus playbackStatus) {

        int notificationAction = android.R.drawable.ic_media_pause;
        PendingIntent play_pauseAction = null;

        if (playbackStatus == PlaybackStatus.PLAYING) {
            play_pauseAction = playbackAction(1);
        } else if (playbackStatus == PlaybackStatus.PAUSED) {
            notificationAction = android.R.drawable.ic_media_play;
            play_pauseAction = playbackAction(0);
        }

        RemoteViews notificationLayout = new RemoteViews(getPackageName(), R.layout.notification_small);

        notificationLayout.setTextViewText(R.id.notification_artist_name, activeAudio.getArtist());
        notificationLayout.setTextViewText(R.id.notification_song_name, activeAudio.getTitle());
        notificationLayout.setImageViewResource(R.id.btnPause, notificationAction);
        notificationLayout.setOnClickPendingIntent(R.id.btnPrev, playbackAction(3));
        notificationLayout.setOnClickPendingIntent(R.id.btnPause, play_pauseAction);
        notificationLayout.setOnClickPendingIntent(R.id.btnNext, playbackAction(2));
        notificationLayout.setOnClickPendingIntent(R.id.btnClose, playbackAction(4));

        PendingIntent pi = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, "1")
                .setCustomContentView(notificationLayout)
                .setShowWhen(false)
                .setContentIntent(pi)
                .setSmallIcon(android.R.drawable.stat_sys_headset)
                .setOngoing(true)
                .setStyle(new android.support.v4.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken()))
                .setColor(getResources().getColor(R.color.colorPrimary));

        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).notify(MainActivity.NOTIFICATION_ID, notificationBuilder.build());
    }

    private void removeNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert notificationManager != null;
        notificationManager.cancel(MainActivity.NOTIFICATION_ID);
    }

    public PendingIntent playbackAction(int actionNumber) {
        Intent playbackAction = new Intent(this, MediaPlayerService.class);
        switch (actionNumber) {
            case 0:
                // Play
                playbackAction.setAction(ACTION_PLAY);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);

            case 1:
                // Pause
                playbackAction.setAction(ACTION_PAUSE);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);

            case 2:
                // Next track
                playbackAction.setAction(ACTION_NEXT);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);

            case 3:
                // Previous track
                playbackAction.setAction(ACTION_PREVIOUS);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            case 4:
                playbackAction.setAction(ACTION_CLOSE);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            default:
                break;
        }
        return null;
    }

    private void handleIncomingActions(Intent playbackAction) {
        if (playbackAction == null || playbackAction.getAction() == null) return;

        String actionString = playbackAction.getAction();
        if (actionString.equalsIgnoreCase(ACTION_PLAY)) {
            transportControls.play();
        } else if (actionString.equalsIgnoreCase(ACTION_PAUSE)) {
            transportControls.pause();
        } else if (actionString.equalsIgnoreCase(ACTION_NEXT)) {
            transportControls.skipToNext();
        } else if (actionString.equalsIgnoreCase(ACTION_PREVIOUS)) {
            transportControls.skipToPrevious();
        } else if (actionString.equalsIgnoreCase(ACTION_CLOSE)) {
            transportControls.pause();
            transportControls.stop();
            removeNotification();
        }
    }


    private boolean requestAudioFocus() {
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        assert audioManager != null;
        int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }

    private boolean removeAudioFocus() {
        return AudioManager.AUDIOFOCUS_REQUEST_GRANTED == audioManager.abandonAudioFocus(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            StorageUtil storage = new StorageUtil(getApplicationContext());
            audioList = storage.loadAudio();
            audioIndex = storage.loadAudioIndex();
            if (audioIndex != -1 && audioIndex < audioList.size()) {
                activeAudio = audioList.get(audioIndex);
            } else {
                stopSelf();
            }

        } catch (NullPointerException e) {
            Log.d("Media Player error", "Error get extra string media");
            stopSelf();
        }

        if (!requestAudioFocus()) {
            stopSelf();
        }

        if (mediaSession == null) {
            try {
                initMediaSession();
                initMediaPlayer();
            } catch (Exception e) {
                e.printStackTrace();
                stopSelf();
            }

            buildNotification(PlaybackStatus.PLAYING);
        }

        handleIncomingActions(intent);

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    Intent intent = new Intent(MainActivity.RECEIVER_INTENT);
                    if (mediaPlayer != null) {
                        int mCurrentPosition = mediaPlayer.getCurrentPosition() / 1000;
                        int mAllSize = mediaPlayer.getDuration() / 1000;

                        intent.putExtra(MainActivity.CURRENT_POSITION, mCurrentPosition);
                        intent.putExtra(MainActivity.ALL_DURATION, mAllSize);
                        intent.putExtra(MainActivity.ACTION, MainActivity.CHANGE_SEEKBAR);
                        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
                    }

                    mHandler.postDelayed(this, 1000);
                } catch (Exception e) {
                    e.printStackTrace();
                    mHandler.removeCallbacks(this);
                }
            }
        });

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            stopMedia();
            mediaPlayer.release();
        }

        removeAudioFocus();

        if (phoneStateListener != null) {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
        }

        removeNotification();

        unregisterReceiver(becomingNoisyReceiver);
        unregisterReceiver(playNewAudio);
        unregisterReceiver(updatePlaylist);

        new StorageUtil(getApplicationContext()).clearCachedAudioPlayList();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return iBinder;
    }

    @Override
    public void onAudioFocusChange(int focusState) {
        switch (focusState) {
            case AudioManager.AUDIOFOCUS_GAIN:
                Log.d(TAG, "onAudioFocusChange: AUDIOFOCUS_GAIN");

                if (mediaPlayer.isPlaying()) mediaPlayer.setVolume(1f, 1f);
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                Log.d(TAG, "onAudioFocusChange: AUDIOFOCUS_LOSS_TRANSIENT");

                if (mediaPlayer.isPlaying()) mediaPlayer.setVolume(0.1f, 0.1f);
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                Log.d(TAG, "onAudioFocusChange: AUDIOFOCUS_LOSS");

                if (mediaPlayer.isPlaying()) mediaPlayer.setVolume(0.1f, 0.1f);
                break;
            case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT:
                Log.d(TAG, "onAudioFocusChange: AUDIOFOCUS_GAIN_TRANSIENT");

                if (mediaPlayer.isPlaying()) mediaPlayer.setVolume(0.1f, 0.1f);
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                Log.d(TAG, "onAudioFocusChange: AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK");

                if (mediaPlayer.isPlaying()) mediaPlayer.setVolume(0.1f, 0.1f);
                break;
        }
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mediaPlayer, int i) {

    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        // 0 - no repeat
        // 1 - repeat list
        // 2 - repeat one

        if (new StorageUtil(getApplicationContext()).getRepeatStatus() == 0) {
            skipToNextNoRepeatList();
            updateMetaData();
            buildNotification(PlaybackStatus.PLAYING);
        } else if (new StorageUtil(getApplicationContext()).getRepeatStatus() == 1) {
            skipToNext();
            updateMetaData();
            buildNotification(PlaybackStatus.PLAYING);
        } else {
            playRepeat();
            updateMetaData();
            buildNotification(PlaybackStatus.PLAYING);
        }

        Intent intent = new Intent(MainActivity.RECEIVER_INTENT);
        if (mediaPlayer != null) {
            intent.putExtra(MainActivity.ACTION, MainActivity.NEW_AUDIO);
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.e(getPackageName(), String.format("Error(%s%s)", what, extra));

        if (what == MediaPlayer.MEDIA_ERROR_SERVER_DIED)
            mp.reset();
        else if (what == MediaPlayer.MEDIA_ERROR_UNKNOWN)
            mp.reset();
        playMedia();
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnPreparedListener(this);

        return true;
    }

    @Override
    public boolean onInfo(MediaPlayer mediaPlayer, int i, int i1) {
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        playMedia();
    }

    @Override
    public void onSeekComplete(MediaPlayer mediaPlayer) {

    }

    class LocalBinder extends Binder {
        MediaPlayerService getService() {
            return MediaPlayerService.this;
        }
    }
}
