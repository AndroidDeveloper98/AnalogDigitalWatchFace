package com.mathi.alllivesmatter.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.complications.ComplicationData;
import android.support.wearable.complications.rendering.ComplicationDrawable;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;

import androidx.annotation.NonNull;
import com.mathi.alllivesmatter.R;

import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class WatchFaceService extends CanvasWatchFaceService {
    private static final String TAG = "WatchFaceService";
    private Paint mCirclePaint;
    private float mCenterX;
    private float mCenterY;
    private static final float CIRCLE_STROKE_WIDTH = 40f;

    private Calendar mCalendar;
    private boolean mRegisteredTimeZoneReceiver = false;

    private static final float CENTER_GAP_AND_CIRCLE_RADIUS = 4f;

    private Paint mHourPaint;
    private static final float HOUR_STROKE_WIDTH = 32f;
    private float mHourHandLength;
    private float mHoursRotation;

    private Paint mMinutePaint;
    private static final float MINUTE_STROKE_WIDTH = 8f;
    private float mMinuteHandLength;
    private float mMinutesRotation;

    private Paint mSecondPaint;

    Paint textPaint = new Paint();
    private static final float SECOND_TICK_STROKE_WIDTH = 4f;
    private float mSecondHandLength;
    private float mSecondsRotation;

    private static final int MSG_UPDATE_TIME = 0;
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

    private boolean isAmbientMode;



    /******************************************************************************/
    private static final int LEFT_COMPLICATION_ID = 100;
    private static final int RIGHT_COMPLICATION_ID = 101;
    private static final int TOP_COMPLICATION_ID = 102;
    private static final int BOTTOM_COMPLICATION_ID = 103;
    private static final int NOTIFICATION_COMPLICATION_ID = 104;

    // Background, Left and right complication IDs as array for Complication API.
    private static final int[] COMPLICATION_IDS = {
            LEFT_COMPLICATION_ID, RIGHT_COMPLICATION_ID, TOP_COMPLICATION_ID, BOTTOM_COMPLICATION_ID, NOTIFICATION_COMPLICATION_ID
    };

    // Left and right dial supported types.
    private static final int[][] COMPLICATION_SUPPORTED_TYPES = {
            {
                    // left
                    ComplicationData.TYPE_RANGED_VALUE,
                    ComplicationData.TYPE_ICON,
                    ComplicationData.TYPE_SHORT_TEXT,
                    ComplicationData.TYPE_SMALL_IMAGE,
                    ComplicationData.TYPE_NO_PERMISSION
            },
            {
                    // right
                    ComplicationData.TYPE_RANGED_VALUE,
                    ComplicationData.TYPE_ICON,
                    ComplicationData.TYPE_SHORT_TEXT,
                    ComplicationData.TYPE_SMALL_IMAGE,
                    ComplicationData.TYPE_NO_PERMISSION
            },
            {
                    // top
                    ComplicationData.TYPE_LONG_TEXT,
                    ComplicationData.TYPE_RANGED_VALUE,
                    ComplicationData.TYPE_ICON,
                    ComplicationData.TYPE_SHORT_TEXT,
                    ComplicationData.TYPE_SMALL_IMAGE,
                    ComplicationData.TYPE_NO_PERMISSION
            },
            {
                    // bottom
                    ComplicationData.TYPE_LONG_TEXT,
                    ComplicationData.TYPE_RANGED_VALUE,
                    ComplicationData.TYPE_ICON,
                    ComplicationData.TYPE_SHORT_TEXT,
                    ComplicationData.TYPE_SMALL_IMAGE,
                    ComplicationData.TYPE_NO_PERMISSION
            },
            {
                    // notification
                    ComplicationData.TYPE_LONG_TEXT,
                    ComplicationData.TYPE_RANGED_VALUE,
                    ComplicationData.TYPE_ICON,
                    ComplicationData.TYPE_SHORT_TEXT,
                    ComplicationData.TYPE_SMALL_IMAGE,
                    ComplicationData.TYPE_NO_PERMISSION
            }
    };

    // Used by {@link ConfigRecyclerViewAdapter} to check if complication location
    // is supported in settings config_list activity.
/*    public static int getComplicationId(
            ConfigRecyclerViewAdapter.ComplicationLocation complicationLocation) {
        // Add any other supported locations here.
        switch (complicationLocation) {
            case LEFT:
                return LEFT_COMPLICATION_ID;
            case RIGHT:
                return RIGHT_COMPLICATION_ID;
            case TOP:
                return TOP_COMPLICATION_ID;
            case BOTTOM:
                return BOTTOM_COMPLICATION_ID;
            case NOTIFICATION:
                return NOTIFICATION_COMPLICATION_ID;
            default:
                return -1;
        }
    }*/

    // Used by {@link ConfigRecyclerViewAdapter} to retrieve all complication ids.
    public static int[] getComplicationIds() {
        return COMPLICATION_IDS;
    }

    // Used by {@link ConfigRecyclerViewAdapter} to see which complication types
    // are supported in the settings config_list activity.
/*    public static int[] getSupportedComplicationTypes(
            ConfigRecyclerViewAdapter.ComplicationLocation complicationLocation) {
        // Add any other supported locations here.
        switch (complicationLocation) {
            case LEFT:
                return COMPLICATION_SUPPORTED_TYPES[0];
            case RIGHT:
                return COMPLICATION_SUPPORTED_TYPES[1];
            case TOP:
                return COMPLICATION_SUPPORTED_TYPES[2];
            case BOTTOM:
                return COMPLICATION_SUPPORTED_TYPES[3];
            case NOTIFICATION:
                return COMPLICATION_SUPPORTED_TYPES[4];
            default:
                return new int[]{};
        }
    }*/


    private SparseArray<ComplicationData> mActiveComplicationDataSparseArray;

    /* Maps complication ids to corresponding ComplicationDrawable that renders the
     * the complication data on the watch face.
     */
    private SparseArray<ComplicationDrawable> mComplicationDrawableSparseArray;

    @Override
    public Engine onCreateEngine() {
        Log.d(TAG, "onCreateEngine: ");
        return new Engine();
    }

    private class Engine extends CanvasWatchFaceService.Engine {
        private final Handler mUpdateTimeHandler;

        Engine() {
            super();
            mUpdateTimeHandler = new Handler(getMainLooper()) {
                @Override
                public void handleMessage(@NonNull Message message) {
                    invalidate();
                    if (shouldTimerBeRunning()) {
                        long timeMs = System.currentTimeMillis();
                        long delayMs = INTERACTIVE_UPDATE_RATE_MS - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                        mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
                    }
                }
            };
        }

        private final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            }
        };

        @Override
        public void onCreate(SurfaceHolder holder) {
            Log.d(TAG, "onCreate: ");
            super.onCreate(holder);
            mCalendar = Calendar.getInstance();

            setWatchFaceStyle(
                    new WatchFaceStyle.Builder(WatchFaceService.this)
                            .setAcceptsTapEvents(true)
                            .setHideNotificationIndicator(true) // unread top indicator
                            .setShowUnreadCountIndicator(false) // unread count #
                            //.setStatusBarGravity(Gravity.CENTER_HORIZONTAL)
                            .setViewProtectionMode(WatchFaceStyle.PROTECT_STATUS_BAR)
                            .build());
            initializeComplications();
            initWatch();
        }

        private void initializeComplications() {
            Log.d(TAG, "initializeComplications()");

            mActiveComplicationDataSparseArray = new SparseArray<>(COMPLICATION_IDS.length);

            // Creates a ComplicationDrawable for each location where the user can render a
            // complication on the watch face.
            // All styles for the complications are defined in
            // drawable/custom_complication_styles.xml.
            ComplicationDrawable leftComplicationDrawable =
                    (ComplicationDrawable) getDrawable(R.drawable.custom_complication_styles);
            leftComplicationDrawable.setContext(getApplicationContext());
            ComplicationDrawable rightComplicationDrawable =
                    (ComplicationDrawable) getDrawable(R.drawable.custom_complication_styles);
            rightComplicationDrawable.setContext(getApplicationContext());
            ComplicationDrawable topComplicationDrawable =
                    (ComplicationDrawable) getDrawable(R.drawable.custom_complication_styles);
            topComplicationDrawable.setContext(getApplicationContext());
            ComplicationDrawable bottomComplicationDrawable =
                    (ComplicationDrawable) getDrawable(R.drawable.custom_complication_styles);
            bottomComplicationDrawable.setContext(getApplicationContext());
            ComplicationDrawable notificationComplicationDrawable =
                    (ComplicationDrawable) getDrawable(R.drawable.custom_complication_styles);
            notificationComplicationDrawable.setContext(getApplicationContext());


            // Adds new complications to a SparseArray to simplify setting styles and ambient
            // properties for all complications, i.e., iterate over them all.
            mComplicationDrawableSparseArray = new SparseArray<>(COMPLICATION_IDS.length);

            mComplicationDrawableSparseArray.put(LEFT_COMPLICATION_ID, leftComplicationDrawable);
            mComplicationDrawableSparseArray.put(RIGHT_COMPLICATION_ID, rightComplicationDrawable);
            mComplicationDrawableSparseArray.put(TOP_COMPLICATION_ID, topComplicationDrawable);
            mComplicationDrawableSparseArray.put(BOTTOM_COMPLICATION_ID, bottomComplicationDrawable);
            mComplicationDrawableSparseArray.put(NOTIFICATION_COMPLICATION_ID, notificationComplicationDrawable);

            // set default values
            setDefaultSystemComplicationProvider(TOP_COMPLICATION_ID, ConfigData.DEFAULT_TOP_COMPLICATION[0], ConfigData.DEFAULT_TOP_COMPLICATION[1]);
            setDefaultSystemComplicationProvider(LEFT_COMPLICATION_ID, ConfigData.DEFAULT_LEFT_COMPLICATION[0], ConfigData.DEFAULT_LEFT_COMPLICATION[1]);
            setDefaultSystemComplicationProvider(RIGHT_COMPLICATION_ID, ConfigData.DEFAULT_RIGHT_COMPLICATION[0], ConfigData.DEFAULT_RIGHT_COMPLICATION[1]);
            setDefaultSystemComplicationProvider(BOTTOM_COMPLICATION_ID, ConfigData.DEFAULT_BOTTOM_COMPLICATION[0], ConfigData.DEFAULT_BOTTOM_COMPLICATION[1]);
            //setDefaultSystemComplicationProvider(NOTIFICATION_COMPLICATION_ID, ConfigData.DEFAULT_NOTIFICATION_COMPLICATION[0], ConfigData.DEFAULT_NOTIFICATION_COMPLICATION[1]);

           // setComplicationsActiveAndAmbientColors();
            setActiveComplications(COMPLICATION_IDS);
        }


        private boolean mLowBitAmbient;
        private boolean mBurnInProtection;

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            Log.d(TAG, "onPropertiesChanged: low-bit ambient = " + mLowBitAmbient);

            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
            mBurnInProtection = properties.getBoolean(PROPERTY_BURN_IN_PROTECTION, false);

            // Updates complications to properly render in ambient mode based on the
            // screen's capabilities.
            ComplicationDrawable complicationDrawable;

            for (int COMPLICATION_ID : COMPLICATION_IDS) {
                complicationDrawable = mComplicationDrawableSparseArray.get(COMPLICATION_ID);

                complicationDrawable.setLowBitAmbient(mLowBitAmbient);
                complicationDrawable.setBurnInProtection(mBurnInProtection);
            }
        }

        /*
         * Called when there is updated data for a complication id.
         */
        @Override
        public void onComplicationDataUpdate(
                int complicationId, ComplicationData complicationData) {
            Log.d(TAG, "onComplicationDataUpdate() id: " + complicationId);

            // this is where the watch face can interact with the complications it is displaying
            // TODO resize the large complications if a small one is selected
            //if (complicationId == TOP_COMPLICATION_ID && complicationData.getType() == ComplicationData.TYPE_ICON) {
            // etc.. TODO
            //}

            // TODO print info about complications to prevent the need for selecting the notification preview complication manually

            // Adds/updates active complication data in the array.
            mActiveComplicationDataSparseArray.put(complicationId, complicationData);

            // Updates correct ComplicationDrawable with updated data.
            ComplicationDrawable complicationDrawable =
                    mComplicationDrawableSparseArray.get(complicationId);
            complicationDrawable.setComplicationData(complicationData);

            invalidate();
        }

        @Override
        public void onTapCommand(int tapType, int x, int y, long eventTime) {
            Log.d(TAG, "OnTapCommand()");
            if (tapType == TAP_TYPE_TAP) {// If your background complication is the first item in your array, you need
                // to walk backward through the array to make sure the tap isn't for a
                // complication above the background complication.
                for (int i = COMPLICATION_IDS.length - 1; i >= 0; i--) {
                    int complicationId = COMPLICATION_IDS[i];
                    ComplicationDrawable complicationDrawable =
                            mComplicationDrawableSparseArray.get(complicationId);

                    boolean successfulTap = complicationDrawable.onTap(x, y);

                    if (successfulTap) {
                        return;
                    }
                }
            }
        }


        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            mCalendar.setTimeInMillis(System.currentTimeMillis());
            drawBackground(canvas);
            drawComplications(canvas, System.currentTimeMillis());
            drawDigitalTime(canvas);
            drawWatchFace(canvas);
        }

        private void drawComplications(Canvas canvas, long currentTimeMillis) {
            ComplicationDrawable complicationDrawable;

            int skipComplication = NOTIFICATION_COMPLICATION_ID;
            if (getNotificationCount() > 0) {
                skipComplication = TOP_COMPLICATION_ID;
            }

            for (int complicationId : COMPLICATION_IDS) {
                if (complicationId == skipComplication) {
                    continue;
                }
                complicationDrawable = mComplicationDrawableSparseArray.get(complicationId);
                complicationDrawable.draw(canvas, currentTimeMillis);
            }
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            Log.d(TAG, "onSurfaceChanged: ");
            super.onSurfaceChanged(holder, format, width, height);
            mCenterX = width / 2f;
            mCenterY = height / 2f;
            mHourHandLength = (float) (mCenterX * 0.4);
            mMinuteHandLength = (float) (mCenterX * 0.6);
            mSecondHandLength = (float) (mCenterX * 0.75);


            // For most Wear devices, width and height are the same, so we just chose one (width).
            int sizeOfComplication = width / 5;
            int sizeOfLongComplicationWidth = width / 2;
            int sizeOfLongComplicationHeight = height / 6;

            int midpointOfScreen = width / 2;

            int horizontalOffset = (midpointOfScreen - sizeOfComplication) / 2;
            int verticalOffset = midpointOfScreen - (sizeOfComplication / 2);

            Rect leftBounds =
                    // Left, Top, Right, Bottom
                    new Rect(
                            horizontalOffset,
                            verticalOffset,
                            (horizontalOffset + sizeOfComplication),
                            (verticalOffset + sizeOfComplication));

            ComplicationDrawable leftComplicationDrawable =
                    mComplicationDrawableSparseArray.get(LEFT_COMPLICATION_ID);
            leftComplicationDrawable.setBounds(leftBounds);

            Rect rightBounds =
                    // Left, Top, Right, Bottom
                    new Rect(
                            (midpointOfScreen + horizontalOffset),
                            verticalOffset,
                            (midpointOfScreen + horizontalOffset + sizeOfComplication),
                            (verticalOffset + sizeOfComplication));

            ComplicationDrawable rightComplicationDrawable =
                    mComplicationDrawableSparseArray.get(RIGHT_COMPLICATION_ID);
            rightComplicationDrawable.setBounds(rightBounds);

            Rect topBounds =
                    // Left, Top, Right, Bottom
                    new Rect(
                            midpointOfScreen - (sizeOfLongComplicationWidth / 2),
                            midpointOfScreen - ((midpointOfScreen - sizeOfLongComplicationHeight) / 2) - sizeOfLongComplicationHeight,
                            midpointOfScreen + (sizeOfLongComplicationWidth / 2),
                            midpointOfScreen - (midpointOfScreen - sizeOfLongComplicationHeight) / 2);

            ComplicationDrawable topComplicationDrawable =
                    mComplicationDrawableSparseArray.get(TOP_COMPLICATION_ID);
            topComplicationDrawable.setBounds(topBounds);

            Rect bottomBounds =
                    // Left, Top, Right, Bottom
                    new Rect(
                            midpointOfScreen - (sizeOfLongComplicationWidth / 2),
                            midpointOfScreen + ((midpointOfScreen - sizeOfLongComplicationHeight) / 2),
                            midpointOfScreen + (sizeOfLongComplicationWidth / 2),
                            midpointOfScreen + ((midpointOfScreen - sizeOfLongComplicationHeight) / 2) + sizeOfLongComplicationHeight);

            ComplicationDrawable bottomComplicationDrawable =
                    mComplicationDrawableSparseArray.get(BOTTOM_COMPLICATION_ID);
            bottomComplicationDrawable.setBounds(bottomBounds);

            // notification bounds same as top
            ComplicationDrawable notificationComplicationDrawable =
                    mComplicationDrawableSparseArray.get(NOTIFICATION_COMPLICATION_ID);
            notificationComplicationDrawable.setBounds(topBounds);

        }


        @Override
        public void onVisibilityChanged(boolean visible) {
            Log.d(TAG, "onVisibilityChanged: " + visible);
            super.onVisibilityChanged(visible);

            if (visible) {
                registerTimeZoneReceiver(mTimeZoneReceiver);
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            } else {
                unregisterTimeZoneReceiver(mTimeZoneReceiver);
            }
            updateTimer();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            Log.d(TAG, "onAmbientModeChanged: " + inAmbientMode);
            super.onAmbientModeChanged(inAmbientMode);
            isAmbientMode = inAmbientMode;
            updateWatchHandStyle();
            updateTimer();
        }

        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        private boolean shouldTimerBeRunning() {
            return isVisible();
        }

        private void initWatch() {
            Log.d(TAG, "initWatch: ");
            initCirclePaint();
            initHourPaint();
            initMinutePaint();
            initSecondPaint();
        }

        private void initCirclePaint() {
            Log.d(TAG, "initCirclePaint: ");
            mCirclePaint = new Paint();
            mCirclePaint.setColor(Color.YELLOW);
            mCirclePaint.setStrokeWidth(CIRCLE_STROKE_WIDTH);
            mCirclePaint.setAntiAlias(true);
        }

        private void initHourPaint() {
            Log.d(TAG, "initHourPaint: ");
            mHourPaint = new Paint();
            mHourPaint.setColor(Color.parseColor("#FFDBFF"));
            mHourPaint.setStrokeCap(Paint.Cap.ROUND);
            mHourPaint.setStrokeWidth(HOUR_STROKE_WIDTH);
            mHourPaint.setAntiAlias(true);
        }

        private void initMinutePaint() {
            Log.d(TAG, "initMinutePaint: ");
            mMinutePaint = new Paint();
            mMinutePaint.setColor(Color.parseColor("#FAFAFA"));
            mMinutePaint.setStrokeCap(Paint.Cap.SQUARE);
            mMinutePaint.setStrokeWidth(MINUTE_STROKE_WIDTH);
            mMinutePaint.setAntiAlias(true);
        }

        private void initSecondPaint() {
            Log.d(TAG, "initSecondPaint: ");
            mSecondPaint = new Paint();
            mSecondPaint.setColor(Color.parseColor("#900FD6"));
            mSecondPaint.setStrokeWidth(SECOND_TICK_STROKE_WIDTH);
            mSecondPaint.setAntiAlias(true);
        }

        private void drawBackground(Canvas canvas) {
            if (isAmbientMode) canvas.drawColor(Color.BLACK);
            else canvas.drawColor(Color.BLACK);
            //else canvas.drawBitmap(mBackgroundBitmap, 0, 0, null);
        }

        private void drawWatchFace(Canvas canvas) {
            //drawCircle(canvas);
            canvas.save();

            calculateRotation();
            drawHour(canvas);
            drawMinute(canvas);
            drawSecond(canvas);
            drawCenterDot(canvas);
            canvas.restore();
        }

        private void drawDigitalTime(Canvas canvas) {
            textPaint.setAntiAlias(true);
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setColor(Color.parseColor("#CEA3FF"));
            textPaint.setTextSize(160f);
            int xPos = (canvas.getWidth() / 2);
            int yPos = (int) (canvas.getHeight() / 2);
            String hours = String.format("%02d", mCalendar.get(Calendar.HOUR));
            String minute = String.format("%02d", mCalendar.get(Calendar.MINUTE));
            drawString(canvas, "" + hours + "\n" + minute, xPos, yPos, textPaint);
        }

        private void drawCenterDot(Canvas canvas) {
            int width = canvas.getWidth();
            int height = canvas.getHeight();
            textPaint.setColor(Color.parseColor("#900FD6"));
            textPaint.setAntiAlias(true);
            canvas.drawCircle(width / 2, height / 2, 6, textPaint);
        }

        public void drawString(Canvas canvas, String text, int x, int y, Paint paint) {
            if (text.contains("\n")) {
                String[] texts = text.split("\n");
                for (String txt : texts) {
                    canvas.drawText(txt, x, y, paint);
                    y += paint.getTextSize() - 32;
                }
            } else {
                canvas.drawText(text, x, y, paint);
            }
        }

        public String convertSecondsToHMmSs(long seconds) {
            long s = seconds % 60;
            long m = (seconds / 60) % 60;
            long h = (seconds / (60 * 60)) % 24;
            return String.format("%d:%02d:%02d", h, m, s);
        }

        private void registerTimeZoneReceiver(BroadcastReceiver mTimeZoneReceiver) {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            WatchFaceService.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterTimeZoneReceiver(BroadcastReceiver mTimeZoneReceiver) {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            WatchFaceService.this.unregisterReceiver(mTimeZoneReceiver);
        }

        private void calculateRotation() {
            float mHourHandOffset = mCalendar.get(Calendar.MINUTE) / 2f;
            mHoursRotation = (mCalendar.get(Calendar.HOUR) * 30) + mHourHandOffset;

            mMinutesRotation = mCalendar.get(Calendar.MINUTE) * 6f;

            float seconds = (mCalendar.get(Calendar.SECOND) + mCalendar.get(Calendar.MILLISECOND) / 1000f);
            mSecondsRotation = seconds * 6f;
        }

        private void drawHour(Canvas canvas) {
            canvas.rotate(mHoursRotation, mCenterX, mCenterY);
            canvas.drawLine(mCenterX, mCenterY - CENTER_GAP_AND_CIRCLE_RADIUS, mCenterX, mCenterY - mHourHandLength, mHourPaint);
        }

        private void drawMinute(Canvas canvas) {
            canvas.rotate(mMinutesRotation - mHoursRotation, mCenterX, mCenterY);
            canvas.drawLine(mCenterX, mCenterY - CENTER_GAP_AND_CIRCLE_RADIUS, mCenterX, mCenterY - mMinuteHandLength, mMinutePaint);
        }

        private void drawSecond(Canvas canvas) {
            if (!isAmbientMode) {
                canvas.rotate(mSecondsRotation - mMinutesRotation, mCenterX, mCenterY);
                canvas.drawLine(mCenterX, mCenterY - CENTER_GAP_AND_CIRCLE_RADIUS, mCenterX, mCenterY - mSecondHandLength, mSecondPaint);
            }
        }

        private void updateWatchHandStyle() {
            if (isAmbientMode) {
                mCirclePaint.setColor(Color.BLACK);
                mSecondPaint.setColor(Color.BLACK);

                mHourPaint.setAntiAlias(false);
                mMinutePaint.setAntiAlias(false);
            } else {
                mCirclePaint.setColor(Color.WHITE);
                mSecondPaint.setColor(Color.WHITE);

                mHourPaint.setAntiAlias(true);
                mMinutePaint.setAntiAlias(true);
            }
        }
    }
}
