package com.example.emotionlink.AudioDemo.Client;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.util.Base64;
import java.util.Map;

import com.example.emotionlink.AudioDemo.AudioUrlCallback;

public class WebSocketUploader extends WebSocketClient {

    private final AudioUrlCallback audioUrlCallback;

    public WebSocketUploader(URI serverUri, Map<String, String> headers , AudioUrlCallback callback) {
        super(serverUri, headers);
        this.audioUrlCallback = callback;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        System.out.println("Audio WebSocket opened");
    }

    public void sendInit() {
        try {
            JSONObject init = new JSONObject();
            init.put("req_id", "req-" + System.currentTimeMillis());
            init.put("rec_status", 0);
            JSONObject option = new JSONObject();
            option.put("sample_rate", 16000);
            option.put("enable_punctuation", true);
            option.put("enable_inverse_text_normalization", true);
            init.put("option", option);
            send(init.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendAudioChunk(byte[] audio, int length) {
        try {
            byte[] chunk = new byte[length];
            System.arraycopy(audio, 0, chunk, 0, length);
//            JSONObject audioObj = new JSONObject();
//            audioObj.put("rec_status", 1);
//            audioObj.put("audio_stream", Base64.getEncoder().encodeToString(chunk));
//            send(audioObj.toString());
            send(chunk);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onMessage(String text) {
        try {
            System.out.println("Audio进入回调"+text);//{"code":10000,"message":"Success","res_status":0,"sid":"c27a74f3-de39-430e-8ed9-7356232d15a7"}
            JSONObject json = new JSONObject(text);
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
            JSONObject end = new JSONObject();
            end.put("rec_status", 2);
            send(end.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onError(Exception ex) {
        System.err.println("Audio WebSocket error: " + ex.getMessage());
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("Audio WebSocket closed: " + reason);
    }
}
