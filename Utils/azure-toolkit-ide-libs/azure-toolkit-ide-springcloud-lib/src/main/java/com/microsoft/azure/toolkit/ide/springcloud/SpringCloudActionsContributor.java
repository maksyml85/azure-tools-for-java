/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.springcloud;

import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.ActionGroup;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.action.IActionGroup;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudApp;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudAppInstance;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudDeployment;

import java.util.Optional;

public class SpringCloudActionsContributor implements IActionsContributor {
    public static final int INITIALIZE_ORDER = ResourceCommonActionsContributor.INITIALIZE_ORDER + 1;

    public static final String APP_ACTIONS = "actions.springcloud.app";
    public static final String CLUSTER_ACTIONS = "actions.springcloud.cluster";
    public static final String SERVICE_ACTIONS = "actions.springcloud.service";
    public static final String APP_INSTANCE_ACTIONS = "actions.springcould.appInstance";
    public static final Action.Id<SpringCloudApp> OPEN_PUBLIC_URL = Action.Id.of("user/springcloud.open_public_url.app");
    public static final Action.Id<SpringCloudApp> OPEN_TEST_URL = Action.Id.of("user/springcloud.open_test_url.app");
    public static final Action.Id<SpringCloudApp> STREAM_LOG_APP = Action.Id.of("user/springcloud.open_stream_log.app");
    public static final Action.Id<SpringCloudAppInstance> STREAM_LOG = Action.Id.of("user/springcloud.open_stream_log.instance");
    public static final Action.Id<SpringCloudApp> ENABLE_REMOTE_DEBUGGING = Action.Id.of("user/springcloud.enable_remote_debugging.app");
    public static final Action.Id<SpringCloudApp> DISABLE_REMOTE_DEBUGGING = Action.Id.of("user/springcloud.disable_remote_debugging.app");
    public static final Action.Id<SpringCloudAppInstance> ATTACH_DEBUGGER = Action.Id.of("user/springcloud.attach_debugger.instance");
    public static final Action.Id<SpringCloudApp> ATTACH_DEBUGGER_APP = Action.Id.of("user/springcloud.attach_debugger.app");
    public static final Action.Id<ResourceGroup> GROUP_CREATE_CLUSTER = Action.Id.of("user/springcloud.create_cluster.group");

    @Override
    public void registerActions(AzureActionManager am) {
        new Action<>(OPEN_PUBLIC_URL)
            .visibleWhen(s -> s instanceof SpringCloudApp)
            .enableWhen(s -> s.getFormalStatus(true).isRunning() && s.isPublicEndpointEnabled())
            .withLabel("Access Public Endpoint")
            .withIcon(AzureIcons.Action.BROWSER.getIconPath())
            .withIdParam(AbstractAzResource::getName)
            .withShortcut("control alt P")
            .withHandler(s -> am.getAction(ResourceCommonActionsContributor.OPEN_URL).handle(s.getApplicationUrl()))
            .register(am);

        new Action<>(OPEN_TEST_URL)
            .visibleWhen(s -> s instanceof SpringCloudApp)
            .enableWhen(s -> s.getFormalStatus(true).isRunning())
            .withLabel("Access Test Endpoint")
            .withIcon(AzureIcons.Action.BROWSER.getIconPath())
            .withIdParam(AbstractAzResource::getName)
            .withHandler(s -> am.getAction(ResourceCommonActionsContributor.OPEN_URL).handle(s.getTestUrl()))
            .register(am);

        new Action<>(STREAM_LOG_APP)
            .visibleWhen(s -> s instanceof SpringCloudApp)
            .withLabel("Start Streaming Logs")
            .withIcon(AzureIcons.Action.LOG.getIconPath())
            .withIdParam(AbstractAzResource::getName)
            .register(am);

        new Action<>(STREAM_LOG)
            .visibleWhen(s -> s instanceof SpringCloudAppInstance)
            .withLabel("Start Streaming Logs")
            .withIcon(AzureIcons.Action.LOG.getIconPath())
            .withIdParam(AbstractAzResource::getName)
            .register(am);

        new Action<>(GROUP_CREATE_CLUSTER)
            .visibleWhen(s -> s instanceof ResourceGroup)
            .enableWhen(s -> s.getFormalStatus(true).isConnected())
            .withLabel("Spring Apps")
            .withIdParam(AbstractAzResource::getName)
            .register(am);

        new Action<>(ENABLE_REMOTE_DEBUGGING)
            .visibleWhen(s -> s instanceof SpringCloudApp && ((SpringCloudApp) s).getFormalStatus(true).isRunning() && Optional.ofNullable(((SpringCloudApp) s).getActiveDeployment()).map(deployment -> !deployment.isRemoteDebuggingEnabled()).orElse(false))
            .withLabel("Enable Remote Debugging")
            .withIdParam(AbstractAzResource::getName)
            .register(am);

        new Action<>(DISABLE_REMOTE_DEBUGGING)
            .visibleWhen(s -> s instanceof SpringCloudApp && ((SpringCloudApp) s).getFormalStatus(true).isRunning() && Optional.ofNullable(((SpringCloudApp) s).getActiveDeployment()).map(SpringCloudDeployment::isRemoteDebuggingEnabled).orElse(false))
            .withLabel("Disable Remote Debugging")
            .withIdParam(AbstractAzResource::getName)
            .register(am);

        new Action<>(ATTACH_DEBUGGER_APP)
            .visibleWhen(s -> s instanceof SpringCloudApp)
            .enableWhen(s -> s.getFormalStatus(true).isRunning())
            .withLabel("Attach Debugger")
            .withIcon(AzureIcons.Action.ATTACH_DEBUGGER.getIconPath())
            .withIdParam(AbstractAzResource::getName)
            .register(am);

        new Action<>(ATTACH_DEBUGGER)
            .visibleWhen(s -> s instanceof SpringCloudAppInstance)
            .enableWhen(s -> s.getParent().getParent().getFormalStatus(true).isRunning())
            .withLabel("Attach Debugger")
            .withIcon(AzureIcons.Action.ATTACH_DEBUGGER.getIconPath())
            .withIdParam(AbstractAzResource::getName)
            .register(am);
    }

    @Override
    public void registerGroups(AzureActionManager am) {
        final ActionGroup serviceActionGroup = new ActionGroup(
            ResourceCommonActionsContributor.REFRESH,
            ResourceCommonActionsContributor.OPEN_AZURE_REFERENCE_BOOK,
            "---",
            ResourceCommonActionsContributor.CREATE
        );
        am.registerGroup(SERVICE_ACTIONS, serviceActionGroup);

        final ActionGroup clusterActionGroup = new ActionGroup(
            ResourceCommonActionsContributor.PIN,
            "---",
            ResourceCommonActionsContributor.REFRESH,
            ResourceCommonActionsContributor.OPEN_AZURE_REFERENCE_BOOK,
            ResourceCommonActionsContributor.OPEN_PORTAL_URL,
            "---",
            ResourceCommonActionsContributor.CREATE
        );
        am.registerGroup(CLUSTER_ACTIONS, clusterActionGroup);

        final ActionGroup appActionGroup = new ActionGroup(
            ResourceCommonActionsContributor.PIN,
            "---",
            ResourceCommonActionsContributor.REFRESH,
            ResourceCommonActionsContributor.OPEN_PORTAL_URL,
            SpringCloudActionsContributor.OPEN_PUBLIC_URL,
            SpringCloudActionsContributor.OPEN_TEST_URL,
            ResourceCommonActionsContributor.SHOW_PROPERTIES,
            "---",
            ResourceCommonActionsContributor.DEPLOY,
            "---",
            SpringCloudActionsContributor.ATTACH_DEBUGGER_APP,
            SpringCloudActionsContributor.ENABLE_REMOTE_DEBUGGING,
            SpringCloudActionsContributor.DISABLE_REMOTE_DEBUGGING,
            "---",
            ResourceCommonActionsContributor.START,
            ResourceCommonActionsContributor.STOP,
            ResourceCommonActionsContributor.RESTART,
            ResourceCommonActionsContributor.DELETE,
            "---",
            SpringCloudActionsContributor.STREAM_LOG_APP
        );
        am.registerGroup(APP_ACTIONS, appActionGroup);

        final ActionGroup appInstanceGroup = new ActionGroup(
            ResourceCommonActionsContributor.REFRESH,
            SpringCloudActionsContributor.ATTACH_DEBUGGER,
            SpringCloudActionsContributor.STREAM_LOG
        );
        am.registerGroup(APP_INSTANCE_ACTIONS, appInstanceGroup);

        final IActionGroup group = am.getGroup(ResourceCommonActionsContributor.RESOURCE_GROUP_CREATE_ACTIONS);
        group.addAction(GROUP_CREATE_CLUSTER);
    }

    @Override
    public int getOrder() {
        return INITIALIZE_ORDER;
    }
}
