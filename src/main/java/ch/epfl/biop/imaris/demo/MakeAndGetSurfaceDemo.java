package ch.epfl.biop.imaris.demo;

import Imaris.Error;
import Imaris.ISurfacesPrx;
import ch.epfl.biop.imaris.EasyXT;
import ch.epfl.biop.imaris.SurfacesDetector;
import net.imagej.ImageJ;

public class MakeAndGetSurfaceDemo {


    public static void main(String... args) {


        try {
            // Launch FIJI
            ImageJ ij = new ImageJ();
            ij.ui().showUI();

            // Makes a surface detector and detect the surface

            ISurfacesPrx surf = SurfacesDetector.Channel(0)
                    .setSmoothingWidth(5)
                    .setLowerThreshold(103)
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

        } catch ( Error error ) {
            System.out.println( "ERROR:"+ error.mDescription);
        }

    }
}
