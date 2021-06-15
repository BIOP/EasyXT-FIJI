import ch.epfl.biop.imaris.*
import ij.*
import ij.plugin.Duplicator
import ij.measure.ResultsTable

IJ.run("Close All", "");

// based on the Imaris image name, we get the path and open the image
image_path = EasyXT.Samples.getImarisDemoFile("CellDemoMembrane3D.ims")
EasyXT.Files.openImage(image_path)

// get the corresponding ImagePlus
imp = EasyXT.Dataset.getImagePlus(EasyXT.Dataset.getCurrent())
imp.show()


// Here we just do some silly processing in Fiji
// duplicate, the channel 1 , threshold, median filter it and skeletonize
mb = new Duplicator().run(imp, 1, 1, 1, 44, 1, 1);
mb.show()
IJ.run(mb, "Convert to Mask", "method=Default background=Dark calculate black");
IJ.run(mb, "Median 3D...", "x=2 y=2 z=2");
IJ.run(mb, "Skeletonize (2D/3D)", "");
mb.setTitle("Skeleton")

// before we send back the Mask as a Surfaces to Imaris
mb_surf = EasyXT.Surfaces.create(mb)
EasyXT.Scene.addItem(mb_surf)


// How to get some RESULTS !
//
// to get all the measurements of a Imaris Surfaces
// rt = EasyXT.Stats.export(mb_surf) 
//
// but it also possible to define measurements
measures = Arrays.asList("Volume", "Intensity Mean")

// here we would like to append Results if a table exist
rt = ResultsTable.getResultsTable("Results")
if (rt == null) {// no existing table, so we export using EasyXT
    rt = EasyXT.Stats.export(mb_surf, measures)
} else {// a table exist, so we append
    rt = new StatsQuery(mb_surf)
            .selectStatistics(measures)
            .appendTo(rt)
            .get()
}
rt.show()
