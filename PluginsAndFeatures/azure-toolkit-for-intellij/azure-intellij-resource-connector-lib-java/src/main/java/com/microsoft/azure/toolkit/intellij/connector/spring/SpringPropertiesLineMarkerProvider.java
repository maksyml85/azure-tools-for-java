/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.spring;

import com.intellij.codeInsight.daemon.GutterIconNavigationHandler;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.ide.DataManager;
import com.intellij.lang.properties.psi.impl.PropertyImpl;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.psi.PsiElement;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.ConnectionManager;
import com.microsoft.azure.toolkit.intellij.connector.Resource;
import com.microsoft.azure.toolkit.lib.common.messager.ExceptionNotification;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Objects;

public class SpringPropertiesLineMarkerProvider implements LineMarkerProvider {

    @Override
    @Nullable
    @ExceptionNotification
    public LineMarkerInfo<PsiElement> getLineMarkerInfo(@Nonnull PsiElement element) {
        if (!(element instanceof PropertyImpl)) {
            return null;
        }
        final String propKey = ((PropertyImpl) element).getKey();
        final String propVal = ((PropertyImpl) element).getValue();
        final Module module = ModuleUtil.findModuleForFile(element.getContainingFile().getVirtualFile(), element.getProject());
        if (Objects.isNull(module)) {
            return null;
        }
        final ImmutablePair<String, String> keyProp = new ImmutablePair<>(propKey, propVal);
        final List<Connection<?, ?>> connections = element.getProject().getService(ConnectionManager.class)
                .getConnectionsByConsumerId(module.getName());
        for (final Connection<?, ?> connection : connections) {
            final List<Pair<String, String>> properties = SpringSupported.getProperties(connection);
            if (!properties.isEmpty() && properties.get(0).equals(keyProp)) {
                final Resource<?> r = connection.getResource();
                return new LineMarkerInfo<>(element, element.getTextRange(),
                        IntelliJAzureIcons.getIcon(AzureIcons.Connector.CONNECT),
                        element2 -> String.format("%s (%s)", r.getName(), r.getDefinition().getTitle()),
                        new SpringDatasourceNavigationHandler(r),
                        GutterIconRenderer.Alignment.LEFT, () -> "");
            }
        }
        return null;
    }

    @RequiredArgsConstructor
    public static class SpringDatasourceNavigationHandler implements GutterIconNavigationHandler<PsiElement> {
        private final Resource<?> resource;

        @Override
        @ExceptionNotification
        @AzureOperation(name = "user/connector.navigate_from_line_marker")
        public void navigate(MouseEvent mouseEvent, PsiElement psiElement) {
            final DataContext context = DataManager.getInstance().getDataContext(mouseEvent.getComponent());
            final AnActionEvent event = AnActionEvent.createFromInputEvent(mouseEvent, ActionPlaces.EDITOR_GUTTER, null, context);
            this.resource.navigate(event);
        }
    }
}
