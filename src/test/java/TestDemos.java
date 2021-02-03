import ch.epfl.biop.imaris.EasyXT;
import ch.epfl.biop.imaris.demo.*;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Execute each demo one at a time - no error should be thrown
 */

public class TestDemos {
private static String mode = "Test Mode";

    @Test@Ignore
    public void AddChildObjects() throws Exception {
        AddChildObjects.main(mode);
    }

    @Test@Ignore
    public void AddStatsDemo() throws Exception {
        AddStatsDemo.main(mode);
    }

    @Test@Ignore
    public void AllGetters() throws Exception {
        AllGettersDemo.main(mode);
    }

    @Test@Ignore
    public void FreshStartWithIJAndBIOPImsSample() throws Exception {
        FreshStartWithIJAndBIOPImsSample.main(mode);
    }

    @Test@Ignore
    public void GetStatisticsDemo() throws Exception {
        GetStatisticsDemo.main(mode);
    }

    @Test@Ignore
    public void LaunchIJFromEasyXT() throws Exception {
        EasyXT.main(mode);
    }

    @Test@Ignore
    public void MakeAndGetSpotsDemo() throws Exception {
        MakeAndGetSpotsDemo.main(mode);
    }

    @Test@Ignore
    public void MakeAndGetSurfaceDemo() throws Exception {
        MakeAndGetSurfaceDemo.main(mode);
    }

    @Test@Ignore
    public void MakeSurfaceFromMaskDemo() throws Exception {
        MakeSurfaceFromMaskDemo.main(mode);
    }

}
