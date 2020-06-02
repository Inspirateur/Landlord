package io.github.Inspirateur.Landlord;

import org.bukkit.Particle;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

public class ZoneParticles implements Runnable {
	private final Map<UUID, PartialZone> partialZones;
	/*
	ids = [
		[1, 2],
		[0, 2],
		[0, 1]
	]
	 */
	private final int[][] ids;

	public ZoneParticles(Map<UUID, PartialZone> partialZones) {
		this.partialZones = partialZones;
		ids = new int[3][];
		for(int i=0; i<=2; i++) {
			int finalI = i;
			ids[i] = IntStream.range(0, 3).filter(p->p!=finalI).toArray();
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
				for(int i=0; i<=2; i++) {
					double len = (pMax.get(i)-pMin.get(i))/2.;
					double c = pMin.get(i)+len;
					offset = new double[]{0., 0., 0.};
					offset[i] = len/3;
					for(int j=0; j<=1; j++) {
						for(int k=0; k<=1; k++) {
							center = new double[]{(double)pMin.x, (double)pMin.y, (double)pMin.z};
							center[i] = c;
							center[ids[i][0]] += (pMax.get(ids[i][0])-pMin.get(ids[i][0]))*j;
							center[ids[i][1]] += (pMax.get(ids[i][1])-pMin.get(ids[i][1]))*k;
							zone.world.spawnParticle(
								Particle.FLAME, center[0], center[1], center[2],
								(int)len*12, offset[0], offset[1], offset[2], 0.
							);
						}
					}
				}
			} else {
				// we Know one corner is defined
				Point p = zone.corner1 == null? zone.corner2 : zone.corner1;
				zone.world.spawnParticle(Particle.SPELL_INSTANT, p.x, p.y, p.z,
					24, 0, 0, 0, 0.);
			}
		}
	}
}
