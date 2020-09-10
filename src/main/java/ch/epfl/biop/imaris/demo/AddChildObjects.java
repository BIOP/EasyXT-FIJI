package ch.epfl.biop.imaris.demo;

import Imaris.Error;
import Imaris.IDataContainerPrx;
import Imaris.IDataSetPrx;
import Imaris.ISpotsPrx;
import ch.epfl.biop.imaris.EasyXT;
import ch.epfl.biop.imaris.ImarisCalibration;
import ch.epfl.biop.imaris.SpotsDetector;
import ij.IJ;
import ij.ImagePlus;
import net.imagej.ImageJ;
import sun.font.EAttribute;

public class AddChildObjects {


    public static void main( String... args ) {
        try {
            // Launch FIJI
            ImageJ ij = new ImageJ( );

            ij.ui( ).showUI( );

            //Get Extents of currently open dataset to create he same thing, but with two channels
            IDataContainerPrx new_group = EasyXT.createGroup( "My Spots" );
            EasyXT.addToScene( new_group );

            // Make another one
            ISpotsPrx spots = SpotsDetector.Channel( 0 )
                    .setName( "Spots" )
                    .setDiameter( 3.0 )
                    .setRegionsThresholdManual( 100 )
                    .isSubtractBackground( true )
                    .isRegionsFromLocalContrast( true )
                    .isRegionsSpotsDiameterFromVolume( false )
                    .createRegionsChannel()
                    .build( ).detect( );
            EasyXT.addToScene( new_group, spots );



        } catch ( Error error ) {
            System.out.println( "ERROR:" + error.mDescription );
            System.out.println( "LOCATION:" + error.mLocation );
            System.out.println( "String:" + error.toString() );
        }
    }
}