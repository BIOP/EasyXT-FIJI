package ch.epfl.biop.imaris.demo;

import Imaris.Error;
import Imaris.ISpotsPrx;
import Imaris.ISurfacesPrx;
import ch.epfl.biop.imaris.*;
import ij.macro.Variable;
import ij.measure.ResultsTable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

public class AddStatsDemo {
    public static void main(String[] args) throws Error {

        // Fresh Start with the sample dataset
        FreshStartWithIJAndBIOPImsSample.main();

        // Makes a surface detector and detect the surface
        ISpotsPrx spots = SpotsDetector.Channel(2)
                .isSubtractBackground(true)
                .setDiameter(1.0)
                .setName("My Spots")
                .setColor(new Integer[]{255, 120, 45})
                .setFilter("\"Quality\" above automatic threshold")
                .build()
                .detect();

        EasyXT.addToScene(spots);
        //spots = EasyXT.getSpots( "My Spots" );

        // Get the spot statistics first before adding a new one
        ResultsTable stats = EasyXT.getStatistics(spots, Arrays.asList("Intensity Mean"), Arrays.asList(1, 2));

        // Compute the mean of the two channels in ImageJ from the results table
        for (int i = 0; i < stats.size(); i++) {
            double mean = stats.getValue("Intensity Mean C1", i) + stats.getValue("Intensity Mean C2", i);
            mean /= 2;
            //Add as a result
            stats.setValue("C1-C2 Mean", i, mean);
        }

        // Update the stats in Fiji
        stats.show("Some Stats");

        // Export the new statistic into a format that we can insert into Imaris
        Map<Long, Map<String, Double>> means = StatsQuery.extractStatistic(stats, "C1-C2 Mean");

        // Finally, we should be able to add these as a new result
        new StatsCreator(spots, "C1-C2 Mean", means)
                .setCategory("EasyXT")
                .send();
    }
}
