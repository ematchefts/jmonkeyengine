package com.jme3.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import com.jme3.input.android.AndroidInput;
import com.jme3.input.controls.TouchListener;
import com.jme3.input.event.TouchEvent;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeSystem;
import com.jme3.system.android.AndroidConfigChooser.ConfigType;
import com.jme3.system.android.JmeAndroidSystem;
import com.jme3.system.android.OGLESContext;
import com.jme3.util.JmeFormatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <code>AndroidHarness</code> wraps a jme application object and runs it on Android
 * @author Kirill
 * @author larynx
 */
public class AndroidHarness extends Activity implements TouchListener, DialogInterface.OnClickListener {

    protected final static Logger logger = Logger.getLogger(AndroidHarness.class.getName());
    /**
     * The application class to start
     */
    protected String appClass = "jme3test.android.Test";
    /**
     * The jme3 application object
     */
    protected Application app = null;
    /**
     * ConfigType.FASTEST is RGB565, GLSurfaceView default
     * ConfigType.BEST is RGBA8888 or better if supported by the hardware
     */
    protected ConfigType eglConfigType = ConfigType.FASTEST;
    /**
     * If true all valid and not valid egl configs are logged
     */
    protected boolean eglConfigVerboseLogging = false;
    /**
     * If true MouseEvents are generated from TouchEvents
     */
    protected boolean mouseEventsEnabled = true;
    /**
     * Flip X axis
     */
    protected boolean mouseEventsInvertX = true;
    /**
     * Flip Y axis
     */
    protected boolean mouseEventsInvertY = true;
    /**
     * Title of the exit dialog, default is "Do you want to exit?"
     */
    protected String exitDialogTitle = "Do you want to exit?";
    /**
     * Message of the exit dialog, default is "Use your home key to bring this app into the background or exit to terminate it."
     */
    protected String exitDialogMessage = "Use your home key to bring this app into the background or exit to terminate it.";
    
    /**
     * Set the screen window size
     * if screenFullSize is true, then the notification bar and title bar are
     *   removed and the screen covers the entire display
     * if screenFullSize is false, then the notification bar remains visible
     *   if screenShowTitle is true while screenFullScreen is false, then the
     *     title bar is also displayed under the notification bar
     */
    protected boolean screenFullScreen = true;
    
    /**
     * if screenShowTitle is true while screenFullScreen is false, then the
     *     title bar is also displayed under the notification bar
     */
    protected boolean screenShowTitle = true;
    
    /**
     * Splash Screen picture Resource ID.  If a Splash Screen is desired, set
     * splashPicID to the value of the Resource ID (i.e. R.drawable.picname).
     * If splashPicID = 0, then no splash screen will be displayed.
     */
    protected int splashPicID = 0;

    /**
     * Set the screen orientation, default is SENSOR
     * ActivityInfo.SCREEN_ORIENTATION_* constants
     * package android.content.pm.ActivityInfo
     *  
     *   SCREEN_ORIENTATION_UNSPECIFIED
     *   SCREEN_ORIENTATION_LANDSCAPE
     *   SCREEN_ORIENTATION_PORTRAIT
     *   SCREEN_ORIENTATION_USER
     *   SCREEN_ORIENTATION_BEHIND
     *   SCREEN_ORIENTATION_SENSOR (default)
     *   SCREEN_ORIENTATION_NOSENSOR
     */
    protected int screenOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR;
    protected OGLESContext ctx;
    protected GLSurfaceView view = null;
    protected boolean isGLThreadPaused = true;
    private ImageView splashImageView = null;   
    private FrameLayout frameLayout = null;
    final private String ESCAPE_EVENT = "TouchEscape";

    static {
        try {
            System.loadLibrary("bulletjme");
        } catch (UnsatisfiedLinkError e) {
        }
        JmeSystem.setSystemDelegate(new JmeAndroidSystem());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Logger log = logger;
        boolean bIsLogFormatSet = false;
        do {
            if (log.getHandlers().length == 0) {
                log = logger.getParent();
                if (log != null) {
                    for (Handler h : log.getHandlers()) {
                        //h.setFormatter(new SimpleFormatter());
                        h.setFormatter(new JmeFormatter());
                        bIsLogFormatSet = true;
                    }
                }
            }
        } while (log != null && !bIsLogFormatSet);

        JmeAndroidSystem.setResources(getResources());
        JmeAndroidSystem.setActivity(this);

        if (screenFullScreen) {
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else {
            if (!screenShowTitle) {
                requestWindowFeature(Window.FEATURE_NO_TITLE);
            }
        }

        setRequestedOrientation(screenOrientation);

        // Create Settings
        AppSettings settings = new AppSettings(true);

        // Create the input class
        AndroidInput input = new AndroidInput(this);
        input.setMouseEventsInvertX(mouseEventsInvertX);
        input.setMouseEventsInvertY(mouseEventsInvertY);
        input.setMouseEventsEnabled(mouseEventsEnabled);

        // Create application instance
        try {
            if (app == null) {
                @SuppressWarnings("unchecked")
                Class<? extends Application> clazz = (Class<? extends Application>) Class.forName(appClass);
                app = clazz.newInstance();
            }

            app.setSettings(settings);
            app.start();
            ctx = (OGLESContext) app.getContext();
            view = ctx.createView(input, eglConfigType, eglConfigVerboseLogging);

            // Set the screen reolution
            WindowManager wind = this.getWindowManager();
            Display disp = wind.getDefaultDisplay();
            ctx.getSettings().setResolution(disp.getWidth(), disp.getHeight());

            AppSettings s = ctx.getSettings();
            logger.log(Level.INFO, "Settings: Width {0} Height {1}", new Object[]{s.getWidth(), s.getHeight()});

            layoutDisplay();

        } catch (Exception ex) {
            handleError("Class " + appClass + " init failed", ex);
            setContentView(new TextView(this));
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if (app != null) {
            app.restart();
        }
        logger.info("onRestart");
    }

    @Override
    protected void onStart() {
        super.onStart();
        logger.info("onStart");
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (view != null) {
            view.onResume();
        }
        isGLThreadPaused = false;
        logger.info("onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (view != null) {
            view.onPause();
        }
        isGLThreadPaused = true;
        logger.info("onPause");
    }

    @Override
    protected void onStop() {
        super.onStop();
        logger.info("onStop");
    }

    @Override
    protected void onDestroy() {
        if (app != null) {
            app.stop(!isGLThreadPaused);
        }
        super.onDestroy();
        logger.info("onDestroy");
    }

    public Application getJmeApplication() {
        return app;
    }

    /**
     * Called when an error has occured. This is typically
     * invoked when an uncaught exception is thrown in the render thread.
     * @param errorMsg The error message, if any, or null.
     * @param t Throwable object, or null.
     */
    public void handleError(final String errorMsg, final Throwable t) {
        String sTrace = "";
        if (t != null && t.getStackTrace() != null) {
            for (StackTraceElement ste : t.getStackTrace()) {
                sTrace += "\tat " + ste.getClassName() + "." + ste.getMethodName() + "(";
                if (ste.isNativeMethod()){
                    sTrace += "Native";
                }else{
                    sTrace += ste.getLineNumber();
                }
                sTrace +=  ")\n";
            }
        }

        final String stackTrace = sTrace;

        logger.log(Level.SEVERE, t != null ? t.toString() : "OpenGL Exception");
        logger.log(Level.SEVERE, "{0}{1}", new Object[]{errorMsg != null ? errorMsg + ": " : "", stackTrace});

        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog dialog = new AlertDialog.Builder(AndroidHarness.this) // .setIcon(R.drawable.alert_dialog_icon)
                        .setTitle(t != null ? (t.getMessage() != null ? (t.getMessage() + ": " + t.getClass().getName()) : t.getClass().getName()) : "OpenGL Exception").setPositiveButton("Kill", AndroidHarness.this).setMessage((errorMsg != null ? errorMsg + ": " : "") + stackTrace).create();
                dialog.show();
            }
        });

    }

    /**
     * Called by the android alert dialog, terminate the activity and OpenGL rendering
     * @param dialog
     * @param whichButton
     */
    public void onClick(DialogInterface dialog, int whichButton) {
        if (whichButton != -2) {
            if (app != null) {
                app.stop(true);
            }
            this.finish();
        }
    }

    /**
     * Gets called by the InputManager on all touch/drag/scale events
     */
    @Override
    public void onTouch(String name, TouchEvent evt, float tpf) {
        if (name.equals(ESCAPE_EVENT)) {
            switch (evt.getType()) {
                case KEY_UP:
                    this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            AlertDialog dialog = new AlertDialog.Builder(AndroidHarness.this) // .setIcon(R.drawable.alert_dialog_icon)
                                    .setTitle(exitDialogTitle).setPositiveButton("Yes", AndroidHarness.this).setNegativeButton("No", AndroidHarness.this).setMessage(exitDialogMessage).create();
                            dialog.show();
                        }
                    });

                    break;
                default:
                    break;
            }
        }

    }

    public void layoutDisplay() {
            logger.log(Level.INFO, "Splash Screen Picture Resource ID: {0}", splashPicID);
            if (splashPicID != 0) {
                FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                        LayoutParams.FILL_PARENT,
                        LayoutParams.FILL_PARENT,
                        Gravity.CENTER
                        );

                frameLayout = new FrameLayout(this);

                splashImageView = new ImageView(this);

                Drawable drawable = this.getResources().getDrawable(splashPicID);
                if (drawable instanceof NinePatchDrawable) {
                    splashImageView.setBackgroundDrawable(drawable);
                } else {
                    splashImageView.setImageResource(splashPicID);
}

                frameLayout.addView(view);
                frameLayout.addView(splashImageView, lp);

                setContentView(frameLayout);
                logger.log(Level.INFO, "Splash Screen Created");
            } else {
                logger.log(Level.INFO, "Splash Screen Skipped.");
                setContentView(view);
            }

    }

    public void removeSplashScreen() {
        logger.log(Level.INFO, "Splash Screen Picture Resource ID: {0}", splashPicID);
        if (splashPicID != 0) {
            if (frameLayout != null) {
                if (splashImageView != null) {
                    this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            splashImageView.setVisibility(View.INVISIBLE);
                            frameLayout.removeView(splashImageView);
                        }
                    });
                } else {
                    logger.log(Level.INFO, "splashImageView is null");
                }
            } else {
                logger.log(Level.INFO, "frameLayout is null");
            }
        }
    }
}
