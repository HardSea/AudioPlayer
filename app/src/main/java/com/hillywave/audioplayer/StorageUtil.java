package com.hillywave.audioplayer;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;

public class StorageUtil {

    private final String STORAGE = "com.hillywave.audioplayer.STORAGE";
    private SharedPreferences preferences;
    private Context context;

    public StorageUtil(Context context){
        this.context = context;
    }

    public void storeAudio(ArrayList<Audio> arrayList){
        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(arrayList);
        editor.putString("audioArrayList", json);
        editor.apply();
    }


    public void clearList(){
        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove("audioArrayList");
        editor.apply();
    }


    public ArrayList<Audio> loadAudio(){
        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = preferences.getString("audioArrayList", null);
        Type type = new TypeToken<ArrayList<Audio>>(){
        }.getType();
        return gson.fromJson(json, type);
    }

    public void storeAudioIndex(int index){
        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("audioIndex", index);
        editor.apply();
    }

    public int loadAudioIndex(){
        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE);
        return preferences.getInt("audioIndex", -1);
    }

    public void clearCachedAudioPlayList(){
        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.clear();
        editor.apply();
    }

    public void plusIndex(){
        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("audioIndex", preferences.getInt("audioIndex", -1) + 1);
        editor.apply();
    }

    public void minusIndex(){
        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("audioIndex", preferences.getInt("audioIndex", -1) - 1);
        editor.apply();

    }

    public void setPlaybackStatus(boolean playbackstatus){

        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("playbackStatus", playbackstatus);
        editor.apply();

    }

    public boolean getPlaybackStatus(){
        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE);
        return preferences.getBoolean("playbackStatus", false);
    }

    public int getRepeatStatus(){
        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE);
        return preferences.getInt("repeatStatus", 1);
    }

    public void changeRepeatStatus(){
        // 0 - no repeat
        // 1 - repeat list
        // 2 - repeat one

        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE);
        int repeatStatus = preferences.getInt("repeatStatus", 1);

        if (repeatStatus == 2){
            repeatStatus = 0;
        } else {
            repeatStatus++;
        }

        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("repeatStatus", repeatStatus);
        editor.apply();

    }

    public boolean getShuffleStatus(){
        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE);
        return preferences.getBoolean("shuffleStatus", false);
    }

    public void changeShuffleStatus(){
        // 0 - no random
        // 1 - random

        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE);
        boolean shuffleStatus = preferences.getBoolean("shuffleStatus", false);

        shuffleStatus = !shuffleStatus;

        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("shuffleStatus", shuffleStatus);
        editor.apply();

    }



}
