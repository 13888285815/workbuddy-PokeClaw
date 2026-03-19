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

public class CollapseNotificationsTool extends BaseTool {

    @Override
    public String getName() {
        return "collapse_notifications";
    }

    @Override
    public String getDisplayName() {
        return ClawApplication.Companion.getInstance().getString(R.string.tool_name_collapse_notifications);
    }

    @Override
    public String getDescriptionEN() {
        return "Collapse the notification shade / quick settings panel.";
    }

    @Override
    public String getDescriptionCN() {
        return "收起通知栏/快捷设置面板。";
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
        boolean success = service.collapseNotifications();
        return success ? ToolResult.success("Collapsed notifications")
                : ToolResult.error("Failed to collapse notifications");
    }
}
