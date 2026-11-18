package com.api.manojmobiles.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Encoder/decoder for the Google "Encoded Polyline Algorithm Format".
 *
 * <p>Google Directions API returns route geometry as one long ASCII string. Encoding it
 * keeps the payload small enough to store in Postgres and push over WebSocket without
 * bloating either. The React client can decode it with
 * {@code google.maps.geometry.encoding.decodePath()}.
 *
 * @see <a href="https://developers.google.com/maps/documentation/utilities/polylinealgorithm">Google docs</a>
 */
public final class PolylineCodec {

    private PolylineCodec() {
    }

    /**
     * Encodes {@code [lat, lng]} pairs into Google's compressed polyline string.
     */
    public static String encode(double[][] points) {
        if (points == null || points.length == 0) {
            return "";
        }
        StringBuilder encoded = new StringBuilder();
        long previousLat = 0;
        long previousLng = 0;

        for (double[] point : points) {
            long lat = Math.round(point[0] * 1e5);
            long lng = Math.round(point[1] * 1e5);
            encodeValue(lat - previousLat, encoded);
            encodeValue(lng - previousLng, encoded);
            previousLat = lat;
            previousLng = lng;
        }
        return encoded.toString();
    }

    /**
     * Decodes a Google polyline string into {@code [lat, lng]} pairs.
     */
    public static double[][] decode(String encoded) {
        if (encoded == null || encoded.isEmpty()) {
            return new double[0][];
        }
        List<double[]> points = new ArrayList<>();
        int index = 0;
        int lat = 0;
        int lng = 0;

        while (index < encoded.length()) {
            int[] latResult = decodeValue(encoded, index);
            lat += latResult[0];
            index = latResult[1];

            int[] lngResult = decodeValue(encoded, index);
            lng += lngResult[0];
            index = lngResult[1];

            points.add(new double[]{lat / 1e5, lng / 1e5});
        }
        return points.toArray(new double[0][]);
    }

    private static void encodeValue(long value, StringBuilder out) {
        long shifted = value < 0 ? ~(value << 1) : (value << 1);
        while (shifted >= 0x20) {
            out.append((char) ((0x20 | (shifted & 0x1f)) + 63));
            shifted >>= 5;
        }
        out.append((char) (shifted + 63));
    }

    /** @return {@code [value, nextIndex]} */
    private static int[] decodeValue(String encoded, int startIndex) {
        int index = startIndex;
        int shift = 0;
        int result = 0;
        int b;
        do {
            b = encoded.charAt(index++) - 63;
            result |= (b & 0x1f) << shift;
            shift += 5;
        } while (b >= 0x20);
        int value = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
        return new int[]{value, index};
    }
}
