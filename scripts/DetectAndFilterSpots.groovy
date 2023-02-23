
import Imaris.ISurfacesPrx
import ch.epfl.biop.imaris.EasyXT
import ij.plugin.*
import ij.*

IJ.run("Close All", "")

// Imaris comes with a couple of images, stored into a user folder...
// "celldemo.ims" , ...
image_path = EasyXT.Samples.getImarisDemoFile("celldemo.ims")
//println image_path

EasyXT.Files.openImage(image_path)

// Detect surfaces
spots = EasyXT.Spots.create(0)
						.setName("My Elliptic Region Grown Spots")
                    //.setDiameter( 1.0 )// [Source Channel] Estimated XY Diameter
                    //.setAxialDiameter(2.0)// [Source Channel] Estimated Z Diameter
                    	.setDiameterXYZ(1.0, 2.0)// alternative  that sets both parameters
                    	.isSubtractBackground(true)// [Source Channel] Background Substraction = true
                    	.setFilter("\"Quality\" above 15.0")// [Classify Spots] A String, "\" as escape character
                    	.isRegionsFromLocalContrast(true)// [Spot Region Type] Region Growing  = Local Contrast (if set to false => intensity)
                    	.setRegionsThresholdManual(40)// [Spot Regions] Region Growing Manual Threshold (Automatic Threshold is set to false if setting manual value)
                    	.isRegionsSpotsDiameterFromVolume(true)// [Spot Regions] Region Growing Diameter = Diameter From Volume
                    	.isCreateRegionsChannel(false)// [Spot Regions] Create Region Channel
                    	.setColor(new Integer[]{255, 128, 0})
                    	.build()
                    	.detect()
                    	
//uncomment below to have the "non-tracked" surface
EasyXT.Scene.setName(spots, "spots")
EasyXT.Scene.addItem(spots)



filtered_spots = EasyXT.Utils.filter(spots, "Volume", 20, 100)
EasyXT.Scene.setName(filtered_spots, "filtered_spots")
EasyXT.Scene.addItem(filtered_spots)

return

// And finally track Surfaces and add to the scene
tracked_surf = EasyXT.Tracks.create(nuclei_surf)
					.useBrownianMotion()
					.setMaxDistance(10.0)
					.setGapSize(3)
					.build()
					.track()

EasyXT.Scene.setName(tracked_surf, "Tracked_Nuclei")
EasyXT.Scene.addItem(tracked_surf)
