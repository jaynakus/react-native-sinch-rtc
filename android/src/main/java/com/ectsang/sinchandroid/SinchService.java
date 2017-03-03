package com.ectsang.sinchandroid;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import com.sinch.android.rtc.*;
import com.sinch.android.rtc.calling.Call;
import com.sinch.android.rtc.calling.CallClient;
import com.sinch.android.rtc.calling.CallClientListener;
import com.sinch.android.rtc.messaging.MessageClientListener;
import com.sinch.android.rtc.messaging.WritableMessage;

public class SinchService extends Service {

    public static String USER_ID = "anonym";
    public static String APP_KEY = "enter-application-key";
    public static String APP_SECRET = "enter-application-secret";
    public static String ENVIRONMENT = "sandbox.sinch.com";

    private static final String TAG = SinchService.class.getSimpleName();

    private final SinchServiceInterface mServiceInterface = new SinchServiceInterface();

    private SinchClient mSinchClient = null;
    private StartFailedListener mListener;

    public class SinchServiceInterface extends Binder {

        public void setStartListener(StartFailedListener listener) {
            mListener = listener;
        }

        public void addMessageClientListener(MessageClientListener listener) {
            mSinchClient.getMessageClient().addMessageClientListener(listener);
        }

        public void addCallClientListener(CallClientListener listener) {
            mSinchClient.getCallClient().addCallClientListener(listener);
        }

        public void removeMessageClientListener(MessageClientListener listener) {
            mSinchClient.getMessageClient().removeMessageClientListener(listener);
        }

        public void removeCallClientListener(CallClientListener listener) {
            mSinchClient.getCallClient().removeCallClientListener(listener);
        }

        public boolean isStarted() {
            return SinchService.this.isStarted();
        }

        public void startClient() {
            start(USER_ID);
        }

        public void stopClient() {
            stop();
        }


        public void sendMessage(String recipientUserId, String textBody) {
            SinchService.this.sendMessage(recipientUserId, textBody);
        }


        public CallClient getCallClient() {
            return SinchService.this.mSinchClient.getCallClient();
        }

        public Call callPhoneNumber(String phoneNumber) {
            return mSinchClient.getCallClient().callPhoneNumber(phoneNumber);
        }

        public Call callUser(String userId) {
            if (mSinchClient == null) {
                return null;
            }
            return mSinchClient.getCallClient().callUser(userId);
        }

        public String getUserName() {
            return USER_ID;
        }

        public void startClient(String userName) {
            start(userName);
        }

        public Call getCall(String callId) {
            return mSinchClient.getCallClient().getCall(callId);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        stop();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mServiceInterface;
    }

    private boolean isStarted() {
        return (mSinchClient != null && mSinchClient.isStarted());
    }

    private void sendMessage(String recipientUserId, String textBody) {
        if (isStarted()) {
            WritableMessage message = new WritableMessage(recipientUserId, textBody);
            mSinchClient.getMessageClient().send(message);
        }
    }

    private void start(String userName) {
        if (mSinchClient == null) {
            mSinchClient = Sinch.getSinchClientBuilder()
                    .context(getApplicationContext())
                    .userId(userName)
                    .applicationKey(APP_KEY)
                    .applicationSecret(APP_SECRET)
                    .environmentHost(ENVIRONMENT)
                    .build();

            mSinchClient.setSupportMessaging(true);
            mSinchClient.setSupportCalling(true);
            mSinchClient.setSupportActiveConnectionInBackground(true);
            mSinchClient.addSinchClientListener(new MySinchClientListener());

            mSinchClient.getCallClient().setRespectNativeCalls(false);

            mSinchClient.startListeningOnActiveConnection();
            mSinchClient.start();
        }
    }

    private void stop() {
        if (mSinchClient != null) {
            mSinchClient.stopListeningOnActiveConnection();
            mSinchClient.terminate();
            mSinchClient = null;
        }
    }

    public interface StartFailedListener {

        void onStartFailed(SinchError error);

        void onStarted();
    }

    private class MySinchClientListener implements SinchClientListener {

        @Override
        public void onClientFailed(SinchClient client, SinchError error) {
            if (mListener != null) {
                mListener.onStartFailed(error);
            }
            stop();
        }

        @Override
        public void onClientStarted(SinchClient client) {
            Log.d(TAG, "SinchClient started");
            if (mListener != null) {
                mListener.onStarted();
            }
        }

        @Override
        public void onClientStopped(SinchClient client) {
            Log.d(TAG, "SinchClient stopped");
        }

        @Override
        public void onLogMessage(int level, String area, String message) {
            switch (level) {
                case Log.DEBUG:
                    Log.d(area, message);
                    break;
                case Log.ERROR:
                    Log.e(area, message);
                    break;
                case Log.INFO:
                    Log.i(area, message);
                    break;
                case Log.VERBOSE:
                    Log.v(area, message);
                    break;
                case Log.WARN:
                    Log.w(area, message);
                    break;
            }
        }

        @Override
        public void onRegistrationCredentialsRequired(SinchClient client, ClientRegistration clientRegistration) {
        }
    }
}
