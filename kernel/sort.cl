kernel void bitonicSort(global float* data, const int phase)
{
	uint id = get_global_id(0);
	uint size = get_global_size(0);
	int stride = size/phase;
	
	float first  = data[(id/stride)*2*stride+ id%stride];
	float second = data[(id/stride)*2*stride+ id%stride+stride];

	if(first > second) {
		data[(id/stride)*2*stride+ id%stride] 	     = second;
		data[(id/stride)*2*stride+ id%stride+stride] = first;
	}
}

kernel void makeBitonicUp(global float* data, const int phase, const int offset)
{
	uint id = get_global_id(0);
	uint size = get_global_size(0);
	int stride = size/phase;
	
	float first  = data[offset+(id/stride)*2*stride+ id%stride];
	float second = data[offset+(id/stride)*2*stride+ id%stride+stride];

	if(first > second) {
		data[offset+(id/stride)*2*stride+ id%stride] 	    = second;
		data[offset+(id/stride)*2*stride+ id%stride+stride] = first;
	}

}

kernel void makeBitonicDown(global float* data, const int phase, const int offset)
{
	uint id = get_global_id(0);
	uint size = get_global_size(0);
	int stride = size/phase;
	
	float first  = data[offset+(id/stride)*2*stride+ id%stride];
	float second = data[offset+(id/stride)*2*stride+ id%stride+stride];

	if(first < second) {
		data[offset+(id/stride)*2*stride+ id%stride] 	    = second;
		data[offset+(id/stride)*2*stride+ id%stride+stride] = first;
	}

}