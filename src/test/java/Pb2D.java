import ij.*;
import ch.epfl.biop.imaris.EasyXT;
import Imaris.*;
import net.imagej.ImageJ;


public class Pb2D {

    public static void main(String... args) throws Exception {
        net.imagej.ImageJ ij = new ImageJ();
        ij.ui().showUI();
        
        EasyXT.Scene.reset();
        IJ.run("Close All", "");

        boolean try2D = true;
        ImagePlus imp = new ImagePlus();

        if (try2D){
            imp = IJ.openImage("http://imagej.nih.gov/ij/images/blobs.gif");
            IJ.run(imp, "Invert LUT", "");
        }else{
            imp = IJ.openImage("http://imagej.nih.gov/ij/images/Spindly-GFP.zip");
        }

        // load an image in Imaris, 2D or 3D depending of try2D  true or flase
        IDataSetPrx dataset = EasyXT.Dataset.create(imp);
        EasyXT.Dataset.setCurrent(dataset);

        // Here EasyXT makes Imaris create/detect a Surfaces object : "surf"
        ISurfacesPrx surf = EasyXT.Surfaces.create(0)
                .setSmoothingWidth(1)
                .enableAutomaticLowerThreshold() // default behavior
                //.setLowerThreshold(80) // possible to change value to a defined one
                .setSurfaceFilter("\"Volume\" above 1.0 um^3")
                .setName("Nuclei")
                .build()
                .detect();

        // we now add "surf" to the scene
        EasyXT.Scene.addItem(surf);

        // Here we can retrieve
        ISurfacesPrx getSurf = EasyXT.Scene.findSurfaces("Nuclei");

        // Here we produce an error : "Stack required" if the image is 2D
        //mask_imp = EasyXT.Surfaces.getMaskImage( surf  )
        ImagePlus mask_imp = EasyXT.Surfaces.getMaskImage(getSurf);
        mask_imp.show();
    }
}
