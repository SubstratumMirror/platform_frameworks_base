/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.systemui.qs.tileimpl;


import static androidx.lifecycle.Lifecycle.State.DESTROYED;
import static androidx.lifecycle.Lifecycle.State.RESUMED;

import static com.android.internal.logging.nano.MetricsProto.MetricsEvent.ACTION_QS_CLICK;
import static com.android.internal.logging.nano.MetricsProto.MetricsEvent.ACTION_QS_LONG_PRESS;
import static com.android.internal.logging.nano.MetricsProto.MetricsEvent.ACTION_QS_SECONDARY_CLICK;
import static com.android.internal.logging.nano.MetricsProto.MetricsEvent.FIELD_QS_POSITION;
import static com.android.internal.logging.nano.MetricsProto.MetricsEvent.FIELD_QS_VALUE;
import static com.android.internal.logging.nano.MetricsProto.MetricsEvent.FIELD_STATUS_BAR_STATE;
import static com.android.internal.logging.nano.MetricsProto.MetricsEvent.TYPE_ACTION;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Intent;
import android.metrics.LogMaker;
import android.os.Handler;
import android.os.Looper;
import android.service.quicksettings.Tile;
import android.testing.AndroidTestingRunner;
import android.testing.TestableLooper;
import android.testing.TestableLooper.RunWithLooper;

import androidx.test.filters.SmallTest;

import com.android.internal.logging.InstanceId;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.UiEventLogger;
import com.android.internal.logging.testing.UiEventLoggerFake;
import com.android.systemui.SysuiTestCase;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.qs.QSTile;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.qs.QSEvent;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.QSTileHost;
import com.android.systemui.qs.logging.QSLogger;
import com.android.systemui.statusbar.StatusBarState;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidTestingRunner.class)
@RunWithLooper(setAsMainLooper = true)
@SmallTest
public class QSTileImplTest extends SysuiTestCase {

    public static final int POSITION = 14;
    private static final String SPEC = "spec";

    @Mock
    private QSLogger mQsLogger;

    private TestableLooper mTestableLooper;
    private TileImpl mTile;
    @Mock
    private QSTileHost mHost;
    @Mock
    private MetricsLogger mMetricsLogger;
    @Mock
    private StatusBarStateController mStatusBarStateController;
    @Mock
    private ActivityStarter mActivityStarter;
    private UiEventLoggerFake mUiEventLoggerFake;
    private InstanceId mInstanceId = InstanceId.fakeInstanceId(5);

    @Captor
    private ArgumentCaptor<LogMaker> mLogCaptor;

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);
        mTestableLooper = TestableLooper.get(this);
        mUiEventLoggerFake = new UiEventLoggerFake();
        when(mHost.indexOf(SPEC)).thenReturn(POSITION);
        when(mHost.getContext()).thenReturn(mContext.getBaseContext());
        when(mHost.getUiEventLogger()).thenReturn(mUiEventLoggerFake);
        when(mHost.getNewInstanceId()).thenReturn(mInstanceId);

        Handler mainHandler = new Handler(mTestableLooper.getLooper());

        mTile = new TileImpl(mHost, mTestableLooper.getLooper(), mainHandler,
                mMetricsLogger, mStatusBarStateController, mActivityStarter, mQsLogger);
        mTile.setTileSpec(SPEC);
    }

    @Test
    public void testClick_Metrics() {
        mTile.click();
        verify(mMetricsLogger).write(argThat(new TileLogMatcher(ACTION_QS_CLICK)));
        assertEquals(1, mUiEventLoggerFake.numLogs());
        UiEventLoggerFake.FakeUiEvent event = mUiEventLoggerFake.get(0);
        assertEvent(QSEvent.QS_ACTION_CLICK, event);
    }

    @Test
    public void testClick_log() {
        when(mStatusBarStateController.getState()).thenReturn(StatusBarState.SHADE);

        mTile.click();
        verify(mQsLogger).logTileClick(SPEC, StatusBarState.SHADE, Tile.STATE_ACTIVE);
    }

    @Test
    public void testClick_Metrics_Status_Bar_Status() {
        when(mStatusBarStateController.getState()).thenReturn(StatusBarState.SHADE);
        mTile.click();
        verify(mMetricsLogger).write(mLogCaptor.capture());
        assertEquals(StatusBarState.SHADE, mLogCaptor.getValue()
                .getTaggedData(FIELD_STATUS_BAR_STATE));
    }

    @Test
    public void testSecondaryClick_Metrics() {
        mTile.secondaryClick();
        verify(mMetricsLogger).write(argThat(new TileLogMatcher(ACTION_QS_SECONDARY_CLICK)));
        assertEquals(1, mUiEventLoggerFake.numLogs());
        UiEventLoggerFake.FakeUiEvent event = mUiEventLoggerFake.get(0);
        assertEvent(QSEvent.QS_ACTION_SECONDARY_CLICK, event);
    }

    @Test
    public void testSecondaryClick_log() {
        when(mStatusBarStateController.getState()).thenReturn(StatusBarState.SHADE);

        mTile.secondaryClick();
        verify(mQsLogger).logTileSecondaryClick(SPEC, StatusBarState.SHADE, Tile.STATE_ACTIVE);
    }

    @Test
    public void testSecondaryClick_Metrics_Status_Bar_Status() {
        when(mStatusBarStateController.getState()).thenReturn(StatusBarState.KEYGUARD);
        mTile.secondaryClick();
        verify(mMetricsLogger).write(mLogCaptor.capture());
        assertEquals(StatusBarState.KEYGUARD, mLogCaptor.getValue()
                .getTaggedData(FIELD_STATUS_BAR_STATE));
    }

    @Test
    public void testLongClick_Metrics() {
        mTile.longClick();
        verify(mMetricsLogger).write(argThat(new TileLogMatcher(ACTION_QS_LONG_PRESS)));
        assertEquals(1, mUiEventLoggerFake.numLogs());
        UiEventLoggerFake.FakeUiEvent event = mUiEventLoggerFake.get(0);
        assertEvent(QSEvent.QS_ACTION_LONG_PRESS, event);
    }


    @Test
    public void testLongClick_log() {
        when(mStatusBarStateController.getState()).thenReturn(StatusBarState.SHADE);

        mTile.longClick();
        verify(mQsLogger).logTileLongClick(SPEC, StatusBarState.SHADE, Tile.STATE_ACTIVE);
    }

    @Test
    public void testLongClick_Metrics_Status_Bar_Status() {
        when(mStatusBarStateController.getState()).thenReturn(StatusBarState.SHADE_LOCKED);
        mTile.click();
        verify(mMetricsLogger).write(mLogCaptor.capture());
        assertEquals(StatusBarState.SHADE_LOCKED, mLogCaptor.getValue()
                .getTaggedData(FIELD_STATUS_BAR_STATE));
    }

    @Test
    public void testPopulateWithLockedScreen() {
        LogMaker maker = mock(LogMaker.class);
        when(maker.setSubtype(anyInt())).thenReturn(maker);
        when(maker.addTaggedData(anyInt(), any())).thenReturn(maker);
        mTile.getState().value = true;
        mTile.populate(maker);
        verify(maker).addTaggedData(eq(FIELD_QS_VALUE), eq(1));
        verify(maker).addTaggedData(eq(FIELD_QS_POSITION), eq(POSITION));
    }

    @Test
    public void testPopulateWithUnlockedScreen() {
        LogMaker maker = mock(LogMaker.class);
        when(maker.setSubtype(anyInt())).thenReturn(maker);
        when(maker.addTaggedData(anyInt(), any())).thenReturn(maker);
        mTile.getState().value = true;
        mTile.populate(maker);
        verify(maker).addTaggedData(eq(FIELD_QS_VALUE), eq(1));
        verify(maker).addTaggedData(eq(FIELD_QS_POSITION), eq(POSITION));
    }

    //TODO(b/161799397) Bring back testStaleTimeout when we can use FakeExecutor

    @Test
    public void testStaleListening() {
        mTile.handleStale();
        mTestableLooper.processAllMessages();
        verify(mQsLogger).logTileChangeListening(SPEC, true);

        mTile.handleRefreshState(null);
        mTestableLooper.processAllMessages();
        verify(mQsLogger).logTileChangeListening(SPEC, false);
    }

    @Test
    public void testHandleDestroyClearsHandlerQueue() {
        mTile.handleRefreshState(null); // this will add a delayed H.STALE message
        mTile.handleDestroy();

        assertFalse(mTile.mHandler.hasMessages(QSTileImpl.H.STALE));
    }

    @Test
    public void testHandleDestroyLifecycle() {
        assertNotEquals(DESTROYED, mTile.getLifecycle().getCurrentState());
        mTile.handleDestroy();

        mTestableLooper.processAllMessages();

        assertEquals(DESTROYED, mTile.getLifecycle().getCurrentState());
    }

    @Test
    public void testHandleDestroy_log() {
        mTile.handleDestroy();
        verify(mQsLogger).logTileDestroyed(eq(SPEC), anyString());
    }

    @Test
    public void testListening_log() {
        mTile.handleSetListening(true);
        verify(mQsLogger).logTileChangeListening(SPEC, true);

        mTile.handleSetListening(false);
        verify(mQsLogger).logTileChangeListening(SPEC, false);
    }

    @Test
    public void testListeningTrue_stateAtLeastResumed() {
        mTile.setListening(new Object(), true); // Listen with some object

        TestableLooper.get(this).processAllMessages();

        assertTrue(mTile.getLifecycle().getCurrentState().isAtLeast(RESUMED));
    }

    @Test
    public void testTileDoesntStartResumed() {
        assertFalse(mTile.getLifecycle().getCurrentState().isAtLeast(RESUMED));
    }

    @Test
    public void testListeningFalse_stateAtMostCreated() {
        Object o = new Object();
        mTile.setListening(o, true);

        mTile.setListening(o, false);

        TestableLooper.get(this).processAllMessages();
        assertFalse(mTile.getLifecycle().getCurrentState().isAtLeast(RESUMED));
    }

    @Test
    public void testListeningFalse_stateNotDestroyed() {
        Object o = new Object();
        mTile.setListening(o, true);

        mTile.setListening(o, false);

        TestableLooper.get(this).processAllMessages();
        assertNotEquals(DESTROYED, mTile.getLifecycle().getCurrentState());
    }

    @Test
    public void testRefreshStateAfterDestroyedDoesNotCrash() {
        mTile.destroy();
        mTile.refreshState();

        mTestableLooper.processAllMessages();
    }

    @Test
    public void testSetListeningAfterDestroyedDoesNotCrash() {
        Object o = new Object();
        mTile.destroy();

        mTile.setListening(o, true);
        mTile.setListening(o, false);

        mTestableLooper.processAllMessages();
    }

    private void assertEvent(UiEventLogger.UiEventEnum eventType,
            UiEventLoggerFake.FakeUiEvent fakeEvent) {
        assertEquals(eventType.getId(), fakeEvent.eventId);
        assertEquals(SPEC, fakeEvent.packageName);
        assertEquals(mInstanceId, fakeEvent.instanceId);
    }

    private class TileLogMatcher implements ArgumentMatcher<LogMaker> {

        private final int mCategory;
        public String mInvalid;

        public TileLogMatcher(int category) {
            mCategory = category;
        }

        @Override
        public boolean matches(LogMaker arg) {
            if (arg.getCategory() != mCategory) {
                mInvalid = "Expected category " + mCategory + " but was " + arg.getCategory();
                return false;
            }
            if (arg.getType() != TYPE_ACTION) {
                mInvalid = "Expected type " + TYPE_ACTION + " but was " + arg.getType();
                return false;
            }
            if (arg.getSubtype() != mTile.getMetricsCategory()) {
                mInvalid = "Expected subtype " + mTile.getMetricsCategory() + " but was "
                        + arg.getSubtype();
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return mInvalid;
        }
    }

    private static class TileImpl extends QSTileImpl<QSTile.BooleanState> {
        protected TileImpl(
                QSHost host,
                Looper backgroundLooper,
                Handler mainHandler,
                MetricsLogger metricsLogger,
                StatusBarStateController statusBarStateController,
                ActivityStarter activityStarter,
                QSLogger qsLogger
        ) {
            super(host, backgroundLooper, mainHandler, metricsLogger, statusBarStateController,
                    activityStarter, qsLogger);
            getState().state = Tile.STATE_ACTIVE;
        }

        @Override
        public BooleanState newTileState() {
            return new BooleanState();
        }

        @Override
        protected void handleClick() {

        }

        @Override
        protected void handleUpdateState(BooleanState state, Object arg) {

        }

        @Override
        public int getMetricsCategory() {
            return 42;
        }

        @Override
        public Intent getLongClickIntent() {
            return null;
        }

        @Override
        public CharSequence getTileLabel() {
            return null;
        }
    }
}
