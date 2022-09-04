package com.microsoft.azure.toolkit.ide.guidance.task;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.EmptyAction;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.guidance.ComponentContext;
import com.microsoft.azure.toolkit.ide.guidance.Task;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import lombok.RequiredArgsConstructor;

import javax.annotation.Nonnull;
import java.util.Objects;

@RequiredArgsConstructor
public class FocusResourceInAzureExplorerTask implements Task {
    public static final String RESOURCE_ID = "resourceId";
    public static final String ID = "task.common.focus_resource_in_explorer";

    @Nonnull
    private final ComponentContext context;

    @Override
    @AzureOperation(name = "guidance.focus_resource", type = AzureOperation.Type.SERVICE)
    public void execute() {
        final String resourceId = (String) context.getParameter(RESOURCE_ID);
        final AbstractAzResource<?, ?> resource = Objects.requireNonNull(Azure.az().getById(resourceId), String.format("failed to get resource with id (%s) in Azure", resourceId));
        final DataContext context = dataId -> CommonDataKeys.PROJECT.getName().equals(dataId) ? this.context.getProject() : null;
        final AnActionEvent event = AnActionEvent.createFromAnAction(new EmptyAction(), null, "azure.guidance.summary", context);
        AzureActionManager.getInstance().getAction(ResourceCommonActionsContributor.HIGHLIGHT_RESOURCE_IN_EXPLORER).handle(resource, event);
    }

    @Nonnull
    @Override
    public String getName() {
        return ID;
    }
}
