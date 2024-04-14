package com.example.aplayer;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
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

public class MainActivity extends AppCompatActivity {

    private ListView listView;
    private TextView playingSongTextView;
    private SeekBar seekBar;
    private MediaPlayer mediaPlayer;
    private Handler handler = new Handler();
    private Button playButton, pauseButton, showButton;
    private String[] musicFiles;
    private String musicDirPath;
    private Button prevButton, nextButton, volumeUpButton, volumeDownButton;
    private int currentVolume;
    private AudioManager audioManager;
    private int currentPosition = 0; // 用于保存和恢复播放位置

    private int currentSongIndex = 0; // 跟踪当前播放歌曲的索引

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

        // 设置ListView点击事件
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                playSong(musicDirPath + "/" + musicFiles[position]);
            }
        });

        // 设置SeekBar的事件监听
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            boolean wasPlayingBeforeSeek = false;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (mediaPlayer != null && fromUser) {
                    // 用户改变进度条时，跳转到特定的位置
                    mediaPlayer.seekTo(progress * 1000);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // 当用户开始拖动进度条时触发
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
                    // 如果音乐在用户拖动前是播放状态，则继续播放
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
                // TODO: 实现上一首歌曲的逻辑
                prevSong();
            }
        });

        // 下一首歌曲按钮的事件监听
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: 实现下一首歌曲的逻辑
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
        // 以下代码应该放在动态权限请求之后
        musicDirPath = Environment.getExternalStorageDirectory().getPath() + "/Music/";
        File musicDir = new File(musicDirPath);
        musicFiles = musicDir.list(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".mp3") || name.endsWith(".wav");
            }
        });

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, musicFiles);
        listView.setAdapter(adapter);
    }

    private void playSong(String path) {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }
        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(path);
            mediaPlayer.prepare();
            mediaPlayer.start();

            playingSongTextView.setText(new File(path).getName());
            seekBar.setMax(mediaPlayer.getDuration() / 1000);

            updateSeekBar(); // 开始更新SeekBar
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateSeekBar() {
        if (mediaPlayer != null) {
            int mCurrentPosition = mediaPlayer.getCurrentPosition() / 1000;
            seekBar.setProgress(mCurrentPosition);
            handler.postDelayed(updateSeekBar, 1000);
        }
    }

    private Runnable updateSeekBar = new Runnable() {
        @Override
        public void run() {
            updateSeekBar();
        }
    };

    // 播放上一首歌曲的方法
    private void prevSong() {
        if (musicFiles.length > 0) {
            // 计算上一首歌曲的索引
            currentSongIndex = (currentSongIndex - 1 + musicFiles.length) % musicFiles.length;
            // 播放选中的歌曲
            playSong(musicDirPath + "/" + musicFiles[currentSongIndex]);
        } else {
            Toast.makeText(MainActivity.this, "音乐列表为空", Toast.LENGTH_SHORT).show();
        }
    }

    // 播放下一首歌曲的方法
    private void nextSong() {
        if (musicFiles.length > 0) {
            // 计算下一首歌曲的索引
            currentSongIndex = (currentSongIndex + 1) % musicFiles.length;
            // 播放选中的歌曲
            playSong(musicDirPath + "/" + musicFiles[currentSongIndex]);
        } else {
            Toast.makeText(MainActivity.this, "音乐列表为空", Toast.LENGTH_SHORT).show();
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
        handler.removeCallbacks(updateSeekBar); // 防止内存泄露
    }
}
