package com.hcl.appscan.issuegateway.appscanprovider.asoc;

import com.hcl.appscan.issuegateway.issues.PushJobData;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class ASOCFixGroupReportHandler extends ASOCReportHandler {
    private String reportId = null;
    private File report = null;

    @Override
    protected CreateReportRequest getCreateReportRequest(PushJobData jobData, String issueId) {
        CreateReportRequest createReportRequest = new CreateReportRequest();
        createReportRequest.OdataFilter = "";
        createReportRequest.Configuration = new CreateReportRequestConfiguration();
        createReportRequest.Configuration.TableOfContent = true;
        return createReportRequest;
    }

    @Override
    protected String postReportJob(PushJobData jobData, String issueId, List<String> errors) {
        if (reportId == null)
            reportId = super.postReportJob(jobData, issueId, errors);
        return reportId;
    }

    @Override
    protected File downloadReport(PushJobData jobData, String reportId, List<String> errors) throws IOException {
        if (report == null)
            report = super.downloadReport(jobData, reportId, errors);
        return report;
    }
}
