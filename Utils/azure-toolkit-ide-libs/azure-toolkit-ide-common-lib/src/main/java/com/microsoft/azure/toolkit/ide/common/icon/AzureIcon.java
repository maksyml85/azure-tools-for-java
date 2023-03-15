/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.common.icon;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FilenameUtils;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
public class AzureIcon {

    private String iconPath;
    private List<Modifier> modifierList;

    public static final String getIconPathWithModifier(@Nonnull final AzureIcon azureIcon) {
        if (CollectionUtils.isEmpty(azureIcon.getModifierList())) {
            return azureIcon.getIconPath();
        }
        final File parent = new File(azureIcon.getIconPath()).getParentFile();
        final String iconName = azureIcon.getModifierList().stream().filter(Objects::nonNull).map(Modifier::getIconPath).collect(Collectors.joining("-"));
        final String path = new File(parent, String.format("%s.svg", iconName)).getPath();
        return FilenameUtils.separatorsToUnix(path);
    }

    @Getter
    @EqualsAndHashCode
    public static class Modifier {
        public static final Modifier LINUX = new AzureIcon.Modifier("linux", ModifierLocation.BOTTOM_LEFT);
        public static final Modifier RUNNING = new AzureIcon.Modifier("running", ModifierLocation.BOTTOM_RIGHT);
        public static final Modifier STOPPED = new AzureIcon.Modifier("stopped", ModifierLocation.BOTTOM_RIGHT);
        public static final Modifier FAILED = new AzureIcon.Modifier("failed", ModifierLocation.BOTTOM_RIGHT);
        public static final Modifier UNKNOWN = new AzureIcon.Modifier("unknown", ModifierLocation.BOTTOM_RIGHT);
        public static final Modifier REFRESHING = new AzureIcon.Modifier("refreshing", ModifierLocation.BOTTOM_RIGHT);
        public static final Modifier SEND_DISABLED = new AzureIcon.Modifier("senddisabled", ModifierLocation.BOTTOM_RIGHT);
        public static final Modifier RECEIVED_DISABLED = new AzureIcon.Modifier("receivedisabled", ModifierLocation.BOTTOM_RIGHT);

        private final String iconPath;
        private final ModifierLocation location;
        private int horizontalShift;
        private int verticalShift;

        public Modifier(@Nonnull final String iconPath, @Nonnull final ModifierLocation location) {
            this.iconPath = iconPath;
            this.location = location;
        }

        public Modifier(@Nonnull final String iconPath, int horizontalShift, int verticalShift) {
            this.iconPath = iconPath;
            this.location = ModifierLocation.OTHER;
            this.horizontalShift = horizontalShift;
            this.verticalShift = verticalShift;
        }
    }

    public enum ModifierLocation {
        BOTTOM_RIGHT,
        BOTTOM_LEFT,
        TOP_RIGHT,
        TOP_LEFT,
        OTHER
    }
}
