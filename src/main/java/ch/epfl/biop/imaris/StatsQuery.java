/*-
 * #%L
 * API and commands to facilitate communication between Imaris and FIJI
 * %%
 * Copyright (C) 2020 - 2023 ECOLE POLYTECHNIQUE FEDERALE DE LAUSANNE, Switzerland, BioImaging And Optics Platform (BIOP)
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */
/*
 * Copyright (c) 2021 Ecole Polytechnique Fédérale de Lausanne. All rights reserved.
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions
 * and the following disclaimer in the documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse
 * or promote products derived from this software without specific prior written permission.
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package ch.epfl.biop.imaris;

import Imaris.Error;
import Imaris.IDataItemPrx;
import Imaris.cStatisticValues;
import ij.measure.ResultsTable;

import java.io.File;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * This class is based on a Builder model to obtain selected statistics from an Imaris {@link cStatisticValues} object
 * It allows for selecting specific item IDs, stat names, channels and timepoints. Statistics are returned in the form
 * of a {@link ResultsTable} Multiple images are not directly supported, as we have never had a use for it.
 * See the example uses in the source code of {@link ch.epfl.biop.imaris.demo.GetStatisticsDemo}
 *
 * @author Olivier Burri
 * @version 1.0
 */
public class StatsQuery {
    private static final List<String> firstColumns = Arrays.asList("Label", "Name", "ID", "Timepoint", "Category");
    private static final Consumer<String> log = (str) -> System.out.println("StatsQuery : " + str);
    private final String itemName;
    private final cStatisticValues stats;
    private final List<Long> ids = new ArrayList<>();
    private final List<String> names = new ArrayList<>();
    private List<String> timepoints = new ArrayList<>();
    private List<String> channels = new ArrayList<>();
    private int nImages = 1;
    private ResultsTable results = new ResultsTable();


    /**
     * Constructor for getting selected statistics
     *
     * @param item the Imaris object from which we want statistics (Spots, Surfaces, ...)
     * @throws Error an Imaris Error Object
     */
    public StatsQuery(IDataItemPrx item) throws Error {

        // Heavy lifting here by Imaris to get all the statistics
        this.stats = item.GetStatistics();

        // Figure out some things: Will there be more than one image for the statistics?
        int imageFactor = Arrays.asList(this.stats.mFactorNames).indexOf("Image");

        // There should always be at least two values: "" and "Image 1"
        this.nImages = Arrays.stream(this.stats.mFactors[imageFactor]).distinct().collect(Collectors.toList()).size() - 1;

        if (this.nImages > 1 ) log.accept("More than one image: 'Image' will be appended to some column names ");

        this.itemName = item.GetName();
    }

    /**
     * Extracts the selected statistic as a LinkedHashMap where the keys are the ID of the object
     * We keep the IDs as Longs as that is a _bit_ more compatible with Imaris.
     *
     * @param results  the results table, usually an output from {@link EasyXT.Stats#export(IDataItemPrx)}
     * @param statName the name of the column, like "Circularity" or "Intensity Mean C1"
     * @return a map with eack keyset being the ID of the spot and the value of the statistic
     */
    public static Map<Long, Map<String, Double>> extractStatistic(ResultsTable results, String statName) {
        if (results.columnExists(statName)) {
            Map<Long, Map<String, Double>> stat = new LinkedHashMap<>(results.size());
            for (int i = 0; i < results.size(); i++) {
                // Put the default columns
                Map<String, Double> values = new LinkedHashMap<String, Double>();
                for (String col : firstColumns) {
                    values.put(col, results.getValue(col, i));
                }
                // Put the result we want, finally
                values.put(statName, results.getValue(statName, i));
                stat.put((long) results.getValue("ID", i), values);
            }
            return stat;
        }
        // The selected statistic does not exist
        log.accept("The statistic: '" + statName + "' does not exist in the provided Results Table");

        return null;
    }

    /**
     * Allows for the selection of a specific ID. Note that IDs are not necesarily continuous nor necesarily start at 0.
     * These are the IDs as per Imaris's ID value in the GUI
     *
     * @param id the ID to recover statistics from
     * @return the same StatsQuery object to continue configuration
     */
    public StatsQuery selectId(final Integer id) {
        this.ids.add(id.longValue());
        return this;
    }

    /**
     * Will force StatsQuery to use the given ResultsTable and append to it
     *
     * @param rt the results table to which results will be appended
     * @return the same StatsQuery object to continue configuration
     */
    public StatsQuery resultsTable(ResultsTable rt) {
        this.results = rt;
        return this;
    }

    /**
     * Allows to set a list of IDs from which to get statistics from
     *
     * @param ids the list of spot or surface IDs to recover statistics from
     * @return the same StatsQuery object to continue configuration
     */
    public StatsQuery selectIds(final List<Integer> ids) {
        this.ids.addAll(ids.stream().map(id -> id.longValue()).collect(Collectors.toList()));
        return this;
    }

    /**
     * Allows to select the name of the statistic to export. These are the same names as in the Imaris GUI **Minus** the
     * channel or image (eg. do not enter "Intensity Sum" Ch1=1 Img=1, just "Intensity Sum") Use {@link
     * StatsQuery#selectChannels(List)} and {@link StatsQuery#selectChannel(Integer)} to specify channels
     *
     * @param name the name of the statistic to recover as it appears in the Imaris GUI.
     * @return the same StatsQuery object to continue configuration
     */
    public StatsQuery selectStatistic(final String name) {
        this.names.add(name);
        return this;
    }

    /**
     * Allows to set a list of IDs from which to get statistics from
     *
     * @param names the list of statistic names to recover as they appear in the Imaris GUI
     * @return the same StatsQuery object to continue configuration
     */
    public StatsQuery selectStatistics(final List<String> names) {
        this.names.addAll(names);
        return this;
    }

    /**
     * Allows to select the timepoint of the statistics to export. 0-based
     * Careful. Imaris results are one-based for timepoints
     *
     * @param timepoint the timpoint to get the stats from. One-based
     * @return the same StatsQuery object to continue configuration
     */
    public StatsQuery selectTime(final Integer timepoint) {
        this.timepoints.add(timepoint.toString());
        return this;
    }

    /**
     * Allows to set a list of timepoints from which to get statistics from
     * Careful. Imaris results are one-based for timepoints
     *
     * @param timepoints the timpoints to get the stats from. One-based
     * @return the same StatsQuery object to continue configuration
     */
    public StatsQuery selectTimes(final List<Integer> timepoints) {
        this.timepoints = timepoints.stream().map(t -> t.toString()).collect(Collectors.toList());
        return this;
    }

    /**
     * Allows to select the channel from which to get statistics from
     * Careful. Imaris results are one-based for channels
     *
     * @param channel the channel to get the stats from. One-based
     * @return the same StatsQuery object to continue configuration
     */
    public StatsQuery selectChannel(Integer channel) {
        if (channel > 0) this.channels.add(channel.toString());
        return this;
    }

    /**
     * Allows to set a list of channels from which to get statistics from
     * Careful. Imaris results are one-based for channels
     *
     * @param channels a list of channels to get the stats from (one based)
     * @return the same StatsQuery object to continue configuration
     */
    public StatsQuery selectChannels(final List<Integer> channels) {
        this.channels = channels.stream().map(c -> c.toString()).collect(Collectors.toList());
        return this;
    }

    /**
     * Allows appending results from a previous run
     *
     * @param results a results table from ImageJ or from a finished StatsQuery
     * @return the same StatsQuery object to continue configuration
     */
    public StatsQuery appendTo(ResultsTable results) {
        for (int i = 0; i < results.size(); i++) {
            this.results.incrementCounter();
            for (String c : results.getHeadings()) {
                String stringVal = results.getStringValue(c, i);
                if (!isDoubleValue(stringVal)) {
                    this.results.addValue(c, stringVal);
                } else {
                    this.results.addValue(c, results.getValue(c, i));
                }
            }
        }
        return this;
    }

    /**
     * To get String from ResultsTable , necessary for appendTo()
     *
     * @param stringTotTest , try to covnert it as a Double,
     * @return true if can be parse into a Double, false if can't
     */
    private Boolean isDoubleValue(String stringTotTest) {
        try {
            Double dummy = Double.parseDouble(stringTotTest);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Heavy lifting function that performs the requested operation and returns a table.
     * It is rather naive. It will go through each row of the raw Imaris statistics and see if that row matches
     * the names, channels and timepoints that were requested. If they all match, then we add them as a Map
     * We return a sorted results table by ID (Rows) and Column names, minus selected columns
     * NOTE: We ignore statistics without IDs (average values in Imaris) as we assume we can get them outside of Imaris
     *
     * @return the resultsTable with all requested statistics
     * @throws Error an Imaris Error
     */
    public ResultsTable get() throws Error {
        // identify what we need
        List<Integer> selectedIndexes = new ArrayList<>();
        // Stats are all about having a unique ID per row.
        // This means that channels should be appended to the ID
        // IDs change per timepoint, so no need for that
        Map<Long, Map<String, String>> statsById = new HashMap<>();

        // Name of object we are getting the statistics from
        String imageName = new File(EasyXT.Files.getOpenFileName()).getName();
        for (int i = 0; i < this.stats.mIds.length; i++) {

            boolean matchesName, matchesChannel, matchesTime, matchesID;

            if (this.names.size() > 0) { // We have requested specific statistic names
                matchesName = false;
                for (String name : this.names) {
                    matchesName = this.stats.mNames[i].matches(name);
                    if (matchesName) break;
                }
            } else matchesName = true; // No specific names selected, make true for all

            if (this.channels.size() > 0) { // We have requested specific channels
                matchesChannel = false;
                Integer channelIdx = Arrays.asList(this.stats.mFactorNames).indexOf("Channel");

                for (String channel : this.channels) {
                    // Special case, if the name matches but there is no channel information, provide the result
                    // nonetheless. Eg. Requesting "Volume" should return volume, independently of channel
                    matchesChannel = this.stats.mFactors[channelIdx][i].matches(channel) ||
                                    (this.stats.mFactors[channelIdx][i].matches("") && matchesName);
                    if (matchesChannel) break;
                }
            } else matchesChannel = true;

            if (this.timepoints.size() > 0) { // We have requested specific timepoints
                Integer timeIdx = Arrays.asList(this.stats.mFactorNames).indexOf("Time");

                matchesTime = false;
                for (String time : this.timepoints) {
                    matchesTime = this.stats.mFactors[timeIdx][i].matches(time);
                    if (matchesTime) break;
                }
            } else matchesTime = true;

            if (this.ids.size() > 0) { // We have requested specific object IDs
                matchesID = false;
                for (Long id : this.ids) {
                    matchesID = this.stats.mIds[i] == id;
                    if (matchesID) break;
                }
            } else matchesID = true;

            // If we get a true for all these, then we can keep the statistic
            // Any "global" statistic has an ID of -1. We ignore these in favor of computing these outside Imaris
            if (matchesName && matchesChannel && matchesID && matchesTime && this.stats.mIds[i] != -1) {
                String name = stats.mNames[i];

                Float value = stats.mValues[i];
                long id = this.stats.mIds[i];

                // If it exists, use it and append more stats
                Map<String, String> statElements = (statsById.containsKey(id)) ? statsById.get(id) : new HashMap<>();

                // List all stats we want to add
                statElements.put("Label", imageName);
                statElements.put("ID", String.valueOf(id));
                statElements.put("Name", this.itemName);

                // Build the name of this statistic based on the factors that are available
                for (int factorIdx = 0; factorIdx < this.stats.mFactorNames.length; factorIdx++) {
                    String factorName = this.stats.mFactorNames[factorIdx];
                    String factorValue = this.stats.mFactors[factorIdx][i];

                    if (!factorValue.equals("")) {

                        switch (factorName) {
                            case "Time":
                                // Goes into Timepoint column
                                statElements.put("Timepoint", factorValue);
                                break;
                            case "Category":
                                statElements.put("Category", factorValue);
                                break;
                            case "Collection":
                            case "Time Index":
                                // Do nothing
                                break;
                            case "Image":
                                if (this.nImages > 1) name += " : " + factorValue;
                                break;
                            case "Channel":
                                name += " C" + factorValue;
                                break;
                            default:
                                name += " : " + factorName +" : " + factorValue;
                                break;
                        }
                    }
                }
                statElements.put(name, String.valueOf(value));

                // TODO : Check if this can be rewritten in a neater way as it is not necessary to 'put' again if it is already in statsByID
                // TODO: But because it checks if the ID is unique, the overhead is not much. Still ugly though.
                statsById.put(id, statElements);
            }
        }

        // Sort the Ids to have them in order
        Map<Long, Map<String, String>> statsByIdSorted = new ImarisResultComparator().sort(statsById);

        // Add all the results to the results table
        statsByIdSorted.forEach((uid, columns) -> {

            results.incrementCounter();

            // We want some order in the columns. Label, Name, ID, Timepoint, Category
            firstColumns.forEach(name -> {
                if (isNumber(columns.get(name))) {
                    results.addValue(name, Double.valueOf(columns.get(name)));
                } else {
                    results.addValue(name, columns.get(name));
                }
                columns.remove(name);
            });

            // Add all the columns
            columns.forEach((name, value) -> {
                if (isNumber(value))
                    results.addValue(name, Double.valueOf(value));
                else
                    results.addValue(name, value);
            });
        });
        return results;
    }

    /**
     * Convenience to check if we can parse the number or not
     *
     * @param test the string to test
     * @return
     */
    private boolean isNumber(String test) {
        try {
            Double.valueOf(test);
            return true;
        } catch (NumberFormatException | NullPointerException ne) {
            return false;
        }
    }

    /**
     * Comparators to help sort the results. The first one compares by ID, the second one by column name
     */
    class ImarisResultComparator implements Comparator<Map.Entry<Long, Map<String, String>>> {

        @Override
        public int compare(Map.Entry<Long, Map<String, String>> o1, Map.Entry<Long, Map<String, String>> o2) {
            // Compare the keys
            return o1.getKey().compareTo(o2.getKey());
        }

        private Map<Long, Map<String, String>> sort(Map<Long, Map<String, String>> idMap) {
            // First create the List
            ArrayList<Map.Entry<Long, Map<String, String>>> idList = new ArrayList<Map.Entry<Long, Map<String, String>>>(idMap.entrySet());

            // Sort the list
            Collections.sort(idList, this);

            // Copying entries from List to Map
            Map<Long, Map<String, String>> sortedMap = new LinkedHashMap<>();
            for (Map.Entry<Long, Map<String, String>> entry : idList) {
                Map<String, String> columns = new ImarisColumnComparator().sort(entry.getValue());
                sortedMap.put(entry.getKey(), columns);
            }
            // Finally return map
            return sortedMap;
        }
    }

    class ImarisColumnComparator implements Comparator<Map.Entry<String, String>> {

        @Override
        public int compare(Map.Entry<String, String> o1, Map.Entry<String, String> o2) {
            // Compare the keys
            return o1.getKey().compareTo(o2.getKey());
        }

        public Map<String, String> sort(Map<String, String> columnMap) {
            // First create the List
            ArrayList<Map.Entry<String, String>> columnList = new ArrayList<Map.Entry<String, String>>(columnMap.entrySet());

            // Sort the list
            Collections.sort(columnList, this);

            // Copying entries from List to Map
            Map<String, String> sortedColumn = new LinkedHashMap<>();
            for (Map.Entry<String, String> entry : columnList) {
                sortedColumn.put(entry.getKey(), entry.getValue());
            }
            // Finally return map
            return sortedColumn;
        }
    }
}
