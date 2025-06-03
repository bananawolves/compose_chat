//package com.example.emotionlink.AudioDemo;
//import java.io.File;
//import android.annotation.SuppressLint;
//import android.app.Dialog;
//import android.content.Context;
//import android.content.DialogInterface;
//import android.content.DialogInterface.OnDismissListener;
//import android.graphics.drawable.AnimationDrawable;
//import android.media.MediaRecorder;
//import android.os.CountDownTimer;
//import android.os.Environment;
//import android.os.Handler;
//import android.os.Message;
//import android.os.Vibrator;
//import android.util.AttributeSet;
//import android.view.Gravity;
//import android.view.MotionEvent;
//import android.view.ViewGroup;
//import android.view.WindowManager.LayoutParams;
//import android.widget.Button;
//import android.widget.ImageView;
//
//import com.example.emotionlink.R;
//
///**
// * 自定义语音按钮样式
// * @author ss
// * @date 2015-7-8 下午4:34:20
// * @version V1.0.0
// */
//@SuppressLint("NewApi")
//public class RecordButton extends androidx.appcompat.widget.AppCompatButton  {
//
//    public RecordButton(Context context) {
//        super(context);
//        init();
//    }
//
//    public RecordButton(Context context, AttributeSet attrs, int defStyle) {
//        super(context, attrs, defStyle);
//        init();
//    }
//
//    public RecordButton(Context context, AttributeSet attrs) {
//        super(context, attrs);
//        init();
//    }
//
//    public void setSavePath(String path) {
//        File filePath = new File(path);
//        if (!filePath.exists()) {
//            File file2 = new File(path.substring(0, path.lastIndexOf("/") + 1));
//            file2.mkdirs();
//        }
//        mFileName = path;
//    }
//
//    public void setOnFinishedRecordListener(OnFinishedRecordListener listener) {
//        finishedListener = listener;
//    }
//
//    private String mFileName = null;
//    private OnFinishedRecordListener finishedListener;
//    private static long startTime;
//    private Dialog recordDialog;
//    private static int[] res = { R.drawable.mic_1, R.drawable.mic_2, R.drawable.mic_3,
//            R.drawable.mic_4, R.drawable.mic_5 };
//    private static ImageView view;
//    private MediaRecorder recorder;
//    private ObtainDecibelThread thread;
//    private Handler volumeHandler;
//    private static final int MIN_INTERVAL_TIME = 1*1000;// 1s 最短
//    public final static int MAX_TIME = 60*1000 + 500;// 1分钟，最长
//    public static String File_Voice = Environment.getExternalStorageDirectory()
//            .getPath() + "/acoe/demo/voice";// 录音全部放在这个目录下
//    private final String  SAVE_PATH = File_Voice;
//
//    private float y ;
//
//    @SuppressLint("HandlerLeak")
//    private void init() {
//        volumeHandler = new Handler(){
//            @Override
//            public void handleMessage(Message msg) {
//                if(msg.what == -100){
//                    stopRecording();
//                    recordDialog.dismiss();
//                }else if(msg.what != -1){
//                    view.setImageResource(res[msg.what]);
//                }
//            }
//        };
//    }
//
//    private AnimationDrawable anim;
//    @SuppressLint("ClickableViewAccessibility")
//    @Override
//    public boolean onTouchEvent(MotionEvent event) {
//        int action = event.getAction();
//        y = event.getY();
//        if(view!=null && y<0){
//            view.setBackgroundResource(R.drawable.mic_cancel);
//            anim.stop();
//        }else if(view != null){
//            view.setBackgroundResource(R.drawable.anim_mic);
//            anim = (AnimationDrawable) view.getBackground();
//            anim.start();
//        }
//
//        switch (action) {
//            case MotionEvent.ACTION_DOWN:
//                setText("松开发送");
//                initDialogAndStartRecord();
//                anim = (AnimationDrawable) view.getBackground();
//                anim.start();
//                break;
//            case MotionEvent.ACTION_UP:
//                this.setText("按住录音");
//                startTimer.cancel(); // 主动松开时取消计时
//                recordTimer.cancel(); // 主动松开时取消计时
//                if(y>=0 && (System.currentTimeMillis() - startTime <= MAX_TIME)){
//                    finishRecord();
//                }else if(y<0){  //当手指向上滑，会cancel
//                    cancelRecord();
//                }
//                break;
//            case MotionEvent.ACTION_CANCEL: // 异常
//                cancelRecord();
//                break;
//        }
//
//        return true;
//    }
//
//    /**
//     * 初始化录音对话框 并 开始录音
//     */
//    private void initDialogAndStartRecord() {
//        startTime = System.currentTimeMillis();
//        recordDialog = new Dialog(getContext(), R.style.like_toast_dialog_style);
//        view = new ImageView(getContext());
//        view.setBackgroundResource(R.drawable.anim_mic);
//        recordDialog.setContentView(view, new LayoutParams(
//                ViewGroup.LayoutParams.WRAP_CONTENT,
//                ViewGroup.LayoutParams.WRAP_CONTENT));
//        recordDialog.setOnDismissListener(onDismiss);
//        LayoutParams lp = recordDialog.getWindow().getAttributes();
//        lp.gravity = Gravity.CENTER;
//
//        startRecording();
//        recordDialog.show();
//    }
//
//    /**
//     * 放开手指，结束录音处理
//     */
//    private void finishRecord() {
//        long intervalTime = System.currentTimeMillis() - startTime;
//        if (intervalTime < MIN_INTERVAL_TIME) {
//            volumeHandler.sendEmptyMessageDelayed(-100, 1000);
//            view.setBackgroundResource(R.drawable.mic_short);
//            anim.stop();
//            File file = new File(mFileName);
//            file.delete();
//            return;
//        }else{
//            stopRecording();
//            recordDialog.dismiss();
//        }
//        //如果有回调，则发送录音结束回调
//        if (finishedListener != null)
//            finishedListener.onFinishedRecord(mFileName,(int) (intervalTime/1000));
//    }
//
//    /**
//     * 取消录音对话框和停止录音
//     */
//    public void cancelRecord() {
//        stopRecording();
//        recordDialog.dismiss();
//        //MyToast.makeText(getContext(), "取消录音！", Toast.LENGTH_SHORT);
//        File file = new File(mFileName);
//        file.delete();
//    }
//
//    /**
//     * 录音开始计时器，允许的最大时长倒数10秒时进入倒计时
//     */
//    private CountDownTimer startTimer = new CountDownTimer(MAX_TIME - 500 - 10000, 1000) { // 50秒后开始倒计时
//        @Override
//        public void onFinish() {
//            recordTimer.start();
//        }
//        @Override
//        public void onTick(long millisUntilFinished) {
//        }};
//
//
//    /**
//     * 录音最后10秒倒计时器，倒计时结束发送录音
//     */
////    private CountDownTimer recordTimer = new CountDownTimer(10000, 1000){
////        @Override
////        public void onFinish() {
////            finishRecord();
////        }
////        @Override
////        public void onTick(long millisUntilFinished) { // 显示倒计时动画
////            switch ((int)millisUntilFinished / 1000) {
////                case 10:
////                    view.setBackgroundResource(R.drawable.mic_count_10);
////                    break;
////                case 9:
////                    view.setBackgroundResource(R.drawable.mic_count_9);
////                    break;
////                case 8:
////                    view.setBackgroundResource(R.drawable.mic_count_8);
////                    break;
////                case 7:
////                    view.setBackgroundResource(R.drawable.mic_count_7);
////                    break;
////                case 6:
////                    view.setBackgroundResource(R.drawable.mic_count_6);
////                    break;
////                case 5:
////                    view.setBackgroundResource(R.drawable.mic_count_5);
////                    break;
////                case 4:
////                    view.setBackgroundResource(R.drawable.mic_count_4);
////                    break;
////                case 3:
////                    view.setBackgroundResource(R.drawable.mic_count_3);
////                    break;
////                case 2:
////                    view.setBackgroundResource(R.drawable.mic_count_2);
////                    break;
////                case 1:
////                    view.setBackgroundResource(R.drawable.mic_count_1);
////                    break;
////            }
////        }};
//
////    private void stopRecording() {
////        if (thread != null) {
////            thread.exit();
////            thread = null;
////        }
////        if(extAudioRecorder != null){
////            extAudioRecorder.stop();
////            extAudioRecorder.release();
////        }
////    }
//
//    private class ObtainDecibelThread extends Thread {
//
//        private volatile boolean running = true;
//
//        public void exit() {
//            running = false;
//        }
//
//        @Override
//        public void run() {
//            while (running) {
//                if (recorder == null || !running) {
//                    break;
//                }
//                int x = recorder.getMaxAmplitude(); //振幅
//                if (x != 0 && y>=0) {
//                    int f = (int) (10 * Math.log(x) / Math.log(10));
//                    if (f < 15)
//                        volumeHandler.sendEmptyMessage(0);
//                    else if (f < 22)
//                        volumeHandler.sendEmptyMessage(1);
//                    else if (f < 29)
//                        volumeHandler.sendEmptyMessage(2);
//                    else if (f < 38)
//                        volumeHandler.sendEmptyMessage(3);
//                    else
//                        volumeHandler.sendEmptyMessage(4);
//                }
//
//                volumeHandler.sendEmptyMessage(-1);
//                if(System.currentTimeMillis() - startTime > MAX_TIME){
//                    finishRecord();
//                }
//
//                try {
//                    Thread.sleep(200);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
//
//    }
//
//    private OnDismissListener onDismiss = dialog -> stopRecording();
//
//    public interface OnFinishedRecordListener {
//        public void onFinishedRecord(String audioPath, int time);
//    }
//
//    class CountDown extends CountDownTimer {
//
//        /**
//         * @param millisInFuture
//         * @param countDownInterval
//         */
//        public CountDown(long millisInFuture, long countDownInterval) {
//            super(millisInFuture, countDownInterval);
//            // TODO Auto-generated constructor stub
//        }
//
//        @Override
//        public void onFinish() {
//        }
//
//        @Override
//        public void onTick(long millisUntilFinished) {
//        }
//
//    }
//}
