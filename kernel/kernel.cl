float3 closestLPAdirection(global float* lpas, float3 position)
{
	float3 close = (float3)(0.0f, 1.0f, 0.0f);
	
	float distance = length(close - position);
	
	float dist = 0;
	float3 curLPA;
	for(int i = 0; i < 30; i+=3)
	{
		curLPA = (float3)(lpas[i], lpas[i+1], lpas[i+2]);
		if(curLPA.y > position.y)
		{
			dist = length(curLPA - position);
			if(dist < distance)
			{
				distance = dist;
				close = curLPA;
			}
		}
	}
	
	return normalize(close - position);
}

kernel void move(global float* positions, global float* velocities, global float* lifetimes, global float* lowPressureAreas, const int dTime)
{
	const uint id = get_global_id(0);
	
	const float lifetime = lifetimes[id * 2] - ((float)dTime);
	
	if(lifetime <= 0.0f) 
	{
		lifetimes[id * 2]     = -10000.0f;
		lifetimes[id * 2 + 1] = 0.0f;
		return;
	}
	
	const float baseSpeed = 0.0001f;
	const float speed = baseSpeed * ((float)dTime);

	const float  alive    = lifetimes[id * 2 + 1];
	const float3 position = (float3)(positions [id * 3], positions [id * 3 + 1], positions [id * 3 + 2]);
	const float3 velocity = (float3)(velocities[id * 3], velocities[id * 3 + 1], velocities[id * 3 + 2]);
	
	float3 newPosition = (float3)0.0f;
	float3 newVelocity = (float3)0.0f;
	
	newVelocity = normalize(velocity) * 0.9995f + closestLPAdirection(lowPressureAreas, position) * 0.0005f;
	newPosition = position + newVelocity * speed;
	
	
	

	
	
	
	lifetimes [id * 2]     = lifetime;
	positions [id * 3]     = newPosition.x;
	positions [id * 3 + 1] = newPosition.y;
	positions [id * 3 + 2] = newPosition.z;
	velocities[id * 3]     = newVelocity.x;
	velocities[id * 3 + 1] = newVelocity.y;
	velocities[id * 3 + 2] = newVelocity.z;
	
}

kernel void respawn(global float* positions, global float* velocities, global float* lifetimes, global float* newValues, const int numElements, const int offset) 
{
	const uint newId = get_global_id(0);
	const uint oldId = (newId + offset) % numElements; 
	
	const ushort numParams = 7;
	
	positions [oldId * 3]     = newValues[newId * numParams];
	positions [oldId * 3 + 1] = newValues[newId * numParams + 1];
	positions [oldId * 3 + 2] = newValues[newId * numParams + 2];
	velocities[oldId * 3]     = newValues[newId * numParams + 3];
	velocities[oldId * 3 + 1] = newValues[newId * numParams + 4];
	velocities[oldId * 3 + 2] = newValues[newId * numParams + 5];
	lifetimes [oldId * 2]     = newValues[newId * numParams + 6];
	lifetimes [oldId * 2 + 1] = 1.0f;
}