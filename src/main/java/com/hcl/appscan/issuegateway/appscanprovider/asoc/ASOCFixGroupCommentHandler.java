package com.hcl.appscan.issuegateway.appscanprovider.asoc;

import com.hcl.appscan.issuegateway.issues.AppScanIssue;
import com.hcl.appscan.issuegateway.issues.PushJobData;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

public class ASOCFixGroupCommentHandler extends ASOCCommentHandler {
    private static final String CUSTOM_COMMENT_TOKEN = "AppScan Issue Management Gateway (Fix Group)";
    private Map<String, List<AppScanIssue>> fixGroupIssuesMap;

    public ASOCFixGroupCommentHandler(Map<String, List<AppScanIssue>> fixGroupIssuesMap) {
        this.fixGroupIssuesMap = fixGroupIssuesMap;
    }

    @Override
    public String getCommentToken() {
        return CUSTOM_COMMENT_TOKEN;
    }

    @Override
    protected void submitComment(PushJobData jobData, List<String> errors, Map.Entry<String, String> result, String fixGroupId) {
        List<AppScanIssue> appScanIssues = fixGroupIssuesMap.get(fixGroupId);
        if (appScanIssues != null) {
            for (AppScanIssue issue : appScanIssues)
                super.submitComment(jobData, errors, result, issue.get(ASOCConstants.ID));
        }
    }
}
