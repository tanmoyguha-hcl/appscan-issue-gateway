package com.hcl.appscan.issuegateway.appscanprovider.asoc;

import com.hcl.appscan.issuegateway.appscanprovider.FilterHandler;
import com.hcl.appscan.issuegateway.issues.AppScanIssue;
import com.hcl.appscan.issuegateway.issues.PushJobData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ASOCFixGroupFilterHandler extends FilterHandler {
    private Map<String, List<AppScanIssue>> fixGroupIssuesMap;

    public ASOCFixGroupFilterHandler(Map<String, List<AppScanIssue>> fixGroupIssuesMap) {
        this.fixGroupIssuesMap = fixGroupIssuesMap;
    }

    @Override
    protected AppScanIssue[] filterOutPreviouslyHandledIssues(List<AppScanIssue> fixGroupIssues, PushJobData jobData, List<String> errors) throws Exception {
        List<AppScanIssue> filteredFixGroup = new ArrayList<>();
        final int maxIssueCount = jobData.getAppscanData().getMaxissues();
        int issueCount = 0;
        boolean checkDuplicates = shouldCheckDuplicates(jobData);
        ASOCFixGroupCommentHandler commentHandler = new ASOCFixGroupCommentHandler(fixGroupIssuesMap);
        for (AppScanIssue fixGroup : fixGroupIssues) {
            boolean foundOurComment = false;
            if (checkDuplicates) {
                List<AppScanIssue> appScanIssues = fixGroupIssuesMap.get(fixGroup.get(ASOCConstants.ID));
                for (AppScanIssue issue : appScanIssues) {
                    for (String comment : commentHandler.getComments(issue, jobData, errors)) {
                        if (comment.startsWith(commentHandler.getCommentToken())) {
                            foundOurComment = true;
                            break;
                        }
                    }
                    if (foundOurComment) break;
                }
            }
            if (!foundOurComment) {
                filteredFixGroup.add(fixGroup);
                issueCount++;
                if (issueCount >= maxIssueCount) {
                    break;
                }
            }
        }
        return filteredFixGroup.toArray(new AppScanIssue[0]);
    }
}
