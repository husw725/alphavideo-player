package com.melot.kkalphavideo.texture;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;

import java.io.IOException;

/**
 * Created by siwen.hu on 2019/6/14.
 */
public class AlphaVideoPlayer extends TextureView implements TextureView.SurfaceTextureListener, MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {

    public static String videoPath;//=Environment.getExternalStorageDirectory().toString() + "/007/" + "test_video_alpha_1.mp4";
    private MediaPlayer mediaPlayer;
    private VideoTextureSurfaceRenderer videoRenderer;
    private int surfaceWidth;
    private int surfaceHeight;
    private Context mContext;
    private boolean pause;
    private boolean looping = false;
    private boolean prepared;
    private boolean textureAvailable;
    private VideoTextureSurfaceRenderer.FitType mFitType;
    private Runnable completionCallback;

    public void setCompletionCallback(Runnable completionCallback) {
        this.completionCallback = completionCallback;
    }

    public AlphaVideoPlayer(Context context) {
        super(context);
        init(context);
    }

    public AlphaVideoPlayer(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public MediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }

    public void closeVolume() {
        if (mediaPlayer != null) {
            mediaPlayer.setVolume(0, 0);
        }
    }

    public void openVolume() {
        if (mediaPlayer != null) {
            mediaPlayer.setVolume(1f, 1f);
        }
    }

    private void initIfNecessary() {
        try {
            if (null == mediaPlayer) {
                mediaPlayer = new MediaPlayer();
                while (videoRenderer.getVideoTexture() == null) {
                    try {
                        Thread.sleep(20);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                mediaPlayer.setOnVideoSizeChangedListener(videoRenderer);
                Surface surface = new Surface(videoRenderer.getVideoTexture());
                mediaPlayer.setSurface(surface);
                surface.release();
                mediaPlayer.setOnPreparedListener(this);
                mediaPlayer.setOnCompletionListener(this);
                mediaPlayer.setOnErrorListener(this);
            }
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }


    private void init(Context context) {
        setOpaque(false);
        setVisibility(View.GONE);
        mContext = context;
        setSurfaceTextureListener(this);
    }

    public AlphaVideoPlayer loop(boolean looping) {
        this.looping = looping;
        return this;
    }

    public void setFitType(VideoTextureSurfaceRenderer.FitType type) {
        if (videoRenderer == null) {
            mFitType = type;
        } else {
            videoRenderer.setFitTyp(type);
        }
    }

    float tX, tY, tZ;

    public void setTranslateM(float x, float y, float z) {
        if (videoRenderer == null) {
            tX = x;
            tY = y;
            tZ = z;
        } else {
            videoRenderer.setTranslateM(x, y, z);
        }
    }

    public void playVideo(String path) {
        if (TextUtils.isEmpty(path)) return;
        prepared = false;
        setVisibility(View.VISIBLE);
        if (!textureAvailable) {
            if (mediaPlayer == null) {
                videoPath = path;
                return;
            }
        }
        initIfNecessary();
        play(path);
    }

    private void play(String path) {
        if (TextUtils.isEmpty(path)) return;
        try {
            mediaPlayer.reset();
            mediaPlayer.setDataSource(path);
            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void _playVideo(SurfaceTexture surfaceTexture) {
        videoRenderer = new VideoTextureSurfaceRenderer(mContext, surfaceTexture, surfaceWidth, surfaceHeight);
        videoRenderer.setFitTyp(mFitType);
        videoRenderer.setTranslateM(tX, tY, tZ);
        initIfNecessary();
        play(videoPath);
        videoPath = null;
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        surfaceWidth = width;
        surfaceHeight = height;
        textureAvailable = true;
        _playVideo(surface);
        if (videoRenderer != null)
            videoRenderer.onSurfaceTextureAvailable(surface, width, height);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        if (videoRenderer != null)
            videoRenderer.onSurfaceTextureSizeChange(width, height);
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        prepared = true;
        try {
            if (mp != null) {
                mp.start();
            }
            //要在start后调才有效
            mp.setLooping(looping);
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    public void onPause() {
        if (mediaPlayer != null && prepared) {
            pause = true;
            mediaPlayer.pause();
        }
        if (videoRenderer != null)
            videoRenderer.onPause();
    }

    public void onResume() {
        if (pause) {
            if (mediaPlayer != null && prepared)
                mediaPlayer.start();
            if (videoRenderer != null)
                videoRenderer.onResume();
        }
    }

    public void onDestroy() {
        releasePlayer();
        if (videoRenderer != null)
            videoRenderer.onDestroy();
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if (completionCallback != null)
            completionCallback.run();
        releasePlayer();
    }

    public void releasePlayer() {
        setVisibility(GONE);
        //观察内存发现，播放下一个视频，上一个视频内存不回收
        //播完一个都主动释放mediaPlayer。这样就是每个都次都要重建
        //media player 系统bug 要先reset再release才能释放
        if (mediaPlayer != null) {
            prepared = false;
            mediaPlayer.reset();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        prepared = false;
        return false;
    }
}