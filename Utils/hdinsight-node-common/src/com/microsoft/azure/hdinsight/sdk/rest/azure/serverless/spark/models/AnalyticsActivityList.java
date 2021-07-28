/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.rest.azure.serverless.spark.models;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * List of activities.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AnalyticsActivityList {
    /**
     * the list of resource pools.
     */
    @JsonProperty(value = "value", access = JsonProperty.Access.WRITE_ONLY)
    private List<AnalyticsActivity> value;

    /**
     * The nextLink property.
     */
    @JsonProperty(value = "nextLink")
    private String nextLink;

    /**
     * The count property.
     */
    @JsonProperty(value = "count")
    private Integer count;

    /**
     * Get the list of resource pools.
     *
     * @return the value value
     */
    public List<AnalyticsActivity> value() {
        return this.value;
    }

    /**
     * Get the nextLink value.
     *
     * @return the nextLink value
     */
    public String nextLink() {
        return this.nextLink;
    }

    /**
     * Set the nextLink value.
     *
     * @param nextLink the nextLink value to set
     * @return the AnalyticsActivityList object itself.
     */
    public AnalyticsActivityList withNextLink(String nextLink) {
        this.nextLink = nextLink;
        return this;
    }

    /**
     * Get the count value.
     *
     * @return the count value
     */
    public Integer count() {
        return this.count;
    }

    /**
     * Set the count value.
     *
     * @param count the count value to set
     * @return the AnalyticsActivityList object itself.
     */
    public AnalyticsActivityList withCount(Integer count) {
        this.count = count;
        return this;
    }

}
