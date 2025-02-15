/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.runner.localrun;

import com.google.gson.JsonObject;
import com.intellij.execution.BeforeRunTask;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.JavaRunConfigurationModule;
import com.intellij.execution.configurations.LocatableConfiguration;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.configurations.RunProfileWithCompileBeforeLaunchOption;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.ConnectionManager;
import com.microsoft.azure.toolkit.intellij.connector.ConnectionRunnerForRunConfiguration;
import com.microsoft.azure.toolkit.intellij.connector.IConnectionAware;
import com.microsoft.azure.toolkit.intellij.legacy.common.AzureRunConfigurationBase;
import com.microsoft.azure.toolkit.intellij.legacy.function.runner.core.FunctionUtils;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.microsoft.azure.toolkit.intellij.common.AzureBundle.message;

public class FunctionRunConfiguration extends AzureRunConfigurationBase<FunctionRunModel>
        implements LocatableConfiguration, RunProfileWithCompileBeforeLaunchOption, IConnectionAware {
    private JsonObject appSettingsJsonObject;
    private FunctionRunModel functionRunModel;
    @Getter
    private final Set<Connection<?, ?>> connections = new HashSet<>();

    protected FunctionRunConfiguration(@NotNull Project project, @NotNull ConfigurationFactory factory, String name) {
        super(project, factory, name);
        this.functionRunModel = new FunctionRunModel();
        this.myModule = new JavaRunConfigurationModule(project, true);
    }

    @NotNull
    @Override
    public Module[] getModules() {
        final Module module = getModule();
        return module == null ? Module.EMPTY_ARRAY : new Module[]{module};
    }

    @Override
    public Module getModule() {
        Module module = ReadAction.compute(() ->
                Optional.ofNullable(getConfigurationModule()).map(JavaRunConfigurationModule::getModule).orElse(null));
        if (module == null && StringUtils.isNotEmpty(this.functionRunModel.getModuleName())) {
            module = Arrays.stream(ModuleManager.getInstance(getProject()).getModules())
                    .filter(m -> StringUtils.equals(this.functionRunModel.getModuleName(), m.getName()))
                    .findFirst().orElse(null);
            this.myModule.setModule(module);
        }
        return module;
    }

    @Override
    public void addConnection(@Nonnull Connection<?, ?> connection) {
        connections.add(connection);
    }

    @Override
    public FunctionRunModel getModel() {
        return functionRunModel;
    }

    @Override
    public String getTargetName() {
        return "untitled";
    }

    @Override
    public String getTargetPath() {
        return "null";
    }

    @Override
    public String getSubscriptionId() {
        return "null";
    }

    @NotNull
    @Override
    public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
        return new FunctionRunSettingEditor(getProject(), this);
    }

    @Nullable
    @Override
    public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment executionEnvironment) throws ExecutionException {
        return new FunctionRunState(getProject(), this, executor);
    }

    public JsonObject getAppSettingsJsonObject() {
        return appSettingsJsonObject;
    }

    public String getDebugOptions() {
        return functionRunModel.getDebugOptions();
    }

    public void setDebugOptions(String debugOptions) {
        functionRunModel.setDebugOptions(debugOptions);
    }

    public String getFuncPath() {
        return functionRunModel.getFuncPath();
    }

    public String getHostJsonPath() {
        return functionRunModel.getHostJsonPath();
    }

    public String getModuleName() {
        return functionRunModel.getModuleName();
    }

    public String getLocalSettingsJsonPath() {
        return functionRunModel.getLocalSettingsJsonPath();
    }

    @javax.annotation.Nullable
    public String getDefaultLocalSettingsJsonPath(final Module module) {
        // workaround to get module file, todo: investigate the process canceled exception with FilenameIndex API
        return Paths.get(ModuleUtil.getModuleDirPath(module), "local.settings.json").toString();
    }

    public Map<String, String> getAppSettings() {
        return functionRunModel.getAppSettings();
    }

    public FunctionRunModel getFunctionRunModel() {
        return functionRunModel;
    }

    public void setLocalSettingsJsonPath(String localSettingsJsonPath) {
        functionRunModel.setLocalSettingsJsonPath(localSettingsJsonPath);
    }

    public void setAppSettings(Map<String, String> appSettings) {
        functionRunModel.setAppSettings(appSettings);
    }

    public void setFunctionRunModel(FunctionRunModel functionRunModel) {
        this.functionRunModel = functionRunModel;
    }

    public void setAppSettingsJsonObject(JsonObject appSettingsJsonObject) {
        this.appSettingsJsonObject = appSettingsJsonObject;
    }

    public String getAppSettingsKey() {
        return functionRunModel.getAppSettingsKey();
    }

    public void setAppSettingsKey(String appSettingsStorageKey) {
        functionRunModel.setAppSettingsKey(appSettingsStorageKey);
    }

    public void saveModule(Module module) {
        if (module == null) {
            return;
        }
        this.functionRunModel.setModuleName(module.getName());
        this.myModule.setModule(module);
    }

    public String getFunctionHostArguments() {
        return this.functionRunModel.getFunctionHostArguments();
    }

    public void setFunctionHostArguments(final String arguments) {
        this.functionRunModel.setFunctionHostArguments(arguments);
    }

    public void initializeDefaults(Module module) {
        if (module == null) {
            return;
        }
        saveModule(module);
        if (StringUtils.isEmpty(this.getFuncPath())) {
            try {
                this.setFuncPath(FunctionUtils.getFuncPath());
            } catch (IOException | InterruptedException ex) {
                // ignore;
            }
        }
        if (StringUtils.isEmpty(this.getFunctionHostArguments())) {
            this.setFunctionHostArguments(FunctionUtils.getDefaultFuncArguments());
        }
        try {
            prepareBeforeRunTasks();
        } catch (final Throwable throwable) {
            // ignore;
        }
    }

    // workaround to correct before run tasks in quick launch as BeforeRunTaskAdder may not work or have wrong config in task in this case
    private void prepareBeforeRunTasks() {
        final List<Connection<?, ?>> connections = this.getProject().getService(ConnectionManager.class).getConnections();
        if (CollectionUtils.isEmpty(connections)) {
            return;
        }
        final List<BeforeRunTask<?>> tasks = this.getBeforeRunTasks();
        final List<ConnectionRunnerForRunConfiguration.MyBeforeRunTask> rcTasks = tasks.stream().filter(t -> t instanceof ConnectionRunnerForRunConfiguration.MyBeforeRunTask)
                .map(t -> (ConnectionRunnerForRunConfiguration.MyBeforeRunTask)t)
                .collect(Collectors.toList());
        final List<ConnectionRunnerForRunConfiguration.MyBeforeRunTask> invalidTasks =
                rcTasks.stream().filter(t -> !Objects.equals(this, t.getConfig())).collect(Collectors.toList());
        tasks.removeAll(invalidTasks);
        rcTasks.removeAll(invalidTasks);
        if (CollectionUtils.isEmpty(rcTasks) && connections.stream().anyMatch(c -> c.isApplicableFor(this))) {
            this.getBeforeRunTasks().add(new ConnectionRunnerForRunConfiguration.MyBeforeRunTask(this));
        }
    }

    public void setStagingFolder(String stagingFolder) {
        functionRunModel.setStagingFolder(stagingFolder);
    }

    public void setFuncPath(String funcPath) {
        functionRunModel.setFuncPath(funcPath);
    }

    public void setHostJsonPath(String hostJsonPath) {
        functionRunModel.setHostJsonPath(hostJsonPath);
    }

    @Override
    public void validate() throws ConfigurationException {
        if (getModule() == null) {
            throw new ConfigurationException(message("function.run.validate.noModule"));
        }

        if (StringUtils.isEmpty(getFuncPath())) {
            throw new ConfigurationException(message("function.run.validate.noFuncPath"));
        }

        final File func = new File(getFuncPath());
        if (!func.exists() || !func.isFile() || !func.getName().contains("func")) {
            throw new ConfigurationException(message("function.run.validate.invalidFuncPath"));
        }
    }

    @Override
    public boolean isGeneratedName() {
        return false;
    }

    @Nullable
    @Override
    public String suggestedName() {
        return "Unnamed";
    }
}
