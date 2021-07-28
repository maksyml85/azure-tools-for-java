/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.runner.webapp;

import com.microsoft.azure.management.appservice.PricingTier;

public class Constants {
    public static final String LINUX_JAVA_SE_RUNTIME = "JAVA|8-jre8";
    public static final String CREATE_NEW_SLOT = "+ Create new deployment slot";
    public static final String DO_NOT_CLONE_SLOT_CONFIGURATION = "Don't clone configuration from an existing slot";
    public static final String WEBAPP_DEFAULT_PRICING_TIER = new PricingTier("Premium", "P1V2").toString();
}
