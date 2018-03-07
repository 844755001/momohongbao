package com.liuchang.momohongbao.service;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Path;
import android.os.Parcelable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.liuchang.momohongbao.activity.MainActivity;
import com.liuchang.momohongbao.model.bean.MessageEvent;
import com.liuchang.momohongbao.util.PowerUtil;

import org.greenrobot.eventbus.EventBus;

import java.util.List;

import static android.app.Notification.EXTRA_TEXT;


public class HongbaoService extends AccessibilityService {
    private String currentActivityName;

    private boolean mNotifyMutex = false, mListMutex = false, mChatMutex = false, mOpenMutex = false, shouqi = false;
    private PowerUtil powerUtil;

    /**
     * AccessibilityEvent
     *
     * @param event 事件
     */
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        setCurrentActivityName(event);

        if (!mNotifyMutex && watchNotifications(event)) {
            mNotifyMutex = true;
            return;
        }
        if (mNotifyMutex && watchList(event)) return;
        if (mListMutex && watchChat(event)) return;
        if (mChatMutex) {
            openPacket(event);
        }
    }

    private boolean watchNotifications(AccessibilityEvent event) {
        // Not a notification
        if (event.getEventType() != AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED)
            return false;
        Parcelable parcelable = event.getParcelableData();
        if (parcelable instanceof Notification) {
            Notification notification = (Notification) parcelable;

            CharSequence cs = notification.extras.getCharSequence(EXTRA_TEXT);
            if (cs == null) {
                return false;
            }
            String content = cs.toString();
            if (content.contains("km") || !content.contains("[红包]") || content.contains("口令红包"))
                return false;

            try {
                /* 清除signature,避免进入会话后误判 */
//                signature.cleanSignature();

                notification.contentIntent.send();
                mNotifyMutex = true;
                return true;
            } catch (PendingIntent.CanceledException e) {
                e.printStackTrace();
                return false;
            }
        }

        return false;
    }

    private boolean watchList(AccessibilityEvent event) {
        AccessibilityNodeInfo eventSource = event.getSource();
        // Not a message
        if (mListMutex || !currentActivityName.contains("MaintabActivity") || event.getEventType() != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED || eventSource == null)
            return false;
        List<AccessibilityNodeInfo> nodes = eventSource.findAccessibilityNodeInfosByText("[红包]");
        //增加条件判断currentActivityName.contains(WECHAT_LUCKMONEY_GENERAL_ACTIVITY)
        //避免当订阅号中出现标题为“[微信红包]拜年红包”（其实并非红包）的信息时误判
        if (!nodes.isEmpty()) {
            AccessibilityNodeInfo nodeToClick = null;
            for (AccessibilityNodeInfo node : nodes) {
                if (node.getText().toString().startsWith("[红包]")) {
                    nodeToClick = node;
                    break;
                }
            }
            if (nodeToClick == null) return false;
            AccessibilityNodeInfo parent = nodeToClick.getParent();
            if (parent == null) return false;
            CharSequence contentDescription = nodeToClick.getText();
            if (contentDescription.toString().startsWith("[红包]")) {
                parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                Log.d("test", "list");
                mListMutex = true;
                return true;
            }
        }
        return false;
    }

    private boolean watchChat(AccessibilityEvent event) {
        AccessibilityNodeInfo eventSource = event.getSource();
        if (mChatMutex || !currentActivityName.contains("ChatActivity") || event.getEventType() != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED || eventSource == null)
            return false;
        List<AccessibilityNodeInfo> nodes = eventSource.findAccessibilityNodeInfosByText("红包");
        if (!nodes.isEmpty()) {
            AccessibilityNodeInfo nodeToClick = nodes.get(nodes.size() - 1);
            if (nodeToClick == null) return false;
            if (nodeToClick.getText().toString().contains("手气红包")) {
                shouqi = true;
            }
            AccessibilityNodeInfo parent = nodeToClick.getParent();
            if (parent == null) return false;
            parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            Log.d("test", "chat");
            mChatMutex = true;
            return true;
        }
        return false;
    }

    private void openPacket(AccessibilityEvent event) {
        AccessibilityNodeInfo eventSource = getRootInActiveWindow();
        if (mOpenMutex || !currentActivityName.contains("MomoMKWebActivity") || event.getEventType() != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED || eventSource == null)
            return;
        if (eventSource.findAccessibilityNodeInfosByText("发红包").isEmpty()) {
            Log.d("test", "open");
            mOpenMutex = true;
            DisplayMetrics metrics = getResources().getDisplayMetrics();
            if (android.os.Build.VERSION.SDK_INT > 23) {
                Path path = new Path();
                path.moveTo(metrics.widthPixels / 2, metrics.heightPixels / 5 * 3);
                GestureDescription.Builder builder = new GestureDescription.Builder();
                GestureDescription gestureDescription = builder.addStroke(new GestureDescription.StrokeDescription(path, 450, 50)).build();
                dispatchGesture(gestureDescription, new GestureResultCallback() {
                    @Override
                    public void onCompleted(GestureDescription gestureDescription) {
                        Log.d("test", "onCompleted");
//                    hasOpen = true;
                        mOpenMutex = false;
                        super.onCompleted(gestureDescription);
                    }

                    @Override
                    public void onCancelled(GestureDescription gestureDescription) {
                        Log.d("test", "onCancelled");
                        mOpenMutex = false;
                        super.onCancelled(gestureDescription);
                    }
                }, null);
            }
        } else {
//            getWebview(eventSource);
            EventBus.getDefault().post(new MessageEvent(shouqi));
            try {
                Thread.sleep(500);
                startActivity(new Intent(getBaseContext(), MainActivity.class));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            mNotifyMutex = false;
            mListMutex = false;
            mChatMutex = false;
            mOpenMutex = false;
            shouqi = false;
        }
//        }
    }

    private void setCurrentActivityName(AccessibilityEvent event) {
        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return;
        }

        try {
            ComponentName componentName = new ComponentName(
                    event.getPackageName().toString(),
                    event.getClassName().toString()
            );

            getPackageManager().getActivityInfo(componentName, 0);
            currentActivityName = componentName.flattenToShortString();
        } catch (PackageManager.NameNotFoundException e) {
            currentActivityName = "MaintabActivity";
        }
    }

    @Override
    public void onInterrupt() {

    }

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        powerUtil = new PowerUtil(this);
        powerUtil.handleWakeLock(true);
    }

    @Override
    public void onDestroy() {
        this.powerUtil.handleWakeLock(false);
        super.onDestroy();
    }
}
