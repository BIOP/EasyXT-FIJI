import ch.epfl.biop.imaris.EasyXT

dapi_ch = 2
vesicles_ch = 3

// Imaris comes with a couple of images, stored into user...
// "celldemo.ims" , ...
image_path = EasyXT.Samples.getImarisDemoFile("celldemo.ims")
println image_path

EasyXT.Files.openImage(image_path)

// To create a surface from a channel, here Nucleus
// you just need to specify the channel index (zero based)
dapi_surf = EasyXT.Surfaces.create(dapi_ch - 1)
        .build()
        .detect()
EasyXT.Scene.addItem(dapi_surf)

// to control a bit more the surface creation, you can set some option
dapi_surf_basic = EasyXT.Surfaces.create(dapi_ch - 1)
        .setSmoothingWidth(1)
//.enableAutomaticLowerThreshold() // default behavior
//.setLowerThreshold(80) // possible to change value to a defined one
        .setSurfaceFilter('"Volume" above 50.0 um^3')
        .setName("Nuclei")
        .build()
        .detect()
EasyXT.Scene.addItem(dapi_surf_basic)

// for more complex surfaces creation cf ....groovy file


// The same for Spots
vesicles_spot = EasyXT.Spots.create(vesicles_ch - 1)
        .setDiameter(0.5)
        .isSubtractBackground(false)
        .setFilter('"Intensity Mean Ch=3 Img=1" above 80.0')
        .setName("Vesicles")
        .build()
        .detect()
EasyXT.Scene.addItem(vesicles_spot)

