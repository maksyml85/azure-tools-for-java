/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.runner.container.webapponlinux;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.project.Project;
import com.microsoft.intellij.runner.container.AzureDockerSupportConfigurationType;

import com.microsoft.intellij.util.PluginUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

public class WebAppOnLinuxDeployConfigurationFactory extends ConfigurationFactory {
    private static final String FACTORY_NAME = "Web App for Containers";
    private static final String ICON_PATH = "/icons/PublishWebAppOnLinux_16.png";

    public WebAppOnLinuxDeployConfigurationFactory(AzureDockerSupportConfigurationType configurationType) {
        super(configurationType);
    }

    @NotNull
    @Override
    public RunConfiguration createTemplateConfiguration(@NotNull Project project) {
        return new WebAppOnLinuxDeployConfiguration(project, this, String.format("%s: %s", FACTORY_NAME, project
                .getName()));
    }

    @Override
    public String getName() {
        return FACTORY_NAME;
    }

    @Override
    public RunConfiguration createConfiguration(String name, RunConfiguration template) {
        return new WebAppOnLinuxDeployConfiguration(template.getProject(), this, name);
    }

    @Override
    public Icon getIcon() {
        return PluginUtil.getIcon(ICON_PATH);
    }
}
