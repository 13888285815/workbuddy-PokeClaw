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

public class PressBackTool extends BaseTool {

    @Override
    public String getName() {
        return "press_back";
    }

    @Override
    public String getDisplayName() {
        return ClawApplication.Companion.getInstance().getString(R.string.tool_name_press_back);
    }

    @Override
    public String getDescriptionEN() {
        return "Press the Back button to navigate back.";
    }

    @Override
    public String getDescriptionCN() {
        return "按下返回键进行返回导航。";
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
        boolean success = service.pressBack();
        return success ? ToolResult.success("Pressed Back button")
                : ToolResult.error("Failed to press Back button");
    }
}
