package codelab.gdg.watchfacehack;

import android.content.ContentUris;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.provider.WearableCalendarContract;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.DynamicLayout;
import android.text.Editable;
import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.format.DateUtils;
import android.view.SurfaceHolder;

import java.util.concurrent.TimeUnit;

public class CalendarWatchFaceService extends CanvasWatchFaceService {

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private class Engine extends CanvasWatchFaceService.Engine {
        static final int BACKGROUND_COLOR = Color.BLACK;
        static final int FOREGROUND_COLOR = Color.WHITE;
        static final int TEXT_SIZE = 15;

        /* 일정 수의 맞춰서 수정가능한 String을 표현하기위한 객체입니다 */
        final Editable mEditable = new SpannableStringBuilder();

        /* mLayout이 생성됬을 때, 가로 길이를 측정하기 위한 변수 */
        int mLayoutWidth;

        /* 일정 수의 맞춰서 변화하는 Layout을 선언 */
        DynamicLayout mLayout;

        /* Text를 그리기 위해 사용되는 Paint */
        final TextPaint mTextPaint = new TextPaint();

        /* 일정 정보를 읽어오는 Task의 인스턴스 */
        private AsyncTask<Void, Void, Integer> mLoadMeetingsTask;

        private class LoadMeetingsTask extends AsyncTask<Void, Void, Integer> {
            @Override
            protected Integer doInBackground(Void... voids) {
                long begin = System.currentTimeMillis();
                Uri.Builder builder =
                        WearableCalendarContract.Instances.CONTENT_URI.buildUpon();
                ContentUris.appendId(builder, begin);
                ContentUris.appendId(builder, begin + DateUtils.DAY_IN_MILLIS);
                final Cursor cursor = getContentResolver() .query(builder.build(),
                        null, null, null, null);
                int numMeetings = cursor.getCount();
                return numMeetings;
            }

            @Override
            protected void onPostExecute(Integer result) {
                /* 미팅의 수를 통해 화면을 다시 그림 */
                onMeetingsLoaded(result);
            }
        }

        int mNumMeetings;

        static final int MSG_LOAD_MEETINGS = 0;

        /* Handler to load the meetings once a minute in interactive mode. */
        final Handler mLoadMeetingsHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case MSG_LOAD_MEETINGS:
                        if (mLoadMeetingsTask != null) {
                            mLoadMeetingsTask.cancel(true);
                        }
                        mLoadMeetingsTask = new LoadMeetingsTask();
                        mLoadMeetingsTask.execute();
                        break;
                }
            }
        };

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
            /* 워치페이스가 보이게 되거나 안보이게될 때 호출되는 콜백 */
            if (visible) {
                mLoadMeetingsHandler.sendEmptyMessage(MSG_LOAD_MEETINGS);
            } else {
                mLoadMeetingsHandler.removeMessages(MSG_LOAD_MEETINGS);
                if (mLoadMeetingsTask != null) {
                    mLoadMeetingsTask.cancel(true);
                }
            }
        }

        final long  LOAD_MEETINGS_DELAY_MS = TimeUnit.HOURS.toMillis(1);
        private void onMeetingsLoaded(Integer result) {
            if (result != null) {
                mNumMeetings = result;
                invalidate();
            }
            if (isVisible()) {
                mLoadMeetingsHandler.sendEmptyMessageDelayed(
                        MSG_LOAD_MEETINGS, LOAD_MEETINGS_DELAY_MS);
            }
        }

        @Override
         public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);
            /* 워치페이스 초기화시 호출되는 콜백 */
            setWatchFaceStyle(new WatchFaceStyle.Builder(CalendarWatchFaceService.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_VARIABLE)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .build());

            mTextPaint.setColor(FOREGROUND_COLOR);
            mTextPaint.setTextSize(TEXT_SIZE);

            mLoadMeetingsHandler.sendEmptyMessage(MSG_LOAD_MEETINGS);
        }

        @Override
        public void onDestroy() {
            /* 워치페이스 종료시 호출되는 콜백 */
            mLoadMeetingsHandler.removeMessages(MSG_LOAD_MEETINGS);
            if (mLoadMeetingsTask != null) {
                mLoadMeetingsTask.cancel(true);
            }
            super.onDestroy();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            // Layout을 생성합니다.
            if (mLayout == null || mLayoutWidth != bounds.width()) {
                mLayoutWidth = bounds.width();
                mLayout = new DynamicLayout(mEditable, mTextPaint, mLayoutWidth,
                        Layout.Alignment.ALIGN_OPPOSITE, 1, 0, false);
            }

            // mEditable의 값을 갱신합니다.
            mEditable.clear();
            mEditable.append("  24시간내 당신의 일정은 : ");
            mEditable.append(String.valueOf(mNumMeetings));
            mEditable.append("개 입니다.");

            int xPos = (canvas.getWidth() / 2) - (mLayout.getWidth() / 2);
            int yPos = (int) ((canvas.getHeight() / 2) - ((mTextPaint.descent() + mTextPaint.ascent()) / 2)) ;

            // text를 그립니다.
            canvas.drawColor(BACKGROUND_COLOR);
            canvas.drawText(mEditable.toString(), xPos, yPos, mTextPaint);
        }
    }
}
