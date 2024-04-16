package com.example.aplayer;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ListView listView;
    private TextView playingSongTextView;
    private SeekBar seekBar;
    private MediaPlayer mediaPlayer;
    private Handler handler = new Handler();
    private Button playButton, pauseButton, showButton;
    private String[] musicFiles;
    private String musicDirPath = Environment.getExternalStorageDirectory().getPath() + "/Music/";
    private Button prevButton, nextButton, volumeUpButton, volumeDownButton;
    private int currentVolume;
    private AudioManager audioManager;
    private int currentPosition = 0; // 用于保存和恢复播放位置

    private int currentSongIndex = 0; // 跟踪当前播放歌曲的索引

    private List<Uri> songUris = new ArrayList<>();

    private List<String> musicFilesList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listView = findViewById(R.id.listView);
        playingSongTextView = findViewById(R.id.playingSong);
        seekBar = findViewById(R.id.seekBar);
        playButton = findViewById(R.id.playButton);
        pauseButton = findViewById(R.id.pauseButton);
        showButton = findViewById(R.id.showButton);

        loadMusicFiles();

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Uri contentUri = songUris.get(position);  // 获取对应位置的歌曲URI
                playSong(contentUri);  // 使用音乐文件的Uri播放歌曲
                currentSongIndex = position;  // 更新当前播放索引
                playingSongTextView.setText(musicFilesList.get(position));  // 显示当前播放的歌曲信息
            }
        });



        // 设置SeekBar的事件监听
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            boolean wasPlayingBeforeSeek = false;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (mediaPlayer != null && fromUser) {
                    // 改变进度条时跳转到特定的位置
                    mediaPlayer.seekTo(progress * 1000);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    // 如果音乐正在播放，记住这一状态并暂停播放
                    wasPlayingBeforeSeek = true;
                    mediaPlayer.pause();
                }
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // 当用户放开进度条时触发
                if (mediaPlayer != null && wasPlayingBeforeSeek) {
                    // 如果音乐在拖动前是播放状态，则继续播放
                    mediaPlayer.start();
                }
                wasPlayingBeforeSeek = false; // 重置标记状态
            }
        });


        // 设置播放按钮的事件监听
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
                    mediaPlayer.start();
                    updateSeekBar();
                }
            }
        });

        // 设置暂停按钮的事件监听
        pauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                    currentPosition = mediaPlayer.getCurrentPosition(); // 保存当前播放位置
                }
            }
        });

        // 设置SHOW按钮的事件监听
        showButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "55210308-张佳琦", Toast.LENGTH_LONG).show();
            }
        });

        // 获取系统的AudioManager服务
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        // 设置当前音量和系统音量的最大值
        currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

        // 新按钮的初始化
        prevButton = findViewById(R.id.prevButton);
        nextButton = findViewById(R.id.nextButton);
        volumeUpButton = findViewById(R.id.volumeUpButton);
        volumeDownButton = findViewById(R.id.volumeDownButton);

        // 上一首歌曲按钮的事件监听
        prevButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                prevSong();
            }
        });

        // 下一首歌曲按钮的事件监听
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nextSong();
            }
        });

        // 音量增加按钮的事件监听
        volumeUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 音量增加
                adjustVolume(true);
            }
        });

        // 音量减小按钮的事件监听
        volumeDownButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 音量减小
                adjustVolume(false);
            }
        });
    }

    private void loadMusicFiles() {
        // 需要查询的媒体信息列
        String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.SIZE
        };

        // 选择条件，只加载音乐文件
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";

        // 清除之前的列表以避免重复条目
        musicFilesList.clear();
        songUris.clear();

        // 查询外部媒体内容URI
        try (Cursor cursor = getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                MediaStore.Audio.Media.TITLE + " ASC")) {  // 根据标题排序

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
                    int titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
                    int albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM);
                    int artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
                    int dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
                    int durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
                    int sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE);

                    // 构建当前文件的内容URI
                    Uri contentUri = ContentUris.withAppendedId(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            cursor.getLong(idColumn));

                    // 将URI和标题添加到列表中
                    songUris.add(contentUri);
                    String fileInfo = "标题: " + cursor.getString(titleColumn) +
                            ", 专辑: " + cursor.getString(albumColumn) +
                            ", 艺术家: " + cursor.getString(artistColumn) +
                            ", 持续时间: " + cursor.getLong(durationColumn)/1000 + " 秒";
                    musicFilesList.add(fileInfo);

                } while (cursor.moveToNext());
            }
        } catch (SecurityException e) {
            Toast.makeText(this, "访问媒体文件被拒绝", Toast.LENGTH_SHORT).show();
        }

        // 为ListView设置适配器
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                musicFilesList
        );
        listView.setAdapter(adapter);
    }





    private void playSong(Uri songUri) {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }
        mediaPlayer = MediaPlayer.create(this, songUri);
        if (mediaPlayer == null) {
            Toast.makeText(this, "播放此歌曲出错", Toast.LENGTH_SHORT).show();
            return;
        }
        mediaPlayer.start();
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                nextSong();  // 歌曲播放完毕自动播放下一首
            }
        });

        // 更新正在播放歌曲的TextView
        playingSongTextView.setText(musicFilesList.get(currentSongIndex));

        // 更新SeekBar的进度和最大值
        seekBar.setMax(mediaPlayer.getDuration() / 1000);
        updateSeekBar();
    }



    private void prevSong() {
        if (!songUris.isEmpty()) {
            currentSongIndex = (currentSongIndex - 1 + songUris.size()) % songUris.size();
            playSong(songUris.get(currentSongIndex));
            playingSongTextView.setText(musicFilesList.get(currentSongIndex));  // 更新正在播放歌曲的信息
        } else {
            Toast.makeText(this, "音乐列表为空", Toast.LENGTH_SHORT).show();
        }
    }

    private void nextSong() {
        if (!songUris.isEmpty()) {
            currentSongIndex = (currentSongIndex + 1) % songUris.size();
            playSong(songUris.get(currentSongIndex));
            playingSongTextView.setText(musicFilesList.get(currentSongIndex));  // 更新正在播放歌曲的信息
        } else {
            Toast.makeText(this, "音乐列表为空", Toast.LENGTH_SHORT).show();
        }
    }


    // 调整音量的方法
    private void adjustVolume(boolean increase) {
        currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int change = increase ? 1 : -1;
        currentVolume += change;

        // 确保音量值在合理范围内
        currentVolume = Math.max(0, Math.min(currentVolume, maxVolume));

        // 设置音量
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, AudioManager.FLAG_SHOW_UI);
    }

    private void updateSeekBar() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    seekBar.setProgress(mediaPlayer.getCurrentPosition() / 1000);
                    handler.postDelayed(this, 1000);
                }
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mediaPlayer != null) {
            currentPosition = mediaPlayer.getCurrentPosition(); // 保存当前播放位置
            mediaPlayer.pause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(currentPosition);
            mediaPlayer.start();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}
