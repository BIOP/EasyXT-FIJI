package ch.epfl.biop.imaris.demo;

import Imaris.Error;
import Imaris.ISpotsPrx;
import ch.epfl.biop.imaris.EasyXT;
import ch.epfl.biop.imaris.SpotsDetector;

/**
 * EasyXT Demo
 *
 * How to make a surface using SurfaceDetector and show it in ImageJ
 *
 * @author Nicolas Chiaruttini
 *
 * October 2020
 *
 * EPFL - SV -PTECH - PTBIOP
 */

public class MakeAndGetSpotsDemo {

    // Note : you need to be in the 3D View in order to perform

    public static void main(String... args) {
        try {
            // Fresh Start with the sample dataset
            FreshStartWithIJAndBIOPImsSample.main();

            // Makes a surface detector and detect the surface
            ISpotsPrx detected_spots = SpotsDetector.Channel(0)
                    .setName("My Spots")
                    .setDiameter(3)
                    .setFilter("\"Quality\" above 25")
                    .isSubtractBackground(true)
                    .setColor(new Integer[]{255,120,45})
                    .build()
                    .detect();

            // Adds the surface to the scene
            EasyXT.addToScene(detected_spots);

            // Gets an existing surface
            ISpotsPrx got_spots = EasyXT.getSpots( "My Spots" );

            // TODO : Display spots in Fiji
            // EasyXT.getSpotsMask( got_spots ).show();


        } catch ( Error error ) {
            System.out.println( "ERROR:" + error.mDescription );
            System.out.println( "LOCATION:" + error.mLocation );
            System.out.println( "String:" + error.toString() );
        }
    }
}
