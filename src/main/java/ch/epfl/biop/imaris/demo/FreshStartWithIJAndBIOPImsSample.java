package ch.epfl.biop.imaris.demo;

import ch.epfl.biop.imaris.EasyXT;
import net.imagej.ImageJ;

import java.io.File;

public class FreshStartWithIJAndBIOPImsSample {

    public static void main(String... args) throws Imaris.Error {
        ImageJ ij = new ImageJ();
        ij.ui().showUI();
        EasyXT.openImage(new File("C:\\Users\\chiarutt\\Imaris Demo Images\\CellDemoMembrane3D.ims"));
        //EasyXT.openImage(new File("src/main/resources/blobs.tif"));

    }
}
