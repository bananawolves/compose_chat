package com.example.emotionlink.AudioDemo.Client;

import android.util.Log;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.util.Base64;
import java.util.Map;

import com.example.emotionlink.AudioDemo.AudioUrlCallback;
import com.example.emotionlink.AudioDemo.WebSocketStatusListener;

public class WebSocketUploader extends WebSocketClient {

    private final AudioUrlCallback audioUrlCallback;
    public WebSocketStatusListener statusListener; // 添加监听器字段
    private int chunkId = 0;
    public void setStatusListener(WebSocketStatusListener listener) {
        this.statusListener = listener;
    }

    public WebSocketUploader(URI serverUri, AudioUrlCallback callback) {
        super(serverUri);
        this.audioUrlCallback = callback;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        Log.d("WebSocket","Audio WebSocket opened");
        if (statusListener != null) {
            statusListener.onConnected();
        }
    }

    public void sendInit() {
        try {
            JSONObject init = new JSONObject();
            init.put("msg_type", "init");
            init.put("dtype", "int16");
            init.put("sample_rate", 16000);
            init.put("channels", 1);
            send(init.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void sendAudioChunk(byte[] audio, int length) {
        try {
            JSONObject audioObj = new JSONObject();
            audioObj.put("msg_type", "chunk_info");
            audioObj.put("chunk_id", chunkId);
            audioObj.put("status", "start"); // 可选："start" 或 "end"
            audioObj.put("chunk_size", length);
            send(audioObj.toString()); // 发送 JSON 元数据

            // 发送音频二进制 chunk（直接发裸 byte[]）
            byte[] chunk = new byte[length];
            System.arraycopy(audio, 0, chunk, 0, length);
            send(chunk); // 发送音频数据本体

            chunkId++; // 递增 chunk_id
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onMessage(String text) {
        try {
            Log.d("Websocket","Audio进入回调"+text);
            JSONObject json = new JSONObject(text);
            if (json.has("data") && !json.isNull("data")) {
                JSONObject data = json.getJSONObject("data");

                String fromUser = data.getString("from_user");
                String fromLanguage = data.getString("from_language");
                String language = data.getString("language");
                String textContent = data.getString("text");
                String wavFile = data.getString("wav_file");

                // 打印字段或进行后续逻辑处理
                System.out.println("fromUser = " + fromUser);
                System.out.println("fromLanguage = " + fromLanguage);
                System.out.println("language = " + language);
                System.out.println("text = " + textContent);
                System.out.println("wavFile = " + wavFile);
            } else {
                System.out.println("data 字段为空或不存在");
            }
            if ("audio".equals(json.optString("type"))) {
                String audioUrl = json.optString("audio_url");
                if (audioUrl != null && !audioUrl.isEmpty() && audioUrlCallback != null) {
                    audioUrlCallback.onAudioUrlReceived(audioUrl);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    public void sendEnd() {
        try {
            JSONObject endObj = new JSONObject();
            endObj.put("msg_type", "chunk_info");
            endObj.put("chunk_id", chunkId); // 最后一个 ID
            endObj.put("status", "end");
            endObj.put("chunk_size", 0);
            send(endObj.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onError(Exception ex) {
       Log.e("Websocket","Audio WebSocket error: " + ex.getMessage());
        if (statusListener != null) {
            statusListener.onError(ex);
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        Log.d("Websocket","WebSocket closed. Code: " + code + ", Reason: " + reason + ", Remote: " + remote);

    }
}
