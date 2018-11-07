package com.jwplayer.opensourcedemo;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.longtailvideo.jwplayer.JWPlayerView;
import com.longtailvideo.jwplayer.cast.CastManager;
import com.longtailvideo.jwplayer.configuration.PlayerConfig;
import com.longtailvideo.jwplayer.events.ControlBarVisibilityEvent;
import com.longtailvideo.jwplayer.events.FullscreenEvent;
import com.longtailvideo.jwplayer.events.listeners.VideoPlayerEvents;
import com.longtailvideo.jwplayer.media.playlists.PlaylistItem;

import java.util.ArrayList;
import java.util.List;

public class JWPlayerViewExample extends AppCompatActivity implements
		VideoPlayerEvents.OnFullscreenListener,
		VideoPlayerEvents.OnControlBarVisibilityListener{

	/**
	 * Reference to the {@link JWPlayerView}
	 */
	private JWPlayerView mPlayerView;

	/**
	 * An instance of our event handling class
	 */
	private JWEventHandler mEventHandler;

	/**
	 * Reference to the {@link CastManager}
	 */
	private CastManager mCastManager;

	/**
	 * Stored instance of CoordinatorLayout
	 * http://developer.android.com/reference/android/support/design/widget/CoordinatorLayout.html
	 */
	private CoordinatorLayout mCoordinatorLayout;

	private WebView webView;


	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_jwplayerview);

		mPlayerView = findViewById(R.id.jwplayer);
		TextView outputTextView = findViewById(R.id.output);
		ScrollView scrollView = findViewById(R.id.scroll);
		mCoordinatorLayout = findViewById(R.id.activity_jwplayerview);


		// Handle hiding/showing of ActionBar
		mPlayerView.addOnFullscreenListener(this);
		mPlayerView.addOnControlBarVisibilityListener(this);

		// Keep the screen on during playback
		new KeepScreenOnHandler(mPlayerView, getWindow());

		// Instantiate the JW Player event handler class
//		new JWEventHandler(mPlayerView, outputTextView, scrollView);

		// Setup JWPlayer
		setupJWPlayer();
		webviewListener();


		// Get a reference to the CastManager
		mCastManager = CastManager.getInstance();
	}

	/*Will not work in min SDK verison 16*/
	@SuppressLint({"AddJavascriptInterface", "SetJavaScriptEnabled", "JavascriptInterface" })
	private void webviewListener() {

		webView = new WebView(getWindow().getContext());
		webView.getSettings().setJavaScriptEnabled(true);
		webView.addJavascriptInterface(new MyQualitySettingsInterface(), "MyQualitySettingsInterface");
		webView.loadData("", "text/html", null);
		webView.loadUrl("javascript:MyQualitySettingsInterface.initializeQualitySettingsListener()");

	}

	public class MyQualitySettingsInterface {

		String element = "";
		boolean clicked = false;

		@JavascriptInterface
		public boolean isClicked() {
			return clicked;
		}

		@JavascriptInterface
		public String getElement() {
			return element;
		}

		@JavascriptInterface
		public void initializeQualitySettingsListener() {
			Log.i("HYUNJOO", "initializeQualitySettingsListener()");
			element = "window.document.getElementsByClassName('jw-icon jw-icon-inline jw-button-color jw-reset jw-icon-settings jw-settings-submenu-button')[0].addEventListener(\"click\",function(){" +
					"clicked = true;" +
					"});";
			Toast.makeText(JWPlayerViewExample.this,"initializeQualitySettingsListener", Toast.LENGTH_LONG).show();
		}

		@JavascriptInterface
		void print(){
			Toast.makeText(JWPlayerViewExample.this,"Quality Settings Touched!", Toast.LENGTH_LONG).show();
		}

	}

	@Override
	public void onControlBarVisibilityChanged(ControlBarVisibilityEvent controlBarVisibilityEvent) {

		String scriptToSeeElement = "javascript:(function(){ return MyQualitySettingsInterface.getElement(); })()"; // I know that the element is available

		String script = "javascript:(function(){ return MyQualitySettingsInterface.isClicked(); })()";

		// TODO: problem is the the click is not listened
		if(controlBarVisibilityEvent.isVisible()){
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
				webView.evaluateJavascript(script, new ValueCallback<String>() {
					@Override
					public void onReceiveValue(String value) {
						Log.i("HYUNJOO","onControlBar - OnReceived value: " + value);
						if(value.equals("true")) Toast.makeText(JWPlayerViewExample.this, value, Toast.LENGTH_SHORT).show();
					}
				});
			}
		}
	}

	private void setupJWPlayer() {
		List<PlaylistItem> playlistItemList = createPlaylist();


		mPlayerView.setup(new PlayerConfig.Builder()
					.playlist(playlistItemList)
					.autostart(true)
					.preload(true)
					.build()
				);
	}

	private List<PlaylistItem> createPlaylist() {
		List<PlaylistItem> playlistItemList = new ArrayList<>();

		String[] playlist = {
				"https://cdn.jwplayer.com/manifests/jumBvHdL.m3u8",
				"http://playertest.longtailvideo.com/adaptive/bipbop/gear4/prog_index.m3u8"
				};

		for(String each : playlist){
			playlistItemList.add(new PlaylistItem(each));
		}

		return playlistItemList;
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		// Set fullscreen when the device is rotated to landscape
		mPlayerView.setFullscreen(newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE, true);
		super.onConfigurationChanged(newConfig);
	}

	@Override
	protected void onResume() {
		// Let JW Player know that the app has returned from the background
		super.onResume();
		mPlayerView.onResume();
	}

	@Override
	protected void onPause() {
		// Let JW Player know that the app is going to the background
		mPlayerView.onPause();
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		// Let JW Player know that the app is being destroyed
		mPlayerView.onDestroy();
		super.onDestroy();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// Exit fullscreen when the user pressed the Back button
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (mPlayerView.getFullscreen()) {
				mPlayerView.setFullscreen(false, true);
				return false;
			}
		}
		return super.onKeyDown(keyCode, event);
	}

	/**
	 * Handles JW Player going to and returning from fullscreen by hiding the ActionBar
	 *
	 * @param fullscreenEvent true if the player is fullscreen
	 */
	@Override
	public void onFullscreen(FullscreenEvent fullscreenEvent) {
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			if (fullscreenEvent.getFullscreen()) {
				actionBar.hide();
			} else {
				actionBar.show();
			}
		}

		// When going to Fullscreen we want to set fitsSystemWindows="false"
		mCoordinatorLayout.setFitsSystemWindows(!fullscreenEvent.getFullscreen());
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_jwplayerview, menu);
		// Register the MediaRouterButton on the JW Player SDK
		mCastManager.addMediaRouterButton(menu, R.id.media_route_menu_item);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.switch_to_fragment:
				Intent i = new Intent(this, JWPlayerFragmentExample.class);
				startActivity(i);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}
}
