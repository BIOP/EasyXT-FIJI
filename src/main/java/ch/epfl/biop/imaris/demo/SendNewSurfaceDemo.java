package ch.epfl.biop.imaris.demo;

import Imaris.Error;
import Imaris.ISurfacesPrx;
import ch.epfl.biop.imaris.EasyXT;
import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.gui.WaitForUserDialog;

/**
 * EasyXT Demo
 *
 * How to modify a surface in ImageJ and send a modified version to Imaris
 * TODO name is confusing, may we consider renaming it to UpdateSurfaceDemo as we're not really sending a new surface but just modifying an existing one
 *
 *
 *
 * @author Romain Guiet
 * @author Nicolas Chiaruttini
 *
 * October 2020
 *
 * EPFL - SV -PTECH - PTBIOP
 */

public class SendNewSurfaceDemo {

    public static void main(String... args) throws Exception {

        try {
            // Surface created and shown in ImageJ
            MakeAndGetSurfaceDemo.main();

            ImagePlus surface_ij = IJ.getImage(); // Surface Image

            if ((args.length>0)&&(args[0].equals("Test Mode"))) {
                IJ.log("The surface will be skeletonized ...");
            } else {
                new WaitForUserDialog("The surface will be skeletonized ...").show();
            }
            // because of skeletonize
            Prefs.blackBackground = true;
            IJ.run(surface_ij,"Skeletonize", "stack");
            //IJ.run(surface_ij, "Invert", "stack");

            if ((args.length>0)&&(args[0].equals("Test Mode"))) {
                IJ.log("And sent back to Imaris ...");
            } else {
                new WaitForUserDialog("And sent back to Imaris ...").show();
            }

            ISurfacesPrx surface = EasyXT.getAllSurfaces().get(0);

            EasyXT.setSurfacesMask(surface, surface_ij);

            surface_ij.changes = false;

        } catch (Error error) {
            error.printStackTrace();
        }

    }
}
