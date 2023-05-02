package com.mathi.alllivesmatter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.util.Log;
import android.view.SurfaceHolder;

import androidx.annotation.NonNull;

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
            initWatch();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            mCalendar.setTimeInMillis(System.currentTimeMillis());
            drawBackground(canvas);
            drawDigitalTime(canvas);
            drawWatchFace(canvas);
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
