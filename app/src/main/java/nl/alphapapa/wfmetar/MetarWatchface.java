package nl.alphapapa.wfmetar;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.support.wearable.complications.ComplicationData;
import android.support.wearable.complications.rendering.ComplicationDrawable;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.WindowInsets;
import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Digital watch face with seconds. In ambient mode, the seconds aren't displayed. On devices with
 * low-bit ambient mode, the text is drawn without anti-aliasing in ambient mode.
 * <p>
 * Important Note: Because watch face apps do not have a default Activity in
 * their project, you will need to set your Configurations to
 * "Do not launch Activity" for both the Wear and/or Application modules. If you
 * are unsure how to do this, please review the "Run Starter project" section
 * in the Google Watch Face Code Lab:
 * https://codelabs.developers.google.com/codelabs/watchface/index.html#0
 */




public class MetarWatchface extends CanvasWatchFaceService {
    private static final Typeface NORMAL_TYPEFACE =
            Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);


    //Update rate for interactive mode
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

    //Handler id
    private static final int MSG_UPDATE_TIME = 0;

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private static class EngineHandler extends Handler {
        private final WeakReference<MetarWatchface.Engine> mWeakReference;

        public EngineHandler(MetarWatchface.Engine reference) {
            mWeakReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            MetarWatchface.Engine engine = mWeakReference.get();
            if (engine != null) {
                switch (msg.what) {
                    case MSG_UPDATE_TIME:
                        engine.handleUpdateTimeMessage();
                        break;
                }
            }
        }
    }


    private class Engine extends CanvasWatchFaceService.Engine {

        private final Handler mUpdateTimeHandler = new EngineHandler(this);
        private Calendar mCalendarLocal,mCalendarUTC;
        private final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mCalendarLocal.setTimeZone(TimeZone.getDefault());
                mCalendarUTC.setTimeZone(TimeZone.getTimeZone("UTC"));
                invalidate();
            }
        };

        float mBatteryPercent, mBatteryVolt;

        private final BroadcastReceiver mBatteryLevelReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
                Intent batteryStatus = context.registerReceiver(null, ifilter);
                int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                int voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);

                mBatteryPercent = (level / (float)scale) * 100;
                mBatteryVolt = (float)voltage / 1000;

                invalidate();
            }
        };


        private boolean mRegisteredTimeZoneReceiver = false;
        private float mXOffset;
        private float mYOffset;
        private Paint mBackgroundPaint;
        private Paint mTextPaint;
        private Paint mTextPaintSmall;
        private Paint mTextPaintLarge;
        private Paint mBatteryArc;
        
		/**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        private boolean mLowBitAmbient;
        private boolean mBurnInProtection;
        private boolean mAmbient;

        private static final int COMPLICATION_ID = 0;
        private final int[] COMPLICATION_IDS = {COMPLICATION_ID};
        private final int[] COMPLICATION_SUPPORTED_TYPES = {ComplicationData.TYPE_LONG_TEXT};

        private SparseArray<ComplicationData> mActiveComplicationDataSparseArray;
        private SparseArray<ComplicationDrawable> mComplicationDrawableSparseArray;


        private void initializeComplications() {


            Log.d("MetarWatchface", "initializeComplications()");

            mActiveComplicationDataSparseArray = new SparseArray<>(COMPLICATION_IDS.length);

            //Hier moet ik nog iets maken


            /*
            ComplicationDrawable leftComplicationDrawable =
                    (ComplicationDrawable) getDrawable(R.drawable.custom_complication_styles);
            leftComplicationDrawable.setContext(getApplicationContext());

            ComplicationDrawable rightComplicationDrawable =
                    (ComplicationDrawable) getDrawable(R.drawable.custom_complication_styles);
            rightComplicationDrawable.setContext(getApplicationContext());
            */
            mComplicationDrawableSparseArray = new SparseArray<>(COMPLICATION_IDS.length);
            //mComplicationDrawableSparseArray.put(COMPLICATION_ID, leftComplicationDrawable);


            setActiveComplications(COMPLICATION_IDS);
        }


        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);
            Log.v("MetarWatchface", "Starting watchface..");
            //Hide Notification dot since we already have our own MSG indicator
            initializeComplications();

            setWatchFaceStyle(new WatchFaceStyle.Builder(MetarWatchface.this)
                    .setHideNotificationIndicator(true)
                    .build());

            mCalendarLocal = Calendar.getInstance();
            mCalendarUTC = Calendar.getInstance();

            Resources resources = MetarWatchface.this.getResources();
            mYOffset = resources.getDimension(R.dimen.digital_y_offset);

            // Initializes background.
            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(ContextCompat.getColor(getApplicationContext(), R.color.background));

            // Initializes Watch Face.
            // Background color to initialize into darkness
            mTextPaint = new Paint();
            mTextPaint.setTypeface(NORMAL_TYPEFACE);
            mTextPaint.setAntiAlias(true);
            mTextPaint.setColor(ContextCompat.getColor(getApplicationContext(), R.color.background));

            mTextPaintLarge = new Paint();
            mTextPaintLarge.setTypeface(NORMAL_TYPEFACE);
            mTextPaintLarge.setAntiAlias(true);
            mTextPaintLarge.setColor(ContextCompat.getColor(getApplicationContext(), R.color.background));

            mTextPaintSmall = new Paint();
            mTextPaintSmall.setTypeface(NORMAL_TYPEFACE);
            mTextPaintSmall.setAntiAlias(true);
            mTextPaintSmall.setColor(ContextCompat.getColor(getApplicationContext(), R.color.background));

            mBatteryArc = new Paint();
            mBatteryArc.setAntiAlias(true);
            mBatteryArc.setColor(ContextCompat.getColor(getApplicationContext(), R.color.digital_text));
            mBatteryArc.setStyle(Paint.Style.STROKE);
            mBatteryArc.setStrokeWidth(resources.getDimension(R.dimen.arc_stroke_width));
        }



        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();
                // Update time zone in case it changed while we weren't visible.
                mCalendarLocal.setTimeZone(TimeZone.getDefault());
                mCalendarUTC.setTimeZone(TimeZone.getTimeZone("UTC"));
                invalidate();
            } else {
                unregisterReceiver();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            MetarWatchface.this.registerReceiver(mTimeZoneReceiver, filter);
            MetarWatchface.this.registerReceiver(mBatteryLevelReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            MetarWatchface.this.unregisterReceiver(mTimeZoneReceiver);
            MetarWatchface.this.unregisterReceiver(mBatteryLevelReceiver);
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);

            // Load resources that have alternate values for round watches.
            Resources resources = MetarWatchface.this.getResources();
            boolean isRound = insets.isRound();
            mXOffset = resources.getDimension(isRound
                    ? R.dimen.digital_x_offset_round : R.dimen.digital_x_offset);
            float textSize = resources.getDimension(isRound
                    ? R.dimen.digital_text_size_round : R.dimen.digital_text_size);
            float textSizeLarge = resources.getDimension(isRound
                    ? R.dimen.digital_text_size_large_round : R.dimen.digital_text_size_large);
            float textSizeSmall = resources.getDimension(R.dimen.battery_text_size);


            mTextPaint.setTextSize(textSize);
            mTextPaintLarge.setTextSize(textSizeLarge);
            mTextPaintSmall.setTextSize(textSizeSmall);
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
            mBurnInProtection = properties.getBoolean(PROPERTY_BURN_IN_PROTECTION, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            //Called at least once a minute or when date/time/timezone has changed
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);

            mAmbient = inAmbientMode;
            if (mLowBitAmbient) {
                mTextPaint.setAntiAlias(!inAmbientMode);
                mTextPaintLarge.setAntiAlias(!inAmbientMode);
                mTextPaintSmall.setAntiAlias(!inAmbientMode);
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }


        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            String mDateText;
            float mLeft, mRight, mTop, mBot;
            float mMSGx = 169;
            float mMSGy = 365;


            Resources resources = MetarWatchface.this.getResources();
            // Draw the background.
            if (isInAmbientMode()) {
                canvas.drawColor(Color.BLACK);
            } else {
                canvas.drawRect(0, 0, bounds.width(), bounds.height(), mBackgroundPaint);
            }

            if(!mAmbient) {
                mTextPaintLarge.setColor(ContextCompat.getColor(getApplicationContext(), R.color.digital_text));
                mTextPaintSmall.setColor(ContextCompat.getColor(getApplicationContext(), R.color.digital_text));
                mTextPaint.setColor(ContextCompat.getColor(getApplicationContext(), R.color.digital_text));
            } else {
                mTextPaintLarge.setColor(ContextCompat.getColor(getApplicationContext(), R.color.ambient_text));
                mTextPaintSmall.setColor(ContextCompat.getColor(getApplicationContext(), R.color.ambient_text));
                mTextPaint.setColor(ContextCompat.getColor(getApplicationContext(), R.color.ambient_text));
            }

            // Draw H:MM in ambient mode or H:MM:SS in interactive mode.
            long now = System.currentTimeMillis();
            mCalendarLocal.setTimeInMillis(now);
            mCalendarUTC.setTimeInMillis(now);

			//Large time indication top center
            String timetextLarge = String.format(Locale.US, "%02d:%02d", mCalendarLocal.get(Calendar.HOUR_OF_DAY), mCalendarLocal.get(Calendar.MINUTE));
            canvas.drawText(timetextLarge, 102, 150, mTextPaintLarge);

			//Zulu time indication middle left
            String timetext = String.format(Locale.US, "%02d:%02dZ", mCalendarUTC.get(Calendar.HOUR_OF_DAY), mCalendarUTC.get(Calendar.MINUTE));
            canvas.drawText(timetext, mXOffset, mYOffset, mTextPaint);

            ////////////////
            //Cleanup here//
            ////////////////
            //int DayOfWeek;
            //DayOfWeek = (mCalendarLocal.get(Calendar.DAY_OF_WEEK) == 0) ? 7 : mCalendarLocal.get(Calendar.DAY_OF_WEEK)-1;
            //String WeekText = String.format(Locale.US,"%04dwk%02d.%01d", mCalendarLocal.get(Calendar.YEAR),mCalendarLocal.get(Calendar.WEEK_OF_YEAR), DayOfWeek);


            //Show date. When local date and UTC date differ, show both.
            String LocalDateText = String.format(Locale.US,"%02d-%02d-%04d", mCalendarLocal.get(Calendar.DAY_OF_MONTH),mCalendarLocal.get(Calendar.MONTH)+1,mCalendarLocal.get(Calendar.YEAR));
            String UTCDateText = String.format(Locale.US,"%02d-%02d-%04d", mCalendarUTC.get(Calendar.DAY_OF_MONTH),mCalendarUTC.get(Calendar.MONTH)+1,mCalendarUTC.get(Calendar.YEAR));


            mLeft = (int)resources.getDimension(R.dimen.arc_margin);
            mRight = 400 - (int)resources.getDimension(R.dimen.arc_margin);
            mTop = (int)resources.getDimension(R.dimen.arc_margin);
            mBot = 400 - (int)resources.getDimension(R.dimen.arc_margin);

            if(LocalDateText.equals(UTCDateText)||mAmbient){
                mDateText = LocalDateText;
            } else {
                mDateText = LocalDateText + " / " + UTCDateText + "Z";
            }
            canvas.drawText(mDateText, mXOffset, mYOffset+24, mTextPaint);

            ////////////////
            //Cleanup here//
            ////////////////

            int mNumNotifications = getUnreadCount();
            //Show things that are visible in interactive mode
            if(!mAmbient) {
                if(mNumNotifications > 0){
                    canvas.drawText("MSG", mMSGx, mMSGy, mTextPaint);
                } else {
                    canvas.drawText(String.format(Locale.US, "%.2fV", mBatteryVolt), 183, 360, mTextPaintSmall);
                    canvas.drawText(String.format(Locale.US, "%.0f%%", mBatteryPercent), 186, 375, mTextPaintSmall);
                }
                canvas.drawArc(mLeft, mTop, mRight, mBot,275f,(mBatteryPercent * (float)3.5),false, mBatteryArc);
            } else {
                if (mNumNotifications > 0) {
                    canvas.drawText("MSG", mMSGx, mMSGy,  mTextPaint);
                }
            }
        }

        /**
         * Starts the {@link #mUpdateTimeHandler} timer if it should be running and isn't currently
         * or stops it if it shouldn't be running but currently is.
         */
        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer should
         * only run when we're visible and in interactive mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        private void handleUpdateTimeMessage() {
            invalidate();
            if (shouldTimerBeRunning()) {
                long timeMs = System.currentTimeMillis();
                long delayMs = INTERACTIVE_UPDATE_RATE_MS
                        - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
            }
        }
    }
}

