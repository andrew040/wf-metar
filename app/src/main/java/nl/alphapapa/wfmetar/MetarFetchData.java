package nl.alphapapa.wfmetar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.TimeZone;

import android.support.wearable.complications.ComplicationData;
import android.support.wearable.complications.ComplicationManager;
import android.support.wearable.complications.ComplicationProviderService;
import android.support.wearable.complications.ComplicationText;

public class MetarFetchData extends WearableActivity {
    private TextView mAppHeader;
    private TextView mMetarText;
    private static boolean bAlreadyLoaded = false;
    private static String mMetarTime = "000000Z";
    private static final int mDelay = 1000;
    Handler handler = new Handler();


    private AlarmManager alarmMgr;
    private PendingIntent alarmIntent;

    public class AlarmIntentReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            ////////Not working yet
            intent = new Intent(context, AlarmIntentReceiver.class);
            alarmIntent = PendingIntent.getBroadcast(context, 0, intent, 0);

            Log.e("MetarWatchface", "Alarm triggered ");
        }
    }

    private void DoEverySecond(){
        handler.postDelayed(new Runnable(){
            public void run() {
                String mHours="", mMinutes="", mDay="";
                long lTimeDifference;

                try {
                    mDay = mMetarTime.substring(0, 2);
                    mHours = mMetarTime.substring(2, 4);
                    mMinutes = mMetarTime.substring(4, 6);
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
                Calendar MetarTime = Calendar.getInstance();
                Calendar CurrentTime = Calendar.getInstance();

                CurrentTime.setTimeZone(TimeZone.getTimeZone("UTC"));
                MetarTime.setTimeZone(TimeZone.getTimeZone("UTC"));
                MetarTime.set(Calendar.DAY_OF_MONTH, Integer.valueOf(mDay));
                MetarTime.set(Calendar.HOUR_OF_DAY, Integer.valueOf(mHours));
                MetarTime.set(Calendar.MINUTE, Integer.valueOf(mMinutes));
                MetarTime.set(Calendar.SECOND, 0);

                lTimeDifference = CurrentTime.getTimeInMillis() - MetarTime.getTimeInMillis();
                lTimeDifference = lTimeDifference / 1000;

                if (bAlreadyLoaded) {
                    if (lTimeDifference < 10) {                                                 // Less than 10 seconds ago
                        mAppHeader.setText("Just now");
                    } else {
                        if (lTimeDifference < 60) {                                             // Less than a minute ago
                            mAppHeader.setText(lTimeDifference + " seconds ago");
                        } else {
                            if (lTimeDifference < 120) {                                        // Between 60 and 120 seconds ago
                                mAppHeader.setText("A minute ago");
                            } else {
                                if (lTimeDifference < 3600) {                                   // Between two and 60 minutes ago
                                    mAppHeader.setText(lTimeDifference / 60 + " minutes ago");
                                } else {
                                    mAppHeader.setText(lTimeDifference / 3600 + " hours ago");
                                }
                            }
                        }
                    }
                    handler.postDelayed(this, mDelay);
               } else {
                   mAppHeader.setText("");
               }
            }
        }, mDelay);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_metar_fetch_data);

        mAppHeader = findViewById(R.id.textViewTime);
        mMetarText = findViewById(R.id.textViewMetar);

        bAlreadyLoaded = false;

        //Not working yet:
        alarmMgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 50);
        alarmMgr.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),6000, alarmIntent);
        Log.v("MetarWatchface", "Alarm set.");
        ///////////////////


        DoEverySecond();

        new GetUrlContentTask().execute("http://www.alphapapa.nl/metar/");
    }

    public void onClickBtn(View view)
    {
        new GetUrlContentTask().execute("http://www.alphapapa.nl/metar/");
    }

    private class GetUrlContentTask extends AsyncTask<String, Integer, String> {
         protected String doInBackground(String... urls) {
            try {
                URL url = new URL(urls[0]);
                int mStatusCode = 0;
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setDoOutput(true);
                connection.setConnectTimeout(1000);
                connection.setReadTimeout(1000);
                connection.connect();
                mStatusCode =  connection.getResponseCode();
                BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String content = "", line;
                Log.v("MetarWatchface", "HTTP response: " + mStatusCode);

                if(mStatusCode == 200){
                    while ((line = rd.readLine()) != null) {
                        content += line + "\n";
                        bAlreadyLoaded = true;
                    }
                } else {
                    content = "Load failed..";
                }
                return content;
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
         }

         protected void onProgressUpdate(Integer... progress) {
         }

         protected void onPostExecute(String result) {
             try {
                 String[] message = result.split(";");
                 String[] metar = message[0].split(",");
                 String[] index = message[1].split(",");
                 String mWind = "", mTempDew = "", mUDP = "", mQNH = "", mMETARSummary = "";
                 int i = 0;
                 boolean bEndOfMessage = false;

                 for (String data : index) {
                     if (data.equals("time") && !bEndOfMessage) {
                         mMetarTime = metar[i];
                     }
                     if (data.equals("wind") && !bEndOfMessage) {
                         mWind = metar[i];
                     }
                     if (data.equals("tempdew") && !bEndOfMessage) {
                         mTempDew = metar[i];
                     }
                     if (data.equals("udp") && !bEndOfMessage) {
                         mUDP = metar[i];
                     }
                     if (data.equals("qnh") && !bEndOfMessage) {
                         mQNH = metar[i];
                         bEndOfMessage = true;
                     }
                     i++;
                 }
                 mMETARSummary = mWind + " " + mTempDew + " " + mQNH;
                 Log.v("MetarWatchface", "Got this: " + mMETARSummary);
                 mMetarText.setText(message[2]);
             }catch(NullPointerException e){
                 Log.e("MetarWatchface", "Failed: " + e);
                 mMetarText.setText("Failed to load METAR :(");
             }
         }
    }


    // Not working yet
    public class LongTextProviderService extends ComplicationProviderService {
        @Override
        public void onComplicationUpdate(int complicationId, int type, ComplicationManager manager) {
            if (type != ComplicationData.TYPE_LONG_TEXT) {
                manager.noUpdateRequired(complicationId);
                return;
            }

            ComponentName thisProvider = new ComponentName(this, getClass());
            PendingIntent complicationTogglePendingIntent = ComplicationToggleReceiver.getToggleIntent(this, thisProvider, complicationId);

            ComplicationData data = null;
            new ComplicationData.Builder(type)
                    .setLongText(
                            ComplicationText.plainText("De lange text"))
                    .setLongTitle(
                            ComplicationText.plainText("Titel"))
                    .setTapAction(complicationTogglePendingIntent)
                    .build();
            manager.updateComplicationData(complicationId, data);
        }
    }
 }