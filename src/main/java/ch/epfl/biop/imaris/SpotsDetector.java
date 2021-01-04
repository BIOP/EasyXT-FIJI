/**
 * Copyright (c) 2020 Ecole Polytechnique Fédérale de Lausanne. All rights reserved.
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
import Imaris.ISpotsPrx;

/**
 * Helper functions for spots detection in Imaris
 *
 * With the use of a builder pattern, this class combines the following Imaris functions :
 * - DetectSpots2()
 * - DetectSpotsRegionGrowing() TODO: implement and explain how the builder switch or not to this function
 *
 * DetectSpotsWithRegions() not supported -> replaced by DetectSpotsRegionGrowing() T
 * DetectSpots() obsolete -> replaced by DetectSpots2()
 *
 * The Builder is tuned to allow for an invisible switch between these functions depending on the builder methods calls
 *
 * TODO : look at Elliptical spot detection
 *
 * Authors
 * Nicolas Chiaruttini, nicolas.chiaruttini@epfl.ch
 * Olivier Burri, olivier.burri@epfl.ch
 * Romain Guiet, romain.guiet@epfl.ch
 *
 * BIOP, EPFL,  Jan 2021
 *
 * Useful links:
 * file:///C:/Program%20Files/Bitplane/Imaris%20x64%209.5.1/html/xtinterface/structImaris_1_1IImageProcessing.html
 * file:///C:/Program%20Files/Bitplane/Imaris%20x64%209.5.1/html/xtinterface/structImaris_1_1IImageProcessing.html#ae10348d92f0d2df6848ed44253c69391
 */

public class SpotsDetector {

    // Fields from Imaris API - DetectSpots2D (Replaces DetectSpots, implemented for bpPointsViewer.)
    // Process entire image if aRegionsOfInterest is empty.
    // Example of aSpotFiltersString: '"Position X" above 30.000 um "Intensity Center Ch=1" above automatic threshold'

    IDataSetPrx aDataSet;
    int[][] aRegionsOfInterest; // aRegionsOfInterest is a Nx8 matrix with the Rois : public [vMinX, vMinY, vMinZ, vMinT, vMaxX, vMaxY, vMaxZ, vMaxT]
    Integer aChannelIndex;
    Float aEstimateDiameter;
    Boolean aSubtractBackground;
    String aSpotFiltersString;

    // Additional fields from Imaris API - DetectSpotsRegionGrowing
    // If aRegionsFromLocalContrast is false, regions are computed from channel intensity.
    // If aRegionsThresholdAutomatic is true, aRegionsThresholdManual is ignored.
    // If aRegionsSpotsDiameterFromVolume is false, spots diameter is equal to region border distance.

    Boolean  	aRegionsFromLocalContrast;
    Boolean  	aRegionsThresholdAutomatic;
    Float  	    aRegionsThresholdManual;
    Boolean  	aRegionsSpotsDiameterFromVolume;
    Boolean  	aRegionsCreateChannel;

    // Fields added to detect ellipses

    float[] aEstimateDiameterXYZ;


    // Fields added to modify output

    String name;
    Integer[] color;

    public ISpotsPrx detect() throws Error {

        ISpotsPrx spots;

        // If one of the RegionGrowing parameters is set
        if ((aRegionsFromLocalContrast!=null)||
            (aRegionsThresholdAutomatic!=null)||
            (aRegionsThresholdManual!=null)||
            (aRegionsSpotsDiameterFromVolume!=null)||
            (aRegionsCreateChannel!=null)) {

            // they should be all set !
            if ( (aRegionsFromLocalContrast==null)||
                    (aRegionsThresholdAutomatic==null)||
                    (aRegionsThresholdManual==null)||
                    (aRegionsSpotsDiameterFromVolume==null)||
                    (aRegionsCreateChannel==null)) {

                System.err.println( "Please specify all parameters  ");
            }

                // DetectSpotsRegionGrowing

                // ISpots* Imaris::IImageProcessing::DetectSpotsRegionGrowing 	( 	IDataSet *  	aDataSet,
                //		tInts2D  	aRegionsOfInterest,
                //		int  	aChannelIndex,
                //		float  	aEstimateDiameter,
                //		bool  	aSubtractBackground,
                //		string  	aSpotFiltersString,
                //		bool  	aRegionsFromLocalContrast,
                //		bool  	aRegionsThresholdAutomatic,
                //		float  	aRegionsThresholdManual,
                //		bool  	aRegionsSpotsDiameterFromVolume,
                //		bool  	aRegionsCreateChannel
                //	)

            if ( aEstimateDiameterXYZ!=null ){ // look for ellipses
                spots = EasyXT.getImaris().GetImageProcessing().DetectEllipticSpotsRegionGrowing(aDataSet,
                        aRegionsOfInterest,
                        aChannelIndex,
                        aEstimateDiameterXYZ,
                        aSubtractBackground,
                        aSpotFiltersString,
                        aRegionsFromLocalContrast,
                        aRegionsThresholdAutomatic,
                        aRegionsThresholdManual,
                        aRegionsSpotsDiameterFromVolume,
                        aRegionsCreateChannel);

            } else { // or simple spots

                spots = EasyXT.getImaris().GetImageProcessing().DetectSpotsRegionGrowing(aDataSet,
                        aRegionsOfInterest,
                        aChannelIndex,
                        aEstimateDiameter,
                        aSubtractBackground,
                        aSpotFiltersString,
                        aRegionsFromLocalContrast,
                        aRegionsThresholdAutomatic,
                        aRegionsThresholdManual,
                        aRegionsSpotsDiameterFromVolume,
                        aRegionsCreateChannel);
            }

        } else {
                // DetectSpots2()
                // ISpots* Imaris::IImageProcessing::DetectSpots2 	( 	IDataSet *  	aDataSet,
                //		tInts2D  	aRegionsOfInterest,
                //		int  	aChannelIndex,
                //		float  	aEstimateDiameter,
                //		bool  	aSubtractBackground,
                //		string  	aSpotFiltersString
                //	)

                // TODO Understand what happens with the Map<String, String> with detectspots2
                if ( aEstimateDiameterXYZ!=null ){ // look for ellipses
                    spots = EasyXT.getImaris().GetImageProcessing().DetectEllipticSpots(aDataSet,
                            aRegionsOfInterest,
                            aChannelIndex,
                            aEstimateDiameterXYZ,
                            aSubtractBackground,
                            aSpotFiltersString);

                } else { // or simple spots
                    spots = EasyXT.getImaris().GetImageProcessing().DetectSpots2(aDataSet,
                            aRegionsOfInterest,
                            aChannelIndex,
                            aEstimateDiameter,
                            aSubtractBackground,
                            aSpotFiltersString);
                }
        }

        // EasyXT Specific

        if (name!=null) {
            spots.SetName(name);
        } // or else it will have the default Imaris name


        if (color!=null) {
            // copied from SurfacesDetector
            spots.SetColorRGBA( color[0] + (color[1] * 256) + (color[2] * 256 * 256 ) );
        }

        return spots;
    }

    // Removes a bit of the verbosity
    public static SpotsDetectorBuilder Channel(int indexChannel ) throws Error {
        return SpotsDetectorBuilder.aSpotsDetector(indexChannel);
    }

    public static final class SpotsDetectorBuilder {

        IDataSetPrx aDataSet;
        int[][]     aRegionsOfInterest; // aRegionsOfInterest is a Nx8 matrix with the Rois : public [vMinX, vMinY, vMinZ, vMinT, vMaxX, vMaxY, vMaxZ, vMaxT]
        Integer     aChannelIndex;
        Float       aEstimateDiameter;
        Boolean     aSubtractBackground;
        String      aSpotFiltersString;

        // Additional fields from Imaris API - DetectSpotsRegionGrowing
        // If aRegionsFromLocalContrast is false, regions are computed from channel intensity.
        // If aRegionsThresholdAutomatic is true, aRegionsThresholdManual is ignored.
        // If aRegionsSpotsDiameterFromVolume is false, spots diameter is equal to region border distance.

        Boolean  	aRegionsFromLocalContrast;
        Boolean  	aRegionsThresholdAutomatic;
        Float  	    aRegionsThresholdManual;
        Boolean  	aRegionsSpotsDiameterFromVolume;
        Boolean  	aRegionsCreateChannel;

        // Fields added to detect ellipses

        float[] aEstimateDiameterXYZ;

        // Fields added to modify output

        String name;
        Integer[] color;

        private SpotsDetectorBuilder(int channelIndex) throws Error {
            // default values
            aDataSet = EasyXT.getImaris().GetDataSet();
            this.aChannelIndex = channelIndex;
        }

        public static SpotsDetectorBuilder aSpotsDetector(int channelIndex) throws Error  {
            return new SpotsDetectorBuilder(channelIndex);
        }

        public SpotsDetectorBuilder setDataSet(IDataSetPrx aDataSet) {
            this.aDataSet = aDataSet;
            return this;
        }

        public SpotsDetectorBuilder setROI(int[][] aRegionsOfInterest) {
            this.aRegionsOfInterest = aRegionsOfInterest;
            return this;
        }

        public SpotsDetectorBuilder setColor(Integer[] color) {
            this.color = color;
            return this;
        }

        public SpotsDetectorBuilder setName(String name) {
            this.name = name;
            return this;
        }

        /**
         *
         * @param diameter , corresponds to [Source Channel] Estimated XY Diameter in the "Creation Parameters"
         *                 (in the "Creation" tab of a completed spots object)
         * @return
         */
        public SpotsDetectorBuilder setDiameter(double diameter) {
            this.aEstimateDiameter = new Float(diameter);
            return this;
        }

        /**
         * setAxialDiameter build the float[] aEstimateDiameterXYZ ( required by DetectEllipticSpots() and DetectSpotsRegionGrowing() )
         * requires aEstimateDiameter to be set otherwise print an error
         *
         * TODO decide if it's a good way to do it or if we should setDiameterXY by default to avoid error.
         ** I did it this way because the creation parameters has "Estimated XY Diameter" and "Estimated Z Diameter"
         ** I also created an alternative function setDiameterXYZ(aEstimateDiameterXY, aEstimateDiameterZ)
         *
         * @param aEstimateDiameterZ , corresponds to [Source Channel] Estimated Z Diameter in the "Creation Parameters"
         *                           (in the "Creation" tab of a completed spots object)
         * @return
         */
        public SpotsDetectorBuilder setAxialDiameter( double aEstimateDiameterZ ){
            if ( this.aEstimateDiameter != null)
                this.aEstimateDiameterXYZ = new float[] {this.aEstimateDiameter,this.aEstimateDiameter , new Float(aEstimateDiameterZ) };
            else
                System.err.println( "EasyXT error log : Please specify XY diameter using setDiameter() when building the SpotsDetector.");
            return this;
        }

        /**
         * setDiameterXYZ build the float[] aEstimateDiameterXYZ
         * required by DetectEllipticSpots() and DetectSpotsRegionGrowing()
         * an alternative to setAxialDiameter
         *
         * @param aEstimateDiameterXY , corresponds to [Source Channel] Estimated XY Diameter in the "Creation Parameters"
         *                           (in the "Creation" tab of a completed spots object)
         * @param aEstimateDiameterZ , corresponds to [Source Channel] Estimated Z Diameter in the "Creation Parameters"
         *                           (in the "Creation" tab of a completed spots object)
         * @return
         */
        public SpotsDetectorBuilder setDiameterXYZ(double aEstimateDiameterXY, double aEstimateDiameterZ ){
            this.aEstimateDiameterXYZ = new float[] { new Float(aEstimateDiameterXY), new Float(aEstimateDiameterXY), new Float(aEstimateDiameterZ)};
            return this;
        }

        /**
         *
         * @param aSubtractBackground , corresponds to [Source Channel] Background Substraction = true in the "Creation Parameters"
         *                             (in the "Creation" tab of a completed spots object)
         * @return
         */
        public SpotsDetectorBuilder isSubtractBackground(Boolean aSubtractBackground) {
            this.aSubtractBackground = aSubtractBackground;
            return this;
        }

        /**
         *
         * @param aFiltersString , corresponds to [Classify Spots] with a String (eg. "Quality" above 5) in the "Creation Parameters"
         *                         (in the "Creation" tab of a completed spots object)
         *                       use  "\" as escape character  or combination of '' and ""
         * @return
         */
        public SpotsDetectorBuilder setFilter(String aFiltersString) {
            this.aSpotFiltersString = aFiltersString;
            return this;
        }

        /**
         *
         * @param aRegionsFromLocalContrast, corresponds to  [Spot Region Type] Region Growing in the "Creation Parameters"
         *                                   if true = Local Contrast
         *                                   if false = TO DO
         *                                   (in the "Creation" tab of a completed spots object)
         * @return
         */
        public SpotsDetectorBuilder isRegionsFromLocalContrast(Boolean aRegionsFromLocalContrast) {
            this.aRegionsFromLocalContrast = aRegionsFromLocalContrast;
            return this;
        }

        /**
         *  corresponds to [Spot Regions] Region Growing Automatic Threshold = true in the "Creation Parameters"
         *  (in the "Creation" tab of a completed spots object)
         * @return
         */
        public SpotsDetectorBuilder enableRegionsThresholdAutomatic() {
            this.aRegionsThresholdAutomatic = true;
            return this;
        }

        /**
         *
         * @param threshold , corresponds to [Spot Regions] Region Growing Manual Threshold in the "Creation Parameters"
         *                   (in the "Creation" tab of a completed spots object)
         * @return
         */
        public SpotsDetectorBuilder setRegionsThresholdManual(double threshold) {
            this.aRegionsThresholdManual = new Float(threshold);
            this.aRegionsThresholdAutomatic = false;
            return this;
        }

        // TODO it's not necessary true BUT it's a required parameters, need a setter function instead
        public SpotsDetectorBuilder createRegionsChannel() {
            this.aRegionsCreateChannel = true;
            return this;
        }

        /**
         *
         * @param doItOrNot , corresponds to [Spot Regions] Create Region Channel in the "Creation Parameters"
         *                  (in the "Creation" tab of a completed spots object)
         * @return
         */
        public SpotsDetectorBuilder createRegionsChannel( boolean doItOrNot) {
            this.aRegionsCreateChannel = doItOrNot;
            return this;
        }

        /**
         *
         * @param flag , corresponds to [Spot Regions] Region Growing Diameter = Diameter From Volume in the "Creation Parameters"
         *             (in the "Creation" tab of a completed spots object)
         * @return
         */
        public SpotsDetectorBuilder isRegionsSpotsDiameterFromVolume(Boolean flag) {
            this.aRegionsSpotsDiameterFromVolume = flag;
            return this;
        }

        public SpotsDetector build() {
            SpotsDetector spotsDetector = new SpotsDetector();
            spotsDetector.aDataSet = this.aDataSet;
            spotsDetector.aRegionsOfInterest = this.aRegionsOfInterest;
            spotsDetector.aChannelIndex = this.aChannelIndex;
            spotsDetector.aEstimateDiameter = this.aEstimateDiameter;
            spotsDetector.aSubtractBackground = this.aSubtractBackground;
            spotsDetector.aSpotFiltersString = this.aSpotFiltersString;

            spotsDetector.aRegionsFromLocalContrast = this.aRegionsFromLocalContrast;
            spotsDetector.aRegionsThresholdAutomatic = this.aRegionsThresholdAutomatic;
            spotsDetector.aRegionsThresholdManual = this.aRegionsThresholdManual;
            spotsDetector.aRegionsSpotsDiameterFromVolume = this.aRegionsSpotsDiameterFromVolume;
            spotsDetector.aRegionsCreateChannel = this.aRegionsCreateChannel;

            spotsDetector.aEstimateDiameterXYZ = this.aEstimateDiameterXYZ;
            spotsDetector.color = this.color;
            spotsDetector.name = this.name;

            return spotsDetector;
        }
    }
}
