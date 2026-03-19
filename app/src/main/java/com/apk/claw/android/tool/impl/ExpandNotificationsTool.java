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

public class ExpandNotificationsTool extends BaseTool {

    @Override
    public String getName() {
        return "expand_notifications";
    }

    @Override
    public String getDisplayName() {
        return ClawApplication.Companion.getInstance().getString(R.string.tool_name_expand_notifications);
    }

    @Override
    public String getDescriptionEN() {
        return "Expand the notification shade to view notifications.";
    }

    @Override
    public String getDescriptionCN() {
        return "展开通知栏查看通知。";
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
        boolean success = service.expandNotifications();
        return success ? ToolResult.success("Expanded notifications")
                : ToolResult.error("Failed to expand notifications");
    }
}
