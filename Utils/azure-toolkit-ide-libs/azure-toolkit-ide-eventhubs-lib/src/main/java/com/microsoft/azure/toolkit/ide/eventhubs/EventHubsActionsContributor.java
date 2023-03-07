package com.microsoft.azure.toolkit.ide.eventhubs;

import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.ActionGroup;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.action.IActionGroup;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.view.IView;
import com.microsoft.azure.toolkit.lib.eventhubs.EventHubsInstance;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;

import java.util.ArrayList;

public class EventHubsActionsContributor implements IActionsContributor {
    public static final int INITIALIZE_ORDER = ResourceCommonActionsContributor.INITIALIZE_ORDER + 1;
    public static final String SERVICE_ACTIONS = "actions.eventhubs.service";
    public static final String NAMESPACE_ACTIONS = "actions.eventhubs.namaspace";
    public static final String INSTANCE_ACTIONS = "actions.eventhubs.instance";
    public static final String SET_STATUS_ACTIONS = "actions.eventhubs.set_status.group";
    public static final Action.Id<EventHubsInstance> ACTIVE_INSTANCE = Action.Id.of("user/eventhubs.active_instance.instance");
    public static final Action.Id<EventHubsInstance> DISABLE_INSTANCE = Action.Id.of("user/eventhubs.disable_instance.instance");
    public static final Action.Id<EventHubsInstance> SEND_DISABLE_INSTANCE = Action.Id.of("user/eventhubs.send_disable_instance.instance");
    public static final Action.Id<EventHubsInstance> COPY_CONNECTION_STRING = Action.Id.of("user/eventhubs.copy_connection_string.instance");
    public static final Action.Id<EventHubsInstance> SEND_MESSAGE_INSTANCE = Action.Id.of("user/eventhubs.open_send_message_panel.instance");
    public static final Action.Id<EventHubsInstance> START_LISTENING_INSTANCE = Action.Id.of("user/eventhubs.start_listening.instance");
    public static final Action.Id<EventHubsInstance> STOP_LISTENING_INSTANCE = Action.Id.of("user/eventhubs.stop_listening.instance");
    public static final Action.Id<ResourceGroup> GROUP_CREATE_EVENT_HUBS = Action.Id.of("user/eventhubs.create_eventhubs.group");
    @Override
    public void registerActions(AzureActionManager am) {
        new Action<>(ACTIVE_INSTANCE)
                .visibleWhen(s -> s instanceof EventHubsInstance)
                .enableWhen(s -> !s.isActive())
                .withLabel("Active")
                .withIdParam(AbstractAzResource::getName)
                .register(am);
        new Action<>(DISABLE_INSTANCE)
                .visibleWhen(s -> s instanceof EventHubsInstance)
                .enableWhen(s -> !s.isDisabled())
                .withLabel("Disabled")
                .withIdParam(AbstractAzResource::getName)
                .register(am);
        new Action<>(SEND_DISABLE_INSTANCE)
                .visibleWhen(s -> s instanceof EventHubsInstance)
                .enableWhen(s -> !s.isSendDisabled())
                .withLabel("SendDisabled")
                .withIdParam(AbstractAzResource::getName)
                .register(am);
        new Action<>(COPY_CONNECTION_STRING)
                .visibleWhen(s -> s instanceof EventHubsInstance)
                .withLabel("Copy Connection String")
                .withIdParam(AbstractAzResource::getName)
                .register(am);
        new Action<>(SEND_MESSAGE_INSTANCE)
                .visibleWhen(s -> s instanceof EventHubsInstance)
                .enableWhen(EventHubsInstance::isActive)
                .withLabel("Send Message")
                .withIdParam(AbstractAzResource::getName)
                .register(am);
        new Action<>(START_LISTENING_INSTANCE)
                .visibleWhen(s -> s instanceof EventHubsInstance)
                .enableWhen(s -> !s.isDisabled() && !s.isListening())
                .withLabel("Start Listening")
                .withIdParam(AbstractAzResource::getName)
                .register(am);
        new Action<>(STOP_LISTENING_INSTANCE)
                .visibleWhen(s -> s instanceof EventHubsInstance && !((EventHubsInstance) s).isDisabled()
                        && ((EventHubsInstance) s).isListening())
                .withLabel("Stop Listening")
                .withIdParam(AbstractAzResource::getName)
                .register(am);
        new Action<>(GROUP_CREATE_EVENT_HUBS)
                .withLabel("Event Hubs")
                .withIdParam(AzResource::getName)
                .visibleWhen(s -> s instanceof ResourceGroup)
                .enableWhen(s -> s.getFormalStatus(true).isConnected())
                .register(am);
    }

    @Override
    public void registerGroups(AzureActionManager am) {
        final IView.Label.Static view = new IView.Label.Static("Set Status");
        final ActionGroup setStatusActionGroup = new ActionGroup(new ArrayList<>(), view);
        setStatusActionGroup.addAction(ACTIVE_INSTANCE);
        setStatusActionGroup.addAction(DISABLE_INSTANCE);
        setStatusActionGroup.addAction(SEND_DISABLE_INSTANCE);
        am.registerGroup(SET_STATUS_ACTIONS, setStatusActionGroup);

        final ActionGroup serviceGroup = new ActionGroup(
                ResourceCommonActionsContributor.REFRESH,
                ResourceCommonActionsContributor.OPEN_AZURE_REFERENCE_BOOK,
                "---",
                ResourceCommonActionsContributor.CREATE_IN_PORTAL
                );
        am.registerGroup(SERVICE_ACTIONS, serviceGroup);

        final ActionGroup namespaceGroup = new ActionGroup(
                ResourceCommonActionsContributor.PIN,
                "---",
                ResourceCommonActionsContributor.REFRESH,
                ResourceCommonActionsContributor.OPEN_AZURE_REFERENCE_BOOK,
                ResourceCommonActionsContributor.OPEN_PORTAL_URL,
                "---",
                ResourceCommonActionsContributor.DELETE);
        am.registerGroup(NAMESPACE_ACTIONS, namespaceGroup);

        final ActionGroup instanceGroup = new ActionGroup(
                ResourceCommonActionsContributor.REFRESH,
                ResourceCommonActionsContributor.OPEN_PORTAL_URL,
                "---",
                COPY_CONNECTION_STRING,
                SET_STATUS_ACTIONS,
                SEND_MESSAGE_INSTANCE,
                START_LISTENING_INSTANCE,
                STOP_LISTENING_INSTANCE,
                "---",
                ResourceCommonActionsContributor.DELETE);
        am.registerGroup(INSTANCE_ACTIONS, instanceGroup);

        final IActionGroup group = am.getGroup(ResourceCommonActionsContributor.RESOURCE_GROUP_CREATE_ACTIONS);
        group.addAction(GROUP_CREATE_EVENT_HUBS);
    }

    @Override
    public int getOrder() {
        return INITIALIZE_ORDER;
    }
}
