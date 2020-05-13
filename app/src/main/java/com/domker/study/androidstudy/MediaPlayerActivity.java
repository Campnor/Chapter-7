package com.domker.study.androidstudy;

import android.content.pm.ActivityInfo;
import android.graphics.Point;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MediaPlayerActivity extends AppCompatActivity {
    private SurfaceView surfaceView;
    private MediaPlayer player;
    private SurfaceHolder holder;

    private SeekBar seekBar;
    private TextView tv_start;
    private TextView tv_end;

    private int HEIGHT;
    private int WIDTH;

    private ScheduledExecutorService scheduledExecutorService;
    private boolean isSeekBarChanging;//互斥变量，防止进度条和定时器冲突。

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setTitle("胖胖斌的播放器");


        setContentView(R.layout.layout_media_player);
        surfaceView = findViewById(R.id.surfaceView);
        player = new MediaPlayer();

        Point outSize = new Point();
        getWindowManager().getDefaultDisplay().getRealSize(outSize);
        WIDTH = outSize.x;
        HEIGHT = outSize.y;

        seekBar = findViewById(R.id.seekBar);
        tv_start = findViewById(R.id.tv_start);
        tv_end = findViewById(R.id.tv_end);

        try {
            Uri uri = getIntent().getData();
            if(uri!=null) {
                player.setDataSource(this, uri);
            }else {
                player.setDataSource(getResources().openRawResourceFd(R.raw.bytedance));
            }
            holder = surfaceView.getHolder();
            holder.addCallback(new PlayerCallBack());
            player.prepare();
            player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    player.setLooping(true);
                }
            });
            player.setOnVideoSizeChangedListener(new MediaPlayer.OnVideoSizeChangedListener() {
                @Override
                public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
                    changeVideoSize(mp);
                }
            });
            player.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
                @Override
                public void onBufferingUpdate(MediaPlayer mp, int percent) {
                    System.out.println(percent);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        surfaceView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!player.isPlaying()) {
                    player.start();
                    int duration = player.getDuration();//获取音乐总时间
                    seekBar.setMax(duration);//将音乐总时间设置为Seekbar的最大值
                    scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
                    scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
                        @Override
                        public void run() {
                            if(!isSeekBarChanging){
                                seekBar.setProgress(player.getCurrentPosition());
                            }
                        }
                    }, 0, 1, TimeUnit.SECONDS);
                } else {
                    player.pause();
                }
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int duration2 = player.getDuration() / 1000;//获取音乐总时长
                int position = player.getCurrentPosition();//获取当前播放的位置
                tv_start.setText(calculateTime(position / 1000));//开始时间
                tv_end.setText(calculateTime(duration2));//总时长
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isSeekBarChanging = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                isSeekBarChanging = false;
                player.seekTo(seekBar.getProgress());//在当前位置播放
                tv_start.setText(calculateTime(player.getCurrentPosition() / 1000));
            }
        });


    }

    //计算播放时间
    public String calculateTime(int time){
        int minute;
        int second;
        if(time > 60){
            minute = time / 60;
            second = time % 60;
            //分钟再0~9
            if(minute >= 0 && minute < 10){
                //判断秒
                if(second >= 0 && second < 10){
                    return "0"+minute+":"+"0"+second;
                }else {
                    return "0"+minute+":"+second;
                }
            }else {
                //分钟大于10再判断秒
                if(second >= 0 && second < 10){
                    return minute+":"+"0"+second;
                }else {
                    return minute+":"+second;
                }
            }
        }else if(time < 60){
            second = time;
            if(second >= 0 && second < 10){
                return "00:"+"0"+second;
            }else {
                return "00:"+ second;
            }
        }
        return null;
    }


    public void changeVideoSize(MediaPlayer mediaPlayer) {
        if (getResources().getConfiguration().orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
            //竖屏
            surfaceView.setLayoutParams(new LinearLayout.LayoutParams(WIDTH, WIDTH*WIDTH/HEIGHT));
        } else {
            //横屏
            surfaceView.setLayoutParams(new LinearLayout.LayoutParams(WIDTH, HEIGHT-130));
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (player != null) {
            player.stop();
            player.release();
        }
    }

    private class PlayerCallBack implements SurfaceHolder.Callback {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            player.setDisplay(holder);
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {

        }
    }
}
