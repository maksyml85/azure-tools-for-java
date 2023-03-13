/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.springcloud.deplolyment;

import com.azure.resourcemanager.appplatform.models.DeploymentInstance;
import com.intellij.execution.DefaultExecutionResult;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.ide.springcloud.SpringCloudActionsContributor;
import com.microsoft.azure.toolkit.intellij.common.messager.IntellijAzureMessager;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessage;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.IArtifact;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.springcloud.*;
import com.microsoft.azure.toolkit.lib.springcloud.config.SpringCloudAppConfig;
import com.microsoft.azure.toolkit.lib.springcloud.task.DeploySpringCloudAppTask;
import com.microsoft.azure.toolkit.intellij.common.RunProcessHandler;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.*;

import static com.microsoft.azure.toolkit.lib.common.messager.AzureMessageBundle.message;

public class SpringCloudDeploymentConfigurationState implements RunProfileState {
    private static final int GET_URL_TIMEOUT = 60;
    private static final int GET_STATUS_TIMEOUT = 180;
    private static final String UPDATE_APP_WARNING = "It may take some moments for the configuration to be applied at server side!";
    private static final String GET_DEPLOYMENT_STATUS_TIMEOUT = "Deployment succeeded but the app is still starting, " +
        "you can check the app status from Azure Portal.";
    private static final String NOTIFICATION_TITLE = "Get deployment status";

    private final SpringCloudDeploymentConfiguration config;
    private final Project project;

    public SpringCloudDeploymentConfigurationState(Project project, SpringCloudDeploymentConfiguration configuration) {
        this.config = configuration;
        this.project = project;
    }

    @Override
    public @Nullable ExecutionResult execute(Executor executor, @Nonnull ProgramRunner<?> runner) {
        final Action<Void> retry = Action.retryFromFailure(() -> this.execute(executor, runner));
        final RunProcessHandler processHandler = new RunProcessHandler();
        processHandler.addDefaultListener();
        processHandler.startNotify();
        final ConsoleView consoleView = TextConsoleBuilderFactory.getInstance().createBuilder(this.project).getConsole();
        final ConsoleMessager messager = new ConsoleMessager(consoleView);
        consoleView.attachToProcess(processHandler);
        final Runnable execute = () -> {
            try {
                final SpringCloudDeployment springCloudDeployment = this.execute(messager);
                messager.success("Deploy succeed!");
                AzureTaskManager.getInstance().runInBackground("get deployment status", () -> {
                    final SpringCloudApp app = springCloudDeployment.getParent();
                    if (!springCloudDeployment.waitUntilReady(GET_STATUS_TIMEOUT)) {
                        messager.warning(GET_DEPLOYMENT_STATUS_TIMEOUT, NOTIFICATION_TITLE, getOpenStreamingLogAction(springCloudDeployment));
                    }
                });
            } catch (final Exception e) {
                messager.error(e, "Azure", retry, getOpenStreamingLogAction(getDeploymentFromConfig()));
            }
        };
        final Disposable subscribe = Mono.fromRunnable(execute)
            .doOnTerminate(processHandler::notifyComplete)
            .subscribeOn(Schedulers.boundedElastic())
            .subscribe();
        processHandler.addProcessListener(new ProcessAdapter() {
            @Override
            public void processTerminated(@Nonnull ProcessEvent event) {
                subscribe.dispose();
            }
        });

        return new DefaultExecutionResult(consoleView, processHandler);
    }

    @AzureOperation(name = "user/springcloud.deploy_app.app", params = {"this.config.getAppConfig().getAppName()"})
    public SpringCloudDeployment execute(IAzureMessager messager) {
        OperationContext.current().setMessager(messager);
        OperationContext.current().setTelemetryProperties(getTelemetryProperties());
        final SpringCloudAppConfig appConfig = this.config.getAppConfig();
        if (Optional.ofNullable(this.config.getAppConfig().getDeployment().getArtifact()).map(IArtifact::getFile).filter(File::exists).isEmpty()) {
            final Action.Id<Void> REOPEN = Action.Id.of("user/springcloud.reopen_deploy_dialog");
            final Action<Void> action = new Action<>(REOPEN)
                .withLabel("Add BeforeRunTask")
                .withHandler((v) -> DeploySpringCloudAppAction.deploy(this.config, this.project));
            throw new AzureToolkitRuntimeException(
                message("springcloud.deploy_app.no_artifact").toString(),
                message("springcloud.deploy_app.no_artifact.tips").toString(),
                action);
        }
        final DeploySpringCloudAppTask task = new DeploySpringCloudAppTask(appConfig);
        final SpringCloudDeployment deployment = task.execute();
        final SpringCloudApp app = deployment.getParent();
        printPublicUrl(app);
        return deployment;
    }

    private void printPublicUrl(final SpringCloudApp app) {
        final IAzureMessager messager = AzureMessager.getMessager();
        if (!app.isPublicEndpointEnabled()) {
            return;
        }
        messager.info(String.format("Getting public url of app(%s)...", app.getName()));
        String publicUrl = app.getApplicationUrl();
        if (StringUtils.isEmpty(publicUrl)) {
            publicUrl = Utils.pollUntil(() -> {
                app.refresh();
                return app.getApplicationUrl();
            }, StringUtils::isNotBlank, GET_URL_TIMEOUT);
        }
        if (StringUtils.isEmpty(publicUrl)) {
            messager.warning("Failed to get application url", NOTIFICATION_TITLE);
        } else {
            messager.info(String.format("Application url: %s", publicUrl));
        }
    }

    private @Nullable SpringCloudDeployment getDeploymentFromConfig() {
        final SpringCloudAppConfig appConfig = this.config.getAppConfig();
        final String clusterName = appConfig.getClusterName();
        final String appName = appConfig.getAppName();
        final String resourceGroup = appConfig.getResourceGroup();
        return Optional.ofNullable(Azure.az(AzureSpringCloud.class)
                        .clusters(appConfig.getSubscriptionId())
                        .get(clusterName, resourceGroup)).map(springCloudCluster -> springCloudCluster.apps().get(appName, resourceGroup))
                .map(SpringCloudApp::getActiveDeployment).orElse(null);
    }
    @Nullable
    private Action<SpringCloudAppInstance> getOpenStreamingLogAction(@Nullable SpringCloudDeployment deployment) {
        final List<SpringCloudAppInstance> instances = Optional.ofNullable(deployment)
                .map(SpringCloudDeployment::getInstances)
                .orElse(Collections.emptyList());
        final SpringCloudAppInstance appInstance = instances.stream().max((o1, o2) -> {
            final DeploymentInstance remote1 = o1.getRemote();
            final DeploymentInstance remote2 = o2.getRemote();
            if (Objects.isNull(remote1)) {
                return -1;
            } else if (Objects.isNull(remote2)) {
                return 1;
            }
            return StringUtils.compare(remote1.startTime(), remote2.startTime());
        }).orElse(null);
        if (Objects.isNull(appInstance)) {
            return null;
        }
        return AzureActionManager.getInstance().getAction(SpringCloudActionsContributor.STREAM_LOG).bind(appInstance);
    }

    protected Map<String, String> getTelemetryProperties() {
        final Map<String, String> props = new HashMap<>();
        final SpringCloudAppConfig cfg = config.getAppConfig();
        props.put("runtime", String.valueOf(cfg.getRuntimeVersion()));
        props.put("subscriptionId", String.valueOf(cfg.getSubscriptionId()));
        props.put("public", String.valueOf(cfg.isPublic()));
        props.put("jvmOptions", String.valueOf(StringUtils.isNotEmpty(cfg.getDeployment().getJvmOptions())));
        props.put("instanceCount", String.valueOf(cfg.getDeployment().getInstanceCount()));
        props.put("memory", String.valueOf(cfg.getDeployment().getMemoryInGB()));
        props.put("cpu", String.valueOf(cfg.getDeployment().getCpu()));
        props.put("persistentStorage", String.valueOf(cfg.getDeployment().getEnablePersistentStorage()));
        return props;
    }

    @RequiredArgsConstructor
    private static class ConsoleMessager extends IntellijAzureMessager {
        private final ConsoleView consoleView;

        @Override
        public boolean show(IAzureMessage raw) {
            if (raw.getType() == IAzureMessage.Type.INFO) {
                println(raw.getMessage().toString(), ConsoleViewContentType.NORMAL_OUTPUT);
                return true;
            } else if (raw.getType() == IAzureMessage.Type.SUCCESS) {
                println(raw.getMessage().toString(), ConsoleViewContentType.NORMAL_OUTPUT);
            } else if (raw.getType() == IAzureMessage.Type.DEBUG) {
                println(raw.getMessage().toString(), ConsoleViewContentType.LOG_DEBUG_OUTPUT);
                return true;
            } else if (raw.getType() == IAzureMessage.Type.WARNING) {
                println(raw.getMessage().toString(), ConsoleViewContentType.LOG_WARNING_OUTPUT);
            } else if (raw.getType() == IAzureMessage.Type.ERROR) {
                println(raw.getMessage().toString(), ConsoleViewContentType.ERROR_OUTPUT);
            }
            return super.show(raw);
        }

        private void println(String originText, ConsoleViewContentType type) {
            consoleView.print(originText + System.lineSeparator(), type);
        }
    }
}
