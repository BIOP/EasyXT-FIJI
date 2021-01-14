package ch.epfl.biop.imaris.demo;

import Imaris.IApplicationPrx;
import Imaris.IDataSetPrx;
import Imaris.ISurfacesPrx;
import ch.epfl.biop.imaris.EasyXT;
import ch.epfl.biop.imaris.ImarisCalibration;
import ch.epfl.biop.imaris.SurfacesDetector;
import net.imagej.ImageJ;
import ij.*;

public class GetSurfacesLabelDemo {

    public static void main(String... args) throws Imaris.Error {
        ImageJ ij = new ImageJ();
        ij.ui().showUI();

        // Fresh Start with the sample dataset
        FreshStartWithIJAndBIOPImsSample.main();

        // get Imaris App if you need to use Imaris API
        IApplicationPrx imaris_app = EasyXT.getImaris();
        // eg to get the dataset we can use and here the dataset with calibration
        IDataSetPrx dataset = imaris_app.GetDataSet();

        // Makes a surface detector and detect the surface
        ISurfacesPrx surface = SurfacesDetector.Channel(2)
                .setSmoothingWidth(0.275)
                .setLocalContrastFilterWidth(1.0)
                .setLowerThreshold(40)
                .setUpperThreshold(255.0)
                .setName("My Surface")
                .setColor(new Integer[]{255, 120, 45})
                .build()
                .detect();
        EasyXT.addToScene(surface);

        // Here we ask for a Label image of the surfaces
        ImagePlus label_imp = EasyXT.getSurfacesLabel(surface);
        label_imp.show();

        // The bit_depth of the Label Image depends of the number of object
        // to be able to add the label as a new channel to the dataset,
        // we convert the dataset to the compatible type
        EasyXT.setDataType(label_imp.getBitDepth());

        // Now we add the Label image as a channel to the dataset of the scene
        // The fastest way is to clone the dataset,add the imp to it
        // and finally set the current dataset.
        IDataSetPrx newDataset = dataset.Clone();
        EasyXT.addChannels(newDataset, label_imp);
        EasyXT.setCurrentDataset(newDataset);

    }

}
