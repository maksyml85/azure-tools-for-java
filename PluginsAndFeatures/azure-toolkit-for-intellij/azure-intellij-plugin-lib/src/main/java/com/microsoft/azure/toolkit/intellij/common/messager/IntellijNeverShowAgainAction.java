/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common.messager;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.microsoft.azure.toolkit.intellij.common.settings.IntellijStore;
import com.microsoft.azure.toolkit.lib.common.messager.ExceptionNotification;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@Setter
@Slf4j
public class IntellijNeverShowAgainAction extends NotificationAction {

    public static final String ID = "user/common.never_show_again";

    public IntellijNeverShowAgainAction() {
        super("Never Show Again");
    }

    @Override
    @ExceptionNotification
    @AzureOperation(name = "user/common.suppress_action")
    public void actionPerformed(@NotNull AnActionEvent event, @NotNull Notification notification) {
        Optional.ofNullable(ActionManager.getInstance().getId(this)).ifPresent(id -> {
            IntellijStore.getInstance().getState().getSuppressedActions().put(id, Boolean.TRUE);
            notification.expire();
        });
    }

}
