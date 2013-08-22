kernel void move(global float* positions, global float* velocities, global float* lifetimes, const int dTime)
{
	uint id = get_global_id(0);
	float speed = 0.001f;
	
	if(lifetimes[id * 2] <= 0.0f) {
		lifetimes[id * 2]     = -10000.0f;
		lifetimes[id * 2 + 1] = 0.0f;
		return;
	}

	lifetimes[id * 2]  -= dTime;
	
	float timeFactor = speed * ((float)dTime);
	positions[id * 3]     = positions[id * 3]     + velocities[id * 3]     * timeFactor;
	positions[id * 3 + 1] = positions[id * 3 + 1] + velocities[id * 3 + 1] * timeFactor;
	positions[id * 3 + 2] = positions[id * 3 + 2] + velocities[id * 3 + 2] * timeFactor;
	
	// DEBUG
	//if(positions[id*3+1]>1) positions[id*3+1] = -1;
		
}

kernel void respawn(global float* positions, global float* velocities, global float* lifetimes, global float* newValues) 
{
	uint id = get_global_id(0);
	
	ushort numParams = 7;
	
	positions[id * 3]      = newValues[id * numParams];
	positions[id * 3 + 1]  = newValues[id * numParams + 1];
	positions[id * 3 + 2]  = newValues[id * numParams + 2];
	velocities[id * 3]     = newValues[id * numParams + 3];
	velocities[id * 3 + 1] = newValues[id * numParams + 4];
	velocities[id * 3 + 2] = newValues[id * numParams + 5];
	lifetimes[id * 2]      = newValues[id * numParams + 6];
	lifetimes[id * 2 + 1]  = 1.0f;
}

kernel void bitonicSort(global float* lifetime, global float* positions, global float* velocities, const int phase)
{
	uint id     = get_global_id(0);
	uint size   = get_global_size(0);
	
	uint stride    = size / phase;
	uint group     = id / stride;
	uint elementId = id % stride;
	uint groupSize = 2 * stride;
		
	uint firstId2f  = group * groupSize * 2 + elementId * 2;
	uint secondId2f = firstId2f + stride * 2;

	if(lifetime[firstId2f] > lifetime[secondId2f]) {
		uint firstId3f  = group * groupSize * 3 + elementId * 3;
		uint secondId3f = firstId3f + stride * 3;
		
		// store values
		float lifetimeMS = lifetime[firstId2f];
		float alive      = lifetime[firstId2f + 1];
		
		float posX = positions[firstId3f];
		float posY = positions[firstId3f + 1];
		float posZ = positions[firstId3f + 2];
		
		float velX = velocities[firstId3f];
		float velY = velocities[firstId3f + 1];
		float velZ = velocities[firstId3f + 2];
		
		// copy two to one
		lifetime[firstId2f]     = lifetime[secondId2f];
		lifetime[firstId2f + 1] = lifetime[secondId2f + 1];
		
		positions[firstId3f]     = positions[secondId3f];
		positions[firstId3f + 1] = positions[secondId3f + 1];
		positions[firstId3f + 2] = positions[secondId3f + 2];
		
		velocities[firstId3f]     = velocities[secondId3f];
		velocities[firstId3f + 1] = velocities[secondId3f + 1];
		velocities[firstId3f + 2] = velocities[secondId3f + 2];
		
		// receive values
		lifetime[secondId2f]     = lifetimeMS;
		lifetime[secondId2f + 1] = alive;
		
		positions[secondId3f]     = posX;
		positions[secondId3f + 1] = posY;
		positions[secondId3f + 2] = posZ;
		
		velocities[secondId3f]     = velX;
		velocities[secondId3f + 1] = velY;
		velocities[secondId3f + 2] = velZ;
	}
}

kernel void makeBitonicUp(global float* lifetime, global float* positions, global float* velocities, const int phase, const int offset)
{
	uint id     = get_global_id(0);
	uint size   = get_global_size(0);
	
	uint stride    = size / phase;
	uint group     = id / stride;
	uint elementId = id % stride;
	uint groupSize = 2 * stride;
		
	uint firstId2f  = offset * 2 + group * groupSize * 2 + elementId * 2;
	uint secondId2f = firstId2f + stride * 2;

	if(lifetime[firstId2f] > lifetime[secondId2f]) {
		uint firstId3f  = offset * 3 + group * groupSize * 3 + elementId * 3;
		uint secondId3f = firstId3f + stride * 3;
		
		// store values
		float lifetimeMS = lifetime[firstId2f];
		float alive      = lifetime[firstId2f + 1];
		
		float posX = positions[firstId3f];
		float posY = positions[firstId3f + 1];
		float posZ = positions[firstId3f + 2];
		
		float velX = velocities[firstId3f];
		float velY = velocities[firstId3f + 1];
		float velZ = velocities[firstId3f + 2];
		
		// copy two to one
		lifetime[firstId2f]     = lifetime[secondId2f];
		lifetime[firstId2f + 1] = lifetime[secondId2f + 1];
		
		positions[firstId3f]     = positions[secondId3f];
		positions[firstId3f + 1] = positions[secondId3f + 1];
		positions[firstId3f + 2] = positions[secondId3f + 2];
		
		velocities[firstId3f]     = velocities[secondId3f];
		velocities[firstId3f + 1] = velocities[secondId3f + 1];
		velocities[firstId3f + 2] = velocities[secondId3f + 2];
		
		// receive values
		lifetime[secondId2f]     = lifetimeMS;
		lifetime[secondId2f + 1] = alive;
		
		positions[secondId3f]     = posX;
		positions[secondId3f + 1] = posY;
		positions[secondId3f + 2] = posZ;
		
		velocities[secondId3f]     = velX;
		velocities[secondId3f + 1] = velY;
		velocities[secondId3f + 2] = velZ;
	}
}

kernel void makeBitonicDown(global float* lifetime, global float* positions, global float* velocities, const int phase, const int offset)
{
	uint id     = get_global_id(0);
	uint size   = get_global_size(0);
	
	uint stride    = size / phase;
	uint group     = id / stride;
	uint elementId = id % stride;
	uint groupSize = 2 * stride;
		
	uint firstId2f  = offset * 2 + group * groupSize * 2 + elementId * 2;
	uint secondId2f = firstId2f + stride * 2;

	if(lifetime[firstId2f] < lifetime[secondId2f]) {
		uint firstId3f  = offset * 3 + group * groupSize * 3 + elementId * 3;
		uint secondId3f = firstId3f + stride * 3;
		
		// store values
		float lifetimeMS = lifetime[firstId2f];
		float alive      = lifetime[firstId2f + 1];
		
		float posX = positions[firstId3f];
		float posY = positions[firstId3f + 1];
		float posZ = positions[firstId3f + 2];
		
		float velX = velocities[firstId3f];
		float velY = velocities[firstId3f + 1];
		float velZ = velocities[firstId3f + 2];
		
		// copy two to one
		lifetime[firstId2f]     = lifetime[secondId2f];
		lifetime[firstId2f + 1] = lifetime[secondId2f + 1];
		
		positions[firstId3f]     = positions[secondId3f];
		positions[firstId3f + 1] = positions[secondId3f + 1];
		positions[firstId3f + 2] = positions[secondId3f + 2];
		
		velocities[firstId3f]     = velocities[secondId3f];
		velocities[firstId3f + 1] = velocities[secondId3f + 1];
		velocities[firstId3f + 2] = velocities[secondId3f + 2];
		
		// receive values
		lifetime[secondId2f]     = lifetimeMS;
		lifetime[secondId2f + 1] = alive;
		
		positions[secondId3f]     = posX;
		positions[secondId3f + 1] = posY;
		positions[secondId3f + 2] = posZ;
		
		velocities[secondId3f]     = velX;
		velocities[secondId3f + 1] = velY;
		velocities[secondId3f + 2] = velZ;
	}
}