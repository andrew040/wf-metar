package nl.alphapapa.wfmetar;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.widget.TextView;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import android.support.wearable.complications.ComplicationData;
import android.support.wearable.complications.ComplicationManager;
import android.support.wearable.complications.ComplicationProviderService;
import android.support.wearable.complications.ComplicationText;



public class MetarFetchData extends WearableActivity {

    private TextView mAppHeader;
    private TextView mMetarText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_metar_fetch_data);

        mAppHeader = (TextView) findViewById(R.id.text);
        mMetarText = (TextView) findViewById(R.id.textView);

        // Enables Always-on
       //setAmbientEnabled();

        //om de zoveel tijd
        new GetUrlContentTask().execute("http://www.alphapapa.nl/metar/");
    }

    private class GetUrlContentTask extends AsyncTask<String, Integer, String> {
         protected String doInBackground(String... urls) {
            try{
                URL url = new URL(urls[0]);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setDoOutput(true);
                connection.setConnectTimeout(1000);
                connection.setReadTimeout(1000);
                connection.connect();
                BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String content = "", line;
                while ((line = rd.readLine()) != null) {
                    content += line + "\n";

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
