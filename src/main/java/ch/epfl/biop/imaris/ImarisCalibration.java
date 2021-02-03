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
import ij.ImagePlus;
import ij.measure.Calibration;

import java.util.Objects;

/**
 * Extension of ImageJ calibration:
 * Easy way to set ImageJ calibration from an Imaris dataset
 * by using a custom constructor
 * @author Olivier Burri
 * @author Nicolas Chiaruttini
 * @version 1.0
 */
public class ImarisCalibration extends Calibration {
    public final double xEnd, yEnd, zEnd;
    public int xSize, ySize, zSize, cSize, tSize;
    public float[] cMin, cMax;
    public int[] cColorsRGBA;
    public String[] cNames;
    public ImarisCalibration( IDataSetPrx dataset ) throws Error {

        // I know it's supposed to be pixels... BUT
        // why is it double then?
        // Makes no sense so here I do what I want
        this.xOrigin = dataset.GetExtendMinX();
        this.yOrigin = dataset.GetExtendMinY();
        this.zOrigin = dataset.GetExtendMinZ();

        this.xEnd = dataset.GetExtendMaxX();
        this.yEnd = dataset.GetExtendMaxY();
        this.zEnd = dataset.GetExtendMaxZ();

        this.xSize = dataset.GetSizeX();
        this.ySize = dataset.GetSizeY();
        this.zSize = dataset.GetSizeZ();

        this.cSize = dataset.GetSizeC();
        this.tSize = dataset.GetSizeT();

        this.pixelWidth  = (this.xEnd - this.xOrigin) / (this.xSize-1) ;
        this.pixelHeight = (this.yEnd - this.yOrigin) / (this.ySize-1);
        this.pixelDepth  = (this.zEnd - this.zOrigin) / (this.zSize-1);

        this.setUnit( dataset.GetUnit() );
        this.setTimeUnit( "s" );
        this.frameInterval = dataset.GetTimePointsDelta();

        // For each channel get the min and max display values and colors
        cMin = new float[this.cSize];
        cMax = new float[this.cSize];
        cColorsRGBA = new int[this.cSize];
        cNames = new String[this.cSize];

        for (int c = 0; c < this.cSize; c++) {
            cMin[c] = dataset.GetChannelRangeMin(c);
            cMax[c] = dataset.GetChannelRangeMax(c);
            cColorsRGBA[c] = dataset.GetChannelColorRGBA(c);
            cNames[c] = dataset.GetChannelName(c);
        }

    }

    public ImarisCalibration getDownsampled( double downsample ) {

        ImarisCalibration new_calibration = (ImarisCalibration) this.clone();

        new_calibration.xSize *= downsample;
        new_calibration.ySize *= downsample;
        new_calibration.zSize *= downsample;

        new_calibration.pixelWidth  /= downsample;
        new_calibration.pixelHeight /= downsample;
        new_calibration.pixelDepth  /= downsample;

        return new_calibration;
    }

    /**
     * Compares this calibration to the provided image. Returns true if size in XYCZT is the same as the image
     * @param imp the image to compare the dimensions of
     * @return true if the current calibration and the image match in XYCZT
     */
    public boolean isSameSize(ImagePlus imp) {
        return xSize == imp.getWidth() && ySize == imp.getHeight() && zSize == imp.getNSlices() && cSize == imp.getNChannels() && tSize == imp.getNFrames();
    }
}
