package com.yingyingshejiao.Call;

import android.content.res.Configuration;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.yingyingshejiao.R;
import com.hyphenate.chat.EMCallStateChangeListener;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMVideoCallHelper;
import com.hyphenate.exceptions.HyphenateException;
import com.hyphenate.media.EMCallSurfaceView;
import com.superrtc.sdk.VideoView;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;


/**
 * Created by chen on 2018/3/17.
 * 视频通话界面处理
 */
public class VideoCallActivity extends CallActivity implements View.OnClickListener {

    // 视频通话帮助类
    private EMVideoCallHelper videoCallHelper;
    // SurfaceView 控件状态，-1 表示通话未接通，0 表示本小远大，1 表示远小本大
    private int surfaceState = -1;

    private EMCallSurfaceView localSurface = null;
    private EMCallSurfaceView oppositeSurface = null;
    private RelativeLayout.LayoutParams localParams = null;
    private RelativeLayout.LayoutParams oppositeParams = null;

    // 获取控件
    private View controlLayout;
    private RelativeLayout surfaceLayout;

    private ImageButton exitFullScreenBtn;
    private TextView callStateView;
    private TextView callTimeView;
    private ImageButton micSwitch;
    private ImageButton cameraSwitch;
    private ImageButton speakerSwitch;
    private ImageButton recordSwitch;
    private ImageButton changeCameraSwitch;
    private FloatingActionButton rejectCallFab;
    private FloatingActionButton endCallFab;
    private FloatingActionButton answerCallFab;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_call);

        initView();
    }

    /**
     * 重载父类方法,实现一些当前通话的操作，
     */
    @Override protected void initView() {
        super.initView();
        controlLayout=(View)findViewById(R.id.layout_call_control);
        surfaceLayout=(RelativeLayout)findViewById(R.id.layout_surface_container);
        callStateView=(TextView)findViewById(R.id.text_call_state);
        callTimeView=(TextView)findViewById(R.id.text_call_time);
        cameraSwitch=(ImageButton)findViewById(R.id.btn_camera_switch);
        exitFullScreenBtn=(ImageButton)findViewById(R.id.btn_exit_full_screen);
        micSwitch=(ImageButton)findViewById(R.id.btn_mic_switch);
        speakerSwitch=(ImageButton)findViewById(R.id.btn_speaker_switch);
        recordSwitch=(ImageButton)findViewById(R.id.btn_record_switch);
        changeCameraSwitch=(ImageButton)findViewById(R.id.btn_change_camera_switch);
        rejectCallFab=(FloatingActionButton)findViewById(R.id.fab_reject_call);
        endCallFab=(FloatingActionButton)findViewById(R.id.fab_end_call);
        answerCallFab=(FloatingActionButton)findViewById(R.id.fab_answer_call);

        controlLayout.setOnClickListener(VideoCallActivity.this);
        exitFullScreenBtn.setOnClickListener(VideoCallActivity.this);
        micSwitch.setOnClickListener(VideoCallActivity.this);
        cameraSwitch.setOnClickListener(VideoCallActivity.this);
        speakerSwitch.setOnClickListener(VideoCallActivity.this);
        changeCameraSwitch.setOnClickListener(VideoCallActivity.this);
        rejectCallFab.setOnClickListener(VideoCallActivity.this);
        endCallFab.setOnClickListener(VideoCallActivity.this);
        answerCallFab.setOnClickListener(VideoCallActivity.this);

        if (CallManager.getInstance().isInComingCall()) {
            endCallFab.setVisibility(View.GONE);
            answerCallFab.setVisibility(View.VISIBLE);
            rejectCallFab.setVisibility(View.VISIBLE);
            callStateView.setText(R.string.call_connected_is_incoming);
        } else {
            endCallFab.setVisibility(View.VISIBLE);
            answerCallFab.setVisibility(View.GONE);
            rejectCallFab.setVisibility(View.GONE);
            callStateView.setText(R.string.call_connecting);
        }

        micSwitch.setActivated(!CallManager.getInstance().isOpenMic());
        cameraSwitch.setActivated(!CallManager.getInstance().isOpenCamera());
        speakerSwitch.setActivated(CallManager.getInstance().isOpenSpeaker());
        recordSwitch.setActivated(CallManager.getInstance().isOpenRecord());

        // 初始化视频通话帮助类
        //videoCallHelper = EMClient.getInstance().callManager().getVideoCallHelper();

        // 初始化显示通话画面
        initCallSurface();
        // 判断当前通话时刚开始，还是从后台恢复已经存在的通话
        if (CallManager.getInstance().getCallState() == CallManager.CallState.ACCEPTED) {
            endCallFab.setVisibility(View.VISIBLE);
            answerCallFab.setVisibility(View.GONE);
            rejectCallFab.setVisibility(View.GONE);
            callStateView.setText(R.string.call_accepted);
            refreshCallTime();
            // 通话已接通，修改画面显示
            onCallSurface();
        }

        try {
            // 设置默认摄像头为前置
            EMClient.getInstance().callManager().setCameraFacing(Camera.CameraInfo.CAMERA_FACING_FRONT);
        } catch (HyphenateException e) {
            e.printStackTrace();
        }
        CallManager.getInstance().setCallCameraDataProcessor();
    }

    /**
     * 界面控件点击监听器
     */

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.layout_call_control:
                onControlLayout();
                break;
            case R.id.btn_exit_full_screen:
                // 最小化通话界面
                //exitFullScreen();
                break;
            case R.id.btn_change_camera_switch:
                // 切换摄像头
                changeCamera();
                break;
            case R.id.btn_mic_switch:
                // 麦克风开关
                onMicrophone();
                break;
            case R.id.btn_camera_switch:
                // 摄像头开关
                onCamera();
                break;
            case R.id.btn_speaker_switch:
                // 扬声器开关
                onSpeaker();
                break;
            case R.id.fab_end_call:
                // 结束通话
                endCall();
                break;
            case R.id.fab_reject_call:
                // 拒绝接听通话
                rejectCall();
                break;
            case R.id.fab_answer_call:
                // 接听通话
                answerCall();
                break;
        }
    }


    /**
     * 控制界面的显示与隐藏
     */
    private void onControlLayout() {
        if (controlLayout.isShown()) {
            controlLayout.setVisibility(View.GONE);
        } else {
            controlLayout.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 退出全屏通话界面
     */
//    private void exitFullScreen() {
//        CallManager.getInstance().addFloatWindow();
//        // 结束当前界面
//        onFinish();
//    }

    /**
     * 切换摄像头
     */
    private void changeCamera() {
        // 根据切换摄像头开关是否被激活确定当前是前置还是后置摄像头
        try {
            if (EMClient.getInstance().callManager().getCameraFacing() == 1) {
                EMClient.getInstance().callManager().switchCamera();
                EMClient.getInstance().callManager().setCameraFacing(0);
                // 设置按钮图标
                changeCameraSwitch.setImageResource(R.drawable.ic_camera_rear_white_24dp);
            } else {
                EMClient.getInstance().callManager().switchCamera();
                EMClient.getInstance().callManager().setCameraFacing(1);
                // 设置按钮图标
                changeCameraSwitch.setImageResource(R.drawable.ic_camera_front_white_24dp);
            }
        } catch (HyphenateException e) {
            e.printStackTrace();
        }
    }

    /**
     * 麦克风开关，主要调用环信语音数据传输方法
     */
    private void onMicrophone() {
        try {
            // 根据麦克风开关是否被激活来进行判断麦克风状态，然后进行下一步操作
            if (micSwitch.isActivated()) {
                // 设置按钮状态
                micSwitch.setActivated(false);
                // 恢复语音数据的传输
                EMClient.getInstance().callManager().resumeVoiceTransfer();
                CallManager.getInstance().setOpenMic(true);
            } else {
                // 设置按钮状态
                micSwitch.setActivated(true);
                // 暂停语音数据的传输
                EMClient.getInstance().callManager().pauseVoiceTransfer();
                CallManager.getInstance().setOpenMic(false);
            }
        } catch (HyphenateException e) {
            VMLog.e("exception code: %d, %s", e.getErrorCode(), e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 摄像头开关
     */
    private void onCamera() {
        try {
            // 根据摄像头开关按钮状态判断摄像头状态，然后进行下一步操作
            if (cameraSwitch.isActivated()) {
                // 设置按钮状态
                cameraSwitch.setActivated(false);
                // 恢复视频数据的传输
                EMClient.getInstance().callManager().resumeVideoTransfer();
                CallManager.getInstance().setOpenCamera(true);
            } else {
                // 设置按钮状态
                cameraSwitch.setActivated(true);
                // 暂停视频数据的传输
                EMClient.getInstance().callManager().pauseVideoTransfer();
                CallManager.getInstance().setOpenCamera(false);
            }
        } catch (HyphenateException e) {
            VMLog.e("exception code: %d, %s", e.getErrorCode(), e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 扬声器开关
     */
    private void onSpeaker() {
        // 根据按钮状态决定打开还是关闭扬声器
        if (speakerSwitch.isActivated()) {
            // 设置按钮状态
            speakerSwitch.setActivated(false);
            CallManager.getInstance().closeSpeaker();
            CallManager.getInstance().setOpenSpeaker(false);
        } else {
            // 设置按钮状态
            speakerSwitch.setActivated(true);
            CallManager.getInstance().openSpeaker();
            CallManager.getInstance().setOpenSpeaker(true);
        }
    }

    /**
     * 接听通话
     */
    @Override protected void answerCall() {
        super.answerCall();
        endCallFab.setVisibility(View.VISIBLE);
        rejectCallFab.setVisibility(View.GONE);
        answerCallFab.setVisibility(View.GONE);
    }

    /**
     * 初始化通话界面控件
     */
    private void initCallSurface() {
        // 初始化显示远端画面控件
        oppositeSurface = new EMCallSurfaceView(activity);
        oppositeParams = new RelativeLayout.LayoutParams(0, 0);
        oppositeParams.width = RelativeLayout.LayoutParams.MATCH_PARENT;
        oppositeParams.height = RelativeLayout.LayoutParams.MATCH_PARENT;
        oppositeSurface.setLayoutParams(oppositeParams);
        surfaceLayout.addView(oppositeSurface);

        // 初始化显示本地画面控件
        localSurface = new EMCallSurfaceView(activity);
        localParams = new RelativeLayout.LayoutParams(0, 0);
        localParams.width = RelativeLayout.LayoutParams.MATCH_PARENT;
        localParams.height = RelativeLayout.LayoutParams.MATCH_PARENT;
        localParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        localSurface.setLayoutParams(localParams);
        surfaceLayout.addView(localSurface);

        localSurface.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                onControlLayout();
            }
        });

        localSurface.setZOrderOnTop(false);
        localSurface.setZOrderMediaOverlay(true);

        // 设置本地和远端画面的显示方式，是填充，还是居中
        localSurface.setScaleMode(VideoView.EMCallViewScaleMode.EMCallViewScaleModeAspectFill);
        oppositeSurface.setScaleMode(VideoView.EMCallViewScaleMode.EMCallViewScaleModeAspectFill);
        // 设置通话画面显示控件
        EMClient.getInstance().callManager().setSurfaceView(localSurface, oppositeSurface);
    }

    /**
     * 接通通话，这个时候要做的只是改变本地画面 view 大小，不需要做其他操作
     */
    private void onCallSurface() {
        // 更新通话界面控件状态
        surfaceState = 0;

        int width = VMUtil.dp2px(activity, 96);
        int height = VMUtil.dp2px(activity, 128);
        int rightMargin = VMUtil.dp2px(activity, 16);
        int topMargin = VMUtil.dp2px(activity, 96);

        localParams = new RelativeLayout.LayoutParams(width, height);
        localParams.width = width;
        localParams.height = height;
        localParams.rightMargin = rightMargin;
        localParams.topMargin = topMargin;
        localParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        localSurface.setLayoutParams(localParams);

        localSurface.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                changeCallSurface();
            }
        });

        oppositeSurface.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                onControlLayout();
            }
        });
    }

    /**
     * 切换通话界面，这里就是交换本地和远端画面控件设置，以达到通话大小画面的切换
     */
    private void changeCallSurface() {
        if (surfaceState == 0) {
            surfaceState = 1;
            EMClient.getInstance().callManager().setSurfaceView(oppositeSurface, localSurface);
        } else {
            surfaceState = 0;
            EMClient.getInstance().callManager().setSurfaceView(localSurface, oppositeSurface);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN) public void onEventBus(CallEvent event) {
        if (event.isState()) {
            refreshCallView(event);
        }
        if (event.isTime()) {
            // 不论什么情况都检查下当前时间
            refreshCallTime();
        }
    }

    /**
     * 刷新通话界面
     */
    private void refreshCallView(CallEvent event) {
        EMCallStateChangeListener.CallError callError = event.getCallError();
        EMCallStateChangeListener.CallState callState = event.getCallState();
        switch (callState) {
            case CONNECTING: // 正在呼叫对方，TODO 没见回调过
                VMLog.i("正在呼叫对方" + callError);
                break;
            case CONNECTED: // 正在等待对方接受呼叫申请（对方申请与你进行通话）
                VMLog.i("正在连接" + callError);
                if (CallManager.getInstance().isInComingCall()) {
                    callStateView.setText(R.string.call_connected_is_incoming);
                } else {
                    callStateView.setText(R.string.call_connected);
                }
                break;
            case ACCEPTED: // 通话已接通
                VMLog.i("通话已接通");
                callStateView.setText(R.string.call_accepted);
                // 通话接通，更新界面 UI 显示
                onCallSurface();
                break;
            case DISCONNECTED: // 通话已中断
                VMLog.i("通话已结束" + callError);
                onFinish();
                break;
            // TODO 3.3.0版本 SDK 下边几个暂时都没有回调
            case NETWORK_UNSTABLE:
                if (callError == EMCallStateChangeListener.CallError.ERROR_NO_DATA) {
                    VMLog.i("没有通话数据" + callError);
                } else {
                    VMLog.i("网络不稳定" + callError);
                }
                break;
            case NETWORK_NORMAL:
                VMLog.i("网络正常");
                break;
            case VIDEO_PAUSE:
                VMLog.i("视频传输已暂停");
                break;
            case VIDEO_RESUME:
                VMLog.i("视频传输已恢复");
                break;
            case VOICE_PAUSE:
                VMLog.i("语音传输已暂停");
                break;
            case VOICE_RESUME:
                VMLog.i("语音传输已恢复");
                break;
            default:
                break;
        }
    }

    /**
     * 刷新通话时间显示
     */
    private void refreshCallTime() {
        int t = CallManager.getInstance().getCallTime();
        int h = t / 60 / 60;
        int m = t / 60 % 60;
        int s = t % 60 % 60;
        String time = "";
        if (h > 9) {
            time = "" + h;
        } else {
            time = "0" + h;
        }
        if (m > 9) {
            time += ":" + m;
        } else {
            time += ":0" + m;
        }
        if (s > 9) {
            time += ":" + s;
        } else {
            time += ":0" + s;
        }
        if (!callTimeView.isShown()) {
            callTimeView.setVisibility(View.VISIBLE);
        }
        callTimeView.setText(time);
    }

    /**
     * 屏幕方向改变回调方法
     */
    @Override public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override protected void onFinish() {
        // 结束通话要把 SurfaceView 释放 重置为 null
        surfaceLayout.removeAllViews();
        localSurface = null;
        oppositeSurface = null;
        super.onFinish();
    }

}
