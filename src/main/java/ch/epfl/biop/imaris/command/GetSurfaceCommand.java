package ch.epfl.biop.imaris.command;

import Imaris.Error;
import Imaris.ISurfacesPrx;
import ch.epfl.biop.imaris.EasyXT;
import ij.ImagePlus;
import ij.process.LUT;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.stream.IntStream;

@Plugin(type = Command.class, menuPath = "EasyXT>Surface>Get Surface from Imaris")
public class GetSurfaceCommand implements Command{

    @Parameter
    String surfaceName;

    @Parameter(type = ItemIO.OUTPUT)
    ImagePlus surface;

    @Override
    public void run() {
        // Gets an existing surface
        try {
            ISurfacesPrx surfprx = EasyXT.getSurfaces( surfaceName );
            // Display surfaces
            surface = EasyXT.getSurfaceMask( surfprx );
            surface.setLut(LUT.createLutFromColor(EasyXT.getColorFromInt(surfprx.GetColorRGBA())));
            surface.setDisplayRange(0,255);
            // Multiply by 255 to allow to use ImageJ binary functions
            int nProcessor = surface.getStack().getSize();
            IntStream.range(0, nProcessor).parallel().forEach(index -> {
                surface.getStack().getProcessor(index+1).multiply(255);
            });

        } catch (Error error) {
            error.printStackTrace();
        }


    }
}
