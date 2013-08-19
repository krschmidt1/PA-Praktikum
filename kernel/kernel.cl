kernel void move(global float* positions)
{
	uint id = get_global_id(0);
	
	positions[id*3+1] = positions[id*3+1] + 0.01f;
	if(positions[id*3+1]>1.0f)
		positions[id*3+1]=-1.0f;
}