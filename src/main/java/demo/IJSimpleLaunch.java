package demo;
import Imaris.Error;

import Imaris.ISurfacesPrx;
import ch.epfl.biop.imaris.EasyXT;
import ch.epfl.biop.imaris.SurfacesDetector;
import net.imagej.ImageJ;

public class IJSimpleLaunch {

    public static void main(String... args) {


        try {
            //ISpotsPrx spots = e.getSpotsObject( "Spots From neutro" );

            // Arrange
            // create the ImageJ application context with all available services
            ImageJ ij = new ImageJ();
            ij.ui().showUI();
            //ij.setVisible(true);

            // Makes a surface detector and detect the surface

            ISurfacesPrx surf = SurfacesDetector.Channel(0)
                    .setSmoothingWidth(5)
                    .setLowerThreshold(103)
                    .setName("My Surface")
                    .setColor(new Integer[]{255,120,45})
                    .build()
                    .detect();

            // Adds the surface to the scene
            EasyXT.getApp().GetSurpassScene().AddChild(surf,0);


            // Gets an existing surface

            //surf = EasyXT.getSurfaces( "My surface" );

            // Display surfaces

            EasyXT.getSurfaceMask( surf ).show();


            /*ISpotsPrx spots = ch.epfl.biop.imaris.SpotsDetector.Channel(2)
                                .setDiameter(5)
                                .isSubtractBackground(true)
                                .setName("Spot from FIJI")
                                .build()
                                .detect();

            // Adds the spots to the scene
            EasyXT.getApp().GetSurpassScene().AddChild(spots,0);*/

            //EasyXT.getSpots

        } catch ( Error error ) {
            System.out.println( "ERROR:"+ error.mDescription);
        }

    }
}