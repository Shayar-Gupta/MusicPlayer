package com.example.musicplayer;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements SongChangeListener {

    private final List<MusicList> musicLists = new ArrayList<>();
    private RecyclerView musicRecyclerView;
    private MediaPlayer mediaPlayer;
    private TextView startTime, endTime;
    private SeekBar playerSeekBar;
    private Timer timer;
    private ImageView playPauseImg;
    private MusicAdapter musicAdapter;
    private boolean isPlaying = false;
    private int currentSongListPosition = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        View decodeView = getWindow().getDecorView();
        int options = View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        decodeView.setSystemUiVisibility(options);

        setContentView(R.layout.activity_main);

        final LinearLayout searchBtn = findViewById(R.id.searchBtn);
        final LinearLayout menuBtn = findViewById(R.id.menuBtn);
        final CardView playPauseCard = findViewById(R.id.playPauseCardView);
        playPauseImg = findViewById(R.id.playPauseImg);
        final ImageView nextBtn = findViewById(R.id.nextBtn);
        final ImageView prevBtn = findViewById(R.id.previousBtn);
        startTime = findViewById(R.id.startTime);
        endTime = findViewById(R.id.endTime);
        playerSeekBar = findViewById(R.id.playerSeekbar);

        musicRecyclerView = findViewById(R.id.musicRecyclerView);
        musicRecyclerView.setHasFixedSize(true);
        musicRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        musicAdapter = new MusicAdapter(musicLists, MainActivity.this);
        musicRecyclerView.setAdapter(musicAdapter);

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            getMusicFiles();
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 11);
            } else {
                getMusicFiles();
            }
        }

        nextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int nextSongListPostion = currentSongListPosition + 1;
                if (nextSongListPostion >= musicLists.size()) nextSongListPostion = 0;

                musicLists.get(currentSongListPosition).setPlaying(false);
                musicLists.get(nextSongListPostion).setPlaying(true);

                musicAdapter.updateList(musicLists);
                musicRecyclerView.scrollToPosition(nextSongListPostion);
                onChanged(nextSongListPostion);
            }
        });

        prevBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int prevSongListPostion = currentSongListPosition - 1;
                if (prevSongListPostion < 0) prevSongListPostion = musicLists.size() - 1;

                musicLists.get(currentSongListPosition).setPlaying(false);
                musicLists.get(prevSongListPostion).setPlaying(true);

                musicAdapter.updateList(musicLists);
                musicRecyclerView.scrollToPosition(prevSongListPostion);
                onChanged(prevSongListPostion);
            }
        });

        playPauseCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentSongListPosition == -1 && !musicLists.isEmpty()) {
                    currentSongListPosition = (int)(Math.random() * musicLists.size());
                    startPlayback(currentSongListPosition);
                } else if (isPlaying) {
                    pausePlayback();
                } else {
                    resumePlayback();
                }
            }
        });

        playerSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    mediaPlayer.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void startPlayback(int position) {
        try {
            mediaPlayer.reset();
            mediaPlayer.setDataSource(MainActivity.this, musicLists.get(position).getMusicFile());
            mediaPlayer.prepareAsync();
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mp.start();
                    isPlaying = true;
                    playPauseImg.setImageResource(R.drawable.icon_pause_24);
                    musicLists.get(currentSongListPosition).setPlaying(false);
                    musicLists.get(position).setPlaying(true);
                    musicAdapter.updateList(musicLists);
                    musicRecyclerView.scrollToPosition(position);
                    onChanged(position);

                }
            });
            // Other mediaPlayer setup...
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(MainActivity.this, "Unable to play track", Toast.LENGTH_SHORT).show();
        }
    }

    private void pausePlayback() {
        if (isPlaying) {
            mediaPlayer.pause();
            isPlaying = false;
            playPauseImg.setImageResource(R.drawable.icon_play_24);
        }
    }

    private void resumePlayback() {
        if (!isPlaying) {
            mediaPlayer.start();
            isPlaying = true;
            playPauseImg.setImageResource(R.drawable.icon_pause_24);
        }
    }

    private void updateSeekBar() {
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        int currentDuration = mediaPlayer.getCurrentPosition();
                        playerSeekBar.setProgress(currentDuration);
                        startTime.setText(formatDuration(currentDuration));
                    }
                });
            }
        }, 0, 1000);
    }

    private void scrollToPosition(int position) {
        LinearLayoutManager layoutManager = (LinearLayoutManager) musicRecyclerView.getLayoutManager();
        if (layoutManager != null) {
            layoutManager.scrollToPositionWithOffset(position, 0);
        }
    }

    private String formatDuration(int durationInMillis) {
        return String.format(Locale.getDefault(), "%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(durationInMillis),
                TimeUnit.MILLISECONDS.toSeconds(durationInMillis) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(durationInMillis)));
    }

    @SuppressLint("Range")
    private void getMusicFiles() {
        ContentResolver contentResolver = getContentResolver();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        Cursor cursor = contentResolver.query(uri, null, MediaStore.Audio.Media.DATA + " LIKE?", new String[]{"%.mp3%"}, null);

        if (cursor == null) {
            Toast.makeText(this, "Something went wrong!!", Toast.LENGTH_SHORT).show();
        } else if (!cursor.moveToFirst()) {
            Toast.makeText(this, "No Music Found", Toast.LENGTH_SHORT).show();
        } else {
            while (cursor.moveToNext()) {
                final String getMusicFileName = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                final String getArtistName = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                long cursorId = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media._ID));
                Uri musicFileUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, cursorId);

                String duration =  cursor.getString(cursor.getColumnIndex(MediaStore.Audio.AudioColumns.DURATION));
                long durationInMillis = Long.parseLong(duration);
                String getDuration = String.format(Locale.getDefault(), "%02d:%02d",
                        TimeUnit.MILLISECONDS.toMinutes(durationInMillis),
                        TimeUnit.MILLISECONDS.toSeconds(durationInMillis) -
                                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(durationInMillis)));


                final MusicList musicList = new MusicList(getMusicFileName, getArtistName, getDuration, false, musicFileUri);
                musicLists.add(musicList);
            }
            musicAdapter.updateList(musicLists);
        }
        cursor.close();
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getMusicFiles();
        } else {
            Toast.makeText(this, "Permission Declined By User", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if (hasFocus) {
            View decodeView = getWindow().getDecorView();
            int options = View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
            decodeView.setSystemUiVisibility(options);
        }
    }

    @Override
    public void onChanged(int position) {
        currentSongListPosition = position;
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            mediaPlayer.reset();
        }

        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    mediaPlayer.setDataSource(MainActivity.this, musicLists.get(position).getMusicFile());
                    mediaPlayer.prepare();
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, "Unable to play track", Toast.LENGTH_SHORT).show();
                }
            }
        }).start();

        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                final int getTotalDuration = mp.getDuration();

                String generateDuration = String.format(Locale.getDefault(), "%02d:%02d",
                        TimeUnit.MILLISECONDS.toMinutes(getTotalDuration),
                        TimeUnit.MILLISECONDS.toSeconds(getTotalDuration),
                        TimeUnit.MILLISECONDS.toSeconds(TimeUnit.MILLISECONDS.toMinutes(getTotalDuration)));

                endTime.setText(generateDuration);
                isPlaying = true;

                mp.start();

                playerSeekBar.setMax(getTotalDuration);
                playPauseImg.setImageResource(R.drawable.icon_pause_24);

                // Update UI to highlight currently playing song
                musicLists.get(position).setPlaying(true); // Highlight currently playing song
                musicAdapter.updateList(musicLists); // Update RecyclerView

                // Automatic scrolling to the currently playing song
                int firstVisibleItemPosition = ((LinearLayoutManager) musicRecyclerView.getLayoutManager()).findFirstVisibleItemPosition();
                int lastVisibleItemPosition = ((LinearLayoutManager) musicRecyclerView.getLayoutManager()).findLastVisibleItemPosition();
                if (position < firstVisibleItemPosition || position > lastVisibleItemPosition) {
                    musicRecyclerView.scrollToPosition(position);
                }
            }
        });

        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        final int getCurrentDuration = mediaPlayer.getCurrentPosition();

                        String generateDuration = String.format(Locale.getDefault(), "%02d:%02d",
                                TimeUnit.MILLISECONDS.toMinutes(getCurrentDuration),
                                TimeUnit.MILLISECONDS.toSeconds(getCurrentDuration),
                                TimeUnit.MILLISECONDS.toSeconds(TimeUnit.MILLISECONDS.toMinutes(getCurrentDuration)));

                        playerSeekBar.setProgress(getCurrentDuration);
                        startTime.setText(generateDuration);
                    }
                });
            }
        }, 1000, 1000);

        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mediaPlayer.reset();
                timer.purge();
                timer.cancel();
                isPlaying = false;
                playPauseImg.setImageResource(R.drawable.icon_play_24);
                playerSeekBar.setProgress(0);
            }
        });
    }

}
