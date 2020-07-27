package com.hcl.appscan.issuegateway.appscanprovider.asoc;

import com.hcl.appscan.issuegateway.issues.PushJobData;

public class ASOCFixGroupIssueRetrievalHandler extends ASOCFixGroupRetrievalHandler {
    private String URL = "/api/v2/FixGroups/SCOPE/SID/FGID/Issues";

    @Override
    protected String getURL(PushJobData jobData) {
        return URL.replace("SCOPE", ASOCConstants.SCOPE_APPLICATION)
                .replace("SID", jobData.getAppscanData().getAppid())
                .replace("FGID", jobData.getAppscanData().getFixGroupId());
    }
}
