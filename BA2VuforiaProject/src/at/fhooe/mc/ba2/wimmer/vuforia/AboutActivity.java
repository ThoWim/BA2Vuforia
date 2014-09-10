package at.fhooe.mc.ba2.wimmer.vuforia;

import android.app.Activity;
import android.os.Bundle;
import android.view.MenuItem;
import android.webkit.WebView;

public class AboutActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.about);
		getActionBar().setDisplayHomeAsUpEnabled(true);

		WebView webView = (WebView) findViewById(R.id.webViewAbout);
		webView.loadUrl("file:///android_asset/about.html");
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	        case android.R.id.home:
	            this.finish();
	            return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}
}
