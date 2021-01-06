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
                    .setColor(new Integer[]{255,128,0})
                    .build()
                    .detect();

            // Adds the surface to the scene
            EasyXT.addToScene(detected_spots);

            // Gets an existing surface
            ISpotsPrx got_spots = EasyXT.getSpots( "My Spots" );

            ISpotsPrx detected_ellipticSpots = SpotsDetector.Channel(0)
                    .setName("My Elliptic Region Grown Spots")
                    //.setDiameter( 1.0 )					// [Source Channel] Estimated XY Diameter
                    //.setAxialDiameter(2.0)				// [Source Channel] Estimated Z Diameter
                    .setDiameterXYZ(  1.0 , 2.0 ) // alternative  that sets both parameters
                    .isSubtractBackground(true)				// [Source Channel] Background Substraction = true
                    .setFilter("\"Quality\" above 15.0")	// [Classify Spots] A String "\" as escape character
                    .isRegionsFromLocalContrast(true) 		// [Spot Region Type] Region Growing  = Local Contrast (if set to false => intensity)
                    .setRegionsThresholdManual(40)			// [Spot Regions] Region Growing Manual Threshold (Automatic Threshold is set to false if setting manual value)
                    .isRegionsSpotsDiameterFromVolume(true)	// [Spot Regions] Region Growing Diameter = Diameter From Volume
                    .isCreateRegionsChannel(false)			// [Spot Regions] Create Region Channel
                    .setColor(new Integer[]{255,128,0})
                    .build()
                    .detect();

            // Adds the surface to the scene
            EasyXT.addToScene(detected_ellipticSpots);

            // TODO : Display spots in Fiji
            // EasyXT.getSpotsMask( got_spots ).show();


        } catch ( Error error ) {
            System.out.println( "ERROR:" + error.mDescription );
            System.out.println( "LOCATION:" + error.mLocation );
            System.out.println( "String:" + error.toString() );
        }
    }
}
