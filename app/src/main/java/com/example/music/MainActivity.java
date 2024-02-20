package com.example.music;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.io.File;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    TextView noMusicTextView;
    ArrayList<AudioModel> songsList = new ArrayList<>();

    private MusicService musicService;
    private boolean isBound = false;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.LocalBinder binder = (MusicService.LocalBinder) service;
            musicService = binder.getService();
            isBound = true;

            // Start playing the song if the service is bound
            if (songsList.size() > 0) {
                musicService.play(songsList.get(0).getPath());
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recycler_view);
        noMusicTextView = findViewById(R.id.no_songs_text);

        if (checkPermission() == false) {
            requestPermission();
            return;
        }

        String[] projection = {
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.DURATION
        };

        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";

        Cursor cursor = getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                null
        );

        while (cursor.moveToNext()) {
            AudioModel songData = new AudioModel(
                    cursor.getString(1),
                    cursor.getString(0),
                    cursor.getString(2)
            );
            if (new File(songData.getPath()).exists())
                songsList.add(songData);
        }

        if (songsList.size() == 0) {
            noMusicTextView.setVisibility(View.VISIBLE);
        } else {
            // RecyclerView
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            recyclerView.setAdapter(new MusicListAdapter(songsList, getApplicationContext()));
        }

        // Bind to MusicService
        Intent intent = new Intent(this, MusicService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unbind from MusicService
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }
    }

    boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(
                MainActivity.this,
                Manifest.permission.READ_EXTERNAL_STORAGE
        );
        return result == PackageManager.PERMISSION_GRANTED;
    }

    void requestPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                MainActivity.this,
                Manifest.permission.READ_EXTERNAL_STORAGE
        )) {
            Toast.makeText(
                    MainActivity.this,
                    "READ PERMISSION IS REQUIRED, PLEASE ALLOW FROM SETTINGS",
                    Toast.LENGTH_SHORT
            ).show();
        } else
            ActivityCompat.requestPermissions(
                    MainActivity.this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    123
            );
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isBound && songsList.size() > 0) {
            // Resume playing the song if the service is bound
            musicService.resume();
        }
    }

    // Example method to play a song
    public void playSong(View view) {
        if (isBound && songsList.size() > 0) {
            musicService.play(songsList.get(0).getPath());
        }
    }

    // Example method to pause the playback
    public void pausePlayback(View view) {
        if (isBound) {
            musicService.pause();
        }
    }

    // Example method to resume the playback
    public void resumePlayback(View view) {
        if (isBound) {
            musicService.resume();
        }
    }

    // Example method to stop the playback
    public void stopPlayback(View view) {
        if (isBound) {
            musicService.stop();
        }
    }
}
