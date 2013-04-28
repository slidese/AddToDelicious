
package se.slide.addtodelicious;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get intent, action and MIME type
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if ("text/plain".equals(type)) {
                
                String url = intent.getStringExtra(Intent.EXTRA_TEXT);
                String title = intent.getStringExtra(Intent.EXTRA_SUBJECT);
                
                Intent i = new Intent(getApplicationContext(), ShareService.class);
                i.putExtra(Intent.EXTRA_TEXT, url);
                i.putExtra(Intent.EXTRA_SUBJECT, title);
                
                startService(i);
                
            }
        }

        
        finish();
    }

    
}
