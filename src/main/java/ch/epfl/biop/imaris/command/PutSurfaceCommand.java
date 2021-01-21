package ch.epfl.biop.imaris.command;

import Imaris.Error;
import Imaris.ISurfacesPrx;
import ch.epfl.biop.imaris.EasyXT;
import ij.ImagePlus;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@Plugin(type = Command.class, menuPath = "Plugins>BIOP>EasyXT>Surface>Put Surface to Imaris")
public class PutSurfaceCommand implements Command {

    @Parameter
    ImagePlus surface_imp;

    @Override
    public void run() {
        // Using the Image Name as a key to access the surface
        try {
            ISurfacesPrx surfprx = EasyXT.Surfaces.getSurfaces(surface_imp.getTitle());
            //IDataSetPrx dataset = EasyXT.getSurfaceDataset(surfprx);
            //surfprx.RemoveAllSurfaces();
            EasyXT.Surfaces.setSurfacesMask(surfprx, surface_imp);
            //surfprx.AddSurface(dataset,0);
        } catch (Error error) {
            error.printStackTrace();
        }
    }
}
