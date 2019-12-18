import Imaris.Error;
import Imaris.IDataSetPrx;
import ij.measure.Calibration;

public class ImarisCalibration extends Calibration {
    public final double xEnd, yEnd, zEnd;
    public int xSize, ySize, zSize, cSize, tSize;

    public ImarisCalibration( IDataSetPrx dataset ) throws Error {

        // YES Bitch, I know it's supposed to be pixels... BUT
        // why is it double then?
        // Makes no sense se here I do what I want
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

        this.pixelWidth  = Math.abs( this.xEnd - this.xOrigin  ) / this.xSize;
        this.pixelHeight = Math.abs( this.yEnd - this.yOrigin  ) / this.ySize;
        this.pixelDepth  = Math.abs( this.zEnd - this.zOrigin  ) / this.zSize;

        this.setUnit( dataset.GetUnit() );
        this.setTimeUnit( "s" );
        this.frameInterval = dataset.GetTimePointsDelta();
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
