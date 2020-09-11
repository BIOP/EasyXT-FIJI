package ch.epfl.biop.imaris;

import Imaris.Error;
import Imaris.IDataItemPrx;
import Imaris.cStatisticValues;
import ij.measure.ResultsTable;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class is based on a Builder model to obtain selected statistics from an Imaris {@link cStatisticValues} object
 * It allows for selecting specific item IDs, stat names, channels and timepoints. Statistics are returned in the form
 * of a {@link ResultsTable} Multiple images are not directly supported, as we have never had a use for it.
 *
 * @author Olivier Burri
 * @version 0.1
 */
public class StatsQuery {
    private List<Long> ids = new ArrayList<>( );
    private List<String> names = new ArrayList<>( );
    private List<String> timepoints = new ArrayList<>( );
    private List<String> channels = new ArrayList<>( );

    private ResultsTable results = new ResultsTable( );
    private cStatisticValues stats;
    private int channelIdx, timeIdx, catIdx;

    /**
     * Constructor for getting selected statistics
     *
     * @param item the Imaris object from which we want statistics (Spots, Surfaces, ...)
     * @throws Error an Imaris Error Object
     */
    public StatsQuery( IDataItemPrx item ) throws Error {

        // Heavy lifting here by Imaris to get all the statistics
        this.stats = item.GetStatistics( );

        // Identify the position of factors we want to use
        this.channelIdx = Arrays.asList( this.stats.mFactorNames ).indexOf( "Channel" );
        this.timeIdx = Arrays.asList( this.stats.mFactorNames ).indexOf( "Time" );
        this.catIdx = Arrays.asList( this.stats.mFactorNames ).indexOf( "Category" );
    }

    /**
     * Allows for the selection of a specific ID. Note that IDs are not necesarily continuous nor necesarily start at 0.
     * These are the IDs as per Imaris's ID value in the GUI
     *
     * @param id the ID to recover statistics from
     * @return
     */
    public StatsQuery selectId( final Integer id ) {
        this.ids.add( id.longValue( ) );
        return this;
    }

    /**
     * allows to set a list of IDs from which to get statistics from
     *
     * @param ids the list of IDs to recover statistics from
     * @return
     */
    public StatsQuery selectIds( final List<Integer> ids ) {
        this.ids.addAll( ids.stream( ).map( id -> id.longValue( ) ).collect( Collectors.toList( ) ) );
        return this;
    }

    /**
     * allows to select the name of the statistic to export. These are the same names as in the Imaris GUI **Minus** the
     * channel or image (eg. do not enter "Intensity Sum" Ch1=1 Img=1, just "Intensity Sum") Use {@link
     * StatsQuery#selectChannels(List)} and {@link StatsQuery#selectChannel(Integer)} to specify channels
     *
     * @param name the name of the statistic to recover as it appears in the Imaris GUI.
     * @return
     */
    public StatsQuery selectStatistic( final String name ) {
        this.names.add( name );
        return this;
    }

    /**
     * allows to set a list of IDs from which to get statistics from
     *
     * @param names the list of statistic names to recover as they appear in the Imaris GUI
     * @return
     */
    public StatsQuery selectStatistics( final List<String> names ) {
        this.names.addAll( names );
        return this;
    }

    /**
     * allows to select the timepoint of the statistics to export. 0-based
     *
     * @param timepoint the timepoint
     * @return
     */
    public StatsQuery selectTime( final Integer timepoint ) {
        this.timepoints.add( timepoint.toString( ) );
        return this;
    }

    /**
     * allows to set a list of timepoints from which to get statistics from
     *
     * @param timepoints
     * @return
     */
    public StatsQuery selectTimes( final List<Integer> timepoints ) {
        this.timepoints = timepoints.stream( ).map( t -> t.toString( ) ).collect( Collectors.toList( ) );
        return this;
    }

    /**
     * allows to select the channel from which to get statistics from
     *
     * @param channel
     * @return
     */
    public StatsQuery selectChannel( final Integer channel ) {
        this.channels.add( channel.toString( ) );
        return this;
    }

    /**
     * allows to set a list of channels from which to get statistics from
     *
     * @param channels
     * @return
     */
    public StatsQuery selectChannels( final List<Integer> channels ) {
        // We choose channels to be 0-based, but the stats are 1-based
        this.channels = channels.stream( ).map( c -> {
            c = c + 1;
            return c.toString( );
        } ).collect( Collectors.toList( ) );
        return this;
    }

    /**
     * Allows appending results from a previous run
     *
     * @param results a results table from ImageJ or from a finished StatsQuery
     * @return
     */
    public StatsQuery appendTo( ResultsTable results ) {
        for ( int i = 0; i < results.size( ); i++ ) {
            this.results.incrementCounter( );
            for ( String c : results.getHeadings( ) ) {
                this.results.addValue( c, results.getValue( c, i ) );
                // TODO allow for String results
            }
        }
        return this;
    }

    /**
     * Heavy lifting function that performs the requested operation and returns a table It is rather naive. It will go
     * through each row of the raw Imaris statistics and see if that row matches the names, channels and timepoints that
     * were requested. If they all match
     *
     * @return
     * @throws Error
     */
    public ResultsTable get( ) throws Error {
        // identify what we need
        for ( int i = 0; i < this.stats.mIds.length; i++ ) {

            boolean matchesName, matchesChannel, matchesTime, matchesID;

            if ( this.names.size( ) > 0 ) { // We have requested specific statistic names
                matchesName = false;
                for ( String name : this.names ) {
                    matchesName = this.stats.mNames[ i ].matches( name );
                    if ( matchesName ) break;
                }
            } else matchesName = true; // No specific names selected, make true for all

            if ( this.channels.size( ) > 0 ) { // We have requested specific channels
                matchesChannel = false;
                for ( String channel : this.channels ) {
                    matchesChannel = this.stats.mFactors[ channelIdx ][ i ].matches( channel );
                    if ( matchesChannel ) break;
                }
            } else matchesChannel = true;

            if ( this.timepoints.size( ) > 0 ) { // We have requested specific channels
                matchesTime = false;
                for ( String time : this.timepoints ) {
                    matchesTime = this.stats.mFactors[ timeIdx ][ i ].matches( time );
                    if ( matchesTime ) break;
                }
            } else matchesTime = true;

            if ( this.ids.size( ) > 0 ) { // We have requested specific object IDs
                matchesID = false;
                for ( Long id : this.ids ) {
                    matchesID = this.stats.mIds[ i ] == id;
                    if ( matchesID ) break;
                }
            } else matchesID = true;

            // If we get a true for all these, then we can keep the statistic
            if ( matchesName && matchesChannel && matchesID && matchesTime ) {
                String columnName = stats.mNames[ i ];
                Float value = stats.mValues[ i ];
                String cat = this.stats.mFactors[ catIdx ][ i ];
                String channel = this.stats.mFactors[ channelIdx ][ i ];
                String time = this.stats.mFactors[ timeIdx ][ i ];
                long id = this.stats.mIds[ i ];

                // Make a new results row
                results.incrementCounter( );

                results.addLabel( new File( EasyXT.getOpenImageName( ) ).getName( ) );
                results.addValue( "ID", id );
                results.addValue( columnName, value );
                if ( !cat.equals( "" ) ) results.addValue( "Category", cat );
                if ( !channel.equals( "" ) ) results.addValue( "Channel", channel );
                if ( !time.equals( "" ) ) results.addValue( "Timepoint", time );
            }
        }
        return this.results;
    }


}
