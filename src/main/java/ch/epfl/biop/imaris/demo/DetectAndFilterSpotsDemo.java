/*-
 * #%L
 * API and commands to facilitate communication between Imaris and FIJI
 * %%
 * Copyright (C) 2020 - 2021 ECOLE POLYTECHNIQUE FEDERALE DE LAUSANNE, Switzerland, BioImaging And Optics Platform (BIOP)
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

import Imaris.IDataItemPrx;
import Imaris.ISpotsPrx;
import ch.epfl.biop.imaris.EasyXT;

import java.io.File;

public class DetectAndFilterSpotsDemo {

    public static void main(String... args) throws Exception, Error {

        // Imaris comes with a couple of images, stored into a user folder...
        // "celldemo.ims" , ...
        File image_path = EasyXT.Samples.getImarisDemoFile("celldemo.ims");
        //println image_path

        EasyXT.Files.openImage(image_path);

        // Detect surfaces
        //.setDiameter( 1.0 )// [Source Channel] Estimated XY Diameter
        //.setAxialDiameter(2.0)// [Source Channel] Estimated Z Diameter
        // alternative  that sets both parameters
        // [Source Channel] Background Substraction = true
        // [Classify Spots] A String, "\" as escape character
        // [Spot Region Type] Region Growing  = Local Contrast (if set to false => intensity)
        // [Spot Regions] Region Growing Manual Threshold (Automatic Threshold is set to false if setting manual value)
        // [Spot Regions] Region Growing Diameter = Diameter From Volume
        // [Spot Regions] Create Region Channel
        ISpotsPrx spots = EasyXT.Spots.create(0)
                .setName("My Elliptic Region Grown Spots")
                //.setDiameter( 1.0 )// [Source Channel] Estimated XY Diameter
                //.setAxialDiameter(2.0)// [Source Channel] Estimated Z Diameter
                .setDiameterXYZ(1.0, 2.0)// alternative  that sets both parameters
                .isSubtractBackground(true)// [Source Channel] Background Substraction = true
                .setFilter("\"Quality\" above 15.0")// [Classify Spots] A String, "\" as escape character
                .isRegionsFromLocalContrast(true)// [Spot Region Type] Region Growing  = Local Contrast (if set to false => intensity)
                .setRegionsThresholdManual(40)// [Spot Regions] Region Growing Manual Threshold (Automatic Threshold is set to false if setting manual value)
                .isRegionsSpotsDiameterFromVolume(true)// [Spot Regions] Region Growing Diameter = Diameter From Volume
                .isCreateRegionsChannel(false)// [Spot Regions] Create Region Channel
                .setColor(new Integer[]{255, 128, 0})
                .build()
                .detect();

        //uncomment below to have the "non-tracked" surface
        EasyXT.Scene.setName(spots, "spots");
        EasyXT.Scene.addItem(spots);

        IDataItemPrx filtered_spots = EasyXT.Utils.filter(spots, "Volume", 20, 100);
        EasyXT.Scene.setName(filtered_spots, "filtered_spots");
        EasyXT.Scene.addItem(filtered_spots);

    }
}
