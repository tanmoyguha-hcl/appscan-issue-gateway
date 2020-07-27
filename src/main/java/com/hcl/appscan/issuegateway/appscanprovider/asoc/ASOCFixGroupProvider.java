package com.hcl.appscan.issuegateway.appscanprovider.asoc;

import com.hcl.appscan.issuegateway.issues.AppScanIssue;
import com.hcl.appscan.issuegateway.issues.PushJobData;
import common.IProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ASOCFixGroupProvider extends ASOCProvider {
    private PushJobData jobData;
    private Map<String, List<AppScanIssue>> fixGroupIssuesMap;
    private static final Logger logger = LoggerFactory.getLogger(ASOCFixGroupProvider.class);

    public ASOCFixGroupProvider(PushJobData jobData) {
        super(jobData);
        this.jobData = jobData;
        fixGroupIssuesMap = new HashMap<>();
    }

    @Override
    public AppScanIssue[] getIssues(List<String> errors) {
        if (!StringUtils.isEmpty(jobData.getAppscanData().getFixGroupId())) {
            AppScanIssue[] scanIssues = new ASOCFixGroupIssueRetrievalHandler().retrieveIssues(jobData, errors);
            if (jobData.getAppscanData().getMaxissues() < 0) jobData.getAppscanData().setMaxissues(scanIssues.length);
            return scanIssues;
        } else {
            AppScanIssue[] appScanFixGroups = new ASOCFixGroupRetrievalHandler().retrieveIssues(jobData, errors);
            try {
                AppScanIssue[] appScanIssues = new ASOCIssueRetrievalHandler().retrieveIssues(jobData, errors);
                populateFixGroupIssuesMap(appScanFixGroups, appScanIssues);
            } catch (Exception e) {
                logger.error("Error While Grouping Issues", e);
            }
            return appScanFixGroups;
        }
    }

    @Override
    public AppScanIssue[] getFilteredIssues(AppScanIssue[] issues, List<String> errors) {
        if (!StringUtils.isEmpty(jobData.getAppscanData().getFixGroupId())) {
            return super.getFilteredIssues(issues, errors);
        } else {
            return new ASOCFixGroupFilterHandler(fixGroupIssuesMap).filterIssues(issues, jobData, errors);
        }
    }

    @Override
    public void retrieveReports(AppScanIssue[] filteredIssues, List<String> errors) {
        if (!StringUtils.isEmpty(jobData.getAppscanData().getFixGroupId())) {
            super.retrieveReports(filteredIssues, errors);
        } else {
            new ASOCFixGroupReportHandler().retrieveReports(filteredIssues, jobData, errors);
        }
    }

    @Override
    public void submitIssuesAndUpdateAppScanProvider(AppScanIssue[] filteredIssues, List<String> errors, Map<String, String> results, IProvider provider) {
        if (!StringUtils.isEmpty(jobData.getAppscanData().getFixGroupId())) {
            super.submitIssuesAndUpdateAppScanProvider(filteredIssues, errors, results, provider);
        } else {
            Map<String, Object> config = new HashMap<>(jobData.getImData().getConfig());
            config.put(ASOCConstants.FIX_GROUP_BASED, jobData.getAppscanData().getFixGroupBased());
            new ASOCFixGroupJsonReportHandler().generateJsonReport(filteredIssues, jobData, fixGroupIssuesMap, errors);
            provider.submitIssues(filteredIssues, config, errors, results);
            new ASOCFixGroupCommentHandler(fixGroupIssuesMap).submitComments(jobData, errors, results);
        }
    }

    private void populateFixGroupIssuesMap(AppScanIssue[] appScanFixGroups, AppScanIssue[] appScanIssues) {
        for (AppScanIssue appScanFixGroup : appScanFixGroups) {
            String id = appScanFixGroup.get(ASOCConstants.ID);
            fixGroupIssuesMap.put(id, new ArrayList<>());
        }
        for (AppScanIssue appScanIssue : appScanIssues) {
            String fixGroupId = appScanIssue.get(ASOCConstants.FIX_GROUP_ID);
            List<AppScanIssue> appScanIssueList = fixGroupIssuesMap.get(fixGroupId);
            if (appScanIssueList != null) appScanIssueList.add(appScanIssue);
        }
    }
}
