package com.apk.claw.android.service;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import com.apk.claw.android.utils.XLog;
import android.view.Display;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Core accessibility service that provides all device interaction capabilities.
 * Singleton-pattern: the running instance is accessible via {@link #getInstance()}.
 */
public class ClawAccessibilityService extends AccessibilityService {

    private static final String TAG = "ClawA11yService";
    private static volatile ClawAccessibilityService instance;

    public static ClawAccessibilityService getInstance() {
        return instance;
    }

    public static boolean isRunning() {
        return instance != null;
    }

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        XLog.i(TAG, "Accessibility service connected");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Events can be processed here if needed in the future
    }

    @Override
    public void onInterrupt() {
        XLog.w(TAG, "Accessibility service interrupted");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
        XLog.i(TAG, "Accessibility service destroyed");
    }

    // ======================== Gesture Operations ========================

    /**
     * Performs a tap at the given screen coordinates.
     */
    public boolean performTap(int x, int y) {
        return performTap(x, y, 100);
    }

    public boolean performTap(int x, int y, long durationMs) {
        Path path = new Path();
        path.moveTo(x, y);
        GestureDescription.StrokeDescription stroke =
                new GestureDescription.StrokeDescription(path, 0, durationMs);
        GestureDescription gesture = new GestureDescription.Builder()
                .addStroke(stroke)
                .build();
        return dispatchGestureSync(gesture);
    }

    /**
     * Performs a long press at the given screen coordinates.
     */
    public boolean performLongPress(int x, int y, long durationMs) {
        Path path = new Path();
        path.moveTo(x, y);
        GestureDescription.StrokeDescription stroke =
                new GestureDescription.StrokeDescription(path, 0, durationMs);
        GestureDescription gesture = new GestureDescription.Builder()
                .addStroke(stroke)
                .build();
        return dispatchGestureSync(gesture);
    }

    /**
     * Performs a swipe gesture from (startX, startY) to (endX, endY).
     */
    public boolean performSwipe(int startX, int startY, int endX, int endY, long durationMs) {
        Path path = new Path();
        path.moveTo(startX, startY);
        path.lineTo(endX, endY);
        GestureDescription.StrokeDescription stroke =
                new GestureDescription.StrokeDescription(path, 0, durationMs);
        GestureDescription gesture = new GestureDescription.Builder()
                .addStroke(stroke)
                .build();
        return dispatchGestureSync(gesture);
    }

    /**
     * Dispatches a gesture and waits for it to complete synchronously.
     */
    private boolean dispatchGestureSync(GestureDescription gesture) {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean result = new AtomicBoolean(false);

        boolean dispatched = dispatchGesture(gesture, new GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription gestureDescription) {
                result.set(true);
                latch.countDown();
            }

            @Override
            public void onCancelled(GestureDescription gestureDescription) {
                result.set(false);
                latch.countDown();
            }
        }, null);

        if (!dispatched) {
            return false;
        }

        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
        return result.get();
    }

    // ======================== Node Operations ========================

    /**
     * Finds all nodes matching the given text.
     */
    public List<AccessibilityNodeInfo> findNodesByText(String text) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            return new ArrayList<>();
        }
        List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByText(text);
        return nodes != null ? nodes : new ArrayList<>();
    }

    /**
     * Finds all nodes matching the given view ID (e.g. "com.example:id/button").
     */
    public List<AccessibilityNodeInfo> findNodesById(String viewId) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            return new ArrayList<>();
        }
        List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByViewId(viewId);
        return nodes != null ? nodes : new ArrayList<>();
    }

    /**
     * Clicks on a node.
     */
    public boolean clickNode(AccessibilityNodeInfo node) {
        if (node == null) {
            return false;
        }
        if (node.isClickable()) {
            return node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        }
        // Try clicking the parent if the node itself is not clickable
        AccessibilityNodeInfo parent = node.getParent();
        while (parent != null) {
            if (parent.isClickable()) {
                return parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            }
            parent = parent.getParent();
        }
        // Fallback: tap at center of node bounds
        Rect bounds = new Rect();
        node.getBoundsInScreen(bounds);
        return performTap(bounds.centerX(), bounds.centerY());
    }

    /**
     * Sets text on a node (for EditText fields).
     */
    public boolean setNodeText(AccessibilityNodeInfo node, String text) {
        if (node == null) {
            return false;
        }
        Bundle args = new Bundle();
        args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text);
        return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args);
    }

    /**
     * Collects a tree representation of the current screen for AI analysis.
     */
    public String getScreenTree() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        buildNodeTree(root, sb, 0);
        return sb.toString();
    }

    private void buildNodeTree(AccessibilityNodeInfo node, StringBuilder sb, int depth) {
        if (node == null) {
            return;
        }
        String indent = "  ".repeat(depth);
        sb.append(indent);
        sb.append("[").append(node.getClassName()).append("]");

        if (node.getViewIdResourceName() != null) {
            sb.append(" id=").append(node.getViewIdResourceName());
        }
        if (node.getText() != null) {
            sb.append(" text=\"").append(node.getText()).append("\"");
        }
        if (node.getContentDescription() != null) {
            sb.append(" desc=\"").append(node.getContentDescription()).append("\"");
        }
        if (node.isClickable()) {
            sb.append(" [clickable]");
        }
        if (node.isScrollable()) {
            sb.append(" [scrollable]");
        }
        if (node.isFocusable()) {
            sb.append(" [focusable]");
        }
        if (node.isEditable()) {
            sb.append(" [editable]");
        }

        Rect bounds = new Rect();
        node.getBoundsInScreen(bounds);
        sb.append(" bounds=").append(bounds.toShortString());

        sb.append("\n");

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                buildNodeTree(child, sb, depth + 1);
                child.recycle();
            }
        }
    }

    /**
     * Recycles a list of AccessibilityNodeInfo nodes.
     * Call this after you are done using nodes returned by findNodesByText/findNodesById.
     */
    public static void recycleNodes(List<AccessibilityNodeInfo> nodes) {
        if (nodes == null) return;
        for (AccessibilityNodeInfo node : nodes) {
            if (node != null) {
                try {
                    node.recycle();
                } catch (Exception ignored) {
                    // Already recycled
                }
            }
        }
    }

    /**
     * Finds a specific node and returns detailed info as a string.
     */
    public String getNodeDetail(AccessibilityNodeInfo node) {
        if (node == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("class=").append(node.getClassName());
        if (node.getViewIdResourceName() != null) {
            sb.append(", id=").append(node.getViewIdResourceName());
        }
        if (node.getText() != null) {
            sb.append(", text=\"").append(node.getText()).append("\"");
        }
        if (node.getContentDescription() != null) {
            sb.append(", desc=\"").append(node.getContentDescription()).append("\"");
        }
        sb.append(", clickable=").append(node.isClickable());
        sb.append(", enabled=").append(node.isEnabled());
        sb.append(", visible=").append(node.isVisibleToUser());
        Rect bounds = new Rect();
        node.getBoundsInScreen(bounds);
        sb.append(", bounds=").append(bounds.toShortString());
        return sb.toString();
    }

    // ======================== Global Actions ========================

    public boolean pressBack() {
        return performGlobalAction(GLOBAL_ACTION_BACK);
    }

    public boolean pressHome() {
        return performGlobalAction(GLOBAL_ACTION_HOME);
    }

    public boolean openRecentApps() {
        return performGlobalAction(GLOBAL_ACTION_RECENTS);
    }

    public boolean expandNotifications() {
        return performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS);
    }

    public boolean collapseNotifications() {
        return performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS);
    }

    public boolean lockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN);
        }
        return false;
    }

    // ======================== Screenshot ========================

    /**
     * Takes a screenshot (requires API 30+).
     * Returns the bitmap or null on failure.
     */
    public Bitmap takeScreenshot(long timeoutMs) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return null;
        }
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Bitmap> bitmapRef = new AtomicReference<>(null);

        takeScreenshot(Display.DEFAULT_DISPLAY, getMainExecutor(),
                new TakeScreenshotCallback() {
                    @Override
                    public void onSuccess(ScreenshotResult result) {
                        Bitmap bmp = Bitmap.wrapHardwareBuffer(
                                result.getHardwareBuffer(), result.getColorSpace());
                        bitmapRef.set(bmp);
                        result.getHardwareBuffer().close();
                        latch.countDown();
                    }

                    @Override
                    public void onFailure(int errorCode) {
                        XLog.e(TAG, "Screenshot failed with error code: " + errorCode);
                        latch.countDown();
                    }
                });

        try {
            latch.await(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return bitmapRef.get();
    }

    // ======================== Key Event Injection (TV Remote) ========================

    /**
     * Sends a key event via shell command. Works reliably on Android TV boxes.
     *
     * @param keyCode Android KeyEvent keycode (e.g. KeyEvent.KEYCODE_DPAD_UP = 19)
     * @return true if the command executed without error
     */
    public boolean sendKeyEvent(int keyCode) {
        try {
            Process process = Runtime.getRuntime().exec(
                    new String[]{"input", "keyevent", String.valueOf(keyCode)});
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            XLog.e(TAG, "Failed to send key event: " + keyCode, e);
            return false;
        }
    }

    // ======================== App Launch ========================

    /**
     * Opens an app by its package name.
     */
    public boolean openApp(String packageName) {
        try {
            Intent intent = getPackageManager().getLaunchIntentForPackage(packageName);
            if (intent == null) {
                XLog.e(TAG, "Cannot resolve launch intent for " + packageName);
                return false;
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            return true;
        } catch (Exception e) {
            XLog.e(TAG, "Failed to open app: " + packageName, e);
            return false;
        }
    }
}
