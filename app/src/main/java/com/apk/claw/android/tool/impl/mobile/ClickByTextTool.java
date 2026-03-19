package com.apk.claw.android.tool.impl.mobile;

import android.view.accessibility.AccessibilityNodeInfo;

import com.apk.claw.android.ClawApplication;
import com.apk.claw.android.R;
import com.apk.claw.android.service.ClawAccessibilityService;
import com.apk.claw.android.tool.BaseTool;
import com.apk.claw.android.tool.ToolParameter;
import com.apk.claw.android.tool.ToolResult;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ClickByTextTool extends BaseTool {

    @Override
    public String getName() {
        return "click_by_text";
    }

    @Override
    public String getDisplayName() {
        return ClawApplication.Companion.getInstance().getString(R.string.tool_name_click_by_text);
    }

    @Override
    public String getDescriptionEN() {
        return "Find an element by its visible text and click on it. Useful when you know the text label of a button or link.";
    }

    @Override
    public String getDescriptionCN() {
        return "通过可见文本查找元素并点击。适用于已知按钮或链接的文本标签时使用。";
    }

    @Override
    public List<ToolParameter> getParameters() {
        return Collections.singletonList(
                new ToolParameter("text", "string", "The visible text of the element to click", true)
        );
    }

    @Override
    public ToolResult execute(Map<String, Object> params) {
        ClawAccessibilityService service = ClawAccessibilityService.getInstance();
        if (service == null) {
            return ToolResult.error("Accessibility service is not running");
        }
        String text = requireString(params, "text");
        List<AccessibilityNodeInfo> nodes = service.findNodesByText(text);
        if (nodes.isEmpty()) {
            return ToolResult.error("No element found with text: " + text);
        }
        try {
            boolean success = service.clickNode(nodes.get(0));
            return success ? ToolResult.success("Clicked element with text: " + text)
                    : ToolResult.error("Found element but failed to click: " + text);
        } finally {
            ClawAccessibilityService.recycleNodes(nodes);
        }
    }
}
