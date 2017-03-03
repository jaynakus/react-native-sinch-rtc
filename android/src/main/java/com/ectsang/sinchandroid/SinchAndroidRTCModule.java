package com.ectsang.sinchandroid;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;
import com.facebook.react.bridge.*;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.sinch.android.rtc.PushPair;
import com.sinch.android.rtc.SinchError;
import com.sinch.android.rtc.calling.Call;
import com.sinch.android.rtc.calling.CallClient;
import com.sinch.android.rtc.calling.CallClientListener;
import com.sinch.android.rtc.calling.CallListener;
import com.sinch.android.rtc.messaging.*;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;

import static android.content.Context.BIND_AUTO_CREATE;

public class SinchAndroidRTCModule extends ReactContextBaseJavaModule implements ServiceConnection {

    private ReactApplicationContext mContext;
    private CallClient callClient;
    private Callback mCallback;
    private SinchService.SinchServiceInterface mSinchServiceInterface;

    private MessageClientInterface messageClientInterface;
    private CallClientInterface callClientInterface;
    private HashMap<String, Call> incomingCalls = new HashMap<>();
    private HashMap<String, Call> outgoingCalls = new HashMap<>();

    public class CallInterface implements CallListener {

        @Override
        public void onCallProgressing(Call call) {
            Log.i("CallProgressing", call.getCallId().toString());
        }

        @Override
        public void onCallEstablished(Call call) {
            Log.i("CallEstablished", call.getCallId().toString());
        }

        @Override
        public void onCallEnded(Call call) {
            Log.i("CallEnded", call.getCallId().toString());
            incomingCalls.remove(call.getRemoteUserId());
        }

        @Override
        public void onShouldSendPushNotification(Call call, List<PushPair> list) {
            Log.i("ShouldSendPushNotif", call.getCallId().toString());
        }
    }

    public class MessageClientInterface implements MessageClientListener {

        @Override
        public void onIncomingMessage(MessageClient messageClient, Message message) {
            String sender = message.getSenderId();
            WritableMap map = Arguments.createMap();
            map.putString("sender", sender);
            map.putString("body", message.getTextBody());
            map.putString("id", message.getMessageId());
            map.putString("timestamp", new SimpleDateFormat("YYYYMMddHHmmss").format(message.getTimestamp()));
            mContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                    .emit("onIncomingMessage", map);
        }

        @Override
        public void onMessageSent(MessageClient messageClient, Message message, String s) {

        }

        @Override
        public void onMessageFailed(MessageClient messageClient, Message message, MessageFailureInfo messageFailureInfo) {

        }

        @Override
        public void onMessageDelivered(MessageClient messageClient, MessageDeliveryInfo messageDeliveryInfo) {

        }

        @Override
        public void onShouldSendPushData(MessageClient messageClient, Message message, List<PushPair> list) {

        }
    }

    public class CallClientInterface implements CallClientListener {
        @Override
        public void onIncomingCall(CallClient callClient, Call call) {
            String caller = call.getRemoteUserId();
            incomingCalls.put(caller, call);
            WritableMap map = Arguments.createMap();
            map.putString("caller", caller);
            mContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                    .emit("onIncomingCall", map);
        }
    }

    public SinchAndroidRTCModule(ReactApplicationContext reactContext) {
        super(reactContext);
        mContext = reactContext;
    }

    @Override
    public String getName() {
        return "SinchAndroidRTCAndroid";
    }

    @ReactMethod
    public void init(String applicationKey, String applicationSecret, String environmentHost, String userId) {
        SinchService.APP_KEY = applicationKey;
        SinchService.APP_SECRET = applicationSecret;
        if (environmentHost != null)
            SinchService.ENVIRONMENT = environmentHost;
        SinchService.USER_ID = userId;
        mContext.bindService(new Intent(mContext, SinchService.class), this, BIND_AUTO_CREATE);
    }

    @ReactMethod
    public void startSinchClient(Callback callback) {
        mCallback = callback;
        if (mSinchServiceInterface != null) {
            mSinchServiceInterface.startClient();
        }
    }

    @ReactMethod
    public void terminateSinchClient() {
        if (mSinchServiceInterface != null) {
            if (messageClientInterface != null) {
                mSinchServiceInterface.removeMessageClientListener(messageClientInterface);
                messageClientInterface = null;
            }
            if (callClientInterface != null) {
                mSinchServiceInterface.removeCallClientListener(callClientInterface);
                callClientInterface = null;
            }
            mSinchServiceInterface.stopClient();
            mSinchServiceInterface = null;
            incomingCalls.clear();
        }
    }

    @ReactMethod
    public void setupAppToAppCall(String remoteUserId) {
        callClient = mSinchServiceInterface.getCallClient();
        Call call = callClient.callUser(remoteUserId);
        call.addCallListener(new CallInterface());
    }

    @ReactMethod
    public void setupConferenceCall(String conferenceId) {
        callClient = mSinchServiceInterface.getCallClient();
        Call call = callClient.callConference(conferenceId);
        call.addCallListener(new CallInterface());
    }

    @ReactMethod
    public void answerIncomingCall(String caller) {
        Call call = incomingCalls.get(caller);
        if (call != null) {
            call.addCallListener(new CallInterface());
            call.answer();
        }
    }

    @ReactMethod
    public void declineIncomingCall(String caller) {
        Call call = incomingCalls.get(caller);
        if (call != null) {
            call.hangup();
            incomingCalls.remove(caller);
        }
    }

    private void consumeCallback(Boolean success, WritableMap payload) {
        if (mCallback != null) {
            if (success) {
                mCallback.invoke(null, payload);
            } else {
                mCallback.invoke(payload, null);
            }
            mCallback = null;
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        if (SinchService.class.getName().equals(name.getClassName())) {
            mSinchServiceInterface = (SinchService.SinchServiceInterface) service;
            mSinchServiceInterface.setStartListener(new SinchService.StartFailedListener() {
                @Override
                public void onStartFailed(SinchError error) {
                    consumeCallback(false, new WritableNativeMap());
                }

                @Override
                public void onStarted() {
                    consumeCallback(true, new WritableNativeMap());
                }
            });
            messageClientInterface = new MessageClientInterface();
            mSinchServiceInterface.addMessageClientListener(messageClientInterface);
            callClientInterface = new CallClientInterface();
            mSinchServiceInterface.addCallClientListener(callClientInterface);
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        if (SinchService.class.getName().equals(name.getClassName())) {
            terminateSinchClient();
        }
    }
}
