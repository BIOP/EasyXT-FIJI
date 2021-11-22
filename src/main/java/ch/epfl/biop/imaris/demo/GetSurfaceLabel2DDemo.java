package ch.epfl.biop.imaris.demo;

import Imaris.IDataSetPrx;
import Imaris.ISurfacesPrx;
import ch.epfl.biop.imaris.EasyXT;
import ij.IJ;
import ij.ImagePlus;
import net.imagej.ImageJ;

/**
 * EasyXT Demo
 * <p>
 * Makes surfaces on a 2D image.
 *
 * @author Romain Guiet
 * @author Nicolas Chiaruttini
 * <p>
 * November 2021
 * <p>
 * EPFL - SV - PTECH - PTBIOP
 */


public class GetSurfaceLabel2DDemo {

    public static void main(String... args) throws Exception {
        net.imagej.ImageJ ij = new ImageJ();
        ij.ui().showUI();

        EasyXT.Scene.reset();
        IJ.run("Close All", "");

        ImagePlus blobs = IJ.openImage("http://imagej.nih.gov/ij/images/blobs.gif");
        IJ.run(blobs, "Invert LUT", "");

        blobs.show();

        // load an image in Imaris, 2D or 3D depending of try2D  true or flase
        IDataSetPrx dataset = EasyXT.Dataset.create(blobs);
        EasyXT.Dataset.setCurrent(dataset);

        // Here EasyXT makes Imaris create/detect a Surfaces object : "surf"
        ISurfacesPrx surface = EasyXT.Surfaces.create(0)
                .setSmoothingWidth(1)
                .enableAutomaticLowerThreshold() // default behavior
                //.setLowerThreshold(80) // possible to change value to a defined one
                .setSurfaceFilter("\"Volume\" above 1.0 um^3")
                .setName("Nuclei")
                .build()
                .detect();

        // we now add "surf" to the scene
        EasyXT.Scene.addItem(surface);

        // Here we can retrieve the surface
        ISurfacesPrx getSurf = EasyXT.Scene.findSurfaces("Nuclei");

        ImagePlus mask_imp = EasyXT.Surfaces.getMaskImage(getSurf);
        mask_imp.show();

        ImagePlus label_imp = EasyXT.Surfaces.getLabelsImage(getSurf);
        label_imp.show();
    }
}
