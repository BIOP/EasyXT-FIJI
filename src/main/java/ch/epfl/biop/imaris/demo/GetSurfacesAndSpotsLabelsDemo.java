package ch.epfl.biop.imaris.demo;

import Imaris.IDataSetPrx;
import Imaris.ISpotsPrx;
import Imaris.ISurfacesPrx;
import ch.epfl.biop.imaris.EasyXT;
import ij.ImagePlus;
import net.imagej.ImageJ;

import java.net.URISyntaxException;

public class GetSurfacesAndSpotsLabelsDemo {

    public static void main(String... args) throws Imaris.Error, URISyntaxException {
        ImageJ ij = new ImageJ();
        ij.ui().showUI();

        // Fresh Start with the sample dataset
        FreshStartWithIJAndBIOPImsSample.main();

        // Makes a surface detector and detect the surface
        ISurfacesPrx surface = EasyXT.Surfaces.create(2)
                .setSmoothingWidth(0.275)
                .setLocalContrastFilterWidth(1.0)
                .setLowerThreshold(40)
                .setUpperThreshold(255.0)
                .setName("My Surface")
                .setColor(new Integer[]{255, 120, 45})
                .build()
                .detect();
        EasyXT.Scene.addItem(surface);

        // Here we ask for a Label image of the surfaces
        ImagePlus label_imp = EasyXT.Surfaces.getLabelsImage(surface);
        label_imp.show();

        // The bit_depth of the Label Image depends of the number of object
        // to be able to add the label as a new channel to the dataset,
        // we convert the dataset to the compatible type
        EasyXT.Dataset.setBitDepth(label_imp.getBitDepth());

        // Makes a surface detector and detect the surface
        ISpotsPrx detected_ellipticSpots = EasyXT.Spots.create(0)
                .setName("My Elliptic Region Grown Spots")
                //.setDiameter( 1.0 )// [Source Channel] Estimated XY Diameter
                //.setAxialDiameter(2.0)// [Source Channel] Estimated Z Diameter
                .setDiameterXYZ(1.0, 2.0)// alternative  that sets both parameters
                .isSubtractBackground(true)// [Source Channel] Background Substraction = true
                .setFilter("\"Quality\" above 15.0")// [Classify Spots] A String, "\" as escape character
                .isRegionsFromLocalContrast(true)// [Spot Region Type] Region Growing  = Local Contrast (if set to false => intensity)
                .setRegionsThresholdManual(40)// [Spot Regions] Region Growing Manual Threshold (Automatic Threshold is set to false if setting manual value)
                .isRegionsSpotsDiameterFromVolume(true)// [Spot Regions] Region Growing Diameter = Diameter From Volume
                .isCreateRegionsChannel(false)// [Spot Regions] Create Region Channel
                .setColor(new Integer[]{255, 128, 0})
                .build()
                .detect();
        EasyXT.Scene.addItem(detected_ellipticSpots);

        ImagePlus spots_label_imp = EasyXT.Spots.getLabelsImage(detected_ellipticSpots);
        spots_label_imp.show();

        // Now we add the Label image as a channel to the dataset of the scene
        // The fastest way is to:
        //  - clone the dataset,
        //  - add the imp to it
        //  - and finally set the current dataset.
        IDataSetPrx newDataset = EasyXT.Dataset.getCurrent().Clone();
        EasyXT.Dataset.addChannels(label_imp, newDataset);
        EasyXT.Dataset.addChannels(spots_label_imp, newDataset);
        EasyXT.Dataset.setCurrent(newDataset);

        System.out.println("Done!");

    }

}
