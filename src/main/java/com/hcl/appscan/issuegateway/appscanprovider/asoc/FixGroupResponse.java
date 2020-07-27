package com.hcl.appscan.issuegateway.appscanprovider.asoc;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.hcl.appscan.issuegateway.issues.AppScanIssue;

public class FixGroupResponse {
    private AppScanIssue[] items;
    private Integer count;
    private String nextPageLink;

    @JsonGetter("Items")
    public AppScanIssue[] getItems() {
        return items;
    }

    @JsonSetter("Items")
    public void setItems(AppScanIssue[] items) {
        this.items = items;
    }

    @JsonGetter("Count")
    public Integer getCount() {
        return count;
    }

    @JsonSetter("Count")
    public void setCount(Integer count) {
        this.count = count;
    }

    @JsonGetter("NextPageLink")
    public String getNextPageLink() {
        return nextPageLink;
    }

    @JsonSetter("NextPageLink")
    public void setNextPageLink(String nextPageLink) {
        this.nextPageLink = nextPageLink;
    }
}
