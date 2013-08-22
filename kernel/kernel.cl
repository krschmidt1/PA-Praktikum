kernel void move(global float* positions, global float* velocities, global float* lifetimes, const int dTime)
{
	uint id = get_global_id(0);
	float speed = 0.005f;
	
	if(lifetimes[id * 2] <= 0.0f) {
		lifetimes[id * 2 + 1] = 0.0f;
		return;
	}

	lifetimes[id * 2]  -= dTime;
	
	float timeFactor = speed * ((float)dTime);
	positions[id * 3]     = positions[id * 3]     + velocities[id * 3]     * timeFactor;
	positions[id * 3 + 1] = positions[id * 3 + 1] + velocities[id * 3 + 1] * timeFactor;
	positions[id * 3 + 2] = positions[id * 3 + 2] + velocities[id * 3 + 2] * timeFactor;
	
	// DEBUG
	if(positions[id*3+1]>1) positions[id*3+1] = -1;
		
}

kernel void respawn(global float* positions, global float* velocities, global float* lifetimes, global float* newValues) 
{
	uint id = get_global_id(0);
	
	ushort numParams = 7;
	
	positions[id * 3]     = newValues[id * numParams];
	positions[id * 3 + 1] = newValues[id * numParams + 1];
	positions[id * 3 + 2] = newValues[id * numParams + 2];
	velocities[id * 3]    = newValues[id * numParams + 3];
	velocities[id * 3 + 1]= newValues[id * numParams + 4];
	velocities[id * 3 + 2]= newValues[id * numParams + 5];
	lifetimes[id * 2]     = newValues[id * numParams + 6];
	lifetimes[id * 2 + 1] = 1.0f;
}