package com.example.emotionlink.AudioDemo;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.emotionlink.R;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import com.example.emotionlink.AudioDemo.Client.WebSocketAuthGenerator;
import com.example.emotionlink.AudioDemo.Client.WebSocketUploader;

public class AudioActivity extends AppCompatActivity implements AudioUrlCallback{
    private static final int REQUEST_PERMISSION_CODE = 1000;
    private static final int SAMPLE_RATE = 16000;

    private AudioRecord audioRecord;
    private boolean isRecording = false;
    private WebSocketUploader wsClient;
    private LinearLayout voiceContainer;
    private String latestAudioUrl = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio);
        Button recordButton = findViewById(R.id.recordButton);
        voiceContainer = findViewById(R.id.voiceContainer);

        if (!checkPermissions()) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            }, REQUEST_PERMISSION_CODE);
        }

        recordButton.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    startStreaming();
                    recordButton.setText("松开发送");
                    return true;
                case MotionEvent.ACTION_UP:
                    stopStreaming();
                    recordButton.setText("按住说话");
                    createVoiceButton();
                    return true;
            }
            return false;
        });
    }
    @Override
    public void onAudioUrlReceived(String audioUrl) {
        latestAudioUrl = audioUrl;
    }

    private void startStreaming() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "未获得录音或存储权限", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            String xAppId = "d0a488df365749648010ec85133e6273";
            String xAppKey = "40c27006325c480a804c4162c9234b60";
            String region = "SH";
            String url = "wss://openapi.teleagi.cn:443/aipaas/voice/v1/asr/fy";
            String originName = "teleai-cloud-auth-v1";
            int expiration = 1800;

            String authorization = WebSocketAuthGenerator.generateAuthorization(xAppId, xAppKey, region, url, expiration, originName);
            URI uri = new URI(url); // 替换成实际地址
            Map<String, String> headers = new HashMap<>();
            headers.put("X-APP-ID", xAppId);
            headers.put("Authorization", authorization);

            wsClient = new WebSocketUploader(uri, headers, (AudioUrlCallback) this);
            wsClient.connect();

            new Thread(() -> {
                // 等待连接成功再发送 init
                while (!wsClient.isOpen()) {
                    SystemClock.sleep(50);
                }

                wsClient.sendInit();

                int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT);

                audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                        SAMPLE_RATE,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufferSize);
                byte[] buffer = new byte[bufferSize];

                audioRecord.startRecording();
                isRecording = true;
                while (isRecording && wsClient.isOpen()) {
                    int read = audioRecord.read(buffer, 0, buffer.length);
                    if (read > 0) {
                        wsClient.sendAudioChunk(buffer, read);
                    }
                }
                audioRecord.stop();
                audioRecord.release();
                audioRecord = null;

                wsClient.sendEnd();

            }).start();

            Toast.makeText(this, "开始录音并上传", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "WebSocket连接失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopStreaming() {
        isRecording = false;
        Toast.makeText(this, "停止上传", Toast.LENGTH_SHORT).show();
    }

    private void createVoiceButton() {
        Button voiceBtn = new Button(this);
        voiceBtn.setText("点击播放语音");
        voiceBtn.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                600,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = 24;
        voiceBtn.setLayoutParams(params);
        voiceBtn.setBackgroundColor(0xFFDDDDDD);
        voiceBtn.setAllCaps(false);

        voiceBtn.setOnClickListener(v -> {
            if (latestAudioUrl != null) {
                MediaPlayer mediaPlayer = new MediaPlayer();
                try {
                    mediaPlayer.setDataSource(latestAudioUrl);
                    mediaPlayer.setOnPreparedListener(MediaPlayer::start);
                    mediaPlayer.setOnCompletionListener(MediaPlayer::release);
                    mediaPlayer.prepareAsync();
                    Toast.makeText(this, "开始播放音频", Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    mediaPlayer.release();
                    e.printStackTrace();
                    Toast.makeText(this, "播放失败", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "没有可播放的音频", Toast.LENGTH_SHORT).show();
            }
        });

        voiceContainer.addView(voiceBtn);
    }

    private boolean checkPermissions() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_CODE) {
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "请授予权限", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
            }
        }
    }
}