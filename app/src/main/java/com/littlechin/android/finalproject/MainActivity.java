package com.littlechin.android.finalproject;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.renderscript.ScriptGroup;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    String resString = "";
    boolean trackOn = false;
    boolean getPrice = false;
    boolean getName = false;
    String stock = "";
    double resStockPrice = 0;
    String resStockName = "";
    int reqTurn = 100;
    boolean judgeBig = true;
    boolean judgeBigTmp = true;
    double judgeNum = 0;
    boolean judgeOddLot = false;
    boolean judgeOddLotTmp = false;

    String APIKEY = "{{ Your Fugle API Key }}";
    // Fugle API KEY

    OkHttpClient client = new OkHttpClient().newBuilder().build();

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView stockPriceShow = findViewById(R.id.stockPriceShow);
        TextView stockPriceTitle = findViewById(R.id.stockPriceTitle);
        Button trackBtn = findViewById(R.id.trackBtn);
        ProgressBar bar = findViewById(R.id.bar);
        bar.setMax(100);
        bar.setMin(0);

//        重複執行確認是否要進行 UI 更正與呼叫價格查詢
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    runOnUiThread(() -> {
                        if (trackOn) {
                            if (!getPrice) {
                                stockPriceShow.setText(String.valueOf(resStockPrice));
                            }

                            if (!getName) {
                                if (judgeOddLot) {
                                    stockPriceTitle.setText(stock + " " + resStockName + " 零股現價");
                                }
                                else {
                                    stockPriceTitle.setText(stock + " " + resStockName + " 整股現價");
                                }
                            }

                            bar.setProgress(reqTurn);
                            Log.d("bar", String.valueOf(reqTurn));
                            if (reqTurn < 0){
                                getPrice(stock, judgeOddLot);
                                if (!getPrice) {
                                    if (judgeBig) {
                                        if (resStockPrice >= judgeNum) {
                                            trackOn = false;
                                        }
                                    }
                                    else {
                                        if (resStockPrice <= judgeNum) {
                                            trackOn = false;
                                        }
                                    }
                                    if (!trackOn) {
                                        trackBtn.setText("Track");
                                        notificationShow("😍到價啦~", "追蹤的 " + stock  + " " + resStockName + " 價格已經來到 " + String.valueOf(resStockPrice) + "！", 10);
                                        notificationShow("追蹤狀態遭到改變", "追蹤器符合停止規則，已停止追蹤 " + stock, 20);
                                    }
                                    reqTurn = 100;
                                }
                            }
                            if (!getPrice) {
                                reqTurn -= 20;
                            }
                        }
                    });
                }
            }
        }).start();
    }

//    查價 funcition
    public void getPrice(String symbolId, boolean oddLot){
        getPrice = true;
        resString = "";
        String URL = "https://api.fugle.tw/realtime/v0.2/intraday/dealts?apiToken=" + APIKEY + "&limit=1&oddLot=";
        if (oddLot) {
            URL += "true&symbolId=" + String.valueOf(symbolId);
        }
        else {
            URL += "false&symbolId=" + String.valueOf(symbolId);
        }
        Log.d("OkHttp URL", URL);
        Request request = new Request.Builder()
                .url(URL)
                .build();

        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String result = response.body().string();
                Log.d("OkHttp result", result);
                resString = result;
                Log.d("httpFlagProcess", String.valueOf(resString));
                JSONObject j = null;
                JSONArray ja = null;
                try {
                    j = new JSONObject(resString);
                    ja = j.getJSONObject("data").getJSONArray("dealts");
                    resStockPrice = ja.getJSONObject(0).getDouble("price");
                    reqTurn = 100;
                }catch (JSONException err){
                    Log.d("Error", err.toString());
                }
            }
            @Override
            public void onFailure(Call call, IOException e) {
            }
        });
        getPrice = false;
    }

//    查股票名稱 function
    public void getName(String symbolId){
        getName = true;
        resString = "";
        String URL = "https://api.fugle.tw/realtime/v0.2/intraday/meta?apiToken=" + APIKEY + "&symbolId=" + String.valueOf(symbolId);
        Log.d("OkHttp URL", URL);
        Request request = new Request.Builder()
                .url(URL)
                .build();

        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String result = response.body().string();
                Log.d("OkHttp result", result);
                resString = result;
                Log.d("httpFlagProcess", String.valueOf(resString));
                JSONObject j = null;
                try {
                    j = new JSONObject(resString);
                    resStockName = j.getJSONObject("data").getJSONObject("meta").getString("nameZhTw");
                }catch (JSONException err){
                    Log.d("Error", err.toString());
                }
            }
            @Override
            public void onFailure(Call call, IOException e) {
            }
        });
        getName = false;
    }

//    發出通知
    public void notificationShow(String title, String text, int tunnel){
        String id = "my_channel_01";
        CharSequence name = "Default";
        String description = "Default";

        Context context = getApplicationContext();

        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent= PendingIntent.getActivity(context, 0, intent, 0);
        long[] vibratepattern = {100, 400, 500, 400};

        //START SETTING CHANNEL ID
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationManager ntfMgr = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        Notification.Builder builder = new Notification.Builder(context)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(pendingIntent)
                .setAutoCancel(false)
                .setChannelId(id);

        NotificationChannel mChannel = new NotificationChannel(id, name, NotificationManager.IMPORTANCE_HIGH);

        mChannel.setDescription(description);
        mChannel.enableLights(true);
        mChannel.setLightColor(Color.RED);
        mChannel.enableVibration(true);
        mChannel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
        ntfMgr.createNotificationChannel(mChannel);

        ntfMgr.notify(tunnel, builder.build());
    }

    public void bigOnClick(View view) {
        RadioButton radioBig = findViewById(R.id.radioBig);
        RadioButton radioSmall = findViewById(R.id.radioSmall);
        radioBig.setChecked(true);
        radioSmall.setChecked(false);
        judgeBigTmp = true;
    }

    public void smallOnClick(View view) {
        RadioButton radioBig = findViewById(R.id.radioBig);
        RadioButton radioSmall = findViewById(R.id.radioSmall);
        radioBig.setChecked(false);
        radioSmall.setChecked(true);
        judgeBigTmp = false;
    }

    public void oddLotOnClick(View view) {
        Switch oddLotSwitch = findViewById(R.id.oddLotSwitch);
        judgeOddLotTmp = !judgeOddLotTmp;
        oddLotSwitch.setChecked(judgeOddLotTmp);
    }

    public void trackOnClick(View view) {
        EditText stockId = findViewById(R.id.stockIdInput);
        EditText setPrice = findViewById(R.id.setPriceInput);
        TextView stockPriceShow = findViewById(R.id.stockPriceShow);
        TextView stockPriceTitle = findViewById(R.id.stockPriceTitle);
        Button trackBtn = findViewById(R.id.trackBtn);
        EditText setPriceInput = findViewById(R.id.setPriceInput);

        if (!trackOn) {
            if (stockId.getText().toString().matches("") || setPrice.getText().toString().matches("")) {
                Toast.makeText(MainActivity.this, "請填寫股票代碼與設定價格😭", Toast.LENGTH_SHORT).show();
            } else {
                resStockPrice = 0;
                stock = stockId.getText().toString();
                getPrice(stock, judgeOddLot);
                getName(stock);
                trackOn = true;
                judgeOddLot = judgeOddLotTmp;
                if (judgeOddLot) {
                    stockPriceTitle.setText(stock + " 零股現價");
                }
                else {
                    stockPriceTitle.setText(stock + " 整股現價");
                }

                trackBtn.setText("Stop Track");
                stockPriceShow.setText("N/A");
                reqTurn = 100;

                judgeNum = Double.parseDouble(setPriceInput.getText().toString().trim());
                String notiMes = "開始追蹤 " + stock + " 價格";
                judgeBig = judgeBigTmp;
                if (judgeBig) {
                    notiMes += "大於 " + String.valueOf(judgeNum);
                }
                else {
                    notiMes += "小於 " + String.valueOf(judgeNum);
                }
                notificationShow("追蹤狀態遭到改變", notiMes, 20);
            }
        }
        else {
            trackOn = false;
            trackBtn.setText("Track");
            reqTurn = 0;
            notificationShow("追蹤狀態遭到改變", "停止追蹤 " + stock, 20);
        }
    }
}