package ch.epfl.biop.imaris.demo;

import Imaris.*;
import Imaris.Error;
import ch.epfl.biop.imaris.EasyXT;
import ch.epfl.biop.imaris.ItemQuery;
import ij.IJ;

import java.util.List;

public class AllGetters {

    public static void main(String... args) throws Error {

        // Fresh Start with the sample dataset
        FreshStartWithIJAndBIOPImsSample.main();

        //Highest level getters for spots and surfaces

        // Single spot
        ISpotsPrx spotByName = EasyXT.getSpots( "Spots 1" );
        ISpotsPrx spotByPosition = EasyXT.getSpots( 1 ); // 0 based

        // All Spots in Scene
        List<ISpotsPrx> spotsList = EasyXT.getAllSpots( );

        // Single surface
        ISurfacesPrx surfaceByName = EasyXT.getSurfaces( "Surfaces 1" );
        ISurfacesPrx surfaceByPosition = EasyXT.getSurfaces( 1 ); // 0 based

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

    }
}
