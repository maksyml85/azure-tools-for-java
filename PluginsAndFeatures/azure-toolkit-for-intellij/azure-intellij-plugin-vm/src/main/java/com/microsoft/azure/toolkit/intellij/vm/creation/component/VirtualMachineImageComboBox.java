package com.microsoft.azure.toolkit.intellij.vm.creation.component;

import com.intellij.icons.AllIcons;
import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.compute.vm.AzureImage;
import com.microsoft.azure.toolkit.lib.compute.vm.AzureVirtualMachine;
import com.microsoft.azure.toolkit.lib.compute.vm.model.OperatingSystem;
import lombok.Setter;
import org.apache.commons.collections.ListUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class VirtualMachineImageComboBox extends AzureComboBox<AzureImage> {
    @Setter
    private Subscription subscription;
    @Setter
    private Region region;
    private AzureImage customImage;

    @Override
    protected ExtendableTextComponent.Extension getExtension() {
        return ExtendableTextComponent.Extension.create(AllIcons.General.OpenDisk, "Select marketplace image", this::selectImage);
    }

    private void selectImage() {
        if (region == null || subscription == null) {
            AzureMessager.getMessager().warning("PLease select subscription&region first");
        }
        final VirtualMachineImageDialog dialog = new VirtualMachineImageDialog(subscription, region);
        if (dialog.showAndGet()) {
            customImage = dialog.getData();
            refreshItems();
            setValue(customImage);
        }
    }

    @Nullable
    @Override
    protected Icon getItemIcon(Object item) {
        if(item instanceof AzureImage){
            return ((AzureImage) item).getOperatingSystem() == OperatingSystem.Linux ? AllIcons.RunConfigurations.Wsl : AllIcons.Gutter.Colors;
        }
        return super.getItemIcon(item);
    }

    @Nonnull
    @Override
    protected List<? extends AzureImage> loadItems() throws Exception {
        final List<AzureImage> knownImages = Azure.az(AzureVirtualMachine.class).getKnownImages();
        return Optional.ofNullable(customImage).map(image -> ListUtils.union(knownImages, Collections.singletonList(image))).orElse(knownImages);
    }

    @Override
    protected String getItemText(Object item) {
        return item instanceof AzureImage ? String.format("%s %s", ((AzureImage) item).offer(), ((AzureImage) item).sku()) : super.getItemText(item);
    }
}
