/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.database.sqlserver;

import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.database.sqlserver.SqlServerActionsContributor;
import com.microsoft.azure.toolkit.intellij.common.action.IntellijActionsContributor;
import com.microsoft.azure.toolkit.intellij.connector.ConnectorDialog;
import com.microsoft.azure.toolkit.intellij.database.OpenInDatabaseToolsAction;
import com.microsoft.azure.toolkit.intellij.database.connection.SqlDatabaseResource;
import com.microsoft.azure.toolkit.intellij.database.sqlserver.connection.SqlServerDatabaseResourceDefinition;
import com.microsoft.azure.toolkit.intellij.database.sqlserver.creation.CreateSqlServerAction;
import com.microsoft.azure.toolkit.intellij.database.sqlserver.creation.SqlServerCreationDialog;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.database.DatabaseServerConfig;
import com.microsoft.azure.toolkit.lib.database.JdbcUrl;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;
import com.microsoft.azure.toolkit.lib.sqlserver.AzureSqlServer;
import com.microsoft.azure.toolkit.lib.sqlserver.MicrosoftSqlServer;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

public class IntellijSqlServerActionsContributor implements IActionsContributor {
    private static final String NAME_PREFIX = "SQL Server - %s";
    private static final String DEFAULT_DRIVER_CLASS_NAME = "com.microsoft.sqlserver.jdbc.SQLServerDriver";

    @Override
    public void registerHandlers(AzureActionManager am) {
        final BiPredicate<Object, AnActionEvent> condition = (r, e) -> r instanceof AzureSqlServer;
        final BiConsumer<Object, AnActionEvent> handler = (c, e) -> CreateSqlServerAction.create(e.getProject(), null);
        am.registerHandler(ResourceCommonActionsContributor.CREATE, condition, handler);

        am.<AzResource<?, ?>, AnActionEvent>registerHandler(ResourceCommonActionsContributor.CONNECT, (r, e) -> r instanceof MicrosoftSqlServer,
            (o, e) -> AzureTaskManager.getInstance().runLater(() -> {
                final ConnectorDialog dialog = new ConnectorDialog(e.getProject());
                final MicrosoftSqlServer server = (MicrosoftSqlServer) o;
                dialog.setResource(new SqlDatabaseResource<>(server.databases().list().get(0),
                    server.getAdminName() + "@" + server.getName(), SqlServerDatabaseResourceDefinition.INSTANCE));
                dialog.show();
            }));

        final BiConsumer<ResourceGroup, AnActionEvent> groupCreateServerHandler = (r, e) -> {
            final DatabaseServerConfig config = SqlServerCreationDialog.getDefaultConfig();
            config.setSubscription(r.getSubscription());
            config.setRegion(r.getRegion());
            config.setResourceGroup(r);
            CreateSqlServerAction.create(e.getProject(), config);
        };
        am.registerHandler(SqlServerActionsContributor.GROUP_CREATE_SQLSERVER, (r, e) -> true, groupCreateServerHandler);

        final BiConsumer<AzResource<?, ?>, AnActionEvent> openDatabaseHandler = (c, e) -> openDatabaseTool(e.getProject(), (MicrosoftSqlServer) c);
        am.registerHandler(SqlServerActionsContributor.OPEN_DATABASE_TOOL, (r, e) -> true, openDatabaseHandler);
    }

    @AzureOperation(name = "sqlserver.open_database_tools.server", params = {"server.getName()"}, type = AzureOperation.Type.ACTION)
    private void openDatabaseTool(Project project, @Nonnull MicrosoftSqlServer server) {
        final String DATABASE_TOOLS_PLUGIN_ID = "com.intellij.database";
        final String DATABASE_PLUGIN_NOT_INSTALLED = "\"Database tools and SQL\" plugin is not installed.";
        final String NOT_SUPPORT_ERROR_ACTION = "\"Database tools and SQL\" plugin is only provided in IntelliJ Ultimate edition.";
        if (PluginManagerCore.getPlugin(PluginId.findId(DATABASE_TOOLS_PLUGIN_ID)) == null) {
            throw new AzureToolkitRuntimeException(DATABASE_PLUGIN_NOT_INSTALLED, NOT_SUPPORT_ERROR_ACTION, IntellijActionsContributor.TRY_ULTIMATE);
        }
        final OpenInDatabaseToolsAction.DatasourceProperties properties = OpenInDatabaseToolsAction.DatasourceProperties.builder()
            .name(String.format(NAME_PREFIX, server.getName()))
            .driverClassName(DEFAULT_DRIVER_CLASS_NAME)
            .url(JdbcUrl.sqlserver(Objects.requireNonNull(server.getFullyQualifiedDomainName())).toString())
            .username(server.getAdminName() + "@" + server.getName())
            .build();
        AzureTaskManager.getInstance().runLater(() -> OpenInDatabaseToolsAction.openDataSourceManagerDialog(project, properties));
    }

    @Override
    public int getOrder() {
        return SqlServerActionsContributor.INITIALIZE_ORDER + 1;
    }
}
