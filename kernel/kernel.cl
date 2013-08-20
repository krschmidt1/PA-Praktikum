kernel void move(global float* positions, global float* lifetimes, const int dTime)
{
	uint id = get_global_id(0);
	
	if(lifetimes[id*2] <= 0.0f) {
		lifetimes[id*2+1] = 0.0f;
	}
	else {
		lifetimes[id*2] -= dTime;
	}
	
	if(positions[id*3+1] >= 1.0f) {
		positions[id*3+1] = -1.0f;
	}
	else
		positions[id*3+1] = positions[id*3+1] + 0.01f;
		
}

kernel void respawn(global float* positions, global float* lifetimes) 
{
	uint id = get_global_id(0);
}