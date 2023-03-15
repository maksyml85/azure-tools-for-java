package com.microsoft.azure.toolkit.ide.servicebus;

import com.azure.resourcemanager.servicebus.models.EntityStatus;
import com.microsoft.azure.toolkit.ide.common.IExplorerNodeProvider;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.common.component.*;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcon;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIconProvider;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.servicebus.AzureServiceBusNamespace;
import com.microsoft.azure.toolkit.lib.servicebus.ServiceBusNamespace;
import com.microsoft.azure.toolkit.lib.servicebus.model.ServiceBusInstance;
import com.microsoft.azure.toolkit.lib.servicebus.queue.ServiceBusQueue;
import com.microsoft.azure.toolkit.lib.servicebus.topic.ServiceBusTopic;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ServiceBusNodeProvider implements IExplorerNodeProvider {
    private static final String NAME = "Service Bus";
    private static final String ICON = AzureIcons.ServiceBus.MODULE.getIconPath();
    private static final AzureIconProvider<ServiceBusInstance> SERVICE_BUS_ICON_PROVIDER =
            new AzureResourceIconProvider<ServiceBusInstance>()
                    .withModifier(ServiceBusNodeProvider::getStatusdModifier);

    @Nullable
    @Override
    public Object getRoot() {
        return Azure.az(AzureServiceBusNamespace.class);
    }

    @Override
    public boolean accept(@Nonnull Object data, @Nullable Node<?> parent, ViewType type) {
        return data instanceof AzureServiceBusNamespace || data instanceof ServiceBusNamespace ||
                data instanceof ServiceBusQueue || data instanceof ServiceBusTopic;
    }

    @Nullable
    @Override
    public Node<?> createNode(@Nonnull Object data, @Nullable Node<?> parent, @Nonnull Manager manager) {
        if (data instanceof AzureServiceBusNamespace) {
            final AzureServiceBusNamespace service = (AzureServiceBusNamespace) data;
            return new Node<>(service).view(new AzureServiceLabelView<>(service, NAME, ICON))
                    .actions(ServiceBusActionsContributor.SERVICE_ACTIONS)
                    .addChildren(this::listServiceBusNamespaces, ((serviceBusNamespace, azureServiceBusNamespaceNode) ->
                            this.createNode(serviceBusNamespace, azureServiceBusNamespaceNode, manager)));
        } else if (data instanceof ServiceBusNamespace) {
            final ServiceBusNamespace serviceBusNamespace = (ServiceBusNamespace) data;
            return new Node<>(serviceBusNamespace).view(new AzureResourceLabelView<>(serviceBusNamespace))
                    .addInlineAction(ResourceCommonActionsContributor.PIN)
                    .actions(ServiceBusActionsContributor.NAMESPACE_ACTIONS)
                    .addChildren(ServiceBusNamespace::getSubModules, (module, parentNode) -> new Node<>(module)
                            .view(new AzureModuleLabelView<>(module, module.getResourceTypeName().replace("Service Bus ", "") + "s"))
                            .addChildren(AbstractAzResourceModule::list, (d, pn) -> this.createNode(d, pn, manager)));
        } else if (data instanceof ServiceBusQueue) {
            final ServiceBusQueue queue = (ServiceBusQueue) data;
            return new Node<>(queue)
                    .view(new AzureResourceLabelView<>(queue, ServiceBusQueue::getStatus, SERVICE_BUS_ICON_PROVIDER))
                    .actions(ServiceBusActionsContributor.QUEUE_ACTIONS);
        } else if (data instanceof ServiceBusTopic) {
            final ServiceBusTopic topic = (ServiceBusTopic) data;
            return new Node<>(topic)
                    .view(new AzureResourceLabelView<>(topic, ServiceBusTopic::getStatus, SERVICE_BUS_ICON_PROVIDER))
                    .actions(ServiceBusActionsContributor.TOPIC_ACTIONS);
        }
        return null;
    }

    private List<ServiceBusNamespace> listServiceBusNamespaces(final AzureServiceBusNamespace azureServiceBusNamespace) {
        return azureServiceBusNamespace.list().stream().flatMap(m -> m.getServiceBusNamespaceModule().list().stream()).collect(Collectors.toList());
    }

    @Nullable
    private static AzureIcon.Modifier getStatusdModifier(ServiceBusInstance resource) {
        final EntityStatus entityStatus = resource.getEntityStatus();
        if (Objects.isNull(entityStatus)) {
            return null;
        }
        switch (entityStatus) {
            case SEND_DISABLED: return AzureIcon.Modifier.SEND_DISABLED;
            case RECEIVE_DISABLED: return AzureIcon.Modifier.RECEIVED_DISABLED;
            default: return null;
        }
    }

}
