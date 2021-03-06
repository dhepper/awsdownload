/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package ro.cs.products.base;

import java.awt.geom.Rectangle2D;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Base class for reading and writing S2/L8 tile extents.
 *
 * @author  Cosmin Cara
 */
public abstract class TileMap {
    protected final Map<String, Rectangle2D> tiles;

    protected TileMap() {
        tiles = new TreeMap<>();
    }

    public void read(InputStream inputStream) throws IOException {
        try (Scanner scanner = new Scanner(inputStream)) {
            String line, tile;
            while (scanner.hasNextLine()) {
                line = scanner.nextLine();
                if (scanner.ioException() != null) {
                    throw scanner.ioException();
                }
                tile = line.substring(0, line.indexOf(" "));
                line = line.replaceAll(tile, "").trim();
                //line = line.substring(0, line.length() - 1);
                String[] tokens = line.split(",");
                Rectangle2D.Double rectangle = new Rectangle2D.Double(
                        Double.parseDouble(tokens[0].substring(2)),
                        Double.parseDouble(tokens[1].substring(2)),
                        Double.parseDouble(tokens[2].substring(2)),
                        Double.parseDouble(tokens[3].substring(2)));
                tiles.put(tile, rectangle);
            }
        } finally {
            if (inputStream != null)
                inputStream.close();
        }
    }

    public void write(Path file) throws IOException {
        BufferedWriter bufferedWriter = null;
        try {
            bufferedWriter = Files.newBufferedWriter(file, StandardOpenOption.CREATE);
            StringBuilder line = new StringBuilder();
            for (Map.Entry<String, Rectangle2D> entry : tiles.entrySet()) {
                line.append(entry.getKey()).append(" ");
                Rectangle2D rectangle = entry.getValue();
                line.append("x=").append(rectangle.getX()).append(",");
                line.append("y=").append(rectangle.getY()).append(",");
                line.append("w=").append(rectangle.getWidth()).append(",");
                line.append("h=").append(rectangle.getHeight()).append("\n");
                bufferedWriter.write(line.toString());
                bufferedWriter.flush();
                line.setLength(0);
            }
        } finally {
            if (bufferedWriter != null)
                bufferedWriter.close();
        }
    }

    public void fromKmlFile(String file) throws IOException {
        Path kmlFile = Paths.get(file);
        if (Files.exists(kmlFile)) {
            fromKml(Files.newBufferedReader(kmlFile));
        }
    }

    public abstract void fromKml(BufferedReader bufferedReader) throws IOException;

    public List<String> getTileNames() {
        return this.tiles.keySet().stream().collect(Collectors.toList());
    }

    /**
     * Returns the number of tiles contained in this map
     */
    public int getCount() {
        return tiles.size();
    }

    /**
     * Computes the bounding box for the given list of tile identifiers
     * @param tileCodes     List of tile identifiers
     */
    public Rectangle2D boundingBox(Set<String> tileCodes) {
        if (tileCodes == null) {
            return null;
        }
        Rectangle2D accumulator = null;
        for (String code : tileCodes) {
            Rectangle2D rectangle2D = tiles.get(code);
            if (rectangle2D != null) {
                if (accumulator == null) {
                    accumulator = rectangle2D;
                } else {
                    accumulator = accumulator.createUnion(rectangle2D);
                }
            }
        }
        return accumulator;
    }

    /**
     * Computes the list of tiles that intersect the given area of interest (rectangle).
     *
     * @param ulx   The upper left corner longitude (in degrees)
     * @param uly   The upper left corner latitude (in degrees)
     * @param lrx   The lower right corner longitude (in degrees)
     * @param lry   The lower right corner latitude (in degrees)
     */
    public Set<String> intersectingTiles(double ulx, double uly, double lrx, double lry) {
        return intersectingTiles(new Rectangle2D.Double(ulx, uly, ulx - lrx, uly - lry));
    }
    /**
     * Computes the list of tiles that intersect the given area of interest (rectangle).
     *
     * @param aoi   The area of interest bounding box
     */
    public Set<String> intersectingTiles(Rectangle2D aoi) {
        Set<String> tileCodes = new HashSet<>();
        tileCodes.addAll(
                tiles.entrySet().stream()
                        .filter(entry -> entry.getValue().intersects(aoi))
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toSet()));
        return tileCodes;
    }

    protected Rectangle2D boundingBox(Rectangle2D...rectangles) {
        if (rectangles == null) {
            return null;
        }
        if (rectangles.length == 1) {
            return rectangles[0];
        } else {
            Rectangle2D accumulator = rectangles[0];
            for (int i = 1; i < rectangles.length; i++) {
                accumulator.add(rectangles[i]);
            }
            return accumulator;
        }
    }

}
