package opencl;

import org.lwjgl.opencl.CLCommandQueue;
import org.lwjgl.opencl.CLContext;
import org.lwjgl.opencl.CLKernel;
import org.lwjgl.opencl.CLProgram;

import pa.cl.CLUtil;
import pa.cl.CLUtil.PlatformDevicePair;
import pa.cl.OpenCL;
import pa.util.IOUtil;

public class CL {

	public void init() {
		
		CLUtil.createCL();
		
		PlatformDevicePair pair = CLUtil.choosePlatformAndDevice();
		
		CLContext context = OpenCL.clCreateContext(pair.platform, pair.device, null, null);
        
		CLCommandQueue queue = OpenCL.clCreateCommandQueue(context, pair.device, OpenCL.CL_QUEUE_PROFILING_ENABLE);
        
        CLProgram program = OpenCL.clCreateProgramWithSource(context, IOUtil.readFileContent("kernel/kernel.cl"));
        
        OpenCL.clBuildProgram(program, pair.device, "", null);
        
        CLKernel kernel = OpenCL.clCreateKernel(program, "rngrngrng");
        
		
	}
}
