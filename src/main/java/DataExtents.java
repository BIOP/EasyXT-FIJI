import Imaris.Error;
import Imaris.IApplicationPrx;

public class DataExtents {
    float xstart;
    float xend;

    float ystart;
    float yend;

    float zstart;
    float zend;

    int sizex;
    int sizey;
    int sizez;

    int sizec;
    int sizet;




    public DataExtents( IApplicationPrx app ) throws Error {
        // Populate the extents
        xstart = app.GetDataSet().GetExtendMinX();
        ystart = app.GetDataSet().GetExtendMinY();
        zstart = app.GetDataSet().GetExtendMinZ();

        xend = app.GetDataSet().GetExtendMaxX();
        yend = app.GetDataSet().GetExtendMaxY();
        zend = app.GetDataSet().GetExtendMaxZ();

        sizex = app.GetDataSet().GetSizeX();
        sizey = app.GetDataSet().GetSizeY();
        sizez = app.GetDataSet().GetSizeZ();

        sizec = app.GetDataSet().GetSizeC();
        sizet = app.GetDataSet().GetSizeT();


    }
}
