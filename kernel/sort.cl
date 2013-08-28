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

kernel void shift(global float* positions, global float* velocities, global float* lifetimes, const int size, const int n) 
{
	//uint id = get_global_id(0);
	//uint size = get_global_size(0);
	
	//uint id3 = (size*3)+id*3;
	//uint id2 = (size*2)+id*2;
	
	for(int id = 0; id < size; id++) {
		positions[(size+n-1)*3-id*3]     = positions[(size-1)*3-id*3];
		positions[(size+n-1)*3-id*3 + 1] = positions[(size-1)*3-id*3 + 1];
		positions[(size+n-1)*3-id*3 + 2] = positions[(size-1)*3-id*3 + 2];
	
		velocities[(size+n-1)*3-id*3]     = velocities[(size-1)*3-id*3];
		velocities[(size+n-1)*3-id*3 + 1] = velocities[(size-1)*3-id*3 + 1];
		velocities[(size+n-1)*3-id*3 + 2] = velocities[(size-1)*3-id*3 + 2];
	
		lifetimes[(size+n-1)*2-id*2]     = lifetimes[(size-1)*2-id*2];
		lifetimes[(size+n-1)*2-id*2 + 1] = lifetimes[(size-1)*2-id*2 + 1];
	}
}