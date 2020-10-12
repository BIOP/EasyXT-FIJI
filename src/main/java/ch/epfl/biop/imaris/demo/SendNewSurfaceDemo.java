package ch.epfl.biop.imaris.demo;

import Imaris.Error;
import Imaris.ISurfacesPrx;
import ch.epfl.biop.imaris.EasyXT;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.WaitForUserDialog;

/**
 * EasyXT Demo
 *
 * How to modify a surface in ImageJ and send a modified version to Imaris
 *
 * @author BIOP Team, EPFL 2020
 *
 */

public class SendNewSurfaceDemo {

    public static void main(String... args) {

        try {
            // Surface created and shown in ImageJ
            MakeAndGetSurfaceDemo.main();

            ImagePlus surface_ij = IJ.getImage(); // Surface Image

            new WaitForUserDialog("The surface will be skeletonized ...").show();

            IJ.run(surface_ij,"Skeletonize", "stack");
            IJ.run(surface_ij, "Invert", "stack");

            new WaitForUserDialog("And sent back to Imaris ...").show();

            ISurfacesPrx surface = EasyXT.getAllSurfaces().get(0);

            EasyXT.setSurfaceMask(surface, surface_ij);

            surface_ij.changes = false;

        } catch (Error error) {
            error.printStackTrace();
        }

    }
}
