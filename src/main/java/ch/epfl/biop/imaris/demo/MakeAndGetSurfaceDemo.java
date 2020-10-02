package ch.epfl.biop.imaris.demo;

import Imaris.ISurfacesPrx;
import ch.epfl.biop.imaris.EasyXT;
import ch.epfl.biop.imaris.SurfacesDetector;

public class MakeAndGetSurfaceDemo {

    // Note : you need to be in the 3D View in order to perform

    public static void main(String... args) throws Imaris.Error {

            // Fresh Start with the sample dataset
            FreshStartWithIJAndBIOPImsSample.main();

            // Makes a surface detector and detect the surface
            ISurfacesPrx surf = SurfacesDetector.Channel(0)
                    .setSmoothingWidth(5)
                    .setLowerThreshold(300)
                    .setName("My Surface")
                    .setColor(new Integer[]{255,120,45})
                    .build()
                    .detect();

            // Adds the surface to the scene
            EasyXT.getImaris().GetSurpassScene().AddChild(surf,0);

            // Gets an existing surface
            surf = EasyXT.getSurfaces( "My Surface" );

            // Display surfaces
            EasyXT.getSurfaceMask( surf ).show();


            /*ISpotsPrx spots = ch.epfl.biop.imaris.SpotsDetector.Channel(2)
                                .setDiameter(5)
                                .isSubtractBackground(true)
                                .setName("Spot from FIJI")
                                .build()
                                .detect();

            // Adds the spots to the scene
            EasyXT.getImaris().GetSurpassScene().AddChild(spots,0);*/

            //EasyXT.getSpots

    }
}
