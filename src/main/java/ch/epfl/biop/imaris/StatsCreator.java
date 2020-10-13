package ch.epfl.biop.imaris;

import Imaris.Error;
import Imaris.IDataItemPrx;
import Imaris.cStatisticValues;

import java.util.*;
import java.util.function.Consumer;

public class StatsCreator {

    private String channel = "";
    private String statName;
    private Map<Long, Map<String, Double>> statValues;
    private IDataItemPrx item;
    private Integer time;
    private List<String> units;
    private String category;

    private static Consumer<String> log = (str) -> System.out.println("StatsCreator : " + str);

    // Need IDs
    // Need stat name
    // Need channel (if applicable)
    // Need timepoint
    public StatsCreator(IDataItemPrx item, String statName, Map<Long, Map<String, Double>> values) {
        this.item = item;
        this.statName = statName;
        this.statValues = values;
        this.time = 1;
        this.units = new ArrayList<>(statValues.size());

        // Make timepoint 0 the default
        for (int t = 0; t < statValues.size(); t++) {
            units.add("");
        }
    }

    public StatsCreator setTimepoint(Integer timepoint) {
        this.time = timepoint;
        return this;
    }

    public StatsCreator setChannel(Integer channel) {
        if (channel != null) this.channel = channel.toString();
        return this;
    }

    public StatsCreator setCategory(String category) {
        this.category = category;
        return this;
    }

    public StatsCreator setUnit(String unit) {
        this.units.clear();

        for (int i = 0; i < statValues.size(); i++) {
            this.units.add(unit);
        }
        return this;
    }

    public void send() throws Error {
        // Sanity checks
        // 1. Check that the statValues are set
        if (statValues.size() == 0) {
            throw new Error("No statistics", "No statistics were set", "The statistic values list was empty when using 'StatsCreator.send()'");
        }
        if (item == null) {
            throw new Error("Null Imaris object", "Imaris Object was null", "The Imaris object provided to 'StatsCreator.send()' was null");
        }

        if (!statValues.get(statValues.keySet().iterator().next()).containsKey("Timepoint")) {
            log.accept("Your custom statistic has no timepoints, associating to timepoint " + this.time);
        }

        // Finally start doing some stuff
        // We need to use the following method
        //AddStatistics (String[] aNames, Float[] aValues, String[] aUnits, String[][] aFactors, String[] aFactorNames, Long[] aIds)
        // each element should have the same length
        //Build all the arrays we will need

        // Build the factorNames from the object's current statistics
        cStatisticValues rawStats = item.GetStatistics();
        String[] factorNames = rawStats.mFactorNames;

        int n = statValues.size();
        String[] finalStatNames = new String[n];
        String[] finalStatUnits = new String[n];
        long[] finalStatIds = new long[n];
        float[] finalStatValues = new float[n];

        // Initialize Factors
        String[][] finalStatFactors = new String[factorNames.length][n];

        for (int i = 0; i < factorNames.length; i++) {
            finalStatFactors[i] = new String[n];
            for (int j = 0; j < n; j++) {
                finalStatFactors[i][j] = "";
            }

        }

        int channelIdx = Arrays.asList(factorNames).indexOf("Channel");
        int timeIdx = Arrays.asList(factorNames).indexOf("Time");
        int catIdx = Arrays.asList(factorNames).indexOf("Category");

        // Iterate through each id
        int i = 0;
        for (long id : this.statValues.keySet()) {

            finalStatNames[i] = this.statName;
            finalStatValues[i] = this.statValues.get(id).get(statName).floatValue();
            finalStatIds[i] = id;
            finalStatUnits[i] = this.units.get(i);
            finalStatFactors[channelIdx][i] = this.channel;
            finalStatFactors[timeIdx][i] = this.statValues.get(id).containsKey("Timepoint") ? String.valueOf(Math.round(this.statValues.get(id).get("Timepoint"))) : time.toString();
            finalStatFactors[catIdx][i] = this.category;
            i++;


        }

        this.item.AddStatistics(finalStatNames, finalStatValues, finalStatUnits, finalStatFactors, factorNames, finalStatIds);

    }
}
