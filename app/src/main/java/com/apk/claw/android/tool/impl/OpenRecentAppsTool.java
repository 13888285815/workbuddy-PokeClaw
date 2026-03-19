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

public class OpenRecentAppsTool extends BaseTool {

    @Override
    public String getName() {
        return "open_recent_apps";
    }

    @Override
    public String getDisplayName() {
        return ClawApplication.Companion.getInstance().getString(R.string.tool_name_open_recent_apps);
    }

    @Override
    public String getDescriptionEN() {
        return "Open the recent apps (task switcher) screen.";
    }

    @Override
    public String getDescriptionCN() {
        return "打开最近任务（任务切换器）界面。";
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
        boolean success = service.openRecentApps();
        return success ? ToolResult.success("Opened recent apps")
                : ToolResult.error("Failed to open recent apps");
    }
}
