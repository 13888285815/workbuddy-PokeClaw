package com.apk.claw.android.tool.impl;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityNodeInfo;

import com.apk.claw.android.ClawApplication;
import com.apk.claw.android.R;
import com.apk.claw.android.service.ClawAccessibilityService;
import com.apk.claw.android.tool.BaseTool;
import com.apk.claw.android.tool.ToolParameter;
import com.apk.claw.android.tool.ToolResult;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class InputTextTool extends BaseTool {

    @Override
    public String getName() {
        return "input_text";
    }

    @Override
    public String getDisplayName() {
        return ClawApplication.Companion.getInstance().getString(R.string.tool_name_input_text);
    }

    @Override
    public String getDescriptionEN() {
        return "Input text into a focused text field or a text field found by resource ID. If no ID is provided, it will attempt to set text on the currently focused node.";
    }

    @Override
    public String getDescriptionCN() {
        return "向聚焦的文本框或通过资源ID找到的文本框输入文本。如果未提供ID，将尝试在当前聚焦的节点上设置文本。";
    }

    @Override
    public List<ToolParameter> getParameters() {
        return Arrays.asList(
                new ToolParameter("text", "string", "The text to input", true),
                new ToolParameter("id", "string", "The resource ID of the text field (optional, uses focused node if not provided)", false)
        );
    }

    @Override
    public ToolResult execute(Map<String, Object> params) {
        ClawAccessibilityService service = ClawAccessibilityService.getInstance();
        if (service == null) {
            return ToolResult.error("Accessibility service is not running");
        }

        String text = requireString(params, "text");
        String id = optionalString(params, "id", "");

        AccessibilityNodeInfo targetNode = null;

        if (!id.isEmpty()) {
            List<AccessibilityNodeInfo> nodes = service.findNodesById(id);
            if (!nodes.isEmpty()) {
                targetNode = nodes.get(0);
                // Recycle unused nodes (skip index 0 which is targetNode)
                for (int i = 1; i < nodes.size(); i++) {
                    try { nodes.get(i).recycle(); } catch (Exception ignored) {}
                }
            }
        } else {
            targetNode = service.getRootInActiveWindow() != null
                    ? findFocusedEditText(service.getRootInActiveWindow())
                    : null;
        }

        if (targetNode == null) {
            return ToolResult.error("No target text field found");
        }

        // 先尝试点击获取焦点
        targetNode.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
        targetNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);

        // 策略1: 先尝试 ACTION_SET_TEXT（标准方式）
        Bundle args = new Bundle();
        args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text);
        if (targetNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)) {
            return ToolResult.success("Input text: " + text);
        }

        // 策略2: 通过剪贴板粘贴（兼容性更好）
        boolean clipboardSet = setClipboardText(service, text);
        if (!clipboardSet) {
            return ToolResult.error("Failed to set clipboard text");
        }

        // 先清空已有内容：全选 + 粘贴覆盖
        Bundle selectAllArgs = new Bundle();
        selectAllArgs.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, 0);
        selectAllArgs.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, Integer.MAX_VALUE);
        targetNode.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, selectAllArgs);

        // 执行粘贴
        if (targetNode.performAction(AccessibilityNodeInfo.ACTION_PASTE)) {
            return ToolResult.success("Input text (via paste): " + text);
        }

        return ToolResult.error("Failed to input text, both ACTION_SET_TEXT and clipboard paste failed");
    }

    private boolean setClipboardText(Context context, String text) {
        CountDownLatch latch = new CountDownLatch(1);
        boolean[] result = {false};

        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboard != null) {
                    clipboard.setPrimaryClip(ClipData.newPlainText("input_text", text));
                    result[0] = true;
                }
            } catch (Exception ignored) {
            }
            latch.countDown();
        });

        try {
            latch.await(2, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
        }
        return result[0];
    }

    private AccessibilityNodeInfo findFocusedEditText(AccessibilityNodeInfo root) {
        if (root == null) return null;
        AccessibilityNodeInfo focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
        if (focused != null && focused.isEditable()) {
            return focused;
        }
        // Fallback: find first editable node
        return findFirstEditable(root);
    }

    private AccessibilityNodeInfo findFirstEditable(AccessibilityNodeInfo node) {
        if (node == null) return null;
        if (node.isEditable()) return node;
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child == null) continue;
            AccessibilityNodeInfo result = findFirstEditable(child);
            if (result != null) {
                // Don't recycle child if it's the result itself
                if (result != child) {
                    child.recycle();
                }
                return result;
            }
            child.recycle();
        }
        return null;
    }
}
