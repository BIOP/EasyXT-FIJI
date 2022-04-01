/*-
 * #%L
 * API and commands to facilitate communication between Imaris and FIJI
 * %%
 * Copyright (C) 2020 - 2022 ECOLE POLYTECHNIQUE FEDERALE DE LAUSANNE, Switzerland, BioImaging And Optics Platform (BIOP)
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * class to manage creating statistics for insertion into Imaris
 * @author Olivier Burri
 * January 2021
 */
public class StatsCreator {

    private static Consumer<String> log = (str) -> System.out.println("StatsCreator : " + str);
    private String channel = "";
    private String statName;
    private Map<Long, Map<String, Double>> statValues;
    private IDataItemPrx item;
    private Integer time;
    private List<String> units;
    private String category;

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
