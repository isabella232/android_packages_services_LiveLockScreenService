/*
 * Copyright (C) 2016 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.cyanogenmod.livelockscreen.service;

import android.content.ComponentName;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.RemoteException;
import android.util.Log;

import cyanogenmod.app.BaseLiveLockManagerService;
import cyanogenmod.app.LiveLockScreenInfo;
import cyanogenmod.providers.CMSettings;

import java.util.Objects;

public class LiveLockScreenManagerService extends BaseLiveLockManagerService {
    private static final String TAG = LiveLockScreenManagerService.class.getSimpleName();
    private static final boolean DEBUG = false;

    private LiveLockScreenInfo mCurrentLiveLockScreen;

    private WorkerHandler mHandler;
    private final HandlerThread mWorkerThread = new HandlerThread("worker",
            Process.THREAD_PRIORITY_BACKGROUND);

    private class WorkerHandler extends Handler {
        private static final int MSG_UPDATE_CURRENT = 1000;

        public WorkerHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_UPDATE_CURRENT:
                    handleUpdateCurrentLiveLockScreenLocked((LiveLockScreenInfo) msg.obj);
                    break;
                default:
                    Log.w(TAG, "Unknown message " + msg.what);
                    break;
            }
        }
    }

    @Override
    public void enqueueLiveLockScreen(String pkg, int id, LiveLockScreenInfo lls, int[] idReceived,
            int userId) throws RemoteException {
        Log.i(TAG, "enqueueLiveLockScreen is not implemented");
    }

    @Override
    public void cancelLiveLockScreen(String pkg, int id, int userId) throws RemoteException {
        Log.i(TAG, "cancelLiveLockScreen is not implemented");
    }

    @Override
    public LiveLockScreenInfo getCurrentLiveLockScreen() throws RemoteException {
        return mCurrentLiveLockScreen;
    }

    @Override
    public void updateDefaultLiveLockScreen(LiveLockScreenInfo llsInfo) throws RemoteException {
        Message msg = mHandler.obtainMessage(WorkerHandler.MSG_UPDATE_CURRENT, llsInfo);
        mHandler.sendMessage(msg);
    }

    public LiveLockScreenManagerService() {
        super();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (DEBUG) Log.d(TAG, "service created");
        mWorkerThread.start();
        mHandler = new WorkerHandler(mWorkerThread.getLooper());

        mCurrentLiveLockScreen = getDefaultLiveLockScreenInternal();
    }

    private void handleUpdateCurrentLiveLockScreenLocked(LiveLockScreenInfo llsInfo) {
        if (!Objects.equals(mCurrentLiveLockScreen, llsInfo)) {
            mCurrentLiveLockScreen = llsInfo;
            notifyChangeListeners(llsInfo);
        }
    }

    private LiveLockScreenInfo getDefaultLiveLockScreenInternal() {
        final String defComponent = CMSettings.Secure.getString(getContentResolver(),
                CMSettings.Secure.DEFAULT_LIVE_LOCK_SCREEN_COMPONENT);

        if (defComponent != null) {
            return new LiveLockScreenInfo.Builder()
                    .setComponent(ComponentName.unflattenFromString(defComponent))
                    .build();
        }
        return null;
    }
}
