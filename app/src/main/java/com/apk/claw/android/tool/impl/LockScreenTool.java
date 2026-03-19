package com.apk.claw.android.tool.impl;

import com.apk.claw.android.ClawApplication;
import com.apk.claw.android.R;
import com.apk.claw.android.service.ClawAccessibilityService;
import com.apk.claw.android.tool.BaseTool;
import com.apk.claw.android.tool.ToolParameter;
import com.apk.claw.android.tool.ToolResult;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class LockScreenTool extends BaseTool {

    @Override
    public String getName() {
        return "lock_screen";
    }

    @Override
    public String getDisplayName() {
        return ClawApplication.Companion.getInstance().getString(R.string.tool_name_lock_screen);
    }

    @Override
    public String getDescriptionEN() {
        return "Lock the screen (requires Android 9+).";
    }

    @Override
    public String getDescriptionCN() {
        return "锁定屏幕（需要 Android 9+）。";
    }

    @Override
    public List<ToolParameter> getParameters() {
        return Collections.emptyList();
    }

    @Override
    public ToolResult execute(Map<String, Object> params) {
        ClawAccessibilityService service = ClawAccessibilityService.getInstance();
        if (service == null) {
            return ToolResult.error("Accessibility service is not running");
        }
        boolean success = service.lockScreen();
        return success ? ToolResult.success("Screen locked")
                : ToolResult.error("Failed to lock screen. Requires Android 9+ (API 28).");
    }
}
