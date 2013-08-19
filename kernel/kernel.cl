

#define SIZE 512

kernel void rngrngrng(global uint* data)
{
	uint id = get_global_id(0);
	uint N = get_global_size(0);
	
	for(uint i = 0; i < SIZE; ++i)
	{
		data[(47 * (id + i)) % N] = data[(27 * (id + i)) % N];
	}
}