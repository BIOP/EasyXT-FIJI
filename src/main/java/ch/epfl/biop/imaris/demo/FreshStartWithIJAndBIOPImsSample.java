package ch.epfl.biop.imaris.demo;

import ch.epfl.biop.imaris.EasyXT;
import net.imagej.ImageJ;

import java.io.File;

/**
 * EasyXT Demo
 *
 * - Creates a new instance of FIJI
 * - Opens the demo image, a cell reaching metaphase, 3 channels, 40 timepoints, 8 bits
 *
 *  @author Nicolas Chiaruttini
 *
 * October 2020
 *
 * EPFL - SV -PTECH - PTBIOP
 */

public class FreshStartWithIJAndBIOPImsSample {

    public static void main(String... args) throws Imaris.Error {
        ImageJ ij = new ImageJ();
        ij.ui().showUI();
        EasyXT.openImage(new File("src/main/resources/HeLa_H2B-mcherry_Tubline-EGFP_mitochondria-MitoTracker_reduced.ims"));
    }
}
