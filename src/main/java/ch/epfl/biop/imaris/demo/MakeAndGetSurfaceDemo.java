package ch.epfl.biop.imaris.demo;

import Imaris.Error;
import Imaris.ISurfacesPrx;
import ch.epfl.biop.imaris.EasyXT;
import ch.epfl.biop.imaris.SurfacesDetector;

/**
 * EasyXT Demo
 *
 * How to make a surface using SurfaceDetector and show it in ImageJ
 *
 * @author BIOP Team, EPFL 2020
 *
 */

public class MakeAndGetSurfaceDemo {

    // Note : you need to be in the 3D View in order to perform

    public static void main(String... args) {
        try {
            // Fresh Start with the sample dataset
            FreshStartWithIJAndBIOPImsSample.main();

            // Makes a surface detector and detect the surface
            ISurfacesPrx surface = SurfacesDetector.Channel(0)
                    .setSmoothingWidth(1)
                    .setLowerThreshold(40)
                    .setName("My Surface")
                    .setColor(new Integer[]{255,120,45})
                    .build()
                    .detect();

            // Adds the surface to the scene
            EasyXT.getImaris().GetSurpassScene().AddChild(surface,0);

            // Gets an existing surface
            surface = EasyXT.getSurfaces( "My Surface" );

            // Display surfaces
            EasyXT.getSurfaceMask( surface ).show();
        } catch ( Error error ) {
            System.out.println( "ERROR:" + error.mDescription );
            System.out.println( "LOCATION:" + error.mLocation );
            System.out.println( "String:" + error.toString() );
        }
    }
}
