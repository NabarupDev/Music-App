package com.example.music;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class MusicPlayerActivity extends AppCompatActivity implements AudioManager.OnAudioFocusChangeListener {

    private static final String ACTION_VIDEO_STATE_CHANGED = "com.example.music.ACTION_VIDEO_STATE_CHANGED";

    TextView titleTv, currentTimeTv, totalTimeTv;
    SeekBar seekBar, soundBar;
    ImageView pausePlay, nextBtn, previousBtn, musicIcon, shuffleBtn, shuffleBigIcon, repeatBtn, repeatOneBtn;
    ArrayList<AudioModel> songsList;
    AudioModel currentSong;
    MediaPlayer mediaPlayer;
    int x = 0;
    int lastProgress = 0;
    private AudioManager audioManager;
    private boolean isPausedByFocusLoss = false;
    private boolean isShuffleMode = false;
    private boolean isRepeatMode = false;

    private BroadcastReceiver videoPlaybackReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null && intent.getAction().equals(ACTION_VIDEO_STATE_CHANGED)) {
                boolean isVideoPlaying = intent.getBooleanExtra("IS_PLAYING", false);
                if (!isVideoPlaying && mediaPlayer != null && mediaPlayer.isPlaying()) {
                    if (requestAudioFocus()) {
                        mediaPlayer.start();
                        isPausedByFocusLoss = false;
                    }
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_player);

        titleTv = findViewById(R.id.song_title);
        currentTimeTv = findViewById(R.id.current_time);
        totalTimeTv = findViewById(R.id.total_time);
        seekBar = findViewById(R.id.seek_bar);
        pausePlay = findViewById(R.id.pause_play);
        nextBtn = findViewById(R.id.next);
        previousBtn = findViewById(R.id.previous);
        musicIcon = findViewById(R.id.music_icon_big);
        shuffleBtn = findViewById(R.id.shuffle);
        shuffleBigIcon = findViewById(R.id.shuffle_big);
        repeatBtn = findViewById(R.id.repeate);
        repeatOneBtn = findViewById(R.id.repeate_one);
        soundBar = findViewById(R.id.sound_bar); // Initialize soundBar

        titleTv.setSelected(true);

        songsList = (ArrayList<AudioModel>) getIntent().getSerializableExtra("LIST");
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mediaPlayer = new MediaPlayer();

        // Initialize shuffle button and shuffle_big_ico ImageView
        shuffleBtn.setOnClickListener(v -> toggleShuffleMode());
        shuffleBigIcon.setOnClickListener(v -> toggleShuffleMode());

        // Initialize repeat buttons
        repeatBtn.setOnClickListener(v -> toggleRepeatMode());
        repeatOneBtn.setOnClickListener(v -> toggleRepeatMode());

        setResourcesWithMusic();

        MusicPlayerActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && !isDestroyed() && !isFinishing()) {
                    seekBar.setProgress(mediaPlayer.getCurrentPosition());
                    currentTimeTv.setText(convertToMMSS(mediaPlayer.getCurrentPosition() + ""));

                    if (mediaPlayer.isPlaying()) {
                        pausePlay.setImageResource(R.drawable.ic_baseline_pause_circle_outline_24);
                        musicIcon.setRotation(x++);
                    } else {
                        pausePlay.setImageResource(R.drawable.ic_baseline_play_circle_outline_24);
                        musicIcon.setRotation(0);
                    }
                }
                new Handler().postDelayed(this, 100);
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (mediaPlayer != null && fromUser) {
                    mediaPlayer.seekTo(progress);
                    int rotationSpeed = 3; // You can adjust this value for desired speed
                    int rotation = (progress - lastProgress) * rotationSpeed;
                    musicIcon.setRotation(musicIcon.getRotation() + rotation);
                    lastProgress = progress;
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        // Set up the soundBar
        soundBar.setMax(audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
        soundBar.setProgress(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));

        soundBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        nextBtn.setOnClickListener(v -> playNextSong());

        mediaPlayer.setOnCompletionListener(mp -> playNextSong());

        IntentFilter videoStateFilter = new IntentFilter(ACTION_VIDEO_STATE_CHANGED);
        registerReceiver(videoPlaybackReceiver, videoStateFilter);
    }

    void setResourcesWithMusic() {
        currentSong = songsList.get(MyMediaPlayer.currentIndex);

        titleTv.setText(currentSong.getTitle());

        totalTimeTv.setText(convertToMMSS(currentSong.getDuration()));

        pausePlay.setOnClickListener(v -> pausePlay());
        previousBtn.setOnClickListener(v -> playPreviousSong());

        playMusic();
    }

    private void playMusic() {
        if (requestAudioFocus()) {
            mediaPlayer.reset();
            try {
                mediaPlayer.setDataSource(currentSong.getPath());
                mediaPlayer.prepare();
                mediaPlayer.start();
                seekBar.setProgress(0);
                seekBar.setMax(mediaPlayer.getDuration());
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Log.e("MusicPlayerActivity", "Failed to obtain audio focus");
        }
    }

    private boolean requestAudioFocus() {
        int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }

    private void abandonAudioFocus() {
        audioManager.abandonAudioFocus(this);
    }

    private void playNextSong() {
        if (isShuffleMode) {
            MyMediaPlayer.currentIndex = getRandomIndex();
        } else {
            if (MyMediaPlayer.currentIndex < songsList.size() - 1) {
                MyMediaPlayer.currentIndex += 1;
            } else {
                MyMediaPlayer.currentIndex = 0;
            }
        }

        mediaPlayer.reset();
        setResourcesWithMusic();
    }

    private void playPreviousSong() {
        if (MyMediaPlayer.currentIndex > 0) {
            MyMediaPlayer.currentIndex -= 1;
        } else {
            MyMediaPlayer.currentIndex = songsList.size() - 1;
        }

        mediaPlayer.reset();
        setResourcesWithMusic();
    }

    private void pausePlay() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            isPausedByFocusLoss = true;
            rotateIconBackToFirstPosition(true);
        } else {
            if (isPausedByFocusLoss) {
                if (requestAudioFocus()) {
                    mediaPlayer.start();
                    isPausedByFocusLoss = false;
                }
            } else {
                playMusic();
            }
        }
    }

    private void toggleShuffleMode() {
        isShuffleMode = !isShuffleMode;
        if (isShuffleMode) {
            shuffleBtn.setVisibility(View.GONE);
            shuffleBigIcon.setVisibility(View.VISIBLE);
        } else {
            shuffleBtn.setVisibility(View.VISIBLE);
            shuffleBigIcon.setVisibility(View.GONE);
        }
        // Update UI or show a toast message indicating the shuffle mode state
    }

    private void toggleRepeatMode() {
        isRepeatMode = !isRepeatMode;
        // Handle the logic for repeat mode (e.g., update UI, set repeat mode for MediaPlayer)
        // You can use the isRepeatMode variable to determine the current state.
        // For example, if isRepeatMode is true, it means the repeat mode is active.
        if (isRepeatMode) {
            repeatBtn.setVisibility(View.GONE);
            repeatOneBtn.setVisibility(View.VISIBLE);
        } else {
            repeatBtn.setVisibility(View.VISIBLE);
            repeatOneBtn.setVisibility(View.GONE);
        }

        if (isRepeatMode) {
            mediaPlayer.setLooping(true);  // Set MediaPlayer to loop the song
        } else {
            mediaPlayer.setLooping(false);  // Disable looping
        }
    }

    private void rotateIconBackToFirstPosition(boolean isPlaying) {
        final int rotationIncrement = 1;
        final int targetRotation = 0;
        final Handler handler = new Handler();

        final int rotationSpeed = isPlaying ? 1 : 5;

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (musicIcon.getRotation() > targetRotation) {
                    musicIcon.setRotation(musicIcon.getRotation() - rotationIncrement);
                    handler.postDelayed(this, rotationSpeed);
                } else {
                    musicIcon.setRotation(0);
                }
            }
        };

        handler.post(runnable);
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_LOSS:
                mediaPlayer.pause();
                isPausedByFocusLoss = true;
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                mediaPlayer.pause();
                isPausedByFocusLoss = true;
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                mediaPlayer.setVolume(0.2f, 0.2f);
                break;
            case AudioManager.AUDIOFOCUS_GAIN:
                if (isPausedByFocusLoss) {
                    mediaPlayer.start();
                    isPausedByFocusLoss = false;
                }
                mediaPlayer.setVolume(1.0f, 1.0f);
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(videoPlaybackReceiver);
        mediaPlayer.release();
        abandonAudioFocus();
    }

    public static String convertToMMSS(String duration) {
        Long millis = Long.parseLong(duration);
        return String.format("%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(millis) % TimeUnit.HOURS.toMinutes(1),
                TimeUnit.MILLISECONDS.toSeconds(millis) % TimeUnit.MINUTES.toSeconds(1));
    }

    private int getRandomIndex() {
        return (int) (Math.random() * songsList.size());
    }
}
