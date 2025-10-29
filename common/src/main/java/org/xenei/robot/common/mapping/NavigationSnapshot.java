package org.xenei.robot.common.mapping;

import org.xenei.robot.common.utils.DoubleUtils;

/**
 * An immutable snapshot of a position and target.
 */
public class NavigationSnapshot {
    public final MapPosition position;
    public final MapLocation target;

    /**
     * Constructor.
     *
     * @param currentPosition
     *            The position to preserve in the snapshot
     * @param target
     *            the target to preserve in the snapshot.
     */
    public NavigationSnapshot(MapPosition currentPosition, MapLocation target) {
        this.position = currentPosition;
        this.target = target;
        if (position != null && target != null) {
            this.position.getTargetData(target);
        }
    }

    /**
     * Convenience method to get the heading from the stored position.
     *
     * @return the heading.
     */
    public double heading() {
        return position == null ? Double.NaN : position.getHeading();
    }

    /**
     * Checks for change in position or target.
     *
     * @param other
     *            the navigation snapshot to check against.
     * @return true if location, heading, or target has changed.
     */
    public boolean didChange(NavigationSnapshot other) {
        return didHeadingChange(other.position) || didLocationChange(other.position) || didTargetChange(other.target);
    }

    /**
     * Checks for change in position or target.
     *
     * @param positionToCheck
     *            the position to check against.
     * @param targetToCheck
     *            the target to check against.
     * @return true if location, heading, or target has changed.
     */
    public boolean didChange(MapPosition positionToCheck, MapCoordinate targetToCheck) {
        return didHeadingChange(positionToCheck) || didLocationChange(positionToCheck)
                || didTargetChange(targetToCheck);
    }

    /**
     * Checks for change in heading.
     *
     * @param snapshot
     *            the navigation snapshot to check against.
     * @return true if heading has changed.
     */
    public boolean didHeadingChange(NavigationSnapshot snapshot) {
        return didHeadingChange(snapshot.position);
    }

    /**
     * Checks for change in heading.
     *
     * @param positionToCheck
     *            the position to check against.
     * @return true if heading has changed.
     */
    public boolean didHeadingChange(MapPosition positionToCheck) {
        if (position == null) {
            return positionToCheck != null;
        }
        return positionToCheck == null || !DoubleUtils.eq(position.getHeading(), positionToCheck.getHeading());
    }

    /**
     * Checks for change in location.
     *
     * @param snapshot
     *            the navigation snapshot to check against.
     * @return true if locatoin has changed.
     */
    boolean didLocationChange(NavigationSnapshot snapshot) {
        return didLocationChange(snapshot.position);
    }

    /**
     * Checks for change in location.
     *
     * @param positionToCheck
     *            the position to check against.
     * @return true if location has changed.
     */
    boolean didLocationChange(MapPosition positionToCheck) {
        if (position == null) {
            return (positionToCheck != null);
        }
        return positionToCheck == null || position.getMap().THETA_COMPARE.compare(position, positionToCheck) != 0;
    }

    /**
     * Checks for change in target.
     *
     * @param snapshot
     *            the navigation snapshot to check against.
     * @return true if target has changed.
     */
    public boolean didTargetChange(NavigationSnapshot snapshot) {
        return didTargetChange(snapshot.target);
    }

    /**
     * Checks for change in target.
     *
     * @param coordinateToCheck
     *            the target to check against.
     * @return true if target has changed.
     */
    public boolean didTargetChange(MapCoordinate coordinateToCheck) {
        if (target == null) {
            return (coordinateToCheck != null);
        }
        return coordinateToCheck == null || target.compareTo(coordinateToCheck) != 0;
    }

    @Override
    public String toString() {
        return String.format("NavigationSnapshot[%s -> %s]", position, target);
    }
}
