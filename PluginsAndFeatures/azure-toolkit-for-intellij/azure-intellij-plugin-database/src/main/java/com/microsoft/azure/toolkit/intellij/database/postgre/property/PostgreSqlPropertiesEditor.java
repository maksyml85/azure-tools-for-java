/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.database.postgre.property;

import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.azure.toolkit.intellij.common.AzureHideableTitledSeparator;
import com.microsoft.azure.toolkit.intellij.common.properties.AzResourcePropertiesEditor;
import com.microsoft.azure.toolkit.intellij.database.component.DatabaseComboBox;
import com.microsoft.azure.toolkit.intellij.database.ui.ConnectionSecurityPanel;
import com.microsoft.azure.toolkit.intellij.database.ui.ConnectionStringsOutputPanel;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemetry;
import com.microsoft.azure.toolkit.lib.postgre.PostgreSqlDatabase;
import com.microsoft.azure.toolkit.lib.postgre.PostgreSqlServer;
import com.microsoft.azure.toolkit.lib.postgre.PostgreSqlServerDraft;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;

public class PostgreSqlPropertiesEditor extends AzResourcePropertiesEditor<PostgreSqlServer> {

    public static final String ID = "com.microsoft.azure.toolkit.intellij.postgre.property.PostgreSqlPropertiesEditor";
    private final Project project;
    @Nonnull
    private final PostgreSqlServer server;
    @Nonnull
    private final PostgreSqlServerDraft draft;

    private AzureHideableTitledSeparator overviewSeparator;
    private PostgrePropertyOverviewPanel overview;
    private AzureHideableTitledSeparator connectionSecuritySeparator;
    private ConnectionSecurityPanel connectionSecurity;
    private AzureHideableTitledSeparator connectionStringsSeparator;
    private ConnectionStringsOutputPanel connectionStringsJDBC;
    private ConnectionStringsOutputPanel connectionStringsSpring;
    private JPanel rootPanel;
    private JPanel contextPanel;
    private JScrollPane scrollPane;
    private PostgreSqlPropertyActionPanel propertyActionPanel;
    private DatabaseComboBox<PostgreSqlDatabase> databaseComboBox;
    private JLabel databaseLabel;
    public static final String POSTGRE_SQL_OUTPUT_TEXT_PATTERN_SPRING =
        "spring.datasource.driver-class-name=org.postgresql.Driver" + System.lineSeparator() +
            "spring.datasource.url=jdbc:postgresql://%s:5432/%s?useSSL=true&requireSSL=false" + System.lineSeparator() +
            "spring.datasource.username=%s" + System.lineSeparator() + "spring.datasource.password={your_password}";

    public static final String POSTGRE_SQL_OUTPUT_TEXT_PATTERN_JDBC =
        "String url =\"jdbc:postgresql://%s:5432/%s?useSSL=true&requireSSL=false\";" + System.lineSeparator() +
            "myDbConn = DriverManager.getConnection(url, \"%s\", {your_password});";

    @Override
    protected void rerender() {
        AzureTaskManager.getInstance().runLater(() -> this.setData(this.draft));
    }

    PostgreSqlPropertiesEditor(@Nonnull Project project, @Nonnull PostgreSqlServer server, @Nonnull final VirtualFile virtualFile) {
        super(virtualFile, server, project);
        this.project = project;
        this.server = server;
        this.draft = (PostgreSqlServerDraft) server.update();

        overviewSeparator.addContentComponent(overview);
        connectionSecuritySeparator.addContentComponent(connectionSecurity);
        connectionStringsSeparator.addContentComponent(databaseLabel);
        connectionStringsSeparator.addContentComponent(databaseComboBox);
        connectionStringsSeparator.addContentComponent(connectionStringsJDBC);
        connectionStringsSeparator.addContentComponent(connectionStringsSpring);
        connectionStringsJDBC.getTitleLabel().setText("JDBC");
        connectionStringsJDBC.getOutputTextArea().setText(getConnectionString(POSTGRE_SQL_OUTPUT_TEXT_PATTERN_JDBC, null, null, null));
        connectionStringsSpring.getTitleLabel().setText("Spring");
        connectionStringsSpring.getOutputTextArea().setText(getConnectionString(POSTGRE_SQL_OUTPUT_TEXT_PATTERN_SPRING, null, null, null));
        this.rerender();
        this.initListeners();
    }

    private String getConnectionString(final String pattern, final String hostname, final String database, final String username) {
        final String newHostname = StringUtils.isNotBlank(hostname) ? hostname : "{your_hostname}";
        final String newDatabase = StringUtils.isNotBlank(database) ? database : "{your_database}";
        final String newUsername = StringUtils.isNotBlank(username) ? username : "{your_username}";
        return String.format(pattern, newHostname, newDatabase, newUsername);
    }

    private void setData(PostgreSqlServer server) {
        this.overview.setFormData(this.server);
        this.databaseComboBox.setServer(this.server);
        this.refreshButtons();
        final boolean ready = StringUtils.equalsIgnoreCase("READY", this.server.getStatus());
        connectionSecuritySeparator.expand();
        connectionStringsSeparator.expand();
        connectionSecuritySeparator.setEnabled(!ready);
        connectionStringsSeparator.setEnabled(!ready);
        connectionSecurity.getAllowAccessFromAzureServicesCheckBox().setEnabled(ready);
        connectionSecurity.getAllowAccessFromLocalMachineCheckBox().setEnabled(ready);
        AzureTaskManager.getInstance().runOnPooledThread(() -> {
            final boolean originalAllowAccessToAzureServices = this.draft.isAzureServiceAccessAllowed();
            final boolean originalAllowAccessToLocal = this.draft.isLocalMachineAccessAllowed();
            AzureTaskManager.getInstance().runLater(() -> {
                connectionSecurity.getAllowAccessFromAzureServicesCheckBox().setSelected(originalAllowAccessToAzureServices);
                connectionSecurity.getAllowAccessFromLocalMachineCheckBox().setSelected(originalAllowAccessToLocal);
            });
        });
    }

    private void initListeners() {
        // update to trigger save/discard buttons
        connectionSecurity.getAllowAccessFromAzureServicesCheckBox().addItemListener(this::onCheckBoxChanged);
        connectionSecurity.getAllowAccessFromLocalMachineCheckBox().addItemListener(this::onCheckBoxChanged);
        // actions of copy buttons
        connectionStringsJDBC.getCopyButton().addActionListener(this::onJDBCCopyButtonClicked);
        connectionStringsSpring.getCopyButton().addActionListener(this::onSpringCopyButtonClicked);
        // save/discard buttons
        propertyActionPanel.getSaveButton().addActionListener(e -> this.apply());
        propertyActionPanel.getDiscardButton().addActionListener(e -> this.reset());
        // database combobox changed
        databaseComboBox.addItemListener(this::onDatabaseComboBoxChanged);
    }

    private void onCheckBoxChanged(ItemEvent itemEvent) {
        this.refreshButtons();
    }

    private void onJDBCCopyButtonClicked(ActionEvent e) {
        try {
            copyToSystemClipboard(this.connectionStringsJDBC.getOutputTextArea().getText());
        } catch (final Exception exception) {
            final String error = "copy JDBC connection strings";
            final String action = "try again later.";
            throw new AzureToolkitRuntimeException(error, action);
        }
    }

    private void copyToSystemClipboard(String text) {
        CopyPasteManager.getInstance().setContents(new StringSelection(text));
    }

    private void onSpringCopyButtonClicked(ActionEvent e) {
        try {
            copyToSystemClipboard(this.connectionStringsSpring.getOutputTextArea().getText());
        } catch (final Exception exception) {
            final String error = "copy Spring connection strings";
            final String action = "try again later.";
            throw new AzureToolkitRuntimeException(error, action);
        }
    }

    private void apply() {
        this.propertyActionPanel.getSaveButton().setEnabled(false);
        this.propertyActionPanel.getDiscardButton().setEnabled(false);
        final Runnable runnable = () -> {
            final String subscriptionId = draft.getSubscriptionId();
            final boolean allowAccessToAzureServices = connectionSecurity.getAllowAccessFromAzureServicesCheckBox().getModel().isSelected();
            final boolean allowAccessToLocal = connectionSecurity.getAllowAccessFromLocalMachineCheckBox().getModel().isSelected();
            this.draft.setAzureServiceAccessAllowed(allowAccessToAzureServices);
            this.draft.setLocalMachineAccessAllowed(allowAccessToLocal);
            final AzureTelemetry.Context context = AzureTelemetry.getActionContext();
            context.setProperty("subscriptionId", subscriptionId);
            context.setProperty("allowAccessToLocal", String.valueOf(allowAccessToLocal));
            context.setProperty("allowAccessToAzureServices", String.valueOf(allowAccessToAzureServices));
            this.draft.commit();
            this.refreshButtons();
        };
        AzureTaskManager.getInstance().runInBackground(new AzureTask<>(this.project, "Saving updates", false, runnable));
    }

    private void refreshButtons() {
        AzureTaskManager.getInstance().runOnPooledThread(() -> {
            final boolean modified = this.isModified();
            AzureTaskManager.getInstance().runLater(() -> {
                this.propertyActionPanel.getSaveButton().setEnabled(modified);
                this.propertyActionPanel.getDiscardButton().setEnabled(modified);
            });
        });
    }

    private void reset() {
        this.draft.reset();
        this.rerender();
    }

    private void onDatabaseComboBoxChanged(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED && e.getItem() instanceof PostgreSqlDatabase) {
            final PostgreSqlDatabase database = (PostgreSqlDatabase) e.getItem();
            final String username = this.draft.getAdminName() + "@" + this.draft.getName();
            connectionStringsJDBC.getOutputTextArea().setText(getConnectionString(POSTGRE_SQL_OUTPUT_TEXT_PATTERN_JDBC,
                this.draft.getFullyQualifiedDomainName(), database.getName(), username));
            connectionStringsSpring.getOutputTextArea().setText(getConnectionString(POSTGRE_SQL_OUTPUT_TEXT_PATTERN_SPRING,
                this.draft.getFullyQualifiedDomainName(), database.getName(), username));
        }
    }

    @Override
    public boolean isModified() {
        return this.draft.isAzureServiceAccessAllowed() != connectionSecurity.getAllowAccessFromAzureServicesCheckBox().getModel().isSelected() ||
            this.draft.isLocalMachineAccessAllowed() != connectionSecurity.getAllowAccessFromLocalMachineCheckBox().getModel().isSelected();
    }

    @Override
    public @Nonnull
    JComponent getComponent() {
        return rootPanel;
    }

    protected void refresh() {
        this.propertyActionPanel.getDiscardButton().setEnabled(false);
        this.propertyActionPanel.getSaveButton().setEnabled(false);
        final String refreshTitle = String.format("Refreshing PostgreSQL server(%s)...", this.draft.getName());
        AzureTaskManager.getInstance().runInBackground(refreshTitle, () -> {
            this.draft.reset();
            this.draft.refresh();
            this.rerender();
        });
    }
}
