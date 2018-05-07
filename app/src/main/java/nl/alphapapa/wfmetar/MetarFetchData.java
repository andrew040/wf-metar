package nl.alphapapa.wfmetar;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
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
    private static String mMetarTime = "000000Z";
    private static final int mDelay = 1000;
    Handler handler = new Handler();

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

                if (lTimeDifference < 10) {
                    mAppHeader.setText("Just now");
                } else {
                    if (lTimeDifference < 60) {
                        mAppHeader.setText(lTimeDifference + " seconds ago");
                    } else {
                        if (lTimeDifference < 120) {
                            mAppHeader.setText("A minute ago");
                        } else {
                            mAppHeader.setText(lTimeDifference / 60 + " minutes ago");
                        }
                    }
                }
                handler.postDelayed(this, mDelay);
            }
        }, mDelay);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_metar_fetch_data);

        mAppHeader = (TextView) findViewById(R.id.textViewTime);
        mMetarText = (TextView) findViewById(R.id.textViewMetar);

        DoEverySecond();
        // Enables Always-on
        // setAmbientEnabled();
        //om de zoveel tijd
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
