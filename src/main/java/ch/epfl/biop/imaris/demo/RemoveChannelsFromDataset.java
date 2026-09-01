/*-
 * #%L
 * API and commands to facilitate communication between Imaris and FIJI
 * %%
 * Copyright (C) 2020 - 2024 ECOLE POLYTECHNIQUE FEDERALE DE LAUSANNE, Switzerland, BioImaging And Optics Platform (BIOP)
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */
package ch.epfl.biop.imaris.demo;

import Imaris.Error;
import Imaris.IDataSetPrx;
import ch.epfl.biop.imaris.EasyXT;
import ch.epfl.biop.imaris.ImarisCalibration;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.WaitForUserDialog;

/**
 * EasyXT Demo
 * <p>
 * Shows how to remove channels from the current Imaris dataset, the reverse of
 * {@link AddChannelsToDataset}.
 * <p>
 * We start from the BIOP sample dataset, append two extra channels so the dataset
 * has something to remove, then remove them.
 */
public class RemoveChannelsFromDataset {

    public static void main(String... args) throws Exception {
        try {
            FreshStartWithIJAndBIOPImsSample.main();

            IDataSetPrx dataset = EasyXT.Dataset.getCurrent();
            ImarisCalibration cal = new ImarisCalibration(dataset);
            int bitDepth = EasyXT.Dataset.getBitDepth(dataset);
            int originalC = dataset.GetSizeC();

            // Append two dummy channels so we have something to remove.
            ImagePlus extraChannels = IJ.createImage("Extra Channels",
                    bitDepth + "-bit color-mode label",
                    cal.xSize, cal.ySize, 2, cal.zSize, cal.tSize);
            extraChannels.show();
            EasyXT.Dataset.addChannels(extraChannels);

            int afterAddC = EasyXT.Dataset.getCurrent().GetSizeC();
            IJ.log("Channels after add: " + afterAddC + " (started with " + originalC + ")");

            new WaitForUserDialog(
                    "Dataset now has " + afterAddC + " channels. Click OK to remove the two we just added."
            ).show();

            // Remove the two trailing channels we just added.
            long t0 = System.currentTimeMillis();
            EasyXT.Dataset.removeChannels(afterAddC - 2, afterAddC - 1);
            long dt = System.currentTimeMillis() - t0;
            IJ.log("Remove trailing channels: " + dt + " ms");

            int afterRemoveC = EasyXT.Dataset.getCurrent().GetSizeC();
            IJ.log("Channels after remove: " + afterRemoveC);

            // Demonstrate removing a channel from the middle (if there are at least 3 channels).
            if (afterRemoveC >= 3) {
                new WaitForUserDialog(
                        "Now removing the middle channel (index 1) to demonstrate non-contiguous removal."
                ).show();
                long t2 = System.currentTimeMillis();
                EasyXT.Dataset.removeChannels(1);
                long dt2 = System.currentTimeMillis() - t2;
                IJ.log("Remove middle channel: " + dt2 + " ms");
                IJ.log("Channels after middle remove: " + EasyXT.Dataset.getCurrent().GetSizeC());
            }

        } catch (Error error) {
            System.out.println("ERROR:" + error.mDescription);
            System.out.println("LOCATION:" + error.mLocation);
            System.out.println("String:" + error);
        }
    }
}