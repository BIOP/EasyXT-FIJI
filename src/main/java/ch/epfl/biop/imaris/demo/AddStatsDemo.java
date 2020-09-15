package ch.epfl.biop.imaris.demo;

import Imaris.Error;
import Imaris.ISpotsPrx;
import ch.epfl.biop.imaris.EasyXT;
import ch.epfl.biop.imaris.StatsCreator;
import ij.measure.ResultsTable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class AddStatsDemo {
    public static void main( String[] args ) throws Error {
        ISpotsPrx spots = EasyXT.getSpots( "Spots 1" );

        // Get the spot statistics first before adding a new one
        ResultsTable stats = EasyXT.getStatistics( spots, Arrays.asList( "Intensity Mean" ), Arrays.asList( 1, 2 ) );
        stats.show( "Some Stats" );
        // Compute the average intensity
        Map<Integer, Map<Long, Double>> spotMeans = new HashMap<>( );
        spotMeans.put( 1, new HashMap<>( ) );
        spotMeans.put( 2, new HashMap<>( ) );

        for ( int i = 0; i < stats.getCounter( ); i++ ) {
            int channel = (int) stats.getValue( "Channel", i );

            spotMeans.get( channel ).put( (long) stats.getValue( "ID", i ), stats.getValue( "Intensity Mean", i ) );

        }

        // Compute the mean for each ID
        Set<Long> ids = spotMeans.get( 1 ).keySet( );

        Map<Long, Double> means = new HashMap<>( ids.size( ) );
        for ( long id : ids ) {
            System.out.println( "ID:" + id + ", mean c1:" + spotMeans.get( 1 ).get( id ) );
            System.out.println( "ID:" + id + ", mean c2:" + spotMeans.get( 2 ).get( id ) );
            means.put( id, ( spotMeans.get( 1 ).get( id ) + spotMeans.get( 2 ).get( id ) ) / 2 );
        }

        // Finally, we should be able to add these as a new result
        new StatsCreator( spots, "Mean Test", means ).setCategory( "Spots" ).send( );
    }
}
