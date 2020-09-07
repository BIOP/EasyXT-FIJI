package ch.epfl.biop.imaris;

import Imaris.Error;
import Imaris.IDataSetPrx;
import ij.measure.Calibration;

/**
 * Extension of ImageJ calibration:
 * Easy way to set ImageJ calibration from an Imaris dataset
 * by using a custom constructor
 */
public class ImarisCalibration extends Calibration {
    public final double xEnd, yEnd, zEnd;
    public int xSize, ySize, zSize, cSize, tSize, bitDepth;

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

        this.pixelWidth  = Math.abs( this.xEnd - this.xOrigin ) / this.xSize;
        this.pixelHeight = Math.abs( this.yEnd - this.yOrigin ) / this.ySize;
        this.pixelDepth  = Math.abs( this.zEnd - this.zOrigin ) / this.zSize;

        this.setUnit( dataset.GetUnit() );
        this.setTimeUnit( "s" );
        this.frameInterval = dataset.GetTimePointsDelta();
        
        this.bitDepth = EasyXT.getBitDepth( dataset );
    }

    public ImarisCalibration getDownsampled( double downsample ) {

        ImarisCalibration new_calibration = (ImarisCalibration) this.clone();

        new_calibration.xSize /= downsample;
        new_calibration.ySize /= downsample;
        new_calibration.zSize /= downsample;

        new_calibration.pixelWidth *= downsample;
        new_calibration.pixelHeight /= downsample;
        new_calibration.pixelDepth /= downsample;

        return new_calibration;
    }
}
