import Imaris.Error;
import ch.epfl.biop.imaris.demo.*;
import org.junit.Test;

/**
 * Execute each demo one at a time - no error should be thrown
 */
public class TestDemos {

    @Test
    public void AddChildObjects() throws Error {
        AddChildObjects.main("Test Mode");
    }

    @Test
    public void AddStatsDemo() throws Error {
        AddStatsDemo.main("Test Mode");
    }

    @Test
    public void AllGetters() throws Error {
        AllGetters.main("Test Mode");
    }

    @Test
    public void FreshStartWithIJAndBIOPImsSample() throws Error {
        FreshStartWithIJAndBIOPImsSample.main("Test Mode");
    }

    @Test
    public void GetStatisticsDemo() throws Error {
        GetStatisticsDemo.main("Test Mode");
    }

    @Test
    public void IJSimpleLaunch() throws Error {
        IJSimpleLaunch.main("Test Mode");
    }

    @Test
    public void MakeAndGetSpotsDemo() throws Error {
        MakeAndGetSpotsDemo.main("Test Mode");
    }

    @Test
    public void MakeAndGetSurfaceDemo() throws Error {
        MakeAndGetSurfaceDemo.main("Test Mode");
    }

    @Test
    public void SendNewSurfaceDemo() throws Error {
        SendNewSurfaceDemo.main("Test Mode");
    }

}
