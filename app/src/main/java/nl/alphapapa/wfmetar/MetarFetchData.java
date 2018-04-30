package nl.alphapapa.wfmetar;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.widget.TextView;

import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import java.io.IOException;

public class MetarFetchData extends WearableActivity {

    private TextView mTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_metar_fetch_data);

        mTextView = (TextView) findViewById(R.id.text);

        // Enables Always-on
        //setAmbientEnabled();
    }


    /*
    private DefaultHttpClient createHttpClient() {

        HttpParams my_httpParams = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(my_httpParams, 3000);
        SchemeRegistry registry = new SchemeRegistry();
        registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
        ThreadSafeClientConnManager multiThreadedConnectionManager = new ThreadSafeClientConnManager(my_httpParams, registry);
        DefaultHttpClient httpclient = new DefaultHttpClient(multiThreadedConnectionManager, my_httpParams);
        return httpclient;
    }

    private class DownloadUserInfoTask extends AsyncTask<Void, Void, Void> {
        String mResultString;
        Integer mStatusCode = 0;
        Exception mConnectionException;


        @Override
        protected Void doInBackground(Void... args) {
            String fetchUrl = "http://www.alphapapa.nl/metar/";

            DefaultHttpClient httpclient = createHttpClient();
            HttpGet httpget = new HttpGet(fetchUrl);

            try {
                HttpResponse response = httpclient.execute(httpget);
                StatusLine statusLine = response.getStatusLine();
                mStatusCode = statusLine.getStatusCode();

                if (mStatusCode == 200) {
                    mResultString = EntityUtils.toString(response.getEntity());
                }
            } catch (ClientProtocolException e) {
                e.printStackTrace();
                mConnectionException = e;
            } catch (IOException e) {
                e.printStackTrace();
                mConnectionException = e;
            }
            return null;
        }

        protected void onPostExecute(Void arg) {

            Integer iVelocity, iGusts, iCrossWindLimit = 0, iCross = 0;
        }
    }
    */
}
