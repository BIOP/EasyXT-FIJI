package ch.epfl.biop.imaris.demo;

import Imaris.Error;
import Imaris.IDataSetPrx;
import ch.epfl.biop.imaris.EasyXT;
import ch.epfl.biop.imaris.ImarisCalibration;
import ij.ImagePlus;
import net.imagej.ImageJ;
import ij.IJ;
public class AddChannelsToDataset {


    public static void main( String... args ) {
        try {
            // Launch FIJI
            ImageJ ij = new ImageJ( );

            ij.ui( ).showUI( );

            //Get Extents of currently open dataset to create the same thing, but with two channels
            IDataSetPrx dataset = EasyXT.getCurrentDataset( );
            IJ.log( "Dataset Recovered" );
            ImarisCalibration calibration = new ImarisCalibration( dataset );
            ImagePlus channels_imp = IJ.createImage( "HyperStack", calibration.bitDepth + "-bit color-mode label", calibration.xSize, calibration.ySize, 2, calibration.zSize, calibration.tSize );
            channels_imp.show( );
            IDataSetPrx newDataset = dataset.Clone( );
            EasyXT.addChannels( newDataset, channels_imp );
            EasyXT.getImaris().SetDataSet( newDataset );

        } catch ( Error error ) {
            System.out.println( "ERROR:" + error.mDescription );
            System.out.println( "LOCATION:" + error.mLocation );
            System.out.println( "String:" + error.toString() );
        }
    }
}