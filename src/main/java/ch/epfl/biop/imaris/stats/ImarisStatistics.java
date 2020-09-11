package ch.epfl.biop.imaris.stats;

import Imaris.Error;
import Imaris.IDataItemPrx;
import Imaris.cStatisticValues;
import ch.epfl.biop.imaris.EasyXT;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

public class ImarisStatistics extends cStatisticValues {

    public List<Integer> getStatisticIndexes( String name ) {
        List<Integer> selected = new ArrayList<>( );

        // Find all statistics with the right name
        IntStream.range( 0, mNames.length ).forEachOrdered( i -> {
            if ( mNames[ i ].matches( name ) ) selected.add( i );
        } );
        return selected;
    }


    class StatsBuilder {
        private String[] statNames;
        private Integer[] selectedChannels;

        public StatsBuilder getNames( String... names ) {
            this.statNames = names;
            return this;
        }

        public StatsBuilder getChannels( Integer... channels ) {
            this.selectedChannels = channels;
            return this;
        }
    }
}
