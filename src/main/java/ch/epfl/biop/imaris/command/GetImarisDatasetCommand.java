package ch.epfl.biop.imaris.command;

import Imaris.Error;
import ch.epfl.biop.imaris.EasyXT;
import ij.ImagePlus;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@Plugin(type = Command.class, menuPath = "Plugins>BIOP>EasyXT>Get Complete Imaris Dataset")
public class GetImarisDatasetCommand implements Command {

    @Parameter(type = ItemIO.OUTPUT)
    ImagePlus dataset;

    @Override
    public void run() {
        try {
            dataset = EasyXT.Dataset.getImagePlus(EasyXT.Dataset.getCurrent());
        } catch (Error error) {
            error.printStackTrace();
        }
    }
}
