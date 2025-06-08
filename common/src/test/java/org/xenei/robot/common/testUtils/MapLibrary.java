package org.xenei.robot.common.testUtils;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.xenei.robot.common.ScaleInfo;
import org.xenei.robot.common.mapping.MapBuilder;
import org.xenei.robot.common.mapping.Map;
import org.xenei.robot.common.planning.Solution;
import org.xenei.robot.common.utils.RobutContext;
import org.xenei.robot.mapper.MapImpl;
import org.xenei.robot.mapper.visualization.TextViz;

public class MapLibrary {

    public static Map map1(Map map) {
        MapBuilder b = new MapBuilder(map);

        b.setX(-1, -1, 14, MapBuilder.Type.Obstacle);
        b.setX(16, -1, 14, MapBuilder.Type.Obstacle);

        b.setY(-1, 0, 15, MapBuilder.Type.Obstacle);
        b.setY(14, 0, 15, MapBuilder.Type.Obstacle);

        b.setX(1, 0, 6, MapBuilder.Type.Obstacle);
        b.setY(2, 2, 8, MapBuilder.Type.Obstacle);

        b.set(1, 3);
        b.set(1, 7);
        b.set(0, 5);

        b.setY(4, 2, 8, MapBuilder.Type.Obstacle);

        b.setY(8, 0, 3, MapBuilder.Type.Obstacle);

        b.setX(3, 6, 8, MapBuilder.Type.Obstacle);

        b.setY(12, 0, 3, MapBuilder.Type.Obstacle);

        b.set(0, 10);

        b.setY(1, 10, 14, MapBuilder.Type.Obstacle);

        b.setX(14, 1, 3, MapBuilder.Type.Obstacle);

        b.setX(12, 1, 6, MapBuilder.Type.Obstacle);

        b.setY(6, 7, 11, MapBuilder.Type.Obstacle);

        b.set(5, 13);

        b.setX(14, 5, 12, MapBuilder.Type.Obstacle);

        b.setX(5, 4, 12, MapBuilder.Type.Obstacle);

        b.setY(10, 1, 10, MapBuilder.Type.Obstacle);

        b.setY(8, 7, 10, MapBuilder.Type.Obstacle);

        b.setX(10, 8, 12, MapBuilder.Type.Obstacle);

        b.setY(12, 5, 7, MapBuilder.Type.Obstacle);

        return b.build();
    }

    public static Map map2(Map map) {
        MapBuilder b = new MapBuilder(map);

        b.border(-5, -5, 9, 9);

        b.setX(-1, -3, 1, MapBuilder.Type.Obstacle);

        return b.build();
    }

    public static Map map3(Map map) {
        MapBuilder b = new MapBuilder(map);

        b.border(-5, -5, 9, 9);

        b.setX(-1, -3, 1, MapBuilder.Type.Obstacle);
        b.setY(0, 0, 2, MapBuilder.Type.Obstacle);

        return b.build();
    }

    private static Options getOptions() {
        Options options = new Options();
        options.addOption(Option.builder("m").desc("Model number").hasArg().type(Integer.class).build());
        options.addOption(Option.builder("v").desc("View flag").build());
        options.addOption(Option.builder("o").desc("list obstacles").build());
        return options;
    }

    public static void main(String[] args) throws ParseException {
        CommandLine commandLine = DefaultParser.builder().build().parse(getOptions(), args);
        int mapNumber = commandLine.getParsedOptionValue("m");
        Map map = new MapImpl(new RobutContext(ScaleInfo.DEFAULT, TestChassisInfo.DEFAULT));
        switch (mapNumber) {
            case 1:
                map1(map);
                break;
                case 2:
                    map2(map);
                    break;
            case 3:
                map3(map);
                break;
            default:
                System.err.println("Unknown map number: " + mapNumber);
                System.exit(1);
        }
        if (commandLine.hasOption("v")) {
            TextViz textVis = new TextViz(1, map, () -> new Solution(), () -> null, () -> null);
            textVis.redraw();
        }
        if (commandLine.hasOption("o")) {
            System.out.println(" =========== Obstacles ==========");
            map.getObstacles().thenAccept(s -> s.forEach(o -> System.out.println(o))).join();
        }
    }

}
