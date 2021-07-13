package ImarisIssues

import Imaris.ISurfacesPrx
import ch.epfl.biop.imaris.EasyXT
import ij.plugin.*
import ij.*

IJ.run("Close All", "");

// Imaris comes with a couple of images, stored into user...
// "celldemo.ims" , ...
image_path = EasyXT.Samples.getImarisDemoFile("CellDevelopment.ims")
println image_path

EasyXT.Files.openImage(image_path)

// Get the imp
imp = EasyXT.Dataset.getImagePlus(EasyXT.Dataset.getCurrent())
cal = imp.getCalibration()
imp.show()

def nuc_imp = new Duplicator().run(imp, 1, 1, 1, imp.getNSlices(), 1, imp.getNFrames());

nuc_imp.show()

IJ.setRawThreshold(nuc_imp, 100, 255, null);
IJ.run(nuc_imp, "Convert to Mask", "background=Dark black");


IJ.run(nuc_imp, "Median 3D...", "x=2 y=2 z=2");


//nFrames = imp.getNFrames()
def hypStk = []
(1..imp.getNFrames()-1).each{
//(1..5).each{
	mask = new Duplicator().run(nuc_imp, 1, 1, 1, imp.getNSlices(), it, it);
	IJ.run(mask, "3D Nuclei Segmentation (beta)", "auto_threshold=Default manual=128 separate_nuclei");
	labels_imp = IJ.getImage()
	t_imp = labels_imp.duplicate()
	hypStk.add(t_imp)
}

theImp =  Concatenator.run( hypStk as ImagePlus[])
theImp.show()


surf = EasyXT.Surfaces.createFromLabels(theImp)
EasyXT.Scene.setName(surf, "ConnectedComponent")
EasyXT.Scene.addItem(surf)


// Track Surfaces

tracked_surf = (ISurfacesPrx) EasyXT.Tracks.create(surf)
        .useBrownianMotion()
        .setMaxDistance(10.0)
        .setGapSize(3)
        .build()
        .track()

EasyXT.Scene.setName(tracked_surf, "Tracked_")
EasyXT.Scene.addItem(tracked_surf)
