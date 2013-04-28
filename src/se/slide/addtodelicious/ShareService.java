
package se.slide.addtodelicious;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLEncoder;

import javax.net.ssl.HttpsURLConnection;

public class ShareService extends Service {

    private static final String TAG = "ShareService";

    public final static int NOTIFICATION_ID = 1;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);

        boolean bPrivate = pref.getBoolean("private", false);

        String mark_private = "0";
        if (bPrivate)
            mark_private = "1";

        String username = pref.getString("username", null);
        String password = pref.getString("password", null);
        String tag = pref.getString("tag", "interesting");
        String url = intent.getStringExtra(Intent.EXTRA_TEXT);
        String title = intent.getStringExtra(Intent.EXTRA_SUBJECT);

        Delicious delicious = new Delicious();
        delicious.url = url;
        delicious.title = title;
        delicious.tag = tag;
        delicious.mark_private = mark_private;
        
        if (username == null || password == null) {
            showNotification(delicious);
        }
        else {

            PostDelicious post = new PostDelicious(this);
            post.execute(url, title, tag, mark_private, username, password);
            
        }

        return super.onStartCommand(intent, flags, startId);
    }

    public void showNotification(Delicious delicious) {
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setAutoCancel(true)
                        .setContentTitle(getString(R.string.notification_error_title))
                        .setContentText(getString(R.string.notification_error_userpass));
                        //.setContentText(getString(R.string.notification_error, delicious.title));

        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }

    private class PostDelicious extends AsyncTask<String, Void, Delicious> {

        private WeakReference<Service> weakService;

        public PostDelicious(Service service) {
            weakService = new WeakReference<Service>(service);
        }

        @Override
        protected Delicious doInBackground(String... params) {

            String url = params[0];
            String title = params[1];
            String tag = params[2];
            String mark_private = params[3];
            final String username = params[4];
            final String password = params[5];

            Delicious delicious = new Delicious();
            delicious.url = url;
            delicious.title = title;
            delicious.tag = tag;
            delicious.mark_private = mark_private;
            delicious.status = Delicious.STATUS_OK;

            Authenticator.setDefault(new Authenticator() {
                public PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password.toCharArray());
                }
            });

            URL endpoint;
            HttpsURLConnection conn = null;
            try {

                if (mark_private.equalsIgnoreCase("0"))
                    endpoint = new URL("https://api.del.icio.us/v1/posts/add?url="
                            + URLEncoder.encode(url, "UTF-8") + "&description="
                            + URLEncoder.encode(title, "UTF-8") + "&tags="
                            + URLEncoder.encode(tag, "UTF-8"));
                else
                    endpoint = new URL("https://api.del.icio.us/v1/posts/add?url="
                            + URLEncoder.encode(url, "UTF-8") + "&description="
                            + URLEncoder.encode(title, "UTF-8") + "&tags="
                            + URLEncoder.encode(tag, "UTF-8") + "&shared=no");

                // http://developer.android.com/training/id-auth/authenticate.html

                conn = (HttpsURLConnection) endpoint.openConnection();
                conn.setRequestProperty("Connection", "close");
                conn.setRequestProperty("User-Agent", "Add to Delicious/Android app");
                conn.setReadTimeout(9000);
                conn.setConnectTimeout(9000);

                InputStream input = conn.getInputStream();
                
                BufferedReader reader;
                reader = new BufferedReader(new InputStreamReader(input));

                String line;
                while ((line = reader.readLine()) != null) {
                    Log.d(TAG, line);
                }

                reader.close();
            } catch (Exception e) {
                delicious.status = Delicious.STATUS_ERROR;
                e.printStackTrace();
            } finally {
                if (conn != null)
                    conn.disconnect();
            }

            return delicious;
        }

        @Override
        protected void onPostExecute(Delicious result) {
            super.onPostExecute(result);

            Service service = weakService.get();
            if (service != null && result != null) {

                if (result.status == Delicious.STATUS_ERROR) {
                    ((ShareService) service).showNotification(result);
                }
                else {
                    Toast.makeText(service, "Added to Delicious", Toast.LENGTH_SHORT).show();
                }

            }
        }

    }

    private class Delicious {
        public final static int STATUS_OK = 200;
        public final static int STATUS_ERROR = 500;

        public String url;
        public String title;
        public String tag;
        public String mark_private;
        public int status;
    }

}
