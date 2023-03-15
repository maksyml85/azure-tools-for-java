/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.springcloud.input;

import com.microsoft.azure.toolkit.ide.guidance.ComponentContext;
import com.microsoft.azure.toolkit.ide.guidance.config.InputConfig;
import com.microsoft.azure.toolkit.ide.guidance.input.GuidanceInput;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudCluster;

import javax.annotation.Nonnull;

public class SpringAppClusterInput  implements GuidanceInput<SpringCloudCluster> {
    public static final String SUBSCRIPTION_ID = "subscriptionId";
    public static final String SPRING_APP_CLUSTER = "springAppCluster";
    private final InputConfig config;
    private final ComponentContext context;

    private final SpringAppClusterPanel panel;

    public SpringAppClusterInput(@Nonnull InputConfig config, @Nonnull ComponentContext context) {
        this.config = config;
        this.context = context;
        this.panel = new SpringAppClusterPanel();

        this.panel.setSubscriptionId((String) context.getParameter(SUBSCRIPTION_ID));
        context.addPropertyListener(SUBSCRIPTION_ID, id -> panel.setSubscriptionId((String) id));
    }

    @Override
    public String getDescription() {
        return config.getDescription();
    }

    @Override
    public SpringAppClusterPanel getComponent() {
        return panel;
    }

    @Override
    public void applyResult() {
        context.applyResult(SPRING_APP_CLUSTER, panel.getValue());
    }
}

