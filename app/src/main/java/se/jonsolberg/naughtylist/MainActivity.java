package se.jonsolberg.naughtylist;

import android.app.Activity;
import android.os.Bundle;
import android.os.Process;
import android.view.View;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * The whole game is assets/index.html (copied from ../index.html at build time).
 * This activity shows it full screen in landscape and lets the game's Quit button close the app.
 */
public class MainActivity extends Activity {

    private WebView web;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        web = new WebView(this);
        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);                 // autosave and the three save slots
        settings.setMediaPlaybackRequiresUserGesture(false); // the chiptune music
        settings.setTextZoom(100);                           // keep the pixel layout as designed

        // The game calls window.NLC.quit() from its Quit button.
        web.addJavascriptInterface(new GameBridge(), "NLC");
        // Older builds of the game asked to quit by opening "app:quit"; still honoured.
        web.setWebViewClient(new WebViewClient() {
            @Override
            @SuppressWarnings("deprecation")
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url != null && url.startsWith("app:quit")) {
                    quitGame();
                    return true;
                }
                return false;
            }
        });

        setContentView(web);
        // "#app" switches the game to its full-screen touch layout.
        web.loadUrl("file:///android_asset/index.html#app");
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideSystemBars();
    }

    @SuppressWarnings("deprecation")
    private void hideSystemBars() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }

    /** Close the game for real, so no music or timers keep running in the background. */
    private void quitGame() {
        finishAndRemoveTask();
        Process.killProcess(Process.myPid());
    }

    private class GameBridge {
        @JavascriptInterface
        public void quit() {
            runOnUiThread(MainActivity.this::quitGame);
        }
    }
}
