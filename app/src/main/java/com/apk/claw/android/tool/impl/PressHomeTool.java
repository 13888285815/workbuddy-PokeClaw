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

public class PressHomeTool extends BaseTool {

    @Override
    public String getName() {
        return "press_home";
    }

    @Override
    public String getDisplayName() {
        return ClawApplication.Companion.getInstance().getString(R.string.tool_name_press_home);
    }

    @Override
    public String getDescriptionEN() {
        return "Press the Home button to go to the home screen.";
    }

    @Override
    public String getDescriptionCN() {
        return "按下Home键回到桌面。";
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
        boolean success = service.pressHome();
        return success ? ToolResult.success("Pressed Home button")
                : ToolResult.error("Failed to press Home button");
    }
}
