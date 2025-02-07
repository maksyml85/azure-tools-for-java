/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.function.remotedebug;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.intellij.execution.BeforeRunTask;
import com.intellij.execution.BeforeRunTaskProvider;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.remote.RemoteConfiguration;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.util.Key;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.microsoft.azure.toolkit.ide.appservice.function.remotedebugging.FunctionPortForwarder;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.function.AzureFunctions;
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionAppBase;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

public class FunctionPortForwardingTaskProvider extends BeforeRunTaskProvider<FunctionPortForwardingTaskProvider.FunctionPortForwarderBeforeRunTask> {
    private static final String NAME_TEMPLATE = "Attach to %s";
    private static final Key<FunctionPortForwarderBeforeRunTask> ID = Key.create("FunctionPortForwardingTaskProviderId");
    private static final Icon ICON = IntelliJAzureIcons.getIcon(AzureIcons.Action.REMOTE_DEBUG);
    @Getter
    public Key<FunctionPortForwarderBeforeRunTask> id = ID;
    @Getter
    public String name = String.format(NAME_TEMPLATE, "function app");
    @Getter
    public Icon icon = ICON;

    @Override
    public @Nullable
    Icon getTaskIcon(FunctionPortForwarderBeforeRunTask task) {
        return ICON;
    }

    @Nullable
    @Override
    public FunctionPortForwarderBeforeRunTask createTask(@NotNull RunConfiguration runConfiguration) {
        return new FunctionPortForwarderBeforeRunTask(runConfiguration);
    }

    @Override
    public boolean executeTask(@NotNull DataContext context, @NotNull RunConfiguration configuration, @NotNull ExecutionEnvironment environment, @Nonnull FunctionPortForwarderBeforeRunTask task) {
        if (configuration instanceof RemoteConfiguration) {
            return task.startPortForwarding(Integer.parseInt(((RemoteConfiguration) configuration).PORT));
        }
        return false;
    }

    @Override
    public @Nls(capitalization = Nls.Capitalization.Sentence) String getDescription(FunctionPortForwarderBeforeRunTask task) {
        final ResourceId resourceId = StringUtils.isEmpty(task.getTargetResourceId()) ? null : ResourceId.fromString(task.getTargetResourceId());
        return Objects.isNull(resourceId) ? name : String.format(NAME_TEMPLATE, resourceId.name());
    }

    @Getter
    @Setter
    public static class FunctionPortForwarderBeforeRunTask extends BeforeRunTask<FunctionPortForwarderBeforeRunTask>
            implements PersistentStateComponent<FunctionPortForwarderConfig> {
        private final RunConfiguration config;
        private FunctionPortForwarder forwarder;
        private FunctionPortForwarderConfig portForwarderConfig = new FunctionPortForwarderConfig();

        protected FunctionPortForwarderBeforeRunTask(RunConfiguration config) {
            super(ID);
            this.config = config;
        }

        public void setTarget(FunctionAppBase<?, ?, ?> target) {
            this.portForwarderConfig.setResourceId(target.getId());
        }

        public String getTargetResourceId() {
            return Optional.ofNullable(this.portForwarderConfig).map(FunctionPortForwarderConfig::getResourceId).orElse(null);
        }

        public boolean startPortForwarding(int localPort) {
            if (this.config instanceof RemoteConfiguration && StringUtils.isNotEmpty(portForwarderConfig.getResourceId())) {
                return AzureTaskManager.getInstance().runInBackgroundAsObservable(AzureString.format("Start port forward for remote debugging"), () -> {
                    try {
                        final FunctionAppBase<?, ?, ?> target = Azure.az(AzureFunctions.class).getById(portForwarderConfig.getResourceId());
                        Objects.requireNonNull(target).ping();
                        this.forwarder = new FunctionPortForwarder(target);
                        this.forwarder.initLocalSocket(localPort);
                        AzureTaskManager.getInstance().runOnPooledThread(() -> this.forwarder.startForward(localPort));
                        return true;
                    } catch (final IOException | RuntimeException e) {
                        AzureMessager.getMessager().error(e);
                        return false;
                    }
                }).toBlocking().last();
            }
            return false;
        }

        @Override
        public FunctionPortForwarderConfig getState() {
            return this.portForwarderConfig;
        }

        @Override
        public void loadState(@NotNull FunctionPortForwarderConfig state) {
            XmlSerializerUtil.copyBean(state, this.portForwarderConfig);
        }

    }

    @Data
    public static class FunctionPortForwarderConfig {
        private String resourceId;
    }
}
