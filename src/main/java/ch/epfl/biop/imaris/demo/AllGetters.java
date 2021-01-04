package ch.epfl.biop.imaris.demo;

import Imaris.*;
import Imaris.Error;
import ch.epfl.biop.imaris.EasyXT;
import ch.epfl.biop.imaris.ItemQuery;
import ch.epfl.biop.imaris.SpotsDetector;
import ch.epfl.biop.imaris.SurfacesDetector;
import ij.IJ;

import java.util.List;

/**
 * EasyXT Demo
 *
 * After creations of Spots and a Surface, demo of how accessing objects in the imaris scene
 *
 * This showcases how to create groups, and how to recover objects from the surpass scene
 *
 * The simplest way is to use EasyXT directly, but a more fine-tuning is possible using
 * the {@link ItemQuery} class directly, which is the last example in this demo.
 *
 * @author Olivier Burri
 * @author Nicolas Chiaruttini
 *
 * October 2020
 *
 * EPFL - SV -PTECH - PTBIOP
 *
 */
public class AllGetters {

    public static void main(String... args) throws Exception {
        try {
            // Fresh Start with the sample dataset
            FreshStartWithIJAndBIOPImsSample.main();

            //Get Extents of currently open dataset to create he same thing, but with two channels
            IDataContainerPrx new_group = EasyXT.createGroup( "Spots And Surface" );
            EasyXT.addToScene( new_group );

            // Make a spot detector and detect the spots
            ISpotsPrx spots = SpotsDetector.Channel( 2 )
                    .setName( "My Spots" )
                    .setDiameter( 3.0 )
                    .setRegionsThresholdManual( 100 )
                    .isSubtractBackground( true )
                    .isRegionsFromLocalContrast( true )
                    .isRegionsSpotsDiameterFromVolume( false )
                    .createRegionsChannel()
                    .build( ).detect( );

            EasyXT.addToScene( spots ); // EasyXT.addToScene( new_group, spots ); doesn't work TODO : see TODO below

            // Makes a surface detector and detect the surface
            ISurfacesPrx surface = SurfacesDetector.Channel(0)
                    .setSmoothingWidth(1)
                    .setLowerThreshold(40)
                    .setName("My Surface")
                    .setColor(new Integer[]{255,120,45})
                    .build()
                    .detect();

            EasyXT.addToScene( surface );

            //Highest level getters for spots and surfaces

            // Single spot
            ISpotsPrx spotByName = EasyXT.getSpots( "My Spots" ); // TODO : provide a way to access spots within a group
            ISpotsPrx spotByPosition = EasyXT.getSpots( 0 ); // 0 based // TODO : understand this NPE

            // All Spots in Scene
            List<ISpotsPrx> spotsList = EasyXT.getAllSpots( );

            // Single surface
            ISurfacesPrx surfaceByName = EasyXT.getSurfaces( "My Surface" );
            //ISurfacesPrx surfaceByPosition = EasyXT.getSurfaces( 0 ); // 0 based

            // All Surfaces in Scene
            List<ISurfacesPrx> surfacesList = EasyXT.getAllSurfaces( );

            // Generic Getter if you need other things
            IDataItemPrx rawFrame = EasyXT.getItem( "Frame 1" );

            //it returns the right type
            IJ.log( "Is "+EasyXT.getName(rawFrame)+" an IFramePrx?  - " + (rawFrame instanceof IFramePrx) );

            // And you can cast it directly if you feel like it
            IFramePrx frame = (IFramePrx) rawFrame;

            // Lowest level getter, uses an ItemQuery
            IDataContainerPrx parent = EasyXT.createGroup( "Some Parent" );

            ItemQuery query = new ItemQuery.ItemQueryBuilder( ).setName( "Spots 1" ).setParent( parent ).build( );
            List<IDataItemPrx> items = query.get();

            List<IDataItemPrx> allRawSpots = EasyXT.getAll( "Spots" );
            List<IDataItemPrx> allRawSurfaces = EasyXT.getAll( "Surfaces" );
            List<IDataItemPrx> allVolumes = EasyXT.getAll( "Volume" );

            IJ.log( "Spot: " + spotByName.GetName() );
        } catch ( Error error ) {
            System.out.println( "ERROR:" + error.mDescription );
            System.out.println( "LOCATION:" + error.mLocation );
            System.out.println( "String:" + error.toString() );
        }
    }
}
