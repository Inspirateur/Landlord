package io.github.Inspirateur.Landlord;

import org.bukkit.Particle;
import org.bukkit.World;

import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

public class ZoneParticles implements Runnable {
	private final Map<UUID, PartialZone> partialZones;
	private final World world;
	private final int[][] ids;

	public ZoneParticles(Map<UUID, PartialZone> partialZones, World world) {
		this.partialZones = partialZones;
		this.world = world;
		ids = new int[3][];
		for(int i=0; i<=2; i++) {
			int finalI = i;
			ids[i] = IntStream.range(0, 2).filter(p->p!=finalI).toArray();
		}
	}

	@Override
	public void run() {
		Point pMin;
		Point pMax;
		double[] center;
		double[] offset;
		for(PartialZone zone: partialZones.values()) {
			if (zone.corner1 != null && zone.corner2 != null) {
				pMin = Point.min(zone.corner1, zone.corner2);
				pMax = Point.max(zone.corner1, zone.corner2);
				center = new double[]{(double)pMin.x, (double)pMin.y, (double)pMin.z};
				for(int i=0; i<=2; i++) {
					double len = pMax.get(i)-pMin.get(i)/2.;
					double c = pMin.get(i)+len;
					center[i] = c;
					offset = new double[]{0., 0., 0.};
					offset[i] = len;
					for(int j=0; j<=1; j++) {
						for(int k=0; k<=1; k++) {
							center[ids[i][0]] += (pMax.get(ids[i][0])-pMin.get(ids[i][0]))*j;
							center[ids[i][1]] += (pMax.get(ids[i][1])-pMin.get(ids[i][1]))*k;
							world.spawnParticle(
								Particle.FLAME, center[0], center[1], center[2],
								(int)len*2, offset[0], offset[1], offset[2]
							);
						}
					}
				}
			}
		}
	}
}
