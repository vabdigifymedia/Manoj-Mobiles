package com.api.manojmobiles.utils;

/**
 * Geometry helpers for hyperlocal tracking.
 *
 * All distances are in metres, all coordinates in decimal degrees (WGS-84).
 */
public final class GeoUtils {

    /** Mean Earth radius in metres. */
    private static final double EARTH_RADIUS_M = 6_371_008.8;

    private GeoUtils() {
    }

    /**
     * Great-circle distance between two points.
     */
    public static double haversineMetres(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_M * c;
    }

    /**
     * Distance between two lat/lng points. Null-safe: returns {@link Double#MAX_VALUE}
     * when either point is incomplete so that "nearest agent" scans skip bad data.
     */
    public static double safeDistance(Double lat1, Double lng1, Double lat2, Double lng2) {
        if (lat1 == null || lng1 == null || lat2 == null || lng2 == null) {
            return Double.MAX_VALUE;
        }
        return haversineMetres(lat1, lng1, lat2, lng2);
    }

    /**
     * Initial bearing (compass heading, 0-360 degrees, 0 = north) from point 1 to point 2.
     * Used to rotate the bike marker so it points in the direction of travel.
     */
    public static double bearingDegrees(double lat1, double lng1, double lat2, double lng2) {
        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double dLng = Math.toRadians(lng2 - lng1);
        double y = Math.sin(dLng) * Math.cos(phi2);
        double x = Math.cos(phi1) * Math.sin(phi2)
                - Math.sin(phi1) * Math.cos(phi2) * Math.cos(dLng);
        return (Math.toDegrees(Math.atan2(y, x)) + 360.0) % 360.0;
    }

    /**
     * Total length of a polyline in metres.
     */
    public static double pathLengthMetres(double[][] path) {
        if (path == null || path.length < 2) {
            return 0;
        }
        double total = 0;
        for (int i = 1; i < path.length; i++) {
            total += haversineMetres(path[i - 1][0], path[i - 1][1], path[i][0], path[i][1]);
        }
        return total;
    }

    /**
     * Finds where a live GPS ping sits along the planned route.
     *
     * <p>For every segment of the polyline we project the ping onto the segment and keep
     * the closest one. This "snaps" a jittery GPS point onto the road so the UI can draw
     * the remaining route from exactly that spot, the way Swiggy/Zomato do.
     *
     * @param path  route as {@code [lat, lng]} pairs
     * @param lat   live latitude
     * @param lng   live longitude
     * @return snapped point plus the fraction (0..1) of the route already covered
     */
    public static SnapResult snapToPath(double[][] path, double lat, double lng) {
        if (path == null || path.length == 0) {
            return new SnapResult(lat, lng, 0.0, 0.0);
        }
        if (path.length == 1) {
            return new SnapResult(path[0][0], path[0][1], 0.0, 0.0);
        }

        double totalLength = pathLengthMetres(path);
        double bestDistance = Double.MAX_VALUE;
        double bestLat = path[0][0];
        double bestLng = path[0][1];
        double bestAlong = 0.0;
        double travelled = 0.0;

        for (int i = 1; i < path.length; i++) {
            double[] a = path[i - 1];
            double[] b = path[i];
            double[] projected = projectOntoSegment(a[0], a[1], b[0], b[1], lat, lng);
            double distance = haversineMetres(lat, lng, projected[0], projected[1]);

            double segmentLength = haversineMetres(a[0], a[1], b[0], b[1]);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestLat = projected[0];
                bestLng = projected[1];
                // How far into this segment the projection landed (s, t) = (0..1, 0..1)
                double t = segmentFraction(a[0], a[1], b[0], b[1], projected[0], projected[1]);
                bestAlong = travelled + (segmentLength * t);
            }
            travelled += segmentLength;
        }

        double progress = totalLength <= 0 ? 0.0 : Math.min(1.0, Math.max(0.0, bestAlong / totalLength));
        return new SnapResult(bestLat, bestLng, progress, totalLength - bestAlong);
    }

    /**
     * Point at a given fraction (0..1) along a polyline — used to extrapolate the
     * marker forward between GPS pings so it glides instead of teleporting.
     */
    public static double[] pointAtFraction(double[][] path, double fraction) {
        if (path == null || path.length == 0) {
            return null;
        }
        if (path.length == 1) {
            return path[0];
        }
        double totalLength = pathLengthMetres(path);
        double target = Math.min(1.0, Math.max(0.0, fraction)) * totalLength;

        double covered = 0;
        for (int i = 1; i < path.length; i++) {
            double segmentLength = haversineMetres(path[i - 1][0], path[i - 1][1], path[i][0], path[i][1]);
            if (covered + segmentLength >= target) {
                double remaining = target - covered;
                double t = segmentLength <= 0 ? 0 : remaining / segmentLength;
                double lat = path[i - 1][0] + (path[i][0] - path[i - 1][0]) * t;
                double lng = path[i - 1][1] + (path[i][1] - path[i - 1][1]) * t;
                return new double[]{lat, lng};
            }
            covered += segmentLength;
        }
        return path[path.length - 1];
    }

    /**
     * Projects point p onto the great-circle-ish segment a-b using equirectangular
     * approximation (accurate enough at hyperlocal distances, cheap to compute).
     */
    private static double[] projectOntoSegment(double aLat, double aLng, double bLat, double bLng,
                                               double pLat, double pLng) {
        double scale = Math.cos(Math.toRadians((aLat + bLat) / 2.0));
        double ax = aLng * scale;
        double ay = aLat;
        double bx = bLng * scale;
        double by = bLat;
        double px = pLng * scale;
        double py = pLat;

        double dx = bx - ax;
        double dy = by - ay;
        double lengthSq = dx * dx + dy * dy;

        if (lengthSq == 0) {
            return new double[]{aLat, aLng};
        }

        double t = ((px - ax) * dx + (py - ay) * dy) / lengthSq;
        t = Math.max(0, Math.min(1, t));

        double projX = ax + t * dx;
        double projY = ay + t * dy;
        return new double[]{projY, projX / scale};
    }

    private static double segmentFraction(double aLat, double aLng, double bLat, double bLng,
                                          double pLat, double pLng) {
        double scale = Math.cos(Math.toRadians((aLat + bLat) / 2.0));
        double ax = aLng * scale, ay = aLat;
        double bx = bLng * scale, by = bLat;
        double px = pLng * scale, py = pLat;
        double dx = bx - ax, dy = by - ay;
        double lengthSq = dx * dx + dy * dy;
        if (lengthSq == 0) {
            return 0;
        }
        double t = ((px - ax) * dx + (py - ay) * dy) / lengthSq;
        return Math.max(0, Math.min(1, t));
    }

    /**
     * Result of snapping a GPS ping onto the planned route.
     *
     * @param lat              snapped latitude
     * @param lng              snapped longitude
     * @param progress         fraction of route covered (0..1)
     * @param remainingMetres  distance still to travel along the route
     */
    public record SnapResult(double lat, double lng, double progress, double remainingMetres) {
    }
}
