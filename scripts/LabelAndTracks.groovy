import ch.epfl.biop.imaris.EasyXT
import ij.plugin.*
import ij.*
import Imaris.*

/** In this script we use an Imaris dataset
 *  I.		Make Surfaces from Fiji Labels image
 *  II . 	Use a custom filter on Surfaces (Volume)
 *  III.   Track filtered surfaces
 */



IJ.run("Close All", "");

// Imaris comes with a couple of images, stored into a user folder...
// "celldemo.ims" , ...
image_path = EasyXT.Samples.getImarisDemoFile("CellDevelopment.ims")
//println image_path
EasyXT.Files.openImage(image_path)

// Get the imp
imp = EasyXT.Dataset.getImagePlus(EasyXT.Dataset.getCurrent())
cal = imp.getCalibration()
imp.show()

// I. Here, we'll detect nuclei in Fiji, and get surfaces from labels image_path
// It would be easier to do it with Imaris Surfaces but this won't be fun
//
// I.A. Make a binary
def nuc_imp = new Duplicator().run(imp, 1, 1, 1, imp.getNSlices(), 1, imp.getNFrames());
//nuc_imp.show()
IJ.setRawThreshold(nuc_imp, 100, 255, null);
IJ.run(nuc_imp, "Convert to Mask", "background=Dark black");
IJ.run(nuc_imp, "Median 3D...", "x=2 y=2 z=2");

// I.B. Detect 3D nuclei on all the frames
def hypStk = []
(1..imp.getNFrames()).each{
	mask = new Duplicator().run(nuc_imp, 1, 1, 1, imp.getNSlices(), it, it);
	IJ.run(mask, "3D Nuclei Segmentation (beta)", "auto_threshold=Default manual=128 separate_nuclei");
	labels_imp = IJ.getImage()
	t_imp = labels_imp.duplicate()
	hypStk.add(t_imp)
}
theImp =  Concatenator.run( hypStk as ImagePlus[])
//theImp.show()

// I.C. Add the surfaces to the scene using EasyXT.Surfaces.createFromLabels
nuclei_surf = EasyXT.Surfaces.createFromLabels(theImp)
EasyXT.Scene.setName(nuclei_surf, "3D Nuclei Segmentation")
EasyXT.Scene.addItem(nuclei_surf)

// II . use a filter on surfaces using the Volume measurement
filtered_nuclei_surf = EasyXT.Surfaces.filter(nuclei_surf, "Volume", 1000, 10000)
EasyXT.Scene.setName(filtered_nuclei_surf, "Filtered_Nuclei")
EasyXT.Scene.addItem(filtered_nuclei_surf)

// III. track filtered surfaces and add to the scene
tracked_surf = EasyXT.Tracks.create(filtered_nuclei_surf)
					.useBrownianMotion()
					.setMaxDistance(10.0)
					.setGapSize(3)
					.build()
					.track()

EasyXT.Scene.setName(tracked_surf, "Tracked_Nuclei")
EasyXT.Scene.addItem(tracked_surf)
