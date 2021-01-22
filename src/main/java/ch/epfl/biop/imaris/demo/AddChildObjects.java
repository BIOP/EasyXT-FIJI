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
            IDataContainerPrx my_group = EasyXT.Scene.createGroup("Spots");

            // Adds it to the scene
            EasyXT.Scene.addToScene(my_group);

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
            EasyXT.Scene.addToScene(my_group, spots);

        } catch (Error error) {
            System.out.println("ERROR:" + error.mDescription);
            System.out.println("LOCATION:" + error.mLocation);
            System.out.println("String:" + error.toString());
        }
    }
}