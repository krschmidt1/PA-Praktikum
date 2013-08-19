package main;

import org.lwjgl.PointerBuffer;
import org.lwjgl.opencl.CLCommandQueue;
import org.lwjgl.opencl.CLContext;
import org.lwjgl.opencl.CLKernel;
import org.lwjgl.opencl.CLMem;
import org.lwjgl.opencl.CLProgram;

import pa.cl.CLUtil;
import pa.cl.CLUtil.PlatformDevicePair;
import pa.cl.OpenCL;
import pa.util.IOUtil;
import pa.util.SizeOf;

public class KernelRuntime 
{
    public static void main(String[] args)
    {
        //how to measure kernel runtime
        CLUtil.createCL();
        
        PlatformDevicePair pair = CLUtil.choosePlatformAndDevice();
        
        CLContext context = OpenCL.clCreateContext(pair.platform, pair.device, null, null);
        
        CLCommandQueue queue = OpenCL.clCreateCommandQueue(context, pair.device, OpenCL.CL_QUEUE_PROFILING_ENABLE);
        
        CLProgram program = OpenCL.clCreateProgramWithSource(context, IOUtil.readFileContent("kernel/kernel.cl"));
        
        OpenCL.clBuildProgram(program, pair.device, "", null);
        
        CLKernel kernel = OpenCL.clCreateKernel(program, "rngrngrng");
        
        int elements = 1 << 20;
        
        CLMem mem = OpenCL.clCreateBuffer(context, 0, elements * SizeOf.INTEGER);
        
        OpenCL.clSetKernelArg(kernel, 0, mem);
        
        PointerBuffer gws = new PointerBuffer(1);
        gws.put(0, elements);
        
        long nanos = CLUtil.measureKernelCall(kernel, queue, gws, null, 1);
        
        System.out.println(String.format("'rng' for %d elements took %.3f Seconds.", elements, nanos / (1E9)));
        
        OpenCL.clReleaseMemObject(mem);
        OpenCL.clReleaseKernel(kernel);
        OpenCL.clReleaseProgram(program);
        OpenCL.clReleaseCommandQueue(queue);
        OpenCL.clReleaseContext(context);
        
        CLUtil.destroyCL();
    }
}
