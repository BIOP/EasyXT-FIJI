package ch.epfl.biop.imaris.demo;

import Imaris.Error;
import Imaris.ISurfacesPrx;
import ch.epfl.biop.imaris.EasyXT;
import ch.epfl.biop.imaris.StatsQuery;
import ij.measure.ResultsTable;
import net.imagej.ImageJ;

import java.util.Arrays;

public class GetStatisticsDemo {
    public static void main(String... args) throws Error {
        ImageJ ij = new ImageJ();
        ij.ui().showUI();

        // Get an object
        ISurfacesPrx surfaces = EasyXT.getSurfaces( "Surfaces 1" );

        // Get a specific statistic
        ResultsTable stats1 = EasyXT.getStatistics( surfaces, "Intensity Mean" );
        stats1.show("Mean Results");

        // Get a multiple statistics
        ResultsTable stats2 = EasyXT.getStatistics( surfaces, Arrays.asList( "Intensity Mean", "Intensity Sum" ));
        stats2.show("Intensity Results");

        // Get a multiple statistics for a single channel
        ResultsTable stats3 = EasyXT.getStatistics( surfaces, Arrays.asList( "Intensity Mean", "Intensity Sum" ), 1);
        stats3.show("Intensity Results C1");

        // Get a multiple statistics for multiple channels
        ResultsTable stats4 = EasyXT.getStatistics( surfaces, Arrays.asList( "Intensity Mean", "Intensity Sum" ), Arrays.asList( 1,2 ));
        stats3.show("Intensity Results 2 Channels");

        // Get statistics, the raw way
        ResultsTable stats5 = new StatsQuery( surfaces )
                .selectStatistics( Arrays.asList( "Area", "Sphericity" ) )
                .selectTime( 1 )
                .get();
        stats5.show( "Using StatsQuery" );




    }
}
