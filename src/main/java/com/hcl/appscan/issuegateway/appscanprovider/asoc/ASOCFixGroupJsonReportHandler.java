package com.hcl.appscan.issuegateway.appscanprovider.asoc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hcl.appscan.issuegateway.issues.AppScanIssue;
import com.hcl.appscan.issuegateway.issues.PushJobData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.datetime.DateFormatter;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ASOCFixGroupJsonReportHandler {
    private Logger logger = LoggerFactory.getLogger(ASOCFixGroupJsonReportHandler.class);

    public void generateJsonReport(AppScanIssue[] appScanIssues, PushJobData jobData, Map<String, List<AppScanIssue>> fixGroupIssuesMap, List<String> errors) {
        ObjectMapper mapper = new ObjectMapper();
        for (AppScanIssue appScanIssue : appScanIssues) {
            if (appScanIssue == null) continue;
            String id = appScanIssue.get(ASOCConstants.ID);
            List<AppScanIssue> issueList = fixGroupIssuesMap.get(id);

            if (issueList == null || issueList.size() == 0) {
                logger.error("Issue List Unavailable for Fix Group: " + id);
                errors.add("Issue List Unavailable for Fix Group: " + id);
                continue;
            }

            File htmlReport = getIssuesHtmlReport(issueList, jobData, mapper, appScanIssue, errors);
            if (htmlReport != null) appScanIssue.setIssuesMetadataDetails(htmlReport);
        }
    }

    private File getIssuesHtmlReport(List<AppScanIssue> issueList, PushJobData jobData, ObjectMapper mapper, AppScanIssue fixGroupObject, List<String> errors) {
        String id = fixGroupObject.get(ASOCConstants.ID);
        StringBuilder sb = new StringBuilder();
        sb.append("<html><head><title>").append("Fix Group Issues Json Report").append("</title>");
        sb.append(getStyleSheet());
        sb.append("</head><body>");
        sb.append("<table style=\"font-weight: bold\">");
        sb.append("<tr><td>").append("Fix Group Id").append("</td><td>:</td><td>").append(id).append("</td></tr>");
        sb.append("<tr><td>").append("Issue Count").append("</td><td>:</td><td>").append(issueList.size()).append("</td></tr>");
        sb.append("</table>");
        sb.append("<b><u>").append("Issues").append("</u></b>");
        sb.append("<ol>");

        JsonNode jsonNode = getIssuesJsonResponse(id, jobData, errors);
        if (jsonNode.isArray()) {
            for (JsonNode node : jsonNode) {
                sb.append("<li>");
                /*sb.append("<table style=\"border: 1px solid black;\">");
                Iterator<String> iterator = node.fieldNames();
                boolean highlight = false;
                while (iterator.hasNext()) {
                    String nextField = iterator.next();
                    JsonNode value = node.get(nextField);
                    sb.append(!highlight ? "<tr>" : "<tr style=\"background-color:rgb(222,222,222)\">");
                    sb.append("<td>").append(nextField).append("</td>");
                    sb.append("<td style=\"width: 100%\">").append(value == null ? null : value.toString()).append("</td>").append("</tr>");
                    highlight = !highlight;
                }
                sb.append("</table>");*/
                List<TableRowConfig> rowConfigList = populateIssueTableConfig(node);
                String tableContent = getTableContent(rowConfigList);
                sb.append(tableContent);
                sb.append("</li>").append("<br/>");
            }
        }

        sb.append("</ol>").append("</body>").append("</html>");

        File f = null;
        FileWriter writer = null;
        try {
            f = File.createTempFile("Issues", ".html");
            writer = new FileWriter(f);
            writer.write(sb.toString());
        } catch (IOException e) {
            logger.error("Error while writing to file for Issue: " + id);
            errors.add("Error while writing to file for Issue: " + id);
        } finally {
            try {
                if (writer != null) writer.close();
            } catch (IOException e) {
            }
        }

        return f;
    }

    private List<TableRowConfig> populateIssueTableConfig(JsonNode jsonNode) {
        List<TableRowConfig> tableRowConfigList = new ArrayList<>();
        tableRowConfigList.add(new TableRowConfig("Id", normalizeData(jsonNode.get("Id")), "Issue ID", null));
        String severityCssClass = "box severity_";
        int sValue = 0;
        if (jsonNode.get("SeverityValue") != null) {
            String severityValue = jsonNode.get("SeverityValue").toString();
            try {
                sValue = Integer.parseInt(severityValue) - 1;
            } catch (NumberFormatException e) {
            }
            sValue = sValue < 0 ? 0 : sValue;
            sValue = sValue > 5 ? 5 : sValue;
        }
        severityCssClass += sValue;
        tableRowConfigList.add(new TableRowConfig("Severity", normalizeData(jsonNode.get("Severity")), "Severity", severityCssClass));
        tableRowConfigList.add(new TableRowConfig("Status", normalizeData(jsonNode.get("Status")), "Status", null));
        tableRowConfigList.add(new TableRowConfig("Classification", normalizeData(jsonNode.get("Classification")), "Classification", null));
        tableRowConfigList.add(new TableRowConfig("FixGroupId", normalizeData(jsonNode.get("FixGroupId")), "Fix Group ID", null));
        tableRowConfigList.add(new TableRowConfig("Location", normalizeData(jsonNode.get("Location")), "Location", null));
        tableRowConfigList.add(new TableRowConfig("SourceFile", normalizeData(jsonNode.get("SourceFile")), "Source File", null));
        tableRowConfigList.add(new TableRowConfig("AvailabilityImpact", normalizeData(jsonNode.get("AvailabilityImpact")), "Availability Impact", null));
        tableRowConfigList.add(new TableRowConfig("ConfidentialityImpact", normalizeData(jsonNode.get("ConfidentialityImpact")), "Confidentiality Impact", null));
        tableRowConfigList.add(new TableRowConfig("IntegrityImpact", normalizeData(jsonNode.get("IntegrityImpact")), "Integrity Impact", null));
        tableRowConfigList.add(new TableRowConfig("DateCreated", getFormattedDate(normalizeData(jsonNode.get("DateCreated"))), "Date Created", null));
        tableRowConfigList.add(new TableRowConfig("LastUpdated", getFormattedDate(normalizeData(jsonNode.get("LastUpdated"))), "Last Updated", null));
        tableRowConfigList.add(new TableRowConfig("Cwe", normalizeData(jsonNode.get("Cwe")), "CWE", null));

        return tableRowConfigList;
    }

    private String getFormattedDate(String value) {
        if (value == null || value.trim().isEmpty()) return "";
        SimpleDateFormat fP = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        SimpleDateFormat formatter = new SimpleDateFormat("E, MMMM d, yyyy");
        try {
            return formatter.format(fP.parse(value));
        } catch (ParseException e) {
            return "";
        }
    }

    private String normalizeData(JsonNode v) {
        if (v == null) return "";
        String s = v.toString();
        s = s.startsWith("\"") ? s.substring(1) : s;
        s = s.endsWith("\"") ? s.substring(0, s.length() - 1) : s;

        return s;
    }

    private String getTableContent(List<TableRowConfig> rowConfigList) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"issueHeader\">");
        for (TableRowConfig tableRowConfig : rowConfigList) {
            sb.append("<div class=\"row\">");
            sb.append("<div class=\"name\"><b>").append(tableRowConfig.fieldDisplayName).append("</b></div>");
            sb.append("<div class=\"value\">");
            if (!StringUtils.isEmpty(tableRowConfig.spanClass))
                sb.append("<span class=\"").append(tableRowConfig.spanClass).append("\">").append(tableRowConfig.value).append("</span>");
            else sb.append(tableRowConfig.value);
            sb.append("</div>");
            sb.append("</div>");
        }
        sb.append("</div>");

        return sb.toString();
    }

    private JsonNode getIssuesJsonResponse(String fixGroupId, PushJobData jobData, List<String> errors) {
        JsonNode items = new ASOCFixGroupRetrievalHandler() {
            private static final String FIX_GROUP_ISSUES = "/api/v2/FixGroups/SCOPE/SID/FID/Issues";

            public JsonNode getItems(String fixGroupId, PushJobData pushJobData, List<String> errors) {
                try {
                    RestTemplate restTemplate = ASOCUtils.createASOCRestTemplate();
                    HttpHeaders headers = ASOCUtils.createASOCAuthorizedHeaders(pushJobData);
                    headers.add("Accept-Language", "en-US,en;q=0.9");
                    HttpEntity<Object> entity = new HttpEntity<>(headers);

                    UriComponentsBuilder urlBuilder = UriComponentsBuilder.fromUriString(pushJobData.getAppscanData().getUrl())
                            .path(FIX_GROUP_ISSUES.replace("SCOPE", ASOCConstants.SCOPE_APPLICATION)
                                    .replace("SID", pushJobData.getAppscanData().getAppid())
                                    .replace("FID", fixGroupId))
                            .queryParam("$filter", getStateFilters(pushJobData.getAppscanData().getIssuestates()))
                            .queryParam("$orderby", "SeverityValue")
                            .queryParam("$inlinecount", ASOCConstants.ALL_PAGES);
                    for (String policyId : getPolicyIds(pushJobData)) {
                        urlBuilder.queryParam("policyId", policyId);
                    }

                    URI theURI = urlBuilder.build().encode().toUri();
                    ResponseEntity<JsonNode> response = restTemplate.exchange(theURI, HttpMethod.GET, entity,
                            JsonNode.class);
                    if (!response.getStatusCode().is2xxSuccessful()) {
                        errors.add("Error: Received a " + response.getStatusCodeValue() + " status code from " + theURI);
                        logger.error("Error: Received a " + response.getStatusCodeValue() + " status code from " + theURI);
                    }
                    JsonNode node = response.getBody();
                    if (node != null && node.has(ASOCConstants.ITEMS)) {
                        return node.get(ASOCConstants.ITEMS);
                    }
                } catch (RestClientException e) {
                    errors.add("Internal Server Error while retrieving AppScan Issues: " + e.getMessage());
                    logger.error("Internal Server Error while retrieving AppScan Issues", e);
                }
                return null;
            }
        }.getItems(fixGroupId, jobData, errors);

        return items;
    }

    private String getStyleSheet() {
        String s = "<style type=\"text/css\" media=\"all, print, screen\">" +
                "\ndiv {\n" +
                "text-overflow: ellipsis;\n" +
                "word-wrap: break-word;\n" +
                "}\n" +
                ".issueHeader .row,\n" +
                ".variant .row,\n" +
                ".row {\n" +
                "display: table-row;\n" +
                "}\n" +
                ".row .name {\n" +
                "display: table-cell;\n" +
                "padding: 4px 8px 4px 0px;\n" +
                "vertical-align: top;\n" +
                "}\n" +
                ".row .value {\n" +
                "display: table-cell;\n" +
                "padding: 4px 0 4px 0;\n" +
                "width: 100%;\n" +
                "vertical-align: top;\n" +
                "}\n" +
                ".issueHeader .row .name {\n" +
                "display: table-cell;\n" +
                "padding: 4px 8px 4px 8px;\n" +
                "border-bottom: 1px solid #CCCCCC;\n" +
                "}\n" +
                ".issueHeader .row .value {\n" +
                "display: table-cell;\n" +
                "padding: 4px 0 4px 0;\n" +
                "border-bottom: 1px solid #CCCCCC;\n" +
                "width: 100%;\n" +
                "word-break: break-all;\n" +
                "}\n" +
                ".issueHeader .row:last-child .name,\n" +
                ".issueHeader .row:last-child .value {\n" +
                "border-bottom: none;\n" +
                "}\n" +
                ".box {\n" +
                "border-radius: 0px;\n" +
                "-moz-border-radius: 0px;\n" +
                "display: inline-block;\n" +
                "padding: 2px 4px 0 4px;\n" +
                "font-weight: bold;\n" +
                "}\n" +
                ".severity_0 {\n" +
                "background-color: #FFFFFF;\n" +
                "color: #000000;\n" +
                "border: 1px solid #BFBFBF;\n" +
                "}\n" +
                ".severity_1 {\n" +
                "background-color: #BFBFBF;\n" +
                "color: #FFFFFF;\n" +
                "}\n" +
                ".severity_2 {\n" +
                "background-color: #FFCB0B;\n" +
                "color: #FFFFFF;\n" +
                "}\n" +
                ".severity_3 {\n" +
                "background-color: #FB3627;\n" +
                "color: #FFFFFF;\n" +
                "}\n" +
                ".severity_4 {\n" +
                "background-color: #471B5E;\n" +
                "color: #FFFFFF;\n" +
                "}\n" +
                ".severity_5 {\n" +
                "background-color: #009900;\n" +
                "color: #FFFFFF;\n" +
                "}" +
                "</style>";

        return s;
    }

    class TableRowConfig {
        private String fieldName;
        private String value;
        private String fieldDisplayName;
        private String spanClass;

        public TableRowConfig(String fieldName, String value, String fieldDisplayName, String spanClass) {
            this.fieldName = fieldName;
            this.value = value;
            this.fieldDisplayName = fieldDisplayName;
            this.spanClass = spanClass;
        }
    }
}
