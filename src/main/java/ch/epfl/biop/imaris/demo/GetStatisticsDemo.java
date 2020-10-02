package ch.epfl.biop.imaris.demo;

import Imaris.Error;
import Imaris.ISurfacesPrx;
import ch.epfl.biop.imaris.EasyXT;
import ch.epfl.biop.imaris.StatsQuery;
import ch.epfl.biop.imaris.SurfacesDetector;
import ij.measure.ResultsTable;

import java.util.Arrays;

public class GetStatisticsDemo {
    public static void main(String... args) throws Error {

        // Fresh Start with the sample dataset
        FreshStartWithIJAndBIOPImsSample.main();

        // Makes a surface detector and detect the surface
        ISurfacesPrx surface = SurfacesDetector.Channel(0)
                .setSmoothingWidth(5)
                .setLowerThreshold(300)
                .setName("Surfaces 1")
                .setColor(new Integer[]{255,120,45})
                .build()
                .detect();


        // Get an object
        // ISurfacesPrx surfaces = EasyXT.getSurfaces( "Surfaces 1" ); // For this to work, you need to add the surface as a child object, see {@link AddChildObjects}

        // Get a specific statistic
        ResultsTable stats1 = EasyXT.getStatistics( surface, "Intensity Mean" );
        stats1.show("Mean Results");

        // Get a multiple statistics
        ResultsTable stats2 = EasyXT.getStatistics( surface, Arrays.asList( "Intensity Mean", "Intensity Sum" ));
        stats2.show("Intensity Results");

        // Get a multiple statistics for a single channel
        ResultsTable stats3 = EasyXT.getStatistics( surface, Arrays.asList( "Intensity Mean", "Intensity Sum" ), 1);
        stats3.show("Intensity Results C1");

        // Get a multiple statistics for multiple channels
        ResultsTable stats4 = EasyXT.getStatistics( surface, Arrays.asList( "Intensity Mean", "Intensity Sum" ), Arrays.asList( 1,2 ));
        stats3.show("Intensity Results 2 Channels");

        // Get statistics, the raw way
        ResultsTable stats5 = new StatsQuery( surface )
                .selectStatistics( Arrays.asList( "Area", "Sphericity" ) )
                .selectTime( 1 )
                .get();
        stats5.show( "Using StatsQuery" );




    }
}
