/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.cosmos;

import com.intellij.database.autoconfig.DataSourceRegistry;
import com.intellij.database.dataSource.DatabaseDriverManager;
import com.intellij.database.dataSource.LocalDataSource;
import com.intellij.database.dataSource.LocalDataSourceManager;
import com.intellij.database.psi.DbDataSource;
import com.intellij.database.psi.DbPsiFacade;
import com.intellij.database.psi.DbPsiFacadeImpl;
import com.intellij.database.view.ui.DataSourceManagerDialog;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.registry.Registry;
import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.cosmos.CosmosActionsContributor;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.cosmos.CosmosDBAccount;
import com.microsoft.azure.toolkit.lib.cosmos.cassandra.CassandraCosmosDBAccount;
import com.microsoft.azure.toolkit.lib.cosmos.mongo.MongoCosmosDBAccount;

import java.util.function.BiConsumer;

import static com.microsoft.azure.toolkit.intellij.cosmos.dbtools.AzureCosmosDbAccountParamEditor.KEY_COSMOS_ACCOUNT_ID;

public class IntelliJCosmosActionsContributorForUltimate implements IActionsContributor {
    @Override
    public void registerHandlers(AzureActionManager am) {
        final BiConsumer<CosmosDBAccount, AnActionEvent> openDatabaseHandler = (c, e) -> openDatabaseTool(e.getProject(), c);
        final boolean cassandraOn = Registry.is("azure.toolkit.cosmos_cassandra.dbtools.enabled");
        am.registerHandler(CosmosActionsContributor.OPEN_DATABASE_TOOL, (r, e) -> r instanceof MongoCosmosDBAccount || (r instanceof CassandraCosmosDBAccount && cassandraOn), openDatabaseHandler);
    }

    @AzureOperation(name = "cosmos.open_database_tools.account", params = {"account.getName()"}, type = AzureOperation.Type.ACTION)
    private void openDatabaseTool(Project project, CosmosDBAccount account) {
        AzureTaskManager.getInstance().runLater(() -> {
            final DataSourceRegistry registry = new DataSourceRegistry(project);
            final String driver = account instanceof MongoCosmosDBAccount ? "az_cosmos_mongo" : "az_cosmos_cassandra";
            final LocalDataSource ds = DatabaseDriverManager.getInstance().getDriver(driver).createDataSource(null);
            final DbPsiFacade facade = DbPsiFacade.getInstance(project);
            final LocalDataSourceManager manager = LocalDataSourceManager.getInstance(project);
            final DbDataSource newElement = ((DbPsiFacadeImpl) facade).createDataSourceWrapperElement(ds, manager);
            ds.setAdditionalProperty(KEY_COSMOS_ACCOUNT_ID, account.getId());
            DataSourceManagerDialog.showDialog(facade, newElement, null, null, null);
        });
    }
}
