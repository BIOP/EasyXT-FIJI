package ch.epfl.biop.imaris.demo;

import Imaris.ISpotsPrx;
import ch.epfl.biop.imaris.EasyXT;
import ij.measure.ResultsTable;

import java.util.Arrays;
import java.util.Map;


/**
 * EasyXT Demo
 * Show how to add statistics from ImageJ/Fiji to Imaris
 *
 * @author Olivier Burri
 * @author Nicolas Chiaruttini
 * October 2020
 * EPFL - SV - PTECH - PTBIOP
 */

public class AddStatsDemo {
    public static void main(String... args) throws Exception {

        // Fresh Start with the sample dataset
        FreshStartWithIJAndBIOPImsSample.main();

        // Makes a spots detector and detect the spots
        ISpotsPrx spots = EasyXT.Spots.create(2)
                .isSubtractBackground(true)
                .setDiameter(1.0)
                .setName("My Spots")
                .setColor(new Integer[]{255, 120, 45})
                .setFilter("\"Quality\" above automatic threshold")
                .build().detect();

        EasyXT.Scene.addItem(spots);

        // Get the spot statistics first before adding a new one
        ResultsTable stats = EasyXT.Stats.export(spots, Arrays.asList("Intensity Mean"), Arrays.asList(1, 2));

        // Compute the mean of the two channels in ImageJ from the results table
        for (int i = 0; i < stats.size(); i++) {
            double mean = stats.getValue("Intensity Mean C1", i) + stats.getValue("Intensity Mean C2", i);
            mean /= 2;
            //Add as a result
            stats.setValue("C1-C2 Mean", i, mean);
        }

        // Update the stats in Fiji
        stats.show("The Statistics Stats");

        // Export the new statistic into a format that we can insert into Imaris
        Map<Long, Map<String, Double>> means = EasyXT.Stats.extract(stats, "C1-C2 Mean");

        // Finally, we should be able to add these as a new result
        EasyXT.Stats.create(spots, "C1-C2 Mean", means)
                .setCategory("EasyXT")
                .send();
    }
}
