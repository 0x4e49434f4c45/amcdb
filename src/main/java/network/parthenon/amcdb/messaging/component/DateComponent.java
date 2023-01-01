package network.parthenon.amcdb.messaging.component;

import java.awt.Color;
import java.time.Duration;
import java.time.Instant;
import java.time.Period;
import java.util.Date;
import java.util.EnumSet;

public class DateComponent implements InternalMessageComponent {
    private final Color color;

    private final long timestamp;

    private final EnumSet<Style> appliedStyles;

    private final DateFormat format;

    public DateComponent(long timestamp, DateFormat format) {
        this(timestamp, format,null, EnumSet.noneOf(Style.class));
    }

    public DateComponent(long timestamp, DateFormat format, Color color, EnumSet<Style> appliedStyles) {
        if(format == null) {
            throw new IllegalArgumentException("Date format may not be null");
        }
        if(appliedStyles == null) {
            throw new IllegalArgumentException("Style may not be null");
        }
        this.timestamp = timestamp;
        this.format = format;
        this.color = color;
        this.appliedStyles = appliedStyles;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public DateFormat getDateFormat() {
        return format;
    }

    /**
     * Returns a DateComponent with the same time, color, and styles as this one,
     * with a format of {@link DateFormat#ABSOLUTE}.
     * @return
     */
    public DateComponent asAbsolute() {
        return getDateFormat() == DateFormat.ABSOLUTE ?
                this :
                new DateComponent(timestamp, DateFormat.ABSOLUTE, color, appliedStyles);
    }

    /**
     * Returns a DateComponent with the same time, color, and styles as this one,
     * with a format of {@link DateFormat#RELATIVE}.
     * @return
     */
    public DateComponent asRelative() {
        return getDateFormat() == DateFormat.RELATIVE ?
                this :
                new DateComponent(timestamp, DateFormat.RELATIVE, color, appliedStyles);
    }

    @Override
    public Color getColor() {
        return color;
    }

    @Override
    public String getText() {
        if(format == DateFormat.ABSOLUTE) {
            return new Date(timestamp).toString();
        }

        Duration duration = Duration.between(
                Instant.ofEpochMilli(System.currentTimeMillis()),
                Instant.ofEpochMilli(timestamp));
        Duration absDuration = duration.abs();

        StringBuilder dateBuilder = new StringBuilder();
        long part;
        boolean firstUnit = true;
        if(!duration.isNegative()) {
            dateBuilder.append("in ");
        }

        if ((part = absDuration.toDaysPart()) > 0) {
            dateBuilder.append(firstUnit ? "" : " ").append(part).append(part == 1 ? " day" : " days");
            firstUnit = false;
        }
        if ((part = absDuration.toHoursPart()) > 0) {
            dateBuilder.append(firstUnit ? "" : " ").append(part).append(part == 1 ? " hour" : " hours");
            firstUnit = false;
        }
        if ((part = absDuration.toMinutesPart()) > 0) {
            dateBuilder.append(firstUnit ? "" : " ").append(part).append(part == 1 ? " minute" : " minutes");
            firstUnit = false;
        }
        if ((part = absDuration.toSecondsPart()) > 0 || firstUnit) {
            // if the seconds part is zero but we still haven't added anything,
            // go ahead and say 0 seconds
            dateBuilder.append(firstUnit ? "" : " ").append(part).append(part == 1 ? " second" : " seconds");
        }

        if(duration.isNegative()) {
            dateBuilder.append(" ago");
        }

        return dateBuilder.toString();
    }

    @Override
    public EnumSet<Style> getStyles() {
        return ComponentUtils.copyStyleSet(appliedStyles);
    }

    /**
     * Indicates how a consumer should format a DateComponent.
     * The specific details are left to the consumer; this simply hints
     * whether the time ought to be displayed as a specific point or
     * relative to the current time.
     */
    public enum DateFormat {
        /**
         * Date should be displayed as a date and time (e.g. December 31, 2022 8:43 PM).
         */
        ABSOLUTE,
        /**
         * Date should be displayed relative to the current time (e.g. "4 minutes ago").
         */
        RELATIVE
    }
}
