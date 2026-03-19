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

public class ClickByIdTool extends BaseTool {

    @Override
    public String getName() {
        return "click_by_id";
    }

    @Override
    public String getDisplayName() {
        return ClawApplication.Companion.getInstance().getString(R.string.tool_name_click_by_id);
    }

    @Override
    public String getDescriptionEN() {
        return "Find an element by its resource ID (e.g. 'com.example:id/button') and click on it.";
    }

    @Override
    public String getDescriptionCN() {
        return "通过资源ID查找元素（例如 'com.example:id/button'）并点击。";
    }

    @Override
    public List<ToolParameter> getParameters() {
        return Collections.singletonList(
                new ToolParameter("id", "string", "The resource ID of the element (e.g. 'com.example:id/button')", true)
        );
    }

    @Override
    public ToolResult execute(Map<String, Object> params) {
        ClawAccessibilityService service = ClawAccessibilityService.getInstance();
        if (service == null) {
            return ToolResult.error("Accessibility service is not running");
        }
        String id = requireString(params, "id");
        List<AccessibilityNodeInfo> nodes = service.findNodesById(id);
        if (nodes.isEmpty()) {
            return ToolResult.error("No element found with id: " + id);
        }
        try {
            boolean success = service.clickNode(nodes.get(0));
            return success ? ToolResult.success("Clicked element with id: " + id)
                    : ToolResult.error("Found element but failed to click: " + id);
        } finally {
            ClawAccessibilityService.recycleNodes(nodes);
        }
    }
}
