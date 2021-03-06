package codelab.gdg.watchfacehack;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
import android.view.SurfaceHolder;

import java.util.concurrent.TimeUnit;


public class AnalogWatchFaceService extends CanvasWatchFaceService {
    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

    private class Engine extends CanvasWatchFaceService.Engine {
        static final int MSG_UPDATE_TIME = 0;

        /* 타임 객체 */
        Time mTime;

        /* 디바이스의 특징을 갖는 변수 */
        boolean mLowBitAmbient;

        /* 그래픽 객체 */
        Bitmap mBackgroundBitmap;
        Bitmap mBackgroundScaledBitmap;
        Paint mHourPaint;
        Paint mMinutePaint;
        Paint mSecondPaint;

        /* Interactive 모드일 때, 1초에 한번 시간을 업데이트 하기 위해 사용하는 핸들러 */
        final Handler mUpdateTimeHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case MSG_UPDATE_TIME:
                        invalidate();
                        /* Interactive 모드이면 */
                        if (isVisible() && !isInAmbientMode()) {
                            long timeMs = System.currentTimeMillis();
                            long delayMs = INTERACTIVE_UPDATE_RATE_MS - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                            mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
                        }
                        break;
                }
            }
        };

        /* Timezone의 변경을 감지하는 리시버 */
        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mTime.clear(intent.getStringExtra("time-zone"));
                mTime.setToNow();
            }
        };

        /* Timezone 리시버의 등록 여부를 저장하는 변수 */
        boolean mRegisteredTimeZoneReceiver = false;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);
            /* 워치페이스 초기화시 호출되는 콜백 */

            /* System UI를 설정합니다 */
            setWatchFaceStyle(new WatchFaceStyle.Builder(AnalogWatchFaceService.this)
                .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                .setShowSystemUiTime(false)
                .build());

            /* 배경이미지를 로드합니다 */
            Resources resources = AnalogWatchFaceService.this.getResources();
            Drawable backgroundDrawable = resources.getDrawable(R.drawable.bg);
            mBackgroundBitmap = ((BitmapDrawable) backgroundDrawable).getBitmap();

            /* 그래픽 객체를 생성합니다 */
            mHourPaint = new Paint();
            mHourPaint.setARGB(255, 200, 200, 200);
            mHourPaint.setStrokeWidth(5.f);
            mHourPaint.setAntiAlias(true);
            mHourPaint.setStrokeCap(Paint.Cap.ROUND);

            mMinutePaint = new Paint();
            mMinutePaint.setARGB(255, 200, 200, 200);
            mMinutePaint.setStrokeWidth(3.f);
            mMinutePaint.setAntiAlias(true);
            mMinutePaint.setStrokeCap(Paint.Cap.ROUND);

            mSecondPaint = new Paint();
            mSecondPaint.setARGB(255, 255, 0, 0);
            mSecondPaint.setStrokeWidth(2.f);
            mSecondPaint.setAntiAlias(true);
            mSecondPaint.setStrokeCap(Paint.Cap.ROUND);

            /* 타임 객체를 생성합니다 */
            mTime = new Time();
        }

        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (isVisible() && !isInAmbientMode()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
            /* 워치페이스가 보이게 되거나 안보이게될 때 호출되는 콜백 */
            if (visible) {
                /* Timezone 리시버가 등록되지 않았다면 등록 */
                if (mRegisteredTimeZoneReceiver == false) {
                    mRegisteredTimeZoneReceiver = true;
                    IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
                    AnalogWatchFaceService.this.registerReceiver(mTimeZoneReceiver, filter);
                }
            } else {
                /* Timezone 리시버가 등록되었다면 등록 해제 */
                if (mRegisteredTimeZoneReceiver == true) {
                    mRegisteredTimeZoneReceiver = false;
                    AnalogWatchFaceService.this.unregisterReceiver(mTimeZoneReceiver);
                }
            }
            /* 타이머 갱신 */
            updateTimer();
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            /* Ambient 모드에서 1분마다 호출되는 콜백 */
            invalidate();
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            /* 워치페이스 종료시 호출되는 콜백 */
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            super.onDraw(canvas, bounds);
            /* 워치페이스를 그리는 곳으로 invalidate가 호출되면 자동으로 콜백이 불림(가장 중요) */
            /* 시간을 갱신합니다. */
            mTime.setToNow();

            int width = bounds.width();
            int height = bounds.height();

            /* 배경 이미지의 크기를 bound에 맞게 수정하고 배경으로 지정합니다. */
            if (mBackgroundScaledBitmap == null
                    || mBackgroundScaledBitmap.getWidth() != width
                    || mBackgroundScaledBitmap.getHeight() != height) {
                mBackgroundScaledBitmap = Bitmap.createScaledBitmap(mBackgroundBitmap,
                        width, height, true /* filter */);
            }
            canvas.drawBitmap(mBackgroundScaledBitmap, 0, 0, null);

            /* 중심 좌표를 구합니다. */
            float centerX = width / 2f;
            float centerY = height / 2f;

            /* 각 침들의 각도와 길이를 계산합니다. */
            float secRot = mTime.second / 30f * (float) Math.PI;
            int minutes = mTime.minute;
            float minRot = minutes / 30f * (float) Math.PI;
            float hrRot = ((mTime.hour + (minutes / 60f)) / 6f ) * (float) Math.PI;

            float secLength = centerX - 20;
            float minLength = centerX - 40;
            float hrLength = centerX - 80;

            /* Interactive 모드일 때에는, 초침을 그립니다. */
            if (!isInAmbientMode()) {
                float secX = (float) Math.sin(secRot) * secLength;
                float secY = (float) -Math.cos(secRot) * secLength;
                canvas.drawLine(centerX, centerY, centerX + secX, centerY +
                        secY, mSecondPaint);
            }

            // Draw the minute and hour hands.
            float minX = (float) Math.sin(minRot) * minLength;
            float minY = (float) -Math.cos(minRot) * minLength;
            canvas.drawLine(centerX, centerY, centerX + minX, centerY + minY,
                    mMinutePaint);
            float hrX = (float) Math.sin(hrRot) * hrLength;
            float hrY = (float) -Math.cos(hrRot) * hrLength;
            canvas.drawLine(centerX, centerY, centerX + hrX, centerY + hrY,
                    mHourPaint);
        }
    }
}
