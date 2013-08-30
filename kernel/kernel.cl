// Reorders the float4 values "ids" by "distances", such that the biggest is in .w
void reorder(
	float4* distances, 
	int4* ids
	) 
{
	if((*distances).w < (*distances).z)
	{
		float fTmp = (*distances).w;
		(*distances).w = (*distances).z;
		(*distances).z = fTmp;
		int iTmp = (*ids).w;
		(*ids).w = (*ids).z;
		(*ids).z = iTmp;
	}
	if((*distances).z < (*distances).y)
	{
		float fTmp = (*distances).z;
		(*distances).z = (*distances).y;
		(*distances).y = fTmp;
		int iTmp = (*ids).z;
		(*ids).z = (*ids).y;
		(*ids).y = iTmp;
	}
	if((*distances).y < (*distances).x)
	{
		float fTmp = (*distances).y;
		(*distances).y = (*distances).x;
		(*distances).x = fTmp;
		int iTmp = (*ids).y;
		(*ids).y = (*ids).x;
		(*ids).x = iTmp;
	}
}

// calculates the 4 closes LPAs and returns one randomly
const float3 closestLPAdirection(
	global float* lpas, 
	const int numLPA, 
	const float3 position, 
	const int randIndex
	)
{
	float3 close = (float3)(0.0f, 10.0f, 0.0f);
	
	// distance.w is the biggest
	int4 ids = (int4)(-1);
	float4 distance = (float4) length(close - position);
	
	float dist = 0;
	float3 curLPA;
	for(int i = 0; i < numLPA; i+=3)
	{
		curLPA = (float3)(lpas[i], lpas[i+1], lpas[i+2]);
		if(curLPA.y-0.2f > position.y)
		{
			dist = length(curLPA - position);
			if(dist < distance.w)
			{
				distance.w = dist;
				ids.w = i;
				reorder(&distance, &ids);
			}
		}
	}
	
	int id;
	switch(randIndex)
	{
		case 0: id = ids.x; break;
		case 1: id = ids.y; break;
		case 2: id = ids.z; break;
		case 3: id = ids.w; break; 
	}
	
	if(id != -1)
		close = (float3)(lpas[id], lpas[id+1], lpas[id+2]);
	
	return normalize(close - position);
}

// moves particles according to all forces which affect it
kernel void move(
	global float* positions, 
	global float* velocities, 
	global float* lifetimes, 
	global float* lowPressureAreas, 
	const global int* randIndexLPA, 
	const int numLPA, 
	const int dTime,
	const int pulse, // dirty test hack
	const global float* pulseData
	)
{
	const uint id = get_global_id(0);
	const float lifetime = lifetimes[id * 2] - ((float)dTime);
	
	if(lifetime <= 0.0f) 
	{
		lifetimes[id * 2]     = 0.0f;
		lifetimes[id * 2 + 1] = 0.0f;
		return;
	}
	
	const float baseSpeed     = 0.001f;
	const float speed = baseSpeed * ((float)dTime);

	const float  alive    = lifetimes[id * 2 + 1];
	const float3 position = (float3)(positions [id * 3], positions [id * 3 + 1], positions [id * 3 + 2]);
	const float3 velocity = (float3)(velocities[id * 3], velocities[id * 3 + 1], velocities[id * 3 + 2]);
	
	float3 newPosition = (float3)0.0f;
	float3 newVelocity = (float3)0.0f;
	
	newVelocity = mix(normalize(velocity), closestLPAdirection(lowPressureAreas, numLPA, position, randIndexLPA[id]), 2.5f * speed);
	newPosition = position + newVelocity * speed;
	
	// dirty test hack
	if(pulse) {
		const float3 pulsePosition  = (float3)(pulseData[0], pulseData[1], pulseData[2]);
		const float3 pulseDirection = (float3)(pulseData[3], pulseData[4], pulseData[5]);
		const float  pulseStrength  = pulseData[6];
		
		const float3 diffVector     = position - pulsePosition;
		const float dotProduct = dot(normalize(diffVector), pulseDirection);
		if(dotProduct >= 0.8f) {
			newVelocity += pulseDirection * pulseStrength / dotProduct / dot(diffVector, diffVector);
		}
	}
	

	
	
	
	lifetimes [id * 2]     = lifetime;
	positions [id * 3]     = newPosition.x;
	positions [id * 3 + 1] = newPosition.y;
	positions [id * 3 + 2] = newPosition.z;
	velocities[id * 3]     = newVelocity.x;
	velocities[id * 3 + 1] = newVelocity.y;
	velocities[id * 3 + 2] = newVelocity.z;
	
}

// respawns a certain amount of particles
kernel void respawn(
	global float* positions, 
	global float* velocities, 
	global float* lifetimes, 
	global float* newValues, 
	const int numElements, 
	const int offset
	) 
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