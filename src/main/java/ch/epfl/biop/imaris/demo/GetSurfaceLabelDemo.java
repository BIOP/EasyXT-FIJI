package ch.epfl.biop.imaris.demo;

import Imaris.Error;
import Imaris.IDataSetPrx;
import Imaris.ISurfacesPrx;
import ch.epfl.biop.imaris.EasyXT;
import ij.ImagePlus;
import net.imagej.ImageJ;

/**
 * EasyXT Demo
 * How to modify a surface in ImageJ and send a modified version to Imaris
 * @author Romain Guiet
 * @author Nicolas Chiaruttini
 * October 2020
 * EPFL - SV - PTECH - PTBIOP
 */

public class GetSurfaceLabelDemo {

    public static void main(String... args) throws Exception {

        try {
            ImageJ ij = new ImageJ();
            ij.ui().showUI();
            // Surface created and shown in ImageJ
            //MakeAndGetSurfaceDemo.main();

            //ImagePlus surfaceIJ = IJ.getImage(); // Surface Image

            // Get Surface
            ISurfacesPrx surface = EasyXT.Surfaces.findAll().get(0);
            long time1 = System.currentTimeMillis();
            //dataset = EasyXT.Utils.getImarisApp().GetFactory().CreateDataSet();
           // ImagePlus imgTest = EasyXT.Surfaces.getSurfacesLabel(surface);
           // imgTest.show();
            ImagePlus image = EasyXT.Surfaces.getLabelsImage(surface);

            image.show();

            //ImagePlus masks = EasyXT.Surfaces.getMaskImage(surface);
            //masks.show();

            IDataSetPrx dataset = EasyXT.Dataset.getCurrent().Clone();

            EasyXT.Dataset.setBitDepth(32, dataset);
            EasyXT.Dataset.addChannels(image, dataset);

            EasyXT.Dataset.setCurrent(dataset);
            long time2 = System.currentTimeMillis();

            EasyXT.log.info("Time: "+(time2-time1) / 1000 +" s" );

        } catch (Error error) {
            error.printStackTrace();
        }

    }
}
