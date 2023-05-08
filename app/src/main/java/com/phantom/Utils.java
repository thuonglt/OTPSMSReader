package com.phantom;

import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class Utils {

    public static String TAG = "Utils";
    public static String accessToken = "";
    public static RequestQueue volleyQueue = null;
    public static String loginUrl = "https://citysports-api.herokuapp.com/auth/login/";
    public static String username = "admin";
    public static String password = "1Akaraxelok@";
    public static String uploadSmsUrl = "https://citysports-api.herokuapp.com/payment/upload/sms";

    public static String[] fakeSmsBody = {"SD TK 0691000331123 +5,000,000VND luc 29-01-2021 09:16:31. SD 27,990,153VND. Ref MBVCB.965530138.Hz ck.CT tu 0351000803959 HOANG QUANG TRUNG toi 06910003311..."
            , "SD TK 0691000331123 +1,500,000VND luc 04-12-2020 18:18:42. SD 28,863,589VND. Ref 621322.041220.181817.Ngan hang TMCP Ngoai Thuong Viet Nam 0691000331123 TRU..."
            , "SD TK 0691000331123 +22,073,623VND luc 20-11-2020 06:33:00. SD 35,533,194VND. Ref SEV PAYROLL 50121254 ITM3139"
            , "SD TK 0691000331123 +500,000VND luc 14-11-2020 14:10:42. SD 17,189,821VND. Ref 383208.141120.141039.Thu xoan tai nghe FT20319188602556"
            , "SD TK 0691000331123 +18,637,732VND luc 29-01-2021 06:17:21. SD 22,990,153VND. Ref SEV PAYROLL 50125891 ITM4120"
            , "SD TK 0691000331123 +5,200,000VND luc 21-01-2021 10:02:51. SD 34,745,791VND. Ref MBVCB.953041896.LE TUAN VINH chuyen tien thuong bang lai xe 2101.CT tu 0211..."
            , "SD TK 0691000331123 +5,200,000VND luc 21-01-2021 09:52:43. SD 29,545,791VND. Ref 257053.210121.095238.hoc bang lai xe"
            , "SD TK 0691000331123 +20,334,985VND luc 21-01-2021 06:32:09. SD 22,345,791VND. Ref SEV PAYROLL 50125221 ITM3074"
    };

    public static void loginAndUpload(String mess, String sender) {
        StringRequest loginRequest = new StringRequest(Request.Method.POST, Utils.loginUrl, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                if (!response.equals(null)) {
                    JSONObject jsonObject = null;
                    try {
                        jsonObject = new JSONObject(response);
                        JSONObject tokens = jsonObject.getJSONObject("tokens");
                        Utils.accessToken = tokens.getString("access").toString().trim();
                        Log.e(TAG, "onResponse access:" + Utils.accessToken);
                        Utils.uploadSms(sender, mess);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    Log.e("Your Array Response", "Data Null");
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("error is ", "" + error);
            }
        }) {
            //Pass Your Parameters here
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<String, String>();
                params.put("username", username);
                params.put("password", password);
                return params;
            }
        };
        Utils.volleyQueue.add(loginRequest);
    }

    public static void uploadSms(String sender, String mess) {
        if (accessToken.isEmpty() || volleyQueue == null) {
            Log.d(TAG, "uploadSms accessToken not init!");
            return;
        }
        JSONObject rawBodyJson = exportSmsDataToJson(mess, sender);
        if (rawBodyJson == null) {
            Log.d(TAG, "uploadSms rawBodyJson is null!");
            return;
        }
        Log.d(TAG, "uploadSms start!");
        StringRequest uploadRequest = new StringRequest(Request.Method.POST, Utils.uploadSmsUrl, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                if (response != null && !response.isEmpty()) {
                    JSONObject jsonObject = null;
                    Log.e(TAG, "onResponse: " + response);
                } else {
                    Log.e("Your Array Response", "Data Null");
                }
            }

        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "error is " + error);
            }
        }) {

            //This is for Headers If You Needed
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();
                params.put("Authorization", "Bearer " + accessToken);
                params.put("Content-Type", "application/json");
                return params;
            }

            //Pass Your Body here
            @Override
            public byte[] getBody() throws AuthFailureError {
                byte[] body = {};
                try {
                    body = rawBodyJson.toString().replace("\\/", "/").getBytes("utf-8");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return body;
            }

        };
        Utils.volleyQueue.add(uploadRequest);
    }

    public static SmsData parseSms(String sms) {
        String[] splitData = sms.split(" ");
        SmsData data = new SmsData();
        if (sms.startsWith("SD TK")) {
            data.mAmount = splitData[3];
            data.mTime = splitData[5] + " " + splitData[6];
            data.mTime = data.mTime.replace(".", "");
            data.mTime = data.mTime.replace("-", "/");
            Log.d(TAG, "mTime:" + data.mTime);
            // Ref content
            int startRefIndex = sms.indexOf(". Ref");
            int endRefIndex = sms.indexOf(".CT tu");
            String refData = "";
            if (startRefIndex > 0) {
                if (endRefIndex > startRefIndex) {
                    refData = sms.substring(startRefIndex + 5, endRefIndex).trim();
                } else {
                    refData = sms.substring(startRefIndex + 5).trim();
                }
            }
            data.mContent = refData;
            //-----------------------------------------
        }
        return data;
    }

    public static JSONObject exportSmsDataToJson(String sms, String sender) {
        Log.d(TAG, "exportSmsDataToJson sms:" + sms);
        SmsData smsData = parseSms(sms);
        if (smsData.mAmount.isEmpty() || smsData.mAmount.startsWith("-")) {
            return null;
        }
        JSONObject jsonObject = new JSONObject();
        JSONObject smsObject = new JSONObject();
        JSONObject senderObject = new JSONObject();
        try {
            smsObject.put("content", sms);
            smsObject.put("ref", smsData.mContent);
            smsObject.put("amount", smsData.mAmount);
            smsObject.put("time", smsData.mTime);

            senderObject.put("name", smsData.mNameSender);
            senderObject.put("phone", smsData.mPhoneSender);
            senderObject.put("bank", smsData.mBankSender);

            smsObject.put("sender", senderObject);
            jsonObject.put("sms", smsObject);

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }
}
