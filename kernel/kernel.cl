kernel void move(global float* positions, global float* lifetimes, const int dTime)
{
	uint id = get_global_id(0);
	
	if(lifetimes[id*2] <= 0.0f) {
		lifetimes[id*2+1] = 0.0f;
		return;
	}

	lifetimes[id*2]  -= dTime;
	positions[id*3+1] = positions[id*3+1] + 0.0005f * ((float)dTime);
	
	// DEBUG
	if(positions[id*3+1]>1) positions[id*3+1] = -1;
		
}

kernel void respawn(global float* positions, global float* lifetimes, global float* newValues, const int spawnNum) 
{
	uint id = get_global_id(0);
	
	if(id > spawnNum)
		return;
		
	positions[id * 3]     = newValues[id * 3];
	positions[id * 3 + 1] = newValues[id * 3 + 1];
	positions[id * 3 + 2] = newValues[id * 3 + 2];
	lifetimes[id * 2]     = newValues[id * 3 + 3];
	lifetimes[id * 2 + 1] = 1.0f;
}