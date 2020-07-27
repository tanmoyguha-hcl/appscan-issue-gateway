package com.hcl.appscan.issuegateway.appscanprovider.asoc;

import com.hcl.appscan.issuegateway.issues.AppScanIssue;
import com.hcl.appscan.issuegateway.issues.PushJobData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

public class ASOCFixGroupRetrievalHandler extends ASOCIssueRetrievalHandler {
    private static final String REST_FIX_GROUPS = "/api/v2/FixGroups/SCOPE/ID";
    private static final Logger logger = LoggerFactory.getLogger(ASOCFixGroupRetrievalHandler.class);

    @Override
    public AppScanIssue[] retrieveIssues(PushJobData jobData, List<String> errors) {
        try {
            RestTemplate restTemplate = ASOCUtils.createASOCRestTemplate();
            HttpHeaders headers = ASOCUtils.createASOCAuthorizedHeaders(jobData);
            headers.add("Accept-Language", "en-US,en;q=0.9");
            HttpEntity<Object> entity = new HttpEntity<>(headers);

            UriComponentsBuilder urlBuilder = UriComponentsBuilder.fromUriString(jobData.getAppscanData().getUrl())
                    .path(getURL(jobData))
                    .queryParam("$filter", getStateFilters(jobData.getAppscanData().getIssuestates()))
                    .queryParam("$orderby", "SeverityValue")
                    .queryParam("$inlinecount", ASOCConstants.ALL_PAGES) ;
            for (String policyId : getPolicyIds(jobData)) {
                urlBuilder.queryParam("policyId", policyId);
            }

            URI theURI = urlBuilder.build().encode().toUri();
            ResponseEntity<FixGroupResponse> response = restTemplate.exchange(theURI, HttpMethod.GET, entity,
                    FixGroupResponse.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                errors.add("Error: Received a " + response.getStatusCodeValue() + " status code from " + theURI);
                logger.error("Error: Received a " + response.getStatusCodeValue() + " status code from " + theURI);
            }
            FixGroupResponse fixGroupResponse = response.getBody();
            if (fixGroupResponse != null && fixGroupResponse.getItems() != null) {
                return fixGroupResponse.getItems();
            }
        } catch (RestClientException e) {
            errors.add("Internal Server Error while retrieving AppScan Issues: " + e.getMessage());
            logger.error("Internal Server Error while retrieving AppScan Issues", e);
        }
        // If we get here there were problems, so just return an empty list so nothing
        // bad will happen
        return new AppScanIssue[0];
    }

    protected String getURL(PushJobData jobData) {
        return REST_FIX_GROUPS.replace("SCOPE", ASOCConstants.SCOPE_APPLICATION)
                .replace("ID", jobData.getAppscanData().getAppid());
    }
}
