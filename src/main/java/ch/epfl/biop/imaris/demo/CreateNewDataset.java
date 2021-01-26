package ch.epfl.biop.imaris.demo;

import Imaris.Error;
import Imaris.IDataSetPrx;
import ch.epfl.biop.imaris.EasyXT;
import ij.IJ;
import ij.ImagePlus;

/**
 * EasyXT Demo
 * This demo shows how you can send a dataset directly to Imaris from an ImagePlus
 * @author Olivier Burri
 * January 2021
 * EPFL - SV - PTECH - PTBIOP
 */
public class CreateNewDataset {
    public static void main(String[] args) throws Error {
        EasyXT.main(args);
        ImagePlus imp = IJ.openImage("https://imagej.net/images/confocal-series.zip");

        // This creates the dataset from the given ImagePlus
        IDataSetPrx dataset = EasyXT.Dataset.create(imp);
        EasyXT.Dataset.setCurrent(dataset);
    }
}
