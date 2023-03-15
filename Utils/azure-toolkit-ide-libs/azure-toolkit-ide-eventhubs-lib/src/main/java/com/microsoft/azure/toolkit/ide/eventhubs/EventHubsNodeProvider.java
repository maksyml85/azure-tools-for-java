package com.microsoft.azure.toolkit.ide.eventhubs;

import com.microsoft.azure.toolkit.ide.common.IExplorerNodeProvider;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.common.component.AzureResourceIconProvider;
import com.microsoft.azure.toolkit.ide.common.component.AzureResourceLabelView;
import com.microsoft.azure.toolkit.ide.common.component.AzureServiceLabelView;
import com.microsoft.azure.toolkit.ide.common.component.Node;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcon;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIconProvider;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.eventhubs.AzureEventHubsNamespace;
import com.microsoft.azure.toolkit.lib.eventhubs.EventHubsInstance;
import com.microsoft.azure.toolkit.lib.eventhubs.EventHubsNamespace;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class EventHubsNodeProvider implements IExplorerNodeProvider {
    private static final String NAME = "Event Hubs";
    private static final String ICON = AzureIcons.EventHubs.MODULE.getIconPath();
    private static final AzureIconProvider<EventHubsInstance> EVENT_HUBS_ICON_PROVIDER =
            new AzureResourceIconProvider<EventHubsInstance>()
                    .withModifier(EventHubsNodeProvider::getSendDisabledModifier);

    @Nullable
    @Override
    public Object getRoot() {
        return Azure.az(AzureEventHubsNamespace.class);
    }

    @Override
    public boolean accept(@Nonnull Object data, @Nullable Node<?> parent, ViewType type) {
        return data instanceof AzureEventHubsNamespace ||
                data instanceof EventHubsNamespace || data instanceof EventHubsInstance;
    }

    @Nullable
    @Override
    public Node<?> createNode(@Nonnull Object data, @Nullable Node<?> parent, @Nonnull Manager manager) {
        if (data instanceof AzureEventHubsNamespace) {
            final AzureEventHubsNamespace service = (AzureEventHubsNamespace) data;
            return new Node<>(service).view(new AzureServiceLabelView<>(service, NAME, ICON))
                    .actions(EventHubsActionsContributor.SERVICE_ACTIONS)
                    .addChildren(this::listEventHubNamespaces, (eventHubsNamespace, eventHubsNamespaceModule) ->
                            this.createNode(eventHubsNamespace, eventHubsNamespaceModule, manager));
        } else if (data instanceof EventHubsNamespace) {
            final EventHubsNamespace eventHubsNamespace = (EventHubsNamespace) data;
            return new Node<>(eventHubsNamespace).view(new AzureResourceLabelView<>(eventHubsNamespace))
                    .addInlineAction(ResourceCommonActionsContributor.PIN)
                    .doubleClickAction(ResourceCommonActionsContributor.OPEN_PORTAL_URL)
                    .actions(EventHubsActionsContributor.NAMESPACE_ACTIONS)
                    .addChildren(c -> Optional.ofNullable(c).map(EventHubsNamespace::getInstances).orElse(Collections.emptyList()),
                            (eventHubsInstance, eventHubNode) -> this.createNode(eventHubsInstance, eventHubNode, manager));
        } else if (data instanceof EventHubsInstance) {
            final EventHubsInstance eventHubsInstance = (EventHubsInstance) data;
            return new Node<>(eventHubsInstance)
                    .view(new AzureResourceLabelView<>(eventHubsInstance, EventHubsInstance::getStatus, EVENT_HUBS_ICON_PROVIDER))
                    .actions(EventHubsActionsContributor.INSTANCE_ACTIONS);
        }
        return null;
    }

    private List<EventHubsNamespace> listEventHubNamespaces(final AzureEventHubsNamespace azureEventHubsNamespace) {
        return azureEventHubsNamespace.list().stream().flatMap(m -> m.eventHubsNamespaces().list().stream()).collect(Collectors.toList());
    }

    @Nullable
    private static AzureIcon.Modifier getSendDisabledModifier(EventHubsInstance resource) {
        return resource.isSendDisabled() ? AzureIcon.Modifier.SEND_DISABLED : null;
    }
}
