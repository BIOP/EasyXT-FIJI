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
import Imaris.IDataContainerPrx;
import Imaris.ISpotsPrx;
import ch.epfl.biop.imaris.EasyXT;

/**
 * EasyXT Demo
 * <p>
 * Show how to insert objects into the scene of Imaris, here : spots
 *
 * @author Nicolas Chiaruttini
 * <p>
 * October 2020
 * <p>
 * EPFL - SV -PTECH - PTBIOP
 */

public class AddChildObjects {

    public static void main(String... args) throws Exception {
        try {
            // Fresh Start with the sample dataset
            FreshStartWithIJAndBIOPImsSample.main();

            // Creates a group
            IDataContainerPrx myGroup = EasyXT.Scene.createGroup("Spots");

            // Adds it to the scene
            EasyXT.Scene.addItem(myGroup);

            // Makes a Spot Detector and detect them
            ISpotsPrx spots = EasyXT.Spots.create(2)
                    .setName("My Spots")
                    .setDiameter(3.0)
                    .setRegionsThresholdManual(100)
                    .isSubtractBackground(true)
                    .isRegionsFromLocalContrast(true)
                    .isRegionsSpotsDiameterFromVolume(false)
                    .isCreateRegionsChannel(false)
                    .build().detect();

            // Adds the detected spots into the 'my_group' group
            EasyXT.Scene.addItem(spots, myGroup);

        } catch (Error error) {
            System.out.println("ERROR:" + error.mDescription);
            System.out.println("LOCATION:" + error.mLocation);
            System.out.println("String:" + error);
        }
    }
}
