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

import Imaris.Error;
import Imaris.ISpotsPrx;
import ch.epfl.biop.imaris.EasyXT;
import ch.epfl.biop.imaris.SpotsDetector;

/**
 * EasyXT Demo
 * <p>
 * How to make a surface using SurfaceDetector and show it in ImageJ
 *
 * @author Nicolas Chiaruttini
 * @author Romain Guiet
 * <p>
 * January 2021
 * <p>
 * EPFL - SV -PTECH - PTBIOP
 */

public class MakeAndGetSpotsDemo {

    // Note : you need to be in the 3D View in order to perform

    public static void main(String... args) throws Exception {
        try {
            // Fresh Start with the sample dataset
            FreshStartWithIJAndBIOPImsSample.main();

            // Makes a surface detector and detect the surface
            ISpotsPrx detected_spots = SpotsDetector.Channel(0)
                    .setName("My Spots")
                    .setDiameter(1)
                    .setFilter("\"Quality\" above 15")
                    .isSubtractBackground(true)
                    .setColor(new Integer[]{255, 128, 0})
                    .build()
                    .detect();

            // Adds the surface to the scene
            EasyXT.Scene.addItem(detected_spots);

            // Gets an existing surface
            ISpotsPrx got_spots = EasyXT.Spots.find("My Spots");

            ISpotsPrx detected_ellipticSpots = EasyXT.Spots.create(0)
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

            // Adds the surface to the scene
            EasyXT.Scene.addItem(detected_ellipticSpots);

            // Display spots in Fiji
            EasyXT.Spots.getMaskImage(got_spots).show();
            EasyXT.Spots.getLabelsImage(got_spots).show();

        } catch (Error error) {
            error.printStackTrace();
        }
    }
}
