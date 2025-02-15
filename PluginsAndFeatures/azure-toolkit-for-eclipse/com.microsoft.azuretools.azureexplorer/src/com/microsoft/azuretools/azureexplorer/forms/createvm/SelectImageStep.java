/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.azureexplorer.forms.createvm;

import com.microsoft.azure.management.compute.*;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azuretools.core.Activator;
import com.microsoft.azuretools.core.utils.Messages;
import com.microsoft.azuretools.core.utils.PluginUtil;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import java.util.ArrayList;
import java.util.List;

public class SelectImageStep extends WizardPage {
    private static final String LOADING = "<Loading...>";

    private CreateVMWizard wizard;

    private Button knownImageBtn;
    private Button customImageBtn;
    private Combo knownImageComboBox;
    private Label regionLabel;
    private Combo regionComboBox;
    private Label publisherLabel;
    private Combo publisherComboBox;
    private Label offerLabel;
    private Combo offerComboBox;
    private Label skuLabel;
    private Combo skuComboBox;
    private Label versionLabel;
    private org.eclipse.swt.widgets.List imageLabelList;

    public SelectImageStep(final CreateVMWizard wizard) {
        super("Select a Virtual Machine Image", "Select a Virtual Machine Image", Activator.getImageDescriptor("icons/large/Azure.png"));
        this.wizard = wizard;
    }

    @Override
    public void createControl(Composite parent) {
        GridLayout gridLayout = new GridLayout(1, false);
        GridData gridData = new GridData();
//        gridData.widthHint = 400;
        gridData.horizontalAlignment = SWT.FILL;
        gridData.grabExcessHorizontalSpace = true;
        Composite container = new Composite(parent, 0);
        container.setLayout(gridLayout);
        container.setLayoutData(gridData);

        createSettingsPanel(container);
        regionComboBox.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                selectRegion();
            }
        });

        SelectionListener updateListener = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                enableControls(!knownImageBtn.getSelection());
                validateNext();
                setMachineImage();
            }
        };
        knownImageBtn.addSelectionListener(updateListener);
        customImageBtn.addSelectionListener(updateListener);

        customImageBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (customImageBtn.getSelection()) {
                    fillPublishers();
                }
            }
        });
        knownImageBtn.setSelection(true);
        setMachineImage();

        publisherComboBox.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                fillOffers();
            }
        });
        offerComboBox.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                fillSkus();
            }
        });
        skuComboBox.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                fillImages();
            }
        });

        imageLabelList.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent selectionEvent) {
                imageLabelSelected();
            }
        });
        validateNext();
        this.setControl(container);
    }

    private void createSettingsPanel(Composite container) {
        final Composite composite = new Composite(container, SWT.NONE);
        GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 2;
        GridData gridData = new GridData();
        gridData.horizontalAlignment = SWT.FILL;
        gridData.verticalAlignment = GridData.BEGINNING;
        gridData.grabExcessHorizontalSpace = true;
//        gridData.widthHint = 350;
        composite.setLayout(gridLayout);
        composite.setLayoutData(gridData);

        regionLabel = new Label(composite, SWT.LEFT);
        regionLabel.setText("Location:");
//        regionLabel.setLayoutData(getGridDataForLabel());
        regionComboBox = new Combo(composite, SWT.READ_ONLY);
        regionComboBox.setLayoutData(getGridData(1));
        regionComboBox.setToolTipText("Specifies the location where your virtual machine will be created");

        knownImageBtn = new Button(composite, SWT.RADIO);
        knownImageBtn.setText("Recommended image:");
        knownImageBtn.setLayoutData(getGridData(2));
        knownImageComboBox = new Combo(composite, SWT.READ_ONLY);
        knownImageComboBox.setLayoutData(getGridData(2));
        for (KnownWindowsVirtualMachineImage image : KnownWindowsVirtualMachineImage.values()) {
            knownImageComboBox.add(image.offer() + " - " + image.sku());
            knownImageComboBox.setData(image.offer() + " - " + image.sku(), image);
        }
        for (KnownLinuxVirtualMachineImage image : KnownLinuxVirtualMachineImage.values()) {
            knownImageComboBox.add(image.offer() + " - " + image.sku());
            knownImageComboBox.setData(image.offer() + " - " + image.sku(), image);
        }
        knownImageComboBox.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent selectionEvent) {
                wizard.setKnownMachineImage(knownImageComboBox.getData(knownImageComboBox.getText()));
                validateNext();
            }
        });
        knownImageComboBox.select(0);

        customImageBtn = new Button(composite, SWT.RADIO);
        customImageBtn.setText("Custom image:");
        customImageBtn.setLayoutData(getGridData(2));

        publisherLabel = new Label(composite, SWT.LEFT);
        publisherLabel.setText("Publisher:");
        publisherComboBox = new Combo(composite, SWT.READ_ONLY);
        publisherComboBox.setLayoutData(getGridData(1));
        publisherComboBox.setToolTipText("Specifies the publisher which created the image which you will use to create your virtual machine");

        offerLabel = new Label(composite, SWT.LEFT);
        offerLabel.setText("Offer:");
        offerComboBox = new Combo(composite, SWT.READ_ONLY);
        offerComboBox.setLayoutData(getGridData(1));
        offerComboBox.setToolTipText("Specifies which the virtual machine which offering to use from the selected publisher");

        skuLabel = new Label(composite, SWT.LEFT);
        skuLabel.setText("Sku:");
//        skuLabel.setLayoutData(getGridDataForLabel());
        skuComboBox = new Combo(composite, SWT.READ_ONLY);
        skuComboBox.setLayoutData(getGridData(1));
        skuComboBox.setToolTipText("Specifies the Stockkeeping Unit (SKU) to use from the selected offering");

        versionLabel = new Label(composite, SWT.LEFT);
        versionLabel.setText("Version #:");
        imageLabelList = new org.eclipse.swt.widgets.List(composite, SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL);
        // gridData = new GridData();
        // gridData.widthHint = 300;
        gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
        gridData.verticalIndent = 10;
        imageLabelList.setLayoutData(gridData);
        imageLabelList.setToolTipText("Specifies the label for the specific image to use from the selected SKU; this is often the version for an image");
    }

    private GridData getGridData(int columns) {
        GridData gridData = new GridData(SWT.FILL, SWT.CENTER, true, true);
        gridData.horizontalSpan = columns;
        gridData.horizontalAlignment = SWT.FILL;
//        gridData.widthHint = 280;
        gridData.verticalIndent = 10;
        gridData.grabExcessHorizontalSpace = true;
        return gridData;
    }

    @Override
    public String getTitle() {
        if (wizard.getRegion() == null && wizard.getSubscription() != null) {
//            imageTypeComboBox.setEnabled(false);
            setPageComplete(false);
         // will set to null if selected subscription changes
            if (wizard.getRegion() == null) {
                setPageComplete(false);

                if (wizard.getSubscription() == null) {
                    DefaultLoader.getIdeHelper().invokeAndWait(new Runnable() {
                        @Override
                        public void run() {
                            regionComboBox.setItems(new String[] {LOADING});
                            regionComboBox.select(0);
                            regionComboBox.setEnabled(false);
                        }
                    });
                    AzureTaskManager.getInstance().runInBackground("Loading Available Locations...", new Runnable() {
                        @Override
                        public void run() {
                            try {
                                DefaultLoader.getIdeHelper().invokeLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        fillRegions();
                                    }
                                });
                            } catch (Exception ex) {
                                PluginUtil.displayErrorDialogWithAzureMsg(PluginUtil.getParentShell(), Messages.err, "Error loading locations", ex);
                            }
                        }
                    });
                } else {
                    fillRegions();
                }
            }
            enableControls(customImageBtn.getSelection());
        }
        return super.getTitle();
    }

    private void fillRegions() {
        regionComboBox.removeAll();
        Subscription subs = wizard.getSubscription();
        List<Region> locations = Azure.az(AzureAccount.class).listRegions(subs.getId());
        for (Region location : locations) {
            regionComboBox.add(location.getLabel());
            regionComboBox.setData(location.getLabel(), location);
        }
        if (locations.size() > 0) {
            regionComboBox.select(0);
            selectRegion();
        }
        regionComboBox.setEnabled(true);
        validateNext();
    }

    private void enableControls(boolean customImage) {
        knownImageComboBox.setEnabled(!customImage);
//        setPageComplete(!customImage && regionComboBox.getText() != null);
//        model.getCurrentNavigationState().NEXT.setEnabled(!customImage || !imageLabelList.isSelectionEmpty());
        imageLabelList.setEnabled(customImage);
        publisherComboBox.setEnabled(customImage);
        offerComboBox.setEnabled(customImage);
        skuComboBox.setEnabled(customImage);
        publisherLabel.setEnabled(customImage);
        offerLabel.setEnabled(customImage);
        skuLabel.setEnabled(customImage);
        versionLabel.setEnabled(customImage);
    }

    private void selectRegion() {
        if (customImageBtn.getSelection()) {
            fillPublishers();
        }
        wizard.setRegion((Region) regionComboBox.getData(regionComboBox.getText()));
    }

    private void fillPublishers() {
        setPageComplete(false);
        String region = ((Region) regionComboBox.getData(regionComboBox.getText())).getName();
        publisherComboBox.removeAll();
        offerComboBox.removeAll();
        imageLabelList.removeAll();
        offerComboBox.setEnabled(false);
        skuComboBox.setEnabled(false);
        imageLabelList.setEnabled(false);
        AzureTaskManager.getInstance().runOnPooledThread(() -> {
            final List<VirtualMachinePublisher> publishers = wizard.getComputeManager()
                    .virtualMachineImages().publishers().listByRegion(region);
            DefaultLoader.getIdeHelper().invokeLater(() -> {
                for (VirtualMachinePublisher publisher : publishers) {
                    publisherComboBox.add(publisher.name());
                    publisherComboBox.setData(publisher.name(), publisher);
                }
                if (publishers.size() > 0) {
                    publisherComboBox.select(0);
                }
                fillOffers();
            });
        });
    }

    private void fillOffers() {
        setPageComplete(false);
        offerComboBox.removeAll();
        skuComboBox.removeAll();
        imageLabelList.removeAll();
        skuComboBox.setEnabled(false);
        imageLabelList.setEnabled(false);
        VirtualMachinePublisher publisher = (VirtualMachinePublisher) publisherComboBox.getData(publisherComboBox.getText());

        AzureTaskManager.getInstance().runInBackground("Loading image offers...", new Runnable() {
            @Override
            public void run() {
                try {
                    final java.util.List<VirtualMachineOffer> offers = publisher.offers().list();
                    DefaultLoader.getIdeHelper().invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            for (VirtualMachineOffer offer : offers) {
                                offerComboBox.add(offer.name());
                                offerComboBox.setData(offer.name(), offer);
                            }
                            offerComboBox.setEnabled(true);
                            if (offers.size() > 0) {
                                offerComboBox.select(0);
                            }
                            fillSkus();
                        }
                    });
                } catch (Exception e) {
                    String msg = "An error occurred while attempting to retrieve offers list." + "\n" + e.getMessage();
                    PluginUtil.displayErrorDialogWithAzureMsg(PluginUtil.getParentShell(), Messages.err, msg, e);
                }
            }
        });
    }

    private void fillSkus() {
        setPageComplete(false);
        imageLabelList.setEnabled(false);
        imageLabelList.removeAll();
        VirtualMachineOffer offer = (VirtualMachineOffer) offerComboBox.getData(offerComboBox.getText());
        if (offerComboBox.getItemCount() > 0) {
            AzureTaskManager.getInstance().runInBackground("Loading skus...", new Runnable() {
                @Override
                public void run() {
                    try {
                        final java.util.List<VirtualMachineSku> skus = offer.skus().list();
                        DefaultLoader.getIdeHelper().invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                for (VirtualMachineSku sku : skus) {
                                    skuComboBox.add(sku.name());
                                    skuComboBox.setData(sku.name(), sku);
                                }
                                skuComboBox.setEnabled(true);
                                if (skus.size() > 0) {
                                    skuComboBox.select(0);
                                }
                                fillImages();
                            }
                        });
                    } catch (Exception e) {
                        String msg = "An error occurred while attempting to retrieve skus list." + "\n" + e.getMessage();
                        PluginUtil.displayErrorDialogWithAzureMsg(PluginUtil.getParentShell(), Messages.err, msg, e);
                    }
                }
            });
        } else {
            // todo
        }
    }

    private void fillImages() {
        setPageComplete(false);
        imageLabelList.removeAll();
        VirtualMachineSku sku = (VirtualMachineSku) skuComboBox.getData(skuComboBox.getText());
        AzureTaskManager.getInstance().runInBackground("Loading images...", new Runnable() {
            @Override
            public void run() {
                final java.util.List<VirtualMachineImage> images = new ArrayList<VirtualMachineImage>();
                try {
                    java.util.List<VirtualMachineImage> skuImages = sku.images().list();
                    images.addAll(skuImages);
                    DefaultLoader.getIdeHelper().invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            for (VirtualMachineImage image : images) {
                                imageLabelList.add(image.version());
                                imageLabelList.setData(image.version(), image);
                            }
                            imageLabelList.setEnabled(true);
                        }
                    });
                } catch (Exception e) {
                    String msg = "An error occurred while attempting to retrieve images list." + "\n" + e.getMessage();
                    PluginUtil.displayErrorDialogWithAzureMsg(PluginUtil.getParentShell(), Messages.err, msg, e);
                }
            }
        });
    }

    private void imageLabelSelected() {
        VirtualMachineImage virtualMachineImage = (VirtualMachineImage) imageLabelList.getData(imageLabelList.getItem(imageLabelList.getSelectionIndex()));
        wizard.setVirtualMachineImage(virtualMachineImage);

        if (virtualMachineImage != null) {
//            imageDescription.setText(wizard.getHtmlFromVMImage(virtualMachineImage));
            setPageComplete(true);

            wizard.setSize(null);
        }
    }

    private void setMachineImage() {
        boolean customImage = customImageBtn.getSelection();
        wizard.setKnownMachineImage(!customImage);
        if (customImage) {
            wizard.setKnownMachineImage(null);
        } else {
            wizard.setKnownMachineImage(knownImageComboBox.getData(knownImageComboBox.getText()));
            wizard.setVirtualMachineImage(null);
        }
    }

    private void validateNext() {
        String region = regionComboBox.getText();
        boolean isValid = !(LOADING.equals(region) || region == null || region.isEmpty() ||
                (customImageBtn.getSelection() && imageLabelList.getSelection().length > 0));
        setPageComplete(isValid);
    }
}
