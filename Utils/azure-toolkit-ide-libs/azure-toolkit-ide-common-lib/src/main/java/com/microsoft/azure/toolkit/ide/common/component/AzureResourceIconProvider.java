/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.ide.common.component;

import com.microsoft.azure.toolkit.ide.common.icon.AzureIcon;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIconProvider;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.model.AzResourceBase;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AzureResourceIconProvider<T extends AzResource<?, ?>> implements AzureIconProvider<T> {

    public static final AzureResourceIconProvider<AzResource<?, ?>> DEFAULT_AZURE_RESOURCE_ICON_PROVIDER = new AzureResourceIconProvider<>();

    private final List<Function<T, AzureIcon.Modifier>> modifierFunctionList = new ArrayList<>();

    public AzureResourceIconProvider() {
        withModifier(AzureResourceIconProvider::getStatusModifier);
    }

    public AzureResourceIconProvider<T> withModifier(@Nonnull Function<T, AzureIcon.Modifier> modifierFunction) {
        this.modifierFunctionList.add(modifierFunction);
        return this;
    }

    @Override
    public AzureIcon getIcon(T resource) {
        final AzResourceBase.FormalStatus formalStatus = resource.getFormalStatus();
        if (formalStatus.isWaiting()) {
            return AzureIcons.Common.REFRESH_ICON;
        }
        final String iconPath = getAzureBaseResourceIconPath(resource);
        final List<AzureIcon.Modifier> modifiers = modifierFunctionList.stream()
                .map(function -> function.apply(resource))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        return AzureIcon.builder().iconPath(iconPath).modifierList(modifiers).build();
    }

    public static <T extends AzResource<?, ?>> String getAzResourceIconPath(T resource) {
        AzResource<?, ?> current = resource;
        final StringBuilder modulePath = new StringBuilder();
        while (!(current instanceof AzResource.None)) {
            modulePath.insert(0, "/" + current.getModule().getName());
            current = current.getParent();
        }
        return String.format("/icons%s/default.svg", modulePath);
    }

    public static <T extends AzResource<?, ?>> String getAzureBaseResourceIconPath(T resource) {
        if (resource instanceof AzResource) {
            return AzureResourceIconProvider.getAzResourceIconPath((AzResource) resource);
        }
        return String.format("/icons/%s/default.svg", resource.getClass().getSimpleName().toLowerCase());
    }

    public static <T extends AzResource<?, ?>> AzureIcon.Modifier getStatusModifier(T resource) {
        final AzResourceBase.FormalStatus formalStatus = resource.getFormalStatus();
        if (formalStatus.isWaiting()) {
            return null;
        } else if (formalStatus.isRunning()) {
            return AzureIcon.Modifier.RUNNING;
        } else if (formalStatus.isFailed()) {
            return AzureIcon.Modifier.FAILED;
        } else if (formalStatus.isStopped()) {
            return AzureIcon.Modifier.STOPPED;
        } else {
            return AzureIcon.Modifier.UNKNOWN;
        }
    }

}
