/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.runner.functions.component;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.ui.PopupMenuListenerAdapter;
import com.intellij.ui.SimpleListCellRenderer;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azuretools.core.mvp.model.AzureMvpModel;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import io.reactivex.rxjava3.disposables.Disposable;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import java.awt.*;
import java.awt.event.ItemListener;
import java.util.Collections;
import java.util.List;

import static com.microsoft.intellij.common.CommonConst.NEW_CREATED_RESOURCE;

public class ResourceGroupPanel extends JPanel {
    public static final String CREATE_RESOURCE_GROUP = "Create resource group...";
    private JComboBox cbResourceGroup;
    private JPanel pnlRoot;

    private Window window;
    private String subscriptionId;
    private ResourceGroupWrapper selectedResourceGroup;

    private Disposable rxDisposable;

    public ResourceGroupPanel(Window window) {
        this();
        this.window = window;
    }

    public ResourceGroupPanel() {
        cbResourceGroup.setRenderer(new SimpleListCellRenderer() {
            @Override
            public void customize(JList list, Object object, int i, boolean b, boolean b1) {
                if (object instanceof ResourceGroupWrapper) {
                    setText(((ResourceGroupWrapper) object).getDisplayName());
                } else if (object instanceof String) {
                    setText(object.toString());
                }
            }
        });

        // Set label for screen reader for accessibility issue
        final JLabel label = new JLabel("Resource Group");
        label.setLabelFor(cbResourceGroup);

        cbResourceGroup.addPopupMenuListener(new PopupMenuListenerAdapter() {
            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                onSelectResourceGroup();
            }
        });
    }

    public String getResourceGroupName() {
        final Object selectedObject = cbResourceGroup.getSelectedItem();
        return selectedObject instanceof ResourceGroupWrapper ? ((ResourceGroupWrapper) selectedObject).resourceGroup : null;
    }

    public boolean isNewResourceGroup() {
        final Object selectedObject = cbResourceGroup.getSelectedItem();
        return selectedObject instanceof ResourceGroupWrapper ? ((ResourceGroupWrapper) selectedObject).isCreateNewResourceGroup() : false;
    }

    public void loadResourceGroup(String subscriptionId) {
        if (!StringUtils.equalsIgnoreCase(subscriptionId, this.subscriptionId)) {
            this.subscriptionId = subscriptionId;
            beforeLoadSubscription();
            if (rxDisposable != null && !rxDisposable.isDisposed()) {
                rxDisposable.dispose();
            }
            rxDisposable =
                    ComponentUtils.loadResourcesAsync(
                        () -> AzureMvpModel.getInstance().getResourceGroupsBySubscriptionId(subscriptionId),
                        resourceGroups -> fillResourceGroup(resourceGroups),
                        exception -> {
                            DefaultLoader.getUIHelper().showError(
                                    "Failed to load resource groups", exception.getMessage());
                            fillResourceGroup(Collections.emptyList());
                        });
        }
    }

    public void addItemListener(ItemListener actionListener) {
        cbResourceGroup.addItemListener(actionListener);
    }

    public JComponent getComboComponent() {
        return cbResourceGroup;
    }

    private void createResourceGroup() {
        cbResourceGroup.setSelectedItem(null);
        cbResourceGroup.setPopupVisible(false);
        final NewResourceGroupDialog dialog = new NewResourceGroupDialog(subscriptionId);
        dialog.pack();
        if (dialog.showAndGet() && dialog.getResourceGroup() != null) {
            final ResourceGroupWrapper newResourceGroup = dialog.getResourceGroup();
            final ResourceGroupWrapper targetResourceGroup = getResourceGroupWrapperWithName(newResourceGroup.resourceGroup);
            // Use existing resource group wrapper if user create a new resource group with same name before
            if (targetResourceGroup == null) {
                cbResourceGroup.addItem(newResourceGroup);
                selectedResourceGroup = newResourceGroup;
            } else {
                selectedResourceGroup = targetResourceGroup;
            }
        }
        cbResourceGroup.setSelectedItem(selectedResourceGroup);
    }

    private ResourceGroupWrapper getResourceGroupWrapperWithName(String name) {
        for (int i = 0; i < cbResourceGroup.getItemCount(); i++) {
            final Object selectedItem = cbResourceGroup.getItemAt(i);
            if (selectedItem instanceof ResourceGroupWrapper && StringUtils.equals(((ResourceGroupWrapper) selectedItem).resourceGroup, name)) {
                return (ResourceGroupWrapper) cbResourceGroup.getItemAt(i);
            }
        }
        return null;
    }

    private void onSelectResourceGroup() {
        final Object selectedObject = cbResourceGroup.getSelectedItem();
        if (CREATE_RESOURCE_GROUP.equals(selectedObject)) {
            ApplicationManager.getApplication().invokeLater(this::createResourceGroup);
        } else if (selectedObject instanceof ResourceGroupWrapper) {
            selectedResourceGroup = (ResourceGroupWrapper) selectedObject;
        }
    }

    private void beforeLoadSubscription() {
        cbResourceGroup.removeAllItems();
        cbResourceGroup.setEnabled(false);
        cbResourceGroup.addItem("Loading...");
    }

    private void fillResourceGroup(List<ResourceGroup> resourceGroups) {
        cbResourceGroup.removeAllItems();
        cbResourceGroup.setEnabled(true);
        cbResourceGroup.addItem(CREATE_RESOURCE_GROUP);
        if (resourceGroups.size() == 0) {
            cbResourceGroup.setSelectedItem(null);
        } else {
            resourceGroups.stream().forEach(resourceGroup -> cbResourceGroup.addItem(new ResourceGroupWrapper(resourceGroup)));
            // Choose the first resource group by default
            selectedResourceGroup = (ResourceGroupWrapper) cbResourceGroup.getItemAt(1);
            cbResourceGroup.setSelectedItem(selectedResourceGroup);
        }
        if (window != null) {
            window.pack();
        }
    }

    static class ResourceGroupWrapper {
        private String resourceGroup;
        private boolean isCreateNewResourceGroup;

        public ResourceGroupWrapper(String resourceGroup) {
            this.isCreateNewResourceGroup = true;
            this.resourceGroup = resourceGroup;
        }

        public ResourceGroupWrapper(ResourceGroup resourceGroup) {
            this.isCreateNewResourceGroup = false;
            this.resourceGroup = resourceGroup.name();
        }

        public String getResourceGroup() {
            return resourceGroup;
        }

        public boolean isCreateNewResourceGroup() {
            return isCreateNewResourceGroup;
        }

        public String getDisplayName() {
            return isCreateNewResourceGroup ? String.format(NEW_CREATED_RESOURCE, resourceGroup) : resourceGroup;
        }
    }
}
