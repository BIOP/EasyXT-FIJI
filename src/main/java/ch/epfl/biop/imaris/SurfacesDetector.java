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
import Imaris.IDataSetPrx;
import Imaris.ISurfacesPrx;

/**
 * Helper functions for surface detection in Imaris
 * With the use of a builder pattern, this class combines the following Imaris functions :
 * - DetectSurfaces
 * - DetectSurfaceRegionGrowing TODO: implement and explain how the builder switch or not to this function
 * - DetectSurfacesRegionGrowingWithUpperThreshold TODO: implement and explain how the builder switch or not to this function
 * The Builder is tuned to allow for an invisible switch between these functions depending on the builder methods calls
 * If an upper threshold is set {
 * calling DetectSurfacesRegionGrowingWithUpperThreshold
 * } else if any of the seed detection parameters is set {
 * calling DetectSurfaceRegionGrowing
 * } else {
 * calling DetectSurfaces
 * }
 * @author Nicolas Chiaruttini
 * @author Olivier Burri
 * @version 1.0
 * BIOP, EPFL, Jan 2020
 * Useful links:
 * file:///C:/Program%20Files/Bitplane/Imaris%20x64%209.5.1/html/xtinterface/structImaris_1_1IImageProcessing.html
 * file:///C:/Program%20Files/Bitplane/Imaris%20x64%209.5.1/html/xtinterface/structImaris_1_1IImageProcessing.html#a41275043bc718252958bce85f4d4561a
 */
public class SurfacesDetector {

    // Fields from Imaris API - DetectSurfaces
    // Process entire image if aRegionsOfInterest is empty.
    // Example of aSurfaceFiltersString: '"Volume" above automatic threshold'
    // If aSmoothFilterWidth is equal to zero, smoothing is disabled.
    // If aLocalContrastFilterWidth is equal to zero, local contrast is disabled.
    // If aIntensityThresholdAutomatic is true, aIntensityThresholdManual is ignored.

    IDataSetPrx aDataSet;
    int[][] aRegionsOfInterest; // aRegionsOfInterest is a Nx8 matrix with the Rois : public [vMinX, vMinY, vMinZ, vMinT, vMaxX, vMaxY, vMaxZ, vMaxT]
    Integer aChannelIndex;
    Float aSmoothFilterWidth;
    Float aLocalContrastFilterWidth;

    Boolean aIntensityLowerThresholdAutomatic;  //Boolean aIntensityThresholdAutomatic;
    Float aIntensityLowerThresholdManual;     //Float aIntensityThresholdManual;

    String aSurfaceFiltersString;

    // Additional fields from Imaris API - DetectSurfacesRegionGrowing
    // Example of aSeedsFiltersString: '"Quality" above 7.000'

    Float aSeedsEstimateDiameter;
    Boolean aSeedsSubtractBackground;
    String aSeedsFiltersString;

    // Additional fields from Imaris API - DetectSurfacesRegionGrowingWithUpperThreshold()
    // Detect Surfaces with upper (or double) threshold and region growing.
    //  If aLowerThresholdEnabled is true, aIntensityLowerThresholdAutomatic and aIntensityLowerThresholdManual are ignored.
    //  If aIntensityLowerThresholdAutomatic is true, aIntensityLowerThresholdManual is ignored.
    //  If aUpperThresholdEnabled is true, aIntensityUpperThresholdAutomatic and aIntensityUpperThresholdManual are ignored.
    //  If aIntensityUpperThresholdAutomatic is true, aIntensityUpperThresholdManual is ignored.

    Boolean aLowerThresholdEnabled;
    //Boolean  	aIntensityLowerThresholdAutomatic;
    //Float  	aIntensityLowerThresholdManual;
    Boolean aUpperThresholdEnabled;
    Boolean aIntensityUpperThresholdAutomatic;
    Float aIntensityUpperThresholdManual;

    // Fields added to modify output

    String name;
    Integer[] color;

    public ISurfacesPrx detect() throws Imaris.Error {

        ISurfacesPrx surfaces;

        if ((aUpperThresholdEnabled != null) ||
                (aIntensityUpperThresholdAutomatic != null) ||
                (aIntensityUpperThresholdManual != null)) {
            // DetectSurfacesRegionGrowingWithUpperThreshold

            // ISurfaces* Imaris::IImageProcessing::DetectSurfacesRegionGrowingWithUpperThreshold 	( 	IDataSet *  	aDataSet,
            //		tInts2D  	aRegionsOfInterest,
            //		int  	aChannelIndex,
            //		float  	aSmoothFilterWidth,
            //		float  	aLocalContrastFilterWidth,
            //		bool  	aLowerThresholdEnabled,
            //		bool  	aIntensityLowerThresholdAutomatic,
            //		float  	aIntensityLowerThresholdManual,
            //		bool  	aUpperThresholdEnabled,
            //		bool  	aIntensityUpperThresholdAutomatic,
            //		float  	aIntensityUpperThresholdManual,
            //		float  	aSeedsEstimateDiameter,
            //		bool  	aSeedsSubtractBackground,
            //		string  	aSeedsFiltersString,
            //		string  	aSurfaceFiltersString
            //	)

            // Need to deal with a Lower Threshold Enabled TODO
            if (aSeedsEstimateDiameter == null) {

                surfaces = EasyXT.Utils.getImarisApp().GetImageProcessing().DetectSurfacesWithUpperThreshold(aDataSet,
                        aRegionsOfInterest,
                        aChannelIndex,
                        aSmoothFilterWidth,
                        aLocalContrastFilterWidth,
                        aLowerThresholdEnabled,
                        aIntensityLowerThresholdAutomatic,
                        aIntensityLowerThresholdManual,
                        aUpperThresholdEnabled,
                        aIntensityUpperThresholdAutomatic,
                        aIntensityUpperThresholdManual,
                        aSurfaceFiltersString);
            } else {

                surfaces = EasyXT.Utils.getImarisApp().GetImageProcessing().DetectSurfacesRegionGrowingWithUpperThreshold(aDataSet,
                        aRegionsOfInterest,
                        aChannelIndex,
                        aSmoothFilterWidth,
                        aLocalContrastFilterWidth,
                        aLowerThresholdEnabled,
                        aIntensityLowerThresholdAutomatic,
                        aIntensityLowerThresholdManual,
                        aUpperThresholdEnabled,
                        aIntensityUpperThresholdAutomatic,
                        aIntensityUpperThresholdManual,
                        aSeedsEstimateDiameter,
                        aSeedsSubtractBackground,
                        aSeedsFiltersString,
                        aSurfaceFiltersString);
            }
        } else {
            if ((aSeedsEstimateDiameter != null) || (aSeedsSubtractBackground != null) || (aSeedsFiltersString != null)) {
                // DetectSurfaceRegionGrowing
                // ISurfaces* Imaris::IImageProcessing::DetectSurfacesRegionGrowing 	( 	IDataSet *  	aDataSet,
                //		tInts2D  	aRegionsOfInterest,
                //		int  	aChannelIndex,
                //		float  	aSmoothFilterWidth,
                //		float  	aLocalContrastFilterWidth,
                //		bool  	aIntensityThresholdAutomatic,
                //		float  	aIntensityThresholdManual,
                //		float  	aSeedsEstimateDiameter,
                //		bool  	aSeedsSubtractBackground,
                //		string  	aSeedsFiltersString,
                //		string  	aSurfaceFiltersString
                //	)

                surfaces = EasyXT.Utils.getImarisApp().GetImageProcessing().DetectSurfacesRegionGrowing(aDataSet,
                        aRegionsOfInterest,
                        aChannelIndex,
                        aSmoothFilterWidth,
                        aLocalContrastFilterWidth,
                        aIntensityLowerThresholdAutomatic,
                        aIntensityLowerThresholdManual,
                        aSeedsEstimateDiameter,
                        aSeedsSubtractBackground,
                        aSeedsFiltersString,
                        aSurfaceFiltersString);

            } else {
                // DetectSurfaces

                //ISurfaces* Imaris::IImageProcessing::DetectSurfaces 	( 	IDataSet *  	aDataSet,
                //		tInts2D  	aRegionsOfInterest,
                //		int  	aChannelIndex,
                //		float  	aSmoothFilterWidth,
                //		float  	aLocalContrastFilterWidth,
                //		bool  	aIntensityThresholdAutomatic,
                //		float  	aIntensityThresholdManual,
                //		string  	aSurfaceFiltersString
                //	)

                surfaces = EasyXT.Utils.getImarisApp().GetImageProcessing().DetectSurfaces(aDataSet,
                        aRegionsOfInterest,
                        aChannelIndex,
                        aSmoothFilterWidth,
                        aLocalContrastFilterWidth,
                        aIntensityLowerThresholdAutomatic,
                        aIntensityLowerThresholdManual,
                        aSurfaceFiltersString);
            }
        }

        // EasyXT Specific

        if (name != null) {
            surfaces.SetName(name);
        } // or else it will have the default Imaris name


        if (color != null) {
            surfaces.SetColorRGBA(color[0] + (color[1] * 256) + (color[2] * 256 * 256));
        }

        return surfaces;
    }

    // Removes a bit of the verbosity
    public static SurfacesDetectorBuilder Channel(int indexChannel) throws Error {
        return SurfacesDetectorBuilder.aSurfacesDetector(indexChannel);
    }

    public static final class SurfacesDetectorBuilder {

        IDataSetPrx aDataSet;
        int[][] aRegionsOfInterest;
        Integer aChannelIndex;
        Float aSmoothFilterWidth = new Float(0); // Default with disables smoothing
        Float aLocalContrastFilterWidth = new Float(0); // aLocalContrastFilterWidth is equal to zero, local contrast is disabled.
        //Boolean aIntensityThresholdAutomatic = new Boolean(true);  // If aIntensityThresholdAutomatic is true, aIntensityThresholdManual is ignored.
        //Float aIntensityThresholdManual = new Float(0); // Disabled by default because aIntensityThresholdAutomatic is true by default
        String aSurfaceFiltersString;
        Float aSeedsEstimateDiameter;
        Boolean aSeedsSubtractBackground;
        String aSeedsFiltersString;
        Boolean aLowerThresholdEnabled;
        Boolean aIntensityLowerThresholdAutomatic = Boolean.TRUE;
        Float aIntensityLowerThresholdManual = new Float(0);
        Boolean aUpperThresholdEnabled;
        Boolean aIntensityUpperThresholdAutomatic;
        Float aIntensityUpperThresholdManual;

        Integer[] color;
        String name;

        private SurfacesDetectorBuilder(int channelIndex) throws Imaris.Error {
            // default values
            aDataSet = EasyXT.Utils.getImarisApp().GetDataSet();
            this.aChannelIndex = channelIndex;
        }

        public static SurfacesDetectorBuilder aSurfacesDetector(int channelIndex) throws Imaris.Error {
            return new SurfacesDetectorBuilder(channelIndex);
        }

        public SurfacesDetectorBuilder setDataSet(IDataSetPrx aDataSet) {
            this.aDataSet = aDataSet;
            return this;
        }

        public SurfacesDetectorBuilder setROI(int[][] aRegionsOfInterest) {
            this.aRegionsOfInterest = aRegionsOfInterest;
            return this;
        }

        public SurfacesDetectorBuilder setSmoothingWidth(double aSmoothFilterWidth) {
            this.aSmoothFilterWidth = new Float(aSmoothFilterWidth);
            return this;
        }

        public SurfacesDetectorBuilder setLocalContrastFilterWidth(double aLocalContrastFilterWidth) {
            this.aLocalContrastFilterWidth = new Float(aLocalContrastFilterWidth);
            return this;
        }

        public SurfacesDetectorBuilder setSurfaceFilter(String aSurfaceFiltersString) {
            this.aSurfaceFiltersString = aSurfaceFiltersString;
            return this;
        }

        public SurfacesDetectorBuilder setSeedsDiameter(double aSeedsEstimateDiameter) {
            this.aSeedsEstimateDiameter = new Float(aSeedsEstimateDiameter);
            return this;
        }

        public SurfacesDetectorBuilder isSeedsSubtractBackground(Boolean aSeedsSubtractBackground) {
            this.aSeedsSubtractBackground = aSeedsSubtractBackground;
            return this;
        }

        public SurfacesDetectorBuilder setSeedsFilter(String aSeedsFiltersString) {
            this.aSeedsFiltersString = aSeedsFiltersString;
            return this;
        }

        public SurfacesDetectorBuilder enableAutomaticLowerThreshold() {
            this.aIntensityLowerThresholdAutomatic = true;
            this.aLowerThresholdEnabled = true;
            return this;
        }

        /**
         * @param aIntensityLowerThresholdManual , corresponds to "Manual Threshold Value" in the "Creation Parameters"
         *                                       (in the "Creation" tab of a completed surface)
         * @return the builder for detecting surfaces
         */

        public SurfacesDetectorBuilder setLowerThreshold(double aIntensityLowerThresholdManual) {
            this.aIntensityLowerThresholdManual = new Float(aIntensityLowerThresholdManual);
            this.aLowerThresholdEnabled = true;
            this.aIntensityLowerThresholdAutomatic = false;
            return this;
        }

        public SurfacesDetectorBuilder enableAutomaticUpperThreshold() {
            this.aIntensityUpperThresholdAutomatic = true;
            this.aUpperThresholdEnabled = true;
            return this;
        }

        public SurfacesDetectorBuilder setUpperThreshold(double aIntensityUpperThresholdManual) {
            this.aIntensityUpperThresholdManual = new Float(aIntensityUpperThresholdManual);
            this.aUpperThresholdEnabled = true;
            this.aIntensityUpperThresholdAutomatic = false;
            return this;
        }

        public SurfacesDetectorBuilder setColor(Integer[] color) {
            this.color = color;
            return this;
        }

        public SurfacesDetectorBuilder setName(String name) {
            this.name = name;
            return this;
        }

        public SurfacesDetector build() {
            SurfacesDetector surfacesDetector = new SurfacesDetector();
            surfacesDetector.aIntensityLowerThresholdManual = this.aIntensityLowerThresholdManual;
            surfacesDetector.aUpperThresholdEnabled = this.aUpperThresholdEnabled;
            surfacesDetector.aDataSet = this.aDataSet;
            surfacesDetector.aLocalContrastFilterWidth = this.aLocalContrastFilterWidth;
            surfacesDetector.aRegionsOfInterest = this.aRegionsOfInterest;
            surfacesDetector.aLowerThresholdEnabled = this.aLowerThresholdEnabled;
            surfacesDetector.aIntensityUpperThresholdManual = this.aIntensityUpperThresholdManual;
            surfacesDetector.color = this.color;
            surfacesDetector.name = this.name;
            surfacesDetector.aSeedsEstimateDiameter = this.aSeedsEstimateDiameter;
            surfacesDetector.aSeedsFiltersString = this.aSeedsFiltersString;
            surfacesDetector.aSurfaceFiltersString = this.aSurfaceFiltersString;
            surfacesDetector.aIntensityLowerThresholdAutomatic = this.aIntensityLowerThresholdAutomatic;
            surfacesDetector.aSeedsSubtractBackground = this.aSeedsSubtractBackground;
            surfacesDetector.aSmoothFilterWidth = this.aSmoothFilterWidth;
            surfacesDetector.aIntensityUpperThresholdAutomatic = this.aIntensityUpperThresholdAutomatic;
            surfacesDetector.aChannelIndex = this.aChannelIndex;

            return surfacesDetector;
        }
    }
}
