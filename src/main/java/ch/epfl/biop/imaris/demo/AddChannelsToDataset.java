package ch.epfl.biop.imaris.demo;

import Imaris.Error;
import Imaris.IDataSetPrx;
import ch.epfl.biop.imaris.EasyXT;
import ch.epfl.biop.imaris.ImarisCalibration;
import ij.ImagePlus;
import ij.gui.WaitForUserDialog;
import ij.IJ;

/**
 * EasyXT Demo
 *
 * Show how to add a channel to the imaris dataset
 *
 * Two ways are provided, a fast and a slow one.
 *
 * @author BIOP Team, EPFL 2020
 *
 */

public class AddChannelsToDataset {

    public static void main( String... args ) {
        try {
            // Fresh Start with the BIOP sample dataset
            FreshStartWithIJAndBIOPImsSample.main();

            //Get Extents of currently open dataset to create the same thing, but with two channels
            IDataSetPrx dataset = EasyXT.getCurrentDataset( );

            ImarisCalibration cal = new ImarisCalibration( dataset );
            int bitDepth = EasyXT.getBitDepth( dataset );

            new WaitForUserDialog("Creating a new dataset, adding channels and sending the dataset back").show();
            long t0 = System.currentTimeMillis( );

            ImagePlus channels_imp1 = IJ.createImage( "HyperStack One", bitDepth + "-bit color-mode label", cal.xSize, cal.ySize, 2, cal.zSize, cal.tSize );
            channels_imp1.show( );

            // Copy the dataset
            IDataSetPrx newDataset = dataset.Clone( );
            EasyXT.addChannels( newDataset, channels_imp1 );

            // Place the dataset into the scene
            EasyXT.setCurrentDataset( newDataset );
            long t1 = System.currentTimeMillis( ) - t0;
            IJ.log( "New dataset time: "+t1+" ms" );

            new WaitForUserDialog("We will now add channels to the current dataset").show();

            long t2 = System.currentTimeMillis();
            ImagePlus channels_imp2 = IJ.createImage( "HyperStack Two", bitDepth + "-bit color-mode label", cal.xSize, cal.ySize, 2, cal.zSize, cal.tSize );
            channels_imp2.show();
            EasyXT.addChannels( channels_imp2 );
            long t3 = System.currentTimeMillis( ) - t2;
            IJ.log( "Append to current  dataset time: "+t3+" ms" );

        } catch ( Error error ) {
            System.out.println( "ERROR:" + error.mDescription );
            System.out.println( "LOCATION:" + error.mLocation );
            System.out.println( "String:" + error.toString() );
        }
    }
}