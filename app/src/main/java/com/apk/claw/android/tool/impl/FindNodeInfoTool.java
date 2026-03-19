package com.apk.claw.android.tool.impl;

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

public class FindNodeInfoTool extends BaseTool {

    @Override
    public String getName() {
        return "find_node_info";
    }

    @Override
    public String getDisplayName() {
        return ClawApplication.Companion.getInstance().getString(R.string.tool_name_find_node_info);
    }

    @Override
    public String getDescriptionEN() {
        return "Find elements by text or resource ID and return their detailed information (class, bounds, properties). Useful for inspecting specific elements before interacting.";
    }

    @Override
    public String getDescriptionCN() {
        return "通过文本或资源ID查找元素，返回详细信息（类名、边界、属性）。适用于在交互前检查特定元素。";
    }

    @Override
    public List<ToolParameter> getParameters() {
        return Arrays.asList(
                new ToolParameter("text", "string", "The text to search for (optional if id is provided)", false),
                new ToolParameter("id", "string", "The resource ID to search for (optional if text is provided)", false)
        );
    }

    @Override
    public ToolResult execute(Map<String, Object> params) {
        ClawAccessibilityService service = ClawAccessibilityService.getInstance();
        if (service == null) {
            return ToolResult.error("Accessibility service is not running");
        }

        String text = optionalString(params, "text", "");
        String id = optionalString(params, "id", "");

        if (text == null && id == null) {
            return ToolResult.error("At least one of 'text' or 'id' must be provided");
        }

        List<AccessibilityNodeInfo> nodes;
        if (id != null) {
            nodes = service.findNodesById(id);
        } else {
            nodes = service.findNodesByText(text);
        }

        if (nodes.isEmpty()) {
            return ToolResult.error("No elements found matching the query");
        }

        try {
            StringBuilder sb = new StringBuilder();
            sb.append("Found ").append(nodes.size()).append(" element(s):\n");
            for (int i = 0; i < nodes.size(); i++) {
                sb.append("[").append(i).append("] ").append(service.getNodeDetail(nodes.get(i))).append("\n");
            }
            return ToolResult.success(sb.toString());
        } finally {
            ClawAccessibilityService.recycleNodes(nodes);
        }
    }
}
