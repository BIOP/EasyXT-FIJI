package ch.epfl.biop.imaris.demo;

import ch.epfl.biop.imaris.EasyXT;
import net.imagej.ImageJ;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

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

    public static void main(String... args) throws Imaris.Error, URISyntaxException {
        ImageJ ij = new ImageJ();
        ij.ui().showUI();
        URI demoImageURI = FreshStartWithIJAndBIOPImsSample.class.getResource("/HeLa_H2B-mcherry_Tubline-EGFP_mitochondria-MitoTracker_reduced.ims").toURI();
        EasyXT.openImage(new File(demoImageURI));
    }

}
