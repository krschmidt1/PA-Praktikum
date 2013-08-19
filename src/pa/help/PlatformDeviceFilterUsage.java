package pa.help;

import org.lwjgl.opencl.CL10;

import pa.cl.CLUtil;
import pa.cl.CLUtil.PlatformDeviceFilter;
import pa.cl.CLUtil.PlatformDevicePair;

public class PlatformDeviceFilterUsage 
{
    public static void main(String[] s)
    {
        CLUtil.createCL();
        
        PlatformDeviceFilter filter = new PlatformDeviceFilter();
        
        //set spec here
        filter.addPlatformSpec(CL10.CL_PLATFORM_VENDOR, "NVIDIA");
        filter.addDeviceSpec(CL10.CL_DEVICE_NAME, "GeForce GTX 480");
        filter.setDevCnt(1, 0);
        filter.setDesiredDeviceType(CL10.CL_DEVICE_TYPE_GPU);
        
        //query platform and device
        @SuppressWarnings("unused")
        PlatformDevicePair pair = CLUtil.choosePlatformAndDevice(filter);
        
        //you can use the default one as you did before
        pair = CLUtil.choosePlatformAndDevice();
        
        //....
        
        CLUtil.destroyCL();
    }
}
