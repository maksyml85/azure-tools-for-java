/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.applicationinsights.preference;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.microsoft.applicationinsights.ui.activator.Activator;
import com.microsoft.applicationinsights.util.AILibraryUtil;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.applicationinsights.ApplicationInsight;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.resource.AzureResources;
import com.microsoft.azuretools.authmanage.IdeAzureAccount;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.azure.sdk.AzureSDKManager;

/**
 * Class is intended for creating new application insights resources remotely in the cloud.
 */
public class ApplicationInsightsNewDialog extends TitleAreaDialog {
    private static final String LOADING = "<Loading...>";
    Text txtName;
    private Combo subscription;
    private Label resourceGroupLabel;
    private Button createNewRadioButton;
    private Button useExistingRadioButton;
    private Text resourceGrpField;
    private Combo resourceGrpCombo;
    Combo region;
    Button okButton;
    private Subscription currentSub;
    static ApplicationInsightsResource resourceToAdd;
    private Runnable onCreate;

    public ApplicationInsightsNewDialog(Shell parentShell) {
        super(parentShell);
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText(Messages.appTtl);
        Image image = AILibraryUtil.getImage();
        if (image != null) {
            setTitleImage(image);
        }
    }

    @Override
    protected Control createButtonBar(Composite parent) {
        Control ctrl = super.createButtonBar(parent);
        okButton = getButton(IDialogConstants.OK_ID);
        // populate values after button bar has been created, in order to enable or disable OK button.
        populateValues();
        return ctrl;
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        setTitle(Messages.newKeyTtl);
        setMessage(Messages.newKeyMsg);
        setHelpAvailable(false);

        GridLayout gridLayout = new GridLayout();
        GridData gridData = new GridData();
        gridLayout.numColumns = 2;
        gridLayout.marginBottom = 10;
        gridData.horizontalAlignment = SWT.FILL;
        gridData.grabExcessHorizontalSpace = true;
        Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(gridLayout);
        container.setLayoutData(gridData);

        createNameCmpnt(container);
        createSubCmpnt(container);
        createResourceGroupCmpnt(container);
        createRegionCmpnt(container);

        return super.createDialogArea(parent);
    }

    private void populateValues() {
        try {
            subscription.removeAll();
            if (!IdeAzureAccount.getInstance().isLoggedIn()) {
                return;
            }
            List<Subscription> subList = Azure.az(AzureAccount.class).account().getSelectedSubscriptions();
            // check at least single subscription is associated with the account
            if (subList.size() > 0) {
                for (Subscription sub : subList) {
                    subscription.add(sub.getName());
                    subscription.setData(sub.getName(), sub);
                }
                subscription.select(0);
                currentSub = subList.get(0);

                populateResourceGroupValues(currentSub.getId(), "");

                List<String> regionList = AzureSDKManager.getLocationsForInsights(currentSub.getId());
                String[] regionArray = regionList.toArray(new String[regionList.size()]);
                region.setItems(regionArray);
                region.setText(regionArray[0]);
            }
            enableOkBtn();
        } catch (Exception ex) {
            Activator.getDefault().log(Messages.getValuesErrMsg, ex);
        }
    }

    private void populateResourceGroupValues(String subId, String valtoSet) {
        try {
            final List<String> groupStringList = Azure.az(AzureResources.class).groups(subId).list()
                    .stream().map(group -> group.getName()).collect(Collectors.toList());
            if (groupStringList.size() > 0) {
                String[] groupArray = groupStringList.toArray(new String[groupStringList.size()]);
                resourceGrpCombo.removeAll();
                resourceGrpCombo.setItems(groupArray);
                if (valtoSet.isEmpty() || !groupStringList.contains(valtoSet)) {
                    resourceGrpCombo.setText(groupArray[0]);
                } else {
                    resourceGrpCombo.setText(valtoSet);
                }
            }
        } catch (Exception ex) {
            Activator.getDefault().log(Messages.getValuesErrMsg, ex);
        }
    }

    private void createNameCmpnt(Composite container) {
        Label lblName = new Label(container, SWT.LEFT);
        GridData gridData = gridDataForLbl();
        lblName.setLayoutData(gridData);
        lblName.setText(Messages.name);

        txtName = new Text(container, SWT.LEFT | SWT.BORDER);
        gridData = gridDataForText(180);
        txtName.setLayoutData(gridData);
        txtName.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent arg0) {
                enableOkBtn();
            }
        });
    }

    private void createSubCmpnt(Composite container) {
        Label lblName = new Label(container, SWT.LEFT);
        GridData gridData = gridDataForLbl();
        lblName.setLayoutData(gridData);
        lblName.setText(Messages.sub);

        subscription = new Combo(container, SWT.READ_ONLY);
        gridData = gridDataForText(180);
        subscription.setLayoutData(gridData);

        subscription.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                Subscription newSub = (Subscription) subscription.getData(subscription.getText());
                String prevResGrpVal = resourceGrpCombo.getText();
                if (currentSub.equals(newSub)) {
                    populateResourceGroupValues(currentSub.getId(), prevResGrpVal);
                } else {
                    populateResourceGroupValues(newSub.getId(), "");
                }
                currentSub = newSub;
                enableOkBtn();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {

            }
        });
    }

    private void createResourceGroupCmpnt(Composite container) {
        resourceGroupLabel = new Label(container, SWT.LEFT);
        resourceGroupLabel.setText(Messages.resGrp);
        GridData gridData = new GridData();
        gridData.verticalAlignment = SWT.TOP;
        resourceGroupLabel.setLayoutData(gridData);

        final Composite composite = new Composite(container, SWT.NONE);
        GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 2;
        gridData = new GridData();
        gridData.horizontalAlignment = SWT.FILL;
        gridData.verticalAlignment = GridData.BEGINNING;
        gridData.grabExcessHorizontalSpace = true;
        // gridData.widthHint = 250;
        composite.setLayout(gridLayout);
        composite.setLayoutData(gridData);

        createNewRadioButton = new Button(composite, SWT.RADIO);
        createNewRadioButton.setText("Create new");
        createNewRadioButton.setSelection(true);
        resourceGrpField = new Text(composite, SWT.LEFT | SWT.BORDER);
        gridData = new GridData(SWT.FILL, SWT.CENTER, true, true);
        resourceGrpField.setLayoutData(gridData);

        useExistingRadioButton = new Button(composite, SWT.RADIO);
        useExistingRadioButton.setText("Use existing");
        resourceGrpCombo = new Combo(composite, SWT.READ_ONLY);
        gridData = new GridData(SWT.FILL, SWT.CENTER, true, true);
        resourceGrpCombo.setLayoutData(gridData);
        SelectionListener updateListener = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent arg0) {
                updateResourceGroup();
                enableOkBtn();
            }
        };
        createNewRadioButton.addSelectionListener(updateListener);
        useExistingRadioButton.addSelectionListener(updateListener);
        resourceGrpField.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent arg0) {
                enableOkBtn();
            }
        });
        updateResourceGroup();
    }

    private void updateResourceGroup() {
        final boolean isNewGroup = createNewRadioButton.getSelection();
        resourceGrpField.setEnabled(isNewGroup);
        resourceGrpCombo.setEnabled(!isNewGroup);
    }

    private void createRegionCmpnt(Composite container) {
        Label lblName = new Label(container, SWT.LEFT);
        GridData gridData = gridDataForLbl();
        lblName.setLayoutData(gridData);
        lblName.setText(Messages.region);

        region = new Combo(container, SWT.READ_ONLY);
        gridData = gridDataForText(180);
        region.setLayoutData(gridData);
    }

    /**
     * Method creates grid data for label field.
     */
    private GridData gridDataForLbl() {
        GridData gridData = new GridData();
        gridData.horizontalIndent = 5;
        gridData.verticalIndent = 10;
        return gridData;
    }

    /**
     * Method creates grid data for text field.
     */
    private GridData gridDataForText(int width) {
        GridData gridData = new GridData();
        gridData.horizontalAlignment = SWT.END;
        gridData.horizontalAlignment = SWT.FILL;
        gridData.widthHint = width;
        gridData.verticalIndent = 10;
        gridData.grabExcessHorizontalSpace = true;
        return gridData;
    }

    /**
     * Enable or disable OK button as per text selected in combo box or text box. New� button to create resource group
     * will be disabled if no subscription is selected/exists.
     */
    private void enableOkBtn() {
        if (okButton != null) {
            if (txtName.getText().trim().isEmpty() || subscription.getText().isEmpty()
                    || (resourceGrpCombo.getText().isEmpty() && useExistingRadioButton.getSelection()
                            || resourceGrpField.getText().isEmpty() && createNewRadioButton.getSelection())
                    || region.getText().isEmpty()) {
                okButton.setEnabled(false);
                if (subscription.getText().isEmpty() || subscription.getItemCount() <= 0) {
                    setErrorMessage(Messages.noSubErrMsg);
                } else if (resourceGrpCombo.getText().isEmpty() && useExistingRadioButton.getSelection()
                        || resourceGrpField.getText().isEmpty()) {
                    setErrorMessage(Messages.noResGrpErrMsg);
                } else {
                    setErrorMessage(null);
                }
            } else {
                okButton.setEnabled(true);
                setErrorMessage(null);
            }
        }
    }

    @Override
    protected void okPressed() {
        String subId = currentSub.getId();
        boolean isNewGroup = createNewRadioButton.getSelection();
        String resourceGroup = isNewGroup ? resourceGrpField.getText() : resourceGrpCombo.getText();
        String name = txtName.getText();
        String location = region.getText();
        DefaultLoader.getIdeHelper().runInBackground(null, "Creating Application Insights Resource", false, true,
                "Creating Application Insights Resource " + name + "...", new Runnable() {
                    @Override
                    public void run() {
                        try {
                            ApplicationInsight resource = AzureSDKManager.createInsightsResource(currentSub,
                                    resourceGroup, isNewGroup, name, location, null);
                            resourceToAdd = new ApplicationInsightsResource(resource, currentSub, true);
                            if (onCreate != null) {
                                onCreate.run();
                            }
                        } catch (Exception ex) {
                            DefaultLoader.getUIHelper().showException(ex.getMessage(), ex, Messages.resCreateErrMsg,
                                    true, false);
                        }
                    }
                });
        super.okPressed();
    }

    public static ApplicationInsightsResource getResource() {
        return resourceToAdd;
    }

    public void setOnCreate(Runnable onCreate) {
        this.onCreate = onCreate;
    }
}
