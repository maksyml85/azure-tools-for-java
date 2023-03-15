package com.microsoft.azure.toolkit.intellij.servicebus;

import com.azure.resourcemanager.servicebus.models.EntityStatus;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.servicebus.ServiceBusActionsContributor;
import com.microsoft.azure.toolkit.intellij.common.AzureBundle;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.servicebus.model.ServiceBusInstance;

import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

public class IntelliJServiceBusActionsContributor implements IActionsContributor {
    @Override
    public void registerHandlers(AzureActionManager am) {
        registerActiveActionHandler(am);
        registerDisabledActionHandler(am);
        registerSendDisabledActionHandler(am);
        registerReceiveDisabledActionHandler(am);
        registerCopyConnectionStringActionHandler(am);
    }

    private void registerActiveActionHandler(AzureActionManager am) {
        final BiPredicate<ServiceBusInstance, AnActionEvent> condition = (r, e) -> true;
        final BiConsumer<ServiceBusInstance, AnActionEvent> handler = (c, e) -> c.updateStatus(EntityStatus.ACTIVE);
        am.registerHandler(ServiceBusActionsContributor.ACTIVE_INSTANCE, condition, handler);
    }

    private void registerDisabledActionHandler(AzureActionManager am) {
        final BiPredicate<ServiceBusInstance, AnActionEvent> condition = (r, e) -> true;
        final BiConsumer<ServiceBusInstance, AnActionEvent> handler = (c, e) -> c.updateStatus(EntityStatus.DISABLED);
        am.registerHandler(ServiceBusActionsContributor.DISABLE_INSTANCE, condition, handler);
    }

    private void registerSendDisabledActionHandler(AzureActionManager am) {
        final BiPredicate<ServiceBusInstance, AnActionEvent> condition = (r, e) -> true;
        final BiConsumer<ServiceBusInstance, AnActionEvent> handler = (c, e) -> c.updateStatus(EntityStatus.SEND_DISABLED);
        am.registerHandler(ServiceBusActionsContributor.SEND_DISABLE_INSTANCE, condition, handler);
    }

    private void registerReceiveDisabledActionHandler(AzureActionManager am) {
        final BiPredicate<ServiceBusInstance, AnActionEvent> condition = (r, e) -> true;
        final BiConsumer<ServiceBusInstance, AnActionEvent> handler = (c, e) -> c.updateStatus(EntityStatus.RECEIVE_DISABLED);
        am.registerHandler(ServiceBusActionsContributor.RECEIVE_DISABLE_INSTANCE, condition, handler);
    }

    private void registerCopyConnectionStringActionHandler(AzureActionManager am) {
        final BiPredicate<ServiceBusInstance, AnActionEvent> condition = (r, e) -> true;
        final BiConsumer<ServiceBusInstance, AnActionEvent> handler = (c, e) -> {
            final String connectionString = c.getOrCreateListenConnectionString();
            am.getAction(ResourceCommonActionsContributor.COPY_STRING).handle(connectionString);
            AzureMessager.getMessager().info(AzureBundle.message("azure.eventhubs.info.copyConnectionString"));
        };
        am.registerHandler(ServiceBusActionsContributor.COPY_CONNECTION_STRING, condition, handler);
    }

    @Override
    public int getOrder() {
        return ServiceBusActionsContributor.INITIALIZE_ORDER;
    }
}
