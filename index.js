var React = require('react-native');
var NativeModules = React.NativeModules;
var Platform = React.Platform;
var invariant = require('invariant');
var DeviceEventEmitter = React.DeviceEventEmitter;
var SinchRTC;

if (Platform.OS === 'ios') {
    // invariant(SinchVerificationIOS, 'Add SinchVerificationIOS.h and SinchVerificationIOS.m to your Xcode project');
    // SinchRTC = NativeModules.SinchAndroidRTCIOS;
    invariant(SinchRTC, "iOS is currently not supported.");
} else if (Platform.OS === 'android') {
    invariant(SinchRTC, 'Import libraries to android "rnpm link"');
    SinchRTC = NativeModules.SinchAndroidRTCAndroid;
} else {
    invariant(SinchRTC, "Invalid platform");
}

var applicationKey = null,
    applicationSecret = null,
    environmentHost = null,
    userId = null;
var listeners = {};
module.exports = {

    addListener: function (eventName, callback) {
        var callBacks = [];
        if (listeners[eventName]) {
            callBacks = listeners[eventName];
        }
        callBacks.push(callback);
        listeners[eventName] = callBacks;
    },
    removeListener: function (eventName, callbackRef) {
        var i = listeners[eventName].indexOf(callbackRef);
        if (i != -1) {
            listeners[eventName].splice(i, 1);
        }

        if (!listeners[eventName].length) {
            removeAllListeners(eventName);
        }
    },

    removeAllListeners: function (eventName) {
        delete listeners[eventName];
    },

    init: function (appKey, appSecret, envHost, uId) {
        applicationKey = appKey;
        applicationSecret = appSecret;
        environmentHost = envHost;
        userId = uId;

        SinchRTC.init(appKey, appSecret, envHost, uId);
        DeviceEventEmitter.addListener('onIncomingCall', function (caller) {
            for (var i = 0; i < listeners['call'].length; i++) {
                var listener = listeners['call'][i];
                listener(caller);
            }
        });
        DeviceEventEmitter.addListener('onIncomingMessage', function (message) {
            for (var i = 0; i < listeners['message'].length; i++) {
                var listener = listeners['message'][i];
                listener(message);
            }
        })
    },

    startSinchClient: function (callback) {
        invariant(applicationKey, 'Call init() to setup the Sinch application key.');
        SinchRTC.startSinchClient(callback);
    },

    terminateSinchClient: function () {
        invariant(applicationKey, 'Call init() to setup the Sinch application key.');
        SinchRTC.terminateSinchClient();
    },

    setupAppToAppCall: function (remoteUserId) {
        invariant(applicationKey, 'Call init() to setup the Sinch application key.');
        SinchRTC.setupAppToAppCall(remoteUserId);
    },

    setupConferenceCall: function (conferenceId) {
        invariant(applicationKey, 'Call init() to setup the Sinch application key.');
        SinchRTC.setupConferenceCall(conferenceId);
    },

    answerIncomingCall: function (call) {
        invariant(applicationKey, 'Call init() to setup the Sinch application key.');
        SinchRTC.answerIncomingCall(call);
    },

    declineIncomingCall: function (call) {
        invariant(applicationKey, 'Call init() to setup the Sinch application key.');
        SinchRTC.declineIncomingCall(call);
    },

    // sms: function(phoneNumber, custom, callback) {
    // 	invariant(applicationKey, 'Call init() to setup the Sinch application key.');
    // 	SinchVerification.sms(applicationKey, phoneNumber, custom, callback);
    // },
    //
    // flashCall: function(phoneNumber, custom, callback) {
    // 	invariant(applicationKey, 'Call init() to setup the Sinch application key.');
    // 	SinchVerification.flashCall(applicationKey, phoneNumber, custom, callback);
    // },
    //
    // verify: SinchVerification.verify,

}
